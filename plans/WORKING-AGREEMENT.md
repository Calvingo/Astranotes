# AstraNotes Working Agreement
## Governance & Workflow for Q2 2026

---

## 1) Team Structure (adapted Spotify Model)

### Squad: AstraNotes Core
- **Owner**: jw (architect + lead dev)
- **Scope**: SQLite storage layer, encryption, plugin API, core UI/note lifecycle
- **Cadence**: 1-week sprints (Mon start, Fri review)
- **Goal**: Deliver MVP with all FR1-FR8 + SEC1-3 by end of Q2

### Chapter: Quality & Architecture
- **Owner**: jw (self + peer review via git + doc comments)
- **Scope**: enforce definition of done, schema reviews, encryption decisions, test coverage
- **Meetings**: async code review (24h turnaround)

### Guild: AI + Prompts
- **Members**: jw + Copilot (agent collaboration)
- **Purpose**: Curate reusable prompts, document patterns, share learnings
- **Artifacts**: `prompts/` folder with tagged, versioned prompt library

---

## 2) How Work is Planned & Tracked

### Backlog organization
- **Location**: `plans/BACKLOG.md` (living document, not Jira)
- **Format**: 
  ```
  ## Sprint N (dates)
  - [ ] FR1: Create note — design + implementation + test
  - [ ] SEC1: Encryption — design + implementation + integration test
  ```
- **Priority rules**:
  1. Security (SEC1-3) must pass before any feature ships
  2. Core CRUD (FR1-4) before search (FR5)
  3. Tests before UI integration
  4. Design docs before code

### Tracking cadence
- **Weekly sprint planning** (Mon 9am): review last sprint, estimate next sprint
- **Daily standup** (async in git comments on PRs): 3-5 min check-in
- **Friday review** (Fri 5pm): demo working code, update backlog

### Metrics
- **Acceptance**: all tests pass + design doc signed off
- **Cycle time**: time from "TODO" → "DONE" per item
- **Quality gate**: 0 security debt in main; all FR tests green

---

## 3) How AI is Used in Workflow

### Allowed AI roles (in priority order)
1. **Exploration & research** (unlimited)
   - architecture decisions (options analysis)
   - API design comparisons
   - SQLite schema patterns
   - plugin design patterns
   - Test strategy brainstorms

2. **Code generation** (with review gate)
   - initial skeleton for new classes (e.g., `NoteRepository.java`)
   - boilerplate (getter/setter, basic CRUD)
   - test stubs and fixtures
   - documentation outlines

3. **Code review & refactoring** (with author approval)
   - suggest naming improvements
   - identify unused imports or code duplication
   - propose test coverage gaps
   - optimization hints

4. **Not allowed without explicit override**
   - generate cryptographic code (encryption/HMAC) without manual peer review
   - write security-critical features (auth, key derivation) with minimal human inspection
   - commit directly to main without PR review

### AI collaboration workflow
- **Initiate**: jw writes `.instructions.md` or `.prompt.md` in the branch folder (e.g., `src/storage/.instructions.md`)
- **Execute**: run Copilot with explicit request
- **Output**: Copilot generates code or design doc
- **Review**: jw reviews output, runs tests, signs off in git commit message
- **Iterate**: if output insufficient, refine prompt and re-run (max 3 rounds before pausing to think)

### Prompt library (guild asset)
- **Location**: `prompts/library.md`
- **Template**:
  ```
  ### Prompt: [name]
  **Goal**: [what problem it solves]
  **Usage**: [when to use]
  **Input**: [what you provide]
  **Output**: [what to expect]
  **Quality gates**: [how to vet result]
  **Revisions**: [date, change, reason]
  ```
- **Examples**:
  - "Generate CRUD skeleton for encrypted note model"
  - "Write FTS5 schema with migration script"
  - "Design plugin API interface + event hooks"

---

## 4) Documentation & Decision Traceability

### Three-tier documentation
1. **Plans** (`plans/` folder)
   - `plan-astraNotes.prompt.md` — architecture decisions (updated weekly)
   - `WORKING-AGREEMENT.md` — this file (governance + process)
   - `BACKLOG.md` — current sprint + roadmap
   - `DECISIONS.md` — why we chose Option A (SQLite), not B or C (with date + rationale)

2. **Code** (`src/` folder)
   - Every class: javadoc with "why" not just "what"
   - E.g., `NoteRepository`: "Uses prepared statements to prevent SQL injection and improve FTS5 performance"
   - Test names describe scenario: `testCreateNoteIncrementsVersionAndUpdatesTimestamp()`

3. **Git commits** (`git log`)
   - **Format**: `[TYPE] Subject — reason`
   - **Types**: `feat`, `fix`, `test`, `docs`, `refactor`, `sec` (security)
   - **Examples**:
     ```
     feat: Add SQLiteNoteStorage.create() — FR1 acceptance test now green
     sec: Add HMAC to encrypted note bodies — SEC2 integrity check
     test: Add 10k-note performance benchmark — NFR1 baseline
     ```

### Decision log (DECISIONS.md)
```
## Decision: Use SQLCipher, not app-level encryption
**Date**: 2026-04-01
**Context**: Needed DB encryption + per-note integrity
**Options**: A=SQLCipher, B=app-level field encryption, C=separate vault file
**Choice**: A (SQLCipher)
**Reasoning**: Single ACID transaction boundary, less code surface, transparent to queries
**Trade-offs**: Tied to SQLite; harder to migrate keys later
**Status**: ACCEPTED; supersedes earlier "app-level" design
```

### Revision log (in files)
- Each prompt/design doc includes `### Revisions` section:
  ```
  ### Revisions
  - 2026-04-06: Added Chapter role + prompt library structure (jw, based on Spotify feedback)
  - 2026-04-05: Initial draft (jw)
  ```

---

## 5) AI Output Acceptance Criteria

### Code generated by AI (must pass all):
- [ ] Compiles without warnings
- [ ] All unit tests pass (coverage ≥ 80%)
- [ ] No security anti-patterns (no hardcoded keys, SQL injection risks, etc.)
- [ ] Matches team style guide (naming, indentation, comments)
- [ ] Runs in < 150ms (perf requirements) or has TODO if not
- [ ] Logged in git commit with reference to prompt used

### Design docs generated by AI:
- [ ] Addresses all sections (goal, components, lifecycle, etc.)
- [ ] Includes trade-offs and constraints
- [ ] Aligned with prior decisions (no contradictions)
- [ ] Ready for code skeleton (concrete enough to implement)
- [ ] Cross-checked against requirements (FR/NFR/SEC alignment)

### Prompts added to library:
- [ ] Reusable (not one-off; addresses a common pattern)
- [ ] Tested (proven output quality ≥ 3 successful uses)
- [ ] Documented (template complete + examples provided)
- [ ] Version-tagged (`v1.0` at minimum)

---

## 6) Preventing Drift, Duplication, Low-Quality Output

### Drift prevention
1. **Weekly alignment**: review BACKLOG.md + plan-astraNotes.prompt.md side-by-side
   - confirm sprint items still match architecture
   - flag any scope creep (new features = new backlog item + estimation)
2. **Requirement traceability**: every test/code PR links to FR/NFR/SEC requirement
3. **Done = done**: a feature is only "done" when:
   - Code written + tested
   - Design doc updated
   - Commit message + prompt logged
   - Test result recorded in BACKLOG

### Duplication prevention
1. **Prompt library first**: before asking AI for "write CRUD for Attachment", check library
   - if reusable pattern exists, use it
   - if novel, add to library after first success
2. **Code search before generating**: grep for similar methods in codebase
3. **Git log review**: `git log --oneline` weekly to spot overlapping commits

### Low-quality output
1. **Acceptance gates**:
   - Tests red = auto-reject (don't merge)
   - Security review flags = escalate to peer/human
   - Performance miss (e.g., >150ms) = mark as debt, create follow-up ticket
2. **Regeneration limit**: if AI output needs >2 iterations to pass gates, pause and design manually
3. **Log failures**: add `REJECTED.md` for outputs that didn't make cut (learn from mis-prompts)

---

## 7) Weekly Rhythm

### Monday 9:00
- **Sprint planning** (15 min async or sync)
- Review last sprint: what shipped, what blocked, lessons
- Pick 3-5 items from BACKLOG for this week
- Record in BACKLOG.md with owner initials

### Tue-Thu
- **Daily standby** (async): push code, PR reviews, test runs
- Copilot prompts logged in commits
- Any blockers escalated to Monday debrief

### Friday 5:00
- **Sprint review**: demo working code, update metrics
- Log cycle time + quality metrics
- Retrospective: what accelerated, what slowed down AI use
- Plan improvements for next week

---

## 8) Definition of Done (DoD)

**Core principle**: A feature is DONE when it is **production-ready, traceable, tested, and defensible**.

### Part A: Requirement Mapping (Does this solve a real need?)

**Before starting work:**
- [ ] Task is traced to at least one FR/NFR/SEC requirement from the refined requirements list
- [ ] Acceptance criteria from BACKLOG item are explicit and measurable (not vague)
- [ ] Scope is clear: what is IN, what is OUT (prevent scope creep)

**Example of mapped task**:
```
Task: Implement NoteRepository.create()
Maps to: FR1 (Create note)
Acceptance: 
  - POST/command returns note ID
  - notes table contains row with deleted=0, version=1
  - createdAt=now, updatedAt=now
  - title/body/tags stored without truncation (up to 1MB body limit)
TEST: testCreateNoteReturnsValidIdAndUpdatesDatabase()
```

**Example of unmapped task (REJECT)**:
```
Task: "Fix UI lag during search"
Problem: No FR/NFR linked; "lag" is unmeasurable
Fix: Reframe as "NFR1: Reduce search response to < 150ms for 10k notes database"
```

---

### Part B: Logic & Design Explainability (Can you defend this code?)

**Before submitting PR:**
- [ ] Every class/method has javadoc explaining **why** (not just **what**)
- [ ] Non-obvious logic has an inline comment referencing FR/NFR/SEC or design doc
- [ ] Public API is documented with usage examples (at least one)
- [ ] If cryptographic/security code: design decision logged in DECISIONS.md

**Example of good design doc**:
```java
/**
 * SQLiteNoteStorage provides encrypted CRUD for notes using SQLCipher.
 * 
 * WHY SQLite: Single ACID transaction boundary, transparent encryption via SQLCipher,
 * no custom query layer needed. Supports FTS5 for full-text search without extra layer.
 * (See DECISIONS.md: "Use SQLCipher, not app-level encryption")
 * 
 * @author jw
 * @see EncryptionManager for key lifecycle
 */
public class SQLiteNoteStorage implements NoteRepository {
  ...
}
```

**Example of good decision traceability**:
```java
// HMAC per note for integrity check (SEC2 requirement)
// Uses HMAC-SHA256 per row to detect tampering after decrypt.
// Trade-off: +32 bytes per note, ~1ms per verify. Accepted for security posture.
byte[] hmac = computeHMAC(noteId, title, body, rootKey);
```

**Example of explanatory PR comment** (if using AI):
```
[feat] Add FTS5 full-text search schema

AI generated schema skeleton via prompt: "Design FTS5 virtual table for note search"
Modified: removed redundant indices, added pagination support
Rationale: NFR1 (< 150ms search) requires indexed search; FTS5 provides token matching without extra layer
Test coverage: testSearchNotesByTitleKeyword, testSearchPagination, testFTSPerformance
```

---

### Part C: Quality & Realism Check (Is this production-ready?)

**Code quality gate:**
- [ ] Compiles with **0 warnings** (treat warnings as errors)
- [ ] Linter passes: consistent naming, indentation, no dead code
- [ ] Code review sign-off from peer (async within 24h)
- [ ] No hardcoded paths, passwords, API keys (fail if detected)
- [ ] No debug `System.out.println()` left in code (use logger)

**Realism check** (ask yourself):
- [ ] Does this code handle empty inputs, null values, edge cases?
- [ ] If this crashes in production, is the error message helpful?
- [ ] Did I test on all supported platforms (macOS/Windows/Linux)?
- [ ] Is memory usage reasonable (no leaks, no unbounded caches)?

**Example of quality gate failure**:
```java
// ❌ REJECTED: hardcoded path, no error handling
String dbPath = "/Users/jw/astranotes.db";
Connection conn = DriverManager.getConnection("jdbc:sqlite:"+dbPath);

// ✅ ACCEPTED: configurable path, error handling
String dbPath = getAppDataDir() + "/astranotes.db"; // respects OS paths
try {
  Connection conn = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
} catch (SQLException e) {
  logger.error("Failed to open DB at {}: {}", dbPath, e.getMessage());
  throw new StorageException("Database unavailable", e);
}
```

---

### Part D: Testing, Traceability & Validation (How do I know it works?)

**Test coverage requirements:**
- [ ] ≥ 80% code coverage (unit tests for all public methods)
- [ ] Happy path: test the main scenario (create note → read → return)
- [ ] Error paths: test null inputs, invalid states, exceptions
- [ ] Integration test: if this feature touches DB/encryption, test end-to-end
- [ ] Performance test: if NFR involved (< 150ms search), benchmark included
- [ ] Security test: if SEC involved (encryption, HMAC), verify with intentional tampering

**Test naming convention** (makes intent obvious):
```java
✅ testCreateNoteIncrementsVersionAndUpdatesTimestamp()
✅ testUpdateNoteWithNullBodyThrowsException()
✅ testSearchNotesWithEmptyQueryReturnsAllNotes()
✅ testGetDeletedNoteReturns404NotFound()
❌ testNote() — too vague
❌ test1() — meaningless
```

**Traceability artifact**:
- [ ] Test class name matches feature: `NoteRepositoryCreateTest` for FR1
- [ ] Test method name references requirement: `testCreateNoteMeetsAcceptanceCriteriaFR1()`
- [ ] Git commit references requirement: `[test] Add NoteRepositoryCreateTest — FR1 acceptance`
- [ ] BACKLOG.md links test to DONE status

**Example of traceable test**:
```java
/**
 * Test: FR1 - Create note returns ID and stores in DB
 * Acceptance: POST returns 201 with note ID; notes table contains row with deleted=0, version=1
 */
@Test
public void testCreateNoteReturnsIdAndStoresInDatabase() {
  // GIVEN a note request
  Note request = new Note("title", "body", Arrays.asList("tag1"));
  
  // WHEN we create the note
  String noteId = storage.create(request);
  
  // THEN we get a non-null ID
  assertNotNull(noteId);
  
  // AND the note is in the DB with correct fields
  Note stored = storage.get(noteId);
  assertEquals("title", stored.getTitle());
  assertEquals(1, stored.getVersion());
  assertFalse(stored.isDeleted());
}
```

---

### Part E: Security, Privacy & Governance (Are risks addressed?)

**Security checklist** (STOP if any fail):
- [ ] **Crypto**: No custom crypto code without peer review + DECISIONS.md entry
- [ ] **Secrets**: No password/key in code, logs, or error messages
- [ ] **SQL injection**: All queries use prepared statements (no string concatenation)
- [ ] **Integrity**: If data is encrypted, HMAC/signature is verified on every decrypt
- [ ] **Deletion**: Soft-delete only (tombstone); permanent purge requires explicit purge task

**Privacy checklist**:
- [ ] **Logging**: Note content never logged; metadata-only (IDs, counts, timings)
- [ ] **Caches**: In-memory decrypted data is cleared after use or on lock
- [ ] **Exports**: Export files are plaintext; user must consent to unencrypted copy

**Governance checklist**:
- [ ] **Schema changes**: Migration script added to `migrations/` + version incremented
- [ ] **Breaking changes**: Decision log entry explaining why change is necessary
- [ ] **Audit trail**: Git commit message explains decision + rationale
- [ ] **Backward compat**: Old code paths gracefully upgrade or reject old data

**Example of security-conscious code**:
```java
// ✅ GOOD: Prepared statement (prevents SQL injection)
String query = "SELECT * FROM notes WHERE id = ? AND deleted = 0";
PreparedStatement stmt = conn.prepareStatement(query);
stmt.setString(1, noteId);
ResultSet rs = stmt.executeQuery();

// ❌ BAD: String concatenation (SQL injection risk)
String query = "SELECT * FROM notes WHERE id = '" + noteId + "' AND deleted = 0";
ResultSet rs = conn.createStatement().executeQuery(query);

// ✅ GOOD: HMAC verification + logging
byte[] stored_hmac = decryptedRow.getHmac();
byte[] computed_hmac = computeHMAC(noteId, title, body);
if (!Arrays.equals(stored_hmac, computed_hmac)) {
  logger.error("HMAC mismatch for note {}: data may be corrupted", noteId);
  throw new DataIntegrityException("Note integrity check failed");
}

// ✅ GOOD: Never log content
logger.info("Created note {} with {} bytes and {} tags", noteId, bodySize, tagCount);
// ❌ BAD: Do not log this
logger.info("Created note with content: {}", body);
```

---

### Part F: Deserves to Move Forward (Go/No-Go Decision)

**Final gate** (all must pass):
- [ ] Requirement mapping complete (traces to FR/NFR/SEC)
- [ ] Design is explainable and documented
- [ ] Code compiles cleanly, lint passes, no hardcoded secrets
- [ ] Tests pass (unit + integration + perf/security as needed)
- [ ] Traceability logged (git commit + test name + BACKLOG update)
- [ ] Security/privacy/governance concerns addressed or escalated
- [ ] Code review approved (peer sign-off within 24h)
- [ ] BACKLOG.md marked DONE + cycle time recorded

**Go/No-Go decision**:

| Signal | Status | Action |
|--------|--------|--------|
| All 6 parts above ✅ | **GO** | Merge to `main`, close backlog item, move to next sprint |
| 1-2 checks fail | **HOLD** | Fix and resubmit; update PR within 24h |
| 3+ checks fail or security concern | **NO-GO** | Reject PR, design manually without AI, escalate to decision log |
| Cycle time > 1 week per item | **INVESTIGATE** | Was scope too large? Break into smaller items next sprint. |

**Example: Go/No-Go checklist for FR1 (Create note)**

```
Task: Implement NoteRepository.create()

✅ PART A: Requirement Mapping
  - [x] Traces to FR1: Create note
  - [x] Acceptance criteria: "returns ID, stored in DB with version=1"
  
✅ PART B: Design Explainability
  - [x] Javadoc explains: "Uses SQLCipher + prepared statement + FTS update"
  - [x] Decision logged: "Why prepared statements (SQL injection prevention)"
  
✅ PART C: Quality & Realism
  - [x] Compiles, lint passes, 0 warnings
  - [x] Error handling: null body → throws exception
  - [x] Tested on macOS only (TODO: test Windows/Linux in CI)
  
✅ PART D: Testing & Traceability
  - [x] Test: testCreateNoteReturnsIdAndStoresInDatabase() 
  - [x] Coverage: 85% (happy path + null title error path)
  - [x] Commit: "[feat] Add NoteRepository.create() — FR1 acceptance test green"
  
✅ PART E: Security & Governance
  - [x] Uses SQLCipher (no custom crypto)
  - [x] FTS updated in same transaction (atomic)
  - [x] No logging of note body
  - [x] No keys/secrets in code
  
✅ PART F: Final Go/No-Go
  - [x] All gates passed
  - [x] PR reviewed + approved
  - [x] Cycle time: 2 days (on target)
  
STATUS: ✅ GO — MERGE TO MAIN
```

---

### When to Reject/Escalate

**Auto-reject (do not merge)**:
1. Tests fail (red is red)
2. Security concern detected (crypto, injection, secrets)
3. No requirement traceability
4. Cycle time > 1 week without approval

**Manual escalation** (ask peer/mentor):
1. Design decision that contradicts DECISIONS.md
2. Performance misses NFR by > 20% (e.g., 200ms instead of 150ms)
3. Complex merge conflict involving multiple features
4. Unclear if contribution aligns with MVP scope

---

## Summary: Definition of Done

A feature is **DONE** when:

1. **It solves a real problem** — mapped to FR/NFR/SEC requirement with measurable acceptance
2. **It is defensible** — design documented, logic explained, decision logged
3. **It is quality** — compiles cleanly, no secrets, no warnings, peer-reviewed
4. **It is tested** — ≥ 80% coverage, happy + error paths, perf/security tests as needed
5. **It is traceable** — git commit + test name + BACKLOG update all linked
6. **It is safe** — security/privacy/governance concerns addressed or escalated

**and you can confidently answer "yes" to:**
> "Would I ship this code to production and support it?"

---

## 9) Roles & Responsibilities

| Role | Responsible For | Cadence |
|------|-----------------|---------|
| **Squad Owner (jw)** | Architecture, sprint planning, final decisions, peer review | Daily |
| **Copilot Agent** | Code skeleton, design outline, prompt refinement, test stub | On-demand (request-based) |
| **Tests** | Verify acceptance + prevent regression | Before merge |
| **Git/Commit log** | Traceability, audit trail for decisions | Per-commit |

---

## 10) Escalation Path

If blocked:
1. **Code doesn't compile**: check error log + git history, ask Copilot to regenerate with error context
2. **Test fails**: debug locally, add test case to prevent repeat, push to PR
3. **Performance misses NFR**: log as debt item (`DEBT.md`), mark as "future optimization", move on
4. **Security concern**: STOP, manual review + design doc, escalate to decision log
5. **Scope creep**: add to BACKLOG with "v2" tag, do not interrupt current sprint

---

## Summary

This working agreement ensures:
- **Speed**: AI collaboration accelerates exploration + boilerplate
- **Control**: gates (tests, review, acceptance) prevent low-quality output
- **Clarity**: every decision logged, every prompt versioned, every code change traceable
- **Quality**: no drift from requirements; duplication caught early; security-first mindset

**Effective date**: 2026-04-06  
**Review date**: 2026-04-13  
**Owner**: jw  
**Status**: ACTIVE
