# AstraNotes Governance and Ethics Review Memo

This memo reviews AstraNotes as if it may be released into a real desktop environment. The goal is to identify privacy, AI-use, licensing, dependency, and governance risks early enough that they can shape implementation decisions.

## Part 1: Privacy and PII Review

### Data AstraNotes may store, display, or process
- **Note body content**: markdown text may include names, addresses, phone numbers, email addresses, school information, health information, financial information, passwords, private journal entries, or other sensitive material.
- **Note metadata**: title, tags, notebook name, creation timestamp, update timestamp, version, deleted flag, and note ID.
- **Search data**: FTS/search index entries may reveal note titles, body snippets, tags, or notebook names.
- **Export/import data**: exported bundles may contain protected note content and metadata outside the main database.
- **Plugin data**: plugin state may contain workflow information, search terms, or user preferences.
- **Logs and diagnostics**: logs may record note IDs, counts, errors, timings, and paths. They must not record decrypted note bodies.

### Privacy and PII risks
- **Note content is sensitive by default** because the application cannot know what a user writes.
- **Metadata can expose meaning** even when note body content is encrypted. A title such as "Tax ID" or a tag such as "medical" can be sensitive.
- **Search indexes can leak snippets** if body snippets are stored in plaintext.
- **Soft-deleted data can persist** until explicit purge.
- **Export files can escape normal protection** if users copy them to cloud drives, email, or external devices.
- **Logs can become accidental evidence stores** if they include content, titles, search queries, or full file paths.

### Responsible handling
- Treat all note content and note-adjacent metadata as sensitive.
- Do not log note bodies, decrypted content, or full search queries.
- Minimize logged identifiers to IDs, counts, timings, and error categories where possible.
- Document that the current implementation encrypts protected note body content with AES-GCM, while full database encryption through SQLCipher remains an optional hardening path.
- Require explicit purge for permanent deletion.
- Treat exported bundles as sensitive files and document whether the bundle contains encrypted content, plaintext metadata, or both.

## Part 2: AI Data Leakage Review

### Leakage risks when using AI tools
- Pasting real notes into an AI prompt could disclose private user content to an external service.
- Pasting metadata such as note titles, tags, notebooks, or search queries could reveal sensitive context.
- Sharing local file paths, database paths, environment variables, or stack traces may expose user identity or machine details.
- Sharing encryption keys, passwords, derived key material, or realistic secrets would create direct security risk.
- Asking AI to rewrite security-critical code without review may introduce vulnerabilities that look plausible but are wrong.

### Information that should not be shared casually
- Real note content or screenshots containing note content.
- User names, emails, addresses, student IDs, phone numbers, or account identifiers.
- Encryption passwords, derived keys, HMAC values from real data, or database files.
- Complete logs if they contain local paths, query text, or note metadata.
- Plugin state that may include user behavior or private workflow details.

### Prompt safety rules
- Use synthetic examples such as `Sample Note`, `test@example.com`, or fake IDs.
- Redact local paths, names, note titles, and any private content before asking AI for help.
- Share schema or code structure when needed, but avoid real stored data.
- Record reusable safe prompt patterns in `prompts/library.md`.
- Follow the Week 2.1 Working Agreement: AI can assist, but human review decides acceptance.

## Part 3: Licensing and Dependency Review

### Current dependencies from `pom.xml`

| Component | Purpose | Governance concern |
| --- | --- | --- |
| `org.xerial:sqlite-jdbc` | SQLite storage driver | Native binaries and license obligations should be tracked before distribution. |
| `org.bouncycastle:bcprov-jdk15on` | Cryptography provider | Security-sensitive dependency; version age and maintenance status should be reviewed before release. |
| `com.google.code.gson:gson` | JSON export/import processing | Common library, but export parsing must handle untrusted files carefully. |
| `org.slf4j:slf4j-api` | Logging API | Safe if logs avoid content and secrets. |
| `ch.qos.logback:logback-classic` | Logging implementation | Log configuration must avoid sensitive content and excessive retention. |
| `junit:junit` | Testing | Test-only dependency; should not affect runtime release risk. |
| Optional SQLCipher profile | Whole-database encryption hardening path | Native driver choice and licensing must be reviewed before enabling for release. |

### Licensing and supply-chain risks
- SQLCipher can introduce licensing and native binary obligations depending on the distribution path.
- SQLite JDBC and SQLCipher-style drivers may package native libraries; those must be reviewed for license, platform compatibility, and update practices.
- Cryptography libraries should be kept current because stale crypto dependencies increase security risk.
- Shaded JAR output can bundle third-party code, so dependency notices and license files should be preserved for release.
- Adding plugin frameworks later could expand the attack surface and licensing review burden.

### Responsible dependency handling
- Maintain a dependency/license inventory before any public release.
- Prefer well-known, maintained, permissively licensed dependencies.
- Avoid adding dependencies for small utilities that can be implemented safely with standard Java.
- Review dependency versions during release preparation.
- Document any native-library requirement, especially for SQLite or SQLCipher.

## Part 4: Responsible AI Use

### What responsible AI use means for AstraNotes
- AI is a drafting, critique, and review partner, not an authority.
- AI output must be checked against requirements, tests, security constraints, and the Definition of Done.
- AI-generated security, encryption, access-control, or privacy-related code requires extra human review.
- AI should help expose ambiguity, edge cases, and alternatives, not hide weak assumptions behind polished text.

### Human oversight expectations
- The student must be able to explain every accepted requirement, design decision, and code change.
- AI-generated code is not accepted until it compiles, passes relevant tests, and fits the existing architecture.
- AI-generated documentation is not accepted until vague claims are replaced with AstraNotes-specific details.
- Prompts and important AI-assisted decisions should be recorded in `prompts/library.md`, `plans/DECISIONS.md`, or a submission note.

### Trust boundaries
- Acceptable AI use: brainstorming, test case ideas, requirement critique, boilerplate drafting, documentation structure.
- High-risk AI use: cryptographic implementation, security policy, plugin trust model, migration/deletion behavior, dependency/license conclusions.
- No AI output should be treated as automatically correct because it sounds confident.

## Part 5: Governance Memo

### Identified risks
- **Privacy risk**: notes and metadata can contain personal or sensitive information.
- **Metadata/search-index risk**: encrypted body content does not automatically protect titles, tags, notebook names, or search snippets.
- **AI leakage risk**: real note content or local project details could be exposed through careless prompts.
- **Licensing risk**: SQLite/SQLCipher drivers, cryptography providers, and shaded dependencies may create obligations before release.
- **Supply-chain risk**: native drivers and security libraries require maintenance review.
- **Governance risk**: plugin hooks, import/export, deletion, and encryption assumptions can drift if decisions are not logged.

### Assumptions that must be explicit
- AstraNotes is a local-first desktop application, not a cloud service.
- Users may write sensitive content even if the app does not ask for PII.
- Current baseline encryption protects note body content through application-level AES-GCM; full database encryption is optional unless required later.
- Plugins are trusted/audited extensions, not untrusted sandboxed marketplace plugins.
- AI assistance is advisory and requires human verification.

### Practical safeguards going forward
- Keep note bodies out of logs, prompts, test fixtures with real data, and screenshots used for support.
- Add tests for HMAC tampering, deleted-note filtering, import checksum failure, and purge behavior.
- Maintain `plans/DECISIONS.md` for encryption, plugin, deletion, migration, and dependency decisions.
- Maintain `prompts/library.md` with safe prompt patterns and avoid real user content.
- Create or maintain a dependency/license inventory before release.
- Review `logback.xml` before release to confirm logs are scoped and do not capture sensitive note data.

### Release review expectation
Before any real release, AstraNotes should pass a short governance review:
- Privacy: no note content in logs; export/import behavior documented.
- AI: prompts use synthetic or redacted data; AI output reviewed by the student.
- Licensing: dependencies and native libraries reviewed.
- Security: encryption, integrity, unlock, deletion, and import failure cases tested.
- Governance: decision log and working agreement updated if assumptions change.

## Short Class Discussion Summary

AstraNotes creates privacy risk because it handles free-form notes, and free-form notes can contain almost anything. The responsible approach is to treat all note content and metadata as sensitive, avoid leaking real content into AI tools, review dependencies before release, and require human verification for AI-assisted work. The biggest current governance gap is that application-level encryption protects note body content, but metadata and search snippets may still need clearer handling if the project is judged as requiring whole-database encryption.
