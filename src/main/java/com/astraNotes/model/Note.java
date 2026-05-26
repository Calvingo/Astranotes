package com.astraNotes.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core domain model representing a markdown note with metadata,
 * versioning, encryption markers, and soft-delete support.
 * REQ-1: Create note / REQ-2: Read note / REQ-3: Update note
 */
public class Note {
    private String id;
    private String title;
    private String body;
    private List<String> tags;
    private String notebook;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
    private boolean deleted;
    private byte[] hmac;

    // Constructor for new notes
    public Note(String title, String body, List<String> tags, String notebook) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.body = body;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.notebook = notebook != null ? notebook : "default";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = 1;
        this.deleted = false;
        this.hmac = new byte[0];
    }

    // Full constructor for reconstruction from storage
    public Note(String id, String title, String body, List<String> tags, String notebook,
                Instant createdAt, Instant updatedAt, int version, boolean deleted, byte[] hmac) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.notebook = notebook;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.deleted = deleted;
        this.hmac = hmac;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public List<String> getTags() { return new ArrayList<>(tags); }
    public String getNotebook() { return notebook; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
    public boolean isDeleted() { return deleted; }
    public byte[] getHmac() { return hmac; }

    // Setters for updates
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
        this.version++;
    }

    public void setBody(String body) {
        this.body = body;
        this.updatedAt = Instant.now();
        this.version++;
    }

    public void setTags(List<String> tags) {
        this.tags = new ArrayList<>(tags);
        this.updatedAt = Instant.now();
        this.version++;
    }

    public void setHmac(byte[] hmac) {
        this.hmac = hmac;
    }

    public void markDeleted() {
        this.deleted = true;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return String.format("Note{id='%s', title='%s', version=%d, notebook='%s', tags=%s, deleted=%b}",
                id, title, version, notebook, tags, deleted);
    }
}
