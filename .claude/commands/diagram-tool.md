# LayeredDiagramTool — Development Skill

Use this skill when working on the LayeredDiagramTool diagramming vapp.

## Key documents

- **Roadmap:** `docs/features/layereddiagramtool/ROADMAP.md` — phases, status, and per-phase implementation plans
- **Design reference:** `docs/features/layereddiagramtool/DESIGN.md` — architecture, component roles, layer system, interaction model, persistence format, and design decisions

## Source location

All source currently lives in:
```
virtualdesktop-java8/src/main/java/org/jwellman/demo/layereddiagramtool/
```

The target package after Phase 1 is `org.jwellman.diagram`. Do not begin a package
move mid-task unless the task explicitly asks for it.

## Key source files

| File | What it is |
|------|-----------|
| `LayeredDiagramTool.java` | Top-level JPanel + 5 inner classes (DiagramLayeredPane, GridPanel, DragHandler, DiagramConnection, LayerControlPanel — ResizeHandler is separate) |
| `DiagramShape.java` | Shape component (RECTANGLE, CIRCLE, TRIANGLE) |
| `DiagramText.java` | Text component wrapping JTextField |
| `PropertyEditorPanel.java` | Right-panel property editor (font + color) |
| `ColorPropertyPanel.java` | Color selection sub-panel with swatches |
| `ColorSwatch.java` | 24×24 color swatch with hover |
| `ResizeBorder.java` | 8-handle selection border |
| `ResizeHandler.java` | 8-direction resize mouse handler |
| `DiagramColorable.java` | Interface: fill + border color |
| `DiagramTextAware.java` | Interface: font name/size/style + text color |
| `ShapeType.java` | Enum: RECTANGLE, CIRCLE, TRIANGLE |
| `DiagramData.java` | Root JSON persistence object |
| `LayerData.java` | Per-layer persistence object |
| `ComponentData.java` | Abstract base with @JsonSubTypes dispatch |
| `ShapeData.java` | Shape persistence |
| `TextData.java` | Text persistence |

## Architecture in one paragraph

`LayeredDiagramTool` is a `JPanel` that assembles a `JToolBar` (north), a `JScrollPane`
wrapping a `DiagramLayeredPane` (center), and a 280px sidebar containing layer controls
and a property editor (east). `DiagramLayeredPane` is a `JLayeredPane` with 6 depth-bucketed
layers (0/100/200/300/400/500). Diagram components (`DiagramShape`, `DiagramText`) are
added to the layered pane and given a `DragHandler` at creation time. Selecting a
component installs a `ResizeBorder` and a `ResizeHandler`; deselecting removes them.
Everything persists to JSON via Jackson using a `ComponentData` polymorphic hierarchy.

## Layer constants (on DiagramLayeredPane)

```
GRID_LAYER       =   0   (GridPanel only — not user-accessible)
BACKGROUND_LAYER = 100
SHAPE_LAYER      = 200   (default for new shapes)
TEXT_LAYER       = 300   (default for new text)
CONNECTION_LAYER = 400   (reserved for DiagramConnection)
SELECTION_LAYER  = 500   (currently unused as a component layer)
```

## Coding standards

Follow the project-wide Java formatting rules in CLAUDE.md:
- One statement per line
- Always use braces on if/for/while blocks
- No inline method bodies (`{ return x; }` style)
- `@Override` on its own line
- Java 8 only — no streams, lambdas beyond `java.util.function`, or APIs not available in Java 8

`DiagramText` uses a lambda-style `java.util.function.Consumer` (Java 8 — fine).
BeanShell scripts elsewhere in the project cannot use bare varargs — but that does not
apply here since this tool does not call BeanShell directly yet.

## What NOT to do

- Do not run `mvn compile` or any build commands — the user compiles and tests
- Do not move the package to `org.jwellman.diagram` unless the task is Phase 1
- Do not add undo/redo, multi-select, or export unless those phases are explicitly requested
- Do not embed browser components or suggest JavaFX — see CLAUDE.md design philosophy

## Starting a task

1. Read `docs/features/layereddiagramtool/ROADMAP.md` to confirm which phase is being worked on and its status
2. Read `docs/features/layereddiagramtool/DESIGN.md` for the relevant architectural context
3. Read the specific source files the task will touch before editing them
4. Make the change; report what was done and what the user should verify interactively
