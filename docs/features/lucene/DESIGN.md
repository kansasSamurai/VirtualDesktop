# Lucene Integration Design

## Architectural Philosophy

- **In-Process Only:** Lucene runs embedded in the JVM. No external search daemons or ports.
- **Isolate-by-Default:** Each tool gets its own physical directory under `data/lucene/<sandbox-id>/`.
- **Text as Truth:** Indices are disposable caches. The source files on disk are the authority.
- **Fail-Fast Locks:** `LockObtainFailedException` marks the sandbox `ERROR` in the UI rather than crashing.
- **Async Processing:** Indexing runs on background threads; the Swing EDT only reads `volatile`/`Atomic` state.

---

## Package Map

```plain
org.jwellman.lucene
├── model
│   ├── AnalyzerType              enum  — tokenization strategy selector
│   ├── LuceneGlobalConfig        POJO  — root config, serialized to lucene-config.json
│   ├── DirectorySandboxConfig    POJO  — per-sandbox static settings
│   ├── SandboxRuntimeState       POJO  — live telemetry (volatile + AtomicInteger)
│   └── IndexRowItem              POJO  — sidebar SmartGrid row wrapper
├── engine
│   ├── LuceneDocumentSchema      constants — field name literals
│   ├── AnalyzerFactory           factory  — maps AnalyzerType → Lucene Analyzer
│   ├── IndexSandboxManager       lifecycle — open/close/purge/commit per sandbox
│   ├── BulkIndexer               Runnable — Phase 3 scan/incremental-update/commit pipeline
│   ├── LuceneService             singleton — owns all managers + thread pool; adhoc init guard
│   ├── LuceneConfigLoader        I/O      — Jackson load/save of lucene-config.json
│   └── SearchResult              DTO      — immutable Phase 4 search hit (title, path, score)
└── ui
    ├── LuceneManagementPanel     panel  — top-level JTabbedPane (Management + Search tabs)
    ├── LuceneSidebarPanel        panel  — JTable-based sandbox list with live indicators
    ├── LuceneDetailPanel         panel  — config form + activity log (vertical JSplitPane)
    └── LuceneSearchPanel         panel  — Phase 4 debounced omni-search + SmartGrid results
```

---

## Data Model

| Class | Responsibility |
| :--- | :--- |
| `LuceneGlobalConfig` | Root config POJO; owns `List<DirectorySandboxConfig>`; persisted to `config/lucene-config.json` |
| `DirectorySandboxConfig` | Stable settings for one sandbox: id, displayName, sourcePath, filter, analyzerType |
| `SandboxRuntimeState` | Live thread-safe telemetry: status, docCount (AtomicInteger), progress, errorMessage |
| `IndexRowItem` | UI bridge: pairs config + runtimeState for cell renderers |
| `IndexSandboxManager` | Owns FSDirectory + IndexWriter for one sandbox; open/close/purge/commit |
| `LuceneService` | Singleton; lazy init via vapp; holds map of managers + `ExecutorService`; JVM shutdown hook |
| `BulkIndexer` | `Runnable` submitted to thread pool; scans source dir, performs incremental update, commits |

---

## BulkIndexer — Incremental Scan Behavior

A `BulkIndexer` is submitted for every sandbox each time the service initializes (and again when a sandbox is added or reindexed via the UI button). The scan is **incremental**: it does not blindly rewrite the whole index on every startup.

**Startup scan cost on a warm index** (nothing changed):

1. Open a `DirectoryReader` on the existing index and read all stored `id` + `last_modified_stored` fields into a `Map<String, Long>`.
2. Walk the source directory and collect matching files.
3. Compare each file's `Files.getLastModifiedTime()` against the stored value — skip if equal or older.
4. Call `writer.commit()` (a no-op if no documents were added/deleted).

No file content is read and no Lucene documents are written for unchanged files, so a warm-index startup scan is cheap regardless of corpus size.

**What triggers an actual write:**

- File is new (not in the index map) → `addDocument`
- File's `last_modified` is newer than the stored value → `deleteDocuments` + `addDocument`
- File was deleted from disk but still in the index → `deleteDocuments`

---

## UI Layout

```plain
+------------------------------------+---------------------------------------+
| SIDEBAR (LuceneSidebarPanel)       |  DETAIL (LuceneDetailPanel)           |
+------------------------------------+---------------------------------------+
| ● Global / System Controls         |  [Title: selected sandbox name]       |
|   Sandboxes: 2                     |  Documents: N                         |
|                                    |  Source Path:  [read-only field]      |
| ● Script Repository                |  Index Path:   [read-only field]      |
|   Idle  |  Docs: 1,422             |  File Filter:  [editable field]       |
|                                    |  Analyzer:     [combo box]            |
| ● Personal Notes                   |                                       |
|   Scanning... Docs: 210            |  [ Reindex Directory ]                |
|   ████████░░░░░░░░░░░░ (progress)  |  [ Commit Active Transactions ]       |
|                                    +---------------------------------------+
| ● AuraCode Traces                  |  ACTIVITY LOG                         |
|   Watching  |  Docs: 3,110         |  [INFO] [10:14:02] Scanning ...       |
|                                    |  [SUCCESS] [10:14:05] Commit done     |
+------------------------------------+---------------------------------------+
```

Status dot colors: IDLE=green, SCANNING=blue, WATCHING=green, ERROR=amber.
(IDLE and WATCHING share green; when file monitoring is implemented the distinction can be revisited.)

---

## Write Lock Lifecycle

Each sandbox index directory may contain a `write.lock` file managed by Lucene's `NativeFSLockFactory`.

| Fact | Detail |
| :--- | :--- |
| **What holds it** | `BulkIndexer` — it opens an `IndexWriter` at the start of its run, commits, then closes the writer at the end (in a `finally` block). |
| **When it exists** | Only during an active `BulkIndexer` run. Before indexing starts and after it finishes the directory is lock-free. |
| **The file never disappears** | `NativeFSLockFactory` never deletes `write.lock`. The file persists on disk after the lock is released, but the OS channel lock is gone. Seeing the file between runs is normal; it is harmlessly re-locked on the next indexing run. |
| **Abnormal termination** | If the JVM is killed while `BulkIndexer` is running, the OS channel lock is reclaimed when the process dies (immediately on Linux/macOS; occasionally delayed on Windows). |
| **Automatic recovery** | `BulkIndexer.openWriter()` catches `LockObtainFailedException`. If the sandbox is not in SCANNING state (i.e., no other BulkIndexer is actively running), it deletes the stale `write.lock` and retries once. If the retry also fails the sandbox is placed in `ERROR` state. |

---

## Document Field Schema

| Field | Lucene Type | Stored | Indexed | Notes |
| :--- | :--- | :--- | :--- | :--- |
| `id` | StringField | yes | yes | Absolute file path; not tokenized |
| `title` | TextField | yes | yes | File name or document title |
| `contents` | TextField | no | yes | Full text; not stored (file is the truth) |
| `last_modified` | LongPoint | yes | yes | Epoch millis; used for sort/filter |

---

## Analyzer Strategy

| AnalyzerType | Lucene Class | Use Case |
| :--- | :--- | :--- |
| `STANDARD` | `StandardAnalyzer` | Prose text, notes, Markdown |
| `WHITESPACE` | `WhitespaceAnalyzer` | Data where punctuation is meaningful |
| `CODE_SYNTAX` | `PatternAnalyzer([^\w._]+)` | Source code; preserves `USER_ID`, `sys.log` |

---

## Config File

**Location:** `virtualdesktop-java8/config/lucene-config.json`

```json
{
  "version": "1.0.0",
  "baseIndexDirectory": "data/lucene",
  "maxBackgroundThreads": 2,
  "autoStartFileWatcher": false,
  "sandboxes": []
}
```

`baseIndexDirectory` is relative to the working directory (project root in IDE/Maven runs).
Sandboxes are appended here when added via `LuceneService.addSandbox()`.

---

## Data Directory Layout

```plain
virtualdesktop-java8/
└── data/
    └── lucene/
        ├── scripts/        ← index for "Script Repository" sandbox
        │   ├── segments_N
        │   ├── write.lock
        │   └── ...
        └── notes/          ← index for "Personal Notes" sandbox
            └── ...
```

---

## Service Initialization

Lucene is not initialized at `App` startup. It initializes the first time `SpecLuceneManagement` is opened:

```plain
SpecLuceneManagement constructor
  └── LuceneService.isInitialized() == false
        └── LuceneConfigLoader.load()       → reads config/lucene-config.json
        └── LuceneService.initialize(cfg)
              └── Files.createDirectories(data/lucene)
              └── for each sandbox: IndexSandboxManager.open()
              └── Runtime.addShutdownHook(shutdown)
              └── initialized = true
```

**Gap:** No standard service-initialization registry exists in the project. The `isInitialized()` flag is an adhoc mechanism. Future work: `ServiceRegistry` singleton to formalize this pattern.

---

## Known Gap: No Vapp Closing Lifecycle

The virtual desktop has no mechanism to notify a vapp that its `JInternalFrame` has been dismissed.

**Current behaviour:**

| Event | Effect |
| :--- | :--- |
| First open of Lucene vapp | `LuceneService.initialize()` runs; `FSDirectory` handles opened; no write lock |
| User closes the `JInternalFrame` | Nothing — `LuceneService` singleton is untouched; `FSDirectory` handles remain open |
| User reopens the vapp | `isInitialized()` guard prevents double-init; existing directories reused |
| Clean JVM exit | Shutdown hook fires → `FSDirectory.close()` on each sandbox |
| JVM killed | No cleanup needed for the `FSDirectory`; if `BulkIndexer` was mid-run, stale `write.lock` is auto-cleared on next indexing attempt |

**Consequence:** `FSDirectory` handles are held from first-open until JVM exit. This is a file-descriptor cost, not a lock, so it is far less impactful than the previous design where `IndexWriter` instances (and their write locks) were held indefinitely. It is acceptable for a single-session personal tool.

**Future work:** When the desktop gains a vapp-closing lifecycle event (an `onClose()` callback or an `InternalFrameListener` wired by the framework), `SpecLuceneManagement` should call `LuceneService.get().shutdown()` there and reset `initialized` to `false` so a subsequent open reinitializes cleanly.

---

## Implemented Phases

| Phase | Description |
| :--- | :--- |
| Phase 1 | Infrastructure — `IndexSandboxManager`, `LuceneService`, `LuceneConfigLoader`, fail-fast lock with stale-lock auto-recovery |
| Phase 2 | Document schema (`LuceneDocumentSchema`), `AnalyzerFactory`, full management UI |
| Phase 3 | Indexing Pipeline — `BulkIndexer` with `Files.walk()`, incremental timestamp diffs, `ExecutorService` thread pool |
| Phase 4 | Query UI — `LuceneSearchPanel`, `SearcherManager`, debounced omni-search (150 ms / 3-char minimum), async background thread, SmartGrid results |

## Deferred Phases

| Phase | Description |
| :--- | :--- |
| Phase 3 (stretch) | Live File Monitoring — Java `WatchService` integration |
| Phase 5 | Global Search — `MultiReader` coordinating all sandbox indexes |
| Phase 5 | Highlighting — Lucene `Highlighter` for match emphasis in detail view |
