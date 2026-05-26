# AstraNotes Requirements

## Core Functional Requirements

1. Create note
   - The app must allow users to create a markdown note with title, body, tags, and notebook.
2. Read note
   - The app must load a note by ID and display decrypted content and metadata.
3. Update note
   - Users must be able to edit an existing note, incrementing version and updating timestamps.
4. Delete note
   - Notes must be soft-deleted first, with the option to purge later.
5. Search notes
   - Users must be able to search title/body/tags using full-text search.
6. Offline operation
   - All CRUD work must function without network access.
7. Plugin hooks
   - Trusted plugins must receive lifecycle events for create/update/delete/search.
8. Backup/export/import
   - Users must be able to export and import notes safely.

## Non-functional Requirements

1. Performance
   - Search/list operations should complete under 150ms for a 10k-note dataset.
2. Scalability
   - The app should support 100k notes with manageable DB size and responsive queries.
3. Cross-platform
   - The solution must work on macOS, Windows, and Linux.
4. Maintainability
   - Storage, encryption, plugin, and UI code should be separated cleanly.

## Security, Privacy, Reliability, Governance Requirements

1. Encryption at rest
   - The SQLite database must be encrypted using SQLCipher or equivalent.
2. Integrity checks
   - Each note must include an integrity check such as HMAC.
3. Access control
   - The app must require a password or unlock key to access encrypted data.
4. ACID reliability
   - All writes must use SQLite transactions to prevent partial state.
5. Schema governance
   - Schema versioning and migration scripts must be tracked and logged.
6. Data deletion governance
   - Deleted notes must be tombstoned and purged explicitly, with no unencrypted leftovers.
