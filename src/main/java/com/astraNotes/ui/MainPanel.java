package com.astraNotes.ui;

import com.astraNotes.model.Note;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Main panel containing the note workspace, sidebar, and central area.
 * Coordinates between note list, detail view, and create/edit dialogs.
 */
public class MainPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MainPanel.class);

    private final SQLiteNoteStorage storage;
    private NoteListPanel noteListPanel;
    private NoteDetailPanel noteDetailPanel;
    private NoteCreateDialog createDialog;

    public MainPanel(SQLiteNoteStorage storage) {
        this.storage = storage;
        setLayout(new BorderLayout());
        initializeUI();
    }

    /**
     * Initialize the main UI with sidebar (list) and main area (detail + buttons).
     */
    private void initializeUI() {
        // Left sidebar: note list
        noteListPanel = new NoteListPanel(storage, this::selectNote);
        JScrollPane listScrollPane = new JScrollPane(noteListPanel);
        listScrollPane.setPreferredSize(new Dimension(300, 0));

        // Right main area: note detail
        noteDetailPanel = new NoteDetailPanel();

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newButton = new JButton("New Note");
        newButton.addActionListener(e -> openCreateNoteDialog());
        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> deleteCurrentNote());
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        buttonPanel.add(newButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, noteDetailPanel);
        splitPane.setDividerLocation(300);

        add(splitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        refresh();
    }

    /**
     * Select and display a note in the detail view.
     */
    private void selectNote(String noteId) {
        try {
            Optional<Note> noteOpt = storage.get(noteId);
            if (noteOpt.isPresent()) {
                Note note = noteOpt.get();
                noteDetailPanel.displayNote(note);
                logger.info("Displayed note: {}", noteId);
            }
        } catch (StorageException e) {
            logger.error("Failed to load note: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load note: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Open create note dialog.
     */
    public void openCreateNoteDialog() {
        if (createDialog == null) {
            createDialog = new NoteCreateDialog((JFrame) SwingUtilities.getWindowAncestor(this), storage, this::refresh);
        }
        createDialog.setVisible(true);
    }

    /**
     * Delete the currently selected note.
     */
    private void deleteCurrentNote() {
        String noteId = noteListPanel.getSelectedNoteId();
        if (noteId == null) {
            JOptionPane.showMessageDialog(this, "Please select a note to delete", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Delete this note?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                storage.delete(noteId);
                noteDetailPanel.clear();
                refresh();
                logger.info("Note deleted: {}", noteId);
            } catch (StorageException e) {
                logger.error("Failed to delete note: {}", e.getMessage());
                JOptionPane.showMessageDialog(this, "Failed to delete note: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Refresh the note list.
     */
    public void refresh() {
        noteListPanel.refresh();
    }

    /**
     * Open settings panel.
     */
    public void openSettingsPanel() {
        JDialog settingsDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Settings", true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("AstraNotes Settings");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));

        JLabel dbLabel = new JLabel("Database: " + System.getProperty("user.home") + "/.astraNotes/notes.db");
        panel.add(dbLabel);

        panel.add(Box.createVerticalStrut(10));
        JLabel infoLabel = new JLabel("App: Secure offline-first note-taking");
        panel.add(infoLabel);

        settingsDialog.setContentPane(panel);
        settingsDialog.setSize(400, 200);
        settingsDialog.setLocationRelativeTo((JFrame) SwingUtilities.getWindowAncestor(this));
        settingsDialog.setVisible(true);
    }
}
