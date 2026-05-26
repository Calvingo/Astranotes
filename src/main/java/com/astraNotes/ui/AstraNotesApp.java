package com.astraNotes.ui;

import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Main application entry point and shell for AstraNotes desktop client.
 * Manages window initialization, menu structure, and coordination between UI components.
 */
public class AstraNotesApp {
    private static final Logger logger = LoggerFactory.getLogger(AstraNotesApp.class);
    private static final String APP_TITLE = "AstraNotes";
    private static final String DB_PATH = System.getProperty("user.home") + "/.astraNotes/notes.db";

    private JFrame mainWindow;
    private EncryptionManager encryptionManager;
    private SQLiteNoteStorage storage;
    private MainPanel mainPanel;

    public AstraNotesApp() {
        initializeEncryption();
        initializeStorage();
    }

    /**
     * Initialize encryption manager with default unlock (demo mode).
     */
    private void initializeEncryption() {
        encryptionManager = new EncryptionManager();
        // Demo mode: unlock with fixed password
        if (!encryptionManager.unlock("astraNotes123")) {
            logger.error("Failed to unlock encryption manager");
        }
    }

    /**
     * Initialize SQLite storage layer.
     */
    private void initializeStorage() {
        try {
            File dbDir = new File(System.getProperty("user.home"), ".astraNotes");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            storage = new SQLiteNoteStorage(DB_PATH, encryptionManager);
            storage.initialize();
            logger.info("Storage initialized at {}", DB_PATH);
        } catch (StorageException e) {
            logger.error("Failed to initialize storage: {}", e.getMessage(), e);
            showError("Storage Initialization Failed", e.getMessage());
        }
    }

    /**
     * Build and show the main application window.
     */
    public void launch() {
        SwingUtilities.invokeLater(() -> {
            mainWindow = new JFrame(APP_TITLE);
            mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainWindow.setSize(1200, 800);
            mainWindow.setLocationRelativeTo(null);

            mainPanel = new MainPanel(storage);
            mainWindow.setContentPane(mainPanel);

            // Build menu bar
            mainWindow.setJMenuBar(createMenuBar());

            mainWindow.setVisible(true);
            logger.info("Application launched");
        });
    }

    /**
     * Create menu bar with File, Edit, View, and Help menus.
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newNoteItem = new JMenuItem("New Note");
        newNoteItem.addActionListener(e -> mainPanel.openCreateNoteDialog());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(newNoteItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(e -> mainPanel.openSettingsPanel());
        editMenu.add(settingsItem);

        // View menu
        JMenu viewMenu = new JMenu("View");
        JMenuItem refreshItem = new JMenuItem("Refresh");
        refreshItem.addActionListener(e -> mainPanel.refresh());
        viewMenu.add(refreshItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    /**
     * Show about dialog.
     */
    private void showAboutDialog() {
        JOptionPane.showMessageDialog(
            mainWindow,
            "AstraNotes v0.1.0\n\n" +
            "Secure offline-first markdown note-taking application.\n" +
            "Built with Java, SQLite, and AES encryption.",
            "About AstraNotes",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Show error dialog.
     */
    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(mainWindow, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Cleanup on shutdown.
     */
    private void shutdown() {
        try {
            if (storage != null) {
                storage.close();
            }
            encryptionManager.lock();
            logger.info("Application shutdown");
        } catch (StorageException e) {
            logger.error("Error during shutdown: {}", e.getMessage());
        }
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.warn("Could not set system look and feel: {}", e.getMessage());
        }

        AstraNotesApp app = new AstraNotesApp();
        app.launch();

        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));
    }
}
