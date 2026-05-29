# Taskbar Feature

## Overview

The taskbar provides a persistent panel showing all open tools, their state, and quick-access controls. It is the most mature subsystem in VirtualDesktop in terms of architectural hygiene — it follows the Redux subscriber pattern correctly and serves as the reference implementation for how other UI subsystems (particularly the desktop) should be structured.

---

## Architecture

The taskbar follows a clean three-layer design:

```
Redux Store (AppState)
      │  dispatch / subscribe
      ▼
TaskbarController        ← StoreSubscriber; owns JList + DefaultListModel
      │  builds view models
      ▼
TaskbarItem[]            ← POJO view model (no UI dependencies)
      │  rendered by
      ▼
TaskbarItemRenderer      ← pure ListCellRenderer; no state mutation
```

---

## Key Classes

### State Layer (Redux)

| Class | Role |
| :--- | :--- |
| `state/model/ToolInstance.java` | Immutable record for one open tool: id, type, title, `FrameState`, `DockingState` |
| `state/model/ToolsState.java` | Immutable map of all `ToolInstance`s; copy-on-write mutators |
| `state/model/TaskbarState.java` | Grouping mode, selected tool id; copy-on-write mutators |
| `state/model/FrameState.java` | Enum: `NORMAL`, `MINIMIZED`, `MAXIMIZED`, `HIDDEN` |

All state objects are immutable. Mutators return new instances (`withFrameState()`, `withGroupingEnabled()`, etc.).

### Controller Layer

**`taskbar/TaskbarController.java`**

- Implements `StoreSubscriber`
- Subscribes to `AppStore` in its constructor
- `onStateChanged()` runs on the EDT (via `SwingUtilities.invokeLater`) and calls:
  - `rebuildListModel()` — rebuilds the `DefaultListModel<TaskbarItem>` from `AppState.getTools()`
  - `updateSelection()` — syncs JList selection to `TaskbarState.selectedToolId`
- Provides operations: `activateTool(String toolId)`, `setGroupingEnabled(boolean)`, `toggleGrouping()`
- Exposes static convenience methods for BeanShell scripting access

### View Model

**`taskbar/TaskbarItem.java`**

- Plain Java object, no Swing imports
- Represents either a single tool or a group of tools
- Computes `DockingIndicator` from child items (docked-in / original-present flags)
- Consumed by `TaskbarItemRenderer` for rendering decisions

### View Layer

**`taskbar/TaskbarItemRenderer.java`**

- Implements `ListCellRenderer<TaskbarItem>`
- Pure rendering: reads `TaskbarItem`, returns a configured `JLabel`
- `buildDisplayText()` prepends docking indicator characters
- Colors and fonts reflect selection/minimized/active state
- No state mutation

---

## Data Flow

### Tool opens (e.g. user double-clicks a shortcut)

```
VShortcut.invoke()
  → DesktopAction.actionPerformed()
  → DesktopManager.createAppFrame()
  → AppStore.dispatch(TOOL_OPENED)
  → AppReducer → ToolsReducer adds ToolInstance
  → AppStore notifies TaskbarController
  → rebuildListModel() adds new TaskbarItem
  → JList repaints
```

### Tool minimized / restored / closed

`DesktopManager` wires `InternalFrameListener` to each `VirtualAppFrame` and dispatches the corresponding action (`TOOL_MINIMIZED`, `TOOL_RESTORED`, `TOOL_CLOSED`, `TOOL_ACTIVATED`, `TOOL_DEACTIVATED`). The taskbar updates automatically via the subscriber.

### User clicks taskbar item

```
JList.valueChanged()
  → TaskbarController dispatches TASKBAR_TOOL_SELECTED
  → activateTool(toolId)
  → VirtualAppFrame.restore() + toFront()
```

---

## Grouping

Tools can be grouped by type (class name) in the taskbar display.

- `TaskbarState.groupingEnabled` controls whether grouping is active
- `TaskbarState.groupingMode`: `BY_TYPE` (current), `BY_DOCKING` (future), `NONE`
- When enabled, `rebuildListModel()` produces one group `TaskbarItem` per type containing child items
- Right-click on a group item shows a popup to activate all, close all, etc.
- Toggle via `TaskbarController.toggleGrouping()` or BeanShell: `TaskbarController.toggleTaskbarGrouping()`

---

## Docking Indicators

Each `TaskbarItem` carries a `DockingIndicator` computed from its tool's docking state:

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
| `TOOL_ACTIVATED` | Frame focused | Sets active tool; updates taskbar selection |
| `TOOL_DEACTIVATED` | Frame loses focus | Clears active state |
| `TASKBAR_TOOL_SELECTED` | User clicks item | Updates `TaskbarState.selectedToolId` |
| `TASKBAR_GROUPING_TOGGLED` | User or script | Flips `TaskbarState.groupingEnabled` |

---

## Known Gaps

- **Frame icon cache** — `TaskbarController` reads `VirtualAppFrame` references directly from `DesktopManager.getFrames()` to resolve icons. This is a point of tight coupling; ideally the icon would travel with `ToolInstance` in the Redux state.
- **No shortcut link** — `ToolInstance` does not record which desktop shortcut launched it. See `docs/features/DESKTOP.md` for the planned `linkedToolId` field.
- **Badge rendering** — Docking indicator badges are deferred; currently text-only.

---

## Reference

- `state/store/StoreSubscriber.java` — interface to implement for reactive updates
- `state/store/AppStore.java` — `subscribe()`, `dispatch()`, `getState()`
- `docs/features/DESKTOP.md` — desktop subsystem; target architecture mirrors this one
