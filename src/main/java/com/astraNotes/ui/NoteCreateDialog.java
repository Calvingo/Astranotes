package com.astraNotes.ui;

import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

/**
 * Dialog for creating a new note.
 * REQ-1: Create note with title, body, tags, and notebook.
 */
public class NoteCreateDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(NoteCreateDialog.class);

    private final SQLiteNoteStorage storage;
    private final Runnable onSuccess;

    private JTextField titleField;
    private JTextArea bodyArea;
    private JTextField tagsField;
    private JComboBox<String> notebookCombo;

    public NoteCreateDialog(JFrame parent, SQLiteNoteStorage storage, Runnable onSuccess) {
        super(parent, "Create New Note", true);
        this.storage = storage;
        this.onSuccess = onSuccess;
        setSize(600, 500);
        setLocationRelativeTo(parent);
        initializeUI();
    }

    /**
     * Initialize the create dialog UI.
     */
    private void initializeUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        titleField = new JTextField(20);
        panel.add(titleField, gbc);

        // Notebook
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Notebook:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        notebookCombo = new JComboBox<>(new String[]{"default", "work", "personal", "ideas"});
        notebookCombo.setEditable(true);
        panel.add(notebookCombo, gbc);

        // Tags
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Tags (comma-separated):"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        tagsField = new JTextField(20);
        panel.add(tagsField, gbc);

        // Body
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        gbc.weighty = 0;
        panel.add(new JLabel("Body:"), gbc);

        gbc.gridx = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        bodyArea = new JTextArea(10, 50);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(bodyArea), gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveNote());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        setContentPane(new JPanel(new BorderLayout()));
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Save the note to storage.
     */
    private void saveNote() {
        String title = titleField.getText().trim();
        String body = bodyArea.getText().trim();
        String tagsStr = tagsField.getText().trim();
        String notebook = (String) notebookCombo.getSelectedItem();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title is required", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (body.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Body cannot be empty", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<String> tags = tagsStr.isEmpty() ? List.of() : Arrays.asList(tagsStr.split(","));
            String noteId = storage.create(title, body, tags, notebook);
            logger.info("Note created: {}", noteId);
            JOptionPane.showMessageDialog(this, "Note created successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
            setVisible(false);
            onSuccess.run();
        } catch (StorageException e) {
            logger.error("Failed to create note: {}", e.getMessage());
            JOptionPane.showMessageDialog(this, "Failed to create note: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Clear form fields.
     */
    private void clearFields() {
        titleField.setText("");
        bodyArea.setText("");
        tagsField.setText("");
        notebookCombo.setSelectedIndex(0);
    }
}
