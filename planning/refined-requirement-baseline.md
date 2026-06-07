# AstraNotes Refined Requirement Baseline

This baseline refines the Week 1.2 requirements and Week 2.2 stories into clearer, testable requirements for AstraNotes. The goal is not to add scope; it is to remove ambiguity, expose edge cases, and make the current project direction defensible.

## Refined Requirement Baseline

### Functional Requirements

#### FR-1: Create Note
- The system shall allow a user to create a markdown note with title, body, tags, and notebook.
- The system shall generate a unique note ID.
- The system shall store the note locally in SQLite.
- The system shall initialize `createdAt`, `updatedAt`, `version=1`, and `deleted=false`.
- The system shall encrypt protected note body content before writing it to disk.

Acceptance checks:
- Creating a valid note returns a non-empty unique ID.
- The saved note can be read back with matching title, body, tags, and notebook.
- The stored body is not written as plain text.

#### FR-2: Read Note
- The system shall retrieve a note by ID and return decrypted body content and metadata.
- The system shall verify note integrity before returning the note.
- The system shall not return soft-deleted notes through normal read operations.

Acceptance checks:
- A valid active note ID returns the expected note.
- A missing or deleted note ID returns not found.
- A tampered note fails integrity validation instead of returning corrupted content.

#### FR-3: Update Note
- The system shall allow a user to update an existing note's title, body, tags, and notebook.
- The system shall perform the update in a transaction.
- The system shall increment `version`, update `updatedAt`, re-encrypt the note body, and refresh the integrity check.

Acceptance checks:
- Updating an active note returns success.
- The new content persists after reload.
- The version increases by one.
- A failed update does not partially change the stored note.

#### FR-4: Delete and Purge Note
- The system shall support soft delete by marking a note as deleted.
- Soft-deleted notes shall be excluded from read, list, and search operations.
- The system shall support explicit purge to permanently remove deleted notes.

Acceptance checks:
- A deleted note no longer appears in normal workflows.
- Purge removes a deleted note from the database.
- Purge is separate from normal delete.

#### FR-5: Search Notes
- The system shall support full-text search over note title, body snippet, tags, and notebook.
- Search shall support pagination.
- Search shall order results by most recently updated notes first.
- Search shall exclude deleted notes.

Acceptance checks:
- Searching a matching title, body term, or tag returns expected notes.
- Searching a deleted note's content returns no result.
- Search supports offset and limit.

#### FR-6: Offline Operation
- The system shall support create, read, update, delete, list, and search without network connectivity.
- The system shall use local SQLite storage as the source of truth.
- The system shall not require a remote account, remote API, or cloud service for core workflows.

Acceptance checks:
- Core CRUD and search work while disconnected.
- The application does not block startup or note operations on network checks.

#### FR-7: Trusted Plugin Hooks
- The system shall expose trusted plugin hooks for `beforeCreate`, `afterUpdate`, `beforeDelete`, and `onSearch`.
- Plugin state shall be stored separately from core note rows.
- Plugin failures shall be logged and handled without corrupting core storage.

Acceptance checks:
- Registered plugins receive the expected lifecycle event.
- A plugin can veto supported operations where the hook allows it.
- Plugin state persists separately from note data.

#### FR-8: Export and Import Notes
- The system shall export selected notes to a versioned file bundle.
- The bundle shall include enough metadata to reconstruct notes.
- The bundle shall include an integrity check such as a checksum.
- The import process shall verify bundle integrity before writing notes.
- Import shall handle duplicate note IDs by preserving IDs only when safe or creating new IDs when requested.

Acceptance checks:
- Export creates a readable bundle file with a format version.
- Import into a fresh database reconstructs the exported note.
- A modified bundle with a bad checksum is rejected.

### Non-Functional Requirements

#### NFR-1: Performance
- Search and list operations should return within 150ms for a 10k-note dataset on typical desktop hardware.
- Performance claims must be supported by a repeatable benchmark or test.

#### NFR-2: Scalability
- The design should support growth toward 100k notes without unbounded memory usage.
- Large result sets must be paginated rather than loaded all at once.

#### NFR-3: Cross-Platform Operation
- The application should run on macOS, Windows, and Linux with Java 17 and Maven.
- Local storage paths should be configurable or derived from the user's home/app data location rather than hardcoded to one machine.

#### NFR-4: Maintainability
- Storage, encryption, plugin, import/export, and UI responsibilities should remain separated.
- Public APIs and important architecture decisions should be documented.
- Tests should cover public behavior, not only implementation details.

### Security, Privacy, Reliability, and Governance Requirements

#### SEC-1: Encryption at Rest
- Protected note body content shall be encrypted at rest.
- The current course implementation may use application-level AES-GCM for note bodies, with SQLCipher documented as an optional whole-database hardening path.
- If whole-database encryption is required later, SQLCipher or an equivalent driver must be enabled and documented.

#### SEC-2: Integrity Verification
- Each stored note shall include an integrity validation mechanism, such as HMAC.
- The system shall verify integrity before returning decrypted note content.

#### SEC-3: Access Control
- The encryption manager shall require an unlock secret before encrypting or decrypting note content.
- A failed unlock shall not expose note content.
- Demo passwords must be documented as demo-only and not treated as production-ready access control.

#### REL-1: ACID Reliability
- All write operations shall use SQLite transactions to prevent partial state.
- Failed writes shall roll back or fail without leaving inconsistent note and search index state.

#### GOV-1: Schema Governance
- Schema versioning shall be tracked in the database.
- Schema changes shall be documented in `plans/DECISIONS.md` or a migration note.
- Migration failure behavior shall be explicit.

#### GOV-2: Deletion Governance
- Soft delete and purge shall be separate actions.
- Deleted notes shall remain hidden from normal workflows until purged.
- Purge behavior shall be documented so the student can explain what data remains and what is removed.

## Ambiguity and Edge-Case Review

| Earlier wording or assumption | Problem | Refined rule |
| --- | --- | --- |
| "Safe export/import" | Safe could mean encrypted, checksummed, compatible, or simply successful. | Export bundles must be versioned, include metadata, and verify checksum before import. |
| "Encrypted SQLite database" | This sounded like mandatory SQLCipher, but the current implementation uses note-body AES-GCM with optional SQLCipher. | Protected note content must be encrypted now; whole-database SQLCipher is a documented optional hardening path unless required by the final rubric. |
| "Trusted plugins" | Trusted did not define boundaries or failure behavior. | Plugins receive named lifecycle hooks, store state separately, and cannot be allowed to corrupt core storage. |
| "Offline operation" | It was unclear whether this applied only to CRUD or every workflow. | Core CRUD, list, and search must work without remote services; plugin/network-dependent extras are outside the core guarantee. |
| "Manageable DB size" | Subjective and hard to test. | Growth toward 100k notes should use pagination and avoid unbounded memory usage. |

### Edge cases now explicitly covered
- Empty body is allowed, but title validation must be explicit.
- Very large note bodies need a documented practical limit or performance risk.
- Duplicate IDs during import must be handled by preserving, skipping, or generating new IDs intentionally.
- Deleted notes must not appear in read, list, or search.
- Wrong unlock secret must fail without exposing note content.
- Tampered encrypted content or HMAC must fail integrity validation.
- Migration failure must fail safely and be explainable.
- Plugin exceptions must be handled without corrupting notes.
- Search with punctuation, empty queries, or deleted-note matches must behave predictably.

## Functional vs Non-Functional Separation

Functional requirements describe what AstraNotes does: create, read, update, delete, search, operate offline, notify plugins, and export/import notes.

Non-functional requirements describe quality constraints: performance, scalability, cross-platform behavior, maintainability, security posture, reliability, and governance. The refined baseline keeps these separate so implementation tasks do not confuse a feature with a quality target.

## Knight Capital Lesson Applied

The Knight Capital case shows why hidden assumptions and weak operational discipline matter. For AstraNotes, the equivalent risks are smaller but still real: unclear encryption expectations, vague migration behavior, and untested import or plugin assumptions could create data loss or privacy failures. This baseline reduces that risk by turning vague statements into testable rules and by requiring decisions, edge cases, and failure modes to be documented before implementation moves forward.

## AI Refinement Note

AI was used as a critic rather than only a drafting tool. I used it to ask where my earlier requirements were vague, which requirements were functional versus non-functional, what edge cases were missing, and what assumptions were implied but not written down. I accepted suggestions that made requirements more measurable, such as checksum validation for import and explicit handling of deleted-note reads. I rejected or revised generic suggestions that did not fit AstraNotes, especially broad security language that did not distinguish the current AES-GCM implementation from optional SQLCipher hardening.
