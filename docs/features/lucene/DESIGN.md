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
│   ├── LuceneService             singleton — owns all managers; adhoc init guard
│   └── LuceneConfigLoader        I/O      — Jackson load/save of lucene-config.json
└── ui
    ├── LogEntry                  model  — single activity log line (level + timestamp + message)
    ├── LuceneManagementPanel     panel  — top-level JSplitPane container
    ├── LuceneSidebarPanel        panel  — JTable-based sandbox list with live indicators
    └── LuceneDetailPanel         panel  — config form + activity log (vertical JSplitPane)
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
| `LuceneService` | Singleton; lazy init via vapp; holds map of managers; JVM shutdown hook |

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

Status dot colors: IDLE=gray, SCANNING=blue, WATCHING=green, ERROR=amber.

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

## Deferred Phases

| Phase | Description |
| :--- | :--- |
| Phase 3 | Indexing Pipeline — `Files.walk()` bulk indexer, incremental timestamp diffs, `ExecutorService` thread pool |
| Phase 3 (stretch) | Live File Monitoring — Java `WatchService` integration |
| Phase 4 | Query UI — SmartGrid search results, `SearcherManager`, debounced keystrokes |
| Phase 5 | Global Search — `MultiReader` coordinating all sandbox indexes |
| Phase 5 | Highlighting — Lucene `Highlighter` for match emphasis in detail view |
