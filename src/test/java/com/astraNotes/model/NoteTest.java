package com.astraNotes.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Note domain model.
 * Validates creation, versioning, and metadata management.
 */
public class NoteTest {

    @Test
    public void testCreateNote() {
        Note note = new Note("Test Title", "Test Body", null, null);

        assertNotNull(note.getId());
        assertEquals("Test Title", note.getTitle());
        assertEquals("Test Body", note.getBody());
        assertEquals(1, note.getVersion());
        assertFalse(note.isDeleted());
    }

    @Test
    public void testUpdateTitle() {
        Note note = new Note("Original", "Body", null, null);
        int originalVersion = note.getVersion();

        note.setTitle("Updated");

        assertEquals("Updated", note.getTitle());
        assertEquals(originalVersion + 1, note.getVersion());
    }

    @Test
    public void testMarkDeleted() {
        Note note = new Note("Title", "Body", null, null);
        assertFalse(note.isDeleted());

        note.markDeleted();

        assertTrue(note.isDeleted());
    }

    @Test
    public void testDefaultNotebook() {
        Note note = new Note("Title", "Body", null, null);
        assertEquals("default", note.getNotebook());
    }
}
