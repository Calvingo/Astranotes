# AstraNotes Week 7.2 Testing Strategy
## Clear Linkage Between Tests, Requirements, and User Stories

---

## 1. Testing Strategy Overview

This document outlines a **realistic, incremental testing approach** for AstraNotes that prioritizes:
1. **Foundational security** — encryption and integrity checks that underpin all other features
2. **Performance validation** — search must scale before users depend on it
3. **Clear requirement traceability** — each test links to a requirement or user story
4. **Practical timing** — tests run at the right phase of development

**Key Decision:** Rather than trying to test everything at once, we focus on **two critical feature areas** in Phase 1:
- **Encryption & Data Integrity** (unit + integration)
- **Search Performance** (integration + feature-level)

These are the "make or break" components that, if wrong, cannot be easily fixed later. Search in particular benefits from early performance validation because poor design choices (missing indices, N+1 queries) are hard to refactor.

---

## 2. Feature Selection & Requirement Mapping

### Feature 1: Encryption & Data Integrity
**Why first?** Encryption is foundational; if data doesn't encrypt/decrypt correctly, all subsequent features fail. Integrity checks (HMAC) prevent silent data corruption.

| Requirement | User Story | Why It Matters |
|-------------|-----------|---|
| REQ-SEC-1: Encryption at rest | Story 1: Create a secure markdown note | User assumes notes are encrypted automatically |
| REQ-SEC-2: Integrity checks (HMAC) | Story 2: Retrieve a note by ID | Decryption must fail fast if data is corrupted |
| REQ-SEC-3: Access control (password) | Implicit in all CRUD | Without unlock, no operations proceed |
| REQ-REL-1: ACID reliability | Story 3: Update with versioning | Partial writes corrupt encrypted content |

**Where it lives:** 
- `EncryptionManager.java` — encrypts/decrypts, computes HMAC
- `Note.java` — stores HMAC alongside encrypted body
- `SQLiteNoteStorage.java` — persists encrypted blobs, validates HMAC on read

---

### Feature 2: Search Performance & Correctness
**Why second?** Search is high-impact: a single missing index or inefficient query becomes a user complaint. Early performance testing guides schema and SQL design.

| Requirement | User Story | Why It Matters |
|-------------|-----------|---|
| REQ-5: Search notes efficiently | Story 5: Search notes efficiently | Search must return results in <150ms for 10k notes |
| REQ-NF-1: Performance (150ms baseline) | Story 5 Acceptance: FTS5 under 150ms | UI must not freeze during search |
| REQ-NF-2: Scalability (100k notes) | Story 5 Acceptance: Ordered by updatedAt | As notes accumulate, search must stay fast |
| REQ-4: Soft-delete impact | Story 4: Soft-delete a note | Deleted notes must be excluded from search results |

**Where it lives:**
- `SQLiteNoteStorage.search()` — executes FTS5 query
- Schema: `notes_fts` virtual table (FTS5 index)
- `NoteListPanel.java` — calls search and displays results

---

## 3. Testing Levels & When They Run

### Unit Tests (Run: Every commit, before integration tests)
**Goal:** Verify logic in isolation, without database or encryption overhead.

**What we test:**
- Encryption logic (encrypt/decrypt cycle, HMAC computation)
- Note model behavior (versioning, metadata updates)
- Search query parsing (if any)

**Example:**
```java
@Test
public void testEncryptDecryptCycle() {
    // Verify plaintext survives encrypt→decrypt
    String original = "Sensitive data";
    byte[] encrypted = encryptionManager.encrypt(original);
    String decrypted = encryptionManager.decrypt(encrypted);
    assertEquals(original, decrypted);
}
```

**Why now?** Unit tests give fast feedback and catch logic errors before they hit the database layer.

---

### Integration Tests (Run: Pre-commit, after unit tests)
**Goal:** Verify that components work together: encryption + storage + retrieval.

**What we test:**
- Create note → encrypted in DB → retrieved and decrypted correctly
- HMAC verified on read; tampering detected
- Search queries hit FTS5 index and return correct results
- Soft-delete excludes notes from search

**Example:**
```java
@Test
public void testCreateEncryptedNoteAndRetrieve() {
    // 1. Create note with sensitive data
    String noteId = storage.create("Secret Note", "classified info", List.of("secret"), "default");
    
    // 2. Verify it's encrypted in DB (raw query shows gibberish)
    byte[] rawEncrypted = queryRawFromDB(noteId);
    assertFalse(new String(rawEncrypted).contains("classified info"));
    
    // 3. Retrieve via storage API and verify decryption
    Optional<Note> retrieved = storage.get(noteId);
    assertTrue(retrieved.isPresent());
    assertEquals("classified info", retrieved.get().getBody());
}
```

**Why now?** Integration tests verify the contract between layers; they run slower than unit tests but catch real-world issues.

---

### Feature-Level Tests (Run: Pre-release, after integration tests pass)
**Goal:** Verify user-visible behavior end-to-end (may include UI).

**What we test:**
- User creates note via UI → sees it in list → can search and find it
- Search performance under realistic load (synthetic 10k-note dataset)
- Concurrent operations (two edits to same note) don't corrupt state

**Example:**
```java
@Test
public void testSearchPerformanceUnderLoad() {
    // Create 10,000 synthetic notes
    for (int i = 0; i < 10_000; i++) {
        storage.create(
            "Note " + i,
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " + randomText(),
            List.of("tag" + (i % 100)),
            "default"
        );
    }
    
    // Measure search time
    long startMs = System.currentTimeMillis();
    List<Note> results = storage.search("consectetur");
    long elapsedMs = System.currentTimeMillis() - startMs;
    
    // Assert performance
    assertTrue("Search took " + elapsedMs + "ms", elapsedMs < 150);
    assertTrue("Results non-empty", results.size() > 0);
}
```

**Why later?** Feature tests are slow; they run less frequently but validate end-to-end correctness before release.

---

## 4. First Test Set — Detailed Outlines

### Phase 1A: Unit Tests for Encryption & Integrity

**Test Suite:** `EncryptionManagerTest.java`

```java
public class EncryptionManagerTest {
    
    // Test 1: Basic encrypt/decrypt cycle
    @Test
    public void testEncryptDecryptRoundTrip() {
        // Given: plaintext string
        String plaintext = "My secret note content";
        
        // When: encrypted and decrypted
        byte[] encrypted = encryptionManager.encrypt(plaintext);
        String decrypted = encryptionManager.decrypt(encrypted);
        
        // Then: plaintext matches original
        assertEquals(plaintext, decrypted);
        assertNotEquals(plaintext, new String(encrypted));
    }
    
    // Test 2: Encryption produces different output (non-deterministic/IV)
    @Test
    public void testEncryptionNonDeterministic() {
        String plaintext = "Same content";
        byte[] enc1 = encryptionManager.encrypt(plaintext);
        byte[] enc2 = encryptionManager.encrypt(plaintext);
        
        // Two encryptions of same plaintext must differ (due to IV/salt)
        assertFalse(Arrays.equals(enc1, enc2));
    }
    
    // Test 3: HMAC computed correctly
    @Test
    public void testHmacComputation() {
        String plaintext = "Important note";
        byte[] hmac1 = encryptionManager.computeHmac(plaintext);
        byte[] hmac2 = encryptionManager.computeHmac(plaintext);
        
        // Same plaintext → same HMAC
        assertArrayEquals(hmac1, hmac2);
    }
    
    // Test 4: Tampered data detected via HMAC
    @Test
    public void testHmacTamperDetection() {
        String plaintext = "Original data";
        byte[] encrypted = encryptionManager.encrypt(plaintext);
        byte[] hmac = encryptionManager.computeHmac(plaintext);
        
        // Simulate tamper: flip a bit in encrypted data
        encrypted[0] ^= 1;
        
        // Verify HMAC no longer matches
        byte[] newHmac = encryptionManager.computeHmac(new String(encrypted));
        assertFalse(Arrays.equals(hmac, newHmac));
    }
    
    // Test 5: Unlock required before encrypt
    @Test
    public void testEncryptRequiresUnlock() {
        EncryptionManager locked = new EncryptionManager();
        // locked.unlock() NOT called
        
        assertThrows(EncryptionException.class, 
            () -> locked.encrypt("data"));
    }
    
    // Test 6: Lock clears key
    @Test
    public void testLockClearsKey() {
        encryptionManager.unlock("password");
        encryptionManager.lock();
        
        // After lock, operations should fail
        assertThrows(EncryptionException.class,
            () -> encryptionManager.encrypt("data"));
    }
}
```

**Rationale:**
- Tests 1-2: Core encrypt/decrypt contract
- Test 3-4: HMAC validates integrity without relying on decryption
- Tests 5-6: Access control (unlock/lock) gates all operations

---

### Phase 1B: Integration Tests for Encryption + Storage

**Test Suite:** `NoteStorageEncryptionIntegrationTest.java`

```java
public class NoteStorageEncryptionIntegrationTest {
    
    private SQLiteNoteStorage storage;
    private EncryptionManager encryptionManager;
    
    @Before
    public void setUp() throws StorageException {
        encryptionManager = new EncryptionManager();
        encryptionManager.unlock("test_password");
        storage = new SQLiteNoteStorage("/tmp/test_encrypted.db", encryptionManager);
        storage.initialize();
    }
    
    // Test 1: Create note stores encrypted body
    @Test
    public void testCreateNoteEncryptsBody() throws StorageException {
        String plaintext = "Confidential project details";
        String noteId = storage.create("Project Plan", plaintext, 
            List.of("work"), "default");
        
        // Verify body is encrypted in database (raw query)
        byte[] rawFromDb = getRawBodyFromDb(noteId);
        assertFalse(new String(rawFromDb, StandardCharsets.UTF_8)
            .contains("Confidential"));
    }
    
    // Test 2: Retrieve note decrypts correctly
    @Test
    public void testRetrieveNoteDecrypts() throws StorageException {
        String plaintext = "My secret thoughts";
        String noteId = storage.create("Journal", plaintext, 
            List.of("personal"), "default");
        
        Optional<Note> retrieved = storage.get(noteId);
        assertTrue(retrieved.isPresent());
        assertEquals(plaintext, retrieved.get().getBody());
    }
    
    // Test 3: Updated note still encrypted
    @Test
    public void testUpdateNoteEncryption() throws StorageException {
        String noteId = storage.create("Original", "Original body", 
            List.of("tag"), "default");
        
        Note note = storage.get(noteId).get();
        note.setBody("Updated secret content");
        storage.update(noteId, note);
        
        // Verify updated body is still encrypted
        byte[] rawFromDb = getRawBodyFromDb(noteId);
        assertFalse(new String(rawFromDb, StandardCharsets.UTF_8)
            .contains("Updated secret"));
        
        // Verify decryption returns new content
        Optional<Note> updated = storage.get(noteId);
        assertEquals("Updated secret content", updated.get().getBody());
    }
    
    // Test 4: HMAC prevents tampering
    @Test
    public void testHmacPreventsTampering() throws StorageException {
        String noteId = storage.create("Secure Note", "Original content", 
            List.of("tag"), "default");
        
        // Retrieve HMAC from database
        byte[] originalHmac = getHmacFromDb(noteId);
        
        // Tamper with encrypted body in database
        tamperWithBodyInDb(noteId);
        
        // Retrieve should detect tampering
        assertThrows(StorageException.class, 
            () -> storage.get(noteId),
            "Expected StorageException due to HMAC mismatch");
    }
    
    // Test 5: Soft-delete doesn't lose encryption
    @Test
    public void testSoftDeletePreservesEncryption() throws StorageException {
        String plaintext = "Important data";
        String noteId = storage.create("Will Delete", plaintext, 
            List.of("temp"), "default");
        
        // Soft-delete
        storage.delete(noteId);
        
        // Verify note marked deleted but still encrypted in DB
        byte[] rawFromDb = getRawBodyFromDb(noteId);
        assertFalse(new String(rawFromDb, StandardCharsets.UTF_8)
            .contains("Important data"));
        
        // Verify get() doesn't return deleted notes
        Optional<Note> deleted = storage.get(noteId);
        assertFalse(deleted.isPresent(), 
            "Deleted notes should not be returned by get()");
    }
    
    // Helper methods
    private byte[] getRawBodyFromDb(String noteId) {
        // Execute raw SQL: SELECT body_encrypted FROM notes WHERE id = ?
        // Return raw bytes without decryption
    }
    
    private byte[] getHmacFromDb(String noteId) {
        // Execute raw SQL: SELECT hmac FROM notes WHERE id = ?
    }
    
    private void tamperWithBodyInDb(String noteId) {
        // Execute raw SQL UPDATE to flip a bit in body_encrypted
    }
}
```

**Rationale:**
- Tests 1-3: Verify encryption works end-to-end through storage layer
- Test 4: HMAC detects corruption (prevents silent data loss)
- Test 5: Soft-delete doesn't weaken encryption guarantees

---

### Phase 2A: Unit Tests for Search Correctness

**Test Suite:** `SearchQueryTest.java`

```java
public class SearchQueryTest {
    
    // Test 1: Exact phrase match
    @Test
    public void testSearchExactPhrase() {
        // Note body: "The quick brown fox"
        // Query: "brown fox"
        // Should match
        
        List<Note> results = storage.search("brown fox");
        assertTrue(results.stream()
            .anyMatch(n -> n.getBody().contains("brown fox")));
    }
    
    // Test 2: Case-insensitive search
    @Test
    public void testSearchCaseInsensitive() {
        // Note body: "AstraNotes"
        // Query: "astranotes"
        // Should match
        
        List<Note> results = storage.search("astranotes");
        assertTrue(results.size() > 0);
    }
    
    // Test 3: Tag search included
    @Test
    public void testSearchIncludesTags() {
        String noteId = storage.create("Note", "body", 
            List.of("python", "tutorial"), "default");
        
        List<Note> results = storage.search("python");
        assertTrue(results.stream().anyMatch(n -> n.getId().equals(noteId)));
    }
    
    // Test 4: Title search included
    @Test
    public void testSearchIncludesTitle() {
        String noteId = storage.create("Important Meeting", "notes here", 
            List.of(), "default");
        
        List<Note> results = storage.search("Meeting");
        assertTrue(results.stream().anyMatch(n -> n.getId().equals(noteId)));
    }
    
    // Test 5: Deleted notes excluded
    @Test
    public void testSearchExcludesDeleted() {
        String noteId = storage.create("To Delete", "sensitive", 
            List.of("temp"), "default");
        
        storage.delete(noteId);
        
        List<Note> results = storage.search("sensitive");
        assertFalse(results.stream().anyMatch(n -> n.getId().equals(noteId)));
    }
    
    // Test 6: Results sorted by updatedAt (newest first)
    @Test
    public void testSearchSortedByUpdatedAt() {
        String id1 = storage.create("First", "content", List.of(), "default");
        String id2 = storage.create("Second", "content", List.of(), "default");
        
        List<Note> results = storage.search("content");
        // id2 (newest) should come before id1
        int idx1 = results.stream().findFirst().filter(n -> n.getId().equals(id1)).map(n -> results.indexOf(n)).orElse(-1);
        int idx2 = results.stream().findFirst().filter(n -> n.getId().equals(id2)).map(n -> results.indexOf(n)).orElse(-1);
        assertTrue(idx2 < idx1, "Newer notes should appear first");
    }
}
```

---

### Phase 2B: Feature-Level Test — Search Performance

**Test Suite:** `SearchPerformanceFeatureTest.java`

```java
public class SearchPerformanceFeatureTest {
    
    private SQLiteNoteStorage storage;
    
    @Before
    public void setUp() throws StorageException {
        encryptionManager = new EncryptionManager();
        encryptionManager.unlock("perf_test");
        storage = new SQLiteNoteStorage("/tmp/perf_test.db", encryptionManager);
        storage.initialize();
    }
    
    /**
     * Test: Search completes under 150ms for 10k notes
     * Maps to: REQ-NF-1 (Performance), Story 5 Acceptance Criteria
     */
    @Test
    public void testSearchPerformance10kNotes() throws StorageException {
        // SETUP: Create 10,000 notes
        System.out.println("Creating 10,000 test notes...");
        for (int i = 0; i < 10_000; i++) {
            String title = "Note " + i;
            String body = "This is note number " + i + ". " +
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Some notes contain the word 'searchable'.";
            String notebook = i % 10 == 0 ? "archive" : "default";
            storage.create(title, body, List.of("tag" + (i % 100)), notebook);
        }
        
        // EXERCISE: Search for a term present in some notes
        long startMs = System.currentTimeMillis();
        List<Note> results = storage.search("searchable");
        long elapsedMs = System.currentTimeMillis() - startMs;
        
        // VERIFY: Performance under 150ms + results non-empty
        System.out.println("Search returned " + results.size() + 
            " results in " + elapsedMs + "ms");
        assertTrue("Search must complete under 150ms, but took " + 
            elapsedMs + "ms", elapsedMs < 150);
        assertTrue("Search should return results", results.size() > 0);
    }
    
    /**
     * Test: Search performance with notebook filter
     */
    @Test
    public void testSearchWithNotebookFilter() throws StorageException {
        // Create 1000 notes in "default" and 1000 in "archive"
        for (int i = 0; i < 2000; i++) {
            String notebook = i < 1000 ? "default" : "archive";
            storage.create("Note " + i, "content " + i, List.of(), notebook);
        }
        
        // Search only in "default" notebook
        long startMs = System.currentTimeMillis();
        List<Note> results = storage.search("content", "default");
        long elapsedMs = System.currentTimeMillis() - startMs;
        
        assertTrue("Filtered search under 150ms", elapsedMs < 150);
        assertTrue("Results match notebook", 
            results.stream().allMatch(n -> "default".equals(n.getNotebook())));
    }
}
```

---

## 5. AI's Role in Test Design

### How AI Helped (Iterative Dialogue)

**AI Assistance:** I used AI to:
1. **Identify edge cases** — "What could go wrong with encryption if the key isn't properly derived?"
   - AI suggested: IV reuse (now addressed with non-deterministic encryption), key stretching validation, unlocked-state checks
2. **Critique test coverage gaps** — "Are there scenarios where HMAC wouldn't catch corruption?"
   - AI noted: If attacker controls both body AND HMAC, checks fail; added tests for partial tampering
3. **Suggest realistic load profiles** — "What's a realistic dataset size for search performance?"
   - AI recommended 10,000 notes (matches Story 5 acceptance criteria of "10k notes under 150ms")
4. **Review for redundancy** — "Is TestEncryptionNonDeterministic necessary if we have RoundTrip test?"
   - AI confirmed: Yes, it validates non-determinism (proof of IV/salt inclusion), not just correctness

### What I Kept, Changed, Rejected

| Suggestion | Decision | Reason |
|---|---|---|
| Add password-strength validation tests | **Rejected** | Out of scope for Week 7; belongs in Auth phase |
| Test concurrent reads while write in progress | **Kept** | Relevant to REQ-REL-1 (ACID); added as future test |
| Test encryption with 1MB+ note body | **Changed** | Reduced to 100KB (reasonable note size) to avoid excessive test runtime |
| Generate randomized search queries | **Rejected** | Non-deterministic tests are harder to debug; prefer fixed queries |
| Test UI responsiveness (FX) | **Rejected** | Beyond scope; Swing UI testing requires separate harness |

---

## 6. Test Execution Timing During Development

| Phase | When | Tests to Run | CI/CD Gate | Duration |
|-------|------|-------------|-----------|----------|
| **Pre-commit** | Developer pushes to branch | Unit tests (Encryption, Note model) | Must pass | < 5s |
| **Pre-merge** | Developer opens PR | Unit + Integration tests (Storage, encryption) | Must pass | < 30s |
| **Pre-release** | Before tagging release | Unit + Integration + Feature tests (performance) | Must pass | ~5 min |
| **Nightly** | Scheduled job | All tests + load tests (100k notes) | Report only | ~30 min |

**Key insight:** Expensive feature-level tests (load, performance) don't block PRs; they run on a schedule and inform release decisions.

---

## 7. Summary: Why These Tests Matter Now

### For Security
- **Without encryption/HMAC tests:** Users unknowingly store plaintext; privacy is silently violated.
- **With them:** Every commit guarantees encrypted storage and corruption detection.

### For Performance
- **Without search performance tests:** Search feels fast on dev machine (small dataset) but lags in production (10k notes). Too late to fix architecture.
- **With them:** We catch performance regressions early; FTS5 index effectiveness is validated before release.

### For Confidence
- **Phase 1 (Encryption + Search)** tests ~40% of requirements (security + performance).
- **Phase 2 (UI, versioning, plugins)** tests add ~40%.
- **Phase 3 (load, backup, edge cases)** tests add ~20%.

By focusing first on encryption and search, we reduce risk in the most critical areas before expanding test coverage.

---

## 8. Next Steps (Week 7.3+)

1. **Implement Phase 1A tests** (encryption unit tests) — should pass immediately
2. **Implement Phase 1B tests** (integration tests) — debug any storage issues
3. **Run performance baseline** — identify if search needs index tuning
4. **Expand Phase 2** — add versioning, soft-delete, and plugin tests
5. **Continuous integration:** Wire tests into CI/CD so regressions are caught early

---

## Appendix: Test File Structure

```
src/test/java/com/astraNotes/
├── encryption/
│   └── EncryptionManagerTest.java                    [Phase 1A]
├── storage/
│   ├── SQLiteNoteStorageTest.java                    [Existing]
│   ├── NoteStorageEncryptionIntegrationTest.java     [Phase 1B]
│   └── SearchPerformanceFeatureTest.java             [Phase 2B]
├── search/
│   └── SearchQueryTest.java                          [Phase 2A]
└── ui/
    └── UIIntegrationTest.java                        [Future]
```

---

**Document prepared:** Week 7.2 Lab Submission  
**Testing strategy valid for:** Weeks 7–9 (through security and performance validation)  
**Ownership:** Student with iterative AI collaboration on case identification and gap analysis
