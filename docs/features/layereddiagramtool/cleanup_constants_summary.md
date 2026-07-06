# Constant Extraction Cleanup — org.jwellman.diagram

Covers all classes in and below `org.jwellman.diagram`. Every `new Font(…)`, `new Color(…)`,
`BorderFactory.create*(…)`, `new Dimension(…)`, and `new Insets(…)` that could be a
`private static final` was extracted. Items that depend on instance state or runtime
values are called out at the bottom.

---

## DiagramShape

| Constant | Value | Replaces |
|----------|-------|---------|
| `DEFAULT_FILL_COLOR` | `new Color(173, 216, 230, 200)` | constructor |
| `DEFAULT_BORDER_COLOR` | `new Color(70, 130, 180)` | constructor |

---

## DiagramText

Two constants already existed (`DEFAULT_FONT`, `DEFAULT_BORDER`) but were annotated
`@SuppressWarnings("unused")` and never referenced. The annotations were removed and
three Color constants were added alongside them.

| Constant | Value | Replaces |
|----------|-------|---------|
| `DEFAULT_FILL_COLOR` | `new Color(255, 255, 255, 200)` | constructor |
| `DEFAULT_BORDER_COLOR` | `new Color(70, 130, 180)` | constructor |
| `SELECTION_TINT` | `new Color(100, 150, 255, 30)` | `paintComponent()` |

Note: `DEFAULT_FONT` and `DEFAULT_BORDER` remain declared but still unused — the font
is implicitly encoded in the three separate `fontName`/`fontSize`/`fontStyle` fields that
`DiagramTextAware` requires. These could be removed in a future cleanup pass.

---

## LayerControlPanel

Added `import javax.swing.border.Border`.

| Constant | Value | Replaces |
|----------|-------|---------|
| `LAYER_NAME_FONT` | `new Font("Arial", Font.BOLD, 12)` | `nameLabel.setFont(…)` |
| `LAYER_LABEL_FONT` | `new Font("Arial", Font.PLAIN, 10)` | `countLabel.setFont(…)` and `depthLabel.setFont(…)` (both used same font) |
| `MAX_HEIGHT_SIZE` | `new Dimension(Integer.MAX_VALUE, 40)` | `setMaximumSize(…)` |
| `PANEL_BORDER` | `BorderFactory.createEmptyBorder(5, 5, 5, 5)` | `setBorder(…)` |

---

## DiagramLayeredPane

| Constant | Value | Replaces |
|----------|-------|---------|
| `CANVAS_SIZE` | `new Dimension(2000, 1500)` | `setPreferredSize(…)` in constructor |

Note: The three `setBounds(0, 0, 2000, 1500)` calls on `gridPanel`, `shadowPanel`, and
`edgePanel` use plain `int` literals, not `Dimension` objects, so they are out of scope.

---

## LightCanvasTheme

Previously returned `new Color(…)` from every method on each call. Extracted all seven
colors to `private static final` constants.

| Constant | Value | Replaces |
|----------|-------|---------|
| `GRID_LINE` | `new Color(220, 220, 220)` | `getGridLineColor()` |
| `INTERFACE_HEADER` | `new Color(200, 240, 200)` | `getNodeHeaderBackground("INTERFACE")` |
| `CLASS_HEADER` | `new Color(180, 210, 255)` | `getNodeHeaderBackground(other)` |
| `NODE_BODY` | `new Color(248, 248, 248)` | `getNodeBodyBackground()` |
| `NODE_BORDER` | `new Color(120, 140, 170)` | `getNodeBorderColor()` |
| `STEREOTYPE_TEXT` | `new Color(60, 100, 60)` | `getStereotypeTextColor()` |
| `EDGE` | `new Color(60, 60, 60)` | `getEdgeColor()` |

`BlueprintCanvasTheme` and `WhiteprintCanvasTheme` already used `private static final`
constants — no changes needed there.

---

## ClassNodeContent

Added `import javax.swing.border.Border`.

The `int nameStyle` local variable and its `new Font("SansSerif", nameStyle, 12)` call
were replaced with two distinct constants and a ternary, eliminating the variable entirely.

| Constant | Value | Replaces |
|----------|-------|---------|
| `STEREOTYPE_FONT` | `new Font("SansSerif", Font.PLAIN, 9)` | stereotype label |
| `NAME_FONT` | `new Font("SansSerif", Font.BOLD, 12)` | name label (non-abstract) |
| `ABSTRACT_NAME_FONT` | `new Font("SansSerif", Font.BOLD \| Font.ITALIC, 12)` | name label (abstract) |
| `VISIBILITY_FONT` | `new Font("Monospaced", Font.BOLD, 11)` | visibility toggle buttons |
| `PLACEHOLDER_FONT` | `new Font("SansSerif", Font.ITALIC, 11)` | "(no fields)" / "(no methods)" labels |
| `ENTRY_FONT` | `new Font("Monospaced", Font.PLAIN, 11)` | field/method entry labels |
| `VISIBILITY_INSETS` | `new Insets(1, 3, 1, 3)` | visibility button margins |
| `ROW_INSETS` | `new Insets(1, 1, 1, 1)` | `layoutRowComponents` GBC insets |
| `HEADER_BORDER` | `BorderFactory.createEmptyBorder(4, 6, 4, 4)` | header panel border |
| `EDIT_CARD_BORDER` | `BorderFactory.createEmptyBorder(2, 4, 2, 4)` | edit card panel border |
| `SECTION_LABEL_BORDER` | `BorderFactory.createEmptyBorder(2, 0, 1, 0)` | section label border |
| `SECTION_PADDING` | `BorderFactory.createEmptyBorder(3, 6, 3, 6)` | `makeSection()` border; also reused as inner border in `makeSectionWithDivider()` compound border |

---

## ClassDiagramFactory

Added `import javax.swing.border.Border`.

| Constant | Value | Replaces |
|----------|-------|---------|
| `VISIBILITY_FONT` | `new Font("Monospaced", Font.BOLD, 11)` | visibility toggle buttons in `detailsVisPanel()` |
| `VISIBILITY_INSETS` | `new Insets(1, 3, 1, 3)` | visibility button margins |
| `ROW_INSETS` | `new Insets(1, 1, 1, 1)` | `detailsLayoutRow()` GBC insets |
| `EDITOR_PADDING` | `BorderFactory.createEmptyBorder(8, 8, 8, 8)` | `createPropertyEditorFor()` panel border |
| `PALETTE_PADDING` | `BorderFactory.createEmptyBorder(8, 8, 8, 8)` | `createNodePalettePanel()` border |
| `EMPTY_BORDER` | `BorderFactory.createEmptyBorder()` | scroll pane border in `createDetailsPanelFor()` |
| `BTN_ROW_BORDER` | `BorderFactory.createEmptyBorder(4, 0, 0, 0)` | apply-button row border |
| `DETAILS_BORDER` | `BorderFactory.createEmptyBorder(4, 6, 4, 6)` | outer panel border in `createDetailsPanelFor()` |

Note: `EDITOR_PADDING` and `PALETTE_PADDING` happen to have the same pixel values
(`8, 8, 8, 8`) but are kept as separate constants because they belong to distinct UI
roles. They could be unified into one if that seems cleaner.

---

## LayeredDiagramTool

The local variable `COLLAPSED_BOTTOM_H` inside `createTabContent()` was promoted to the
static field `COLLAPSED_BOTTOM_HEIGHT`, which allowed `COLLAPSED_PANEL_SIZE` to reference
it directly.

| Constant | Value / note | Replaces |
|----------|-------------|---------|
| `COLLAPSED_BOTTOM_HEIGHT` | `30` (int) | local `final int` inside `createTabContent()` |
| `ACCENT_COLOR` | `new Color(60, 100, 150)` | `leftToggleBtn`, `buildAddTabButton`, `detailsCollapseBtn` foreground (×3) |
| `HANDLE_PANEL_SIZE` | `new Dimension(LEFT_HANDLE_WIDTH, 0)` | `handle.setPreferredSize(…)` and `leftPanel.setMinimumSize(…)` (×2) |
| `RIGHT_PANEL_SIZE` | `new Dimension(280, 0)` | right panel preferred size |
| `DETAILS_PANEL_SIZE` | `new Dimension(0, 160)` | details panel preferred size |
| `COLLAPSED_PANEL_SIZE` | `new Dimension(0, COLLAPSED_BOTTOM_HEIGHT)` | details panel minimum size |
| `FILE_BROWSER_SIZE` | `new Dimension(640, 300)` | file browser scroll pane preferred size |
| `FILE_TILE_SIZE` | `new Dimension(1, 64)` | each file tile preferred size |
| `LEFT_CONTENT_BORDER` | `BorderFactory.createEmptyBorder(4, 6, 4, 0)` | left content panel border |
| `PROJECT_TITLE_BORDER` | `BorderFactory.createEmptyBorder(2, 2, 8, 2)` | "Project" title label border |
| `NEW_DIAGRAM_PANEL_BORDER` | `BorderFactory.createEmptyBorder(0, 2, 4, 2)` | new-diagram button panel border |
| `NEW_DIAGRAM_LABEL_BORDER` | `BorderFactory.createEmptyBorder(0, 0, 4, 0)` | "New Diagram" label border |
| `EDGE_EDITOR_BORDER` | `BorderFactory.createEmptyBorder(8, 8, 8, 8)` | edge property editor outer border |
| `RELATIONSHIPS_BORDER` | `BorderFactory.createEmptyBorder()` | relationships scroll pane border |
| `TILES_PANEL_BORDER` | `BorderFactory.createEmptyBorder(4, 4, 4, 4)` | file tiles panel border |
| `HEADER_ROW_BORDER` | `BorderFactory.createEmptyBorder(0, 0, 6, 0)` | directory header row border |
| `DIALOG_CONTENT_BORDER` | `BorderFactory.createEmptyBorder(12, 12, 12, 12)` | file browser dialog content border |
| `FILE_PLACEHOLDER_BORDER` | `BorderFactory.createEmptyBorder(16, 16, 16, 16)` | "no files found" / "dir not found" labels (×2) |
| `FILE_TILE_INNER_BORDER` | `BorderFactory.createEmptyBorder(6, 8, 6, 8)` | inner border of compound file tile border |

---

## Items flagged for review

These instantiations were **not** extracted because they are not statically knowable:

| File | Line | Expression | Reason |
|------|------|------------|--------|
| `LayeredDiagramTool` | `addDiagramTypeButton()` | `new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height)` | Height depends on `btn.getPreferredSize()` at call time — inherently dynamic |
| `LayeredDiagramTool` | `LayeredDiagramTool()` constructor | `new Dimension(savedLeftWidth, 0)` passed to `setPreferredSize` | `savedLeftWidth` is an instance field whose value changes at runtime (panel collapse/expand) |
| `ClassNodeContent` | `makePlaceholderLabel()` / `makeEntryLabel()` | `new Dimension(0, label.getPreferredSize().height)` (×2) | Height depends on the just-created label's preferred size — computed at layout time |
| `LayerControlPanel` | `hoverBackground()` | `new Color(r, g, b)` | Computed by blending two `UIManager` colors — cannot be known at class-load time |
| `DiagramText` | `DEFAULT_FONT` and `DEFAULT_BORDER` | Already-declared constants never referenced in live code | These were declared with suppressed warnings; now warnings are removed but they remain unused. Consider deleting them or wiring them into the constructor logic |

---

## Files with no changes (already clean or out of scope)

`DiagramConnection`, `DragHandler`, `ResizeHandler`, `ResizeBorder` (already has `INSETS`
constant), `PropertyEditorPanel`, `ColorPropertyPanel` (palette colors inline but all unique
and used once), `ColorSwatch` (Dimension depends on `swatchSize` field),
`ShadowLayerPanel` (Color computed per-iteration from loop variable),
`EdgeRenderPanel` (already has `SELECTION_COLOR`),
`CanvasOverlayPanel` (already has `PORT_COLOR`, `RUBBER_BAND_COLOR`, etc.),
`BlueprintCanvasTheme`, `WhiteprintCanvasTheme` (already all static finals),
all `api/` interfaces, `OrthogonalRouter`, `StraightLineRouter`, `DefaultGraphEdge`,
`NodeHostPanel`, `ClassDiagramDemo`, `ToolDiagramDemo`, `ToolFrameworkDiagram`.
