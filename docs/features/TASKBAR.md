# Taskbar Feature

## Overview

The taskbar provides a persistent panel showing all open tools, their state, and quick-access controls. It is the most mature subsystem in VirtualDesktop in terms of architectural hygiene — it follows the Redux subscriber pattern correctly and serves as the reference implementation for how other UI subsystems (particularly the desktop) should be structured.

---

## Multi-Zone Architecture

The taskbar panel is a composite of distinct functional **zones**, not a monolithic window list. Each zone has a different paradigm and lifecycle.

```
┌─────────────────────────────────┐
│  ZONE 1: TOOL LAUNCHER (Flat)   │  Tool-First (Inventory)
│  [+] BeanShell Console          │  Always visible, acts as spawner
│  [ ] Year Calendar              │
├─────────────────────────────────┤
│  ZONE 2: ACTIVE WINDOWS         │  Window-First (Real Estate)
│  [-] [Calendar] Window (Active) │  Reflects physical frames
│  [*] [Console] Window (Min)     │  Tracks minimized state + passengers
└─────────────────────────────────┘
```

### Zone 1 — Tool Launcher / Inventory (planned)

- Flat, immutable catalog of all registered tools in the system
- Tool-first paradigm: items are blueprints/executables, not running instances
- Click spawns a new instance; shows a running-status indicator (green dot, instance count) when active
- Always visible; does not change when windows open or close

### Zone 2 — Active Windows / Window Switcher (current implementation)

- Window-first paradigm: 1:1 mapping to open `JInternalFrame` containers
- Tracks frame state: active, minimized, normal
- Shows a passenger indicator when a frame contains panels docked in from other tools
- The `WindowListController`, `WindowListView`, and `WindowListItem` classes implement this zone

### Why "window list" and not "task list"

Docking is a primary feature of VirtualDesktop. Because panels from different tools can be hosted inside the same physical frame, the list tracks **frames/windows**, not logical tools/tasks. Naming it a "task list" would be misleading; a frame may contain zero, one, or many tools depending on docking state.

### Implementation options for the zone split

- **Option A** — Single SmartGrid with group header rows separating zones
- **Option B** — Two stacked SmartGrid instances in a `JSplitPane` or vertical `BoxLayout`

---

## Architecture

The window list (Zone 2) follows a clean three-layer design:

```
Redux Store (AppState)
      │  dispatch / subscribe
      ▼
WindowListController     ← StoreSubscriber; holds WindowListView interface
      │  builds view models
      ▼
WindowListItem[]         ← POJO view model (no UI dependencies)
      │  rendered by
      ▼
WindowListView impl      ← JListWindowListView or SmartGridWindowListView
```

The controller holds only the `WindowListView` interface. Swapping the concrete view (JList, SmartGrid, or a future implementation) requires one line of change in `App.java` and no changes to the controller or Redux machinery.

---

## Key Classes

### State Layer (Redux)

| Class | Role |
| :--- | :--- |
| `state/model/ToolInstance.java` | Immutable record for one open tool: id, type, title, `FrameState`, `DockingState` |
| `state/model/ToolsState.java` | Immutable map of all `ToolInstance`s; copy-on-write mutators |
| `state/model/WindowListState.java` | Grouping mode, selected tool id; copy-on-write mutators |
| `state/model/FrameState.java` | Enum: `NORMAL`, `MINIMIZED`, `MAXIMIZED`, `HIDDEN` |

All state objects are immutable. Mutators return new instances (`withFrameState()`, `withGroupingEnabled()`, etc.).

### Controller Layer

**`taskbar/WindowListController.java`**

- Implements `StoreSubscriber` and `WindowListViewListener`
- Subscribes to `AppStore` in its constructor
- `onStateChanged()` runs on the EDT (via `SwingUtilities.invokeLater`) and calls:
  - `buildItems()` — builds a `List<WindowListItem>` from `AppState.getTools()`
  - `view.setItems()` and `view.setSelectedId()` — pushes data to the view
- Provides operations: `activateTool(String toolId)`, `setGroupingEnabled(boolean)`, `toggleGrouping()`
- Exposes static convenience methods for BeanShell scripting access:
  - `WindowListController.toggleWindowListGrouping()`
  - `WindowListController.setWindowListGrouping(boolean)`

### View Interface

**`taskbar/WindowListView.java`**

```java
JComponent getComponent();
void setItems(List<WindowListItem> items);
void setSelectedId(String toolId);
void setListener(WindowListViewListener listener);
void applyTheme(WindowListTheme theme);
```

### View Implementations

**`taskbar/JListWindowListView.java`** — JList-based, groups trigger context popup on left-click

**`taskbar/SmartGridWindowListView.java`** — SmartGrid-based; compact 34px rows; 3-column layout (icon → title → close ×); Muted Gold active indicator; inline group expansion; execution progress meter

### View Model

**`taskbar/WindowListItem.java`**

- Plain Java object, no Swing imports
- Represents either a single tool or a group of tools
- Computes `DockingIndicator` from child items (docked-in / original-present flags)
- Consumed by both view implementations

### Theme

**`taskbar/WindowListTheme.java`**

Color palette POJO. Factory methods:
- `WindowListTheme.darkGold()` — Muted Gold active indicator; dark panel background
- `WindowListTheme.light()` — Blue-based light theme

Applied via `WindowListController.applyTheme(theme)` → `view.applyTheme(theme)`. The SmartGrid implementation propagates color changes to the component pool via a shared `themeHolder[]` array, so pool instances pick up new colors without recreation.

---

## Data Flow

### Tool opens (e.g. user double-clicks a shortcut)

```
VShortcut.invoke()
  → DesktopAction.actionPerformed()
  → DesktopManager.createAppFrame()
  → AppStore.dispatch(TOOL_OPENED)
  → AppReducer → ToolsReducer adds ToolInstance
  → AppStore notifies WindowListController
  → buildItems() builds new WindowListItem list
  → view.setItems() updates the visual list
```

### Tool minimized / restored / closed

`DesktopManager` wires `InternalFrameListener` to each `VirtualAppFrame` and dispatches the corresponding action (`TOOL_MINIMIZED`, `TOOL_RESTORED`, `TOOL_CLOSED`, `TOOL_ACTIVATED`, `TOOL_DEACTIVATED`). The window list updates automatically via the subscriber.

### User clicks a window list item

```
WindowListView (user click)
  → WindowListViewListener.onItemSelected(toolId, isGroup)
  → WindowListController.activateTool(toolId)
  → VirtualAppFrame.restore() + toFront()
```

---

## Grouping

Tools can be grouped by type (class name) in the window list display.

- `WindowListState.groupingEnabled` controls whether grouping is active
- `WindowListState.groupingMode`: `BY_TYPE` (current), `BY_DOCKING` (future), `NONE`
- When enabled, `buildItems()` produces one group `WindowListItem` per type containing child items
- Right-click on a group item shows a popup to activate all, close all, etc.
- In `SmartGridWindowListView`: groups expand inline — a bold group header row followed by child rows
- Toggle via `WindowListController.toggleGrouping()` or BeanShell: `WindowListController.toggleWindowListGrouping()`

---

## Docking Indicators

Each `WindowListItem` carries a `DockingIndicator` computed from its tool's docking state:

| Indicator | Meaning |
| :--- | :--- |
| Original panel present | Tool's own panel is still inside its frame |
| Has external content | Frame contains panels docked in from other tools |

The renderer currently expresses these as text prefixes. Future plan: icon badges (deferred pending icon dependency decision).

---

## Redux Actions

| Action | Trigger | Effect |
| :--- | :--- | :--- |
| `TOOL_OPENED` | Frame created | Adds `ToolInstance` to `ToolsState` |
| `TOOL_CLOSED` | Frame closed | Removes `ToolInstance` |
| `TOOL_MINIMIZED` | Frame iconified | Sets `FrameState.MINIMIZED` |
| `TOOL_RESTORED` | Frame deiconified | Sets `FrameState.NORMAL` |
| `TOOL_ACTIVATED` | Frame focused | Sets active tool; updates window list selection |
| `TOOL_DEACTIVATED` | Frame loses focus | Clears active state |
| `WINDOWLIST_TOOL_SELECTED` | User clicks item | Updates `WindowListState.selectedToolId` |
| `WINDOWLIST_GROUPING_TOGGLED` | User or script | Flips `WindowListState.groupingEnabled` |

---

## Known Gaps

- **Frame icon cache** — `WindowListController` reads `VirtualAppFrame` references directly from `DesktopManager.getFrames()` to resolve icons. This is a point of tight coupling; ideally the icon would travel with `ToolInstance` in the Redux state.
- **No shortcut link** — `ToolInstance` does not record which desktop shortcut launched it. See `docs/features/DESKTOP.md` for the planned `linkedToolId` field.
- **Badge rendering** — Docking indicator badges are deferred; currently text-only.
- **Zone 1 (Tool Launcher)** — Not yet implemented; see Multi-Zone Architecture section above.

---

## Reference

- `state/store/StoreSubscriber.java` — interface to implement for reactive updates
- `state/store/AppStore.java` — `subscribe()`, `dispatch()`, `getState()`
- `docs/features/DESKTOP.md` — desktop subsystem; target architecture mirrors this one
