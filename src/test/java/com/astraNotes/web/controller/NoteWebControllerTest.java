package com.astraNotes.web.controller;

import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.web.service.AuthService;
import com.astraNotes.web.service.NoteService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class NoteWebControllerTest {
    private Path dbPath;
    private SQLiteNoteStorage storage;
    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        dbPath = Files.createTempFile("astranotes-controller", ".db");
        EncryptionManager encryptionManager = new EncryptionManager();
        encryptionManager.unlock("controller-test-password");
        storage = new SQLiteNoteStorage(dbPath.toString(), encryptionManager);
        storage.initialize();

        AuthService authService = new AuthService();
        NoteService noteService = new NoteService(storage);
        NoteWebController noteController = new NoteWebController(authService, noteService, storage, encryptionManager);
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController, noteController).build();
    }

    @After
    public void tearDown() throws Exception {
        if (storage != null) {
            storage.close();
        }
        Files.deleteIfExists(dbPath);
    }

    @Test
    public void loggedOutNotesRequestRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/notes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void loginAllowsAccessToNotesWorkspace() throws Exception {
        MockHttpSession session = login("alex", "alex123");

        mockMvc.perform(get("/notes").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("notes"))
                .andExpect(model().attribute("currentUser", hasProperty("id", containsString("alex"))));
    }

    @Test
    public void sharedUserCanReadButCannotOpenEditRoute() throws Exception {
        MockHttpSession alexSession = login("alex", "alex123");

        MvcResult create = mockMvc.perform(post("/notes")
                        .session(alexSession)
                        .param("title", "Controller Shared Note")
                        .param("body", "Shared body")
                        .param("tags", "controller,share")
                        .param("notebook", "course"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String location = create.getResponse().getHeader("Location");
        assertNotNull(location);
        String noteId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/notes/" + noteId + "/share")
                        .session(alexSession)
                        .param("targetUserId", "morgan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes/" + noteId));

        MockHttpSession morganSession = login("morgan", "morgan123");

        mockMvc.perform(get("/notes/" + noteId).session(morganSession))
                .andExpect(status().isOk())
                .andExpect(view().name("notes"))
                .andExpect(model().attribute("selectedNote", hasProperty("title", containsString("Controller Shared Note"))))
                .andExpect(model().attribute("isOwner", false));

        mockMvc.perform(get("/notes/" + noteId + "/edit").session(morganSession))
                .andExpect(status().isOk())
                .andExpect(view().name("notes"))
                .andExpect(model().attribute("error", "Only the note owner can edit this note."));
    }

    @Test
    public void logoutInvalidatesSessionAccess() throws Exception {
        MockHttpSession session = login("alex", "alex123");

        mockMvc.perform(post("/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/notes").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    private MockHttpSession login(String userId, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .param("userId", userId)
                        .param("password", password))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/notes"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession();
    }
}
