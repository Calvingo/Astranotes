# AstraNotes Refined Requirement Baseline

## Refined Requirement Baseline

### Functional Requirements

1. **Create note**
   - The app must allow users to create a markdown note with title, body, tags, and notebook.
   - The note must be saved in the encrypted SQLite database and return a unique note ID.
   - The saved note must include `createdAt`, `updatedAt`, `version`, and `deleted` metadata.

2. **Read note by ID**
   - The app must retrieve a note by its unique ID and return decrypted body and metadata.
   - If the note is soft-deleted, the normal read API must return not found.

3. **Update note**
   - Users must be able to edit an existing note's title, body, tags, and notebook.
   - The note update must occur in a transaction, increment `version`, and update `updatedAt`.

4. **Soft delete note**
   - The app must mark a note as deleted without physically removing it from the database.
   - Soft-deleted notes must be excluded from search and list results.

5. **Search notes**
   - The app must support full-text search on title, body, and tags using FTS5.
   - Search results must be ordered by `updatedAt` desc and support pagination.

6. **Offline operation**
   - All CRUD and search operations must function without network connectivity.
   - The app must not require any remote service for basic note workflows.

7. **Plugin lifecycle hooks**
   - Trusted plugins must be able to receive `beforeCreate`, `afterUpdate`, `beforeDelete`, and `onSearch` events.
   - Plugin state must be stored separately from core note data.

8. **Export/import notes**
   - The app must support exporting notes to a file bundle and importing them back.
   - Import must reconstruct notes into the encrypted SQLite database with metadata intact.

### Non-functional Requirements

1. **Performance**
   - Search/list queries should return results in under 150ms for a 10k-note dataset on typical desktop hardware.

2. **Scalability**
   - The app should support 100k notes with acceptable query performance, stable memory usage, and a manageable database footprint.

3. **Cross-platform**
   - The app must function on macOS, Windows, and Linux with consistent behavior and secure local storage.

4. **Maintainability**
   - Code should separate storage, encryption, plugin, and UI layers.
   - Public APIs and decisions must be documented.

### Security / Privacy / Reliability / Governance Requirements

1. **Encryption at rest**
   - The SQLite database must be encrypted using SQLCipher or a secure equivalent.

2. **Integrity checks**
   - Each note record must include an integrity validation mechanism, such as HMAC.

3. **Access control**
   - A password or unlock key must be required to open encrypted note data.

4. **ACID reliability**
   - All write operations must use SQLite transactions to prevent partial or inconsistent state.

5. **Schema governance**
   - Schema versioning and migration scripts must be tracked, documented, and applied automatically.

6. **Deletion governance**
   - Deleted notes must be tombstoned and only permanently removed via explicit purge.
   - No unencrypted note content should remain on disk after purge.

## Ambiguity Review

### Ambiguous items identified
1. **"Safe export/import"**
   - Previously vague: does it mean encrypted export, plaintext export, or both?
   - Refined to: export bundle format plus explicit import path back into encrypted DB.

2. **"Trusted plugins"**
   - Originally ambiguous on permission boundaries.
   - Clarified as plugins with explicit event hooks and separate plugin state storage, not arbitrary file access.

3. **"Offline operation"**
   - Ambiguous whether only CRUD or also plugin and export/import should work offline.
   - Refined to require CRUD and search offline, and to not depend on remote services.

4. **"Manageable DB size"**
   - Subjective phrase.
   - Refined to support 100k notes without unacceptable query performance or memory usage.

### Weak assumptions
1. Assuming all users can tolerate a single SQLite DB on all platforms.
   - This is valid for desktop MVP but should be explicit as a design choice.
2. Assuming FTS5 search is sufficient for all note content.
   - Acceptable now, but we must note that larger attachments or external indexing are out of scope.
3. Assuming plugin hooks are safe because plugins are trusted.
   - Needs explicit governance: trusted means audited, not sandboxed.

## Edge-case Review

### Missing edge cases added
1. **Empty note title or body**
   - Requirement should allow empty body, but title may be optional or required. Clarified to support both with explicit validation rules.

2. **Maximum size limits**
   - Notes should have a practical max body size (e.g. 1MB) to keep performance and storage predictable.

3. **Duplicate IDs on import**
   - Import must handle existing note IDs gracefully (e.g. merge, skip, or create new ID).

4. **Deleted note read attempts**
   - Clarified that soft-deleted notes are not returned by normal read/list/search.

5. **Database unlock failure**
   - If the encryption key is wrong or missing, the app must fail gracefully and not expose data.

6. **Migration failure**
   - Schema migration must fail safely with a clear rollback or recovery path.

7. **Plugin error handling**
   - A faulty plugin must not crash core note operations.

8. **Export/import data integrity**
   - Exported bundles must include a version or checksum to validate import.

## Functional vs Non-functional Notes

- **Functional requirements** describe what the system does:
  - create/read/update/delete notes
  - search notes
  - export/import notes
  - plugin lifecycle events
  - offline operation

- **Non-functional requirements** describe how well the system does it:
  - performance under load
  - scalability to 100k notes
  - cross-platform behavior
  - maintainability of architecture
  - security and governance constraints

## AI refinement note

AI helped by generating the first draft of requirements, user stories, acceptance criteria, and governance structure. I then refined that output by removing generic Agile language, making assumptions explicit, and converting vague statements into measurable requirements. I rejected AI output that lacked edge-case coverage or clear separation between functional and non-functional requirements, and I strengthened the baseline by adding explicit failure modes for export/import, migration, plugin errors, and encryption unlock.
