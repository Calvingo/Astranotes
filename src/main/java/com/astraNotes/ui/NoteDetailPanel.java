package com.astraNotes.ui;

import com.astraNotes.model.Note;

import javax.swing.*;
import java.awt.*;

/**
 * Panel displaying detailed view of a selected note (read-only).
 */
public class NoteDetailPanel extends JPanel {
    private JLabel titleLabel;
    private JTextArea bodyArea;
    private JLabel metadataLabel;

    public NoteDetailPanel() {
        setLayout(new BorderLayout());
        initializeUI();
    }

    /**
     * Initialize the detail panel UI.
     */
    private void initializeUI() {
        // Title
        titleLabel = new JLabel("(No note selected)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(titleLabel, BorderLayout.NORTH);

        // Body
        bodyArea = new JTextArea();
        bodyArea.setEditable(false);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(bodyArea);
        add(scrollPane, BorderLayout.CENTER);

        // Metadata footer
        metadataLabel = new JLabel(" ");
        metadataLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        add(metadataLabel, BorderLayout.SOUTH);
    }

    /**
     * Display a note in the detail view.
     */
    public void displayNote(Note note) {
        titleLabel.setText(note.getTitle());
        bodyArea.setText(note.getBody());
        bodyArea.setCaretPosition(0);

        String metadata = String.format("ID: %s | Version: %d | Updated: %s | Notebook: %s",
                note.getId().substring(0, 8) + "...",
                note.getVersion(),
                note.getUpdatedAt(),
                note.getNotebook());
        metadataLabel.setText(metadata);
    }

    /**
     * Clear the detail view.
     */
    public void clear() {
        titleLabel.setText("(No note selected)");
        bodyArea.setText("");
        metadataLabel.setText(" ");
    }
}
