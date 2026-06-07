# AstraNotes Web Demo

AstraNotes is now a runnable Spring Boot web application for a secure note-taking demo. It keeps the original SQLite, AES/GCM encryption, HMAC integrity, FTS search, export/import, and JUnit test foundation, then adds a browser-based notes workspace.

## Quick Start

```bash
mvn test
mvn spring-boot:run
```

Open:

```text
http://127.0.0.1:8080/notes
```

## Demo Flow

1. Open the login page and sign in as a demo user.
2. Create a note with title, body, tags, and notebook.
3. Search note content.
4. Open and edit the note as the owner.
5. Share the note with another demo user.
6. Sign in as the shared user and confirm read-only access.
7. Delete the note as the owner.
8. Visit Profile and Settings.
9. Export or import an AstraNotes JSON bundle from Settings.

## Demo Users

| User | Password | Role |
|---|---|---|
| `alex` | `alex123` | Can create, edit, delete, and share owned notes |
| `morgan` | `morgan123` | Can receive shared notes |
| `taylor` | `taylor123` | Can receive shared notes |

## What Is Implemented

- Spring Boot web app entry point
- Thymeleaf browser UI
- Notes workspace with list, detail, search, create, edit, and delete
- Demo login and server-side session handling
- Per-user note ownership
- Owner-only edit/delete
- Read-only sharing with other demo users
- Profile demo page
- Settings page with encryption status
- Export all active notes as JSON
- Import AstraNotes JSON bundles
- SQLite persistence
- AES/GCM note-body encryption
- HMAC integrity verification
- FTS5-backed search
- Service-layer validation for note titles, body size, tags, and notebook defaults
- Maven test and package workflow

## Current Test Coverage

The project includes storage, model, integration, performance, and web service tests.

```bash
mvn test
```

## Project Structure

```text
src/main/java/com/astraNotes/
├── encryption/     # AES/GCM and HMAC support
├── io/             # export/import support
├── model/          # Note domain model
├── plugin/         # trusted plugin hooks/state
├── storage/        # SQLite repository
├── ui/             # older Swing prototype retained
└── web/            # Spring Boot web app, controller, DTOs, service

src/main/resources/
├── application.properties
├── static/css/app.css
└── templates/      # Thymeleaf pages
```

## Configuration

Default demo database:

```text
./data/web-notes.db
```

Default demo unlock password:

```text
ASTRANOTES_DEMO_PASSWORD=astraNotes-demo-123
```

For a different demo password:

```bash
ASTRANOTES_DEMO_PASSWORD="your-demo-password" mvn spring-boot:run
```

## Known Limits

This is a class-demo web application, not a production release. Real multi-user authentication, note ownership, sharing permissions, and hosted deployment are still future work.
