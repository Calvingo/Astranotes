package com.astraNotes.web.service;

import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.model.Note;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.web.dto.NoteForm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class NoteServiceTest {
    private Path dbPath;
    private SQLiteNoteStorage storage;
    private NoteService service;

    @Before
    public void setUp() throws Exception {
        dbPath = Files.createTempFile("astranotes-web-service", ".db");
        EncryptionManager encryptionManager = new EncryptionManager();
        assertTrue(encryptionManager.unlock("service-test-password"));
        storage = new SQLiteNoteStorage(dbPath.toString(), encryptionManager);
        storage.initialize();
        service = new NoteService(storage);
    }

    @After
    public void tearDown() throws Exception {
        if (storage != null) {
            storage.close();
        }
        Files.deleteIfExists(dbPath);
    }

    @Test
    public void createRejectsBlankTitle() {
        NoteForm form = new NoteForm();
        form.setTitle("   ");
        form.setBody("Body");

        try {
            service.create("alex", form);
            fail("Expected blank title to be rejected");
        } catch (ValidationException e) {
            assertEquals("Title is required.", e.getMessage());
        }
    }

    @Test
    public void createTrimsTitleAndParsesTags() {
        NoteForm form = validForm("  Release Plan  ");
        form.setTags("demo, class, demo");
        form.setNotebook(" course ");

        String id = service.create("alex", form);
        Note saved = service.get("alex", id).orElseThrow();

        assertEquals("Release Plan", saved.getTitle());
        assertEquals(List.of("demo", "class"), saved.getTags());
        assertEquals("course", saved.getNotebook());
    }

    @Test
    public void ownerCanSeeNoteButOtherUserCannotUntilShared() {
        String id = service.create("alex", validForm("Private Note"));

        assertTrue(service.get("alex", id).isPresent());
        assertTrue(service.get("morgan", id).isEmpty());

        service.share("alex", id, "morgan");

        assertTrue(service.get("morgan", id).isPresent());
        assertEquals(1, service.search("morgan", "Private").size());
    }

    @Test
    public void sharedUserCannotEditOrDeleteOwnersNote() {
        String id = service.create("alex", validForm("Owner Note"));
        service.share("alex", id, "morgan");

        try {
            service.update("morgan", id, validForm("Changed"));
            fail("Expected shared user edit to be rejected");
        } catch (ValidationException e) {
            assertEquals("Only the note owner can edit this note.", e.getMessage());
        }

        try {
            service.delete("morgan", id);
            fail("Expected shared user delete to be rejected");
        } catch (ValidationException e) {
            assertEquals("Only the note owner can delete this note.", e.getMessage());
        }
    }

    @Test
    public void updateRejectsMissingId() {
        try {
            service.update("alex", "", validForm("Valid"));
            fail("Expected missing id to be rejected");
        } catch (ValidationException e) {
            assertEquals("Missing note id.", e.getMessage());
        }
    }

    private NoteForm validForm(String title) {
        NoteForm form = new NoteForm();
        form.setTitle(title);
        form.setBody("Body");
        form.setTags("tag");
        form.setNotebook("default");
        return form;
    }
}
