# LayeredDiagramTool — Design Reference

Architecture, component structure, and design decisions for the LayeredDiagramTool.
Open this when you need to understand *why it is built the way it is* or when extending
it with new capabilities.

---

## Introduction

LayeredDiagramTool is a structural diagramming vapp — flowcharts, architecture diagrams,
entity-relationship diagrams — built on `JLayeredPane`. It is not a data-visualization
tool; that role belongs to the chart vapps (`SpecJFreeChart`, `SpecXChartDemo`).

The tool lives in `org.jwellman.diagram` (and sub-packages `api`, `core`, `domain/cls`),
runs as a `JPanel` hosted inside a `VirtualAppSpec` (`SpecDiagramTool`), and is registered
in the VirtualDesktop menu via `vapps-config.json`.

**Current state (as of this writing):** Phases 1, 2, and 3 are complete. The canvas
supports both decorative elements (shapes, text) and a semantic graph layer (typed nodes
and edges with port-based routing). A class-diagram demo domain is included, with
self-describing startup diagrams (`ToolDiagramDemo`, `ToolFrameworkDiagram`). The domain
property editor callback seam is proven: selecting a graph node invokes
`CanvasComponentFactory.createPropertyEditorFor()`, which `ClassDiagramFactory` uses to
expose a name-editor field in the sidebar. File persistence uses the `.dgx` extension and
embeds a semantic version block.

---

## Component Hierarchy

```plain
LayeredDiagramTool (JPanel)
├── JToolBar (NORTH)
│     Add Rectangle / Triangle / Circle / Text
│     Show Grid / Snap to Grid / Shadows toggles
│     Theme selector (JComboBox) — runtime theme switching, per tab
│     Bring Forward / Send Back
│     Delete Selected
│     Load / Save / Save as...
│     Connect (JToggleButton) — enters edge-creation mode on the canvas overlay
├── JScrollPane > DiagramLayeredPane (CENTER)
│     GridPanel            @ GRID_LAYER (0)          — 20px grid lines, setOpaque(false)
│     DiagramShape*        @ any user layer           — rectangle, circle, triangle
│     DiagramText*         @ any user layer           — editable text
│     NodeHostPanel*       @ any user layer           — graph node wrapping a domain JPanel
│     EdgeRenderPanel      @ CONNECTION_LAYER (400)   — transparent; paints all graph edges
│     CanvasOverlayPanel   @ OVERLAY_LAYER (500)      — transparent; selection handles/highlights, marquee, port anchors + rubber band
└── layerPanel (EAST, 280px)
      Layers section
        LayerControlPanel × 6  — one per named layer (top→bottom order)
      PropertyEditorPanel
        font name / size / style fields
        ColorPropertyPanel
          fill / border / text color target selector
          predefined swatches + user color slots
          JColorChooser launch button
```

---

## Key Classes

### `org.jwellman.diagram` — tool and decorative elements

| Class | Role |
| --- | --- |
| `LayeredDiagramTool` | Top-level `JPanel`; builds toolbar, pane, and sidebar; owns `modified` flag, save/load dialogs, and a local `CanvasComponentFactory` reference; exposes `setComponentFactory()` and `getDiagramPane()`; routes node selection to the domain property editor |
| `DiagramLayeredPane` | `public JLayeredPane` subclass; owns all diagram components, layer state, selection model, drag wiring, popup menu, graph node/edge registry, and save/load serialization |
| `DragHandler` | `MouseAdapter`; handles drag-to-move with snap-to-grid, including group moves when the pressed component is part of a multi-selection; notifies `DiagramLayeredPane.notifyNodeMoved()` when a `GraphNode` is dragged so edges redraw |
| `ResizeHandler` | `MouseAdapter`; installed/removed per selection cycle; 8-direction resize; snap-to-grid on each drag event |
| `LayerControlPanel` | Row UI for one layer: visibility toggle, name label, item count, active-layer highlight; uses a `Timer` to poll item counts |
| `DiagramShape` | `JComponent`; paints rectangle, circle, or triangle via `Graphics2D`; implements `DiagramColorable` |
| `DiagramText` | `JPanel` wrapping a `JTextField`; implements `DiagramColorable` + `DiagramTextAware`; dispatches mouse events to parent for drag |
| `DiagramConnection` | `JComponent` placeholder; draws a straight line with an arrowhead between hardcoded endpoints; superseded by the graph model layer |
| `ResizeBorder` | Custom `Border`; paints 8 square handle indicators at corners and edge midpoints when a component is selected |
| `PropertyEditorPanel` | Right-panel editor for the selected component's font and color properties; also exposes `showNodeEditor(JPanel)` to display a domain-provided panel without importing any domain types |
| `ColorPropertyPanel` | Sub-panel of `PropertyEditorPanel`; manages fill/border/text color target, swatch grid, and `JColorChooser` integration |
| `ColorSwatch` | 24×24 px colored square with hover effect |

**Persistence (Jackson):**

| Class | Role |
| --- | --- |
| `DiagramData` | Root object: `FileVersion version`, `domainType`, `themeName`, `gridSize`, `snapToGrid`, `activeLayer`, `List<LayerData>`, optional `SemanticGraphData` |
| `FileVersion` | Value object: `major`, `minor`, `patch` ints + `CURRENT_*` constants + `current()` factory; `toString()` → `"0.1.0"` |
| `UnsupportedFormatException` | Thrown by `DiagramLayeredPane.validateFormat()` when the file's major version exceeds the supported maximum |
| `LayerData` | One layer: `layerDepth`, `visible`, `List<ComponentData>` |
| `ComponentData` | Abstract base with `@JsonTypeInfo` / `@JsonSubTypes` for polymorphic dispatch |
| `ShapeData` | Extends `ComponentData`; captures bounds, `ShapeType`, fill/border colors |
| `TextData` | Extends `ComponentData`; captures bounds, text, font name/size/style, all three colors |
| `SemanticGraphData` | Container for `List<GraphNodeData>` and `List<GraphEdgeData>` |
| `GraphNodeData` | Node snapshot: id, type, properties map, x/y/w/h, layer |
| `GraphEdgeData` | Edge snapshot: id, source/target node+port ids, lineStyle, arrowType |

**Interfaces:**

| Interface | Methods | Purpose |
| --- | --- | --- |
| `DiagramColorable` | `getFillColor / setFillColor / getBorderColor / setBorderColor` | Color contract for shapes and text |
| `DiagramTextAware` | `getFontName/set, getFontSize/set, getFontStyle/set, getTextColor/setTextColor` | Font and text color contract |

---

### `org.jwellman.diagram.api` — framework interfaces (no Swing except Point/JComponent)

| Interface / Class | Role |
| --- | --- |
| `GraphNode` | Identity, type, property map, port IDs, canvas-coordinate port locations, visual component accessor |
| `GraphEdge` | Directed edge: source/target node + port IDs, `EdgeAttributes` |
| `EdgeAttributes` | Value class: `LineStyle` (SOLID/DASHED), `ArrowType` (OPEN/FILLED/NONE), color, stroke width |
| `EdgeRouter` | Strategy: `calculatePath(start, startPortId, end, endPortId)` and `getApproachPoint(...)` for arrowhead orientation |
| `CanvasComponentFactory` | Domain plugin: `createContentFor(nodeType, properties)` → `JPanel`; `getPortIds(nodeType)`; `default createPropertyEditorFor(nodeType, properties, onChanged)` → `JPanel` (returns `null` by default — no editor) |
| `CanvasTheme` | Color palette for the canvas surface and all nodes; consumed by `DiagramLayeredPane` (canvas bg + grid) and `CanvasComponentFactory` implementations (node colors) |

---

### `org.jwellman.diagram.core` — framework implementations

| Class | Role |
| --- | --- |
| `NodeHostPanel` | `public JPanel` implementing `GraphNode`; wraps domain-provided content in `BorderLayout.CENTER`; computes port locations lazily from current bounds (no cache); `swapContent(JPanel)` replaces the content panel in-place after a property-editor rebuild |
| `EdgeRenderPanel` | Transparent `JPanel` at `CONNECTION_LAYER`; paints all edges using the configured `EdgeRouter`; always returns `false` from `contains()` to pass mouse events through |
| `CanvasOverlayPanel` | Transparent `JPanel` at `OVERLAY_LAYER`; state machine (`IDLE / EDGE_CREATION / EDGE_DRAGGING`); renders selection handles (single selection) or highlight outlines (multi-selection), the rubber-band marquee rectangle, port anchor circles, and the edge-drag rubber-band line; transparent to mouse events in `IDLE` state except near resize handles |
| `OrthogonalRouter` | Port-direction-aware L/Z router: V-H-V for N/S ports, H-V-H for E/W ports, single-bend L for mixed pairs |
| `StraightLineRouter` | Straight line from start to end |
| `DefaultGraphEdge` | Simple immutable `GraphEdge` implementation used for both interactive and persisted edges |
| `LightCanvasTheme` | `CanvasTheme` implementation: white canvas, light grey grid, blue class headers, green interface headers |
| `BlueprintCanvasTheme` | **Default** `CanvasTheme`: Prussian blue canvas (`#003366`), subtle grid (`#004080`), white borders and text — classic cyanotype blueprint aesthetic |

---

### `org.jwellman.diagram.domain.cls` — class diagram demo domain

| Class | Role |
| --- | --- |
| `ClassNodeContent` | Pure-Swing `JPanel`; `GridBagLayout` vertical stack; header with stereotype tint, field list, method list; colors sourced from `CanvasTheme` |
| `ClassDiagramFactory` | Implements `CanvasComponentFactory`; takes a `CanvasTheme` at construction; overrides `createPropertyEditorFor()` to provide a name-editor field that mutates the properties map and calls `onChanged` on commit |
| `ClassDiagramDemo` | Static `buildDemo(DiagramLayeredPane, CanvasComponentFactory)` method; populates 4 nodes and 3 edges illustrating class relationships |
| `ToolDiagramDemo` | Self-describing startup diagram of the tool's UI layer (9 nodes, 8 edges); wired as the default at startup in `SpecDiagramTool` |
| `ToolFrameworkDiagram` | Self-describing diagram of the framework's api interfaces and core implementations (11 nodes, 4 implements edges); call `buildDemo()` to display on demand |

---

## Layer System

Six named layers with fixed integer depth constants on `DiagramLayeredPane`:

| Constant | Depth | Contents |
| --- | --- | --- |
| `GRID_LAYER` | 0 | `GridPanel` — always at the bottom, never user-accessible |
| `BACKGROUND_LAYER` | 100 | Decorative shapes used as watermarks or region fills |
| `SHAPE_LAYER` | 200 | Default layer for new shapes and graph nodes |
| `TEXT_LAYER` | 300 | Default layer for new text components |
| `CONNECTION_LAYER` | 400 | `EdgeRenderPanel` (always present, transparent); decorative `DiagramConnection` objects |
| `OVERLAY_LAYER` | 500 | `CanvasOverlayPanel` (always present, transparent in IDLE); port anchors and rubber-band during edge creation |

**Active layer** — `DiagramLayeredPane.activeLayer` (default: `SHAPE_LAYER`). New
components are added to the active layer. `LayerControlPanel` highlights the row for
the active layer.

**Add position** — `add(component, layer, 0)` is used throughout so that newly added
components appear at the top of their layer (in front of existing components in the same
layer). The default `add(component, layer)` uses position `-1` (bottom of layer) and was
responsible for a bug where new shapes appeared behind existing ones.

**Layer visibility** — `Map<Integer, Boolean> layerVisibility` on `DiagramLayeredPane`.
Toggling visibility calls `setVisible()` on every component in that layer. The grid,
edge, and overlay panels are excluded from this logic.

---

## Interaction Model

### Selection

`DiagramLayeredPane` tracks selection as `Set<Component> selectedComponents` (a
`LinkedHashSet`), not a single component (Phase 5). `CanvasOverlayPanel` mirrors this
with its own `List<Component> selection` and renders it differently by size:

- **Exactly one selected** — full resize treatment: the overlay paints the 8-handle
  selection border and captures mouse events in the handle zones (see Resize, below).
- **Two or more selected** — a plain highlight outline per component, no resize handles.
- **None selected** — the overlay paints nothing and is fully transparent to mouse events.

**Click selection** (`installInteractionHandlers()`): a component's selection-handling
`MouseListener` is registered *before* its `DragHandler`, so a single `mousePressed`
gesture resolves selection first and `DragHandler` reads the already-updated state.

- Plain click on a component *not* already selected → replaces the selection with just
  that component (`selectComponent()`).
- Plain click on a component that *is* already part of a multi-selection → the selection
  is left untouched, so the drag that follows moves the whole group. A plain click alone,
  without a following drag, does **not** collapse the selection back to one component —
  see Known Design Decisions.
- Ctrl-click → toggles the clicked component in/out of the selection (`toggleSelection()`).

**Rubber-band (marquee) selection**: a press-drag-release on blank canvas is handled by
`DiagramLayeredPane`'s own mouse listener, not the overlay (which stays transparent over
blank canvas in `IDLE` state with no selection nearby). The drag rectangle is normalized
and forwarded to `CanvasOverlayPanel.setMarqueeRect()` for live rendering (translucent
fill + dashed border). On release, every `DiagramShape`, `DiagramText`, and `GraphNode`
whose bounds intersect the rectangle — and whose layer is currently visible — joins the
selection (`selectComponentsIn()`). Holding Ctrl during the drag adds to the existing
selection instead of replacing it. A plain click on blank canvas (no drag) still
deselects immediately.

All selection changes funnel through the private `setSelection()` method, which also
clears any edge selection and fires `selectionListener` → `PropertyEditorPanel` updates
(only when exactly one component is selected; zero or multiple selected both report
`null`, showing "No component selected").

Edge selection is mutually exclusive with component selection: selecting an edge clears
`selectedComponents`, and selecting any component clears `selectedEdge`.

### Drag

`DragHandler` is installed on every component at add time (after the selection listener,
see above) and lives for the component's lifetime. On `mousePressed`, it snapshots the
current bounds of every component to be dragged into a `Map<Component, Rectangle>` —
the whole selection if the pressed component is part of it, otherwise just the pressed
component alone. On each `mouseDragged` event:

1. Compute `dx/dy` as the distance from the press point to the current point, with both
   points converted into `DiagramLayeredPane`'s coordinate space via
   `SwingUtilities.convertPoint()` (see Known Design Decisions for why this matters)
2. Add that one delta to each component's *snapshotted* start bounds — single and
   multi-selection share this same loop, since a single selection is just a
   one-entry map
3. If snap-to-grid is on, round each component's `x`/`y` to the nearest 20px multiple
   independently
4. `setBounds()` on each moved component, then `revalidate()` + `repaint()` on the pane
5. For each moved component that implements `GraphNode`, call
   `layeredPane.notifyNodeMoved(nodeId)` so `EdgeRenderPanel` repaints affected edges

### Resize

Resize only ever applies to a single selected component — never a multi-selection (see
Known Design Decisions). `CanvasOverlayPanel` derives its resize target from
`resizableComponent()`, which returns a component only when exactly one is selected.
While that condition holds, the overlay hit-tests an 8px zone at each of the 8 handle
positions and changes the cursor accordingly. On drag: computes new bounds from the
active handle direction, enforces a 30px minimum size, applies snap-to-grid, and — for a
`GraphNode` target — invalidates its port cache and notifies `EdgeRenderPanel` so
attached edges track the live resize.

### Edge creation (Connect mode)

Toggling the "Connect" button:
- **ON** → `DiagramLayeredPane.enterEdgeCreationMode()` → `CanvasOverlayPanel` state = `EDGE_CREATION`
- **OFF** → `exitEdgeCreationMode()` → state = `IDLE`

In `EDGE_CREATION` mode the overlay captures all mouse events (`contains()` returns true).
Port anchor circles appear at each `GraphNode`'s N/S/E/W positions. Pressing within 8px
of a port begins a drag (`EDGE_DRAGGING`); releasing within 8px of a different port
commits a `DefaultGraphEdge` to `EdgeRenderPanel`. The overlay stays in `EDGE_CREATION`
after a commit so multiple edges can be drawn before toggling off.

### Right-click popup

Available on decorative components (`DiagramShape`, `DiagramText`):

- **Move to Layer** — submenu; current layer shown with ✓ and disabled
- **Bring Forward / Send Back** — nudges raw layer depth by ±1
- **Change Fill Color… / Change Border Color…** — for `DiagramColorable` components
- **Change Text Color…** — for `DiagramTextAware` components
- **Delete**

Graph nodes (`NodeHostPanel`) do not currently show a context menu.

---

## Persistence Format

JSON via Jackson. Default extension: `.dgx`; legacy `.json` files are also accepted on
load. Two top-level content sections: `layers` (decorative canvas elements) and
`semanticGraph` (graph nodes and edges). `semanticGraph` is omitted entirely when no
graph nodes exist.

A `version` block is written at the top of every file. Files without a version block
(pre-versioning `.json` files) are treated as `0.0.0` and loaded leniently. Files with a
`major` version greater than `FileVersion.CURRENT_MAJOR` (currently `0`) are rejected
with `UnsupportedFormatException` before any canvas state is touched.

```json
{
  "version": { "major": 0, "minor": 1, "patch": 0 },
  "themeName": "Whiteprint",
  "gridSize": 20,
  "snapToGrid": true,
  "activeLayer": 200,
  "layers": [
    {
      "layerDepth": 200,
      "visible": true,
      "components": [
        {
          "type": "shape",
          "x": 100, "y": 100, "width": 120, "height": 80,
          "shapeType": "RECTANGLE",
          "fillColor": "#4A90D9FF",
          "borderColor": "#2C5F8AFF"
        }
      ]
    }
  ],
  "semanticGraph": {
    "nodes": [
      {
        "id": "n-user", "type": "CLASS",
        "properties": { "name": "User", "fields": [...], "methods": [...] },
        "x": 380, "y": 80, "w": 210, "h": 160, "layer": 200
      }
    ],
    "edges": [
      {
        "id": "e-dep",
        "sourceNodeId": "n-usersvc", "sourcePortId": "W",
        "targetNodeId": "n-userrepo", "targetPortId": "E",
        "lineStyle": "SOLID", "arrowType": "FILLED"
      }
    ]
  }
}
```

Graph nodes are only restored on load if a `CanvasComponentFactory` is configured on
`DiagramLayeredPane`. If no factory is set, `semanticGraph` is silently skipped
(decoration-only fallback).

`ComponentData` uses `@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")`
with `@JsonSubTypes` entries for `"shape"` → `ShapeData` and `"text"` → `TextData`.

---

## Canvas Theming

The canvas is intentionally always rendered with a **light/white background** regardless
of the host application's Look and Feel. This preserves diagram legibility when the
surrounding VirtualDesktop switches to a dark LAF.

### `CanvasTheme` interface (`org.jwellman.diagram.api`)

Defines the full color palette for one rendering context:

| Method | Used by |
| --- | --- |
| `getCanvasBackground()` | `GridPanel` — fills the entire canvas surface |
| `getGridLineColor()` | `GridPanel` — grid line stroke color |
| `getNodeHeaderBackground(String stereotype)` | `ClassNodeContent` — header tint per node type |
| `getNodeBodyBackground()` | `ClassNodeContent` — body background and filler background |
| `getNodeBorderColor()` | `ClassNodeContent` — outer border of the node card |
| `getTextColor()` | `ClassNodeContent` — node name label and entry labels |
| `getEdgeColor()` | `EdgeRenderPanel` — edge stroke color (wiring pending) |

`stereotype` values passed to `getNodeHeaderBackground()` match the node type string
from `CanvasComponentFactory` (e.g. `"CLASS"`, `"INTERFACE"`).

### `WhiteprintCanvasTheme` (`org.jwellman.diagram.core`) — **current default**

Inverted blueprint aesthetic — technical drawing on drafting paper. A slightly cool
off-white surface (`#F0F4FA`) with blue ink: medium ink-blue borders and edges
(`#3A6BA5`), dark navy text (`#1A3E70`), steel-blue stereotype labels (`#4A7ABF`), and
a pale blue-grey grid (`#D8E5F5`). Drop shadows are especially effective on this theme
because the light background amplifies the shadow contrast.

### `BlueprintCanvasTheme` (`org.jwellman.diagram.core`)

Prussian blue canvas (`#003366`) with pale blue borders, text, and edges (`#C8DCFF`),
and a subtle brighter-blue grid (`#004080`). Evokes classic 19th-century cyanotype
blueprints. Swap this in as the default for the "draft in progress" aesthetic.

### `LightCanvasTheme` (`org.jwellman.diagram.core`)

White canvas, light grey grid, blue class headers, green interface headers.
Original light-mode appearance; predates the theming system.

### How themes flow through the system

**At construction time:**

```
DiagramLayeredPane (owns the theme, defaults to WhiteprintCanvasTheme)
  │  getTheme()
  └──► SpecDiagramTool / caller
         │
         ├──► GridPanel (canvas background + grid lines)
         │
         └──► ClassDiagramFactory(theme)
                └──► ClassNodeContent(…, theme)   ← all node colors from theme
```

`DiagramLayeredPane` is the single source of truth. Callers retrieve the theme via
`getDiagramPane().getTheme()` and pass it to any `CanvasComponentFactory` constructor.
This ensures the factory, the canvas background, and the grid lines all use the same
palette without any static state.

**At runtime (Phase 4.6 — complete):**

`DiagramLayeredPane.setTheme(CanvasTheme newTheme)` swaps the reference and drives a
direct cascade:

```
DiagramLayeredPane.setTheme(newTheme)
  ├── setBackground(newTheme.getCanvasBackground())
  ├── gridPanel.setGridLineColor(newTheme.getGridLineColor())
  ├── edgePanel.setTheme(newTheme)
  └── componentFactory.setTheme(newTheme), then for each live NodeHostPanel:
        content = componentFactory.createContentFor(nodeType, properties, onModified)
        nodeHostPanel.swapContent(content)
```

Rather than introduce a `refreshTheme()` / `ThemeRefreshable` cascade into domain content
classes, node recoloring reuses the **existing rebuild-and-swap path** that already runs
after a property-editor commit (`createContentFor()` + `swapContent()`). This means
`ClassNodeContent` needed no new theming API at all — a theme switch simply rebuilds each
node's content panel from its live `properties` map using the factory's now-updated theme.
`CanvasComponentFactory.setTheme(CanvasTheme)` is a new default no-op method that
theme-aware factories (`ClassDiagramFactory`) override to update their held theme reference
before the rebuild loop runs.

No listener or observer infrastructure is used. The cascade is a direct method-call loop;
`DiagramLayeredPane` already owns the node registry and the component factory reference,
and is the natural driver.

**`CanvasTheme.getThemeName()`** is a stable identifier (`"Whiteprint"`, `"Blueprint"`,
`"Light"`) used by two consumers: the toolbar's theme `JComboBox` (populated from
`CanvasThemeRegistry.names()`) and diagram persistence (see below).

**`CanvasThemeRegistry`** (`org.jwellman.diagram.core`) is a simple name → theme-instance
lookup table. `byName(String)` returns a fresh instance or `null` if unregistered;
`names()` returns all registered names in registration order for the toolbar dropdown.

### Theme persistence

`DiagramData.themeName` is written on save (`theme.getThemeName()`) and read on load.
On load, `DiagramLayeredPane`:

- resolves the name via `CanvasThemeRegistry.byName(...)`
- if found, calls `setTheme(...)` **before** the semantic graph section is restored, so
  freshly-created graph nodes are built with the correct theme immediately (no double
  rebuild)
- if the name is present but unresolvable (e.g. a theme from a build that no longer ships
  it), the pane keeps whatever theme it already has and records the unresolved name;
  `DiagramLayeredPane.getAndClearThemeWarning()` lets the caller retrieve and clear it
- `LayeredDiagramTool` checks this after every load and shows a warning dialog if non-null
  — the diagram still loads successfully with the default/current theme

### Adding a new theme

1. Implement `CanvasTheme` (anywhere — the framework imposes no restriction on location),
   including a unique `getThemeName()`
2. Register it in `CanvasThemeRegistry`'s static initializer
3. Call `DiagramLayeredPane.setTheme(newTheme)` (or select it from the toolbar dropdown) —
   this propagates the new palette to the canvas background, grid, edges, the component
   factory, and all live nodes
4. The same instance is automatically available via `getDiagramPane().getTheme()` for
   any `CanvasComponentFactory` that needs to re-query it

---

## Domain Property Editor

When a `NodeHostPanel` is selected, the framework invokes the domain factory's
`createPropertyEditorFor()` method and places the returned `JPanel` in the sidebar.
`PropertyEditorPanel` never imports domain types — it is given an opaque `JPanel` via
`showNodeEditor(JPanel)`. The callback chain:

```
User clicks NodeHostPanel
  → LayeredDiagramTool selection listener detects NodeHostPanel + factory != null
  → builds Runnable onChanged (closure over node + factory + diagramPane)
  → calls factory.createPropertyEditorFor(type, props, onChanged)
  → if panel != null: propertyEditor.showNodeEditor(panel)
  → else: propertyEditor.setSelectedComponent(comp)  // "No component selected"

User commits edit (focus-lost or Enter)
  → domain editor mutates props map (e.g. props.put("name", newValue))
  → calls onChanged.run()
      → factory.createContentFor(type, props)    // fresh content panel
      → node.swapContent(newContent)             // replaces BorderLayout.CENTER child
      → diagramPane.notifyModified()
```

**`CanvasComponentFactory.createPropertyEditorFor()` contract:**
- Default implementation returns `null` — existing factories need no changes
- The `properties` map is the node's live mutable map; the editor should mutate it
  directly before calling `onChanged`
- `onChanged` should only be called when the value actually changed (guard against
  spurious focus-lost events with an equality check before calling)
- `onChanged` always runs on the Swing EDT (fired from `FocusAdapter` or
  `ActionListener`); no threading concern

**Current implementation (`ClassDiagramFactory`):** Exposes a "Name:" text field.
Commits on focus-lost (covering both Tab and click-away) and on Enter (via
`transferFocus()` → single commit path). Guards against empty strings and no-op commits.

---

## Canvas Size

The `JLayeredPane` preferred size is hardcoded to `2000 × 1500` px, wrapped in a
`JScrollPane`. `GridPanel`, `EdgeRenderPanel`, and `CanvasOverlayPanel` are all set to
the same `2000 × 1500` bounds. There is no auto-grow API at present.

---

## Known Design Decisions

### Why `JLayeredPane` instead of a single `JPanel` with z-order management

`JLayeredPane` gives built-in z-order grouping via integer depth buckets. Shapes in
layer 200 are always behind text in layer 300 without any manual sort. The alternative
(a single `JPanel` with explicit `setComponentZOrder()`) requires re-sorting on every
add or layer change.

### Why `null` layout on `DiagramLayeredPane`

`JLayeredPane` requires `null` layout — each child manages its own bounds via
`setBounds()`. The drag and resize handlers both call `setBounds()` directly, consistent
with this contract.

### Why snap-to-grid is applied per-drag-event rather than only on `mouseReleased`

Applying snap continuously gives immediate visual feedback that the component will land
on a grid position. Applying it only on release causes a free-tracking drag followed by a
jump at the end. The staircase motion during drag is an accepted trade-off for
predictability.

### Why `NodeHostPanel` wraps a domain JPanel rather than having domain classes extend it

Domain code must not extend framework classes — that would create a tight coupling that
prevents swapping the framework or dropping in a new domain without recompilation.
`NodeHostPanel` is the framework's shell; it accepts any `JPanel` as content in its
`BorderLayout.CENTER`. Domain implementations only implement `CanvasComponentFactory`;
they never see `NodeHostPanel` directly.

### Why port locations are computed lazily rather than cached

An explicit port cache requires `invalidatePortCache()` to be called at the right time
after every bounds change. Since `EdgeRenderPanel.paintComponent()` queries port
locations on each repaint anyway, computing them directly from `getX()/getY()/getWidth()/
getHeight()` in `getPortLocation()` is simpler, always correct, and eliminates a class
of staleness bugs. `invalidatePortCache()` is a no-op on `NodeHostPanel` but remains in
the `GraphNode` interface for implementors who genuinely need caching.

### Why `EdgeRouter` receives port IDs, not just coordinates

The same start/end coordinates can require a different path shape depending on which
sides of the nodes they come from. An N→S connection (both vertical ports) needs V-H-V
routing; an E→W connection (both horizontal) needs H-V-H. The port IDs give the router
enough context to choose the right shape without needing access to node bounds.
`getApproachPoint()` on the same interface derives the arrowhead angle from the actual
final segment of the computed path rather than the diagonal from source to target.

### Why selection-handle rendering moved out of `ResizeBorder`/`ResizeHandler` and into `CanvasOverlayPanel`

`setBorder()` on a `JComponent` changes its insets, which triggers `revalidate()`.
For `DiagramShape` and `DiagramText` (no child components) the relayout was instant and
invisible. For `NodeHostPanel` (a real `JPanel` hierarchy with labels and separators)
the relayout caused a brief visual jitter. Phase 4 moved all selection-handle rendering
and resize hit-testing into `CanvasOverlayPanel`, which already paints at canvas
coordinates for port anchors — no border is ever set on any component, and the dynamic
`ResizeHandler` install/remove cycle is gone. `ResizeBorder` and `ResizeHandler` remain
as source files but are no longer referenced by the framework.

### Why resize only applies to a single selected component, never a multi-selection

`CanvasOverlayPanel.resizableComponent()` returns a component only when exactly one is
selected, so resize hit-testing and handle painting are simply absent for a
multi-selection. Resizing a group would require deciding how each component's individual
bounds scale relative to the group's combined bounding box — a meaningfully heavier
feature than uniform group translation. Phase 5 shipped group *move* only; group *resize*
is left for a future phase if it turns out to be needed. A multi-selection instead gets a
plain highlight outline per component with no handles, making the restriction visually
obvious rather than silently doing nothing on drag.

### Why `DragHandler` measures mouse delta in `DiagramLayeredPane`'s coordinate space, not the dragged component's own

An earlier version of the Phase 5 group-move code computed `dx`/`dy` as
`e.getPoint()` (component-relative) minus the press point, then added that delta to a
*fixed* start-bounds snapshot taken once at `mousePressed`. This looks reasonable but is
wrong: `e.getPoint()` is relative to the dragged component's *current* on-screen
position, which itself changes every time `setBounds()` moves it mid-gesture. Combining
a delta measured in that shifting frame with a fixed start-bounds snapshot creates a
feedback loop — each frame's computed position depends on the previous frame's snapped
output, which in turn skews the next delta measurement. Worked out algebraically, the
loop converges to roughly half the true mouse movement per event, with visible
stutter that snap-to-grid quantization made worse. (The pre-existing single-component
drag path avoided this by re-reading `comp.getBounds()` fresh every event instead of
using a fixed snapshot — a different, self-correcting trick that happened to cancel the
same shifting-frame effect, but doesn't generalize to a *group* of components with
independent start positions.)

The fix converts both the press point and the current point into
`DiagramLayeredPane`'s coordinate space via `SwingUtilities.convertPoint()`. The pane
itself never moves during a drag, so this delta is always the true total mouse
movement since press, independent of how many components have already been
repositioned or snapped. It can then be safely added to any component's fixed
start-bounds snapshot — which is what let single- and multi-component drags collapse
into one code path instead of two.

### Why the canvas is always light-colored even when a dark LAF is active

Diagram readability depends on high contrast between content and background. Dark LAFs
vary widely in their exact background tones, making it unpredictable whether node colors
and edge strokes will remain legible. A fixed white canvas is a deliberate override: the
`GridPanel` (which covers the entire canvas area) is made opaque with
`setBackground(theme.getCanvasBackground())`, painting over whatever the LAF would
otherwise show. The LAF still controls the toolbar, sidebar, property editor, and scroll
bar chrome — only the drawing surface is pinned to the theme.

### Why `createPropertyEditorFor()` is a default method on `CanvasComponentFactory` rather than a separate interface

A separate `NodePropertyEditorFactory` interface would split the plugin seam in two:
callers would need to cast to check for editor support, and implementors would need to
implement two interfaces for what is a single domain concern. A Java 8 `default` method
returning `null` gives a safe no-op for all existing implementors while keeping all
domain knowledge (content creation, port declaration, property editing) on one interface.
The `null` return is an explicit "no editor" signal — the framework falls back to
"No component selected" cleanly.

### Why `DiagramText` wraps a `JTextField` rather than extending it

`JTextField` does not forward unconsumed mouse events to its parent, which breaks drag.
By wrapping in a `JPanel` and dispatching from the field's `MouseAdapter` up to the
parent, the `DragHandler` on the `JPanel` receives the events it needs.

### Why runtime theme switching uses a direct method-call loop rather than a listener/observer pattern

Two alternatives were considered: (1) a mutable global theme singleton — mutate its
fields, call `repaint()`; (2) an observer pattern — components register a
`ThemeChangeListener` on the theme, which fires on change.

The singleton approach fails because `ClassNodeContent` applies colors to child Swing
components (via `setBackground()` / `setForeground()`) at construction time. Those child
components cache the color themselves, so mutating the theme object has no effect on
already-constructed nodes. A refresh signal is unavoidable regardless of approach.

The observer pattern works but adds registration infrastructure (`addListener()` /
`removeListener()`, a listener interface, the theme object becoming observable) for what
is ultimately a single call site: `DiagramLayeredPane.setTheme()`. Since
`DiagramLayeredPane` already owns the node registry, it can iterate the list and call
`refreshTheme()` directly — no registration needed.

A second reason to avoid the singleton: a mutable global theme prevents two
`DiagramLayeredPane` instances from having independent themes simultaneously. Keeping the
theme as an immutable reference scoped to each pane instance preserves per-window
independence at zero extra cost.
