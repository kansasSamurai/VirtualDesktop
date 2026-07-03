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
and edges with port-based routing). A class-diagram demo domain is included.

---

## Component Hierarchy

```plain
LayeredDiagramTool (JPanel)
├── JToolBar (NORTH)
│     Add Rectangle / Triangle / Circle / Text
│     Show Grid / Snap to Grid toggles
│     Bring Forward / Send Back
│     Delete Selected
│     Save / Load
│     Connect (JToggleButton) — enters edge-creation mode on the canvas overlay
├── JScrollPane > DiagramLayeredPane (CENTER)
│     GridPanel            @ GRID_LAYER (0)          — 20px grid lines, setOpaque(false)
│     DiagramShape*        @ any user layer           — rectangle, circle, triangle
│     DiagramText*         @ any user layer           — editable text
│     NodeHostPanel*       @ any user layer           — graph node wrapping a domain JPanel
│     EdgeRenderPanel      @ CONNECTION_LAYER (400)   — transparent; paints all graph edges
│     CanvasOverlayPanel   @ OVERLAY_LAYER (500)      — transparent; port anchors + rubber band
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
| `LayeredDiagramTool` | Top-level `JPanel`; builds toolbar, pane, and sidebar; owns `modified` flag and save/load dialogs; exposes `setComponentFactory()` and `getDiagramPane()` |
| `DiagramLayeredPane` | `public JLayeredPane` subclass; owns all diagram components, layer state, selection model, drag wiring, popup menu, graph node/edge registry, and save/load serialization |
| `DragHandler` | `MouseAdapter`; handles drag-to-move with snap-to-grid; notifies `DiagramLayeredPane.notifyNodeMoved()` when a `GraphNode` is dragged so edges redraw |
| `ResizeHandler` | `MouseAdapter`; installed/removed per selection cycle; 8-direction resize; snap-to-grid on each drag event |
| `LayerControlPanel` | Row UI for one layer: visibility toggle, name label, item count, active-layer highlight; uses a `Timer` to poll item counts |
| `DiagramShape` | `JComponent`; paints rectangle, circle, or triangle via `Graphics2D`; implements `DiagramColorable` |
| `DiagramText` | `JPanel` wrapping a `JTextField`; implements `DiagramColorable` + `DiagramTextAware`; dispatches mouse events to parent for drag |
| `DiagramConnection` | `JComponent` placeholder; draws a straight line with an arrowhead between hardcoded endpoints; superseded by the graph model layer |
| `ResizeBorder` | Custom `Border`; paints 8 square handle indicators at corners and edge midpoints when a component is selected |
| `PropertyEditorPanel` | Right-panel editor for the selected component's font and color properties |
| `ColorPropertyPanel` | Sub-panel of `PropertyEditorPanel`; manages fill/border/text color target, swatch grid, and `JColorChooser` integration |
| `ColorSwatch` | 24×24 px colored square with hover effect |

**Persistence (Jackson):**

| Class | Role |
| --- | --- |
| `DiagramData` | Root object: `gridSize`, `snapToGrid`, `activeLayer`, `List<LayerData>`, optional `SemanticGraphData` |
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
| `CanvasComponentFactory` | Domain plugin: `createContentFor(nodeType, properties)` → `JPanel`; `getPortIds(nodeType)` |
| `CanvasTheme` | Color palette for the canvas surface and all nodes; consumed by `DiagramLayeredPane` (canvas bg + grid) and `CanvasComponentFactory` implementations (node colors) |

---

### `org.jwellman.diagram.core` — framework implementations

| Class | Role |
| --- | --- |
| `NodeHostPanel` | `public JPanel` implementing `GraphNode`; wraps domain-provided content in `BorderLayout.CENTER`; computes port locations lazily from current bounds (no cache) |
| `EdgeRenderPanel` | Transparent `JPanel` at `CONNECTION_LAYER`; paints all edges using the configured `EdgeRouter`; always returns `false` from `contains()` to pass mouse events through |
| `CanvasOverlayPanel` | Transparent `JPanel` at `OVERLAY_LAYER`; state machine (`IDLE / EDGE_CREATION / EDGE_DRAGGING`); renders port anchor circles and rubber-band line; transparent to mouse events in `IDLE` state |
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
| `ClassDiagramFactory` | Implements `CanvasComponentFactory`; takes a `CanvasTheme` at construction and passes it to each `ClassNodeContent` |
| `ClassDiagramDemo` | Static `buildDemo(DiagramLayeredPane, CanvasComponentFactory)` method; populates 4 nodes and 3 edges illustrating class relationships |

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

Single-click on a component calls `selectComponent()`:

- Removes `ResizeBorder` and `ResizeHandler` from the previously selected component
- Sets a new `ResizeBorder` on the clicked component (paints 8 handles)
- Installs a fresh `ResizeHandler` on the clicked component
- Fires `selectionListener` → `PropertyEditorPanel` updates

**Known gap / roadmap item:** `ResizeBorder` calls `setBorder()` on the component,
triggering `revalidate()`. For `DiagramShape` / `DiagramText` (no children) this is
invisible. For `NodeHostPanel` (real JPanel hierarchy) this causes a brief layout jitter.
The planned fix is to move all selection-handle rendering into `CanvasOverlayPanel` so no
border is ever set on any component. See roadmap.

### Drag

`DragHandler` is installed on every component at add time and lives for the component's
lifetime. On `mouseDragged`:

1. Compute `dx/dy` from the press point
2. Add delta to the component's current bounds
3. If snap-to-grid is on, round `x` and `y` to the nearest 20px multiple
4. `setBounds()`, then `revalidate()` + `repaint()`
5. If the component implements `GraphNode`, call `layeredPane.notifyNodeMoved(nodeId)`
   so `EdgeRenderPanel` repaints affected edges

### Resize

`ResizeHandler` is installed only while a component is selected and removed on deselect.
Hit-tests an 8px zone at each of the 8 handle positions; changes cursor accordingly.
On drag: computes new bounds from the active handle direction, enforces 20px minimum size,
applies snap-to-grid.

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

JSON via Jackson. Default filename: `diagram.json`. Two top-level sections:
`layers` (decorative canvas elements) and `semanticGraph` (graph nodes and edges).
`semanticGraph` is omitted entirely when no graph nodes exist.

```json
{
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

**Known gap:** the format has no `version` field. Renaming any persisted field will
silently drop data on load. A `version` field should be added to `DiagramData` before
the format is considered stable.

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

### `BlueprintCanvasTheme` (`org.jwellman.diagram.core`) — **current default**

Prussian blue canvas with white borders, white text, and a subtle brighter-blue grid.
Evokes classic 19th-century cyanotype blueprints — signals "structural draft in progress"
to stakeholders rather than a finished commitment. The stereotype label uses ice blue
(`#E6F2FF`) to distinguish it from the primary node name without departing from the
monochrome palette.

### `LightCanvasTheme` (`org.jwellman.diagram.core`)

White canvas, light grey grid, blue class headers, green interface headers.
Swap this in as the default to restore the original light-mode appearance.

### How themes flow through the system

```
DiagramLayeredPane (owns the theme, defaults to LightCanvasTheme)
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

**Domain class import rule:** `ClassNodeContent` is permitted to import `CanvasTheme`
from `org.jwellman.diagram.api`. It is a pure color-palette interface with no Swing
dependency, making it safe for domain use. The existing allowlist now includes
`CanvasTheme` alongside `CanvasComponentFactory`, `EdgeAttributes`, `DefaultGraphEdge`,
and `NodeHostPanel`.

### Adding a new theme

1. Implement `CanvasTheme` (anywhere — the framework imposes no restriction on location)
2. Pass the instance to `DiagramLayeredPane` — a `setTheme()` method can be added when
   needed; for now, the default is `LightCanvasTheme`
3. Pass the same instance to any `CanvasComponentFactory` constructor

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

### Why `ResizeBorder` causes jitter on `NodeHostPanel` (and the planned fix)

`setBorder()` on a `JComponent` changes its insets, which triggers `revalidate()`.
For `DiagramShape` and `DiagramText` (no child components) the relayout is instant and
invisible. For `NodeHostPanel` (a real `JPanel` hierarchy with labels and separators)
the relayout causes a brief visual jitter. The planned fix is to move all
selection-handle rendering into `CanvasOverlayPanel`, which already paints at canvas
coordinates for port anchors. No border would ever be set on any component; the overlay
would paint the 8 handle squares over the selected component's bounds. This also
eliminates the dynamic `ResizeHandler` install/remove cycle, since the overlay can own
the resize hit-testing directly. See roadmap Phase 4.

### Why `ResizeHandler` is installed and removed per selection cycle

Installing `ResizeHandler` permanently on every component would make every component
always respond to resize cursor changes, which is confusing when hovering over unselected
components. Tying it to selection means only the selected component shows resize
behavior, matching the standard desktop selection model.

### Why the canvas is always light-colored even when a dark LAF is active

Diagram readability depends on high contrast between content and background. Dark LAFs
vary widely in their exact background tones, making it unpredictable whether node colors
and edge strokes will remain legible. A fixed white canvas is a deliberate override: the
`GridPanel` (which covers the entire canvas area) is made opaque with
`setBackground(theme.getCanvasBackground())`, painting over whatever the LAF would
otherwise show. The LAF still controls the toolbar, sidebar, property editor, and scroll
bar chrome — only the drawing surface is pinned to the theme.

### Why `DiagramText` wraps a `JTextField` rather than extending it

`JTextField` does not forward unconsumed mouse events to its parent, which breaks drag.
By wrapping in a `JPanel` and dispatching from the field's `MouseAdapter` up to the
parent, the `DragHandler` on the `JPanel` receives the events it needs.
