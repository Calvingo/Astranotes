# AstraNotes Responsible Release Review

## Part 1: Privacy and PII Review

### Data the project may store
- Note content: free-form markdown text, which can include personal data, contacts, passwords, medical information, financial information, and private notes.
- Note metadata: titles, tags, notebook names, timestamps, and version history.
- Attachment metadata and blobs: file names, mime types, and potentially embedded PII.
- Plugin and export metadata: plugin state, import/export logs, and transcript-like data for actions.

### Potential privacy risks
- **PII exposure in note content**: users may store names, email addresses, phone numbers, addresses, credentials, or other sensitive personal details in notes.
- **Metadata leakage**: note titles, tags, and notebook names can reveal sensitive context even if body content is not shown.
- **Deleted data persistence**: soft-deleted notes or exported bundles might retain PII unless purge and cleanup are explicit.
- **Log and diagnostic data**: overly verbose logs or error reporting could capture titles, IDs, or decrypted content if not controlled.

### Responsible handling
- Treat all notes as potentially sensitive by default.
- Encrypt the database and avoid writing note content to logs or diagnostics.
- Keep metadata exposure minimal in UI, logs, and exports.
- Implement explicit purge flows for deleted notes and ensure no plaintext remnants remain in backups or export files unless user explicitly requests it.

## Part 2: AI Data Leakage Review

### Risks when using external AI tools
- Copying note content or metadata into prompt windows can leak personal or proprietary content to the AI provider.
- Prompts that include note text, user identifiers, or plugin data may persist in the AI tool provider’s logs.
- Generated code or design prompts that reference actual project secrets, key paths, or encrypted data variables can create inadvertent leakage.

### What should not be shared casually
- Any real note content, especially if it includes personal or sensitive text.
- User identifiers, email addresses, device IDs, or environment-specific paths.
- Encryption keys, password derivation details, or internal secret handling logic.
- Database schema with production names if it reveals internal structure and protected fields.

### Responsible prompt safety
- Use synthetic or redacted sample data for AI prompts.
- If prompt generation requires actual schema, hide or replace any user-specific data and secrets.
- Maintain a prompt library that defines safe patterns and avoids raw user content.

## Part 3: Licensing and Dependency Review

### Planned third-party components
- SQLite / SQLCipher: likely core storage and encryption dependency.
- Java standard libraries and JDBC drivers.
- Any plugin framework or prompt utility library introduced later.

### Licensing risks
- SQLCipher has a commercial license for some use cases; verify whether the intended distribution requires a paid license or if open-source builds are sufficient.
- Depending on the JDBC driver or SQLite wrapper used, there may be additional license obligations.
- Avoid dependencies with restrictive licenses or unclear redistribution terms.
- Track every dependency and its license in a `LICENSES.md` or approved dependencies list.

### Supply-chain and dependency risk
- Minimize transitive dependencies in the MVP; use standard Java and SQLite-specific libraries only.
- Review dependency sources before adding them, especially native libraries or binary blobs.
- Prefer well-known, actively maintained libraries with clear license terms.

## Part 4: Responsible AI Use

### What responsible AI use means for AstraNotes
- AI is a drafting and assistance partner, not an authority.
- Every AI-generated suggestion must be reviewed by a human owner before acceptance.
- Verification is required for security-sensitive and privacy-sensitive code.
- AI should accelerate routine work and exploration, not replace design judgment.

### Human oversight requirements
- Review AI output for correctness, security, and alignment with requirements.
- Approve generated code only after running tests and ensuring no sensitive data is exposed.
- Log the prompt, the AI output, and the decision to accept or reject it.

### Trust boundaries
- Use AI for scaffolding, boilerplate, and design exploration.
- Do not treat AI output as automatically trustworthy for encryption logic, access control, or compliance features.
- Rejected or low-confidence AI output should be documented in `REJECTED.md` or a similar review log.

## Part 5: Governance Memo

### Identified risks
- **Privacy risk**: note content and metadata are inherently sensitive and can include PII or private statements.
- **AI leakage risk**: careless prompt use can send protected content to external AI providers.
- **Licensing risk**: SQLCipher or selected JDBC/SQLite components may have non-trivial commercial or redistribution terms.
- **Governance risk**: without explicit rules, plugin and AI use can drift into unsafe or low-quality territory.

### Assumptions that must be explicit
- AstraNotes is a local-first desktop app, not a cloud service.
- Notes are treated as private by default and stored encrypted.
- Plugins are trusted extensions, not untrusted third-party code.
- AI use is advisory; humans make final decisions.
- Dependency selection will favor permissive, audited libraries.

### Practical safeguards and review expectations
- **Privacy safeguards**: encrypt all storage, do not log note bodies, purge deleted data explicitly, and require explicit export consent.
- **AI safeguards**: use redacted sample data for prompts, log prompts and outcomes, and require human review for generated output.
- **Licensing safeguards**: maintain a dependency license inventory and review SQLCipher/driver terms before release.
- **Governance safeguards**: keep decision logs, require PR review, and update `plans/WORKING-AGREEMENT.md` if policy changes.

### Recommended review process
- Before each release, perform a quick risk review of privacy, AI prompt safety, and dependency licenses.
- Document any changes to assumptions, dependencies, or AI policies in the governance memo.
- Use the `plans/privacy-governance-review.md` file as the baseline for release readiness.

---

**File location**: `plans/privacy-governance-review.md`
