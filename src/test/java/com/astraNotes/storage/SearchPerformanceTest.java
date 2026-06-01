package com.astraNotes.storage;

import com.astraNotes.encryption.EncryptionManager;
import com.astraNotes.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class SearchPerformanceTest {
    private static final Logger logger = LoggerFactory.getLogger(SearchPerformanceTest.class);
    private static final int NUM_NOTES = 2000; // reduced for quicker feedback
    private static final int WARMUP = 3;
    private static final int RUNS = 5;

    private SQLiteNoteStorage storage;
    private EncryptionManager encryptionManager;
    private String dbPath;

    @Before
    public void setUp() throws Exception {
        encryptionManager = new EncryptionManager();
        encryptionManager.unlock("perf-password");
        dbPath = System.getProperty("java.io.tmpdir") + File.separator + "astra_perf.db";
        // ensure clean
        Files.deleteIfExists(new File(dbPath).toPath());
        storage = new SQLiteNoteStorage(dbPath, encryptionManager, new PluginManager());
        storage.initialize();

        // populate dataset
        logger.info("Populating {} notes (this may take a while)", NUM_NOTES);
        for (int i = 0; i < NUM_NOTES; i++) {
            String title = "Note " + i;
            // include keyword in some notes
            String body = (i % 3 == 0) ? "performance test keyword quick brown fox" : "lorem ipsum dolor sit amet";
            List<String> tags = new ArrayList<>();
            tags.add("tag" + (i % 10));
            storage.create(title, body, tags, "nb");
        }
        logger.info("Population complete");
    }

    @After
    public void tearDown() throws Exception {
        try { storage.close(); } catch (Exception e) { }
        Files.deleteIfExists(new File(dbPath).toPath());
    }

    @Test
    public void benchmarkSearch() throws Exception {
        String query = "keyword";
        // warmup
        for (int i = 0; i < WARMUP; i++) {
            storage.search(query, 0, 50);
        }

        long total = 0;
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            storage.search(query, 0, 50);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            logger.info("Run {}: {} ms", i+1, elapsedMs);
            total += elapsedMs;
        }
        double avg = ((double) total) / RUNS;
        logger.info("Average search time over {} runs: {} ms", RUNS, avg);
        System.out.println("SEARCH_BENCHMARK_AVG_MS=" + avg);
    }
}
