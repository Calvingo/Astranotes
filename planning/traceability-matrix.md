# AstraNotes Requirements-to-UML Traceability Matrix

This matrix checks whether the current AstraNotes UML package supports the most important early requirements from `planning/refined-requirement-baseline.md`. The evidence is taken from the class, object, use case, activity, and deployment views in `planning/uml-design-package.md`.

## Selected Requirements

The selected requirements focus on early implementation: core note lifecycle, search, export/import, encryption, and reliability. These are the areas most likely to expose missing design support before coding expands.

## Traceability Matrix

| Req ID | Requirement | Class/Object Evidence | Use Case/Activity Evidence | Deployment Evidence | Status | Gap Note |
| --- | --- | --- | --- | --- | --- | --- |
| FR-1 | The system shall allow a user to create a markdown note with title, body, tags, and notebook. | `Note` has title/body/tags/notebook fields; `NoteRepository.create()` and `SQLiteNoteStorage.create()` define the create operation; object diagram shows `noteInstance1`. | `(Create Note)` use case exists; Create Note activity includes validation, UUID generation, encryption, HMAC, SQLite transaction, FTS update, and return ID. | `astranotes.jar` writes to local `sqlite_db`; deployment shows protected note bodies in local SQLite. | **Fully Traced** | None. |
| FR-2 | The system shall retrieve a note by ID and return decrypted body content and metadata. | `NoteRepository.get(id)`, `SQLiteNoteStorage.get(id)`, `EncryptionManager.decrypt()`, and `Note` metadata fields support this. | `(Read Note)` use case exists, but no dedicated Read Note activity diagram shows the retrieval path. | Deployment shows local DB access and encryption/decryption inside the desktop JAR. | **Partially Traced** | Read behavior is structurally present, but the UML package still lacks a dedicated read activity. |
| FR-3 | The system shall allow a user to update an existing note's title, body, tags, and notebook. | `NoteRepository.update()`, `SQLiteNoteStorage.update()`, `Note.setTitle()`, `Note.setBody()`, `Note.setTags()`, `version`, and `updatedAt` support updates. | `(Update Note)` use case exists; activity evidence is partial because the package does not include a full Update Note activity diagram. | Deployment supports local SQLite transaction update through `astranotes.jar`. | **Partially Traced** | Update is clear in classes/use cases, but behavioral detail is weaker than create/search. |
| FR-4 | The system shall support soft delete by marking a note as deleted. | `Note.deleted`, `Note.markDeleted()`, `NoteRepository.delete()`, `SQLiteNoteStorage.delete()`, `purge()`, and `purgeDeletedNotes()` support delete/purge. | `(Delete Note)` use case exists and maps to `(Mark Deleted)`, but there is no dedicated delete/purge activity. | Local SQLite DB stores the deleted flag and supports purge from the desktop app. | **Partially Traced** | Soft delete is visible, but purge behavior is not shown in an activity diagram. |
| FR-5 | The system shall support full-text search over note title, body snippet, tags, and notebook. | `NoteRepository.search()`, `SQLiteNoteStorage.search()`, and private `updateFtsIndex()` show search support; object diagram notes `notes_fts` table managed inside storage. | `(Search Notes)` use case exists; Search Note activity includes query tokenization, FTS query, ordering, pagination, and no-results path. | Deployment shows local SQLite DB; FTS is an internal database feature rather than a separate deployed component. | **Fully Traced** | None, but the diagram should continue to avoid showing a fake separate `FTS5SearchIndex` class. |
| FR-8 | The system shall export selected notes to a versioned file bundle. | `ExportImportService`, `exportNotes()`, `importFromFile()`, and `computeChecksum()` support export/import; object diagram shows `exportImportService`. | `(Export Notes)` and `(Import Notes)` use cases exist; Export/Import activity shows checksum validation and duplicate-ID handling. | Deployment includes `io.jar` and local filesystem backup/export path. | **Fully Traced** | None. |
| SEC-1 | Protected note body content shall be encrypted at rest. | `EncryptionManager.encrypt()`, `decrypt()`, `computeHMAC()`, `verifyHMAC()`, and `SQLiteNoteStorage` encryption delegation support protected note bodies. | Create Note activity includes "Encrypt body with root key" and "Compute HMAC"; lock/unlock use case exists. | Deployment describes local SQLite with note bodies encrypted using AES-GCM and optional SQLCipher hardening. | **Fully Traced** | None; wording now matches current AES-GCM implementation instead of implying mandatory SQLCipher. |
| REL-1 | All write operations shall use SQLite transactions to prevent partial state. | `SQLiteNoteStorage` owns the `Connection` and class diagram shows write methods, but no transaction helper class exists. | Create Note activity shows insert, FTS update, and commit transaction; update/delete rollback behavior is not shown. | Deployment uses local SQLite DB, which supports ACID writes. | **Partially Traced** | Transaction intent is visible, but rollback and transaction boundaries are not consistently shown across activities. |

---

## Traceability Metrics Summary

- **Total requirements reviewed**: 8
- **Fully Traced**: 4
  - FR-1, FR-5, FR-8, SEC-1
- **Partially Traced**: 4
  - FR-2, FR-3, FR-4, REL-1
- **Weakly Traced**: 0
- **Not Traced**: 0
- **Major UML elements without clear requirement reason**: 0

## Major UML Element Justification Check

| UML element | Requirement reason |
| --- | --- |
| `Note` | FR-1, FR-2, FR-3, FR-4 |
| `NoteRepository` | FR-1 through FR-5, REL-1 |
| `SQLiteNoteStorage` | FR-1 through FR-5, FR-8, SEC-1, REL-1 |
| `EncryptionManager` | SEC-1, SEC-2, SEC-3 |
| `ExportImportService` | FR-8 |
| `PluginManager`, `ITrustedPlugin`, `PluginStateStore` | FR-7 and governance support, even though FR-7 is not one of the eight scored requirements in this matrix |
| UI classes (`AstraNotesApp`, `MainPanel`, `NoteListPanel`, `NoteDetailPanel`, `NoteCreateDialog`) | User-facing support for FR-1, FR-2, FR-4, and core desktop workflow |
| `sqlite_db` deployment node | FR-6 local-first operation, SEC-1 storage protection, REL-1 SQLite transaction support |
| local filesystem export/backup path | FR-8 |

No major UML element appears to be pure gold plating after the Week 4.1 cleanup. Earlier speculative elements such as a standalone `FTS5SearchIndex`, `MigrationManager`, and `PluginContext` were removed or described as future extension points instead of current classes.

---

## Gap Analysis

1. **Read activity missing**: FR-2 has strong class evidence, but a dedicated activity diagram should show lookup, deleted-note filtering, HMAC verification, decryption, and return/not-found behavior.

2. **Update activity is thin**: FR-3 appears in classes and use cases, but a future activity diagram should show version increment, body re-encryption, HMAC refresh, FTS update, commit, and rollback.

3. **Delete/purge behavior needs behavioral detail**: FR-4 shows soft delete structurally, but purge and deleted-note filtering should be shown explicitly in an activity or sequence-style flow.

4. **Transaction boundaries need clearer UML evidence**: REL-1 is supported by SQLite and implementation intent, but rollback behavior is not visible enough in the diagrams.

5. **Performance is not scored here but should be tracked later**: NFR-1 is important, but Week 5.1 selected eight requirements. Search performance should be traced later through test evidence, benchmark notes, or an explicit performance constraint annotation.

## Recommendations Before Implementation

- Add a Read Note activity diagram.
- Add an Update Note activity diagram with transaction rollback and HMAC refresh.
- Add a Delete/Purge activity diagram or extend the current delete use case.
- Add a short note in the class diagram or activity diagram that FTS is handled inside `SQLiteNoteStorage` through the `notes_fts` table.
- Keep the traceability matrix synced whenever the UML package changes so removed speculative classes do not remain as evidence.

---

## Traceability Metrics Detail

| Metric | Value | Interpretation |
| --- | --- | --- |
| Requirements completeness | 100% (8/8) | Every selected requirement has at least one structural and behavioral/deployment evidence point. |
| Full traceability rate | 50% (4/8) | Core create, search, export/import, and encryption are well represented. |
| Partial traceability rate | 50% (4/8) | Read, update, delete, and transaction reliability need stronger activity-level evidence. |
| Weak/not traced rate | 0% (0/8) | No selected requirement is unsupported. |
| Gold-plating count | 0 | Current major UML elements have requirement justification after cleanup. |

## AI Refinement Note

AI helped draft the traceability structure and identify candidate mappings, but I verified the final matrix against the refined requirement baseline and the updated UML package. I corrected AI-style overreach by removing references to classes that sounded plausible but were not in the current design, such as a separate `FTS5SearchIndex`, `MigrationManager`, and `PluginContext`. I also adjusted the encryption evidence so it matches the current AstraNotes implementation: protected note body content is encrypted with AES-GCM, while SQLCipher remains an optional hardening path.
