# AstraNotes Requirements-to-UML Traceability Matrix

## Traceability Matrix

| Req ID | Requirement | Class/Object Evidence | Use Case Evidence | Activity Evidence | Deployment Evidence | Status | Gap Note |
|--------|-------------|-----------------------|-------------------|-------------------|----------------------|--------|----------|
| FR-1 | Create note with title, body, tags, notebook; return unique ID | `Note` class with id, title, body, tags properties; `SQLiteNoteStorage.create()` method; `noteInstance1` object with generated UUID | (Create Note) use case with preconditions and flows | Create Note Activity showing validation, encryption, HMAC, transaction, FTS update, plugin notification | astranotes.jar writes encrypted blob to sqlite_db | **Fully Traced** | None |
| FR-2 | Read note by ID; return decrypted content and metadata | `NoteRepository.get(id)` interface; `SQLiteNoteStorage.get()` implementation; `noteInstance1` retrieval example | (Read Note) use case listed as actor action | No explicit read activity diagram; implied in search activity | astranotes.jar reads from sqlite_db with decryption | **Partially Traced** | No dedicated read activity diagram showing pure retrieval flow |
| FR-3 | Update note with version increment and `updatedAt` change | `NoteRepository.update()` interface; `SQLiteNoteStorage.update()` impl; `noteInstance1` shown with version=2 | (Update Note) use case includes version increment | Update Note Activity with version++ and `updatedAt` change shown | sqlite_db transaction update | **Fully Traced** | None |
| FR-5 | Search notes by title/body/tags using FTS5; return paginated results | `FTS5SearchIndex` class with `search(query, limit, offset)` method; `searchResult1` object with snippet and rank | (Search Notes) use case --> (Query FTS Index) dependency | Search Note Activity detailed with tokenize, FTS5 query, decrypt, paginate, order by `updatedAt` DESC | FTS5 virtual table on sqlite_db; indexed queries support < 150ms | **Fully Traced** | None |
| FR-7 | Plugin lifecycle hooks: `beforeCreate`, `afterUpdate`, `beforeDelete`, `onSearch` | `PluginManager` class with lifecycle methods; `ITrustedPlugin` interface with onNoteCreated, onNoteUpdated, onNoteDeleted; no runtime plugin instance in object diagram | Plugin actor receives (Receive Note Event) from create/update/delete use cases | Plugin notification shown in Create Note Activity ("Notify plugins: beforeCreate, afterCreate") | plugin.jar component within astranotes.jar JAR; plugins loaded at runtime | **Partially Traced** | Object diagram lacks runtime plugin instance example; plugin event flow not shown for update/delete activities |
| SEC-1 | Encryption at rest using SQLCipher or equivalent | `EncryptionManager` class with `encrypt()`, `decrypt()`, `computeHMAC()`, `verifyHMAC()` methods; `body = [encrypted blob]` in `noteInstance1` | (Lock/Unlock Vault) use case implicit in (Create Note) and (Update Note) flows | "Encrypt body with root key" step explicit in Create Note Activity | SQLCipher DB encrypts data at rest; Keychain/DPAPI/libsecret platform-specific key storage for unlock | **Fully Traced** | None |
| NFR-1 | Performance: search/list queries < 150ms for 10k notes | No explicit performance class or constraint annotation | (Search Notes) use case does not include SLA or timing constraints | Search Note Activity does not show timing or performance guarantee | FTS5 index provides mechanism for sub-150ms queries; no explicit performance test component shown | **Weakly Traced** | No explicit performance SLA constraint class; mechanism (FTS5) present but not guaranteed or tested in UML |
| REL-1 | ACID reliability: all writes in transactions; no partial state | No explicit transaction class; transaction logic implied in `SQLiteNoteStorage` | (Create Note), (Update Note), (Delete Note) use cases do not mention transaction semantics | "(Insert into SQLite transaction)" step shown in Create Note Activity; "Commit transaction" explicit; rollback behavior not shown | SQLite WAL mode mentioned in architecture notes but not in deployment diagram | **Partially Traced** | No explicit transaction guarantee class; rollback failure handling not shown in activities |

---

## Traceability Metrics Summary

- **Total requirements reviewed**: 8 (6 functional, 1 non-functional, 1 reliability)
- **Fully Traced**: 4 (FR-1, FR-3, FR-5, SEC-1)
  - Requirements with complete structural, behavioral, and deployment evidence.
- **Partially Traced**: 3 (FR-2, FR-7, REL-1)
  - Requirements supported by some evidence but with gaps in diagrams or activity flows.
- **Weakly Traced**: 1 (NFR-1)
  - Requirement supported by mechanism (FTS5) but no explicit performance constraint or verification shown.
- **Not Traced**: 0
  - All requirements have at least some supporting evidence.

- **UML elements without clear requirement reason**: 0
  - All major classes/components justify against FR/NFR/SEC requirements:
    - `ExportImportService` → FR-8 (export/import)
    - `MigrationManager` → GOV-1 (schema governance)
    - `PluginContext` → FR-7 (plugin hooks)
    - `FTS5SearchIndex` → FR-5 (search)

---

## Gap Analysis

### Identified Gaps
1. **Read activity missing** (FR-2): The use case diagram includes "(Read Note)" but no dedicated activity diagram shows the pure retrieval flow. This should be added to detail decryption, filtering of deleted notes, and metadata assembly.

2. **Plugin instance in object diagram** (FR-7): The object diagram does not show a runtime plugin instance receiving events. Adding a `pluginInstance1: ITrustedPlugin` object would strengthen traceability.

3. **Performance constraint modeling** (NFR-1): The class diagram has no explicit `PerformanceConstraint` or annotation on `FTS5SearchIndex`. While FTS5 is the correct mechanism, the 150ms SLA should be documented as a class invariant or test case.

4. **Transaction guarantee class** (REL-1): `SQLiteNoteStorage` implies transactions but does not show explicit transaction handling or rollback semantics. Adding a `TransactionManager` class or explicit transaction documentation in `SQLiteNoteStorage` would clarify ACID guarantees.

5. **Deployment testing component missing**: The deployment diagram shows the JAR and DB, but no test or monitoring component. Consider adding a `TestRunner` or `HealthCheck` artifact to verify performance and encryption.

### Recommendations Before Implementation
- Extend object diagram to include a plugin instance receiving events.
- Add a Read Note activity diagram mirroring Create/Update/Delete flows.
- Annotate `FTS5SearchIndex` with `@Constraint: search < 150ms for 10k notes`.
- Document transaction boundaries and rollback behavior in `SQLiteNoteStorage.executeTransaction()` method signature or javadoc.
- Add a brief test/verification component to the deployment diagram.

---

## Traceability Metrics Detail

| Metric | Value | Interpretation |
|--------|-------|-----------------|
| Requirements completeness | 100% (8/8) | All selected requirements have at least some UML evidence. |
| Full traceability rate | 50% (4/8) | Half the requirements are fully mapped; acceptable for early design phase. |
| Weak/missing coverage | 12.5% (1/8) | Only one requirement (NFR-1) is weakly traced; acceptable if addressed in Sprint 1. |
| UML design alignment | 100% (0 unused elements) | No overdesigned or unjustified UML elements; architecture is lean. |

---

## AI Refinement Note

AI helped by generating the initial class diagram structure and use case flows, accelerating the mapping of architecture to UML. During refinement, I:
- Traced each requirement to specific class methods and object properties, ensuring concrete evidence rather than vague mappings.
- Identified and documented gaps (e.g., missing read activity, missing plugin instance) rather than pretending full traceability.
- Rejected vague or generic evidence (e.g., "design supports encryption" without naming the `EncryptionManager` class and its methods).
- Verified that every UML element (class, object, activity, component) had a clear requirement justification, removing any "nice-to-have" elements with no FR/NFR/SEC backing.
- Used the refined requirement baseline as the single source of truth for requirement IDs and wording.

The result is an evidence-based matrix that an engineer can use to:
- Understand which requirements are well-supported in the design.
- Identify which gaps must be closed before implementation.
- Verify that the UML diagrams actually match the requirements.
