# AstraNotes Week 7.1 Architecture Quality and MVC Memo

## Purpose

This memo reviews the current AstraNotes implementation through MVC-style responsibility separation. The current codebase is a Java Swing prototype from Week 6.1, while the forward direction selected in Week 6.2 is a web-based, multi-user AstraNotes application. This memo evaluates both:

- the current prototype structure
- the target MVC direction for the future web implementation

## Current Structure Mapped to MVC

| MVC role | Current AstraNotes classes | Responsibility |
| --- | --- | --- |
| Model | `Note` | Represents note identity, title, body, tags, notebook, timestamps, version, deleted flag, and HMAC. |
| Model / persistence | `NoteRepository`, `SQLiteNoteStorage` | Defines and implements create, read, update, delete, list, search, purge, schema setup, FTS update, and database transactions. |
| Security service | `EncryptionManager` | Handles unlock, lock, encryption, decryption, HMAC computation, and HMAC verification. |
| Application service | `ExportImportService`, `PluginManager`, `PluginStateStore` | Handles export/import, plugin lifecycle events, and plugin state persistence. |
| View | `NoteDetailPanel`, parts of `NoteListPanel`, `NoteCreateDialog`, `AstraNotesApp` window/menu | Displays notes, note list, create form, settings/about dialogs, and shell UI. |
| Controller / coordinator | `MainPanel`, listener callbacks inside UI classes | Coordinates note selection, create dialog opening, refresh, delete, and settings actions. |

## Separation of Concerns Review

### What is working well

- The domain model is separate from storage and UI.
- Encryption is isolated in `EncryptionManager` instead of being embedded in Swing UI code.
- Storage behavior is behind `NoteRepository`, which creates a path to replace or wrap persistence later.
- UI is split into panels instead of one monolithic class.
- Tests cover model and storage behavior independently of the UI.

### Weaknesses in the current prototype

1. **View classes know too much about storage**
   - `NoteListPanel` receives `SQLiteNoteStorage` directly and calls `storage.list()`.
   - This makes the list panel both a view and a data-loading controller.

2. **Controller responsibilities are informal**
   - `MainPanel` coordinates selection and deletion, but there is no explicit controller/service boundary.
   - Button listeners and callbacks perform controller-like behavior inside Swing classes.

3. **Storage is tied to concrete implementation**
   - UI classes depend on `SQLiteNoteStorage` instead of the `NoteRepository` interface.
   - This makes future web migration harder because controllers should depend on services/interfaces, not concrete SQLite details.

4. **Web multi-user concerns do not exist yet**
   - There is no user/session model.
   - Notes are not scoped by owner.
   - There are no route handlers, request DTOs, or controller-level authorization checks.

5. **Error handling is UI-specific**
   - Failures become Swing dialogs.
   - A web app will need HTTP-safe error handling such as validation errors, not found, forbidden, integrity failure, and storage failure.

## Equivalent Pattern for Current Prototype

The current implementation is closest to a **layered MVC-like Swing prototype**:

- Model: `Note`
- Persistence/service layer: `SQLiteNoteStorage`, `EncryptionManager`, `ExportImportService`
- View: Swing panels and dialogs
- Controller/coordinator: `MainPanel` plus event listeners

This is acceptable for an early realization slice, but it is not clean MVC yet because some views directly access storage and controller behavior is spread across UI callbacks.

## Target Web MVC Mapping

For the chosen web-based, multi-user direction, AstraNotes should move toward this structure:

| MVC role | Future web classes or folders | Responsibility |
| --- | --- | --- |
| Model | `Note`, future `User` | Domain state and invariants. |
| Repository | `NoteRepository`, `SQLiteNoteStorage` or `WebNoteRepository` wrapper | Persistence contract and database implementation. |
| Service | `NoteService`, `AuthService`, `ExportImportService` | User-aware application workflows, validation, authorization, encryption coordination. |
| Controller | `NoteController`, `AuthController`, `SettingsController` | HTTP request handling, routing, form/API input mapping, response selection. |
| View | `templates/notes.html`, `templates/note-detail.html`, `templates/settings.html`, static CSS/JS | Browser-rendered UI. |
| DTO/Form objects | `CreateNoteRequest`, `UpdateNoteRequest`, `SearchRequest` | Safe boundary objects for web input. |

## Recommended Refactoring Direction

### Near-term cleanup for current prototype

1. Change UI dependencies from `SQLiteNoteStorage` to `NoteRepository` where possible.
2. Move list/read/delete workflow decisions from `NoteListPanel` into `MainPanel` or a `NoteController`-like coordinator.
3. Keep `NoteDetailPanel` as display-only.
4. Keep validation in the UI, but add defensive validation in service/storage entry points.
5. Avoid expanding Swing UI features now that Web MVC is the forward track.

### Web migration cleanup

1. Add a `NoteService` that wraps repository, encryption-related assumptions, validation, and authorization.
2. Add a user ownership field or join model before claiming multi-user support.
3. Add `NoteController` routes:
   - `GET /notes`
   - `GET /notes/{id}`
   - `GET /notes/new`
   - `POST /notes`
   - `GET /notes/{id}/edit`
   - `POST /notes/{id}`
   - `POST /notes/{id}/delete`
4. Add web-friendly error handling:
   - validation error
   - not found
   - forbidden
   - integrity failure
   - storage failure
5. Add controller tests and feature-level tests aligned to user stories.

## Maintainability Assessment

| Area | Current quality | Assessment |
| --- | --- | --- |
| Domain model | Good | `Note` is focused and easy to explain. |
| Storage | Good but large | `SQLiteNoteStorage` handles schema, CRUD, FTS, import support, plugin state setup, transactions, and reconstruction. It is functional but may need smaller helper classes later. |
| Encryption | Good for prototype | `EncryptionManager` is isolated, but unlock and key management need stronger production design. |
| UI | Moderate | UI is split into panels, but some panels mix display and data access. |
| Controller boundary | Weak | Current controller behavior is spread across Swing listeners and `MainPanel`. |
| Web readiness | Early | Backend logic is reusable, but web controllers, auth, user ownership, and browser views are missing. |

## AI-Assisted Architecture Critique

AI was useful for identifying a common architecture smell: the current Swing UI mixes view and controller responsibilities. It also highlighted that a web multi-user direction requires explicit controller, service, and authorization layers.

I accepted these critiques:
- UI should not directly depend on concrete `SQLiteNoteStorage`.
- A service/controller boundary is needed before the web migration.
- Multi-user ownership must be modeled before the project can claim web multi-user readiness.

I rejected these possible overcorrections:
- Introducing a full enterprise service architecture immediately.
- Replacing all current backend code before proving web routes.
- Adding microservices or cloud-native deployment before the basic Web MVC structure exists.

## Human Judgment

The current code is maintainable enough for a Week 6 prototype because the core model, storage, encryption, and UI are separated. It is not yet maintainable enough for the selected Web MVC direction because request handling, user ownership, and controller/service boundaries are missing.

The correct next step is not to rewrite everything. The better path is:

1. Preserve tested backend logic.
2. Add a `NoteService` layer.
3. Introduce web controllers and templates.
4. Gradually retire Swing UI from the forward architecture.
5. Update UML and traceability once the Web MVC structure exists.

## Summary

AstraNotes currently follows a rough MVC-like pattern, but it is closer to a layered Swing prototype than a clean MVC application. The model and storage responsibilities are clear, but controller behavior is mixed into UI classes. For the chosen web-based, multi-user path, AstraNotes should evolve toward Web MVC: model, repository, service, controller, DTO, and browser view layers. This keeps the project realistic while improving separation of concerns and maintainability.
