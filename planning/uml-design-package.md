# AstraNotes UML Design Package

## Overview
This document contains five coordinated UML views of the AstraNotes system: class diagram, object diagram, use case diagram, activity diagram, and deployment diagram. All views reflect the SQLite local-first architecture with encryption and trusted plugins.

---

## 1) Class Diagram

```
package model {
  class Note {
    id: String (UUID)
    title: String
    body: String (encrypted blob)
    tags: List<String>
    notebook: String
    createdAt: Instant
    updatedAt: Instant
    version: Integer
    deleted: Boolean
    hmac: byte[]
    
    + getId(): String
    + getTitle(): String
    + setTitle(String): void
    + getBody(): String
    + setBody(String): void
    + getTags(): List<String>
    + getVersion(): Integer
  }
}

package storage {
  interface NoteRepository {
    + create(title, body, tags, notebook): String
    + get(id): Optional<Note>
    + update(id, title, body, tags): boolean
    + delete(id): boolean
    + list(offset, limit): List<Note>
    + search(query, offset, limit): List<Note>
  }
  
  class SQLiteNoteStorage implements NoteRepository {
    - dbPath: String
    - connection: Connection
    - encryptionManager: EncryptionManager
    
    + create(title, body, tags, notebook): String
    + get(id): Optional<Note>
    + update(id, title, body, tags): boolean
    + delete(id): boolean
    + list(offset, limit): List<Note>
    + search(query, offset, limit): List<Note>
    - executeQuery(sql, params): ResultSet
    - persistNote(note): void
  }
  
  class MigrationManager {
    - currentVersion: Integer
    - migrations: List<Migration>
    
    + migrate(connection): boolean
    + validate(): boolean
  }
}

package encryption {
  class EncryptionManager {
    - rootKey: byte[]
    - keyStore: KeyStore
    
    + unlock(password): boolean
    + lock(): void
    + encrypt(plaintext): byte[]
    + decrypt(ciphertext): byte[]
    + computeHMAC(noteId, body): byte[]
    + verifyHMAC(noteId, body, hmac): boolean
  }
}

package plugin {
  interface PluginContext {
    + getNoteService(): NoteRepository
    + getMetadataService(): MetadataService
  }
  
  class PluginManager {
    - plugins: Map<String, PluginInstance>
    - pluginContext: PluginContext
    
    + loadPlugin(jar): boolean
    + onNoteCreated(noteId): void
    + onNoteUpdated(noteId): void
    + onNoteDeleted(noteId): void
    + onSearch(query): void
  }
  
  interface ITrustedPlugin {
    + initialize(context): void
    + onNoteCreated(note): void
    + onNoteUpdated(note): void
    + onNoteDeleted(noteId): void
    + shutdown(): void
  }
}

package indexing {
  class FTS5SearchIndex {
    - ftsTableName: String = "fts_notes"
    
    + indexNote(note): void
    + removeNote(noteId): void
    + search(query, limit, offset): List<SearchResult>
  }
}

package io {
  class ExportImportService {
    - storage: NoteRepository
    
    + exportNotes(noteIds, format): File
    + importNotes(file): List<Note>
  }
}

// Relationships
NoteRepository <-- SQLiteNoteStorage
SQLiteNoteStorage --> EncryptionManager : uses
SQLiteNoteStorage --> MigrationManager : uses
SQLiteNoteStorage --> FTS5SearchIndex : uses
PluginManager --> PluginContext : manages
PluginManager --> ITrustedPlugin : invokes
ExportImportService --> NoteRepository : uses
ExportImportService --> EncryptionManager : uses
```

**Key design principles:**
- `NoteRepository` is the main interface; `SQLiteNoteStorage` implements it.
- `EncryptionManager` handles all encryption/decryption and HMAC operations.
- `PluginManager` receives events from storage and invokes plugin hooks.
- `FTS5SearchIndex` handles full-text indexing and search.
- `MigrationManager` handles schema versioning and migrations.

---

## 2) Object Diagram

```
object appContext {
  encryptionManager : EncryptionManager
  noteStorage : SQLiteNoteStorage
  pluginManager : PluginManager
  ftsIndex : FTS5SearchIndex
}

object noteInstance1 : Note {
  id = "550e8400-e29b-41d4-a716-446655440000"
  title = "Meeting Notes"
  body = [encrypted blob]
  version = 2
  deleted = false
  hmac = [32 bytes]
}

object noteInstance2 : Note {
  id = "6ba7b810-9dad-11d1-80b4-00c04fd430c8"
  title = "Deleted Note"
  body = [encrypted blob]
  version = 1
  deleted = true
  hmac = [32 bytes]
}

object searchResult1 {
  noteId = "550e8400-e29b-41d4-a716-446655440000"
  snippet = "Meeting at 3pm..."
  rank = 1.5
}

appContext.noteStorage --> noteInstance1
appContext.noteStorage --> noteInstance2
appContext.ftsIndex --> searchResult1
appContext.encryptionManager --> noteInstance1 : encrypts/decrypts
```

**Interpretation:**
- At runtime, one `appContext` manages a single `SQLiteNoteStorage` and `EncryptionManager`.
- Multiple `Note` objects exist in the storage, with `version` and `deleted` flags.
- `FTS5SearchIndex` maintains search results indexed by `noteId`.

---

## 3) Use Case Diagram

```
actor User
actor Plugin

User --> (Create Note)
User --> (Read Note)
User --> (Update Note)
User --> (Delete Note)
User --> (Search Notes)
User --> (Export Notes)
User --> (Import Notes)
User --> (Lock/Unlock Vault)

Plugin --> (Receive Note Event)
Plugin --> (Query Notes)

(Create Note) --> (Increment Version)
(Create Note) --> (Encrypt Body)
(Update Note) --> (Increment Version)
(Update Note) --> (Update FTS Index)
(Delete Note) --> (Mark Deleted)
(Search Notes) --> (Query FTS Index)
(Lock/Unlock Vault) --> (Manage Encryption Key)
(Receive Note Event) <-- (Create Note)
(Receive Note Event) <-- (Update Note)
(Receive Note Event) <-- (Delete Note)
```

**Key actors:**
- **User**: performs CRUD, search, export/import, and vault management.
- **Plugin**: receives lifecycle events and can query notes.

**Key use cases:**
- Core CRUD: create, read, update, delete.
- Search and indexing.
- Export/import for backup.
- Vault lock/unlock for key management.
- Plugin event hooks.

---

## 4) Activity Diagram

### Create Note Activity
```
start
  --> [User inputs title, body, tags, notebook]
  --> (Validate input)
  --> {Valid?}
  --> [Yes] (Generate UUID)
  --> (Encrypt body with root key)
  --> (Compute HMAC)
  --> (Insert into SQLite transaction)
  --> (Update FTS5 index)
  --> (Commit transaction)
  --> (Notify plugins: beforeCreate, afterCreate)
  --> [Return note ID to user]
  --> end
  
{Valid?} --> [No] (Display error)
  --> end
```

### Search Note Activity
```
start
  --> [User enters search query]
  --> (Tokenize query)
  --> (Query FTS5 index)
  --> {Results found?}
  --> [Yes] (Retrieve note IDs and metadata)
  --> (Decrypt note previews if needed)
  --> (Order by updatedAt DESC)
  --> (Paginate results)
  --> [Display search results]
  --> end
  
{Results found?} --> [No] (Display "No notes found")
  --> end
```

### Export/Import Cycle
```
start
  --> [User clicks Export]
  --> (Select notes to export)
  --> {Export format?}
  --> [JSON] (Serialize notes with metadata)
  --> [Markdown] (Convert body to .md files)
  --> (Add version/checksum to bundle)
  --> (Write to file)
  --> [User saves file]
  --> end
  
  [Later, user clicks Import]
  --> (Select import file)
  --> (Validate checksum)
  --> (Parse file format)
  --> {Duplicate IDs?}
  --> [Yes] (Assign new UUIDs)
  --> [No] (Keep existing IDs)
  --> (Insert into SQLite)
  --> (Update FTS5 index)
  --> [Confirm import success]
  --> end
```

---

## 5) Deployment Diagram

```
artifact astranotes.jar {
  component {
    "note.jar" (model.Note)
    "storage.jar" (SQLiteNoteStorage, MigrationManager)
    "encryption.jar" (EncryptionManager)
    "plugin.jar" (PluginManager, ITrustedPlugin)
    "indexing.jar" (FTS5SearchIndex)
    "io.jar" (ExportImportService)
  }
}

database sqlite_db {
  "astranotes.db" (encrypted with SQLCipher)
}

node UserDesktop {
  device macOS {
    astranotes.jar --> sqlite_db : CRUD + encrypt/decrypt
    astranotes.jar --> Keychain : unlock vault
  }
  
  device Windows {
    astranotes.jar --> sqlite_db : CRUD + encrypt/decrypt
    astranotes.jar --> DPAPI : unlock vault
  }
  
  device Linux {
    astranotes.jar --> sqlite_db : CRUD + encrypt/decrypt
    astranotes.jar --> libsecret : unlock vault
  }
}

note on macOS : Use Keychain for secure key storage
note on Windows : Use DPAPI for secure key storage
note on Linux : Use libsecret fallback or password-based KDF

filesystem {
  "~/.astranotes/config" : App config
  "~/.astranotes/notes.db" : Encrypted SQLite DB
  "~/.astranotes/backup/" : Periodic snapshots
}

astranotes.jar --> filesystem : Read/write local files
```

**Deployment architecture:**
- Single monolithic JAR deployed on user's desktop.
- Encrypted SQLite DB stored in standard platform directories.
- Platform-specific key storage: Keychain (macOS), DPAPI (Windows), libsecret (Linux).
- No network dependencies; fully offline.

---

## Rationale: How the Views Fit Together

### Class Diagram → Runtime
The **class diagram** defines the static structure. At runtime, the **object diagram** shows how instances of those classes interact: a user creates a `Note` object, `SQLiteNoteStorage` persists it, and `EncryptionManager` encrypts it.

### Use Cases → Activities
The **use case diagram** lists what the system does (create, read, search, export). The **activity diagrams** show the step-by-step flow of those use cases: input validation, encryption, indexing, transaction management, and plugin notification.

### Deployment → Architecture
The **deployment diagram** shows where and how the system runs: as a JAR on the user's desktop, with local SQLite storage and platform-specific key management. It reflects the "local-first, no network" design constraint.

### Consistency Across Views
- **Class → Use Case**: Every use case is supported by a class that implements it (e.g., `NoteRepository.search()` supports the "Search Notes" use case).
- **Use Case → Activity**: Every use case has an activity diagram that details its workflow.
- **Activity → Deployment**: Activities execute within the deployed JAR and interact with the local SQLite DB and key storage.
- **Object → Deployment**: Runtime objects persist to the SQLite DB on the user's desktop, encrypted and indexed.

### Design Alignment
- **Encryption-first**: every view emphasizes encryption (class: `EncryptionManager`, activity: "Encrypt body", deployment: "SQLCipher DB").
- **Plugin-aware**: use cases and activities include plugin hooks; class diagram shows `PluginManager` and `ITrustedPlugin`.
- **Offline-only**: no network components in deployment; all storage is local.
- **Version and audit**: every view reflects versioning (class: `version` field, activity: "Increment Version", object: version instances).

---

## Summary

This UML package provides a complete architectural view of AstraNotes:
1. **Class diagram** defines the modular structure.
2. **Object diagram** shows runtime instances and relationships.
3. **Use case diagram** lists user and plugin interactions.
4. **Activity diagrams** detail the workflows for key operations.
5. **Deployment diagram** shows physical architecture and platform dependencies.

An engineer new to the project can read these diagrams in sequence and understand:
- What components exist and how they interact.
- What the system does for users and plugins.
- How operations flow through the system.
- Where and how the system runs.
