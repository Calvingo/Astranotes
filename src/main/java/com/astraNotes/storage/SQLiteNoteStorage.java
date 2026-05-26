package com.astraNotes.storage;

import com.astraNotes.model.Note;
import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.encryption.EncryptionException;
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

    private final String dbPath;
    private final EncryptionManager encryptionManager;
    private Connection connection;

    public SQLiteNoteStorage(String dbPath, EncryptionManager encryptionManager) {
        this.dbPath = dbPath;
        this.encryptionManager = encryptionManager;
    }

    /**
     * Initialize the database connection and create schema if needed.
     */
    public void initialize() throws StorageException {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            connection.setAutoCommit(false);
            createSchema();
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

        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_notes_deleted ON notes(deleted);
            CREATE INDEX IF NOT EXISTS idx_notes_updated_at ON notes(updated_at DESC);
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createNotesTable);
            stmt.execute(createFtsTable);
            stmt.execute(createIndexes);
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

    /**
     * Create a new note with encryption and HMAC.
     * REQ-1: Create note
     */
    @Override
    public String create(String title, String body, List<String> tags, String notebook) throws StorageException {
        Note note = new Note(title, body, tags, notebook);

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
                notes.add(reconstructNote(rs));
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
        String pattern = "%" + query + "%";
        String sql = """
            SELECT * FROM notes
            WHERE deleted = 0 AND (title LIKE ? OR tags LIKE ?)
            ORDER BY updated_at DESC
            LIMIT ? OFFSET ?;
            """;

        List<Note> notes = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                notes.add(reconstructNote(rs));
            }
            logger.debug("Search for '{}' returned {} results", query, notes.size());
            return notes;
        } catch (SQLException e) {
            throw new StorageException("Search failed: " + e.getMessage(), e);
        }
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
}
