package com.astraNotes.web.config;

import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.storage.SQLiteNoteStorage;
import com.astraNotes.storage.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class AstraNotesWebConfig {
    @Bean
    public EncryptionManager encryptionManager(@Value("${astranotes.demo-password}") String demoPassword) {
        EncryptionManager encryptionManager = new EncryptionManager();
        if (!encryptionManager.unlock(demoPassword)) {
            throw new IllegalStateException("Failed to unlock AstraNotes encryption manager");
        }
        return encryptionManager;
    }

    @Bean(destroyMethod = "close")
    public SQLiteNoteStorage noteRepository(
            @Value("${astranotes.db-path}") String dbPath,
            EncryptionManager encryptionManager) throws StorageException {
        File dbFile = new File(dbPath);
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new StorageException("Failed to create database directory: " + parent.getAbsolutePath());
        }
        SQLiteNoteStorage storage = new SQLiteNoteStorage(dbPath, encryptionManager);
        storage.initialize();
        return storage;
    }
}
