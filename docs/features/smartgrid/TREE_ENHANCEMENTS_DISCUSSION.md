# SmartGrid — Tree Enhancements Discussion

**Date:** 2026-05-10  
**Participants:** User (kansasSamurai), Claude (Sonnet 4.6)  
**Purpose:** Design discussion for Phase 6 — Tree / Hierarchy enhancements to SmartGrid.  
**Roadmap reference:** `docs/features/smartgrid/ROADMAP.md` — Phase 6  
**Status:** Discussion complete — pinned for reference before implementation begins.

---

*This document is maintained as a living record of the design discussion.
Entries are added after each meaningful exchange.*

## Contents

- [Topic 1 — How is the tree currently defined? Is grouping dynamic?](#topic-1)
- [Topic 2 — Selection semantics for group headers; missing source reference](#topic-2)
- [Topic 3 — Dedicated tree zone; spanning header row rendering](#topic-3)

---

## Topic 1 — How is the tree currently defined? Is grouping dynamic?

**User question:** The demo tree groups employees by department. Is that grouping
built into the data model we feed the grid, or did we tell the grid to group by
the `department` property? Users will understandably want a dynamic approach —
start with a flat grid and choose the grouping at runtime.

**Current state — pre-baked, data-model-driven:**

The tree is entirely constructed by the code that builds the model, not by the
grid. In `SmartGridDemo.buildTreeTab()` every row is manually created with its
`depth`, `hasChildren`, and `parentId` flags set before the grid ever sees the
data. The grid/tree component has zero awareness of "department" — it simply reads
those flags and renders indentation and expand/collapse controls accordingly.
If a flat list of employees with no depth flags were supplied, the grid would
render a plain flat table. The grouping logic lives entirely in the model-building
code, not in the grid.

**The gap — dynamic row grouping:**

Dynamic row grouping — where a user starts with a flat 1,000-row table and chooses
"group by Department" at runtime — requires the grid to:

1. Scan the flat data and identify the distinct values in the chosen column
2. Synthesize group header rows that do not exist in the original dataset
3. Arrange matching child rows beneath each header with `depth = 1`
4. Recompute this structure when the grouping column changes
5. Decide what to show in the other columns of a group header row
   (count? aggregate? blank? — a design decision in itself)
6. Handle interaction with sort (sort groups? sort within groups?) and filter
   (hide groups with no matching children? show empty groups?)

This is a meaningfully different and more complex feature than what exists today.

---

## Topic 2 — Selection semantics for group headers; missing source reference

**User question:** Right now our selection model allows selecting group header rows
as if they are no different from individual record rows. What does that mean to the
user? What do you get programmatically if you ask for all selected rows? And doesn't
each row need a reference back to the underlying data model — especially for
editability?

### Part A — What does selecting a group header mean to the user?

Currently, nothing coherent. The selection model treats "Engineering" identically to
"Employee 1" — it just highlights the row. But the user's mental model of clicking a
group header is almost certainly one of three things:

- **Select all children** (select the group as a unit)
- **Expand / collapse** (which we already do on click, creating an immediate conflict
  since clicking also selects)
- **Do nothing to the selection** (group headers are not selectable)

All three are legitimate and none of them match the current behavior of "select just
this synthetic row." The current behavior is technically functional but semantically
undefined.

### Part B — What do you get programmatically?

If you iterate selected indices and call `model.getRow(i)`, you get the `GridRow`
object that was created for that group header. For "Engineering" that is a `GridRow`
with `name="Engineering"`, `role="13 employees"`, `salary=""`, `depth=0`,
`hasChildren=true`. It is a **synthetic row** — hand-crafted by the developer who
built the demo. It does not represent an actual domain entity. In a real application
backed by a database there is no "Engineering" record to return — the caller receives
a fabricated object with display strings and no connection to any real data.

### Part C — The missing source reference (and its implications for editability)

This is the most important structural gap. Currently `GridRow` is simultaneously the
data container *and* the view-model row. Display values live in `data` as strings or
raw values, but there is no typed reference back to the original domain object.

In a real application:
```java
Employee emp = employeeService.findById(42);
GridRow row = new GridRow()
    .put("name",   emp.getName())
    .put("salary", emp.getSalary());
// Missing: row.setSource(emp);
```

When the user selects row 42 and code asks "what did they select?", it gets display
values — not the `Employee` object needed to open an edit dialog, persist a change,
or call a service. The only recovery is to round-trip through the display string back
to an ID and re-fetch — fragile and wasteful.

For **inline editability** (Phase 11): editing would mutate `GridRow.data` but with
no source reference that mutation is isolated to the view. Nothing upstream knows the
salary changed.

### Proposed fix — `sourceObject` field on `GridRow`

Add `private Object sourceObject;` (typed as `Object`, cast by the caller who knows
the domain type) with `getSource()` / `setSource(Object)` accessors.

```java
Employee emp = employeeService.findById(42);
GridRow row = new GridRow()
    .put("name",   emp.getName())
    .put("salary", emp.getSalary())
    .setSource(emp);                    // typed reference preserved

// Later, when the user selects and acts:
Employee selected = (Employee) selectedRow.getSource();
```

Group header rows synthesised by dynamic grouping would carry `sourceObject = null`
(or a dedicated grouping marker object), which also gives a clean answer to the
selection question: if `row.getSource() == null`, it is a group header — apply
different selection semantics accordingly.

### Open design questions from this topic

1. Should clicking a group header row **select all children**, **select only the
   header**, or **leave selection unchanged** (expand/collapse only)?
2. Should group headers be **selectable at all** — or should they be visually
   distinct and click-through to expand only?
3. When `getSelectedRows()` is called, should it return **only leaf rows** (filtering
   out headers), **only headers**, or **everything** with the caller responsible for
   filtering by `getSource() == null`?
4. For dynamic grouping (Topic 1), what is the `sourceObject` of a synthesised group
   header — null, a `GroupKey` wrapper, or the first child row?

### Resolution — group headers are not selectable

The `ListSelectionModel` does not need to change. The tension dissolves the moment
group header rows are made non-selectable. Because group headers can never enter the
selection model, everything returned from the selection API is guaranteed to be a leaf
row — no filtering, no semantic ambiguity, no "what did the user mean by selecting
Engineering?"

**Implementation:** a single conditional in `StandardRowPanel.mousePressed()`:
```java
// Only update selection for leaf rows — group headers expand/collapse only
if (sm != null && !capturedRow.isHasChildren()) {
    // existing selection logic unchanged
}
```

This matches the mental model users already have from `JTree`, AG-Grid, and every
other professional tree component: clicking a group node expands or collapses it; it
does not select it as a data item. Making group headers non-selectable is the correct
affordance, not a limitation.

**"Select all children"** — the only real alternative — requires knowing which
children are visible, expanding the group if collapsed, then recomputing indices after
the visible row list shifts. This is genuinely messy and the path that *would*
complicate `ListSelectionModel` usage. It is deferred to a right-click context menu
action if ever needed.

**`sourceObject`** is orthogonal and should be added to `GridRow` regardless — it is
needed for editability whether or not the row is in a tree context.

### Clarification — are group header indices technically eligible for ListSelectionModel?

Yes. Group header rows occupy real integer indices in the visible row list —
`DefaultGridModel.getVisibleRows()` includes them in sequence alongside leaf rows.
The `ListSelectionModel` has no concept of row type; it simply stores integers.
Index 0 could be "Engineering" and index 1 could be "Employee 1" — both are valid
indices the model could store.

The guard is added **at the UI interaction layer only** — in
`StandardRowPanel.mousePressed()`. When the user clicks, the check
`capturedRow.isHasChildren()` bails out before calling
`selectionModel.setSelectionInterval()`. The model never receives the call and the
group header index never enters it.

**Implication:** programmatic code that calls `selectionModel.addSelectionInterval(0, 0)`
directly — where index 0 is a group header — bypasses the guard entirely. Likewise,
any future keyboard navigation would need the same guard independently.

**Future hardening option:** a custom `ListSelectionModel` subclass that validates
each index against the model before adding it — calling `model.getRow(i).isGroupHeader()`
and rejecting group header indices. This makes the guard enforceable from any code
path, not just mouse clicks. Architecturally correct but deferred as over-engineering
for the current use cases.

### Decision — add `isGroupHeader` boolean to GridRow

A dedicated `isGroupHeader` field is preferred over inferring from `isHasChildren()`.
The two concepts are independent:

- `isHasChildren()` — structural: this row has child rows in the tree
- `isGroupHeader` — semantic: this row is a synthesized header with no underlying
  domain entity

They overlap in the demo but diverge in real hierarchies. A manager who is a real
employee (`sourceObject` set, selectable, editable) with direct reports would have
`isHasChildren() == true` but `isGroupHeader == false`. Using `isHasChildren()` as
the selection guard would incorrectly block selection for that manager.

**Pros:**
- Explicit intent — not inferred from structure
- Decouples structural tree properties from grid-component semantics
- Natural companion to `sourceObject`: `isGroupHeader == true` implies
  `sourceObject == null` (no real entity backing the row)
- Guard logic reads `!capturedRow.isGroupHeader()` — more intentional and
  future-proof than `!capturedRow.isHasChildren()`
- Fits naturally alongside existing explicit booleans on `GridRow`
  (`selected`, `processing`, `expanded`, `hasChildren`)
- Dynamic grouping synthesises rows explicitly flagged — no inference at call sites

**Cost:** one boolean per row — negligible (1KB for 1,000 rows, 1MB for 1,000,000).

**Discipline cost:** constructors of group headers must set the flag — low risk since
group headers are always explicitly constructed, never accidentally synthesised.

---

## Topic 3 — Dedicated tree zone; spanning header row rendering

**User question:** The vision is that the main distinction between a flat grid and a
tree grid is that a tree includes non-data "columns" on the left for rendering the
tree visually. The demo does not implement it that way. Additionally, header rows
currently put synthesized data into existing columns, but header row rendering might
need to support a renderer that combines columns — up to the extreme where the entire
row (other than the tree zone) is one "cell."

### Part A — Dedicated tree zone vs. padding the first data column

The current demo uses the "fake it" approach: indent and expand icon are a text
prefix (`"▶ "` / `"▼ "`) and left-padding baked into the first data column's cell.
This is aesthetically wrong and structurally fragile:
- The first data column's content width shrinks with tree depth
- Column alignment is complicated by depth-dependent padding
- Tree chrome is tangled into data rendering logic

**The clean design: a tree zone** — a fixed-width panel prepended to every row,
outside the column data grid entirely. It renders tree chrome (indent, expand/collapse
icon, optional connector lines). Data columns always start at the same x offset.
The `int[] columnWidths` array continues to describe only data columns; the tree zone
is separate fixed-width real estate.

```
Leaf row:         [tree zone 48px] [col0][col1][col2][col3][col4]
Group header row: [tree zone 48px] [     full-width header cell     ]
```

**Tree zone width:** must be fixed so data columns have a stable starting x. For
variable-depth trees this means choosing a maximum — e.g., `maxDepth × 16px + 24px`
for the icon. For the common 2–3 level case, 48–64px is comfortable.

### Part B — Spanning / single-cell rendering for group header rows

The current demo puts synthesized strings into individual column cells for group
headers ("Engineering" in name, "13 employees" in role, "" in salary). This is a
hack — it looks wrong when column widths are unequal since the content floats in
arbitrary proportions.

**The right design:** group header rows use an entirely different renderer
(`GroupHeaderRowPanel`) that ignores `columnWidths[]` for its content area and renders
the full data width as a single unified cell. This is where the `GridComponentFactory`
(Phase 9) and `isGroupHeader` flag compose: when the factory sees
`row.isGroupHeader() == true`, it returns `GroupHeaderRowPanel` instead of
`StandardRowPanel`.

### Architecture — how the two parts compose

- **Tree zone**: fixed-width panel, part of every row's layout, renders tree chrome
  for all row types regardless of whether it is a leaf or header
- **Data area**: right of the tree zone; rendered differently by row type:
  - `StandardRowPanel` (leaf): data cells laid out by `columnWidths[]`
  - `GroupHeaderRowPanel` (header): single full-width cell, ignores column grid

The `GridComponentFactory` (Phase 9) is the selection mechanism. The `isGroupHeader`
flag (decided in Topic 2) is the discriminator.

### Open design questions from this topic

1. Should the tree zone be a separate component prepended to the row panel, or a
   fixed-width first entry in the existing null-layout row — i.e., column index -1?
2. Should connector lines (vertical guide lines showing tree structure) be in scope
   for the initial tree zone implementation, or deferred?
3. ~~For `GroupHeaderRowPanel`, who supplies the content~~ — **resolved below.**
4. ~~At what point does the tree zone activate~~ — **resolved below.**

### Resolution — tree zone activation

The framing of "activated vs. zero-width" was imprecise — there is no meaningful
operational difference between the two. Both are invisible to the user.

The real question was structural: is the tree zone a **permanent part of every row
panel's layout** (always present, width derived from data), or **dynamically added
and removed** as tree rows appear and disappear?

**Resolution: always present, width derived from data.** If no rows have `depth > 0`,
the tree zone width is zero and costs nothing visible. If tree rows appear later
(dynamic grouping, lazy loading), the width grows and all pooled panels already have
the structural slot — no pool rebuild required. This is the same "allocate once,
update in place" philosophy applied to `columnWidths`.

The "activation" question dissolves entirely. A flat table is simply a tree table
where every row has `depth = 0` and the tree zone renders at zero width.

### Resolution — group header content

Two scenarios, same rendering contract.

**Dynamic grouping** (grid synthesizes headers): the grid has full knowledge of the
grouping column, the distinct value, and the child count. It generates the label via
a configurable template function and writes it to a reserved key on the synthesized
`GridRow`:

```java
@FunctionalInterface
public interface GroupHeaderFormatter {
    String format(String columnHeader, Object groupValue, int childCount);
}

// Default: "Department: Engineering  (13)"
(colHeader, value, count) -> colHeader + ": " + value + "  (" + count + ")"
```

The formatter is user-overridable. The result is stored internally as `"_groupLabel"`.

**Pre-baked grouping** (developer constructs headers manually): the developer is
responsible for the content. They either set `"_groupLabel"` explicitly or the
`GroupHeaderRowPanel` falls back to the first data column's value.

**Rendering contract:** `GroupHeaderRowPanel` renders `row.get("_groupLabel")`,
falling back to the first column value if absent. By the time the renderer sees the
row it does not know or care whether the label was synthesized or hand-crafted —
same rendering mechanism for both modes. This is the same "store raw, format for
display" philosophy applied to salary.

**Nuance:** `_groupLabel` is a synthetic key that should not appear when callers
iterate `row.getData()` for other purposes. A dedicated `row.getGroupLabel()` accessor
(backed by the same key internally) is cleaner than a magic string at call sites.

---
