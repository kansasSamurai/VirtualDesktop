# VirtualDesktop Requirements

This document captures design decisions, requirements, and rationale for the VirtualDesktop project. It serves as a reference for maintaining consistency across development sessions.

---

## Vision & Goals

VirtualDesktop is a Java-based virtual desktop application providing a consistent, cross-platform desktop metaphor independent of the underlying OS. Key goals:

- **Self-contained desktop environment** - Not a remote desktop, but a standalone application with integrated tools
- **Java 8 compatibility** - Maintains compatibility with legacy dependencies (with future Java 9+ version planned)
- **Integrated development tools** - BeanShell, Groovy console, database tools, charting, etc.
- **Extensibility** - Easy to add new tools via configuration or scripting

### UI Architecture Philosophy

The deeper goal behind abstracting the Desktop and Taskbar interfaces is **view substitutability**: the application's core logic — state, tool lifecycle, Redux data flow — should be completely independent of any particular view implementation. A different view can be dropped in and the application works exactly as before.

This is a product strategy as much as an engineering goal. The measure of success is the first reaction someone has when they encounter VirtualDesktop:

> *"This works great — I want to make it look like mine."*

If that's their first thought, the architecture won. It means the features hold up, data flows correctly, and the only remaining opportunity is presentation. The alternative — "these features don't work right" — means behavior is still entangled in the view layer, and no amount of reskinning will fix it.

Practically this means: state lives in Redux, controllers mediate all mutations, views are pure renderers, and operations go through defined service interfaces. The Taskbar is close to this ideal today. The Desktop is the next migration target.

---

## Terminology

| Term | Usage | Context |
| :--- | :--- | :--- |
| **tool** | User-facing | UI labels, documentation, user messages |
| **vapp** | Internal only | Class names, code comments referring to internal artifacts |
| **VirtualAppSpec** | Internal | Base class for tool specifications |

**Rationale:** Users understand "tool" intuitively. "vapp" (virtual app) is an implementation detail.

---

## Features

- [BeanShell Integration](#beanshell-integration)
- [External Applications](#external-applications)
- [Tool Configuration](#tool-configuration)
- [UI/UX Guidelines](#uiux-guidelines)
- [Desktop](#desktop)
- [Taskbar](#taskbar)
- [Credential Management](#credential-management)
- [State Management (Redux-Style)](#state-management-redux-style)

### BeanShell Integration {#beanshell-integration}

#### Shared Interpreter Architecture

**Decision:** All BeanShell-enabled tools share a single interpreter instance via `BeanShellService` singleton.

**Rationale:**

- Memory efficiency (one interpreter vs N)
- Inter-tool communication via shared `global.*` namespace
- Console can inspect/modify any tool's state
- Variables persist across tool launches

**Constraints:**

- Console must be injected BEFORE interpreter creation (BeanShell's `setConsole()` is incomplete)
- `initializeEnvironment()` currently disabled due to console initialization conflicts
- Namespace collisions possible - scripts should use unique names or scoped objects

#### Script-Backed Tools

**Decision:** Tools can be defined entirely by BeanShell scripts via `SpecBeanShellScript`.

**Patterns:**

1. **Simple** - Script returns a JComponent directly (no lifecycle support)
2. **Advanced** - Script returns `this` object with lifecycle methods (recommended)

**Lifecycle methods** (all optional):

- `getContent()` - Returns the JComponent to display
- `configure(attrs)` - Called with configuration attributes from vapps-config.json
- `launch()` - Called when the tool window is displayed

**Example advanced pattern:**

```bsh
myTool() {
    // ... create UI ...
    getContent() { return panel; }
    configure(attrs) { /* handle config */ }
    launch() { /* on display */ }
    return this;
}
global.myTool = myTool();
return global.myTool;  // Return "this" for lifecycle support
```

---

### External Applications

#### Placeholder Panel Design

**Decision:** Tools that launch external processes or display in external JFrames show a consistent placeholder panel in the virtual desktop.

**Design elements:**

- Centered layout with 20px padding
- Bold title describing the tool type
- Italicized path/command information
- Blue underlined "click here" link for re-launch/bring-to-front
- Consistent styling across: `SpecHtmlViewer`, `ExternalAppSpec`, `AbstractExternalApp`

**Rationale:** Provides visual consistency and user affordance to re-access external content.

---

### Tool Configuration

#### vapps-config.json Structure

**Location:** `config/vapps-config.json`

**Key elements:**

- `menuStructure` - Defines menu organization and tool entries
- `desktopShortcuts` - Desktop icon definitions
- `attrs` - Custom attributes passed to `Configurable` tools

**Example entry with attributes:**

```json
{
  "class": "org.jwellman.virtualdesktop.vapps.SpecBeanShellScript",
  "title": "Scripted Notepad",
  "icon": "document176",
  "enabled": true,
  "attrs": {
    "scriptPath": "src/main/resources/org/jwellman/bsh/scripts/notepad.bsh",
    "defaultText": "Welcome text here..."
  }
}
```

---

### UI/UX Guidelines {#uiux-guidelines} {{anchor uiux-guidelines}}

#### Portable Tool Design

Tools should follow these guidelines for framework compatibility:

1. Do NOT set a Look and Feel (let framework handle it)
2. Do NOT use your own container (JFrame/JInternalFrame/JDialog)
3. Use JPanel for content (framework handles container placement)
4. Do NOT call `System.exit()` directly
5. Do NOT use modal/non-modal dialogs directly; use portable interface
6. OS-specific behavior must check for OS first and handle gracefully

---

### Desktop

The desktop is the primary surface where shortcuts are placed and tools are launched. The target design follows the same Redux-subscriber pattern as the taskbar: an authoritative `DesktopState` slice in the Redux store, a `DesktopController` that subscribes to state changes, and `VShortcut` as a pure rendering component.

Current implementation has shortcuts created imperatively in `App.java` with hardcoded positioning. Shortcut state is not tracked in Redux.

See **[docs/features/DESKTOP.md](features/DESKTOP.md)** for full design, current deficiencies, and incremental migration plan.

---

### Taskbar

The taskbar shows all open tools and their lifecycle state (minimized, active, grouped). It is implemented as a Redux subscriber (`WindowListController`) backed by immutable state (`WindowListState`, `ToolInstance`). This is the reference architecture for reactive UI subsystems in VirtualDesktop.

See **[docs/features/TASKBAR.md](features/TASKBAR.md)** for full architecture, data flow, and known gaps.

---

### Credential Management

**Problem:** Sensitive values (database passwords, API keys) are currently stored as plain text in config files (e.g., `dbconfig.json`).

**Chosen path:** `CredentialProvider` interface backed by KeePassJava2

- Define a `CredentialProvider` interface that all config/service code calls when it needs a secret — never read passwords directly from JSON
- Back the interface with **KeePassJava2** (`org.linguafranca.pwdb:KeePassJava2`), which reads an existing `.kdbx` database file
- Provide a plaintext fallback implementation for development/testing
- Unlock the `.kdbx` once at startup (master password prompt or key file); subsequent calls are in-memory lookups

**Rationale:** KeePass `.kdbx` is a well-specified, battle-tested encrypted format (AES-256 + Argon2). All major KeePass UIs (KeePassXC, KeePass2, etc.) share this format, so the user's existing password database can be reused directly — no separate credential store to maintain. Rolling a custom encrypted store is not the goal; deferring to a proven external implementation is. The `CredentialProvider` abstraction keeps secret-resolution decoupled from config parsing and lets the backing store be swapped without touching callers.

**Status:** Planned — no implementation yet.

---

### State Management (Redux-Style)

#### Architecture Overview

**Decision:** Adopt Redux-style state management with centralized store, actions, and unidirectional data flow.

**Core Concepts:**

- **Store** - Single source of truth (`AppStore` singleton)
- **State** - Immutable state objects (`AppState`, `ToolsState`, etc.)
- **Actions** - Named events describing state changes (`TOOL_OPENED`, `PANEL_DOCKED_IN`)
- **Reducers** - Pure functions computing new state from current state + action
- **Subscribers** - UI components subscribe to state changes

**Rationale:**

- Centralized state simplifies debugging and reasoning about application state
- Unidirectional flow prevents state synchronization bugs
- Action logging enables debugging and potential undo/redo
- Incremental adoption - start with taskbar, expand over time

#### State Model

```plain
AppState
├── ToolsState
│   ├── toolsById: Map<String, ToolInstance>
│   └── toolsByType: Map<String, Set<String>>
├── WindowListState
│   ├── groupingEnabled: boolean
│   ├── groupingMode: BY_TYPE | BY_DOCKING | NONE
│   └── selectedToolId: String
└── timestamp: long

ToolInstance
├── id: String (UUID)
├── toolType: String (class name)
├── title: String
├── frameState: NORMAL | MINIMIZED | MAXIMIZED | HIDDEN
└── dockingState: DockingState
```

#### Package Structure

```plain
org.jwellman.virtualdesktop.state/
├── store/     - AppStore, StoreSubscriber, Middleware
├── actions/   - Action, ActionTypes, payloads/
├── reducers/  - AppReducer, ToolsReducer, WindowListReducer
└── model/     - AppState, ToolsState, ToolInstance, etc.
```

#### Key Actions

```java
// Tool lifecycle
TOOL_OPENED, TOOL_CLOSED, TOOL_MINIMIZED, TOOL_RESTORED, TOOL_ACTIVATED

// Docking
PANEL_DOCKED_IN, PANEL_DOCKED_OUT, PANEL_LOCATION_CHANGED

// Window list
WINDOWLIST_GROUPING_TOGGLED, WINDOWLIST_TOOL_SELECTED
```

---

## Architecture Decisions

### Docking Framework

**Decision:** All VirtualAppSpecs are dockable by default (as of Oct 2022).

**Implementation:** Uses Docking Frames library with configurable themes (ECLIPSE, FLAT, etc.).

---

## Open Questions / Future Considerations

- [ ] Configuration framework for main app (currently hardcoded)
- [ ] Desktop layout persistence format — see DESKTOP.md Phase 4
- [ ] Dual-version strategy (Java 8 + Java 9+) implementation details
- [ ] Taskbar docking indicator badge rendering (icon vs. text prefix)
- [ ] `ToolInstance` should carry icon reference so taskbar has no direct `DesktopManager` dependency

---

## Change Log

| Date | Decision | Rationale |
| :--- | :--- | :--- |
| 2026-01-16 | Shared BeanShell interpreter | Memory efficiency, inter-tool communication |
| 2026-01-16 | "tool" vs "vapp" terminology | User-facing clarity |
| 2026-01-16 | Consistent placeholder panels | UX consistency for external tools |
| 2026-01-16 | Script lifecycle via returning "this" | Enable configure() and launch() callbacks |
| 2026-01-17 | Redux-style state management | Centralized state, debuggability, future undo/redo |
| 2026-01-17 | Taskbar grouping by tool type | Reduce clutter with multiple tool instances |
| 2026-01-17 | Docking state indicators | Visual feedback for panel movement |
| 2026-05-29 | Desktop/Taskbar feature docs | Formal design docs added; DESKTOP.md captures migration plan |
