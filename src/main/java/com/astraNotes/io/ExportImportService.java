package com.astraNotes.io;

import com.astraNotes.model.Note;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.storage.StorageException;
import com.astraNotes.encryption.EncryptionManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.Base64;

/**
 * Export/import service for notes. Exports include encrypted bodies (base64)
 * and metadata; import reconstructs notes into storage while preserving metadata.
 */
public class ExportImportService {
    private static final Logger logger = LoggerFactory.getLogger(ExportImportService.class);
    private final SQLiteNoteStorage storage;
    private final EncryptionManager encryptionManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ExportImportService(SQLiteNoteStorage storage, EncryptionManager encryptionManager) {
        this.storage = storage;
        this.encryptionManager = encryptionManager;
    }

    public static class ExportNote {
        public String id;
        public String title;
        public String bodyEncryptedBase64;
        public List<String> tags;
        public String notebook;
        public long createdAt;
        public long updatedAt;
        public int version;
        public boolean deleted;
        public String hmacBase64;
    }

    public static class ExportBundle {
        public String formatVersion = "astraNotes-export-v1";
        public List<ExportNote> notes = new ArrayList<>();
        public String checksum; // SHA-256 of payload
    }

    /**
     * Export given note IDs to JSON file. Requires encryption manager unlocked.
     */
    public void exportNotes(List<String> noteIds, File outFile) throws IOException, StorageException {
        if (!encryptionManager.isUnlocked()) {
            throw new IllegalStateException("EncryptionManager must be unlocked to export notes");
        }

        ExportBundle bundle = new ExportBundle();

        for (String id : noteIds) {
            Optional<Note> opt = storage.get(id);
            if (opt.isPresent()) {
                Note n = opt.get();
                ExportNote en = new ExportNote();
                en.id = n.getId();
                en.title = n.getTitle();
                byte[] encrypted;
                try {
                    encrypted = encryptionManager.encrypt(n.getBody());
                } catch (com.astraNotes.encryption.EncryptionException ee) {
                    throw new IOException("Failed to encrypt note body for export: " + ee.getMessage(), ee);
                }
                en.bodyEncryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
                en.tags = n.getTags();
                en.notebook = n.getNotebook();
                en.createdAt = n.getCreatedAt().getEpochSecond();
                en.updatedAt = n.getUpdatedAt().getEpochSecond();
                en.version = n.getVersion();
                en.deleted = n.isDeleted();
                en.hmacBase64 = Base64.getEncoder().encodeToString(n.getHmac());
                bundle.notes.add(en);
            }
        }

        String payload = gson.toJson(bundle.notes);
        bundle.checksum = computeChecksum(payload);
        // write with checksum included
        try (Writer w = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
            w.write(gson.toJson(bundle));
        }
        logger.info("Exported {} notes to {}", bundle.notes.size(), outFile.getAbsolutePath());
    }

    private String computeChecksum(String payload) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IOException("Failed to compute checksum", e);
        }
    }

    /**
     * Import notes from bundle. If createNewIds is true, conflicting IDs are re-generated.
     */
    public Map<String, String> importFromFile(File inFile, boolean createNewIds) throws IOException, StorageException {
        if (!encryptionManager.isUnlocked()) {
            throw new IllegalStateException("EncryptionManager must be unlocked to import notes");
        }

        try (Reader r = new InputStreamReader(new FileInputStream(inFile), StandardCharsets.UTF_8)) {
            ExportBundle bundle = gson.fromJson(r, ExportBundle.class);
            // optional: verify checksum
            String recomputed = computeChecksum(gson.toJson(bundle.notes));
            if (bundle.checksum == null || bundle.checksum.isEmpty()) {
                logger.warn("Bundle has no checksum");
            } else if (!bundle.checksum.equals(recomputed)) {
                throw new IOException("Checksum mismatch on import");
            }

            Map<String, String> idMap = new HashMap<>();

            for (ExportNote en : bundle.notes) {
                String idToUse = en.id;
                if (createNewIds) {
                    idToUse = UUID.randomUUID().toString();
                }

                byte[] encrypted = Base64.getDecoder().decode(en.bodyEncryptedBase64);
                String plaintext;
                try {
                    plaintext = encryptionManager.decrypt(encrypted);
                } catch (com.astraNotes.encryption.EncryptionException ee) {
                    throw new StorageException("Failed to decrypt imported note: " + ee.getMessage(), ee);
                }

                // reconstruct Note
                Note note = new Note(idToUse, en.title, plaintext, en.tags, en.notebook,
                        Instant.ofEpochSecond(en.createdAt), Instant.ofEpochSecond(en.updatedAt), en.version, en.deleted,
                        Base64.getDecoder().decode(en.hmacBase64));

                // import into storage (preserves metadata)
                storage.importNoteFull(note);
                idMap.put(en.id, idToUse);
            }

            logger.info("Imported {} notes from {}", bundle.notes.size(), inFile.getAbsolutePath());
            return idMap;
        }
    }
}
