package com.astraNotes.storage;

import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.io.ExportImportService;
import com.astraNotes.model.Note;
import com.astraNotes.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class SQLiteNoteStorageIntegrationTest {
    private static final String DB_PATH = "test-astra.db";
    private EncryptionManager encryptionManager;
    private SQLiteNoteStorage storage;
    private ExportImportService exportImportService;

    @Before
    public void setUp() throws Exception {
        encryptionManager = new EncryptionManager();
        assertTrue(encryptionManager.unlock("test-password"));
        PluginManager pm = new PluginManager();
        storage = new SQLiteNoteStorage(DB_PATH, encryptionManager, pm);
        storage.initialize();
        exportImportService = new ExportImportService(storage, encryptionManager);
    }

    @After
    public void tearDown() throws Exception {
        try { storage.close(); } catch (Exception e) { }
        Files.deleteIfExists(new File(DB_PATH).toPath());
    }

    @Test
    public void testCreateReadUpdateDeleteFlow() throws Exception {
        String id = storage.create("Title A", "Body A", List.of("tag1","tag2"), "nb1");
        assertNotNull(id);

        Optional<Note> opt = storage.get(id);
        assertTrue(opt.isPresent());
        Note n = opt.get();
        assertEquals("Title A", n.getTitle());
        assertEquals("Body A", n.getBody());
        assertFalse(n.isDeleted());

        boolean updated = storage.update(id, "Title A2", "Body A2", List.of("tag3"));
        assertTrue(updated);

        Optional<Note> opt2 = storage.get(id);
        assertTrue(opt2.isPresent());
        Note n2 = opt2.get();
        assertEquals("Title A2", n2.getTitle());
        assertEquals("Body A2", n2.getBody());
        assertEquals(2, n2.getVersion());

        boolean deleted = storage.delete(id);
        assertTrue(deleted);

        Optional<Note> opt3 = storage.get(id);
        assertFalse(opt3.isPresent());
    }

    @Test
    public void testSearchAndPagination() throws Exception {
        storage.create("SearchOne", "the quick brown fox", List.of("fox"), "nb");
        storage.create("SearchTwo", "lazy dog jumps", List.of("dog"), "nb");
        storage.create("SearchThree", "brown dog and fox", List.of("dog","fox"), "nb");

        List<com.astraNotes.model.Note> results = storage.search("brown", 0, 10);
        assertTrue(results.size() >= 2);

        List<com.astraNotes.model.Note> page1 = storage.list(0, 2);
        assertEquals(2, page1.size());
    }

    @Test
    public void testExportImport() throws Exception {
        String id = storage.create("ExportTitle", "Export body content", List.of("e"), "nbx");
        File tmp = File.createTempFile("astra-export", ".json");
        exportImportService.exportNotes(List.of(id), tmp);
        assertTrue(tmp.exists());

        // import into a fresh DB
        storage.close();
        Files.deleteIfExists(new File(DB_PATH).toPath());

        SQLiteNoteStorage storage2 = new SQLiteNoteStorage(DB_PATH, encryptionManager, new PluginManager());
        storage2.initialize();
        ExportImportService importer = new ExportImportService(storage2, encryptionManager);
        importer.importFromFile(tmp, false);

        List<com.astraNotes.model.Note> all = storage2.list(0, 100);
        assertTrue(all.stream().anyMatch(x -> "ExportTitle".equals(x.getTitle())));

        storage2.close();
        tmp.delete();
    }

    @Test
    public void testPurgeDeletedNote() throws Exception {
        String id = storage.create("PurgeTitle", "Purge body", List.of("p"), "nbp");
        assertTrue(storage.delete(id));
        assertFalse(storage.get(id).isPresent());

        assertTrue(storage.purge(id));
        List<com.astraNotes.model.Note> all = storage.list(0, 10);
        assertTrue(all.isEmpty());
    }

    @Test
    public void testPurgeDeletedNotesBatch() throws Exception {
        String id1 = storage.create("Purge1", "Body1", List.of("p"), "nbp");
        String id2 = storage.create("Purge2", "Body2", List.of("p"), "nbp");
        assertTrue(storage.delete(id1));
        assertTrue(storage.delete(id2));

        int purged = storage.purgeDeletedNotes();
        assertEquals(2, purged);
        List<com.astraNotes.model.Note> all = storage.list(0, 10);
        assertTrue(all.isEmpty());
    }

    @Test
    public void testHmacIntegrityVerification() throws Exception {
        String id = storage.create("TamperTitle", "Tamper body", List.of("t"), "nbt");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             PreparedStatement pstmt = conn.prepareStatement("UPDATE notes SET hmac = x'0000000000000000000000000000000000000000000000000000000000000000' WHERE id = ?;")) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        }

        try {
            storage.get(id);
            fail("Expected StorageException because HMAC integrity failed");
        } catch (StorageException e) {
            assertTrue(e.getMessage().contains("integrity"));
        }
    }

    @Test
    public void testPluginStatePersistence() {
        PluginManager pm = storage.getPluginManager();
        assertNotNull(pm);
        boolean saved = pm.savePluginState("test-plugin", "lastSearch", "keyword");
        assertTrue(saved);

        Optional<String> state = pm.getPluginState("test-plugin", "lastSearch");
        assertTrue(state.isPresent());
        assertEquals("keyword", state.get());
    }
}
