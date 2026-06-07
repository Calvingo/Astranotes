# CSEN 296B-2 Week 10.2 Lab

## AstraNotes Security, Deployment, and Maintenance Readiness Review

## Part 1: Release Context

AstraNotes is now a Spring Boot web demo application with multi-user login, server-side session handling, per-user note ownership, and read-only note sharing. The older Swing prototype remains in the repository as historical implementation work, but the primary demo path is now browser-based.

Current implementation:

- App type today: web-based class demo.
- Language/framework: Java 17, Maven, Spring Boot, Thymeleaf, SQLite JDBC, JUnit.
- Storage/security: SQLite storage, AES/GCM note-body encryption, HMAC integrity checks, soft delete, purge, schema versioning, FTS search, export/import.
- Multi-user behavior: demo users can sign in, own notes, share notes with other demo users, and receive read-only shared notes.
- Build/test automation: Maven build and tests, plus draft GitHub Actions workflow at `.github/workflows/ci.yml`.

Implemented or prototyped features:

- Login/logout with demo users.
- Server-side session handling.
- Create, list, view, edit, search, and delete notes.
- Per-user note ownership.
- Owner-only edit, delete, share, and unshare.
- Read-only access for shared notes.
- Profile and settings pages.
- Export/import note bundles with checksum validation.
- Application-level encryption and HMAC integrity verification.

Planned but incomplete features:

- Production-grade password hashing and account registration.
- Course roster integration.
- Hosted/cloud deployment.
- Full browser automation test suite.
- Automated dependency/security scanning.

Intended final demo path:

1. Build and test the project with `mvn -B -V clean test package`.
2. Start the web app with `mvn spring-boot:run`.
3. Open `http://127.0.0.1:8080/login`.
4. Sign in as `alex / alex123`.
5. Create, search, edit, and share a note.
6. Sign out and sign in as `morgan / morgan123`.
7. Confirm Morgan can read Alex's shared note but cannot edit or delete it.
8. Show profile/settings and export/import readiness.

## Part 2: Security and Privacy Audit Table

| Risk ID | Risk Area | What Could Go Wrong | Related Requirement or Feature | Evidence Checked | Severity | Decision |
|---|---|---|---|---|---|---|
| SEC-01 | Authentication/session | Demo passwords are simple and suitable only for class demo, not production. | Login/logout, session handling | `AuthService`, `/login`, session checks | High | Accept for class demo; replace before production |
| SEC-02 | Authorization/sharing | A shared user could edit or delete another user's note if owner checks fail. | Owner-only edit/delete/share | `NoteServiceTest`, `SQLiteNoteStorage` owner/share methods | High | Fix now; implemented and tested |
| SEC-03 | Private note content / PII | Private note content could be pasted into AI tools during prompt drafting or troubleshooting. | Governance and responsible AI use | `plans/privacy-governance-review.md`, prompt library | High | Fix now through process controls |
| SEC-04 | Secrets/configuration | Demo unlock password has a default value and should not be treated as production secret management. | Encryption unlock and deployment readiness | `application.properties`, `ASTRANOTES_DEMO_PASSWORD` | Medium | Accept for demo; replace for production |
| SEC-05 | Logging/error messages | DEBUG logging and detailed exception messages may expose paths, note IDs, search terms, or operational details. | Logging, storage, search, error handling | `src/main/resources/logback.xml`, storage/controller behavior | Medium | Fix before production release |
| SEC-06 | Dependency/environment | SQLite, Bouncy Castle, Gson, and Spring Boot dependency versions may become vulnerable or behave differently across environments. | Build/deployment and governance | `pom.xml`, Maven build output | Medium | Defer with maintenance control |
| SEC-07 | Deployment/data recovery | Local demo database is stored under `./data/web-notes.db`; data can be lost if the project directory is cleaned. | Persistence, export/import, recovery | `application.properties`, export/import flow | Medium | Accept for demo with recovery plan |
| SEC-08 | Input validation | Bad note input could create unusable records if service validation fails. | FR-1 Create Note | `NoteService`, `NoteServiceTest` | Medium | Fix now; implemented and tested |

## Part 3: SAST vs. DAST Review

### SAST-style checks

1. Demo credentials and secret defaults
   - Inspect `AuthService` and `application.properties`.
   - Evidence checked: demo user passwords are hardcoded for class demonstration; encryption unlock uses `ASTRANOTES_DEMO_PASSWORD` with a default.
   - Acceptable result: demo-only credentials are documented and not described as production security.

2. Authorization implementation
   - Inspect `NoteService`, `NoteWebController`, and `SQLiteNoteStorage`.
   - Evidence checked: list/get/search are user-scoped; update/delete/share use owner-specific paths.
   - Acceptable result: read access supports owner or shared user; mutation requires owner.

3. Sensitive logging and error-message patterns
   - Inspect `logback.xml`, storage classes, and controllers.
   - Evidence checked: app logs note IDs and search terms; note bodies are not intentionally logged.
   - Acceptable result: production logs avoid private note body, credentials, prompt content, and excessive exception detail.

4. Dependency and configuration review
   - Inspect `pom.xml` for dependency versions and optional SQLCipher profile assumptions.
   - Evidence checked: dependencies include Spring Boot, SQLite JDBC, Bouncy Castle, Gson, and JUnit.
   - Acceptable result: dependency versions are documented and reviewed for known risk before production release.

### DAST-style checks

1. Login and session behavior
   - Visit `/notes` while logged out.
   - Expected result: redirect to `/login`.

2. Owner note workflow
   - Sign in as Alex, create a note, edit it, search it, and delete it.
   - Expected result: owner can perform full note workflow.

3. Sharing permission behavior
   - Alex shares a note with Morgan.
   - Expected result: Morgan can read the note.

4. Negative authorization behavior
   - Morgan attempts to edit or delete Alex's shared note.
   - Expected result: edit/delete is denied with owner-only messaging.

### What SAST would likely miss

SAST would likely miss whether the browser workflow actually enforces session state and owner-only behavior after redirects and form posts.

### What DAST would likely miss

DAST would likely miss dependency risk, hardcoded demo credentials in source, and whether logs leak too much detail over time.

## Part 4: Remediation Plan

| Issue | Decision | Rationale | Owner | Verification Evidence Needed | If Ignored |
|---|---|---|---|---|---|
| Owner-only authorization | Fix now | This is the highest privacy risk in a multi-user notes app. | Student developer | Tests show shared users can read but cannot edit/delete owner notes | Shared users could modify or remove private notes. |
| Note title validation | Fix now | Core Create Note behavior must reject blank or invalid titles. | Student developer | Unit/service tests for blank and normalized titles | Bad records could enter the system and confuse search/demo behavior. |
| Demo credentials | Accept for class demo, defer production fix | Hardcoded demo users are acceptable for a course demo but not production. | Student developer | README clearly lists demo users and production limitation | A reviewer may confuse demo auth with production auth. |
| Logging level and sensitive operational details | Defer production hardening | Current logs support debugging but are too detailed for production. | Student developer | Future release profile with safer logging | Logs may expose operational details or search terms. |
| Dependency/security scan not automated | Defer with maintenance control | Useful, but current scope is class demo readiness. | Student developer | Dependency review checklist or future automated scan | Vulnerable dependencies may remain unnoticed. |

## Part 5: Deployment Plan

### Target environment

The current target is local web demo execution for class submission, not hosted production deployment.

- OS: macOS or another Java-capable environment.
- Runtime: Java 17.
- Build tool: Maven 3.6+.
- Web server: embedded Spring Boot/Tomcat.
- Demo URL: `http://127.0.0.1:8080/login`.

### Setup commands

```bash
cd /Users/jw/Documents/scu/AstraNotes_v1
java -version
mvn -version
```

### Build command

```bash
mvn -B -V clean test package
```

### Test command

```bash
mvn test
```

Current evidence:

- Maven tests pass.
- Current test set includes model, storage, integration, search benchmark, validation, and multi-user authorization tests.
- Spring Boot package build succeeds.

### Run command

```bash
mvn spring-boot:run
```

Then open:

```text
http://127.0.0.1:8080/login
```

### Demo users

| User | Password | Demo purpose |
|---|---|---|
| `alex` | `alex123` | Create, edit, delete, and share owned notes |
| `morgan` | `morgan123` | Receive and read shared notes |
| `taylor` | `taylor123` | Additional sharing target |

### Required environment variables or configuration

Current demo:

- No required environment variables.
- Optional: `ASTRANOTES_DEMO_PASSWORD` for encryption unlock.
- Local DB path: `./data/web-notes.db`.

### How secrets will be handled

For the final class demo, no real personal credentials should be used. Demo user passwords are intentionally simple and documented. Private student information, real notes, API keys, and course credentials should not be stored in the repo or pasted into AI prompts.

For production, demo credentials must be replaced with account registration/login, password hashing, and environment-managed secrets.

### Demo data setup

Use synthetic notes only:

- Title: `Week 10 Demo Plan`
- Body: short fake planning notes with no personal information.
- Tags: `demo`, `security`, `release`
- Notebook: `course`

### Known limitations

- Demo authentication is not production-grade.
- No password hashing or account registration.
- No course roster integration.
- No hosted cloud deployment.
- SQLCipher whole-database encryption is optional and not enabled in the default path.

### Rollback or recovery plan

For class demo:

1. Keep the last known passing commit available on `main`.
2. If the app fails, rerun `mvn -B -V clean test package`.
3. If local database state becomes corrupted, remove `./data/web-notes.db` and recreate demo notes.
4. Use export/import JSON bundle as the recovery path for demo notes.
5. If a new branch breaks, return to the last passing commit rather than presenting unverified code.

## Part 6: Maintenance and Evolution Plan

### Four-week maintenance plan

Week 1:

- Add controller-level tests for login redirects, session access, shared read-only behavior, and logout.
- Reduce demo/release logging detail.
- Document demo credentials and security limitations clearly.

Week 2:

- Replace hardcoded demo users with a small persisted user table or configuration-backed demo roster.
- Add password hashing if the project moves beyond class demo.
- Add browser-level feature tests for the core workflow.

Week 3:

- Add richer sharing management: unshare confirmation, shared-by metadata, and share audit entries.
- Add import ownership rules so imported notes belong to the current user.
- Review dependency versions for known security concerns.

Week 4:

- Add deployment profile and hosted demo option if required.
- Add CI checks for controller tests.
- Revisit release readiness decision with web-specific evidence.

### Top three bugs or risks to watch

1. Authorization mistakes that let shared users mutate owner notes.
2. Validation gaps that allow invalid notes to be saved.
3. Encryption or HMAC failures that make notes unreadable or silently skipped.

### Bug triage process

- High priority: unauthorized access, data loss, broken build, failed storage/encryption tests.
- Medium priority: validation gaps, confusing UI errors, export/import edge cases.
- Low priority: visual polish, non-critical wording, optional enhancements.

### Test update process

- Every fixed bug should add or update one focused test.
- Storage/security changes require integration-style tests.
- Web authorization changes require negative permission tests.
- Noisy performance benchmarks remain advisory until stable thresholds are defined.

### Dependency update process

- Review Maven dependency versions weekly during active development.
- Prefer small dependency updates with tests run immediately after.
- Avoid adding dependencies for features that can be implemented simply.
- Record security-related dependency changes in the project documentation.

### One feature to add next

Add controller-level tests for login/session redirects and owner-only permissions. The service/storage permission logic is tested, but route-level evidence makes the web demo easier to defend.

### One feature not to add yet

Do not add public external sharing links yet. Link-based sharing would require token generation, expiration, revocation, and stronger audit controls. The current user-to-user sharing model is safer for the class demo.

## Part 7: AI Use and Human Verification

| Prompt Used | AI Output Summary | What I Accepted | What I Changed or Rejected | Human Verification Performed |
|---|---|---|---|---|
| Prompt 1: Security Audit | AI identified risks around authentication, authorization, note privacy, prompt leakage, secrets, logs, dependencies, and deployment. | Accepted the category structure and the need to connect risks to evidence. | Changed the scope to reflect implemented demo login/sharing instead of production auth. | Checked `AuthService`, `NoteService`, `NoteWebController`, `SQLiteNoteStorage`, and tests. |
| Prompt 2: SAST and DAST | AI separated static code/config checks from running-app behavior checks. | Accepted hardcoded demo credential, logging, dependency, and authorization SAST checks. | Changed DAST checks to include actual login/share/edit denial workflows. | Ran curl-based workflow checks for login, create, share, shared read, and edit denial. |
| Prompt 3: Deployment Plan | AI suggested setup, build, test, configuration, secrets, demo data, limitations, and rollback sections. | Accepted the structure because it matches the lab requirements. | Rejected production/cloud deployment claims. The honest target is local web demo. | Ran Maven test/package and verified Spring Boot starts locally. |
| Prompt 4: Human Verification | AI challenged vague readiness claims and missing evidence. | Accepted the final decision should not be "production ready." | Changed final decision to "Ready for class web demo." | Verified implemented web login, ownership, sharing, and authorization behavior. |

## Part 8: Final Readiness Decision

**Decision: Ready for class web demo.**

AstraNotes is ready for a class web demo because the current application builds successfully, has passing automated tests, runs as a Spring Boot web app, and demonstrates login, session handling, note ownership, owner-only edit/delete/share, read-only shared-note access, note search, export/import, encryption, and HMAC integrity behavior. It is not a production release because demo passwords, no password hashing, no roster integration, no hosted deployment, and limited browser automation remain. The strongest final claim is that AstraNotes is a complete course-demo web application with clear remaining production-hardening work.
