# AstraNotes Backlog

## Priority 1
1. FR1: Create note
   - Task: Implement `SQLiteNoteStorage.create()` with encryption and version tracking
   - Acceptance: note stored, encrypted, version=1, returns ID
2. FR2: Read note by ID
   - Task: Implement `SQLiteNoteStorage.get()` with decryption and deleted-note filtering
   - Acceptance: valid note returns content; deleted note returns not found
3. FR3: Update note
   - Task: Implement `SQLiteNoteStorage.update()` and version increment
   - Acceptance: updatedAt changes, version++, note still encrypted
4. SEC1: Encryption at rest
   - Task: Integrate SQLCipher or secure file-level encryption for the DB
   - Acceptance: DB unreadable without key; unlock works with password

## Priority 2
5. FR4: Soft delete note
   - Task: Implement delete flag, tombstone, and list filtering
   - Acceptance: deleted note hidden from search/list; purge separate
6. FR5: Search notes
   - Task: Create FTS5 schema and search API
   - Acceptance: search returns matches under 150ms for 10k notes
7. FR6: Offline operation
   - Task: Validate app workflows without any network dependencies
   - Acceptance: all CRUD operations work offline

## Priority 3
8. FR7: Plugin hooks
   - Task: Build trusted plugin event interface and storage area
   - Acceptance: hooks fire and plugin state persists
9. FR8: Export/import notes
   - Task: Implement note export/import file formats
   - Acceptance: export bundle restores correctly

## Tech backlog / governance
- NFR1: Add performance benchmark tests for search/list
- SEC2: Add integrity check tests for note HMAC
- GOV1: Add schema migration versioning and DECISIONS.md entry
- Docs: Update `plan-astraNotes.prompt.md` when architecture changes
