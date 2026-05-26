package com.astraNotes.ui;

import com.astraNotes.model.Note;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Panel displaying a scrollable list of notes with selection support.
 * Implements pagination and click-to-select behavior.
 */
public class NoteListPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(NoteListPanel.class);
    private static final int NOTES_PER_PAGE = 20;

    private final SQLiteNoteStorage storage;
    private final Consumer<String> onNoteSelected;
    private DefaultListModel<NoteListItem> listModel;
    private JList<NoteListItem> noteList;
    private String selectedNoteId;

    public NoteListPanel(SQLiteNoteStorage storage, Consumer<String> onNoteSelected) {
        this.storage = storage;
        this.onNoteSelected = onNoteSelected;
        setLayout(new BorderLayout());
        initializeUI();
    }

    /**
     * Initialize the note list UI.
     */
    private void initializeUI() {
        listModel = new DefaultListModel<>();
        noteList = new JList<>(listModel);
        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        noteList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                NoteListItem selected = noteList.getSelectedValue();
                if (selected != null) {
                    selectedNoteId = selected.getId();
                    onNoteSelected.accept(selectedNoteId);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(noteList);
        add(scrollPane, BorderLayout.CENTER);

        JLabel titleLabel = new JLabel("Notes");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        add(titleLabel, BorderLayout.NORTH);
    }

    /**
     * Refresh the list from storage.
     */
    public void refresh() {
        try {
            listModel.clear();
            selectedNoteId = null;
            List<Note> notes = storage.list(0, NOTES_PER_PAGE);
            for (Note note : notes) {
                listModel.addElement(new NoteListItem(note.getId(), note.getTitle(), note.getVersion()));
            }
            logger.debug("Refreshed note list: {} notes", notes.size());
        } catch (StorageException e) {
            logger.error("Failed to refresh note list: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to load notes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Get the currently selected note ID.
     */
    public String getSelectedNoteId() {
        return selectedNoteId;
    }

    /**
     * Simple model for displaying notes in the list.
     */
    public static class NoteListItem {
        private final String id;
        private final String title;
        private final int version;

        public NoteListItem(String id, String title, int version) {
            this.id = id;
            this.title = title;
            this.version = version;
        }

        public String getId() { return id; }

        @Override
        public String toString() {
            return title + " [v" + version + "]";
        }
    }
}
