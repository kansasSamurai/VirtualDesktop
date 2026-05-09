# VirtualDesktop Requirements

This document captures design decisions, requirements, and rationale for the VirtualDesktop project. It serves as a reference for maintaining consistency across development sessions.

---

## Vision & Goals

VirtualDesktop is a Java-based virtual desktop application providing a consistent, cross-platform desktop metaphor independent of the underlying OS. Key goals:

- **Self-contained desktop environment** - Not a remote desktop, but a standalone application with integrated tools
- **Java 8 compatibility** - Maintains compatibility with legacy dependencies (with future Java 9+ version planned)
- **Integrated development tools** - BeanShell, Groovy console, database tools, charting, etc.
- **Extensibility** - Easy to add new tools via configuration or scripting

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

- [BeanShell Integration] #beanshell-integration
- [External Applications](#external-applications)
- [Tool Configuration](#tool-configuration)
- [UI/UX Guidelines] #uiux-guidelines
- [Taskbar](#taskbar)
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

### Taskbar

#### Grouping

**Decision:** Taskbar supports grouping tools by type (class name).

**Behavior:**

- Model supports grouping; view optionally displays grouped/ungrouped based on user preference
- Initial implementation: group by tool type (class)
- Future: grouping by docking relationship

**Rationale:** Reduces taskbar clutter when multiple instances of the same tool type are open.

#### Docking Indicators

**Decision:** Taskbar displays indicators for docking state changes.

**Indicators:**

- **Original panel present** - Whether the tool's original panel is still in its JInternalFrame
- **Has external content** - Whether the JInternalFrame contains panels docked from other tools

**Rationale:** Users need visual feedback when panels are moved between frames via docking.

**View flexibility:** Model provides state; view decides rendering (icon badges, colors, text suffixes). Badge rendering deferred to future dependency addition.

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
├── TaskbarState
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
├── reducers/  - AppReducer, ToolsReducer, TaskbarReducer
└── model/     - AppState, ToolsState, ToolInstance, etc.
```

#### Key Actions

```java
// Tool lifecycle
TOOL_OPENED, TOOL_CLOSED, TOOL_MINIMIZED, TOOL_RESTORED, TOOL_ACTIVATED

// Docking
PANEL_DOCKED_IN, PANEL_DOCKED_OUT, PANEL_LOCATION_CHANGED

// Taskbar
TASKBAR_GROUPING_TOGGLED, TASKBAR_TOOL_SELECTED
```

---

## Architecture Decisions

### Docking Framework

**Decision:** All VirtualAppSpecs are dockable by default (as of Oct 2022).

**Implementation:** Uses Docking Frames library with configurable themes (ECLIPSE, FLAT, etc.).

---

## Open Questions / Future Considerations

- [ ] Configuration framework for main app (currently hardcoded)
- [ ] Desktop layout persistence format
- [ ] Dual-version strategy (Java 8 + Java 9+) implementation details

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
