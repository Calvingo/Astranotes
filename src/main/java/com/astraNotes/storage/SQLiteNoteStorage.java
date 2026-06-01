package com.astraNotes.storage;

import com.astraNotes.model.Note;
import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.encryption.EncryptionException;
import com.astraNotes.plugin.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * SQLite-based implementation of NoteRepository with AES encryption integration.
 * Handles database initialization, CRUD operations, search via FTS5, and transaction management.
 * REQ-6: Offline operation / REQ-NFR-1: Performance (search under 150ms)
 * REQ-SEC-4: ACID reliability via transactions
 */
public class SQLiteNoteStorage implements NoteRepository {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteNoteStorage.class);
    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final String dbPath;
    private final EncryptionManager encryptionManager;
    private final com.astraNotes.plugin.PluginManager pluginManager;
    private Connection connection;

    public SQLiteNoteStorage(String dbPath, EncryptionManager encryptionManager) {
        this(dbPath, encryptionManager, null);
    }

    public SQLiteNoteStorage(String dbPath, EncryptionManager encryptionManager, com.astraNotes.plugin.PluginManager pluginManager) {
        this.dbPath = dbPath;
        this.encryptionManager = encryptionManager;
        this.pluginManager = pluginManager;
    }

    /**
     * Initialize the database connection and create schema if needed.
     */
    public void initialize() throws StorageException {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(false);
            // Attempt to enable SQLCipher if EncryptionManager is unlocked and SQLCipher is available.
            try {
                String hexKey = encryptionManager != null ? encryptionManager.getRootKeyHex() : null;
                if (hexKey != null && !hexKey.isBlank()) {
                    try (Statement pragmaStmt = connection.createStatement()) {
                        String pragma = "PRAGMA key = \"x'" + hexKey + "'\";";
                        pragmaStmt.execute(pragma);
                        // quick check whether SQLCipher is active
                        try (ResultSet rs = pragmaStmt.executeQuery("PRAGMA cipher_version;")) {
                            if (rs.next()) {
                                logger.info("SQLCipher enabled (cipher_version={})", rs.getString(1));
                            } else {
                                logger.warn("PRAGMA cipher_version returned no rows; SQLCipher may not be available");
                            }
                        } catch (SQLException ex) {
                            logger.warn("SQLCipher not available or cipher_version check failed: {}", ex.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to set SQLCipher key; falling back to application-level encryption: {}", e.getMessage());
            }
            migrateSchema();
            if (pluginManager != null) {
                pluginManager.setPluginStateStore(new com.astraNotes.plugin.PluginStateStore(connection));
            }
            logger.info("SQLite storage initialized at: {}", dbPath);
        } catch (SQLException e) {
            throw new StorageException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    /**
     * Create database schema with notes table and FTS5 index.
     * REQ-SEC-5: Schema versioning and migration
     */
    private void createSchema() throws StorageException {
        String createNotesTable = """
            CREATE TABLE IF NOT EXISTS notes (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                body BLOB NOT NULL,
                tags TEXT,
                notebook TEXT NOT NULL DEFAULT 'default',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                version INTEGER NOT NULL DEFAULT 1,
                deleted BOOLEAN NOT NULL DEFAULT 0,
                hmac BLOB
            );
            """;

        String createFtsTable = """
            CREATE VIRTUAL TABLE IF NOT EXISTS notes_fts USING fts5(
                title, body_snippet, tags, notebook
            );
            """;

        String createPluginStateTable = """
            CREATE TABLE IF NOT EXISTS plugin_state (
                plugin_id TEXT NOT NULL,
                state_key TEXT NOT NULL,
                state_value TEXT,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (plugin_id, state_key)
            );
            """;

        String createSchemaVersionTable = """
            CREATE TABLE IF NOT EXISTS schema_version (
                version INTEGER NOT NULL,
                upgraded_at INTEGER NOT NULL
            );
            """;

        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_notes_deleted ON notes(deleted);
            CREATE INDEX IF NOT EXISTS idx_notes_updated_at ON notes(updated_at DESC);
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createNotesTable);
            stmt.execute(createFtsTable);
            stmt.execute(createPluginStateTable);
            stmt.execute(createSchemaVersionTable);
            stmt.execute(createIndexes);
            if (!schemaVersionRowExists()) {
                insertSchemaVersion(CURRENT_SCHEMA_VERSION);
            }
            connection.commit();
            logger.debug("Database schema created/verified");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback failed", rollbackEx);
            }
            throw new StorageException("Failed to create schema: " + e.getMessage(), e);
        }
    }

    private boolean schemaVersionRowExists() throws SQLException {
        String query = "SELECT COUNT(*) FROM schema_version;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void insertSchemaVersion(int version) throws SQLException {
        String insert = "INSERT INTO schema_version (version, upgraded_at) VALUES (?, ?);";
        try (PreparedStatement pstmt = connection.prepareStatement(insert)) {
            pstmt.setInt(1, version);
            pstmt.setLong(2, Instant.now().getEpochSecond());
            pstmt.executeUpdate();
        }
    }

    private void migrateSchema() throws StorageException {
        createSchema();
        int existingVersion = 0;
        String select = "SELECT version FROM schema_version ORDER BY upgraded_at DESC LIMIT 1;";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(select)) {
            if (rs.next()) {
                existingVersion = rs.getInt("version");
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to read schema version: " + e.getMessage(), e);
        }

        if (existingVersion < CURRENT_SCHEMA_VERSION) {
            logger.info("Migrating schema from version {} to {}", existingVersion, CURRENT_SCHEMA_VERSION);
            // Future migrations should be added here.
            try {
                insertSchemaVersion(CURRENT_SCHEMA_VERSION);
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Rollback failed during schema migration", rollbackEx);
                }
                throw new StorageException("Failed to migrate schema: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Create a new note with encryption and HMAC.
     * REQ-1: Create note
     */
    @Override
    public String create(String title, String body, List<String> tags, String notebook) throws StorageException {
        Note note = new Note(title, body, tags, notebook);
        // plugin hook: beforeCreate (may veto)
        if (pluginManager != null) {
            boolean ok = pluginManager.notifyBeforeCreate(note);
            if (!ok) {
                throw new StorageException("Creation vetoed by plugin");
            }
        }

        try {
            byte[] encryptedBody = encryptionManager.encrypt(body);
            byte[] hmac = encryptionManager.computeHMAC(note.getId(), body);
            note.setHmac(hmac);

            String insertNotesSql = """
                INSERT INTO notes (id, title, body, tags, notebook, created_at, updated_at, version, deleted, hmac)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

            try (PreparedStatement pstmt = connection.prepareStatement(insertNotesSql)) {
                pstmt.setString(1, note.getId());
                pstmt.setString(2, title);
                pstmt.setBytes(3, encryptedBody);
                pstmt.setString(4, String.join(",", tags != null ? tags : new ArrayList<>()));
                pstmt.setString(5, notebook);
                pstmt.setLong(6, note.getCreatedAt().getEpochSecond());
                pstmt.setLong(7, note.getUpdatedAt().getEpochSecond());
                pstmt.setInt(8, note.getVersion());
                pstmt.setBoolean(9, note.isDeleted());
                pstmt.setBytes(10, hmac);
                pstmt.executeUpdate();
            }

            // Update FTS index
            updateFtsIndex(note.getId(), title, body, tags, notebook);

            // plugin hook: afterUpdate-like for create (notify as update)
            if (pluginManager != null) {
                pluginManager.notifyAfterUpdate(note);
            }

            connection.commit();
            logger.info("Note created: {}", note.getId());
            return note.getId();
        } catch (EncryptionException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback failed", rollbackEx);
            }
            throw new StorageException("Encryption failed during note creation: " + e.getMessage(), e);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback failed", rollbackEx);
            }
            throw new StorageException("Failed to create note: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve a note by ID with decryption.
     * REQ-2: Read note
     */
    @Override
    public Optional<Note> get(String id) throws StorageException {
        String sql = "SELECT * FROM notes WHERE id = ? AND deleted = 0;";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(reconstructNote(rs));
            }
            return Optional.empty();
        } catch (StorageException e) {
            throw e;
        } catch (SQLException e) {
            throw new StorageException("Failed to retrieve note: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing note's content.
     * REQ-3: Update note with versioning
     */
    @Override
    public boolean update(String id, String title, String body, List<String> tags) throws StorageException {
        try {
            byte[] encryptedBody = encryptionManager.encrypt(body);
            byte[] hmac = encryptionManager.computeHMAC(id, body);

            String updateSql = """
                UPDATE notes SET title = ?, body = ?, tags = ?, updated_at = ?, version = version + 1, hmac = ?
                WHERE id = ? AND deleted = 0;
                """;

            try (PreparedStatement pstmt = connection.prepareStatement(updateSql)) {
                pstmt.setString(1, title);
                pstmt.setBytes(2, encryptedBody);
                pstmt.setString(3, String.join(",", tags != null ? tags : new ArrayList<>()));
                pstmt.setLong(4, Instant.now().getEpochSecond());
                pstmt.setBytes(5, hmac);
                pstmt.setString(6, id);
                int rowsAffected = pstmt.executeUpdate();

                // Update FTS index
                if (rowsAffected > 0) {
                    updateFtsIndex(id, title, body, tags, null);
                    // plugin hook: after update
                    if (pluginManager != null) {
                        try {
                            Note updated = new Note(id, title, body, tags, null, Instant.ofEpochSecond(Instant.now().getEpochSecond()), Instant.ofEpochSecond(Instant.now().getEpochSecond()), 0, false, hmac);
                            pluginManager.notifyAfterUpdate(updated);
                        } catch (Exception e) {
                            logger.error("Failed to notify plugin after update", e);
                        }
                    }
                }

                connection.commit();
                logger.info("Note updated: {}", id);
                return rowsAffected > 0;
            }
        } catch (EncryptionException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback failed", rollbackEx);
            }
            throw new StorageException("Encryption failed during update: " + e.getMessage(), e);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback failed", rollbackEx);
            }
            throw new StorageException("Failed to update note: " + e.getMessage(), e);
        }
    }

    /**
     * Soft-delete a note.
     * REQ-4: Soft-delete note
     */
    @Override
    public boolean delete(String id) throws StorageException {
        // plugin hook: allow veto
        if (pluginManager != null) {
            boolean ok = pluginManager.notifyBeforeDelete(id);
            if (!ok) {
                throw new StorageException("Delete vetoed by plugin");
            }
        }

        String deleteSql = "UPDATE notes SET deleted = 1, updated_at = ? WHERE id = ?;";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteSql)) {
            pstmt.setLong(1, Instant.now().getEpochSecond());
            pstmt.setString(2, id);
            int rowsAffected = pstmt.executeUpdate();
            connection.commit();
            logger.info("Note soft-deleted: {}", id);
            return rowsAffected > 0;
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.error("Rollback failed", rollbackEx);
            }
            throw new StorageException("Failed to delete note: " + e.getMessage(), e);
        }
    }

    /**
     * List non-deleted notes with pagination.
     */
    @Override
    public List<Note> list(int offset, int limit) throws StorageException {
        String sql = "SELECT * FROM notes WHERE deleted = 0 ORDER BY updated_at DESC LIMIT ? OFFSET ?;";
        List<Note> notes = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                try {
                    notes.add(reconstructNote(rs));
                } catch (StorageException ignored) {
                    logger.warn("Skipping note with invalid integrity during list retrieval: {}", rs.getString("id"));
                }
            }
            logger.debug("Listed {} notes", notes.size());
            return notes;
        } catch (SQLException e) {
            throw new StorageException("Failed to list notes: " + e.getMessage(), e);
        }
    }

    /**
     * Search notes by title, body, or tags.
     * Uses LIKE pattern matching on indexed columns for efficiency.
     * REQ-5: Search notes efficiently
     */
    @Override
    public List<Note> search(String query, int offset, int limit) throws StorageException {
        // allow plugins to modify query
        String q = query;
        if (pluginManager != null) {
            q = pluginManager.notifyOnSearch(query);
        }
        String ftsQuery = buildFtsQuery(q);
        String sql = """
            SELECT n.* FROM notes n
            JOIN notes_fts f ON n.rowid = f.rowid
            WHERE n.deleted = 0
              AND notes_fts MATCH ?
            ORDER BY n.updated_at DESC
            LIMIT ? OFFSET ?;
            """;

        List<Note> notes = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ftsQuery);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                try {
                    notes.add(reconstructNote(rs));
                } catch (StorageException ignored) {
                    logger.warn("Skipping note with invalid integrity during search: {}", rs.getString("id"));
                }
            }
            logger.debug("Search for '{}' returned {} results", query, notes.size());
            return notes;
        } catch (SQLException e) {
            throw new StorageException("Search failed: " + e.getMessage(), e);
        }
    }

    private String buildFtsQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }

        String normalized = query.replaceAll("[^\\w\\s]", " ").trim();
        String[] tokens = normalized.split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" OR ");
            }
            builder.append(token).append("*");
        }

        return builder.toString();
    }

    /**
     * Reconstruct a Note object from a database ResultSet, decrypting the body.
     */
    private Note reconstructNote(ResultSet rs) throws SQLException, StorageException {
        try {
            String id = rs.getString("id");
            String title = rs.getString("title");
            byte[] encryptedBody = rs.getBytes("body");
            String bodyDecrypted = encryptionManager.decrypt(encryptedBody);
            List<String> tags = parseTags(rs.getString("tags"));
            String notebook = rs.getString("notebook");
            Instant createdAt = Instant.ofEpochSecond(rs.getLong("created_at"));
            Instant updatedAt = Instant.ofEpochSecond(rs.getLong("updated_at"));
            int version = rs.getInt("version");
            boolean deleted = rs.getBoolean("deleted");
            byte[] hmac = rs.getBytes("hmac");

            if (hmac == null || !encryptionManager.verifyHMAC(id, bodyDecrypted, hmac)) {
                throw new StorageException("Note integrity verification failed for id: " + id);
            }

            return new Note(id, title, bodyDecrypted, tags, notebook, createdAt, updatedAt, version, deleted, hmac);
        } catch (EncryptionException e) {
            throw new StorageException("Failed to decrypt note: " + e.getMessage(), e);
        }
    }

    /**
     * Parse comma-separated tags string into a list.
     */
    private List<String> parseTags(String tagsStr) {
        if (tagsStr == null || tagsStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(tagsStr.split(","));
    }

    /**
     * Update FTS5 index for full-text search.
     */
    private void updateFtsIndex(String noteId, String title, String body, List<String> tags, String notebook) throws SQLException {
        String deleteOld = "DELETE FROM notes_fts WHERE rowid = (SELECT rowid FROM notes WHERE id = ?);";
        String insertNew = "INSERT INTO notes_fts (rowid, title, body_snippet, tags, notebook) VALUES ((SELECT rowid FROM notes WHERE id = ?), ?, ?, ?, ?);";

        try (PreparedStatement deleteStmt = connection.prepareStatement(deleteOld)) {
            deleteStmt.setString(1, noteId);
            deleteStmt.executeUpdate();
        }

        try (PreparedStatement insertStmt = connection.prepareStatement(insertNew)) {
            insertStmt.setString(1, noteId);
            insertStmt.setString(2, title);
            insertStmt.setString(3, body.substring(0, Math.min(200, body.length()))); // snippet
            insertStmt.setString(4, String.join(" ", tags != null ? tags : new ArrayList<>()));
            insertStmt.setString(5, notebook != null ? notebook : "default");
            insertStmt.executeUpdate();
        }
    }

    /**
     * Close the database connection.
     */
    public void close() throws StorageException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            throw new StorageException("Failed to close database: " + e.getMessage(), e);
        }
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * Import a full Note preserving metadata. Encrypts body with current root key
     * and writes the row directly. Used by import service.
     */
    @Override
    public boolean purge(String id) throws StorageException {
        String purgeSql = "DELETE FROM notes WHERE id = ?;";
        try (PreparedStatement pstmt = connection.prepareStatement(purgeSql)) {
            pstmt.setString(1, id);
            int rowsAffected = pstmt.executeUpdate();
            connection.commit();
            logger.info("Note purged: {}", id);
            return rowsAffected > 0;
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException rollbackEx) { logger.error("Rollback failed", rollbackEx); }
            throw new StorageException("Failed to purge note: " + e.getMessage(), e);
        }
    }

    @Override
    public int purgeDeletedNotes() throws StorageException {
        String purgeSql = "DELETE FROM notes WHERE deleted = 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(purgeSql)) {
            int rowsAffected = pstmt.executeUpdate();
            connection.commit();
            logger.info("Purged {} deleted notes", rowsAffected);
            return rowsAffected;
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException rollbackEx) { logger.error("Rollback failed", rollbackEx); }
            throw new StorageException("Failed to purge deleted notes: " + e.getMessage(), e);
        }
    }

    public void importNoteFull(Note note) throws StorageException {
        try {
            byte[] encryptedBody = encryptionManager.encrypt(note.getBody());
            byte[] hmac = encryptionManager.computeHMAC(note.getId(), note.getBody());

            String insertNotesSql = "INSERT OR REPLACE INTO notes (id, title, body, tags, notebook, created_at, updated_at, version, deleted, hmac) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

            try (PreparedStatement pstmt = connection.prepareStatement(insertNotesSql)) {
                pstmt.setString(1, note.getId());
                pstmt.setString(2, note.getTitle());
                pstmt.setBytes(3, encryptedBody);
                pstmt.setString(4, String.join(",", note.getTags()));
                pstmt.setString(5, note.getNotebook() != null ? note.getNotebook() : "default");
                pstmt.setLong(6, note.getCreatedAt().getEpochSecond());
                pstmt.setLong(7, note.getUpdatedAt().getEpochSecond());
                pstmt.setInt(8, note.getVersion());
                pstmt.setBoolean(9, note.isDeleted());
                pstmt.setBytes(10, hmac);
                pstmt.executeUpdate();
            }

            updateFtsIndex(note.getId(), note.getTitle(), note.getBody(), note.getTags(), note.getNotebook());
            connection.commit();
            logger.info("Imported note: {}", note.getId());
        } catch (EncryptionException e) {
            try { connection.rollback(); } catch (SQLException ex) { logger.error("Rollback failed", ex); }
            throw new StorageException("Encryption failed during import: " + e.getMessage(), e);
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { logger.error("Rollback failed", ex); }
            throw new StorageException("Failed to import note: " + e.getMessage(), e);
        }
    }
}
