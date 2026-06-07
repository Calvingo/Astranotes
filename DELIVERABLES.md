# Week 6 AstraNotes Development - Deliverables Checklist

## Submission Requirements vs. Deliverables

### ✓ 1. Development Direction

**Required**: Programming language and desktop/web decision  
**Provided**: 
- **Language**: Java 17
- **Type**: Desktop (Swing GUI)
- **Platform**: Cross-platform (macOS, Windows, Linux)

**Documents**:
- [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) - Section 1
- [WEEK6_QUICKSTART.md](WEEK6_QUICKSTART.md) - Technology stack

---

### ✓ 2. Project Structure & Build Environment

**Required**: Repository structure (src/, test/, docs/, build files) and build/run environment  
**Provided**:

```
✓ src/main/java/com/astraNotes/  (models, storage, encryption, ui)
✓ src/test/java/com/astraNotes/  (unit + integration tests)
✓ pom.xml                         (Maven build configuration)
✓ src/main/resources/logback.xml  (Logging configuration)
✓ BUILD_AND_RUN.md                (Comprehensive build guide)
✓ WEEK6_QUICKSTART.md             (Quick start guide)
```

**Build Command**: `mvn clean package`  
**Run Command**: `java -jar target/astraNotes-0.1.0.jar`  

**Documents**:
- [BUILD_AND_RUN.md](BUILD_AND_RUN.md) - Full section
- [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) - Section 2

---

### ✓ 3. UI Shell Description

**Required**: Menu, profile, settings, notes workspace layout decisions  
**Provided**:

#### Menu Structure
```
File → New Note, Exit
Edit → Settings
View → Refresh
Help → About
```

#### Layout
- **Left Sidebar** (300px): Scrollable note list with pagination
- **Right Main Area**: Note detail view (title + body + metadata)
- **Bottom Buttons**: New Note, Delete, Refresh
- **Dialogs**: Create Note (modal form), Settings (info panel)

#### Features
- ✓ Profile placeholder (notebook context + settings panel; full user profile is future scope)
- ✓ Settings panel (database location, encryption status)
- ✓ Notes workspace (list + detail view)
- ✓ Create dialog with title, body, tags, notebook fields

**Files**:
- `src/main/java/com/astraNotes/ui/AstraNotesApp.java` - Menu + main window
- `src/main/java/com/astraNotes/ui/MainPanel.java` - Workspace coordinator
- `src/main/java/com/astraNotes/ui/NoteListPanel.java` - Note list sidebar
- `src/main/java/com/astraNotes/ui/NoteDetailPanel.java` - Detail view
- `src/main/java/com/astraNotes/ui/NoteCreateDialog.java` - Create dialog

**Documents**:
- [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) - Section 3 (UI Shell)
- [WEEK6_QUICKSTART.md](WEEK6_QUICKSTART.md) - Features implemented

---

### ✓ 4. First 1-2 Functionality Slices Implemented

**Required**: Implement and document 1-2 slices  
**Provided**:

#### Slice 1: Create Note (REQ-1)
**File**: `src/main/java/com/astraNotes/storage/SQLiteNoteStorage.java` (lines 50-100)  
**UI**: `src/main/java/com/astraNotes/ui/NoteCreateDialog.java`  
**Test**: `SQLiteNoteStorageTest::testCreateNote`, `testCreateAndList`  

**What it does**:
1. User enters title, body, tags, notebook
2. Encryption manager encrypts body (AES-GCM)
3. HMAC computed for integrity
4. Inserted into SQLite with auto-incremented version=1
5. FTS5 index updated
6. Note ID returned to user

#### Slice 2: Retrieve, List, and Soft-Delete Notes (REQ-2, REQ-4)
**File**: `src/main/java/com/astraNotes/storage/SQLiteNoteStorage.java` (lines 110-180)  
**UI**: `src/main/java/com/astraNotes/ui/NoteListPanel.java`, `NoteDetailPanel.java`  
**Test**: `SQLiteNoteStorageTest::testReadNote`, `testCreateAndList`, `testSoftDelete`  

**What it does**:
1. User clicks note in sidebar
2. Storage layer queries by ID
3. Body decrypted transparently
4. Note metadata displayed in detail panel
5. Delete removes from list (soft-delete flag)
6. Version/timestamp shown for audit trail

**Documents**:
- [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) - Section 4 (Functionality Slices)
- [WEEK6_QUICKSTART.md](WEEK6_QUICKSTART.md) - "What Was Built"

---

### ✓ 5. Traceability Matrix (Requirements → Implementation → UML)

**Required**: Short traceability note showing which requirements/UML elements are realized  
**Provided**:

#### Requirements Traceability
| REQ ID | Status | Implementation |
|--------|--------|---|
| REQ-1 | ✓ | `SQLiteNoteStorage.create()` |
| REQ-2 | ✓ | `SQLiteNoteStorage.get()` + `NoteDetailPanel` |
| REQ-3 | Partial | Storage-level `SQLiteNoteStorage.update()` exists and is tested; edit UI is future work |
| REQ-4 | ✓ | `SQLiteNoteStorage.delete()` (soft-delete) |
| REQ-6 | ✓ | SQLite local-only, no network |
| REQ-SEC-1 | ✓ | `EncryptionManager.encrypt()` (AES-GCM) |
| REQ-SEC-2 | ✓ | `EncryptionManager.computeHMAC()` |
| REQ-SEC-3 | ✓ | `EncryptionManager.unlock(password)` |
| REQ-SEC-4 | ✓ | SQLite transactions with commit/rollback |

#### UML Mapping
- `Note` class → REQ-1, REQ-2, REQ-3 (properties, versioning)
- `NoteRepository` interface → REQ-1..6 (CRUD contract)
- `SQLiteNoteStorage` class → All REQ implemented
- `EncryptionManager` class → REQ-SEC-1, SEC-2, SEC-3
- User Stories 1-4 → Fully realized (Story 5 partially)

**Documents**:
- [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) - Section 5 (Traceability Matrix)

---

### ✓ 6. AI Reflection (Copilot Use and Changes Made)

**Required**: How Copilot helped and what you changed  
**Provided**:

#### What Copilot Did Well
✓ Maven POM boilerplate (dependencies, plugins)  
✓ Swing UI layouts (GridBagLayout setup)  
✓ JDBC patterns (prepared statements)  
✓ FTS5 schema design  
✓ Method docstrings  

#### What I Changed/Improved
✗ Encryption: Added PBKDF2-like key derivation (Copilot missed)  
✗ Architecture: Refactored into 5 UI components (instead of monolithic)  
✗ Testing: Switched to integration tests + slice-focused test cases  
✗ Transactions: Added explicit commit/rollback on all operations  
✗ Crypto Security: Reviewed and hardened AES implementation  

#### Key Lessons
- Copilot ≈ 40% faster scaffolding phase
- 60% of code required expert review (esp. security/architecture)
- Copilot doesn't anticipate edge cases or performance issues
- Best use: Generate boilerplate, then architect manually

**Documents**:
- [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) - Section 6 (AI Reflection)

---

## Code Quality and Testing

### ✓ Code Compiles and Tests Pass

```bash
$ mvn clean compile
[INFO] BUILD SUCCESS

$ mvn test
[INFO] Tests run: 19, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS

$ mvn package
[INFO] Building jar: target/astraNotes-0.1.0.jar
[INFO] BUILD SUCCESS
```

### Test Coverage

| Test Class | Tests | Purpose |
|---|---|---|
| NoteTest | 4 | Domain model behavior |
| SQLiteNoteStorageTest | 7 | CRUD operations + search |
| SQLiteNoteStorageIntegrationTest | 7 | End-to-end storage, export/import, purge, HMAC, plugin state |
| SearchPerformanceTest | 1 | Search benchmark |
| **Total** | **19** | **Coverage for Week 6 slices plus later storage hardening** |

---

## Documentation Provided

| Document | Purpose | Audience |
|---|---|---|
| [BUILD_AND_RUN.md](BUILD_AND_RUN.md) | Detailed build/run guide | Developers |
| [WEEK6_QUICKSTART.md](WEEK6_QUICKSTART.md) | Quick start + overview | Everyone |
| [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) | Full submission report | Lab instructors |
| [DELIVERABLES.md](DELIVERABLES.md) | This file | Lab instructors |
| Planning docs | Requirements + UML | Design reference |

---

## How to Evaluate

### Step 1: Build the Project
```bash
cd /Users/jw/Documents/scu/AstraNotes_v1
mvn clean package
# Output: target/astraNotes-0.1.0.jar
```

### Step 2: Run the Application
```bash
java -jar target/astraNotes-0.1.0.jar
```

### Step 3: Test Core Functionality

**Test Slice 1 (Create)**:
1. Click "New Note"
2. Enter: Title="Test", Body="Hello World"
3. Click Save
4. Verify: Note appears in sidebar, encrypted in DB

**Test Slice 2 (Retrieve + List)**:
1. Create 3 notes
2. Sidebar shows all 3
3. Click each → detail panel decrypts and displays body
4. Delete one → removed from list (soft-delete)

### Step 4: Run Tests
```bash
mvn test
# All 19 tests pass, demonstrating the initial slices and storage hardening work
```

### Step 5: Review Code
- **Model**: `src/main/java/com/astraNotes/model/Note.java` (clean domain)
- **Storage**: `src/main/java/com/astraNotes/storage/SQLiteNoteStorage.java` (core logic)
- **Crypto**: `src/main/java/com/astraNotes/encryption/EncryptionManager.java` (security)
- **UI**: `src/main/java/com/astraNotes/ui/` (5 components)

### Step 6: Review Documentation
- **Traceability**: [SUBMISSION_WEEK6.md - Section 5](SUBMISSION_WEEK6.md#5-traceability-matrix-slices--requirements--uml)
- **AI Reflection**: [SUBMISSION_WEEK6.md - Section 6](SUBMISSION_WEEK6.md#6-ai-reflection-how-copilot-helped)
- **Quality**: [SUBMISSION_WEEK6.md - Section 7](SUBMISSION_WEEK6.md#7-implementation-readiness-and-quality-findings)

---

## What's NOT Included (for Week 7)

✓ REQ-5: Full-text search implemented and benchmarked on current test dataset  
✓ REQ-7: Plugin hooks and plugin state persistence implemented at storage/service level  
✓ REQ-8: Export/import implemented and integration-tested  
⚠ GOV-1: Schema version table exists; multi-version migration scripts are future work  
❌ Performance testing (100k notes)  
❌ Cross-platform testing (Windows/Linux)  

---

## Summary

**Status**: ✓ READY FOR WEEK 6 REVIEW  

**What Was Built**:
- ✓ Maven-based Java project with proper structure
- ✓ 7 main components + 5 UI panels = ~1500 LOC
- ✓ 19 passing tests covering slices and storage hardening
- ✓ SQLite database with AES-GCM note-body encryption
- ✓ Full menu/dialog/list UI
- ✓ Comprehensive documentation

**Traceability**:
- ✓ REQ-1, REQ-2, REQ-4, REQ-5, REQ-6 implemented; REQ-3 storage update implemented but edit UI remains future work
- ✓ REQ-SEC-1, SEC-2, SEC-3, SEC-4 fully implemented
- ✓ User Stories 1-4 completed (Story 5 partial)
- ✓ All implementations linked to code in docstrings

**Quality**:
- ✓ Compiles without warnings
- ✓ All tests passing
- ✓ Logging configured
- ✓ Exception handling with rollback
- ✓ Separation of concerns (model/storage/encryption/ui)

**AI Use**:
- ✓ Copilot used for 40% scaffolding
- ✓ 60% manually architected (especially security/transactions)
- ✓ Clear documentation of what was changed and why

---

**Next**: Submit [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) as primary deliverable.
