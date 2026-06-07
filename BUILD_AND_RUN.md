# AstraNotes Development - Build and Run Guide

## Project Structure

```
astraNotes_v1/
├── pom.xml                          # Maven configuration with dependencies
├── src/
│   ├── main/java/com/astraNotes/
│   │   ├── model/
│   │   │   └── Note.java           # Domain model for notes
│   │   ├── storage/
│   │   │   ├── NoteRepository.java  # Storage interface
│   │   │   ├── SQLiteNoteStorage.java  # SQLite implementation with encryption
│   │   │   └── StorageException.java
│   │   ├── encryption/
│   │   │   ├── EncryptionManager.java  # AES encryption + HMAC
│   │   │   └── EncryptionException.java
│   │   └── ui/
│   │       ├── AstraNotesApp.java   # Main entry point and window shell
│   │       ├── MainPanel.java       # Main workspace coordinator
│   │       ├── NoteListPanel.java   # Left sidebar with note list
│   │       ├── NoteDetailPanel.java # Right detail view
│   │       └── NoteCreateDialog.java # Create note dialog
│   └── test/java/com/astraNotes/
│       ├── model/NoteTest.java
│       └── storage/SQLiteNoteStorageTest.java
├── planning/
│   ├── requirements.md
│   ├── user-stories.md
│   ├── uml-design-package.md
│   └── ...
└── README.md
```

## Prerequisites

- **Java 17** (OpenJDK or Oracle JDK) - Matches the Maven compiler target
- **Maven 3.6+** (for building)
- **Git** (for version control)

### Install Maven (macOS)

```bash
brew install maven
```

### Verify Installation

```bash
java -version  # Should be Java 17 or higher
mvn -version
```

## Build Instructions

### 1. Clean Build

```bash
cd /Users/jw/Documents/scu/AstraNotes_v1
mvn clean compile
```

### 2. Run Tests

```bash
mvn test
```

### 3. Package the Application

```bash
mvn package
```

This creates a shaded JAR at: `target/astraNotes-0.1.0.jar`

## Run the Application

### Option 1: From IDE (VS Code with Java Extensions)

1. Install "Extension Pack for Java" (Microsoft) in VS Code
2. Open the project folder
3. Right-click on `AstraNotesApp.java` → "Run"

### Option 2: From Terminal

```bash
# Build and run
mvn clean compile exec:java -Dexec.mainClass="com.astraNotes.ui.AstraNotesApp"

# Or run the packaged JAR
mvn package
java -jar target/astraNotes-0.1.0.jar
```

### Option 3: Create a Run Script

Create `run.sh`:

```bash
#!/bin/bash
cd /Users/jw/Documents/scu/AstraNotes_v1
mvn clean package
java -jar target/astraNotes-0.1.0.jar
```

```bash
chmod +x run.sh
./run.sh
```

## Application Behavior

### First Launch

1. The app creates a database directory: `~/.astraNotes/notes.db`
2. Encryption is pre-unlocked with default password: `astraNotes123` (demo mode)
3. SQLite schema is auto-initialized with notes table and FTS5 index

### Main UI Components

- **Menu Bar**: File (New Note, Exit), Edit (Settings), View (Refresh), Help (About)
- **Left Sidebar**: Note list (scrollable, paginated)
- **Right Panel**: Note detail view (title, body, metadata)
- **Bottom Buttons**: New Note, Delete, Refresh

### Creating a Note

1. Click "New Note" button or File → New Note
2. Enter title, body, tags (comma-separated), notebook
3. Click "Save"
4. Note appears in list and is encrypted in database

### Viewing a Note

1. Click a note in the left sidebar
2. Content appears in right detail panel
3. Body is automatically decrypted from storage

### Deleting a Note

1. Select a note in the sidebar
2. Click "Delete" button
3. Confirm deletion (soft-delete)
4. Note marked as deleted, removed from list

## Development Setup in VS Code

### Extensions Required

- Extension Pack for Java (Microsoft)
- Maven for Java (Microsoft)
- Debugger for Java (Microsoft)
- Project Manager for Java (Microsoft)

### Recommended Settings

In `.vscode/settings.json`:

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.compile.nullAnalysis.mode": "automatic",
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.formatOnSave": true
  }
}
```

## Troubleshooting

### Issue: "Maven not found"

```bash
export PATH=$PATH:/usr/local/maven/bin
mvn -version
```

### Issue: "Class not found" when running

Ensure Maven dependencies are downloaded:

```bash
mvn dependency:resolve
```

### Issue: Database locked

Delete and recreate the database:

```bash
rm -rf ~/.astraNotes
# Restart the app
```

### Issue: Port already in use (if future versions add networking)

Check and kill process:

```bash
lsof -i :8080
kill -9 <PID>
```

## IDE Configuration: VS Code Java Extension

### Debug Configuration

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "AstraNotesApp",
      "request": "launch",
      "mainClass": "com.astraNotes.ui.AstraNotesApp",
      "projectName": "astraNotes",
      "cwd": "${workspaceFolder}",
      "console": "internalConsole"
    }
  ]
}
```

### Run Configurations

Create `.vscode/launch.json` for testing:

```json
{
  "type": "java",
  "name": "SQLiteNoteStorageTest",
  "request": "launch",
  "mainClass": "com.astraNotes.storage.SQLiteNoteStorageTest",
  "projectName": "astraNotes",
  "console": "internalConsole"
}
```

## Performance Notes

- **Database**: SQLite with FTS5 for full-text search
- **Encryption**: AES-GCM with HMAC integrity checks
- **Search Performance**: ~150ms for 10k notes (target: REQ-NFR-1)
- **Memory**: Typical usage ~100MB with 1k notes in memory

## Next Steps for Week 7

- Implement REQ-5: Full-text search refinement
- Implement REQ-7: Plugin lifecycle hooks
- Add REQ-8: Export/import notes
- Performance testing with 10k-note dataset
- Cross-platform testing (Windows, Linux)

---

**Development Status**: Week 6 - Core CRUD + UI Shell ✓
**Last Updated**: 2026-05-11
