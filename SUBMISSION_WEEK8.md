# Week 8.1 Lab Submission — AstraNotes Collaborative Git Workflow

## Collaboration Log

### Repository Context

- Project: AstraNotes
- Local workspace: `/Users/jw/Documents/scu/AstraNotes_v1`
- GitHub remote: `https://github.com/Calvingo/Astranotes.git`
- Base branch: `main`
- Exercise type: individual simulation using focused branches and human-style review notes

### Starting State

The workflow started from `main`. I checked the repository state, reviewed recent commits, and confirmed that AstraNotes already had a working local repository and a GitHub remote.

Recent workflow evidence in git history:

```text
41616cf Merge branch 'feature/backend-storage-security'
2dee06c Add secure backend storage: AES/GCM encryption, HMAC integrity, purge, schema versioning, and plugin state storage
4602a31 docs: add Week 8 lab submission summary for collaborative git workflow
62dc5e8 feat: improve note search using FTS5 and add deleted-note regression tests
35bbab6 chore: initialize AstraNotes repository for Week 8 lab workflow
```

The current working tree also contains later lab documentation edits, so I treated this Week 8.1 document as a workflow record rather than making a new live merge while unrelated work was already in progress.

---

## Branch and PR Workflow Summary

### Branch 1: Search Improvement

- Branch name: `feature/fts-search-improvement`
- Purpose: improve note-search correctness and performance by using the existing FTS5 index.
- Requirement/story support:
  - FR-5: Search notes
  - NFR-1: Performance
  - Story 5: Search notes efficiently
- Commit used:
  - `feat: improve note search using FTS5 and add deleted-note regression tests`

What changed:
- Search moved away from weaker ad hoc matching and toward FTS5-backed search.
- Regression coverage checked matching results and deleted-note exclusion.

Why it matters:
- Search is a central user workflow.
- Deleted notes must not appear in normal search.
- FTS5 better matches the database design and performance requirements.

### Branch 2: Backend Storage Security

- Branch name: `feature/backend-storage-security`
- Purpose: strengthen backend storage behavior around encryption, HMAC integrity, purge, schema versioning, and plugin state.
- Requirement/story support:
  - SEC-1: Encryption at rest
  - SEC-2: Integrity verification
  - REL-1: ACID reliability
  - GOV-1: Schema governance
  - FR-7: Plugin state support
- Commit used:
  - `Add secure backend storage: AES/GCM encryption, HMAC integrity, purge, schema versioning, and plugin state storage`

What changed:
- Added stronger secure-storage behavior.
- Added purge and schema-version support.
- Added plugin state persistence.

Why it matters:
- These changes reduce data integrity and governance risk.
- They also give later testing and traceability work concrete backend evidence.

---

## Pull Request Summary

### PR: `feature/fts-search-improvement` into `main`

**Change:**
This PR improves AstraNotes search behavior by using SQLite FTS5 search support and adding regression tests around search results and deleted-note exclusion.

**Why:**
The refined requirements and traceability matrix identify search as an early high-value feature. Search must support note title/body/tag matching and must not return soft-deleted notes.

**Risks:**
- FTS query parsing may behave differently from simple text matching.
- Multi-word or punctuation-heavy queries may need additional edge-case tests.
- Search ordering may need refinement if users expect relevance ranking rather than updated-time ordering.

**Evidence:**
- Local test command used during the workflow: `mvn -Dtest=SQLiteNoteStorageTest test`
- Regression tests cover search result matching and deleted-note exclusion.
- Manual review confirmed the change was focused on storage-level search behavior rather than mixing UI or unrelated refactors.

---

## Example Review Feedback

### Human-style review comment

`The scope is focused and the deleted-note regression test is useful. Before merge, please verify multi-word search behavior and document what should happen for an empty search query. Those cases affect user expectations and could become inconsistent between storage and the future web UI.`

### Review decision

`Comment`

### Reason

The PR looked close to merge-ready because the implementation and tests were focused, but the review raised one behavior question about query edge cases. This did not require rejecting the PR, but it did create a follow-up test/design note.

---

## AI Review Assistant Use

### Prompt used

`Act as a pull request reviewer for an AstraNotes PR that improves note search using SQLite FTS5 and adds a deleted-note regression test. Give review questions focused on requirements fit, architecture fit, edge cases, and test impact.`

### Useful AI suggestion

AI pointed out that the PR should clarify behavior for empty, punctuation-heavy, and multi-word queries. That was useful because FTS5 query construction can fail or behave unexpectedly if input is not normalized.

### AI suggestion rejected or refined

AI suggested adding UI-level search tests in the same PR. I rejected that for this branch because the PR was intentionally storage-focused. UI or future web-controller search tests should be handled in a separate branch so the scope remains reviewable.

### Human judgment

AI helped identify review questions, but it did not approve the PR. The final review decision was based on requirement fit, code scope, and test evidence.

---

## Merge or Merge-Readiness Note

### Merge-readiness note for `feature/fts-search-improvement`

`Ready to merge after local tests pass and the query edge-case behavior is either documented or added as a follow-up test. The branch is focused, supports FR-5/NFR-1, and does not mix search changes with unrelated UI or architecture work.`

### Merge note for `feature/backend-storage-security`

`Merged into main because the backend security scope was clear and aligned with encryption, HMAC, purge, schema governance, and plugin-state requirements. The merge improved backend readiness without changing the UI direction.`

---

## Short Refactor Note

The search branch refactored storage-level search toward the FTS5 index that already existed in the schema. This improves maintainability because search behavior now matches the database design instead of relying on weaker duplicated matching logic. It also improves collaboration because reviewers can focus on one storage behavior change and its tests. The next cleanup should add explicit tests or documentation for empty and multi-word search queries.

The backend security branch also improved maintainability by grouping related security/storage concerns into a focused backend change. It created stronger evidence for later traceability and testing labs.

---

## Optional Conflict Reflection

I did not perform a live merge-conflict exercise during this cleanup because the working tree already contained unrelated later-lab documentation edits. A realistic conflict for AstraNotes would likely happen if one branch changed `SQLiteNoteStorage.search()` while another branch refactored storage method structure or query helpers. A good resolution would preserve the improved FTS5 behavior, keep deleted-note filtering, and avoid mixing unrelated refactor changes into the search feature commit.

---

## AI Help Note

AI helped in three ways:

1. It helped draft PR summary language using `Change`, `Why`, `Risks`, and `Evidence`.
2. It helped identify review questions about edge cases, especially empty and multi-word search queries.
3. It helped separate what belonged in the search branch from what should become a follow-up UI or web-controller test.

What I accepted:
- The suggestion to include edge-case review comments.
- The suggestion to explicitly document risks and evidence.

What I changed:
- I kept the PR summary shorter and more engineering-focused than the AI draft.
- I tied the branch purpose back to AstraNotes requirements and user stories.

What I rejected:
- Suggestions to expand the branch into UI search behavior, controller design, or unrelated architecture cleanup.
- Any implied idea that AI review replaces human merge judgment.

---

## Workflow Lessons

- Focused branches make review easier.
- PR summaries need risk and evidence, not just a description of changed files.
- Review comments should ask about behavior that can break later, such as empty search or deleted-note filtering.
- AI is useful for critique and summarization, but the student must still decide whether the change is scoped, tested, and safe to merge.
