# AstraNotes - Week 6 Development

Secure offline-first markdown note-taking application built with Java + SQLite + AES encryption.

## Quick Start

```bash
# Build
mvn clean package

# Run
java -jar target/astraNotes-0.1.0.jar
```

## Documentation

- **[WEEK6_QUICKSTART.md](WEEK6_QUICKSTART.md)** ← Start here!
- **[SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md)** ← Full submission report
- **[BUILD_AND_RUN.md](BUILD_AND_RUN.md)** ← Detailed build guide
- **[DELIVERABLES.md](DELIVERABLES.md)** ← Checklist of requirements

## What's Included

✓ Core CRUD operations (Create, Read, Update, Delete notes)  
✓ AES-256 encryption with HMAC integrity checks  
✓ SQLite database with FTS5 full-text search  
✓ Swing GUI with menu, sidebar, detail view  
✓ Maven build system with automated testing  
✓ 10 passing tests covering both main slices  
✓ Comprehensive documentation and traceability  

## Project Structure

```
├── pom.xml                          # Maven configuration
├── src/main/java/com/astraNotes/    # Main source code
│   ├── model/                       # Domain models
│   ├── storage/                     # Database layer
│   ├── encryption/                  # Encryption logic
│   └── ui/                          # Swing GUI components
├── src/test/java/com/astraNotes/    # Unit + integration tests
└── planning/                        # Requirements and UML
```

## Technology Stack

- **Language**: Java 15+
- **Build**: Maven 3.6+
- **Database**: SQLite with FTS5
- **Encryption**: Bouncy Castle (AES-256)
- **UI**: Swing
- **Testing**: JUnit

## Features Implemented

| Feature | Status | Test Coverage |
|---------|--------|---|
| Create note (encrypt + store) | ✓ Complete | testCreateNote |
| Retrieve note (decrypt + display) | ✓ Complete | testReadNote |
| List notes (paginated) | ✓ Complete | testCreateAndList |
| Update note (versioning) | ✓ Complete | testUpdateNote |
| Delete note (soft-delete) | ✓ Complete | testSoftDelete |
| Search (FTS5 index) | ✓ Schema ready | testSearch |

## Next Steps

- Week 7: Performance testing, plugin system, export/import
- See [SUBMISSION_WEEK6.md](SUBMISSION_WEEK6.md) for detailed roadmap

---

For detailed setup, build, and run instructions, see **[BUILD_AND_RUN.md](BUILD_AND_RUN.md)**.
