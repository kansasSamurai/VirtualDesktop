# SmartGrid — Feature Roadmap

## Progress Summary

| Phase | Feature | Status | Notes |
|-------|---------|--------|-------|
| Baseline | Core interfaces, viewport recycler, `SmartGridDemo` | ✅ Complete | |
| 1 | Row selection (`ListSelectionModel`, click / Shift / Ctrl) | ✅ Complete | |
| 2 | Tree / hierarchy (depth, expand/collapse, ▶ / ▼) | ✅ Complete | |
| 3 | Proportional column widths + horizontal scroll | ✅ Complete | |
| 4 | Client-side sorting (column header click, ▲ / ▼ indicator) | ✅ Complete | |
| 5 | Client-side filtering (`GridModelFilter`, search field) | ✅ Complete | |
| 7 | VApp integration (`SpecSmartGrid`, `ActionFactory`) | ✅ Complete | |
| 10 | Pagination (explicit page nav), footer row, renderer interfaces | ✅ Complete | |
| 9a | `GridComponentFactory` — registration / dispatch mechanism | ✅ Complete | Prerequisite for tree and edit mode |
| 11 | Bidirectional data flow / inline edit mode (rudimentary) | ✅ Complete | |
| 16 | Checkbox selection strip — standard chrome, left of data columns | ✅ Complete | `Selectable` interface; `setCheckboxColumnVisible(boolean)` |
| 6 | Tree enhancements — Strip abstraction, TreeZoneStrip, GroupHeaderRowPanel | ✅ Complete | |
| 9b | `ScriptableRecyclable` + Blueprint DSL + BeanShell integration | ✅ Complete | `RowScript` adapter + string-eval paths; `ScriptBridge`; `DefaultGridComponentFactory`; live swap via shared `ScriptSpec` |
| 8 | Variable row height (`RowHeightProvider`) | ⬜ End | Deferred by design — touches core scroll math |
| 12 | Header panel refactor: persistent panels, in-place bound updates | ⬜ End | |
| 13 | Filter search cache with configurable row-count threshold | ⬜ End | |
| 14 | Structured filter expressions (`FilterExpression` alongside lambda) | ⬜ Future | Enables query folding — see `docs/features/query-folding/DISCUSSION.md` |
| 17 | Column freezing / pinning — fixed left columns, scrolling right columns | ⬜ Future | Split-pane architecture; see phase detail below |
| 18 | Scroll repaint quality — eliminate canvas flash (blit mode + setVisible refactor) | ⬜ Future | Two-part fix; canvas-bg mitigation currently in place |

---

## Positioning: SmartGrid vs. Legacy JTable

### Where SmartGrid clearly wins

The live-component model is the decisive advantage for any row that needs real
interaction. A `JTable` with an actual interactive `JButton` in a cell requires
synchronizing a renderer (paints the button) with an editor (activates on click),
both stateless, both pretending to be something they're not. `FeaturedRowPanel`
just *has* a button. That's not a marginal improvement — it's a different category
of problem solved.

Heterogeneous row types, the unified list/table/tree model, declarative `fnd-type`
dispatch, integrated filtering with composed predicates, the footer/pagination
infrastructure, the `CellRenderer` registry for per-column formatting — none of
these exist in `JTable` without significant third-party libraries or painful custom
subclassing.

### Where JTable still holds ground

**Extreme-scale pure display.** `JTable`'s stamping model creates exactly one
renderer component regardless of row count. At 5,000,000 rows of read-only data,
the memory delta is real. SmartGrid's ~20 live JPanels is still very small, but
it's non-zero overhead per visible row whereas `JTable` is genuinely constant.

**Accessibility.** `JTable` implements the `Accessible` interface with full screen
reader support, keyboard focus traversal at the cell level, and ARIA-equivalent
metadata. SmartGrid has none of this. For enterprise applications in regulated
industries this is a disqualifier. *Full industry accessibility is not on this
roadmap until there is commercial motivation to add it — speed and power-user
ergonomics are the primary drivers for this component.*

**Out-of-the-box behaviors.** Column drag-to-reorder, `Ctrl+C` to copy selected
rows as tab-separated text, built-in print support via `JTable.print()` — none of
these exist in SmartGrid yet.

**Cell-level keyboard navigation.** Arrow keys moving a cursor through individual
cells, `Tab` moving focus cell-by-cell — `JTable` does this natively. SmartGrid's
interaction unit is the whole row.

### Gaps worth filling (priority order)

**Keyboard row selection** (`↑↓` to move the selected row, `Shift+↑↓` to extend)
is the single highest-value missing feature for power users — it's what separates
"feels like a professional grid" from "works with mouse only."

**`Ctrl+C` clipboard copy** of selected rows as tab-separated text is a one-method
addition that pays enormous dividends for forensic/data-analysis workflows. Select
10 rows, paste into Excel — a complete workflow for the target user.

**Column resizing by mouse drag** is noted in Phase 12 as essentially already
designed (update `columnWidths[i]` in a `MouseMotionListener`). Given the payoff
in perceived polish it is closer than it appears.

### The honest positioning

SmartGrid is what `JTable` would have been if Swing had been designed in 2010
rather than 1997 — it trades `JTable`'s extreme memory parsimony for developer
ergonomics and visual richness. For a desktop framework aimed at power users who
need forensic tools, data visualization, and rapid customization, it is clearly
the right choice. For a regulated enterprise application with accessibility
requirements and millions of rows of static data, `JTable` or a commercial grid
is still correct.

The proof that the vision is sound: the gap between "out of the box" and "custom"
has almost disappeared. `FeaturedRowPanel` took 20 lines and one method call. That
ratio — expressive power per line of user code — is where `JTable` never got close.

---

## Design Notes — Known Behaviors and Fixes

### Re-entrant `refresh()` and Look-and-Feel layout (Fixed)

**Symptom**: Rows rendered blank (checkbox visible, data panel empty) when resizing an
internal frame that contains a SmartGrid. Reproducible only under FlatLaf; the system
LAF did not trigger it. Blank rows persisted even after scrolling away and back.
No console errors.

**Root cause**: Two compounding problems, both rooted in `rebuildHeaderView()` being
called from *inside* `refresh()`.

When the viewport width changes, `refresh()` calls
`scrollPane.setColumnHeaderView(buildHeader(...))`. FlatLaf performs a heavier
synchronous layout pass than the system LAF during this call — it recomputes component
metrics (borders, insets, focus-ring geometry) immediately rather than deferring them.
This layout pass can change the viewport height by a pixel or two, which fires the
viewport `ChangeListener` *synchronously*, which calls `refresh()` again before the
outer call has computed `visibleCount` or bound any slots. The re-entrant call operates
on the same mutable `slots[]` and `slotTypes[]` arrays, leaving the outer call with an
inconsistent view of the world once it resumes.

A second related problem: even without full re-entry, the `vpHeight` captured at the
top of `refresh()` was stale by the time `visibleCount` was computed, because
FlatLaf's layout inside `setColumnHeaderView` could shrink or grow the viewport.
A slot count based on the pre-layout height means some on-screen rows have no slot
bound to them — they show as blank canvas background.

**Fix** (`SmartGrid.java`):

1. **Re-entrancy guard** — `refresh()` now sets a `refreshing` flag and delegates to
   `doRefresh()`. Any re-entrant call bails out immediately via `if (refreshing) return`.
   The guard is cleared in a `finally` block.

2. **Re-read `vpHeight` after header rebuild** — after `rebuildHeaderView()` returns,
   `vpHeight` is re-read from the viewport before computing `visibleCount`. This ensures
   slot allocation reflects the actual post-layout viewport height, not a transient value
   captured before FlatLaf's layout pass ran.

**Why the system LAF didn't show this**: The system (Windows) LAF delegates most layout
measurement to the OS and does less work synchronously during `setColumnHeaderView()`.
The ChangeListener is typically fired asynchronously or not at all for the same viewport
resize that FlatLaf triggers synchronously.

**Relevance to Phase 12**: Phase 12 (persistent header panels) will eliminate
`rebuildHeaderView()` entirely, removing the root cause rather than just guarding
against it. Until then, the guard and re-read are the correct mitigations.

### Could FlatLaf fix this on their end?

FlatLaf can't change *when* the `ChangeListener` fires — that mechanism lives entirely
in `javax.swing.JViewport.fireStateChanged()`, which is core Swing. FlatLaf has no
ownership of it. The listener fires whenever `JViewport.reshape()` detects a size
change, and that call comes from Swing's own scroll pane layout manager, not from
FlatLaf.

What FlatLaf *does* own is the layout work that *causes* `reshape()` to see a size
change. The chain is:

```
setColumnHeaderView()
  → Swing asks the scroll pane's LayoutManager to lay out
    → FlatLaf's layout computes component metrics (borders, insets, focus geometry)
      → viewport gets a slightly different height than before
        → JViewport.reshape() detects the change
          → fireStateChanged() fires the ChangeListener
            → refresh() re-enters
```

FlatLaf's fix opportunity is in step 3. If their `layoutContainer()` override were
*idempotent* — meaning running it twice with the same header height produced identical
viewport dimensions both times — then `reshape()` would see no change, and
`fireStateChanged()` would never fire. The ChangeListener is a correct and faithful
notification; the problem is that FlatLaf's layout is unstable enough that rebuilding
the header (with the same height) produces a different viewport geometry than the
previous layout computed.

The system LAF avoids this because it delegates most metric computation to the OS and
its layout is stable — rebuilding the header panel with the same row height produces
exactly the same viewport dimensions, so `reshape()` sees no delta and stays quiet.

So to directly answer: FlatLaf moving the ChangeListener call wouldn't help because
they don't own it. But FlatLaf stabilizing their layout so it doesn't produce a
different viewport size when nothing semantically changed *would* fix it at the root.
That's a harder problem for them because their richer component geometry
(anti-aliased borders, animated focus rings, precise insets) is exactly what makes
those metrics non-trivially reproducible across consecutive layout passes.

### How the geometry changes between two calls — a concrete example

The most common mechanism is the horizontal scrollbar negotiation cycle. Swing's scroll
pane layout must answer a circular question: "does the content need a horizontal
scrollbar?" — but the answer depends on the viewport width, which depends on whether a
vertical scrollbar is showing, which depends on the content height, which depends on
the viewport height, which depends on whether a horizontal scrollbar is showing.

Swing breaks this cycle by running the layout in a fixed sequence and committing to the
first answer. FlatLaf's pass may reach a *different* committed answer than the prior
pass if its starting assumptions differ by even one pixel.

Concrete scenario with a 600px-wide, 400px-tall viewport and a header that is 32px
tall:

**Prior layout** (before `setColumnHeaderView` is called):
```
Available height        = 400px
Header height           = 32px  (existing header)
Horizontal scrollbar?   → content width 640 > viewport 600 → YES  → hsbHeight = 17px
Viewport height         = 400 - 32 - 17 = 351px
```
Viewport settles at 351px. No `reshape()` delta. Quiet.

**Layout triggered by `setColumnHeaderView`** (FlatLaf rebuilds metrics):
During FlatLaf's synchronous layout, the *new* header panel is being committed. For a
brief moment FlatLaf evaluates the scroll pane geometry while the header's preferred
size has not yet been flushed from the component's internal cache — it temporarily
reads as 0px wide. With no content width competing against the viewport width, FlatLaf
tentatively decides:
```
Horizontal scrollbar?   → content width 0 < viewport 600 → NO   → hsbHeight = 0px
Viewport height         = 400 - 32 - 0  = 368px
```
`JViewport.reshape()` is called with height=368. That differs from 351. `fireStateChanged()`
fires. Our `ChangeListener` re-enters `refresh()`.

On the *next* layout pass (triggered by that ChangeListener), the header's preferred
size is now properly cached, content width is 640 again, and the layout stabilises back
at 351px. But the damage is already done — `refresh()` was re-entered mid-execution.

**The general test for your own layout managers**: after any mutation that could change
geometry (adding a child, changing an inset), call `layoutContainer()` twice in a row
and assert that the bounds of every managed component are identical on the second call.
If they differ, your layout manager is non-idempotent and will trigger spurious
`ChangeListener` or `ComponentListener` events in hosts that call layout from inside an
event handler.

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

## Phase 5 — Client-Side Filtering ✅

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

## Phase 7 — VApp Integration ✅

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

## Phase 9b — `ScriptableRecyclable` + Blueprint DSL + BeanShell Integration

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

### Plan - SmartGrid Phase 9b — ScriptableRecyclable + Blueprint DSL + BeanShell Integration                                          
                                                        
#### Context

 Phase 9a (GridComponentFactory registration/dispatch) is complete. Phase 9b extends it: any row with an
 fnd-type tag can now be rendered by an XML-defined component tree whose bind() / prepareForReuse()
 lifecycle is driven by inline BeanShell scripts. This enables fully runtime-defined row UIs — register a
 new row type from the BeanShell console without restarting the app.

 Prerequisites (all confirmed ✅)

- GridComponentFactory interface — register(String, Supplier<JComponent>)
- SmartGrid.registerRowRenderer() (line 437) and SmartGrid.getColumnWidths() (line 251) — wired and ready
- Recyclable interface — bind(GridRow, int) + prepareForReuse()
- BeanShell bsh.Interpreter is on the classpath (full interpreter, not embedded subset)
- ComponentPool / typedPools dispatch in SmartGrid.doRefresh() — already handles typed rows

---

### Critical Files

┌──────────────────────────────────────────────────────────┬──────────────────┐
│                           File                           │      Action      │
├──────────────────────────────────────────────────────────┼──────────────────┤
│ org/jwellman/swing/grid/ComponentFinder.java             │ Create           │
├──────────────────────────────────────────────────────────┼──────────────────┤
│ org/jwellman/swing/grid/RowBlueprint.java                │ Create           │
├──────────────────────────────────────────────────────────┼──────────────────┤
│ org/jwellman/swing/grid/ScriptableRecyclable.java        │ Create           │
├──────────────────────────────────────────────────────────┼──────────────────┤
│ org/jwellman/swing/grid/DefaultGridComponentFactory.java │ Create           │
├──────────────────────────────────────────────────────────┼──────────────────┤
│ org/jwellman/demo/SmartGridDemo.java                     │ Modify (add tab) │
└──────────────────────────────────────────────────────────┴──────────────────┘

No changes required to SmartGrid.java, GridComponentFactory.java, Recyclable.java, or ComponentPool.java.

---

### Verification

1. mvn compile in virtualdesktop-java8/ — zero errors
2. Run SmartGridDemo — verify the new "Scripted" tab appears
3. Scroll the Scripted tab — every 30th row shows dark-blue background with cyan main label
4. Non-scripted rows in the same tab render normally with the standard renderer
5. Switch between tabs — no regressions on Table, Tree, List, Paged, etc.
6. From BeanShell console in VirtualDesktop: call factory.create("demo2", newXml) on a live grid to verify runtime
registration works

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

### Note: GridRow-per-row memory model and the escape hatches

`DefaultGridModel` stores one `GridRow` per data row — but the situation is better
than it might look, for a few reasons worth unpacking.

**The memory math is real but bounded.** Each `GridRow` carries two `HashMap` instances
(data + tags) plus a handful of primitive fields. At 5 columns, a LinkedHashMap is
roughly 250–350 bytes of overhead. So:

- 1,000 rows ≈ 300KB — trivial
- 100,000 rows ≈ 30MB — manageable on a desktop
- 1,000,000 rows — this is where it legitimately hurts

**The rendering is already fine.** SmartGrid only holds ~20 live JPanels regardless of
row count. The bottleneck is heap, not render performance — `getRow(i)` called 20 times
per scroll event is 20 HashMap lookups, which is noise.

**The design already has the escape hatch.** `GridModel` is an interface.
`DefaultGridModel` is the convenient out-of-the-box implementation for moderate
datasets. For a dataset of 500,000 domain objects you already have in memory, you
implement `GridModel` directly against your own data structure and never create a
GridRow at all — or create them transiently in `getRow(int index)` for just the 20
visible rows:

```java
public class EmployeeGridModel implements GridModel {
    private final List<Employee> employees; // your domain objects

    @Override
    public GridRow getRow(int index) {
        Employee e = employees.get(index);
        return new GridRow()           // created, used for ~1 paint cycle, GC'd
            .put("name",   e.getName())
            .put("salary", e.getSalary())
            .setSourceObject(e);
    }

    @Override
    public int getRowCount() { return employees.size(); }
    // ...
}
```

20 ephemeral GridRows per scroll event live entirely in Eden space and never survive a
minor GC — which is exactly the fast path the JVM is built for.

**The one real gap** is tree state: `isExpanded` and `isHasChildren` live on GridRow,
so if GridRow is transient, the model needs to hold expansion state separately (a
`Set<Integer>` of expanded row indices, for example). That is a solvable design
question for whenever a custom model needs tree support.

**The short verdict:** for `DefaultGridModel` as the simple case, the current design is
fine and will stay fine into the tens of thousands of rows on a desktop. For domain
objects you already own, `GridModel` is already the right abstraction — `sourceObject`
is the pointer back. The lazy-loading model planned in this phase (virtual total count,
fetch-on-demand pages) is the definitive answer for true million-row cases and sidesteps
the GridRow-per-row question entirely by only ever materializing a page at a time.

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

## Phase 13 — Filter Search Cache with Configurable Row-Count Threshold

**Why**: On every keystroke, the filter predicate runs against all visible rows, and
for each row it calls `val.toString()` on every column value. For salary that's
`Integer.toString(86503)` → a fresh `"86503"` String, then `.toLowerCase()` (another
allocation since it has no uppercase, so actually the JVM may return the same
reference — but `.contains()` is still called). Across 1000 rows × 5 columns that's
up to 5,000 short-lived String objects per keystroke.

The reason it feels fast despite that is that the JVM is specifically designed for
this pattern. Those 5,000 strings are all allocated in Eden space (the young
generation), never promoted to the old generation because they die before the next
minor GC, and a minor GC on a modern JVM collects Eden in a few milliseconds. So
you're not actually putting GC "through its paces" in the damaging sense — you're
squarely in the fast path that generational GC was built for.

That said, the instinct identifies the right future concern. At, say, 100,000 rows
the math becomes 500,000 allocations per keystroke, and the minor GC pauses start
becoming noticeable. The standard mitigation is a **pre-computed search cache** — a
`String[][]` (or `Map<GridRow, String[]>`) storing the lower-cased `toString()` of
every cell value, built once when the model loads and invalidated on data change. The
filter predicate then compares against cached strings rather than calling `toString()`
on every check. This is exactly what most production grid components do.

### Threshold design

A threshold absolutely makes sense. The cache has two costs of its own: the up-front
build time (O(n) over all rows and columns) and the ongoing memory footprint (roughly
`rows × columns × average-string-length` bytes of heap). For a 100-row dataset those
costs are larger than the GC savings — you'd be trading a few hundred transient String
allocations for permanent heap occupancy and a build step. For a 100,000-row dataset
the cache pays for itself on the very first keystroke.

A user-configurable threshold (with a sensible default, say 10,000 rows) means the
simple path is always used for casual/small grids, and the cache only activates when
the row count crosses into territory where GC pressure becomes real. There's one
complication worth noting: if rows are added dynamically (lazy loading, tree expand),
the count can cross the threshold mid-session, so the implementation needs to build
the cache on-demand the first time a filter is applied above threshold, not just at
construction time.

### Implementation

**`SmartGrid.java`**
- Add `private int filterCacheThreshold = 10_000;` (default)
- Add `public void setFilterCacheThreshold(int n)` API
- Before applying a filter in `reapplyColumnFilter()` / `applyComposedFilter()`:
  if `model.getRowCount() > filterCacheThreshold` and cache is not built, build it
- Cache: `String[][] searchCache` — `[rowIndex][colIndex]` = `val.toString().toLowerCase()`
- Invalidate cache on `modelReset()` (sort, filter, data change)
- When cache is active, filter predicate reads `searchCache[i][j]` instead of
  calling `row.get(key).toString().toLowerCase()`

**`DefaultGridModel.java`**
- No changes required — the cache lives on the SmartGrid (view) side, not the model

### Expected outcome
- Below threshold: behaviour unchanged; simple `toString()` path
- Above threshold: single O(n) cache build on first filter keystroke;
  subsequent keystrokes do array lookups only — no String allocation per row
- Threshold is user-adjustable via `grid.setFilterCacheThreshold(n)` to tune per use case

---

## Phase 17 — Column Freezing / Pinning

**Why**: Frozen columns are a universal power-user feature for wide datasets — keep
the identifying column (name, ID) visible while scrolling through many data columns.

### Architecture: split panes

Frozen columns require two side-by-side panes inside SmartGrid — a
non-horizontally-scrolling left pane containing the frozen columns, and the existing
right pane for everything else. Vertical scroll must be synchronized between them.
This is how every professional grid (Excel, AG Grid, Handsontable) does it.

```
SmartGrid (BorderLayout)
  ├── NORTH:  toolbar
  ├── WEST:   frozen pane  (JScrollPane: H=NEVER, V=AS_NEEDED)
  │             └── FrozenCanvas (VirtualCanvas variant)
  ├── CENTER: scrolling pane  (existing JScrollPane)
  │             └── VirtualCanvas
  └── SOUTH:  footer / pagination
```

### Where the complexity lives

The current architecture helps in three ways: null layout with absolute positioning,
`columnWidths[]` already split-ready (just divide it at the freeze boundary), and row
slots already typed-pool-dispatched. The hard parts are:

**1. Vertical scroll sync** — two independent viewports, so you need to listen to each
viewport's vertical position and push it to the other. This works but requires care:
the listener fires from the model change, which means you can create a feedback loop
(A updates B, B updates A). A guard flag (`syncingScroll`) breaks it — same pattern
as the `refreshing` guard added for the FlatLaf re-entrancy fix.

**2. Row slot split** — currently one `slots[]` array per visible row. You'd need
`frozenSlots[]` and `scrollSlots[]` in parallel. Each frozen slot is a
`StandardRowPanel` constructed with only the frozen `ColumnDef` list; each scroll slot
gets the remainder. `StandardRowPanel` already accepts `List<ColumnDef>` so this is
just passing a sublist.

**3. Header / strip placement** — the frozen header panel goes above the frozen pane;
the scrolling header stays as `columnHeaderView` of the scrolling pane. The checkbox
and tree zone strips naturally belong on the frozen side (they're already the leftmost
columns). The corner fill trick becomes the frozen header's right edge.

**4. Column width computation** — `computeColumnWidths` runs separately for each pane
against its own viewport width. Frozen pane: `vpWidth = sum of frozen column preferred
widths` (fixed, no scaling). Scrolling pane: `vpWidth = viewport width of scrolling
pane`.

### What comes for free

The vertical virtualization (`firstRow = scrollY / rowHeight`) is identical in both
panes. The strip management, pool dispatch, `resolveRowType()`, and `Selectable`
interface all apply unchanged. The freeze point is just an index into
`model.getColumns()`.

### Complexity estimate

Roughly 1.5× the complexity of the checkbox strip (Phase 16): new fields, layout
restructuring, vertical sync, two slot arrays, header split. Significant but not
architectural — nothing needs to be un-done, only extended. Phase 12 (persistent header
panels) would make the header split considerably cleaner; sequencing that first would
pay off.

### Key design question to resolve before implementing

Should the freeze boundary be **settable at runtime** (drag to resize or toggle), or
**construction-time only**? Runtime freeze is meaningfully more complex because it
requires re-homing slots between the two canvases mid-session.

---

## Phase 18 — Scroll Repaint Quality (Eliminate the Canvas Flash)

Yes, it can be prevented entirely — but it requires addressing two distinct causes, and the current canvas-background fix is a mitigation rather than a cure.

**Cause 1: Blit scroll mode (the dominant one)**

JScrollPane's default scroll mode is `JViewport.BLIT_SCROLL_MODE`. When you scroll, Swing uses `copyArea()` to shift the existing pixel content and then only repaints the newly exposed strip at the boundary. That strip is filled with the canvas background before the row component covers it — that's the flash at the top/bottom boundary during scroll. Switching to `JViewport.BACKINGSTORE_SCROLL_MODE` or `JViewport.SIMPLE_SCROLL_MODE` forces a full repaint from an off-screen buffer on every scroll event, eliminating the exposed strip entirely. For a virtual canvas with only ~20 live components this is cheap, and on any hardware made in the last decade the difference is imperceptible.

**Cause 2: `canvas.remove()` / `canvas.add()` during slot type swap**

When a slot changes from one row type to another (or after `reallocateSlots()`), SmartGrid removes the old component and adds the new one. The `remove()` marks that area dirty. Between `remove()` and the slot being repositioned and bound, the canvas background is briefly exposed. The fix here is architectural: replace `add()`/`remove()` with `setVisible(true/false)` — keep all possible typed-pool components permanently attached to the canvas, just hide or show them as needed. No gap is ever created.

**For commercial quality you'd want both.** The scroll-mode change is a one-liner inside the SmartGrid constructor. The setVisible refactor is non-trivial — it means the canvas always contains the full union of all typed pools' visible slots rather than dynamically swapping them — but it also has the side benefit of eliminating the `canvas.add()` peer-creation overhead during rapid type changes.

The canvas background color fix currently in place is the right "livable" solution: it makes any residual flash the same color as the rows, so it's invisible in practice even though the flash still technically happens.

---

## Notes on Testing Strategy

All phases are verified interactively via `SmartGridDemo` or a new demo variant.
No automated tests exist yet; add JUnit coverage once the model API stabilizes:
- `DefaultGridModel`: sort/filter/visible-row correctness
- `ComponentPool`: checkout/release cycle, pool size bounds
- `GridRow`: fluent builder, tag access
