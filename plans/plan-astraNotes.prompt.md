## ✅ AstraNotes SQLite-based plan (local-first, trusted plugins)

### 1) Goal
- Local-only note app
- SQLite as single source of truth
- Encrypt DB (SQLCipher) + per-note integrity
- Markdown native support
- Trusted plugin API
- Desktop cross-platform

### 2) Core components
1. NoteModel
  - id, title, body, tags, notebook, createdAt, updatedAt, version, deleted
2. SQLiteStorage
  - tables: notes, attachments, fts_notes, plugins, schema_version
  - WAL, PRAGMA secure_delete=ON, PRAGMA foreign_keys=ON
3. EncryptionManager
  - password -> Argon2id -> root key
  - SQLCipher key injection at open
  - HMAC row signature for each note (optional)
4. NoteRepository
  - create/read/update/delete/list/search
  - history events for undo
5. PluginManager (trusted)
  - init, onNoteChange, shutdown
  - limited API (no direct fs)
  - plugin data in plugin_state

### 3) Offline lifecycle
- Startup: open+migrate DB + unlock key
- Operations: create/update (tx + FTS), read/search (decrypt on demand), delete (soft tombstone + purge)
- Shutdown: clear key, close DB

### 4) Index/search
- FTS5 virtual table for markdown text + tags
- pagination + snippets

### 5) Migration + backup
- schema_version table
- incremental migrations on startup
- snapshot copy on clean shutdown/daily
- export/import JSON fallback

### 6) Next deliverables
- Detailed schema file: plans/plan-astraNotes-sqlite.md
- Code skeleton: src/storage/SQLiteNoteStorage + tests
- Plugin endpoint definitions (beforeSave etc)

---

### Decision path
- Confirm attachment strategy (BLOB vs external file refs)
- Confirm rollback and compact policies
- Confirm plugin event names and visibility
