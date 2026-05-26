package com.astraNotes.storage;

import com.astraNotes.model.Note;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface defining CRUD and search operations for notes.
 * Implementations handle storage details and encryption delegation.
 */
public interface NoteRepository {
    /**
     * Create a new note and return its unique ID.
     * REQ-1: Create note with title, body, tags, notebook
     */
    String create(String title, String body, List<String> tags, String notebook) throws StorageException;

    /**
     * Retrieve a note by ID. Returns empty if not found or if soft-deleted.
     * REQ-2: Read note by ID with decrypted content
     */
    Optional<Note> get(String id) throws StorageException;

    /**
     * Update an existing note's content and metadata.
     * REQ-3: Update note with versioning and timestamp
     */
    boolean update(String id, String title, String body, List<String> tags) throws StorageException;

    /**
     * Soft-delete a note (mark deleted flag).
     * REQ-4: Soft-delete note
     */
    boolean delete(String id) throws StorageException;

    /**
     * List notes with pagination.
     * REQ-6: Offline operation (no network required)
     */
    List<Note> list(int offset, int limit) throws StorageException;

    /**
     * Search notes by title, body, or tags.
     * REQ-5: Search notes efficiently
     */
    List<Note> search(String query, int offset, int limit) throws StorageException;
}
