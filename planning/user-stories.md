# AstraNotes User Stories

These stories are selected from the early AstraNotes requirements and checked against the Week 2.1 Working Agreement and Definition of Done. They focus on the first practical planning layer rather than the entire future product.

## Story 1: Create a secure markdown note
- Requirement source: FR1, SEC1, SEC2
- As a user, I want to create a markdown note with title, body, tags, and notebook so that I can capture ideas locally.
- Acceptance criteria:
  - A note can be created and returns a unique ID.
  - The note is stored in SQLite with `deleted=0` and `version=1`.
  - The note body is encrypted at rest.
  - The stored note includes an integrity check that can be verified later.

## Story 2: Retrieve a note by ID
- Requirement source: FR2, SEC2
- As a user, I want to open a note by its ID so I can continue editing it later.
- Acceptance criteria:
  - A valid note ID returns decrypted content and metadata.
  - The note body is verified before it is returned to the UI.
  - Deleted notes are not returned in normal read operations.

## Story 3: Update a note with versioning
- Requirement source: FR3, ACID reliability
- As a user, I want to edit a note and preserve version information for audit and history.
- Acceptance criteria:
  - Editing a note updates `updatedAt` and increments `version`.
  - The update occurs in a transaction and persists correctly.
  - The updated body is re-encrypted and the integrity check is refreshed.

## Story 4: Soft-delete a note
- Requirement source: FR4, data deletion governance
- As a user, I want to delete a note without immediately removing it permanently.
- Acceptance criteria:
  - The note is marked as deleted in the database.
  - The note no longer appears in search/list results.
  - A separate purge action can permanently remove deleted notes.

## Story 5: Search notes efficiently
- Requirement source: FR5, NFR performance
- As a user, I want to search notes by title/body/tags so I can find content quickly.
- Acceptance criteria:
  - Search returns matching note IDs ordered by `updatedAt`.
  - Search is implemented with FTS5 and returns results under 150ms for 10k notes.
  - Deleted notes do not appear in search results.

## Story 6: Work offline
- Requirement source: FR6
- As a user, I want the app to work without network connectivity so I can use notes anywhere.
- Acceptance criteria:
  - CRUD operations work without requiring any remote service.
  - The app does not block on network checks.
  - The local SQLite database remains the source of truth.

## Story 7: Plugin lifecycle hooks
- Requirement source: FR7, governance
- As a developer, I want trusted plugins to receive lifecycle events so I can extend AstraNotes safely.
- Acceptance criteria:
  - Plugin hooks fire for `beforeCreate`, `afterUpdate`, `beforeDelete`, and `onSearch`.
  - Plugin data is stored in a dedicated plugin state table.
  - Plugin errors do not corrupt core note storage.

## Story 8: Export and import notes
- Requirement source: FR8, privacy/governance
- As a user, I want to export and import notes so I can back them up and restore them.
- Acceptance criteria:
  - Export produces a JSON bundle with note metadata and protected note body content.
  - Import reconstructs notes in the local SQLite database.
  - Import verifies bundle integrity before writing notes.
