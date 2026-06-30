# LayeredDiagramTool — Development Skill

Use this skill when working on the LayeredDiagramTool diagramming vapp.

## Key documents

- **Roadmap:** `docs/features/layereddiagramtool/ROADMAP.md` — phases, status, and per-phase implementation plans
- **Design reference:** `docs/features/layereddiagramtool/DESIGN.md` — architecture, component roles, layer system, interaction model, persistence format, and design decisions

## Source location

```
virtualdesktop-java8/src/main/java/org/jwellman/diagram/          ← tool + decorative elements
virtualdesktop-java8/src/main/java/org/jwellman/diagram/api/      ← framework interfaces
virtualdesktop-java8/src/main/java/org/jwellman/diagram/core/     ← framework implementations
virtualdesktop-java8/src/main/java/org/jwellman/diagram/domain/cls/ ← class diagram demo domain
```

The vapp entry point is:
```
virtualdesktop-java8/src/main/java/org/jwellman/virtualdesktop/vapps/SpecDiagramTool.java
```

## Key source files

### `org.jwellman.diagram` — tool and decorative elements

| File | What it is |
|------|-----------|
| `LayeredDiagramTool.java` | Top-level JPanel; toolbar, scroll pane, sidebar; `modified` flag; save/load dialogs; `setComponentFactory()`, `getDiagramPane()` |
| `DiagramLayeredPane.java` | Public JLayeredPane canvas; layer state, selection, drag wiring, graph node/edge registry, save/load |
| `DragHandler.java` | Drag-to-move mouse adapter; notifies `notifyNodeMoved()` for GraphNode components |
| `ResizeHandler.java` | 8-direction resize mouse adapter; installed/removed per selection |
| `LayerControlPanel.java` | Per-layer row UI: visibility toggle, name, item count, active highlight |
| `DiagramShape.java` | Shape component (RECTANGLE, CIRCLE, TRIANGLE) |
| `DiagramText.java` | Text component wrapping JTextField |
| `DiagramConnection.java` | Legacy placeholder connection; superseded by graph model |
| `ResizeBorder.java` | 8-handle selection border (note: causes jitter on NodeHostPanel — see roadmap) |
| `PropertyEditorPanel.java` | Right-panel property editor (font + color) |
| `ColorPropertyPanel.java` | Color selection sub-panel with swatches |
| `ColorSwatch.java` | 24×24 color swatch with hover |
| `DiagramColorable.java` | Interface: fill + border color |
| `DiagramTextAware.java` | Interface: font name/size/style + text color |
| `ShapeType.java` | Enum: RECTANGLE, CIRCLE, TRIANGLE |
| `DiagramData.java` | Root JSON persistence object (layers + optional semanticGraph) |
| `LayerData.java` | Per-layer persistence object |
| `ComponentData.java` | Abstract base with @JsonSubTypes dispatch |
| `ShapeData.java` | Shape persistence |
| `TextData.java` | Text persistence |
| `SemanticGraphData.java` | Container for GraphNodeData and GraphEdgeData lists |
| `GraphNodeData.java` | Graph node persistence: id, type, properties, x/y/w/h, layer |
| `GraphEdgeData.java` | Graph edge persistence: id, source/target node+port, lineStyle, arrowType |

### `org.jwellman.diagram.api` — framework interfaces

| File | What it is |
|------|-----------|
| `GraphNode.java` | Node identity, type, properties, port IDs, port locations, visual component |
| `GraphEdge.java` | Directed edge: source/target node+port IDs, EdgeAttributes |
| `EdgeAttributes.java` | Value class: LineStyle, ArrowType, color, strokeWidth |
| `EdgeRouter.java` | Strategy: `calculatePath(start, startPortId, end, endPortId)` + `getApproachPoint(...)` |
| `CanvasComponentFactory.java` | Domain plugin: `createContentFor(nodeType, props)` + `getPortIds(nodeType)` |

### `org.jwellman.diagram.core` — framework implementations

| File | What it is |
|------|-----------|
| `NodeHostPanel.java` | Public JPanel + GraphNode; wraps domain JPanel; lazy port-location computation |
| `EdgeRenderPanel.java` | Transparent JPanel at CONNECTION_LAYER; paints edges; always passes mouse events through |
| `CanvasOverlayPanel.java` | Transparent JPanel at OVERLAY_LAYER; IDLE/EDGE_CREATION/EDGE_DRAGGING state machine; port anchors + rubber-band line |
| `OrthogonalRouter.java` | Port-direction-aware router: V-H-V (N/S), H-V-H (E/W), single-bend L (mixed) |
| `StraightLineRouter.java` | Direct straight line |
| `DefaultGraphEdge.java` | Immutable GraphEdge used for interactive and persisted edges |

### `org.jwellman.diagram.domain.cls` — class diagram demo domain

| File | What it is |
|------|-----------|
| `ClassNodeContent.java` | Pure-Swing JPanel; header + fields + methods; no framework dependency |
| `ClassDiagramFactory.java` | CanvasComponentFactory for CLASS and INTERFACE node types |
| `ClassDiagramDemo.java` | Static `buildDemo(DiagramLayeredPane, CanvasComponentFactory)` — 4 nodes, 3 edges |

## Architecture in one paragraph

`LayeredDiagramTool` is a `JPanel` that assembles a `JToolBar` (north), a `JScrollPane`
wrapping a `DiagramLayeredPane` (center), and a 280px sidebar with layer controls and a
property editor (east). `DiagramLayeredPane` is a public `JLayeredPane` with 6
depth-bucketed layers (0/100/200/300/400/500). Decorative components (`DiagramShape`,
`DiagramText`) and graph nodes (`NodeHostPanel`) are added to user layers; two permanent
transparent panels sit at CONNECTION_LAYER (`EdgeRenderPanel`) and OVERLAY_LAYER
(`CanvasOverlayPanel`). Selecting a component installs a `ResizeBorder` + `ResizeHandler`;
deselecting removes them. The "Connect" toggle button puts `CanvasOverlayPanel` into
`EDGE_CREATION` mode, which renders port anchors and lets the user drag between ports to
create `DefaultGraphEdge` instances rendered by `EdgeRenderPanel`. Everything persists to
JSON via Jackson in a two-section format: `layers` (decorative) and `semanticGraph` (graph).
Domain-specific node content is provided by a `CanvasComponentFactory` plugin; the
framework never extends or depends on domain classes.

## Layer constants (on DiagramLayeredPane)

```
GRID_LAYER       =   0   (GridPanel only — not user-accessible)
BACKGROUND_LAYER = 100
SHAPE_LAYER      = 200   (default for new shapes and graph nodes)
TEXT_LAYER       = 300   (default for new text)
CONNECTION_LAYER = 400   (EdgeRenderPanel always present; decorative DiagramConnections)
OVERLAY_LAYER    = 500   (CanvasOverlayPanel always present; port anchors + rubber band)
```

New components are added at position `0` within their layer (top of layer = in front).

## Coding standards

Follow the project-wide Java formatting rules in CLAUDE.md:
- One statement per line
- Always use braces on if/for/while blocks
- No inline method bodies (`{ return x; }` style)
- `@Override` on its own line
- Java 8 only — no streams, no `var`, no APIs not available in Java 8
- `java.util.function.Consumer` / `java.util.function.BiConsumer` are fine (Java 8)

## What NOT to do

- Do not run `mvn compile` or any build commands — the user compiles and tests
- Do not add undo/redo or multi-select unless those phases are explicitly requested
- Do not embed browser components or suggest JavaFX — see CLAUDE.md design philosophy
- Do not set `setBorder()` on `NodeHostPanel` for selection feedback — this causes layout
  jitter; the planned fix (overlay-painted selection handles) is on the roadmap
- Do not add imports from `org.jwellman.diagram.core` or `.api` into domain classes
  (`domain/cls`) except `CanvasComponentFactory`, `EdgeAttributes`, `DefaultGraphEdge`,
  and `NodeHostPanel` — domain content panels must remain framework-agnostic

## Starting a task

1. Read `docs/features/layereddiagramtool/ROADMAP.md` to confirm which phase is being worked on and its status
2. Read `docs/features/layereddiagramtool/DESIGN.md` for the relevant architectural context
3. Read the specific source files the task will touch before editing them
4. Make the change; report what was done and what the user should verify interactively
