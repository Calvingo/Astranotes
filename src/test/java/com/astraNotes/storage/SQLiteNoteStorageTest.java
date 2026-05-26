package com.astraNotes.storage;

import com.astraNotes.model.Note;
import com.astraNotes.encryption.EncryptionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Integration tests for SQLiteNoteStorage.
 * Tests REQ-1 (Create), REQ-2 (Read), REQ-3 (Update), REQ-5 (Search).
 */
public class SQLiteNoteStorageTest {
    private SQLiteNoteStorage storage;
    private EncryptionManager encryptionManager;
    private static final String TEST_DB = "/tmp/astraNotes_test.db";

    @Before
    public void setUp() throws StorageException {
        // Clean up test database
        new File(TEST_DB).delete();

        encryptionManager = new EncryptionManager();
        encryptionManager.unlock("test_password_123");

        storage = new SQLiteNoteStorage(TEST_DB, encryptionManager);
        storage.initialize();
    }

    @After
    public void tearDown() throws StorageException {
        storage.close();
        new File(TEST_DB).delete();
    }

    /**
     * Test Slice 1: Create a note
     * REQ-1: Create note with title, body, tags, notebook
     */
    @Test
    public void testCreateNote() throws StorageException {
        String title = "My First Note";
        String body = "This is the body of my first note.";
        List<String> tags = Arrays.asList("work", "important");
        String notebook = "default";

        String noteId = storage.create(title, body, tags, notebook);

        assertNotNull(noteId);
        assertFalse(noteId.isEmpty());

        // Verify retrieval
        Optional<Note> retrieved = storage.get(noteId);
        assertTrue(retrieved.isPresent());
        assertEquals(title, retrieved.get().getTitle());
        assertEquals(body, retrieved.get().getBody());
        assertEquals(tags, retrieved.get().getTags());
        assertEquals(1, retrieved.get().getVersion());
    }

    /**
     * Test Slice 1 & 2: Create and List notes
     * REQ-1: Create / Verify list shows created note
     */
    @Test
    public void testCreateAndList() throws StorageException {
        String id1 = storage.create("Note 1", "Body 1", List.of("tag1"), "default");
        String id2 = storage.create("Note 2", "Body 2", List.of("tag2"), "work");

        List<Note> notes = storage.list(0, 10);

        assertEquals(2, notes.size());
        assertTrue(notes.stream().anyMatch(n -> n.getId().equals(id1)));
        assertTrue(notes.stream().anyMatch(n -> n.getId().equals(id2)));
    }

    /**
     * Test Slice 2: Retrieve a note by ID
     * REQ-2: Read note and verify decryption
     */
    @Test
    public void testReadNote() throws StorageException {
        String noteId = storage.create("Test Note", "Encrypted content", List.of("test"), "default");

        Optional<Note> note = storage.get(noteId);

        assertTrue(note.isPresent());
        assertEquals("Test Note", note.get().getTitle());
        assertEquals("Encrypted content", note.get().getBody());
        assertFalse(note.get().isDeleted());
    }

    /**
     * Test: Update a note with versioning
     * REQ-3: Update increments version and timestamp
     */
    @Test
    public void testUpdateNote() throws StorageException {
        String noteId = storage.create("Original Title", "Original Body", List.of("v1"), "default");

        Optional<Note> before = storage.get(noteId);
        int originalVersion = before.get().getVersion();

        storage.update(noteId, "Updated Title", "Updated Body", List.of("v2", "updated"));

        Optional<Note> after = storage.get(noteId);
        assertEquals("Updated Title", after.get().getTitle());
        assertEquals("Updated Body", after.get().getBody());
        assertEquals(originalVersion + 1, after.get().getVersion());
    }

    /**
     * Test: Soft-delete a note
     * REQ-4: Delete marks deleted flag, removes from list
     */
    @Test
    public void testSoftDelete() throws StorageException {
        String noteId = storage.create("To Delete", "Body", List.of(), "default");

        // Verify it exists
        Optional<Note> before = storage.get(noteId);
        assertTrue(before.isPresent());

        // Delete
        storage.delete(noteId);

        // Verify it's gone from normal queries
        Optional<Note> after = storage.get(noteId);
        assertFalse(after.isPresent());

        // Verify it doesn't appear in list
        List<Note> notes = storage.list(0, 10);
        assertFalse(notes.stream().anyMatch(n -> n.getId().equals(noteId)));
    }

    /**
     * Test: Search notes
     * REQ-5: Search by title/body finds matching notes
     */
    @Test
    public void testSearch() throws StorageException {
        storage.create("Java Programming", "Learn Java basics", List.of("java", "programming"), "work");
        storage.create("Python Notes", "Python advanced topics", List.of("python"), "work");
        storage.create("JavaScript", "JS frameworks and concepts", List.of("javascript"), "work");

        List<Note> results = storage.search("Java", 0, 10);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(n -> n.getTitle().equals("Java Programming")));
        assertTrue(results.stream().anyMatch(n -> n.getTitle().equals("JavaScript")));
        assertEquals("Java Programming", results.get(0).getTitle());
    }

    @Test
    public void testSearchExcludesDeletedNotes() throws StorageException {
        String noteId = storage.create("DeleteMe", "Should not appear", List.of("temp"), "default");
        storage.delete(noteId);

        List<Note> results = storage.search("appear", 0, 10);
        assertTrue(results.isEmpty());
    }
}
