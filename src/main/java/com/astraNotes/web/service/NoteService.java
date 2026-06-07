package com.astraNotes.web.service;

import com.astraNotes.model.Note;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.storage.StorageException;
import com.astraNotes.web.dto.NoteForm;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class NoteService {
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_BODY_LENGTH = 50_000;
    private static final int MAX_TAGS = 12;

    private final SQLiteNoteStorage repository;

    public NoteService(SQLiteNoteStorage repository) {
        this.repository = repository;
    }

    public synchronized List<Note> listNotes(String userId) {
        try {
            return repository.listForUser(userId, 0, DEFAULT_LIMIT);
        } catch (StorageException e) {
            throw new NoteServiceException("Could not list notes", e);
        }
    }

    public synchronized List<Note> search(String userId, String query) {
        if (query == null || query.isBlank()) {
            return listNotes(userId);
        }
        try {
            return repository.searchForUser(userId, query.trim(), 0, DEFAULT_LIMIT);
        } catch (StorageException e) {
            throw new NoteServiceException("Could not search notes", e);
        }
    }

    public synchronized Optional<Note> get(String userId, String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        try {
            return repository.getForUser(userId, id);
        } catch (StorageException e) {
            throw new NoteServiceException("Could not load note", e);
        }
    }

    public synchronized String create(String userId, NoteForm form) {
        ValidatedNote validated = validate(form);
        try {
            return repository.createForUser(userId, validated.title(), validated.body(), validated.tags(), validated.notebook());
        } catch (StorageException e) {
            throw new NoteServiceException("Could not create note", e);
        }
    }

    public synchronized boolean update(String userId, String id, NoteForm form) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Missing note id.");
        }
        ValidatedNote validated = validate(form);
        try {
            boolean updated = repository.updateForOwner(userId, id, validated.title(), validated.body(), validated.tags());
            if (!updated) {
                throw new ValidationException("Only the note owner can edit this note.");
            }
            return true;
        } catch (StorageException e) {
            throw new NoteServiceException("Could not update note", e);
        }
    }

    public synchronized boolean delete(String userId, String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Missing note id.");
        }
        try {
            boolean deleted = repository.deleteForOwner(userId, id);
            if (!deleted) {
                throw new ValidationException("Only the note owner can delete this note.");
            }
            return true;
        } catch (StorageException e) {
            throw new NoteServiceException("Could not delete note", e);
        }
    }

    public synchronized boolean isOwner(String userId, String noteId) {
        try {
            return repository.isOwner(userId, noteId);
        } catch (StorageException e) {
            throw new NoteServiceException("Could not check note owner", e);
        }
    }

    public synchronized void share(String ownerId, String noteId, String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new ValidationException("Choose a user to share with.");
        }
        try {
            repository.shareWithUser(ownerId, noteId, targetUserId);
        } catch (StorageException e) {
            throw new NoteServiceException(e.getMessage(), e);
        }
    }

    public synchronized void removeShare(String ownerId, String noteId, String targetUserId) {
        try {
            repository.removeShare(ownerId, noteId, targetUserId);
        } catch (StorageException e) {
            throw new NoteServiceException(e.getMessage(), e);
        }
    }

    public synchronized List<String> sharedUsers(String ownerId, String noteId) {
        try {
            return repository.listSharedUsers(ownerId, noteId);
        } catch (StorageException e) {
            throw new NoteServiceException("Could not list shared users", e);
        }
    }

    public NoteForm toForm(Note note) {
        NoteForm form = new NoteForm();
        form.setTitle(note.getTitle());
        form.setBody(note.getBody());
        form.setNotebook(note.getNotebook());
        form.setTags(String.join(", ", note.getTags()));
        return form;
    }

    private ValidatedNote validate(NoteForm form) {
        if (form == null) {
            throw new ValidationException("Note form is required.");
        }
        String title = normalizeRequired(form.getTitle(), "Title");
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ValidationException("Title must be " + MAX_TITLE_LENGTH + " characters or fewer.");
        }

        String body = form.getBody() == null ? "" : form.getBody().trim();
        if (body.length() > MAX_BODY_LENGTH) {
            throw new ValidationException("Body is too large for the demo application.");
        }

        String notebook = form.getNotebook() == null || form.getNotebook().isBlank()
                ? "default"
                : form.getNotebook().trim();

        List<String> tags = parseTags(form.getTags());
        if (tags.size() > MAX_TAGS) {
            throw new ValidationException("Use " + MAX_TAGS + " tags or fewer.");
        }

        return new ValidatedNote(title, body, tags, notebook);
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(label + " is required.");
        }
        return value.trim();
    }

    private List<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }

    private record ValidatedNote(String title, String body, List<String> tags, String notebook) {
    }
}
