# SmartGrid — Design Requirements Document

## Context

`JTable` is a "stamping" engine optimized for 1990s memory constraints. It paints a single renderer component repeatedly, making cells effectively dead UI — interactive buttons, progress bars, and hover effects require fighting `CellEditor`/`CellRenderer` synchronization. Additionally, `JList`, `JTable`, and tree-table components each require separate data models despite representing the same conceptual structure.

This document captures requirements for a **SmartGrid** component that solves these problems by treating List, Table, and Tree-Table as three views of one unified data model, using viewport virtualization with live Swing components instead of stamped renderers.

**Component name**: `SmartGrid`
**Package**: `org.jwellman.swing.grid` (within the existing `org.jwellman.swing` custom Swing components package)

---

## Design Philosophy

1. **Unified Model** — List, Table, and Tree-Table are all specialized views of the same flat row stream. No separate `ListModel`, `TableModel`, or `TreeModel`.
2. **Live Components** — Every cell is a real, interactive Swing component. Hover effects, embedded buttons, and progress bars work natively.
3. **Viewport Virtualization** — Only ~20–50 rows are ever instantiated. Components are recycled and rebound as the user scrolls.
4. **Declarative Layout** — Row templates are defined as XML-like Blueprint strings, not `GridBagConstraints`. Templates can be registered at runtime via BeanShell.
5. **Reactive Bidirectional Flow** — Model drives UI; user interactions update the model; state changes propagate to the rest of the app.
6. **Pagination as First-Class Feature** — The grid knows how to request more data when approaching the end of the visible range.

---

## Core Interfaces

### 1. Data Interface: `GridModel`

`javax.swing.table.TableModel` cannot be reused as-is because it:
- Has no concept of row hierarchy (depth, parent-child relationships)
- Has no row-level metadata (tags, processing state)
- Has no async / paginated data loading path
- Exposes data by `(row, col)` index rather than named keys

A new `GridModel` interface is required. A `TableModelAdapter implements GridModel` wrapper can be provided later for backward compatibility with existing `TableModel` sources.

```java
public interface GridModel {
    int getRowCount();                           // total row count (may be virtual)
    GridRow getRow(int index);                   // synchronous for visible range
    void fetchRows(int from, int count,
                   GridModelListener listener);  // async for pagination
    List<ColumnDef> getColumns();                // column metadata for table view
    void addGridModelListener(GridModelListener l);
    void removeGridModelListener(GridModelListener l);
}
```

**`GridModelListener`** notifies the grid of:
- Rows inserted / removed / updated (individual or range)
- Column definitions changed
- Full model reset

### 2. Row Model: `GridRow`

A self-describing container carrying data, hierarchy, state, and metadata (tags):

| Field | Purpose |
|-------|---------|
| `Map<String, Object> data` | Actual values, keyed by column name |
| `int depth` | Tree indentation level (0 = root) |
| `boolean expanded` | Whether children are shown (tree view) |
| `boolean hasChildren` | Whether expand control is shown |
| `String parentId` | Link to parent row for hierarchy |
| `boolean selected` | Selection state (lives in model, not component) |
| `boolean processing` | Visual "glow" / activity indicator trigger |
| `Map<String, String> tags` | Declarative metadata (`fnd-type`, `fnd-style`, `fnd-mode`) |

Fluent builder API:
```java
grid.newRow()
    .data("id", "1024")
    .data("status", "Audit Required")
    .tag("fnd-style", "warning-glow")
    .depth(1)
    .add();
```

Access API:
```java
Object get(String key);
void put(String key, Object value);
String getTag(String key);
void setTag(String key, String value);
```

### 3. Rendering Interface: `Recyclable`

Analogous to `TableCellRenderer` but for full live components:

```java
public interface Recyclable {
    /** Reset to factory-default state — clear text, remove listeners, reset colors. */
    void prepareForReuse();

    /** Inject data and tags from a specific GridRow into this component. */
    void bind(GridRow row, NamespaceBridge bridge);
}
```

Row panel components (both compiled and scriptable) implement `Recyclable`. This is the primary rendering contract.

### 4. Component Factory: `GridComponentFactory`

```java
public interface GridComponentFactory {
    JComponent create(String typeTag);
    void register(String typeTag, Supplier<JComponent> supplier);
}
```

Default tags to register out of the box: `"standard"`, `"header"`, `"audit-log"`, `"sparkline"`. BeanShell scripts can call `register()` at runtime to add new template types without recompiling.

### 5. Column Definition: `ColumnDef`

Column definitions live on `GridModel` (via `getColumns()`), not configured separately on the widget. This keeps data structure and column metadata co-located — important when columns are driven by a data source (e.g., a database result set where columns are not known until runtime).

| Field | Purpose |
|-------|---------|
| `String key` | Maps to `GridRow.data` key |
| `String header` | Display name |
| `int preferredWidth` | Initial width |
| `boolean sortable` | Enable column sort |
| `boolean resizable` | Allow user resize |
| `String fndType` | Factory hint (`"sparkline"`, `"action-button"`, etc.) |

---

## Component Architecture

### Component Pool (Flyweight Manager)

- Groups reusable component instances by `typeTag` using a `Map<String, Stack<JComponent>>`
- **Checkout**: pulls from pool (calling `prepareForReuse()`) or creates via Factory
- **Release**: puts component back when its row scrolls out of viewport
- Memory footprint is flat — ~30 components in memory regardless of dataset size (100 or 100,000 rows)
- **Heterogeneous rows**: pool handles multiple component types simultaneously; each type has its own stack

### Component Recycler (Viewport Virtualization)

Uses a "stunt double" pattern inside a `JScrollPane`:

1. **Spacer** — transparent panel whose height equals `TotalHeight`, forces scrollbar to correct size
2. **Stage** — container holding only the currently visible row components
3. **Scroll Listener** — on scroll: recalculates `firstVisibleIndex`, shifts Stage Y position, recycles/rebinds components

```
JScrollPane
  └── JLayeredPane (or custom layout)
       ├── Spacer (full virtual height, no content)
       └── Stage (floats at current scroll position, holds ~30 live components)
```

Infinite scroll hook: when Stage approaches the bottom of the Spacer, dispatch `FETCH_MORE_DATA` event, then extend Spacer as new rows arrive.

### Visible Row Filter (Tree Logic)

A flat `List<GridRow> allRows` stores all rows. The Recycler works from a filtered `getVisibleRows()` result:

```
For each row in allRows:
  - If row.depth > depth of last unexpanded parent → skip (hidden)
  - Otherwise → include in visible list
  - If included and hasChildren and !expanded → set hiddenDepth = row.depth
```

Expanding/collapsing a node re-runs the filter and updates Spacer height. This is O(n) in visible rows.

### Tree Indentation

First column gets a leading margin: `indent = row.depth × 20px`. Implemented as an empty border on the first cell or a dedicated `<spacer id="treeIndent" width="auto"/>` in the Blueprint.

### Scriptable Templates: `ScriptableRecyclable`

A generic `Recyclable` whose layout, bind, and reset logic are all injected as strings:

- **Layout** — parsed from Blueprint XML string by `FoundationLayoutLoader`
- **Bind script** — BeanShell executed with `self` (the panel), `row` (GridRow), `bridge` (NamespaceBridge) in scope
- **Reset script** — BeanShell executed in `prepareForReuse()`
- **`find(String id)`** — recursive component-tree search with per-instance cache; returns the named component for script manipulation

Example BeanShell registration:
```java
String layout = "<row><label id='ts'/><label id='user'/><button id='act' text='Undo'/></row>";
String bind   = "self.find('ts').setText(row.get('timestamp')); " +
                "if(row.get('type').equals('DELETE')) self.setBackground(Color.RED);";
String reset  = "self.setBackground(null);";

gridFactory.register("delete-event",
    () -> new ScriptableRecyclable(layout, bind, reset, bridge));
```

### Blueprint DSL (`FoundationLayoutLoader`)

Parses XML-like strings into Swing component trees. Supported tags:

| Tag | Maps to |
|-----|---------|
| `<row>` | `JPanel` with horizontal layout |
| `<column>` | `JPanel` with vertical layout |
| `<label>` | `JLabel`, named via `setName(id)` |
| `<button>` | `JButton`, `action` attribute wired to BeanShell listener |
| `<icon>` | Icon-bearing label |
| `<spacer>` | Filler / tree indent spacer |
| `<equalizer>` | Embedded Equalizer component |

Attributes: `id` (component name for `find()`), `style` (CSS-like font/color), `weight` (layout weight), `gap`, `padding`.

### `ComponentFinder` Utility

```java
public static Component find(Container container, String id) {
    // Recursive search using Component.getName() as the identifier
    // Returns null if not found
}
```

`ScriptableRecyclable.find(id)` wraps this with a `Map<String, Component>` cache so repeated calls during high-speed scrolling are O(1).

### Bidirectional Event Flow

- User interaction in a recycled row updates `GridRow` via `row.put(key, value)`
- `bridge.dispatch("ROW_UPDATED", row)` propagates to Global App State
- `prepareForReuse()` **must** remove all dynamically-added listeners to prevent "ghost events" on recycled components
- `FoundationUtils.removeAllListeners(Component c)` — utility to strip all listener registrations from a component

Edit mode controlled by tags:
- `fnd-mode: read-only` → Factory provides template with `JLabel` fields
- `fnd-mode: edit` → Factory provides template with `JTextField`/`JSpinner` fields

---

## View Modes

| View | Configuration |
|------|--------------|
| **List** | 1 column; renders `data.get("default")` or a specified key; depth ignored |
| **Table** | N columns per `ColumnDef` list; depth ignored |
| **Tree-Table** | Respects `depth` for indentation and `expanded` for visibility |

Mode is a configuration concern on the grid component, not a model concern — the same `GridModel` instance can back all three views.

---

## Sorting and Filtering (Requirements Only — Implementation Deferred)

### Sort

- Sort is a **model-side** responsibility — `GridModel` produces a sorted row stream
- `ColumnDef` carries a `boolean sortable` flag
- The grid widget fires a `SortRequest(ColumnDef col, SortOrder order)` event when the user clicks a column header
- `GridModel` handles this event by re-ordering its row stream and notifying listeners
- Multi-column sort is a future enhancement

### Filter

- Filter is also **model-side** — `GridModel` applies a predicate to its row stream
- The grid widget exposes a `setFilter(GridModelFilter filter)` pass-through that delegates to the model
- `GridModelFilter` is a simple functional interface: `boolean accept(GridRow row)`
- Client-side filtering (grid widget caches rows and filters locally) is a future alternative for small datasets

---

## Performance Constraints

### Implementation Notes
The performance credit really goes to the recycler architecture: the JVM only ever manages ~20 row panels regardless of dataset size, so the cost of "live components" essentially disappears. A traditional JTable with 1,000 live buttons would crater; SmartGrid with 100,000 rows costs the same as 20.

The unified model is paying off too — the tree tab shares DefaultGridModel and StandardRowPanel with the flat table with zero extra code paths. The only tree-specific logic is computeVisibleRows() (13 lines) and the buildTreePrefix() helper (4 lines).

- Pool size: `ceil(viewportHeight / rowHeight) + BUFFER` (e.g., buffer = 5)
- `find(id)` cache: O(1) after first call per `id` per component instance
- Ghost listener prevention: mandatory in `prepareForReuse()`
- Async `fetchRows()` must not block the EDT

### Variable Row Height

Variable row height supports rows that grow to fit their content (multi-line cells, expanded card rows). This complicates the Recycler's scroll math:

- `TotalHeight = sum(rowHeight[i])` instead of `count × fixedHeight`
- The Recycler must maintain cumulative heights for visible rows
- `firstVisibleIndex` is found by binary search over cumulative heights rather than integer division
- Spacer height must be recalculated on row expand/collapse or data change

**`RowHeightProvider` interface:**
```java
public interface RowHeightProvider {
    int getRowHeight(GridRow row, int componentWidth);
}
```
- Default implementation returns a fixed constant
- Custom implementation can measure component preferred height
- Result is cached per row index; cache is invalidated when the row's data changes

---

## Integration Points (VirtualDesktop Context)

- **BeanShell**: `ScriptableRecyclable` templates defined and `register()`ed at runtime from the BeanShell console
- **NamespaceBridge**: existing bridge provides `execute()`, `publish()`, `dispatch()` for script context
- **Global App State**: `bridge.dispatch()` for Redux-style state propagation
- **Existing JTable code**: `TableModelAdapter implements GridModel` wrapper (future enhancement, not initial scope)

---

## Resolved Design Decisions

| Decision | Resolution |
|----------|-----------|
| Component name | `SmartGrid`, package `org.jwellman.swing.grid` |
| Column model location | On `GridModel` via `getColumns()` |
| Sort / filter scope | Requirements documented above; implementation deferred |
| Row height | Variable height supported via `RowHeightProvider`; fixed-height is the default |
| Selection model | Reuse `javax.swing.ListSelectionModel` for row selection |
| Async pagination | Callback-based `GridModelListener` pattern (Java 8 compatible) |
| Backward compat | `TableModelAdapter implements GridModel` wrapper deferred to a future phase |
