# AstraNotes Week 6.2 Development Quality Audit

**Project**: AstraNotes  
**Week focus**: Quality in development, scope-track decision, and repo/workflow readiness  
**Chosen primary scope track**: Web-based, multi-user application

---

## 1. Scope Track Decision

### Decision
AstraNotes will use the **web-based, multi-user application** scope track going forward.

### Why this track
- It matches the Week 6.2 default planning baseline.
- It supports a more realistic notes application model: multiple users, authenticated access, shared web UI, and server-side persistence.
- It gives later labs clearer architecture and deployment targets than a desktop-only prototype.
- It still preserves the core AstraNotes requirements: create notes, read notes, update notes, soft delete, search, encryption/privacy handling, export/import, and governance.

### What this means for the current implementation
The current Java Swing implementation should be treated as an **early realization prototype**, not the final product direction. It proves several core slices:
- note model
- SQLite persistence
- AES-GCM note-body encryption
- HMAC integrity checks
- FTS5 search
- soft delete
- export/import service
- JUnit tests

However, it does **not** yet support the chosen web multi-user direction because it lacks:
- HTTP routes or controllers
- a browser-based UI
- user accounts or sessions
- per-user note ownership
- server deployment model
- web feature tests

The reusable parts are the domain, storage, encryption, import/export, and test ideas. The Swing UI should be replaced by a web UI layer.

---

## 2. Coherent Technology Stack

### Recommended stack

**Backend**
- Java 17
- Spring Boot
- SQLite for the course prototype
- JDBC or Spring JDBC for transparent SQL control
- JUnit for unit and integration tests

**Frontend**
- Server-rendered Thymeleaf or simple HTML/CSS/JavaScript
- Minimal browser UI for login, notes list, note detail, create/edit form, search, and settings

**Why this stack**
- It keeps Java 17 and much of the current core logic.
- It avoids introducing a separate frontend framework too early.
- It supports web routes, controller tests, and multi-user concepts without enterprise-scale complexity.
- It is realistic for a quarter project.

### Alternatives considered and rejected

1. **Continue Swing desktop**
   - Rejected as the primary track because Week 6.2 defaults AstraNotes to web multi-user unless another direction is justified.

2. **React + Spring Boot**
   - Rejected for now because it creates two separate application stacks and increases setup/testing burden.

3. **Cloud-native microservices**
   - Rejected because it is too large for the course scope and would distract from requirements, traceability, and quality.

---

## 3. Quality Audit of Week 6.1 Slices

### Slice A: Create Note

**Current behavior**
- User enters title, body, tags, and notebook.
- Storage creates a `Note`.
- Body is encrypted.
- HMAC is computed.
- Row is inserted into SQLite.
- FTS index is updated.

**Quality strengths**
- Clear separation between UI dialog, model, storage, and encryption.
- Storage uses prepared statements.
- Storage commits or rolls back database writes.
- HMAC gives integrity protection.

**Weaknesses**
- The current create UI is Swing-specific and cannot support the chosen web direction.
- Demo unlock password is hardcoded in the desktop entry point.
- Validation exists in the UI, but storage should also defend against invalid title/body values.
- Note ownership is missing, so the slice is not multi-user yet.

**Manual cleanup direction**
- Keep `Note`, `EncryptionManager`, and most of `SQLiteNoteStorage` behavior as reusable backend logic.
- Replace `NoteCreateDialog` with a web controller and browser form.
- Add `ownerUserId` or equivalent user ownership before web multi-user release.
- Move unlock/auth assumptions into a real authentication/session model.

### Slice B: Retrieve/List/Soft Delete Notes

**Current behavior**
- UI sidebar lists notes.
- Selecting a note retrieves it by ID.
- Storage decrypts and verifies the note before returning it.
- Delete marks a note as deleted and removes it from normal list/read flows.

**Quality strengths**
- Soft delete is implemented rather than immediate physical deletion.
- Deleted notes are filtered out of normal reads/lists/searches.
- HMAC verification prevents returning tampered content.
- List/search are paginated.

**Weaknesses**
- There is no web route for `GET /notes`, `GET /notes/{id}`, or `DELETE /notes/{id}`.
- Multi-user authorization is missing: one user's note IDs are not yet isolated from another user.
- UI error handling is desktop-dialog based rather than HTTP response based.
- Delete/purge behavior needs clearer user-facing confirmation in a web context.

**Manual cleanup direction**
- Convert list/read/delete into controller-backed web workflows.
- Add user ownership checks to every read/update/delete/search query.
- Return clear HTTP-level failures: not found, unauthorized, validation error, storage error.
- Keep soft delete and purge as separate operations.

---

## 4. Debugging and Error-Handling Lesson

The most important debugging lesson is that **failure behavior must be handled at more than one layer**.

In the desktop prototype, the UI validates fields and shows Swing dialogs, but the storage layer still needs to protect itself from invalid input because a future web controller or test can call storage directly. In the web direction, this becomes more important: invalid form input, unauthorized note access, wrong note IDs, database lock errors, and decrypt/HMAC failures must produce clear server-side outcomes instead of silent failures or generic UI messages.

For AstraNotes, the web version should define failure handling explicitly:
- invalid input -> validation error
- missing note -> not found
- wrong user -> unauthorized or forbidden
- HMAC mismatch -> integrity failure, do not return note body
- database write failure -> rollback and show safe error
- import checksum failure -> reject bundle before writing notes

---

## 5. AI-Assisted Review and Human Decisions

### What AI helped identify
- The current implementation is technically functional but aligned to desktop single-user behavior.
- A web multi-user scope requires user ownership, authentication, routes/controllers, and a web UI.
- The existing backend classes are useful but should not be confused with a finished web architecture.
- A minimal web stack is better than a large cloud-native stack for the quarter.

### What I accepted
- The recommendation to choose one scope track and stop carrying desktop, web, and cloud-native as equal possibilities.
- The idea of reusing core domain/storage/encryption logic while replacing the UI layer.
- The need to add owner/user isolation before claiming multi-user support.

### What I rejected
- Rebuilding the whole project from scratch.
- Adding React, microservices, cloud deployment, or complex plugin marketplaces at this stage.
- Treating AI-generated architecture suggestions as final without checking project scope and existing code.

---

## 6. Artifact Refactor Impact

Because AstraNotes is now choosing the web multi-user track, the following artifacts need updates:

| Artifact | Refactor needed |
| --- | --- |
| `planning/requirements.md` | Add web/multi-user assumptions such as authentication, user ownership, and browser UI. |
| `planning/user-stories.md` | Rewrite stories from single local user to authenticated web user where appropriate. |
| `planning/backlog.md` | Add web setup, auth/session, controller routes, and web UI items before advanced plugin/export work. |
| `planning/refined-requirement-baseline.md` | Clarify which requirements are local prototype requirements versus web multi-user baseline requirements. |
| `planning/uml-design-package.md` | Replace Swing UI classes in the final target design with controllers, views, services, and user/session classes. |
| `planning/traceability-matrix.md` | Re-map evidence once web controllers and web deployment diagrams exist. |
| `SUBMISSION_WEEK6.md` | Keep as Week 6.1 prototype evidence, but note that Week 6.2 changes the forward direction to web. |
| `BUILD_AND_RUN.md` | Add web run instructions once Spring Boot or another web framework is introduced. |

---

## 7. Minimal Future Repo Structure

The current repo already has `src/`, `src/test/`, `planning/`, and `docs/`. For the web direction, the target structure should evolve toward:

```text
AstraNotes/
├── src/
│   ├── main/
│   │   ├── java/com/astraNotes/
│   │   │   ├── model/
│   │   │   ├── storage/
│   │   │   ├── encryption/
│   │   │   ├── web/
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   └── dto/
│   │   │   └── AstraNotesWebApp.java
│   │   └── resources/
│   │       ├── templates/
│   │       ├── static/
│   │       └── application.properties
│   └── test/
│       └── java/com/astraNotes/
│           ├── storage/
│           ├── encryption/
│           └── web/
├── docs/
│   ├── functional-spec.md
│   └── test-plan.md
├── planning/
├── plans/
├── pom.xml
└── README.md
```

### Folder rationale
- `model`, `storage`, and `encryption` preserve reusable backend logic.
- `web/controller` handles browser/API requests.
- `web/service` coordinates user-aware application workflows.
- `web/dto` isolates form/API inputs from domain objects.
- `templates` and `static` hold browser UI files.
- `docs` holds functional specs and test plans.

---

## 8. GitHub and Local Sync Workflow

### Local workflow
1. Pull latest changes from GitHub.
2. Create a branch for the lab or feature, for example `week6-2-web-quality-audit`.
3. Make focused changes.
4. Run tests locally.
5. Commit with a requirement or lab reference.
6. Push the branch.
7. Open a pull request or record review notes.

### Commit naming pattern
- `docs: add Week 6.2 quality audit and web scope decision`
- `refactor: separate note service for web controller use`
- `test: add note ownership read tests`

### Sync expectation
The local workspace and GitHub repo should stay aligned through small commits, clear branch names, and tests before push. Planning documents should be updated in the same branch as architecture-changing implementation work.

---

## 9. Next Documentation and Test Infrastructure

The next artifacts to add are:
- `docs/functional-spec.md`: describes web user workflows, note ownership, search, export/import, and failure cases.
- `docs/test-plan.md`: maps unit, integration, and feature tests to requirements.
- Web controller tests for create/read/update/delete/search.
- Storage tests that include `ownerUserId` once multi-user ownership is added.
- Feature-level tests for login -> create note -> search -> delete.

---

## 10. Summary

Week 6.2 improves AstraNotes by turning the current desktop prototype into a quality-reviewed stepping stone rather than an accidental final direction. The chosen scope track is now **web-based multi-user**. The existing slices prove useful backend behavior, but they must be refactored into a web architecture with user ownership, routes/controllers, browser UI, and web-ready tests.

The main quality lesson is that AstraNotes should not keep multiple competing directions active. The project now has one forward path: preserve tested backend logic, replace the Swing shell with a web UI, and update planning/UML/traceability artifacts so later implementation work is coherent.
