# Desktop Feature

## Vision

The desktop is the primary surface of VirtualDesktop — the space where shortcuts live, tools are launched, and the user's workflow is organized. Like any well-designed UI subsystem, it should be backed by an authoritative data model that the view renders reactively. User interactions mutate the model through a defined operations interface; the view never mutates state directly.

Target architecture mirrors what the taskbar already does well: immutable Redux state, a controller that subscribes to the store, and a rendering layer that is purely a function of that state. The higher goal is view substitutability — see [UI Architecture Philosophy](../REQUIREMENTS.md#ui-architecture-philosophy) in REQUIREMENTS.md.

---

## Current Implementation

### What Exists

| File | Role |
| :--- | :--- |
| `vapps/DesktopShortcut.java` | Config POJO — deserializes from `vapps-config.json` |
| `vapps/DesktopAction.java` | `AbstractAction` subclass; launches a tool via `DesktopManager` |
| `vapps/ActionFactory.java` | Reads config, builds `DesktopAction` list, loads icons |
| `desktop/VShortcut.java` | UI component (extends `JLabel`); renders and handles all interaction |
| `vswing/VDesktopPane.java` | Paints the desktop background; no state awareness |
| `desktopmgr/DesktopManager.java` | Singleton; owns `JDesktopPane`, creates/tracks `VirtualAppFrame` instances |

### How Shortcuts Are Created Today

`App.java` iterates `ActionFactory.getListOfActions()` and manually places each desktop-only action:

```java
for (DesktopAction a : ActionFactory.getListOfActions()) {
    if (a.isDesktopOnly()) {
        VShortcut vs = new VShortcut(a, label, icon, x, y += 80);
        desktop.add(vs);
    }
}
```

Position is hardcoded (`x=10`, `y` incremented by 80). There is no persistence, layout manager, or grid.

### VShortcut Visual Design

Each shortcut tile is a `JLabel` whose painting is delegated to an inner `MyUI`
class (extends `WindowsLabelUI`). `MyUI.update()` performs all custom Graphics2D
work before and after the standard label rendering pipeline.

#### Tile Rendering

| Layer | What is painted |
|-------|----------------|
| 1 — fill | `fillRoundRect` in a semi-transparent slate (`#18191E` at α=140); hover and selection states use progressively higher-opacity blue tints |
| 2 — border | `drawRoundRect` in `MUTED_GOLD` (`#A0977C`) at 1.5 px stroke, same arc as the fill |
| 3 — label | `super.update()` — standard JLabel renders the icon centered above the text |
| 4 — external indicator | painted on top of the label for external-app shortcuts only (see below) |

Arc radius for all rounded rectangles is 15 px.

#### Label Font

All shortcuts use a single static font resource:

```java
private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 11);
```

This is applied in `init()` before `getPreferredSize()` so height calculations
already account for it. The constant is the hook for future theme integration.

#### External App Indicator

Shortcuts backed by an `ExternalAppAction` (OS-level processes launched via
`ExternalAppSpec`) display a small ↗ arrow in `MUTED_GOLD` at the bottom-right
corner of the icon graphic to signal that activation will leave the JVM sandbox.

**Detection:** `init()` sets `private boolean external = (a instanceof ExternalAppAction)`.
The flag is read in `MyUI.update()` via the `JComponent c` parameter — static
inner classes can access private members of the enclosing class through an
instance reference, so no additional API is needed.

**Geometry:** the arrow is drawn entirely in Graphics2D after `super.update()`:

```
tip   = (w - 6,  iy2 - AS + 3)   // 6px inside right border; icon-bottom-relative y
base  = (tip.x - AS,  tip.y + AS) // lower-left of the diagonal stem
AS    = 8   // arrow span in each axis
AH    = 3   // arrowhead arm length
```

`iy2 = insets.top + icon.getIconHeight()` — the bottom edge of the icon, computed
from the JLabel's insets (the 12 px `PADDING` border) and the icon's own height.
Anchoring `tipX` to `w - 6` (tile-relative) rather than the icon's right edge
keeps the indicator visible even when a large icon fills most of the tile area.

The three draw calls form the arrow:
- Stem: `(base) → (tip)`
- Arrowhead left arm: `(tip) → (tip.x - AH, tip.y)`
- Arrowhead down arm: `(tip) → (tip.x, tip.y + AH)`

Color and stroke (`MUTED_GOLD`, `STROKE_1_0`) match the tile border, visually
tying the indicator to the shortcut frame rather than to the icon content.

---

### Known Design Deficiencies

**1. `VShortcut` conflates model, view, and controller**
The class extends `JLabel` and contains data fields, mouse/motion listeners, drag logic, selection state, and painting — all in one 700-line class. This makes it untestable and impossible to observe from outside.

**2. Global mutable static selection state**
`static VShortcut lastItem` and `static VShortcut curItem` (lines 42, 46) track selection outside Redux. Thread-unsafe; not observable; not serializable.

**3. No Redux presence for shortcuts**
Shortcuts are never dispatched into the store. `invoke()` fires `action.actionPerformed()` directly. There are no `SHORTCUT_*` actions; shortcut state (position, selection, label) is invisible to the rest of the application.

**4. No lifecycle link between shortcut and tool**
When a shortcut launches a frame, the resulting `ToolInstance` in Redux has no reference back to the originating shortcut. You cannot query "which shortcut launched this tool."

**5. No position persistence**
Desktop layout is lost on restart.

---

## Target Architecture

### Data Model — `DesktopState`

A new Redux slice, parallel to `TaskbarState`:

```
DesktopState
├── shortcuts: Map<String, ShortcutInstance>   // keyed by shortcutId
├── selectedShortcutId: String
└── layout: DesktopLayout                      // grid/free-form config

ShortcutInstance
├── id: String (UUID)
├── label: String
├── iconKey: String                            // e.g. "home156"
├── targetClass: String                        // vapp class name
├── position: Point
└── linkedToolId: String                       // ToolInstance id when active, null otherwise
```

### Operations Interface — `DesktopService`

Defined interface (not a concrete class), implemented by `DesktopController`:

```java
interface DesktopService {
    void addShortcut(ShortcutInstance shortcut);
    void removeShortcut(String shortcutId);
    void moveShortcut(String shortcutId, Point newPosition);
    void invokeShortcut(String shortcutId);
    void selectShortcut(String shortcutId);
    void clearSelection();
    void saveLayout();
    void loadLayout();
}
```

### Controller — `DesktopController`

Implements `StoreSubscriber` (same pattern as `TaskbarController`):

- Subscribes to `AppStore` on construction
- `onStateChanged()` rebuilds/repositions `VShortcut` views from `DesktopState`
- User interactions on `VShortcut` dispatch Redux actions (`SHORTCUT_INVOKED`, `SHORTCUT_SELECTED`, `SHORTCUT_MOVED`)
- Manages the `JDesktopPane` reference; `VShortcut` views have no direct desktop reference

### View — `VShortcut` (refactored)

Reduced to a pure rendering component:

- No static fields
- No direct action dispatch — fires events to `DesktopController`
- Renders from a `ShortcutInstance` view model
- Mouse/drag events call back into the controller, which dispatches Redux actions

### Redux Actions to Add

```java
SHORTCUT_ADDED
SHORTCUT_REMOVED
SHORTCUT_INVOKED
SHORTCUT_SELECTED
SHORTCUT_DESELECTED
SHORTCUT_MOVED
DESKTOP_LAYOUT_LOADED
DESKTOP_LAYOUT_SAVED
```

---

## Incremental Migration Path

Because the current code runs, refactoring should be staged:

1. **Phase 1 — Introduce `DesktopState` and actions** (no UI change yet)
   Add `DesktopState` to `AppState`, add `DesktopReducer`, populate state at startup from `vapps-config.json`. Verify state in Redux store without changing any rendering.

2. **Phase 2 — Wire `DesktopController`**
   Replace the `App.java` shortcut-creation loop with a `DesktopController` that builds `VShortcut`s from `DesktopState`. Static selection fields replaced with `selectedShortcutId` in Redux.

3. **Phase 3 — Dispatch from `VShortcut`**
   Route mouse events through the controller. Dispatch `SHORTCUT_INVOKED` and `SHORTCUT_SELECTED`. Link `ShortcutInstance.linkedToolId` when a tool opens.

4. **Phase 4 — Position persistence**
   Save/restore `ShortcutInstance.position` to `config/desktop-layout.json`.

---

## Related Files

- `config/vapps-config.json` — shortcut definitions (source of truth for initial state)
- `state/model/ToolInstance.java` — running tool model (to be linked from `ShortcutInstance`)
- `state/store/AppStore.java` — Redux store
- `taskbar/TaskbarController.java` — reference implementation of the `StoreSubscriber` pattern
