# AstraNotes Backlog

This backlog converts the selected early requirements and user stories into a practical implementation order. Items are intentionally small enough to guide early work without becoming a full Jira-style system.

## High Priority
1. FR1: Create note
   - Task: Implement `SQLiteNoteStorage.create()` with encryption and version tracking
   - Acceptance: note stored, encrypted, version=1, returns ID
   - Depends on: SQLite schema and `EncryptionManager`
2. FR2: Read note by ID
   - Task: Implement `SQLiteNoteStorage.get()` with decryption and deleted-note filtering
   - Acceptance: valid note returns content; deleted note returns not found
   - Depends on: FR1 create flow and HMAC verification
3. FR3: Update note
   - Task: Implement `SQLiteNoteStorage.update()` and version increment
   - Acceptance: updatedAt changes, version++, note still encrypted
   - Depends on: FR1 and FR2
4. SEC1: Encryption at rest
   - Task: Implement application-level AES-GCM for note bodies and document optional SQLCipher path
   - Acceptance: note body is unreadable without unlock key; unlock works with password

## Medium Priority
5. FR4: Soft delete note
   - Task: Implement delete flag, tombstone, and list filtering
   - Acceptance: deleted note hidden from search/list; purge separate
   - Depends on: FR1 and FR2
6. FR5: Search notes
   - Task: Create FTS5 schema and search API
   - Acceptance: search returns matches under 150ms for 10k notes
   - Depends on: notes table, FTS table, and deleted-note filtering
7. FR6: Offline operation
   - Task: Validate app workflows without any network dependencies
   - Acceptance: all CRUD operations work offline
   - Depends on: local SQLite storage and Maven build

## Low Priority
8. FR7: Plugin hooks
   - Task: Build trusted plugin event interface and storage area
   - Acceptance: hooks fire and plugin state persists
   - Depends on: stable note lifecycle events
9. FR8: Export/import notes
   - Task: Implement note export/import file formats
   - Acceptance: export bundle restores correctly
   - Depends on: stable CRUD, encryption, and integrity verification

## Tech backlog / governance
- NFR1: Add performance benchmark tests for search/list
- SEC2: Add integrity check tests for note HMAC
- GOV1: Add schema migration versioning and `plans/DECISIONS.md` entry
- Docs: Update `plan-astraNotes.prompt.md` when architecture changes

## Priority rationale

Create, read, update, and encryption are high priority because every later feature depends on durable, protected note storage. Search, soft delete, and offline validation come next because they improve the core user workflow after the storage foundation exists. Plugin hooks and export/import are lower priority for early planning because they depend on a stable note lifecycle and security model.

## Sequencing dependencies

- Search depends on notes being created, indexed, and filtered for deleted state.
- Export/import depends on a stable note model, encryption behavior, and integrity verification.
- Plugin hooks depend on clear lifecycle points in create, update, delete, and search.
- Performance testing depends on search/list APIs existing first.
