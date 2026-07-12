# LayeredDiagramTool — Feature Roadmap

## Progress Summary

| Phase | Feature | Status | Notes |
|-------|---------|--------|-------|
| Baseline | v1 prototype (JFrame, basic layers) | ✅ Complete | `layereddiagramtoolv1` — deleted |
| Baseline | v2 prototype (JFrame + layer control panels) | ✅ Complete | `layereddiagramtoolv2` — deleted |
| Baseline | Current tool (JPanel, JSON persistence, resize, snap-to-grid, property editor) | ✅ Complete | `layereddiagramtool` — 15 source files |
| 1 | VApp integration + package move + legacy cleanup | ✅ Complete | `org.jwellman.diagram`; SpecDiagramTool; vapps-config.json; demo packages deleted |
| 2 | Inner class extraction | ✅ Complete | 5 classes extracted to top-level files; GridPanel kept as private static inner of DiagramLayeredPane |
| 3 | Graph model layer | ✅ Complete | `api` / `core` / `domain.cls` packages; NodeHostPanel, EdgeRenderPanel, CanvasOverlayPanel, OrthogonalRouter; class diagram demo; two-part JSON persistence |
| 4 | Overlay-painted selection handles | ✅ Complete | Selection handles and resize moved into CanvasOverlayPanel; setBorder() and ResizeHandler cycle eliminated |
| 4.5 | Drop shadows | ✅ Complete | ShadowLayerPanel at SHADOW_LAYER=150; multi-pass procedural shadow on graph nodes; suspended during drag; toolbar toggle |
| 4.6 | Runtime theme switching | ✅ Complete | setTheme() on DiagramLayeredPane; toolbar selector; theme persisted with diagram; unresolved theme on load warns and falls back to default |
| 5 | Multi-select and group operations | ✅ Complete | Rubber-band select; ctrl-click toggle; group move; align/distribute; Select All; multi-delete |
| 5.5 | Hover-to-connect (implicit edge creation) | ⬜ Planned | Hover a node to reveal its ports; drag port-to-port without toggling "Connect"; hover disabled while anything is selected |
| 6 | Undo / Redo | ⬜ Planned | `javax.swing.undo` stack; all mutating operations |
| 7 | Cut / Copy / Paste components | ⬜ Planned | Within and across diagram sessions |
| 8 | Layer management enhancements | ⬜ Planned | Rename layers; reorder layers; lock layers |
| 9 | Export (PNG / SVG / Print) | ⬜ Future | `Graphics2D` image export; Apache Batik for SVG |
| 10 | Shape library expansion | ⬜ Future | Diamonds, parallelograms, callouts, custom paths |
| 11 | Multi-line / rich text | ⬜ Future | Wrap text within shape bounds; bold/italic inline |
| 12 | BeanShell integration | ⬜ Future | Programmatic diagram construction from scripts |
| — | **Cleanup: remove legacy string member format** | ⬜ Near-term | Remove deprecated `List<String>` field/method handling from `ClassNodeContent.promoteToStructured()` and `ClassDiagramFactory`; all saved files should be re-saved in structured `List<Map>` format first |

---

## Baseline (complete)

Three prototype generations, each in `org.jwellman.demo`:

**v1** (`layereddiagramtoolv1`) — `LayeredDiagramToolv1.java`. Extends `JFrame` directly.
Established the basic `JLayeredPane` model with manually positioned components. Entry point
for proving the layered-pane approach was viable.

**v2** (`layereddiagramtoolv2`) — `LayeredDiagramToolv2.java`. Still extends `JFrame` but
adds a layer control side panel. Separated the layer management UI from the main diagram area.

**Current** (`layereddiagramtool`) — 15 source files. The fully-featured version:

- Extends `JPanel` (framework-ready, not `JFrame`)
- 6 depth-constant layers: GRID (0), BACKGROUND (100), SHAPE (200), TEXT (300), CONNECTION (400), SELECTION (500)
- 3 component types: `DiagramShape` (rectangle, circle, triangle), `DiagramText` (editable), `DiagramConnection` (placeholder)
- Drag-and-drop with 20px snap-to-grid
- 8-way resize handles (`ResizeHandler`) with grid snapping
- Right-click context menu (layer, color, z-order operations)
- `PropertyEditorPanel` — font name, size, style, fill/border/text color
- `ColorPropertyPanel` — predefined swatches + `JColorChooser` + user color slots
- `LayerControlPanel` — per-layer visibility toggle, item count, active-layer highlight
- JSON save/load via Jackson (`DiagramData` / `LayerData` / `ComponentData` hierarchy)

The current tool still lives in the `demo` package and has its own `main()` method that creates
a bare `JFrame` — it has never been wired into the VirtualDesktop menu system.

---

## Phase 1 — VApp Integration + Package Move + Legacy Cleanup ✅

All 15 source files moved from `org.jwellman.demo.layereddiagramtool` to `org.jwellman.diagram`.
`SpecDiagramTool.java` created (extends `VirtualAppSpec`, 1200×800). Registered in
`vapps-config.json`. `main()` removed from `LayeredDiagramTool`. Legacy packages
`layereddiagramtoolv1`, `layereddiagramtoolv2`, and the original `layereddiagramtool`
demo package all deleted.

---

## Phase 2 — Inner Class Extraction ✅

Five classes extracted from `LayeredDiagramTool.java` into separate top-level files:
`DiagramLayeredPane`, `DragHandler`, `DiagramConnection`, `LayerControlPanel`,
`ResizeHandler`. `GridPanel` was kept as a private static inner class of
`DiagramLayeredPane` (only used there). `DiagramLayeredPane` made `public` to allow
access from domain packages. `LayeredDiagramTool.java` reduced to ~220 lines of
construction logic.

---

## Phase 3 — Graph Model Layer ✅

Three new packages added alongside `org.jwellman.diagram`:

**`org.jwellman.diagram.api`** — pure interfaces with no domain knowledge:
`GraphNode`, `GraphEdge`, `EdgeAttributes`, `EdgeRouter`, `CanvasComponentFactory`.

**`org.jwellman.diagram.core`** — framework implementations:
- `NodeHostPanel` — public `JPanel` + `GraphNode`; wraps domain content in `BorderLayout.CENTER`; port locations computed lazily from current bounds
- `EdgeRenderPanel` — transparent `JPanel` at `CONNECTION_LAYER`; paints all edges via `EdgeRouter`; passes all mouse events through
- `CanvasOverlayPanel` — transparent `JPanel` at `OVERLAY_LAYER`; `IDLE / EDGE_CREATION / EDGE_DRAGGING` state machine; port anchor circles + rubber-band line
- `OrthogonalRouter` — port-direction-aware: V-H-V for N/S ports, H-V-H for E/W ports, single-bend L for mixed pairs; `getApproachPoint()` for correct arrowhead angle
- `StraightLineRouter`, `DefaultGraphEdge`

**`org.jwellman.diagram.domain.cls`** — illustrative class diagram domain:
`ClassNodeContent` (pure Swing JPanel), `ClassDiagramFactory` (implements `CanvasComponentFactory`),
`ClassDiagramDemo` (static builder: 4 nodes, 3 typed edges).

**`DiagramLayeredPane` changes:** `addGraphNode()`, `addGraphEdge()`, `enterEdgeCreationMode()`,
`exitEdgeCreationMode()`, `notifyNodeMoved()`; `SELECTION_LAYER` renamed to `OVERLAY_LAYER`;
`add(comp, layer, 0)` used throughout so new components appear in front; two-part JSON
persistence (`layers` + `semanticGraph`).

**`LayeredDiagramTool` changes:** "Connect" `JToggleButton` in toolbar; `setComponentFactory()`;
`getDiagramPane()`.

**Known gap carried forward:** `ResizeBorder` on `NodeHostPanel` causes brief layout jitter
because `setBorder()` triggers `revalidate()` on a JPanel with children. Logged as Phase 4.

---

## Phase 4 — Overlay-Painted Selection Handles ✅

`ResizeBorder` was calling `setBorder()` on the selected component, triggering
`revalidate()`. For `NodeHostPanel` (real JPanel hierarchy) this produced brief layout
jitter. Selection handle rendering and resize drag were moved entirely into
`CanvasOverlayPanel`, eliminating `setBorder()` and the `ResizeHandler` install/remove
cycle for all component types.

**`CanvasOverlayPanel` changes:**
- New `ResizeDirection` enum (NONE, NW, N, NE, E, SE, S, SW, W)
- New `setSelectedComponent(Component comp)` method
- `paintComponent()` always paints selection border + 8 handle squares from
  `selectedComponent.getBounds()` in canvas coordinates, even in IDLE state
- `contains()` in IDLE returns `true` only near handle zones (8px tolerance), so resize
  drag events are captured while all other clicks pass through to components below
- `handlePressed/handleDragged/handleReleased` extended to perform resize in IDLE state;
  GraphNode resize also notifies `edgePanel.nodeUpdated()` so edges track live
- `handleMoved` updates the cursor to the appropriate resize cursor near each handle
- Constructor gains two new parameters: `UnaryOperator<Integer> snapFn` (applied per
  coordinate during resize) and `Runnable onResizeComplete` (calls `notifyModified()`)

**`DiagramLayeredPane` changes:**
- `selectComponent()` now calls `overlayPanel.setSelectedComponent(comp)` instead of
  `setBorder()` + `ResizeHandler` install
- `deselectAll()` simplified to just null out `selectedComponent` and call
  `overlayPanel.setSelectedComponent(null)`
- `overlayPanel` constructor call updated with snap lambda and notify lambda
- Unused `MouseListener` / `MouseMotionListener` imports removed

`ResizeBorder` and `ResizeHandler` are retained as source files but are no longer
referenced by the framework.

---

## Phase 4.5 — Drop Shadows ✅

New `ShadowLayerPanel` (in `org.jwellman.diagram`) placed at the new `SHADOW_LAYER = 150`
constant, between `BACKGROUND_LAYER (100)` and `SHAPE_LAYER (200)`. The panel iterates
the JLayeredPane's components in `paintComponent()`, identifies visible `GraphNode`
instances, and paints a soft multi-pass procedural drop shadow behind each using 13 passes
of `fillRoundRect` with exponential opacity falloff — zero `BufferedImage` overhead.

Shadows are deliberately **node-only** (always rectangular); other shape types deferred.

**Drag suspension**: `DragHandler.mousePressed()` calls `DiagramLayeredPane.suspendShadows()`
(hides the panel); `mouseReleased()` calls `resumeShadows()` (restores to user preference).
This avoids recomputing 13 `fillRoundRect` passes per visible node on every drag event.

**Toolbar**: "Shadows" `JCheckBox` (default ON) calls `DiagramLayeredPane.setShadowsEnabled()`,
which stores the preference in `boolean shadowsEnabled` so suspend/resume respects it.

---

## Phase 4.6 — Runtime Theme Switching ✅

**Why**: Three `CanvasTheme` implementations already exist. Swapping between them at runtime
is a natural extension — useful for presentations, for matching the ambient LAF, and for
future user-defined themes.

### What shipped

Themes remain **immutable value objects**; switching means swapping the instance reference,
not mutating one in place. The actual implementation differs from the originally-sketched
`refreshTheme()`/`ThemeRefreshable` cascade — it reuses the existing content-rebuild path
instead of adding a new one:

- `CanvasTheme` gained `getThemeName()` — a stable identifier used by the toolbar selector
  and by persistence. All three implementations (`WhiteprintCanvasTheme`,
  `BlueprintCanvasTheme`, `LightCanvasTheme`) implement it.
- `CanvasThemeRegistry` (`org.jwellman.diagram.core`) — name → theme-instance lookup table
  backing both the toolbar dropdown and file-load resolution.
- `CanvasComponentFactory.setTheme(CanvasTheme)` — new default no-op method; domain
  factories that hold a theme reference (`ClassDiagramFactory`) override it to update
  their field.
- `DiagramLayeredPane.setTheme(CanvasTheme newTheme)`:
  1. Stores the new theme reference
  2. `setBackground(newTheme.getCanvasBackground())` on the pane itself
  3. `gridPanel.setGridLineColor(newTheme.getGridLineColor())`
  4. `edgePanel.setTheme(newTheme)` (repaints with the new edge color)
  5. `componentFactory.setTheme(newTheme)`, then for every live `NodeHostPanel`:
     `componentFactory.createContentFor(nodeType, properties, onModified)` followed by
     `nhp.swapContent(newContent)` — the same rebuild-and-swap path already used after a
     property-editor commit, so no new per-component recoloring logic was needed in
     `ClassNodeContent`
- **Toolbar**: a `JComboBox<String>` ("Theme:") per diagram tab, populated from
  `CanvasThemeRegistry.names()`, calling `pane.setTheme(...)` on selection.
- **Persistence**: `DiagramData.themeName` is written on save (`theme.getThemeName()`) and
  read on load. If the name resolves via `CanvasThemeRegistry`, `setTheme()` is called
  before the semantic graph is restored (so new nodes are built with the correct theme
  immediately, no double-rebuild). If the name is present but unresolvable, the pane keeps
  its current theme and `LayeredDiagramTool` shows a warning dialog after the load
  completes (`DiagramLayeredPane.getAndClearThemeWarning()`); the diagram still loads.

### Deferred (unchanged from original plan)

- Font methods on `CanvasTheme` — requires a `ClassNodeContent` refactor (all hardcoded
  `new Font(...)` constructions replaced with theme queries); meaningful but scope-heavy
- Overlay chrome colors — `CanvasOverlayPanel` static final colors (port anchors, selection
  handles, rubber-band line) are not yet theme-connected
- Shadow color theming — `ShadowLayerPanel` always uses black; a `getShadowColor()` method
  on `CanvasTheme` could expose this

### Verification

- Switch from Whiteprint to Blueprint via the toolbar dropdown: canvas background, grid,
  edges, and all node colors update immediately without reopening the diagram
- Switch back: same result
- Two diagram tabs open simultaneously hold independent theme state
- Save a diagram, reload it: the same theme is restored
- Hand-edit a saved file's `themeName` to a bogus value and load it: a warning dialog
  appears and the diagram loads with the default theme

---

## Phase 5 — Multi-Select and Group Operations ✅

**Why**: Single-selection dragging is the baseline; multi-select is the first feature that
moves the tool from "toy" to "useful" for real diagrams with many components.

### What shipped

**Selection model:** `DiagramLayeredPane` now tracks `Set<Component> selectedComponents`
(a `LinkedHashSet`) instead of a single `Component`. `CanvasOverlayPanel` mirrors this with
`List<Component> selection`; a single selected component still gets the full 8-handle
resize border, while two or more get a plain highlight outline each and no resize
affordance (resize only ever applies to a lone selection).

**Rubber-band selection:** implemented directly on `DiagramLayeredPane`'s own
mouse listener (not the overlay) — a press-drag-release on blank canvas builds a
normalized `Rectangle` and calls `CanvasOverlayPanel.setMarqueeRect()` for live
rendering (translucent fill + dashed border). On release, every `DiagramShape`,
`DiagramText`, and `GraphNode` whose bounds intersect the rectangle — and whose layer
is currently visible — joins the selection. Holding Ctrl during the drag adds to the
existing selection instead of replacing it; a plain click on blank canvas still
deselects immediately, matching the pre-Phase-5 behavior.

**Ctrl-click toggle and group-preserving click:** `installInteractionHandlers()` now
registers the selection-handling `MouseListener` *before* `DragHandler`, so a single
`mousePressed` gesture updates the selection first and `DragHandler` reads the
click-resolved state. Ctrl-click toggles a component in/out of the selection; a plain
click on a component that is *already* part of a multi-selection leaves the whole group
selected (so the drag that follows moves everything); a plain click on anything else
collapses to a single selection as before.

**Group move:** `DragHandler` snapshots the start bounds of every component to be
dragged at `mousePressed` — the whole selection if the pressed component is part of it,
otherwise just the pressed component alone — then applies one delta to all of them on
every `mouseDragged` event (with per-component grid snapping), unifying single- and
multi-component drags into one path. The delta is measured by converting both the press
point and the current point into `DiagramLayeredPane`'s coordinate space via
`SwingUtilities.convertPoint()` rather than using the dragged component's own
(shifting) local coordinates — an initial version used the latter and undertracked the
mouse by roughly half during group drags, with visible jitter under snap-to-grid; see
`DESIGN.md`'s Known Design Decisions for the full analysis.

**Align / Distribute:** `DiagramLayeredPane.alignSelected(Alignment)` (6-value enum:
`LEFT`, `CENTER_HORIZONTAL`, `RIGHT`, `TOP`, `MIDDLE_VERTICAL`, `BOTTOM`) aligns every
selected component to an edge/axis of the selection's combined bounding box; a no-op
below 2 selected. `distributeSelected(boolean horizontal)` spaces components evenly
between the two outermost along one axis; a no-op below 3 selected. Both are exposed via
a single "Align / Distribute" toolbar button opening a `JPopupMenu`
(`LayeredDiagramTool.showAlignMenu()`), whose items self-disable based on the current
selection count rather than requiring the user to discover the minimum via trial and error.

**Select All / Delete:** `Ctrl+A` (`DiagramLayeredPane.selectAll()`) selects every
selectable component on the canvas. `Delete` / `Backspace` (`deleteSelected()`) now
removes every component in the selection set, not just one.

**Bring Forward / Send Back:** extended to iterate the whole selection rather than a
single component, for consistency with the new multi-select model.

### Deliberate simplification

A plain (non-Ctrl) click-and-release *without* dragging on a component that is already
part of a multi-selection does **not** collapse the selection down to that one component
— it requires clicking on blank canvas or a different, unselected component first. Many
desktop tools defer this decision to `mouseReleased` (collapse only if no drag occurred);
that would need extra state to distinguish a click from the start of a drag. Deferred as
unnecessary polish for a proof-of-concept tool — the group stays selected until the user
explicitly clicks elsewhere.

### Verification

- Rubber-band drag selects multiple shapes; all highlight simultaneously
- Ctrl-drag rubber-band adds to the existing selection instead of replacing it
- Ctrl-click toggles a single component in/out of the selection
- Drag any selected shape; all move together with correct offsets, including graph nodes
  (edges track live)
- Align Left/Right/Center and Top/Bottom/Middle on 3+ shapes: all snap to the selection's
  bounding-box edge or axis; menu items disable below 2 selected
- Distribute Horizontally/Vertically on 3+ shapes: even spacing between the two outermost;
  menu items disable below 3 selected
- Ctrl+A selects all; Delete removes all selected components (including graph nodes)
- Right-click Delete on a single component still removes just that one and updates the
  overlay if it was part of a larger selection

---

## Phase 5.5 — Hover-to-Connect (Implicit Edge Creation)

**Why**: The explicit "Connect" toggle button works but is a modal detour from the
direct-manipulation paradigm most diagram tools use today: hover a node, its ports
appear, drag from a port to another node's port — no mode switch required. Most of the
underlying mechanics already exist from Phase 3; this is primarily an interaction-wiring
pass, not new infrastructure.

### Implementation

**Reused as-is (no changes needed):**

- Port hit-testing (`CanvasOverlayPanel.findNearestPort()`), the rubber-band line,
  `OrthogonalRouter`, and edge commit/persistence (`DefaultGraphEdge`,
  `DiagramLayeredPane.addGraphEdge()`) — the "Connect" toggle mode already does the
  actual connecting; only the entry trigger changes.
- The "narrow hot-zone captures the overlay even while otherwise transparent" pattern
  already used for resize handles (`isNearAnyHandle()` / `getHandleAt()` + the
  `contains()` override in `IDLE`) — port-hover-hit-capture reuses the identical
  mechanism with a different hot zone.
- Z-order hit-priority: `CanvasOverlayPanel` sits at `OVERLAY_LAYER` (500), above every
  node's own layer, so a `contains()` hit there already wins over the node's own
  click-to-select/drag listeners with no new plumbing.

**New work:**

1. **Hover tracking** — wire `mouseEntered` / `mouseExited` per `GraphNode` (in
   `DiagramLayeredPane.installInteractionHandlers()`) to a `hoveredNode` field on the
   pane/overlay.
2. **Paint gating** — in `IDLE` state, show port anchors for only the hovered node (not
   all nodes, which is what the "Connect" toggle does today — less clutter). Once a drag
   actually starts (`EDGE_DRAGGING`), fall back to showing anchors on all nodes, matching
   current behavior, so the user can see every valid drop target.
3. **Implicit drag-to-connect** — a port-press while hovering (no toggle active) jumps
   straight into `EDGE_DRAGGING`; on release, commit the edge (or cancel) and drop
   straight back to plain hover-tracking, rather than staying in `EDGE_CREATION` for a
   multi-edge session the way the toggle-driven flow does.
4. **Drag/marquee guard** — suppress hover-port-hit-capture while a resize, component
   drag, or rubber-band marquee is already in progress (reuse existing state checks).

**Design decision — selection/hover collision:** ports sit at N/S/E/W (mid-edge), the
same positions as 4 of the 8 resize handles, so a node that is both selected and hovered
would have a port anchor and a resize handle at the same pixel. Resolved by **disabling
hover entirely whenever anything is selected** (single or multi) rather than offsetting
anchors or requiring a modifier key. This sidesteps the collision outright and matches
the direct-manipulation model most comparable tools use — the user deselects (click
blank canvas) before starting a new connection, so "selected" and "about to connect" are
never simultaneous states.

**Coexistence with the toggle:** the explicit "Connect" button stays. It remains the
better flow for drawing several edges in a row without re-hovering each node
individually; hover-connect is the fast path for a single quick connection.

### Verification

- Hovering a node (nothing selected) reveals only that node's ports; moving off hides them
- Selecting any component (single or multi) suppresses hover-port reveal even when the
  mouse crosses a node
- Press-drag from a hovered port to another node's port commits an edge and immediately
  returns to plain hover-tracking (no lingering edge-creation mode)
- Releasing off any port cancels the in-progress edge cleanly
- The "Connect" toggle still works exactly as before for multi-edge sessions
- Starting a rubber-band marquee or a component drag near a node does not spuriously
  trigger port-hover capture

---

## Phase 6 — Undo / Redo

**Why**: Undo is the single most important missing feature for a drawing tool — it
transforms "risky" interactions (accidental deletes, bad moves) into recoverable ones.

### Implementation

Uses `javax.swing.undo.UndoManager` — already on the classpath as part of Java SE.

**Define `DiagramEdit` types** (each implements `javax.swing.undo.AbstractUndoableEdit`):
- `MoveEdit` — stores component, old bounds, new bounds
- `ResizeEdit` — stores component, old bounds, new bounds
- `AddEdit` — stores component, layer
- `DeleteEdit` — stores component, layer, z-order index
- `PropertyEdit` — stores component, property name, old value, new value (covers color/font changes)

**`DiagramLayeredPane`** — add `UndoManager undoManager`:
- Every mutating operation posts an edit: `undoManager.addEdit(new MoveEdit(...))`
- `DragHandler.mouseReleased()` — post `MoveEdit` only if position actually changed
- `ResizeHandler.mouseReleased()` — post `ResizeEdit`
- Toolbar add/delete buttons — post `AddEdit` / `DeleteEdit`
- `PropertyEditorPanel` change listeners — post `PropertyEdit`

**Toolbar:** add Undo (`Ctrl+Z`) and Redo (`Ctrl+Shift+Z` / `Ctrl+Y`) buttons.
Bind via `InputMap` / `ActionMap` on the `LayeredDiagramTool` panel.

**Limit:** cap the undo stack at 50 edits (`undoManager.setLimit(50)`) to avoid
unbounded memory growth in long editing sessions.

### Verification

- Move a shape; Ctrl+Z returns it to its original position
- Resize; Ctrl+Z restores original size
- Add a shape; Ctrl+Z removes it; Ctrl+Y re-adds it
- Change a color; Ctrl+Z reverts it
- 51st edit drops the oldest from the stack

---

## Phase 7 — Cut / Copy / Paste

**Why**: Duplicating shapes is the second most common drawing operation after move.

### Implementation

**`ClipboardState`** (package-private class): holds a `List<ComponentData>` serialized
from the selected components. Position data is stored relative to the centroid of the
selection so paste can land near the cursor or offset from the original.

**Operations:**

- `Ctrl+C` — serialize selected components to `ClipboardState`; keep originals in place
- `Ctrl+X` — same as Copy, then delete originals (post a compound `UndoableEdit`)
- `Ctrl+V` — deserialize `ClipboardState` into new components, placed with a fixed offset
  (+20px, +20px from the originals) to make the paste visible; add to active layer

**Cross-session paste:** because `ClipboardState` uses the existing `ComponentData` Jackson
hierarchy, it can be serialized to a string and placed on the system clipboard as
`DataFlavor.stringFlavor`. Paste into a second diagram window then deserializes the string.
This is an optional enhancement on top of in-memory paste.

### Verification

- Copy a shape; Ctrl+V places an offset duplicate
- Cut a shape; it disappears; Ctrl+V restores it at the offset position
- Undo after paste removes the pasted copy
- Copy multiple selected shapes; paste reproduces the group with correct relative positions

---

## Phase 8 — Layer Management Enhancements

**Why**: The layer panel is currently read-only beyond visibility. Giving users control
over layer names and ordering makes the tool usable for anything beyond the simplest
diagrams.

### Implementation

**Rename layers:**

- Double-click a layer name in `LayerControlPanel` to enter an inline edit (`JTextField`
  replaces the label)
- Name is stored in `LayerData.name`; `LayerControlPanel` reads it for display

**Reorder layers:**

- Up/Down arrow buttons beside each layer in `LayerControlPanel`
- Swaps the layer depth constants for the two adjacent layers; all components on both
  layers move with them
- The 6 depth constants (GRID, BACKGROUND, SHAPE, TEXT, CONNECTION, SELECTION) become
  user-configurable in their relative order; only GRID (always bottom) and SELECTION
  (always top) are fixed

**Lock layers:**

- A lock icon toggle per layer in `LayerControlPanel`
- Locked layers reject drag and resize events on their components (read-only for interaction)
- `DragHandler` and `ResizeHandler` check lock state before processing

**Layer color coding:**

- Each layer has an optional accent color (stored in `LayerData`)
- `LayerControlPanel` renders a small color swatch per layer
- Locked layers render their swatch in a muted/striped pattern

### Verification

- Rename "SHAPE" to "Boxes"; name persists on save/load
- Move TEXT layer above CONNECTION layer; text components render in front of connections
- Lock BACKGROUND layer; background shapes cannot be dragged

---

## Phase 9 — Export (PNG / SVG / Print)

**Why**: A diagram that cannot leave the application is a dead end. Export unlocks sharing,
documentation, and embedding in other tools.

### PNG export

`Graphics2D` rendering of `DiagramLayeredPane` to a `BufferedImage`:

```java
BufferedImage img = new BufferedImage(pane.getWidth(), pane.getHeight(), BufferedImage.TYPE_INT_ARGB);
Graphics2D g2 = img.createGraphics();
g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
pane.paintAll(g2);
g2.dispose();
ImageIO.write(img, "PNG", file);
```

Grid lines should be hidden before rendering (controlled by a `setGridVisible(boolean)` flag).

**SVG export** via Apache Batik (`org.apache.batik:batik-svggen`) — already a viable
dependency given the project's Apache Commons usage. `SVGGraphics2D` is a drop-in
replacement for `Graphics2D`; all existing `paintComponent()` overrides produce SVG
output without modification.

**Print** — wire `DiagramLayeredPane.print(Graphics g)` to a `PrinterJob`. Scale to fit
the page. `JComponent.print()` handles this with minimal code.

### Toolbar additions

File menu or toolbar buttons: "Export PNG…", "Export SVG…", "Print…" — each opens a
`JFileChooser` (or `PrinterJob` dialog for print).

---

## Phase 10 — Shape Library Expansion

**Why**: Rectangle, circle, and triangle cover the basics but exclude common diagram
vocabularies (flowcharts, UML, network diagrams).

### New shape types (additions to `ShapeType` enum)

| Shape | Use case |
| --- | --- |
| Diamond | Decision nodes in flowcharts |
| Parallelogram | Data/IO in flowcharts |
| Rounded Rectangle | Modern "soft" UI style; process steps |
| Cylinder | Database / storage symbols |
| Callout (speech bubble) | Annotations |
| Hexagon | Generic process nodes |

Each new type adds a `case` in `DiagramShape.paintComponent()` using `Graphics2D` path
operations. No new classes required.

**Custom path shapes (later sub-phase):** allow a `GeneralPath` to be stored in the shape
component, enabling import of SVG path data or BeanShell-constructed polygons.

---

## Phase 11 — Multi-Line / Rich Text

**Why**: `DiagramText` currently wraps a single-line `JTextField`. Most diagram labels
require word-wrap at minimum; callouts and documentation boxes need multi-line support.

### Multi-line (Phase 10a)

Replace the `JTextField` with a `JTextArea` (no scroll pane — the text area clips to the
component bounds). `DiagramText` switches to multi-line mode when the component height
exceeds `2 * fontSize`. Word-wrap is always on; line-wrap is set to word boundaries.

### Rich text (Phase 10b)

Replace `JTextArea` with `JTextPane` backed by a `StyledDocument`. Inline bold/italic/size
changes via a small popup formatting toolbar that appears on text selection. Format state
is serialized in `TextData` as a list of `StyleSpan` records (`{start, end, bold, italic, size}`).

This is a meaningful scope increase — `JTextPane` editing is non-trivial. Phase 10a should
ship and be used for a while before committing to 10b.

---

## Phase 12 — BeanShell Integration

**Why**: BeanShell is the orchestration glue in VirtualDesktop. Exposing the diagram
API to scripts lets users build diagrams programmatically — from database query results,
from file system scans, from any data source the script can reach.

### API surface to expose

```java
// From BeanShell:
DiagramLayeredPane d = diagramTool.getDiagramPane();
d.addShape(ShapeType.RECTANGLE, 100, 100, 200, 80);
d.addText("Hello World", 150, 130);
d.connect(shape1, shape2);
d.setLayerVisible(DiagramLayeredPane.TEXT_LAYER, false);
d.saveDiagram(new File("/tmp/output.json"));
```

**`DiagramLayeredPane`** — add convenience factory methods (`addShape`, `addText`, `connect`)
that return the created component for further manipulation.

**`BeanShellService` integration** — when `SpecDiagramTool` is opened, inject the
`DiagramLayeredPane` reference into the shared BeanShell namespace under a well-known
name (e.g., `diagram`), consistent with how other vapps expose their APIs to the console.

### Demo script

A sample `.bsh` script that generates a layered architecture diagram from a hardcoded
list of components — demonstrates the programmatic path end-to-end.

---

## Design Notes

### Coordinate system

All component positions are stored in `DiagramLayeredPane` coordinates (absolute pixel
offsets from the pane's top-left). The 20px grid is a display convenience, not a stored
unit — components are always stored at pixel coordinates and the grid snap rounds on input.
If the grid size ever becomes configurable (currently hardcoded to 20px in `GridPanel`),
stored positions do not need migration.

### Persistence format

`DiagramData` uses Jackson's `@JsonSubTypes` polymorphism for the `ComponentData` hierarchy.
New component types (e.g., `ConnectionData` in Phase 3) require a new `@JsonSubTypes` entry.
The format is not versioned; breaking changes to field names will silently drop data on load.
Consider adding a `version` field to `DiagramData` before Phase 3 ships so future migrations
have a reliable discriminator.

### Positioning within the desktop

`LayeredDiagramTool` is a **structural diagramming** tool — flowcharts, architecture
diagrams, entity relationships: shapes with meaning and arrows between them. It is not
a data-visualization tool. Data visualization (bar charts, line charts, time-series)
belongs to the chart vapps (`SpecJFreeChart`, `SpecXChartDemo`), which are the natural
integration partners for `SmartGrid`. The two categories share the word "diagram" loosely
but serve different purposes and should remain decoupled.
