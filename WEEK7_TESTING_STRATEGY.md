# AstraNotes Week 7.2 Testing Strategy and First Test Set

## 1. Testing Strategy Overview

AstraNotes is now a Spring Boot web demo with reusable backend storage/security slices and demo multi-user behavior. The testing strategy focuses on the highest-risk behavior first:

1. Secure note lifecycle with encryption and HMAC
2. Search correctness and deleted-note filtering
3. Service-layer validation
4. Multi-user ownership and sharing permissions
5. Controller/session behavior

The current automated test set covers model, storage, integration, search benchmark, validation, service-level authorization, and controller-level session/permission behavior. The next gap is full browser automation for the rendered Thymeleaf workflow.

## 2. Features Selected First

### Feature A: Secure Note Lifecycle

Requirements and stories:

- FR-1: Create note
- FR-2: Read note
- FR-3: Update note
- FR-4: Soft delete note
- SEC-1: Encryption at rest
- SEC-2: Integrity verification
- REL-1: ACID reliability

Why first:

- Every later feature depends on reliable note persistence.
- Encryption and HMAC failures can silently create privacy or data-integrity problems.
- Soft delete must be correct before search and export/import rely on stored data.

### Feature B: Search Correctness and Performance

Requirements and stories:

- FR-5: Search notes
- NFR-1: Performance

Why second:

- Search is user-visible and easy to regress.
- FTS5 behavior must be tested with deleted-note filtering.
- Performance needs a repeatable baseline before the dataset grows.

### Feature C: Multi-User Authorization

Requirements and stories:

- SEC-3: Access control
- Web multi-user scope
- Sharing/permission governance risk

Why third:

- A shared note must not become editable by the receiving user.
- User ownership is the main security boundary in the current web demo.
- Permission tests are high-signal and should block PRs.

## 3. Testing Levels

### Unit and Service Tests

Purpose: verify small focused behavior and business rules.

Current examples:

- `NoteTest`
  - note construction
  - metadata
  - version behavior
  - deleted flag behavior
- `NoteServiceTest`
  - rejects blank titles
  - trims title input
  - parses duplicate tags
  - verifies owner can see private note
  - verifies another user cannot see until shared
  - verifies shared user cannot edit or delete owner note

When they run:

- every local commit
- before integration tests
- in CI on every push/PR

### Integration Tests

Purpose: verify storage, encryption, schema, FTS, export/import, plugin state, and database behavior working together.

Current examples:

- `SQLiteNoteStorageTest`
  - create/read/update/delete
  - list
  - search
  - deleted-note search filtering
- `SQLiteNoteStorageIntegrationTest`
  - full lifecycle
  - search and pagination
  - export/import
  - purge
  - HMAC tamper detection
  - plugin state persistence

When they run:

- before merge
- before final submission
- whenever storage, encryption, export/import, or permission behavior changes

### Feature and Controller Tests

Purpose: verify user-story behavior across web routes and session state.

Current controller and browser evidence:

- logged-out `/notes` redirects to `/login`
- Alex can log in and create a note
- Alex can share a note with Morgan
- Morgan can read the shared note
- Morgan is blocked from owner-only edit/delete

Current automated controller tests:

- logged-out user redirects to `/login`
- logged-in user can access `/notes`
- owner can open edit route
- shared user cannot open edit route
- logout invalidates the session

When they run:

- after unit/service/integration tests pass
- before class demo
- in CI once stable

## 4. Current Test Set

The repository now has tests across model, storage, integration, performance, web service behavior, and web controller behavior.

| Test class | Level | What it validates |
|---|---|---|
| `NoteTest` | Unit | Note construction, metadata, update/version behavior, soft-delete flag. |
| `SQLiteNoteStorageTest` | Integration | CRUD, list, update, soft delete, search, deleted-note search filtering. |
| `SQLiteNoteStorageIntegrationTest` | Integration | Full storage lifecycle, export/import, purge, HMAC tamper detection, plugin state. |
| `SearchPerformanceTest` | Feature/performance | Search benchmark over a 2,000-note synthetic dataset. |
| `NoteServiceTest` | Service/security | Validation, per-user visibility, sharing, owner-only edit/delete. |
| `NoteWebControllerTest` | Controller/security | Login redirects, session workspace access, shared read-only route behavior, logout. |

### Test Set Mapped To Requirements

| Test | Requirement/story link | Expected result |
|---|---|---|
| `testCreateNote` | FR-1 | Create returns ID; stored note can be read back. |
| `testReadNote` | FR-2 | Valid note returns decrypted body and metadata. |
| `testUpdateNote` | FR-3 | Update changes title/body/tags and increments version. |
| `testSoftDelete` | FR-4 | Deleted note disappears from normal read/list. |
| `testSearch` | FR-5 | Search returns matching notes. |
| `testSearchExcludesDeletedNotes` | FR-4, FR-5 | Deleted notes do not appear in search. |
| `testHmacIntegrityVerification` | SEC-2 | Tampered HMAC causes read failure. |
| `testExportImport` | FR-8 | Export bundle can restore note into fresh DB. |
| `ownerCanSeeNoteButOtherUserCannotUntilShared` | SEC-3 | Private note is hidden until shared. |
| `sharedUserCannotEditOrDeleteOwnersNote` | SEC-3 | Shared user has read-only access. |
| `sharedUserCanReadButCannotOpenEditRoute` | SEC-3 | Shared user can view but cannot open owner edit route. |

### Current Verification Result

Current local result:

```text
mvn test
Exit code: 0
```

The performance benchmark currently uses 2,000 notes for fast feedback. Larger benchmarks should remain release-level or advisory checks.

## 5. Browser Test Outlines To Add Next

### Rendered login workflow

1. Given the browser opens `/login`
2. When Alex signs in with valid demo credentials
3. Then the notes workspace renders with Alex's identity

### Rendered sharing workflow

1. Given Alex owns a note
2. When Alex shares it with Morgan
3. Then Morgan can view the shared note after signing in

### Rendered permission denial

1. Given Alex owns a note
2. And Alex shared it with Morgan
3. When Morgan tries to open the edit workflow
4. Then the UI shows owner-only permission denial

### Export/import browser flow

1. Given a user has demo notes
2. When the user exports JSON
3. Then the downloaded bundle can be imported into a clean demo database

## 6. Test Timing Across Development

| Development moment | Tests to run | Why |
|---|---|---|
| Before local commit | Unit and service tests | Catch validation and permission regressions quickly. |
| Before merge/push | Full `mvn test` | Verify storage, encryption, search, export/import, purge, and multi-user authorization. |
| Before release-style submission | Full tests plus manual browser workflow | Confirm user-visible demo behavior. |
| After controller changes | Controller tests plus existing service/storage tests | Ensure route/session behavior still matches service permissions. |
| Before polished demo | Manual browser walkthrough or browser automation | Confirm rendered UI behavior matches route-level tests. |
| Occasional load check | Larger search/load benchmark | Avoid slowing every commit while still tracking scalability. |

## 7. Scripted Automation Vs AI-Native Testing

### Where scripted automation helps

Scripted tests are best for stable, repeatable behavior:

- encryption round trips
- HMAC tamper detection
- create/read/update/delete flows
- deleted-note filtering
- export/import checksum behavior
- search result correctness
- title validation
- owner-only edit/delete/share rules
- login redirects and logout route behavior

These tests should be deterministic and run with `mvn test`.

### Where AI-native testing helps

AI is most useful for critique and exploration:

- identifying missing edge cases
- comparing tests against requirements
- drafting controller and browser test outlines
- suggesting failure scenarios such as wrong user, corrupt bundle, database lock, oversized note, or tampered HMAC

AI should not be the final judge of correctness. The final tests must be executable, deterministic, and reviewed by the student.

## 8. AI Reflection

AI helped turn the implementation into a clearer test plan. It suggested separating tests by unit, integration, service, and feature/controller level, and it helped identify high-risk areas such as HMAC tampering, deleted-note filtering, and multi-user authorization.

What I kept:

- Testing secure note lifecycle first.
- Testing search correctness and performance early.
- Testing user ownership and sharing as a blocking security concern.

What I changed:

- I updated the strategy after the web app, sessions, ownership, and sharing were implemented.
- I moved multi-user tests from future outlines into current service/security and controller coverage.

What I rejected:

- Non-deterministic randomized search tests as the first test set.
- Large 100k-note performance tests on every commit.
- Treating AI-generated test code as correct before checking method names, signatures, and current implementation behavior.

## 9. Summary

The strongest AstraNotes test set now covers secure note lifecycle, search, export/import, validation, multi-user authorization, and controller-level session behavior. The next improvement is full browser automation for the rendered login, sharing, and export/import flows.
