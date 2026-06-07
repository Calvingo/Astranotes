# AstraNotes Decision Log

This file records architecture and governance decisions that affect implementation, testing, security, or project workflow.

## Decision: Use SQLite as the Local Source of Truth

**Date**: 2026-04-06

**Context**: AstraNotes is an offline-first desktop note-taking application. It needs local persistence, ACID writes, search support, and a realistic implementation path for a course project.

**Options**:
- SQLite database
- Plain JSON files
- Remote database or cloud-first backend

**Choice**: SQLite.

**Reasoning**: SQLite supports local-first operation, transactions, indexes, and FTS5 search without requiring network services. It also fits the current Java desktop implementation and Maven build.

**Trade-offs**: SQLite requires careful schema governance and migration tracking. Full database encryption requires SQLCipher or an equivalent native driver, which is more complex than the default SQLite JDBC dependency.

**Status**: Accepted.

## Decision: Use Application-Level AES-GCM While SQLCipher Remains Optional

**Date**: 2026-06-06

**Context**: The refined requirements call for encryption at rest using SQLCipher or equivalent protection. The current project uses AES-GCM for note bodies and HMAC checks for note integrity. The SQLCipher profile is documented but not active by default.

**Options**:
- Require SQLCipher for the entire database
- Use application-level encryption for note bodies and optional SQLCipher for deployments that can provide a compatible driver
- Store notes without encryption

**Choice**: Use application-level AES-GCM for the current implementation and keep SQLCipher as an optional profile.

**Reasoning**: Application-level encryption gives the project a working, testable security layer now. SQLCipher remains a future hardening path, but native dependency setup is not required for the baseline Maven test suite.

**Trade-offs**: Metadata such as title, tags, notebook, and timestamps may remain visible in the SQLite file unless SQLCipher is enabled. This must be clearly disclosed in security documentation and treated as a remaining gap if the instructor expects whole-database encryption.

**Status**: Accepted for current course implementation; revisit if final lab requirements require full database encryption.

## Decision: Store Project Governance in `plans/` and Requirements in `planning/`

**Date**: 2026-06-06

**Context**: Lab 2.1 requires a Working Agreement, Definition of Done, and evidence that AI-native collaboration is managed as a system of work.

**Choice**:
- Governance artifacts live in `plans/`.
- Requirements, backlog, user stories, traceability, and UML planning artifacts live in `planning/`.
- Prompt patterns live in `prompts/`.

**Reasoning**: This keeps course planning artifacts separate from governance/process artifacts while still making the workflow easy to inspect.

**Status**: Accepted.

## Decision: Use Web-Based Multi-User Scope Track Going Forward

**Date**: 2026-06-06

**Context**: Week 6.2 states that the default planning baseline for AstraNotes is a web-based, multi-user application unless another direction is documented and justified. The current implementation is a Java Swing desktop prototype, which proves core storage and encryption behavior but does not satisfy the web multi-user direction.

**Options**:
- Continue as desktop single-user
- Move forward as web-based multi-user
- Expand into cloud-native deployment

**Choice**: Move forward as web-based multi-user.

**Reasoning**: The web track aligns with the course handout, supports user accounts and browser workflows, and creates a clearer path for future architecture, tests, and deployment artifacts. It also allows reuse of current Java backend logic while replacing the Swing UI layer.

**Trade-offs**: Requirements, UML, backlog, traceability, and build/run documentation must be updated over time. Authentication, user ownership, web routes, and web tests must be added before the project can honestly claim multi-user web support.

**Status**: Accepted as the forward direction. The current Swing implementation remains prototype evidence for early backend slices.
