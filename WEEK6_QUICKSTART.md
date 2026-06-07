# Week 6 AstraNotes Development - Quick Start

## What's in This Repository

This is a **Java desktop application** for secure offline-first note-taking built during Week 6 of the AstraNotes lab.

**Status**: MVP with core CRUD + encryption working  
**Lines of Code**: ~1500 main + ~300 tests  
**Build**: Maven  
**Main Class**: `com.astraNotes.ui.AstraNotesApp`

---

## Quick Start (5 minutes)

### 1. Clone/Open the Project

```bash
cd /Users/jw/Documents/scu/AstraNotes_v1
```

### 2. Build

```bash
mvn clean package
```

### 3. Run

```bash
java -jar target/astraNotes-0.1.0.jar
```

Or from IDE (VS Code):
- Open folder in VS Code
- Install "Extension Pack for Java" (Microsoft)
- Run `AstraNotesApp.main()` via CodeLens

### 4. Test the App

**Create a note**:
1. Click "New Note" button
2. Enter: Title="Hello World", Body="This is my first note", Notebook="default"
3. Click Save
4. Note appears in left sidebar

**View a note**:
1. Click note in sidebar
2. Content decrypts and displays on right

**Delete a note**:
1. Select note, click Delete
2. Confirm
3. Note soft-deleted (marked deleted in DB, removed from list)

---

## What Was Built

### Core Components

| Component | Purpose | Status |
|-----------|---------|--------|
| Note Model | Domain object (title, body, tags, version, encryption) | ✓ Complete |
| SQLiteNoteStorage | Database layer with AES encryption | ✓ Complete |
| EncryptionManager | AES-GCM + HMAC for security | ✓ Complete |
| NoteCreateDialog | UI for creating notes | ✓ Complete |
| NoteListPanel | Sidebar with note list | ✓ Complete |
| NoteDetailPanel | Note display panel | ✓ Complete |
| AstraNotesApp | Main window + menu | ✓ Complete |

### Features Implemented

✓ **REQ-1**: Create note (with title, body, tags, notebook)  
✓ **REQ-2**: Read note (decrypt and display)  
✓ **REQ-3**: Update note (version + timestamp tracking)  
✓ **REQ-4**: Delete note (soft-delete, can recover)  
✓ **REQ-6**: Offline operation (SQLite local-only)  
✓ **REQ-SEC-1**: Encryption at rest (AES-GCM note-body encryption)  
✓ **REQ-SEC-2**: Integrity checks (HMAC-SHA256)  
✓ **REQ-SEC-3**: Access control (password unlock)  
✓ **REQ-SEC-4**: ACID transactions (SQLite + explicit commits)  

### Not Yet Implemented

✗ **REQ-5**: Full-text search (FTS5 schema ready, query optimization pending)  
✗ **REQ-7**: Plugin hooks (interface defined, lifecycle not wired)  
✗ **REQ-8**: Export/import (structure ready, UI not built)  
✓ **GOV-1**: Schema versioning table present; multi-version migration scripts are future work  

---

## Project Structure

```
src/main/java/com/astraNotes/
├── model/
│   └── Note.java              # Domain model
├── storage/
│   ├── NoteRepository.java     # Interface
│   ├── SQLiteNoteStorage.java  # Implementation + encryption
│   └── StorageException.java
├── encryption/
│   ├── EncryptionManager.java  # AES + HMAC
│   └── EncryptionException.java
└── ui/
    ├── AstraNotesApp.java      # Main + window
    ├── MainPanel.java          # Workspace coordinator
    ├── NoteListPanel.java      # List sidebar
    ├── NoteDetailPanel.java    # Detail view
    └── NoteCreateDialog.java   # Create dialog

src/test/java/com/astraNotes/
├── model/NoteTest.java
└── storage/SQLiteNoteStorageTest.java

pom.xml                         # Maven config
BUILD_AND_RUN.md               # Detailed build guide
SUBMISSION_WEEK6.md            # Full submission report
```

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Specific Test

```bash
mvn test -Dtest=SQLiteNoteStorageTest
```

### Test Coverage

- **testCreateNote**: Verifies note creation + encryption
- **testCreateAndList**: Verifies list retrieval
- **testReadNote**: Verifies decryption on read
- **testUpdateNote**: Verifies version increment
- **testSoftDelete**: Verifies soft-delete behavior
- **testSearch**: Verifies FTS5 search (basic)

---

## Database

**Location**: `~/.astraNotes/notes.db` (SQLite)  
**Auto-created**: On first launch  
**Schema**: Notes table + FTS5 full-text index  
**Encryption**: AES-GCM per-note body  

### Inspect Database

```bash
sqlite3 ~/.astraNotes/notes.db
sqlite> SELECT id, title, version, deleted FROM notes;
```

---

## Development with Copilot

This project was built with GitHub Copilot assistance. Here's what worked well:

✓ **Boilerplate**: Maven POM, UI layouts  
✓ **APIs**: JDBC patterns, Swing components  
✓ **Documentation**: Method docstrings  

Here's what required manual work:

✗ **Encryption**: Reviewed security, added key derivation  
✗ **Transactions**: Added commit/rollback logic  
✗ **Architecture**: Refactored for separation of concerns  
✗ **Testing**: Switched from unit to integration tests  

**Lesson**: Copilot is great for scaffolding but needs expert review for security and architecture.

---

## For Week 7

1. **Search Performance**: Benchmark FTS5 on 10k notes
2. **Plugin System**: Wire lifecycle events
3. **Export/Import**: JSON-based backup
4. **Cross-platform**: Test on Windows/Linux
5. **UI Refinement**: Background loading, infinite scroll

---

## Troubleshooting

**"Maven not found"**
```bash
brew install maven  # macOS
```

**"Database locked"**
```bash
rm -rf ~/.astraNotes
# Restart app
```

**"Tests fail"**
```bash
mvn clean test -X  # Verbose output
```

**"Can't find main class"**
```bash
mvn clean compile
# Then run from IDE or: java -cp target/classes com.astraNotes.ui.AstraNotesApp
```

---

## Files to Review

1. **[SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md)** ← Full submission report (requirements, traceability, AI reflection)
2. **[BUILD_AND_RUN.md](BUILD_AND_RUN.md)** ← Detailed build/run/debug instructions
3. **pom.xml** ← Dependencies and build config
4. **src/main/java/com/astraNotes/storage/SQLiteNoteStorage.java** ← Core CRUD + encryption
5. **src/main/java/com/astraNotes/encryption/EncryptionManager.java** ← Crypto implementation
6. **src/test/java/com/astraNotes/storage/SQLiteNoteStorageTest.java** ← Integration tests

---

## Questions?

See [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) section "How to Evaluate This Submission" for review guidance.
