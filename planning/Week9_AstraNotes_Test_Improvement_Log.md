# Week 9 Lab: AstraNotes Test Improvement Log

## Feature / Requirement Reviewed

This review focuses on three AstraNotes requirements:

1. **FR-1: Create Note**
   - The system shall allow a user to create a markdown note with title, body, tags, and notebook.
   - Risk area: invalid title behavior must be explicit and tested.

2. **FR-5: Search Notes**
   - The system shall support full-text search over note title, body snippet, tags, and notebook.
   - Risk area: search tests should not enforce accidental ordering unless ordering is a requirement.

3. **SEC-3: Access Control**
   - The system shall prevent users from modifying notes they do not own.
   - Risk area: sharing must be read-only for non-owners.

These connect directly to the Week 9 problem bank:

- Problem 1: Note Title Validation
- Problem 2: Search Test Noise
- Problem 3: Sharing Permission Over-Mocking
- Problem 4: Coverage Illusion

## Weak, Noisy, Brittle, Or Missing Test Area

### Weak area 1: Note title validation

Original weakness:

- Earlier tests verified successful note creation but did not clearly define invalid title behavior.

Missing cases:

- empty title
- whitespace-only title
- oversized title
- leading/trailing spaces

Why this mattered:

- The Web UI requires title input, but browser validation alone is not enough.
- The service layer must reject or normalize bad input before storage.

### Weak area 2: Search assertion noise

Original weakness:

- Search tests could become brittle if they assert exact ordering when the requirement only guarantees matching results.

Better rule:

- If ordering is not a requirement, assert membership.
- If ordering is a requirement, control timestamps/update order and assert that explicit contract.

### Weak area 3: Sharing permission risk

Original risk:

- A multi-user notes app can pass happy-path tests while still allowing the wrong user to edit or delete another user's note.

Important negative cases:

- user B cannot see user A's private note before sharing
- user B can read the note after sharing
- user B cannot edit the shared note
- user B cannot delete the shared note

## Improved Test Implemented

The improved test set is now implemented in:

- `src/test/java/com/astraNotes/web/service/NoteServiceTest.java`

Key tests:

```java
@Test
public void createRejectsBlankTitle()
```

This verifies that whitespace-only titles are rejected by the service layer.

```java
@Test
public void createTrimsTitleAndParsesTags()
```

This verifies that title whitespace is trimmed and duplicate comma-separated tags are normalized.

```java
@Test
public void ownerCanSeeNoteButOtherUserCannotUntilShared()
```

This verifies that a note is private to its owner until explicitly shared.

```java
@Test
public void sharedUserCannotEditOrDeleteOwnersNote()
```

This verifies that sharing grants read access, not mutation rights.

## What Changed And Why

The test improvement is not simply "more tests." It makes tests match real product risk:

- Title validation now has executable service-layer coverage.
- Multi-user authorization is tested through real SQLite-backed service behavior instead of excessive mocks.
- Sharing is tested as a read-only permission, not just a happy-path UI feature.
- Search benchmark output remains useful but advisory, because timing can vary across machines.

## Mocking: Helping Or Hiding Risk?

Current decision:

- Avoid heavy mocking for storage, encryption, search, and sharing permissions.
- Use real `SQLiteNoteStorage` with a temporary test database for service authorization tests.

Where mocking helps:

- Future controller tests may use session setup helpers or mock MVC request objects.
- External services, if added later, can be mocked at the boundary.

Where mocking would hide risk:

- Mocking storage would hide owner/share SQL mistakes.
- Mocking encryption would hide HMAC/decryption failures.
- Mocking all permission services would hide whether user ownership is actually enforced.

Conclusion:

- For the current AstraNotes risks, integration-style service tests are more valuable than isolated mocks.

## Meaningful Coverage Gap

Remaining meaningful gaps:

- controller-level tests for login redirects and logout
- route-level test that shared users cannot access `/notes/{id}/edit`
- import ownership behavior for multi-user mode
- search empty query and punctuation-heavy query behavior
- wrong unlock password or locked encryption manager behavior
- plugin hook failure isolation

Why line coverage is not enough:

- A test can execute a controller without proving the route enforces session state.
- A test can execute sharing code without proving non-owners are blocked from mutation.
- A test can execute import/export without proving imported notes receive correct ownership.

Coverage numbers show what code ran; they do not prove that behavior is safe or aligned to requirements.

## How AI Helped And What I Judged Myself

### How AI helped

AI helped critique the test set by pointing out:

- title validation needed executable negative tests
- search ordering could become an accidental assumption
- permission behavior should be tested with negative cases
- line coverage can hide missing failure paths

### What I accepted

- Add explicit title validation cases.
- Add multi-user permission tests.
- Keep storage/encryption/permission behavior real instead of over-mocked.

### What I changed

- I updated the plan after AstraNotes became a Spring Boot web demo with users, ownership, and sharing.
- I changed the improved test from a future outline into real `NoteServiceTest` coverage.

### What I rejected

- Adding random/fuzz testing as the first fix.
- Mocking the storage layer to make permission tests easier.
- Treating high line coverage as proof of quality.

## Summary

The strongest Week 9 improvement is that AstraNotes now has executable tests for validation and multi-user authorization. The suite still needs controller-level tests, but it already verifies the most important service/security rule: a shared user can read a note but cannot edit or delete the owner's note.
