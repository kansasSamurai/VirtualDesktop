# LayeredDiagramTool — Design Reference

Architecture, component structure, and design decisions for the LayeredDiagramTool.
Open this when you need to understand *why it is built the way it is* or when extending
it with new capabilities.

---

## Introduction

LayeredDiagramTool is a structural diagramming vapp — flowcharts, architecture diagrams,
entity-relationship diagrams — built on `JLayeredPane`. It is not a data-visualization
tool; that role belongs to the chart vapps (`SpecJFreeChart`, `SpecXChartDemo`).

The tool currently lives in `org.jwellman.demo.layereddiagramtool` and runs as a
standalone `JPanel` with its own `main()` method. It has not yet been wired into the
VirtualDesktop menu system; that is Phase 1 of the roadmap.

---

## Component Hierarchy

```
LayeredDiagramTool (JPanel)
├── JToolBar (NORTH)
│     add rectangle / triangle / circle / text
│     show grid / snap to grid toggles
│     bring forward / send back
│     delete selected
│     save / load
├── JScrollPane > DiagramLayeredPane (CENTER)
│     GridPanel            @ GRID_LAYER (0)       — 20px grid lines
│     DiagramShape*        @ BACKGROUND/SHAPE/... — rectangle, circle, triangle
│     DiagramText*         @ TEXT_LAYER (300)      — editable text
│     DiagramConnection*   @ CONNECTION_LAYER (400) — placeholder, not yet functional
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

| Class | File | Role |
|-------|------|------|
| `LayeredDiagramTool` | `LayeredDiagramTool.java` | Top-level `JPanel`; builds toolbar, pane, and sidebar; owns the `modified` flag and save/load dialogs |
| `DiagramLayeredPane` | (inner class) | `JLayeredPane` subclass; owns all diagram components, layer state, selection model, drag wiring, popup menu, save/load serialization |
| `GridPanel` | (inner class) | Static inner `JPanel`; paints 20px light-gray grid lines; sits at GRID_LAYER, `setOpaque(false)` |
| `DragHandler` | (inner class) | `MouseAdapter`; handles drag-to-move with snap-to-grid applied on each `mouseDragged` event |
| `ResizeHandler` | (inner class) | `MouseAdapter`; installed/removed per selection cycle; detects 8px edge zones for 8-direction resize; applies snap-to-grid on release |
| `DiagramShape` | `DiagramShape.java` | `JComponent`; paints rectangle, circle, or triangle via `Graphics2D`; implements `DiagramColorable` |
| `DiagramText` | `DiagramText.java` | `JPanel` wrapping a `JTextField`; implements `DiagramColorable` + `DiagramTextAware`; dispatches mouse events to parent for drag |
| `DiagramConnection` | (inner class) | `JComponent` placeholder; draws a straight line between hardcoded endpoints; not wired to any real shapes |
| `LayerControlPanel` | `LayerControlPanel.java` | Row UI for one layer: visibility checkbox, name label, item count, active-layer highlight; uses a `Timer` to poll item counts |
| `PropertyEditorPanel` | `PropertyEditorPanel.java` | Right-panel editor for the selected component's font and color properties; listens to `selectionListener` from `DiagramLayeredPane` |
| `ColorPropertyPanel` | `ColorPropertyPanel.java` | Sub-panel of `PropertyEditorPanel`; manages fill/border/text color target selection, swatch grid, and `JColorChooser` integration |
| `ColorSwatch` | `ColorSwatch.java` | 24×24 px colored square with hover effect; used in `ColorPropertyPanel` |
| `ResizeBorder` | `ResizeBorder.java` | Custom `Border`; paints 8 square handle indicators at corners and edge midpoints when a component is selected |

### Interfaces

| Interface | Methods | Purpose |
|-----------|---------|---------|
| `DiagramColorable` | `getFillColor / setFillColor / getBorderColor / setBorderColor` | Common color contract for shapes and text |
| `DiagramTextAware` | `getFontName/set, getFontSize/set, getFontStyle/set, getTextColor/setTextColor` | Font and text color contract; implemented by `DiagramText` |

### Persistence classes (Jackson)

| Class | Role |
|-------|------|
| `DiagramData` | Root object: `gridSize`, `snapToGrid`, `activeLayer`, `List<LayerData>` |
| `LayerData` | One layer: `layerDepth`, `visible`, `List<ComponentData>` |
| `ComponentData` | Abstract base with `@JsonTypeInfo` / `@JsonSubTypes` for polymorphic dispatch |
| `ShapeData` | Extends `ComponentData`; captures bounds, `ShapeType`, `fillColor`, `borderColor` |
| `TextData` | Extends `ComponentData`; captures bounds, text, font name/size/style, all three colors |

---

## Layer System

Six named layers with fixed integer depth constants on `DiagramLayeredPane`:

| Constant | Depth | Default use |
|----------|-------|-------------|
| `GRID_LAYER` | 0 | `GridPanel` — always at the bottom, never user-accessible |
| `BACKGROUND_LAYER` | 100 | Decorative shapes (watermarks, region fills) |
| `SHAPE_LAYER` | 200 | Default layer for new shapes |
| `TEXT_LAYER` | 300 | Default layer for text components |
| `CONNECTION_LAYER` | 400 | Reserved for `DiagramConnection` (placeholder) |
| `SELECTION_LAYER` | 500 | Currently unused as a component layer; the name is reserved for future selection-overlay use |

**Active layer** — `DiagramLayeredPane.activeLayer` (default: `SHAPE_LAYER`). New
components are added to the active layer. `LayerControlPanel` highlights the row for
the active layer.

**Per-component layer override** — right-click popup provides a "Move to Layer" submenu.
Components can also be nudged one depth unit at a time via "Bring Forward" / "Send Back"
(toolbar or popup), which increments/decrements the raw depth integer rather than jumping
between named layers.

**Layer visibility** — `Map<Integer, Boolean> layerVisibility` on `DiagramLayeredPane`.
Toggling visibility calls `setVisible()` on every component in that layer. The grid panel
is excluded from this logic.

---

## Interaction Model

### Selection

Single-click on a component calls `selectComponent()`:
- Removes `ResizeBorder` and `ResizeHandler` from the previously selected component
- Sets a new `ResizeBorder` on the clicked component (paints 8 handles)
- Installs a fresh `ResizeHandler` on the clicked component
- Fires `selectionListener` → `PropertyEditorPanel` updates to reflect the new selection

Click on the blank diagram surface or the `GridPanel` calls `deselectAll()`, which
reverses the above.

Only one component can be selected at a time (single-selection model).

### Drag

`DragHandler` is installed on every component at `addDiagramComponent()` time and
lives for the component's lifetime. On `mouseDragged`:

1. Compute `dx/dy` from the press point
2. Add the delta to the component's current bounds
3. If snap-to-grid is on, round the new `x` and `y` independently to the nearest 20px multiple
4. `setBounds()` with the new position; call `revalidate()` + `repaint()`

Snap is applied continuously during drag (each event snaps), not only on release. This
produces a staircase motion on screen but prevents position drift over a long drag.

### Resize

`ResizeHandler` is installed only while a component is selected and removed on deselect.
On `mouseMoved`, it hit-tests an 8px zone at each of the 8 handle positions and changes
the cursor accordingly (`NW_RESIZE_CURSOR`, `N_RESIZE_CURSOR`, etc.).

On `mouseDragged` while a resize is active:
- Computes new bounds from the drag delta, respecting which handle direction is being dragged
- Enforces a 20×20 minimum size
- If snap-to-grid is on, snaps the moving edge(s) to the grid

### Right-click popup

Available on any diagram component:
- **Move to Layer** — submenu listing all 5 user layers; current layer shown with a ✓ and disabled
- **Bring Forward / Send Back** — nudges the raw layer depth by ±1
- **Change Fill Color… / Change Border Color…** — shown for `DiagramColorable` components; opens `JColorChooser`
- **Change Text Color…** — shown for `DiagramTextAware` components; opens `JColorChooser`
- **Delete**

### Unsaved-changes guard

`LayeredDiagramTool` tracks a `boolean modified` flag. Every mutating operation on
`DiagramLayeredPane` fires a `modificationListener` callback which sets `modified = true`.
On load, the tool calls `checkUnsavedChanges()` first — this offers Yes/No/Cancel when
`modified` is true.

---

## Persistence Format

JSON via Jackson. Default filename: `diagram.json`.

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
          "fillColor": "#4A90D9",
          "borderColor": "#2C5F8A"
        }
      ]
    }
  ]
}
```

`ComponentData` uses `@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")`
with `@JsonSubTypes` entries for `"shape"` → `ShapeData` and `"text"` → `TextData`.
`DiagramConnection` is not persisted (it is a placeholder with no real data).

**Known gap:** the format has no `version` field. Renaming any persisted field will
silently drop that data on load. A `version` field should be added to `DiagramData`
before new component types are introduced (see Phase 3 in the roadmap).

---

## DiagramLayeredPane Canvas Size

The `JLayeredPane` preferred size is hardcoded to `2000 × 1500` px. It is wrapped in a
`JScrollPane` in `LayeredDiagramTool`, so the scrollable area is always the full 2000×1500
regardless of the window size. The `GridPanel` is also set to `2000 × 1500` and covers
the full canvas.

There is no auto-grow or canvas resize API at present.

---

## Known Design Decisions

### Why JLayeredPane instead of a single JPanel with z-order management

`JLayeredPane` gives built-in z-order grouping via integer depth buckets. Shapes in layer 200
are always behind text in layer 300 without any manual sort — adding a new component at the
right layer integer just works. The alternative (a single `JPanel` with explicit `setComponentZOrder()`)
requires manual sorting every time any component is added or moved, and the sort must be
re-run whenever the layer assignment changes.

### Why `null` layout on DiagramLayeredPane

`JLayeredPane` requires `null` layout — its contract is that each child component manages
its own bounds via `setBounds()`. This is the standard pattern for all `JLayeredPane` uses
in Swing. The diagram tool's drag and resize handlers both call `setBounds()` directly,
consistent with this contract.

### Why snap-to-grid is applied per-drag-event rather than only on mouseReleased

Applying snap continuously gives the user immediate visual feedback during the drag that
the component will land on a grid position. Applying it only on release means the component
tracks the cursor freely during the drag and then jumps at the end — which is less predictable.
The staircase motion is an acceptable trade-off for predictability. (The earlier AI-generated
code tried a "snap start position once, offset freely" approach — the commented-out block
in `DragHandler.mouseDragged()` — but this caused jitter near grid boundaries and was replaced.)

### Why ResizeHandler is installed and removed per selection cycle

Installing `ResizeHandler` permanently on every component (as `DragHandler` is) would mean
every component always responds to resize cursor changes and resize drags, which makes
interacting with unselected components confusing. Tying `ResizeHandler` to the selection
state means only the selected component grows resize handles, matching the standard
desktop selection model.

### Why DiagramText wraps a JTextField rather than extending it

`JTextField` does not forward unconsumed mouse events to its parent, which breaks drag.
By wrapping it in a `JPanel` and dispatching mouse events from the `JTextField`'s
`MouseAdapter` up to the parent, the drag handler on the `JPanel` receives the events
it needs. Extending `JTextField` directly would require overriding event handling in
ways that conflict with the field's own focus and editing behavior.
