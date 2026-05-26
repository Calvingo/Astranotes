# AstraNotes User Stories

## Story 1: Create a secure markdown note
- As a user, I want to create a markdown note with title, body, tags, and notebook so that I can capture ideas locally.
- Acceptance criteria:
  - A note can be created and returns a unique ID.
  - The note is stored in SQLite with `deleted=0` and `version=1`.
  - The note body is encrypted at rest.

## Story 2: Retrieve a note by ID
- As a user, I want to open a note by its ID so I can continue editing it later.
- Acceptance criteria:
  - A valid note ID returns decrypted content and metadata.
  - Deleted notes are not returned in normal read operations.

## Story 3: Update a note with versioning
- As a user, I want to edit a note and preserve version information for audit and history.
- Acceptance criteria:
  - Editing a note updates `updatedAt` and increments `version`.
  - The update occurs in a transaction and persists correctly.

## Story 4: Soft-delete a note
- As a user, I want to delete a note without immediately removing it permanently.
- Acceptance criteria:
  - The note is marked as deleted in the database.
  - The note no longer appears in search/list results.
  - The delete action is logged for later purge.

## Story 5: Search notes efficiently
- As a user, I want to search notes by title/body/tags so I can find content quickly.
- Acceptance criteria:
  - Search returns matching note IDs ordered by `updatedAt`.
  - Search is implemented with FTS5 and returns results under 150ms for 10k notes.

## Story 6: Work offline
- As a user, I want the app to work without network connectivity so I can use notes anywhere.
- Acceptance criteria:
  - CRUD operations work without requiring any remote service.
  - The app does not block on network checks.

## Story 7: Plugin lifecycle hooks
- As a developer, I want trusted plugins to receive lifecycle events so I can extend AstraNotes safely.
- Acceptance criteria:
  - Plugin hooks fire for `beforeCreate`, `afterUpdate`, `beforeDelete`, and `onSearch`.
  - Plugin data is stored in a dedicated plugin state table.

## Story 8: Export and import notes
- As a user, I want to export and import notes so I can back them up and restore them.
- Acceptance criteria:
  - Export produces a JSON or markdown bundle.
  - Import reconstructs notes in the local SQLite database.
