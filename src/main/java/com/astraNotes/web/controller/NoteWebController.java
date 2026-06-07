package com.astraNotes.web.controller;

import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.io.ExportImportService;
import com.astraNotes.model.Note;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.web.dto.NoteForm;
import com.astraNotes.web.service.AuthService;
import com.astraNotes.web.service.DemoUser;
import com.astraNotes.web.service.NoteService;
import com.astraNotes.web.service.NoteServiceException;
import com.astraNotes.web.service.ValidationException;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Controller
public class NoteWebController {
    private final AuthService authService;
    private final NoteService noteService;
    private final SQLiteNoteStorage storage;
    private final EncryptionManager encryptionManager;

    public NoteWebController(AuthService authService, NoteService noteService, SQLiteNoteStorage storage, EncryptionManager encryptionManager) {
        this.authService = authService;
        this.noteService = noteService;
        this.storage = storage;
        this.encryptionManager = encryptionManager;
    }

    @GetMapping({"/", "/notes"})
    public String notes(@RequestParam(value = "q", required = false) String query, HttpSession session, Model model) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        List<Note> notes = noteService.search(user.id(), query);
        model.addAttribute("notes", notes);
        model.addAttribute("selectedNote", notes.isEmpty() ? null : notes.get(0));
        if (!notes.isEmpty()) {
            addPermissionModel(model, user.id(), notes.get(0).getId());
        }
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("noteForm", new NoteForm());
        model.addAttribute("currentUser", user);
        model.addAttribute("activePage", "notes");
        return "notes";
    }

    @GetMapping("/notes/new")
    public String newNote(HttpSession session, Model model) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("noteForm", new NoteForm());
        model.addAttribute("currentUser", user);
        model.addAttribute("activePage", "notes");
        return "note-form";
    }

    @PostMapping("/notes")
    public String createNote(@ModelAttribute NoteForm noteForm, HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            String id = noteService.create(user.id(), noteForm);
            redirectAttributes.addFlashAttribute("message", "Note created.");
            return "redirect:/notes/" + id;
        } catch (ValidationException | NoteServiceException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("noteForm", noteForm);
            model.addAttribute("currentUser", user);
            model.addAttribute("activePage", "notes");
            return "note-form";
        }
    }

    @GetMapping("/notes/{id}")
    public String viewNote(@PathVariable String id, @RequestParam(value = "q", required = false) String query, HttpSession session, Model model) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        Note selected = noteService.get(user.id(), id).orElse(null);
        model.addAttribute("notes", noteService.search(user.id(), query));
        model.addAttribute("selectedNote", selected);
        if (selected != null) {
            addPermissionModel(model, user.id(), selected.getId());
        }
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("noteForm", new NoteForm());
        model.addAttribute("currentUser", user);
        model.addAttribute("activePage", "notes");
        if (selected == null) {
            model.addAttribute("error", "Note not found.");
        }
        return "notes";
    }

    @GetMapping("/notes/{id}/edit")
    public String editNote(@PathVariable String id, HttpSession session, Model model) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        Note note = noteService.get(user.id(), id).orElse(null);
        if (note == null || !noteService.isOwner(user.id(), id)) {
            model.addAttribute("error", "Only the note owner can edit this note.");
            model.addAttribute("notes", noteService.listNotes(user.id()));
            model.addAttribute("currentUser", user);
            model.addAttribute("activePage", "notes");
            return "notes";
        }
        model.addAttribute("noteId", id);
        model.addAttribute("noteForm", noteService.toForm(note));
        model.addAttribute("currentUser", user);
        model.addAttribute("activePage", "notes");
        return "note-form";
    }

    @PostMapping("/notes/{id}")
    public String updateNote(@PathVariable String id, @ModelAttribute NoteForm noteForm, HttpSession session, RedirectAttributes redirectAttributes, Model model) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            noteService.update(user.id(), id, noteForm);
            redirectAttributes.addFlashAttribute("message", "Note updated.");
            return "redirect:/notes/" + id;
        } catch (ValidationException | NoteServiceException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("noteId", id);
            model.addAttribute("noteForm", noteForm);
            model.addAttribute("currentUser", user);
            model.addAttribute("activePage", "notes");
            return "note-form";
        }
    }

    @PostMapping("/notes/{id}/delete")
    public String deleteNote(@PathVariable String id, HttpSession session, RedirectAttributes redirectAttributes) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            noteService.delete(user.id(), id);
            redirectAttributes.addFlashAttribute("message", "Note deleted.");
        } catch (ValidationException | NoteServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notes";
    }

    @GetMapping("/export")
    public void exportNotes(HttpSession session, HttpServletResponse response) throws IOException {
        DemoUser user = currentUser(session);
        if (user == null) {
            response.sendRedirect("/login");
            return;
        }
        try {
            List<String> ids = noteService.listNotes(user.id()).stream().map(Note::getId).toList();
            File out = File.createTempFile("astranotes-export", ".json");
            new ExportImportService(storage, encryptionManager).exportNotes(ids, out);
            response.setContentType("application/json");
            response.setHeader("Content-Disposition", "attachment; filename=astranotes-export.json");
            Files.copy(out.toPath(), response.getOutputStream());
            Files.deleteIfExists(out.toPath());
        } catch (Exception e) {
            response.sendError(500, "Export failed");
        }
    }

    @PostMapping("/import")
    public String importNotes(@RequestParam("file") MultipartFile file, HttpSession session, RedirectAttributes redirectAttributes) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Choose an export JSON file first.");
            return "redirect:/settings";
        }
        try {
            File in = File.createTempFile("astranotes-import", ".json");
            file.transferTo(in);
            int imported = new ExportImportService(storage, encryptionManager).importFromFile(in, true).size();
            Files.deleteIfExists(in.toPath());
            redirectAttributes.addFlashAttribute("message", "Imported " + imported + " note(s).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Import failed: " + e.getMessage());
        }
        return "redirect:/notes";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("activePage", "profile");
        model.addAttribute("currentUser", user);
        model.addAttribute("noteCount", noteService.listNotes(user.id()).size());
        return "profile";
    }

    @GetMapping("/settings")
    public String settings(HttpSession session, Model model) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("activePage", "settings");
        model.addAttribute("currentUser", user);
        model.addAttribute("encrypted", encryptionManager.isUnlocked());
        return "settings";
    }

    @PostMapping("/notes/{id}/share")
    public String shareNote(@PathVariable String id, @RequestParam String targetUserId, HttpSession session, RedirectAttributes redirectAttributes) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (authService.findById(targetUserId).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Unknown user.");
            return "redirect:/notes/" + id;
        }
        try {
            noteService.share(user.id(), id, targetUserId);
            redirectAttributes.addFlashAttribute("message", "Note shared with " + authService.findById(targetUserId).get().displayName() + ".");
        } catch (ValidationException | NoteServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notes/" + id;
    }

    @PostMapping("/notes/{id}/share/remove")
    public String removeShare(@PathVariable String id, @RequestParam String targetUserId, HttpSession session, RedirectAttributes redirectAttributes) {
        DemoUser user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            noteService.removeShare(user.id(), id, targetUserId);
            redirectAttributes.addFlashAttribute("message", "Sharing removed.");
        } catch (NoteServiceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notes/" + id;
    }

    private DemoUser currentUser(HttpSession session) {
        Object userId = session.getAttribute(AuthService.SESSION_USER_ID);
        if (userId == null) {
            return null;
        }
        return authService.findById(userId.toString()).orElse(null);
    }

    private void addPermissionModel(Model model, String userId, String noteId) {
        boolean owner = noteService.isOwner(userId, noteId);
        model.addAttribute("isOwner", owner);
        model.addAttribute("sharedUsers", owner ? noteService.sharedUsers(userId, noteId) : List.of());
        model.addAttribute("shareTargets", authService.users().stream()
                .filter(user -> !user.id().equals(userId))
                .toList());
    }
}
