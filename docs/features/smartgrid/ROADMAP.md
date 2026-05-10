# SmartGrid — Feature Roadmap

## Progress Summary

| Phase | Feature | Status |
|-------|---------|--------|
| Baseline | Core interfaces, viewport recycler, `SmartGridDemo` | ✅ Complete |
| 1 | Row selection (`ListSelectionModel`, click / Shift / Ctrl) | ✅ Complete |
| 2 | Tree / hierarchy (depth, expand/collapse, ▶ / ▼) | ✅ Complete |
| 3 | Proportional column widths + horizontal scroll | ✅ Complete |
| 4 | Client-side sorting (column header click, ▲ / ▼ indicator) | ✅ Complete |
| 5 | Client-side filtering (`GridModelFilter`, search field) | ⬜ Not started |
| 6 | Tree enhancements (lazy children, keyboard expand/collapse) | ⬜ Not started |
| 7 | VApp integration (`SpecSmartGrid`, `ActionFactory`) | ⬜ Not started |
| 8 | Variable row height (`RowHeightProvider`) | ⬜ Not started |
| 9 | `GridComponentFactory` + `ScriptableRecyclable` (BeanShell) | ⬜ Not started |
| 10 | Pagination (explicit page nav), footer row, renderer interfaces | ✅ Complete |
| 11 | Bidirectional data flow / inline edit mode | ⬜ Not started |
| 12 | Header panel refactor: persistent panels, in-place bound updates | ⬜ Not started |

---

## Baseline (MVP — complete)

The following are already implemented and working via `SmartGridDemo`:

- `GridRow`, `GridModel`, `GridModelListener`, `ColumnDef` — core data contracts
- `Recyclable`, `ComponentPool` — rendering contract and object pool
- `DefaultGridModel` — list-backed model with bulk-load API
- `StandardRowPanel` — default row renderer (alternating colors, tag-based styling)
- `SmartGrid` — viewport virtualizer with `~N+2` live components regardless of row count
- `SmartGridDemo` — standalone demo with 1,000 rows and warning-glow tagged rows

Each phase below adds one feature, verified by running the demo interactively.
Phases are ordered by priority; earlier phases are prerequisites for later ones.

---

## Phase 1 — Row Selection ★

**Why first**: Selection is a basic table affordance expected by users; the MVP has none.

### Implementation

**`SmartGrid.java`**
- Add `DefaultListSelectionModel selectionModel` field
- `SmartGrid` constructor: create it with `MULTIPLE_INTERVAL_SELECTION`
- Expose `getSelectionModel()` and `addListSelectionListener(ListSelectionListener)`
- In `refresh()`: pass `selectionModel` as a parameter to `bind()` so row panels
  can check `isSelectedIndex(rowIndex)` without holding a reference

**`Recyclable` interface** — update `bind` signature:
```java
void bind(GridRow row, int rowIndex, ListSelectionModel selectionModel);
```

**`StandardRowPanel.java`**
- `bind()`: if `selectionModel.isSelectedIndex(rowIndex)`, set background to
  `UIManager.getColor("Table.selectionBackground")` and foreground to
  `UIManager.getColor("Table.selectionForeground")`; otherwise apply normal
  alternating/tag colors
- `bind()`: add a `MouseAdapter` whose `mousePressed()` does:
  ```java
  if (shiftDown)  selectionModel.addSelectionInterval(rowIndex, rowIndex);
  else if (ctrlDown) selectionModel.addSelectionInterval(rowIndex, rowIndex);
  else            selectionModel.setSelectionInterval(rowIndex, rowIndex);
  ```
  then calls `SmartGrid.refresh()` so all slots repaint
- `prepareForReuse()`: remove the listener to prevent ghost events

**Optional — checkbox column**
- A `ColumnDef` with `fndType = "checkbox"` tells `StandardRowPanel` to render a
  `JCheckBox` in that cell whose state mirrors `selectionModel.isSelectedIndex(rowIndex)`

### Demo test (`SmartGridDemo`)
- Add a `JLabel statusBar` at `BorderLayout.SOUTH`
- Wire a `ListSelectionListener` that updates it: `"N row(s) selected"`
- Add "Select All" (`selectionModel.addSelectionInterval(0, rowCount-1)`) and
  "Clear" (`selectionModel.clearSelection()`) toolbar buttons

---

Verification

     Run SmartGridDemo and exercise each tab:

     1. Table tab — single click selects a row (highlighted in system blue); Shift+Click
     extends range; Ctrl+Click toggles individual rows; "Select All" selects all 1000;
     "Clear" deselects all; status label tracks count throughout.
     2. Tree tab — clicking a leaf employee row selects it; clicking a department row
     selects it AND toggles its expand/collapse.
     3. List tab — same selection behavior as Table tab with a single column.
     4. Scroll test — select row 5, scroll down past it, scroll back; row 5 remains
     highlighted (selection lives in the model, not the component).
     

---

## Phase 2 — Tree / Hierarchy Support ★

**Why**: Moved up to priority 2 — the tree view requires different data and a
different tab in the demo, making it the natural next step to validate the unified
model concept before refining column layout.

The demo application is restructured as a **`JTabbedPane`** with three tabs:
- **Table** — existing 1,000-row flat employee dataset
- **Tree** — hierarchical departments → employees; demonstrates expand/collapse
- **List** — single-column view; demonstrates "list is just a 1-column table"

### Implementation

**`DefaultGridModel.java`**
- Add `List<GridRow> visibleRows` + `boolean visibleRowsDirty` fields
- `computeVisibleRows()`: iterates `rows`, skips any row with
  `depth > hiddenDepth` (where `hiddenDepth` is set when a collapsed parent is seen)
- `ensureVisible()`: calls `computeVisibleRows()` only if dirty (avoids O(n²) on bulk load)
- `addRow()` → marks dirty; `notifyDataChanged()` → forces recompute then fires listeners
- `getRowCount()` / `getRow(i)` → delegate to `visibleRows`

For flat data (all `depth=0`, `hasChildren=false`) `computeVisibleRows()` returns
all rows — no behavioral change for existing flat-table use.

**`StandardRowPanel.java`**
- New constructor: `StandardRowPanel(List<ColumnDef>, Runnable expandCollapseAction)`
  (old 1-arg constructor delegates to this with `null`)
- `bind()`:
  - First cell left border set to `8 + row.getDepth() * 16` px
  - First cell text prefixed with `"▼ "` (expanded), `"▶ "` (collapsed), or `"  "` (leaf at depth > 0)
  - If `row.isHasChildren() && expandCollapseAction != null`: add a `MouseAdapter`
    that toggles `row.setExpanded()` then calls `expandCollapseAction.run()`
- `prepareForReuse()`: removes the listener; resets first-cell border to default

**`SmartGrid.java`**
- Pool factory changed to pass `expandCollapseAction`:
  ```java
  final GridModel m = model;
  pool = new ComponentPool(() -> new StandardRowPanel(cols, () -> {
      if (m instanceof DefaultGridModel) ((DefaultGridModel) m).notifyDataChanged();
  }));
  ```

**`SmartGridDemo.java`** — fully rewritten with `JTabbedPane`:
- `buildTableTab()` — 1,000 flat rows (existing demo, with description label)
- `buildTreeTab()` — 5 departments (depth 0, collapsed by default) + employees (depth 1)
- `buildListTab()` — single `ColumnDef`, list of programming languages

### Demo test
- Click department rows in Tree tab → employees appear/disappear; scrollbar resizes
- Confirm Table and List tabs are unaffected

---

Verification Steps

 1. Compile — mvn compile in virtualdesktop-java8/; expect zero errors
 2. Run SmartGridDemo and exercise the Tree tab:
   - All five department rows visible (collapsed); employee rows hidden
   - Click Engineering → 13 employee rows appear below it; scrollbar grows
   - Click Engineering again → employees collapse; scrollbar shrinks
   - Expand two departments simultaneously; verify correct employee sets
   - Select an employee row while a department is expanded; verify selection
 highlight persists after scrolling
 3. Run the List tab:
   - 38 language rows + 2 warning-glow entries visible
   - Selection toolbar works normally (single-column Select All / Ctrl+Click)
 4. Run the Table tab:
   - Confirm no regression — flat 1,000-row data unaffected by visible-row logic
   
---

## Phase 3 — Proportional Column Widths + Horizontal Scroll ✅

**Why**: `GridLayout(1, N)` gives every column equal width; `ColumnDef.preferredWidth`
is currently ignored.

### Implementation (as built)

**Rejected approach**: `GridBagLayout` with `weightx` proportional to `preferredWidth`.
Each independent `JPanel` (row, header, footer) computes its own pixel allocation from
floating-point fractions, producing different widths per panel and causing misalignment.

**Actual approach** — shared mutable `int[] columnWidths` owned by `SmartGrid`:

- `SmartGrid` computes pixel widths once per viewport-width change:
  - If `vpWidth >= totalPref`: scale proportionally; last column absorbs rounding remainder
  - If `vpWidth < totalPref`: use `preferredWidth` values as-is; horizontal scroll appears
- All `StandardRowPanel` instances in the pool hold a **reference to the same array**;
  `SmartGrid` updates it in-place, so the next `bind()` call sees the new widths automatically
- Header, rows, and footer all use `null` layout with `setBounds(x, 0, w, rowHeight)` —
  the only way to guarantee pixel-exact alignment across independently-painted panels
- `VirtualCanvas.getScrollableTracksViewportWidth()` returns `false` when total column
  width exceeds viewport, enabling the horizontal scrollbar

### Demo test
- Column widths in demo: `ID=50, Name=220, Dept=180, Salary=110, Status=80`
- Resize window wider → columns scale proportionally; header / rows / footer stay aligned
- Resize window narrower than 640px total → horizontal scrollbar appears; columns hold fixed widths

---

## Phase 4 — Client-Side Sorting ✅

**Why**: Clicking a column header to sort is a universal data-grid expectation.

### Implementation

**New class `SortOrder.java`** (enum):
```java
public enum SortOrder { NONE, ASCENDING, DESCENDING }
```

**`SmartGrid.java`**
- Add `ColumnDef sortColumn` and `SortOrder sortOrder` fields (default both null/NONE)
- `buildHeader()`: for sortable columns, add `MouseListener` that cycles
  `NONE → ASCENDING → DESCENDING → NONE` and calls `model.sort(key, order)`
- Update header label text to include ` ▲` / ` ▼` indicator

**`DefaultGridModel.java`**
- Add `List<Integer> sortIndex` (starts as identity `[0,1,2,...,n-1]`)
- `sort(String key, SortOrder order)`: rebuild `sortIndex` using `Collections.sort`
  with a comparator on `GridRow.get(key).toString()` (lexicographic for now);
  reverse for DESCENDING; reset to identity for NONE
- `getRow(int i)` → `rows.get(sortIndex.get(i))`
- After sort: call `notifyDataChanged()`

### Demo test
- Mark ID, Name, Dept columns `sortable = true`
- Click headers; verify sort order; verify sort indicator appears/disappears

---

## Phase 5 — Client-Side Filtering

**Why**: Quick text filtering over large datasets is the most common grid interaction.

### Implementation

**New interface `GridModelFilter.java`**:
```java
public interface GridModelFilter {
    boolean accept(GridRow row);
}
```

**`DefaultGridModel.java`**
- Add `GridModelFilter filter` field (default: accepts all)
- Add `List<Integer> filteredIndex` (starts as all rows in sort order)
- `setFilter(GridModelFilter f)`: rebuild `filteredIndex` by iterating `sortIndex`
  and keeping indices where `filter.accept(row)` is true; fire `modelReset()`
- `getRowCount()` → `filteredIndex.size()`
- `getRow(i)` → `rows.get(filteredIndex.get(i))`

**`SmartGrid.java`**
- Add `setFilter(GridModelFilter f)` that delegates to model

### Demo test (`SmartGridDemo`)
- Add a `JTextField searchField` in a toolbar panel at `BorderLayout.NORTH`
- Wire `DocumentListener`: filter accepts rows where any column value contains the
  search text (case-insensitive)
- Show `"Showing N of 1000 rows"` next to the search field, updated on each filter

---

## Phase 6 — Tree / Hierarchy Support (additional enhancement)

**Why**: Placeholder for further tree refinements after Phase 2 validates the basics.
Examples: lazy child loading, drag-to-reorder nodes, keyboard expand/collapse.

### Implementation

**`DefaultGridModel.java`**
- Add `List<GridRow> visibleRows` field managed by `computeVisibleRows()`:
  ```java
  int hiddenDepth = Integer.MAX_VALUE;
  for (GridRow row : allRows) {
      if (row.getDepth() > hiddenDepth) continue;
      visibleRows.add(row);
      hiddenDepth = (row.isHasChildren() && !row.isExpanded())
                   ? row.getDepth() : Integer.MAX_VALUE;
  }
  ```
- `getRowCount()` → `visibleRows.size()`; filter/sort compose on top

**`StandardRowPanel.java`**
- `bind()`: set left border on first cell to `row.getDepth() * 20` pixels
- If `row.isHasChildren()`: prepend a `▶` (collapsed) or `▼` (expanded) label;
  clicking it calls `row.setExpanded(!row.isExpanded())` and
  `model.notifyDataChanged()`

**New demo class `SmartGridTreeDemo.java`**
- Load departments → employees hierarchy with depth 0 / depth 1
- Verify expand/collapse, correct indentation, scroll position stability

---

## Phase 7 — VApp Integration

**Why**: Provides a real-world host for the grid inside VirtualDesktop.

### Implementation

**New class `org.jwellman.virtualdesktop.vapps.SpecSmartGrid`**
- Extends `VirtualAppFrame`; constructor builds a `DefaultGridModel` + `SmartGrid`
  with the employee dataset from `SmartGridDemo`
- Adds the grid to the vapp panel with `BorderLayout.CENTER`
- Wires a status-bar row count label

**`ActionFactory.java`**
- Register `SpecSmartGrid` in the vapp registry

### Demo test
- Launch VirtualDesktop; verify "Smart Grid" appears in the menu
- Open it as an internal frame; scroll, resize, interact

---

## Phase 8 — Variable Row Height (`RowHeightProvider`)

**Why**: Multi-line cells (notes, descriptions) require rows to grow with content.

### Implementation

**New interface `RowHeightProvider.java`**:
```java
public interface RowHeightProvider {
    int getRowHeight(GridRow row, int componentWidth);
}
```

**`SmartGrid.java`**
- Add `RowHeightProvider heightProvider` (default: `(row, w) -> rowHeight`)
- When provider is non-default, maintain `int[] cumulativeHeights` rebuilt on
  `modelReset()`; length = `model.getRowCount() + 1`
- `firstVisibleIndex`: binary search `cumulativeHeights` for `scrollY`
- Slot `setBounds()` height = `cumulativeHeights[i+1] - cumulativeHeights[i]`
- `VirtualCanvas.getPreferredSize()` height = `cumulativeHeights[rowCount]`

### Demo test
- Add a "Notes" column with varying text lengths
- `RowHeightProvider` returns `Math.max(32, noteText.length() / 40 * 18)`
- Verify scrollbar size and slot heights update correctly on scroll

---

## Phase 9 — `GridComponentFactory` + `ScriptableRecyclable`

**Why**: Tag-based component dispatch + BeanShell templates enable runtime-defined row UIs.

### Implementation

**New interface `GridComponentFactory.java`** (see design doc)

**New class `DefaultGridComponentFactory.java`**
- Registry: `Map<String, Supplier<JComponent>>`
- Pre-registers `"standard" → StandardRowPanel`, `"sparkline" → SparklineRowPanel`
- `SmartGrid` uses the factory when creating pool objects

**New class `ComponentFinder.java`**
- `static Component find(Container c, String id)` — recursive by `getName()`

**New class `ScriptableRecyclable.java`**
- Parses a Blueprint XML string in constructor via `FoundationLayoutLoader`
- `bind()` executes the bind BeanShell script with `self`, `row` in scope
- `prepareForReuse()` executes the reset script
- `find(String id)` delegates to `ComponentFinder` with per-instance cache

**New class `FoundationLayoutLoader.java`**
- Parses `<row>`, `<column>`, `<label id=...>`, `<button id=... action=...>` tags
- Uses standard SAX or DOM parser (available in Java 8)

### Demo test
- From BeanShell console: register a `"custom"` template; switch a SmartGrid to use it
- Show live row UI change without restarting the application

---

## Phase 10 — Pagination / Footer Row / Renderer Interfaces ✅

**Why**: Enables grids backed by databases or remote APIs with millions of rows.
Also adds column-aligned footer aggregates and customisable header/footer rendering.

### What was built

- **`HeaderCellRenderer`** / **`FooterCellRenderer`** interfaces — per-column renderer
  contracts; `DefaultHeaderCellRenderer` and `DefaultFooterCellRenderer` provide blanks
- **`PaginationBar`** — ◀ ‹ [1][2][3][4][5] › ▶ with "Showing X–Y of Z rows" label;
  window of 5 page buttons centers on current page
- **`SmartGrid`** — `setPageSize(int)`, `goToPage(int)`, `setFooterRenderer()`,
  `setHeaderRenderer()` public API; SOUTH panel built lazily (zero overhead when unused)
- **Footer** — column-aligned (same `int[]` widths as header/rows); receives current
  page's `List<GridRow>` for per-page aggregates AND the full model for global aggregates
- **Demo "Paged" tab** — 50 rows/page, footer shows row count / salary sum / active count

### Original roadmap item (deferred)

### Implementation

**`GridModel` interface** — add `fetchRows()` method (already in design doc):
```java
void fetchRows(int from, int count, GridModelListener callback);
```

**New class `LazyGridModel implements GridModel`**
- Delegates to `DataProvider` functional interface for async row loading
- Maintains a page cache: `Map<Integer, List<GridRow>>`
- `getRowCount()` returns the virtual total (provided at construction)
- `getRow(i)`: returns cached row or a placeholder `GridRow` and triggers fetch

**`SmartGrid.refresh()`**
- After positioning slots: if `firstRow + visibleCount > loadedCount - PREFETCH_THRESHOLD`,
  fire `model.fetchRows(loadedCount, PAGE_SIZE, this)`

### Demo test
- `SmartGridDemo` variant with `LazyGridModel`, virtual count = 1,000,000
- Slow scroll through large dataset; verify "Loading…" placeholder rows appear and
  are replaced as pages arrive

---

## Phase 11 — Bidirectional Data Flow / Edit Mode

**Why**: Inline editing completes the "live form" vision for each row.

### Implementation

**New class `EditableRowPanel.java`**
- Like `StandardRowPanel` but uses `JTextField` per column
- `bind()`: populates field text and adds `FocusListener` that calls
  `row.put(key, field.getText())` on focus-lost
- `prepareForReuse()`: removes focus listeners, clears fields

**`FoundationUtils.removeAllListeners(Component c)`** — strips all registered
listeners using reflection (helper for all panels)

**`SmartGrid.java`**
- `setEditable(boolean)`: sets a flag; `refresh()` checks out `EditableRowPanel`
  from a separate pool when editable
- Optional: `addRowEditListener(RowEditListener)` for external change callbacks

### Demo test
- Add a toolbar toggle button "Edit Mode"
- Enable; edit a row's Name field; scroll away and back — data persists in `GridRow`
- Disable; verify fields become labels again

---

## Phase 12 — Header Panel Refactor: Persistent Panels, In-Place Bound Updates

**Why**: The current implementation treats both a manual window resize and a
scrollbar appearing/disappearing identically, because it only looks at one
observable: `vpWidth != lastVpWidth`. It has no knowledge of *why* the width
changed.

When the vertical scrollbar appears or disappears, it consumes or releases roughly
15–17 pixels of horizontal space. The viewport shrinks or grows by that amount,
`getWidth()` returns a different value than `lastVpWidth`, and the code can't tell
whether that's because the user dragged the window wider or because the scrollbar
just toggled.

The rebuild *is* technically correct in both cases — if the viewport width changed
for any reason, the proportional column widths need to be recalculated, and the
null-layout header cells need to reflect those new pixel widths. So there's no bug
in the logic, only in the cost of how it responds.

The "heavy" part is the *implementation choice*: create new `JPanel` objects, new
`JLabel` instances, re-add all the children from scratch, and swap the whole thing
into `columnHeaderView`. The alternative — which is what the data rows already do
— would be to keep persistent panel references and only call `setBounds()` on the
existing child components when widths change. That's analogous to how the slot
JPanels are repositioned with `setBounds()` on every scroll rather than being
recreated. Applied to the header, you'd keep the same `filterRowPanel` and just
reposition the fields inside it, which would also eliminate the focus-loss problem
entirely without needing the `rebuildHeaderView()` workaround.

So the fix currently in place is correct, the underlying inefficiency is real, and
the cure would be making the header panels persistent — consistent with how the
recycler handles the data rows.

### Implementation

**`SmartGrid.java`**
- Store `labelRowPanel` and (if enabled) `filterRowPanel` as persistent fields
  created once (in constructor / `setColumnFiltersVisible(true)`)
- On column-width change: update `setBounds()` on each child component of the
  persistent panels; update `setPreferredSize()` on the panels themselves
- On sort-state change: only update the text/font of the affected label cell, not
  the entire panel
- Remove `rebuildHeaderView()` workaround — focus loss no longer occurs because
  `filterRowPanel` is never reparented

### Expected outcome
- No functional change visible to the user
- Focus stays in the active filter field when the scrollbar appears/disappears
- Slightly reduced object allocation on every viewport-width change

---

## Notes on Testing Strategy

All phases are verified interactively via `SmartGridDemo` or a new demo variant.
No automated tests exist yet; add JUnit coverage once the model API stabilizes:
- `DefaultGridModel`: sort/filter/visible-row correctness
- `ComponentPool`: checkout/release cycle, pool size bounds
- `GridRow`: fluent builder, tag access
