# SmartGrid — Design Reference

Architecture, internals, and behavioral contracts for the SmartGrid virtual-scroll
component. Open this when you want to understand *why it works the way it does* or
when onboarding a contributor. For what is planned or what has been completed, see
[ROADMAP.md](ROADMAP.md).

---

## Foundational Principle: Model Layer vs. Rendering Layer

`GridRow` is the canonical source of truth. The row renderer (StandardRowPanel,
FeaturedRowPanel, LogRowPanel, or any custom type) is purely a visual
interpretation of it. These two layers are deliberately decoupled, and that
separation is one of SmartGrid's architectural strengths.

**Practical consequence:** operations that consume data — copy, export, search,
aggregation, footer totals — always operate on the model layer and are completely
indifferent to how a row happens to render. A `FeaturedRowPanel` row and a
`StandardRowPanel` row backed by the same `GridRow` are identical from the
perspective of any of these operations. The spanning layout, the custom painting,
the embedded components — none of it affects what gets copied or exported.

**The `Copyable` interface as a diagnostic signal:** this interface exists as a
narrow escape hatch for rows where data is not fully represented in the `GridRow`
— computed or composed values that live in the renderer rather than the model.
If `Copyable` is needed frequently across many row types, that is a design signal:
data that belongs in `GridRow` has drifted into the renderer layer. The interface
being rarely needed is the healthy outcome.

**Corollary for custom renderer authors:** put all meaningful data in `GridRow`
and let the renderer be purely presentational. Operations like copy and export
will then work correctly with zero additional implementation.

---

## Header and Footer Zone Architecture

### Fixed Stacking Order

SmartGrid uses an opinionated, fixed vertical layout. The zones from top to bottom are:

```
[ Header Zone         ]  addHeaderRow() rows, in call order
[ Toolbar             ]  getToolbar() — always at index 0 within the header zone
[ Column Header       ]  SmartGrid-managed, always present
[ Filter Row          ]  SmartGrid-managed, optional (setColumnFiltersVisible)
[ Canvas              ]  SmartGrid-managed — the virtual scroll area
[ Column Footer       ]  SmartGrid-managed, optional (setFooterRenderer)
[ Pagination          ]  SmartGrid-managed, optional (setPageSize)
[ addFooterRow() zone ]  user rows, in call order
[ Summary Row         ]  SmartGrid-managed, always last, always present
```

SmartGrid owns and sequences everything in the middle. The user owns two zones at
the extremes.

### Header Zone

`addHeaderRow(JComponent)` appends a row below the toolbar and above the column
header. Rows appear in the order they are added. The toolbar (returned by
`getToolbar()`) is always placed at index 0 within the header zone so it remains
topmost regardless of call order.

The entire header zone lives in a `northPanel` (BoxLayout.Y_AXIS) that is created
lazily on the first call to `getToolbar()` or `addHeaderRow()`. Callers that never
use either method pay no overhead.

### Footer Zone (user rows)

`addFooterRow(JComponent)` appends a row below the pagination bar and above the
summary row. Rows appear in the order they are added.

**Important:** user-added footer rows are stored in a persistent `extraFooterRows`
list and re-applied every time `rebuildSouthPanel()` runs. `rebuildSouthPanel()` is
called whenever `setPageSize()` or `setFooterRenderer()` changes — if you add a
row via `addFooterRow()` before calling those methods, your row will survive the
rebuild.

### Summary Row

The summary row is always present and always the last visible element in the grid.
It shows:
- **Left:** "Showing X rows" or "Showing X of Y rows" when a filter is active
  (X = visible rows, Y = total rows via `model.getTotalRowCount()`)
- **Right:** "N row(s) selected"

Both labels update automatically via listeners wired at construction time. The
summary row is an opinionated design decision: it provides a de-facto status
contract that consumers can rely on, and it visually grounds the grid in any
layout. There is no opt-out in the current version.

### Column Footer vs. User Footer Zone

The **column footer** (`setFooterRenderer`) is a column-aligned aggregate row
rendered by `FooterCellRenderer`. Each cell lines up with its data column (same
null-layout and `columnWidths[]` array as the data rows). It is distinct from
the user footer zone — it sits above pagination and is SmartGrid-managed.

The **user footer zone** (`addFooterRow`) accepts any `JComponent` and is not
column-aligned. Use it for free-form rows like status bars, action button panels,
or additional summary text.

### What Lives Outside SmartGrid

Page-level chrome — title, description, feature badges, page background — belongs
in the host container (e.g., `buildPage()` in the demo). SmartGrid's header and
footer zones are for grid-intrinsic content: search fields, action buttons that
operate on the grid, and status displays tied to the grid's data state.

---

## How the Virtual Canvas Works

### The Core Illusion

The fundamental trick is that SmartGrid **lies to the JScrollPane** about its size.
If you have 150 log rows at 64px each, `VirtualCanvas.getPreferredSize()` returns a
height of 9,600px. The JScrollPane dutifully creates a 9,600px scrollbar range. But
the canvas never actually contains 150 components — it contains roughly
**viewport height ÷ rowHeight + 2**, typically 12–18 components regardless of whether
you have 150 rows or 150,000.

The "+2" is a one-row buffer at the top and bottom so there is never a visible gap at
a boundary while scrolling.

### What "Slots" Are

A **slot** is one of those ~16 live component positions. `slots[]` is just an array of
JComponents currently attached to the canvas. Each slot is a physical JPanel that gets
repositioned and repopulated as you scroll — it is never created or destroyed during
normal operation.

The parallel array `slotTypes[]` tracks what *kind* of component each slot currently
holds (`null` for a default StandardRowPanel, `"log-row"` for a LogRowPanel,
`"group-header"` for a GroupHeaderRowPanel, etc.).

### The Scroll Cycle

When you move the scrollbar, `doRefresh()` runs:

1. **Compute `firstRow`** from the viewport's Y position: `scrollY / rowHeight`. This
   tells us which model row should appear at the top.
2. **For each slot index `i`**, the target model row is `firstRow + i`.
3. **Type check** — if the required row type (from the `fnd-type` tag) does not match
   `slotTypes[i]`, swap the component. This is a `releaseSlot` (hide + return to pool)
   followed by a `checkoutSlot` (borrow from pool, attach to canvas if new).
4. **`setBounds()`** — position the slot at the *absolute* y-coordinate:
   `rowIdx * rowHeight`. The slot does not move relative to the viewport — it is placed
   at where it would be in the full 9,600px virtual space. The viewport window just
   happens to be positioned over that area.
5. **`bind(row, modelIdx)`** — push the actual data into the component.
6. **`setVisible(true/false)`** — slots beyond the visible range are hidden rather than
   removed.
7. **`canvas.repaint()`** — one repaint call after all slots are positioned and bound.

### Why setBounds at Absolute Coordinates Works

This is the elegant part. The canvas is 9,600px tall (in the JScrollPane's eyes). The
viewport is, say, 512px tall, currently scrolled to Y=1,280. `firstRow` = 1280/64 = 20.
Slot 0 gets `setBounds(0, 1280, width, 64)` — row 20 at its true position. Slot 1 gets
`setBounds(0, 1344, width, 64)` — row 21, and so on.

The viewport clips to Y=1280..1792, so it shows exactly those slots. When you scroll
down 64px, `firstRow` becomes 21, and the slot that was showing row 20 is repositioned
to show row 36 (or whatever is now needed at the bottom). The scrollbar did not move
components — it moved the *window* over a canvas where components are always at their
correct logical positions.

### The Component Pool

Each row type has its own `ComponentPool`. A pool is a simple queue of idle components.
`checkout()` pops one (calling `prepareForReuse()` on it), `release()` pushes it back.
The pool grows lazily — if the queue is empty, the factory supplier creates a new
instance. In steady-state scrolling the pool never grows because components are released
and checked out in a tight cycle (scroll one row: one slot at the boundary gets
released, one gets checked out).

### show/hide vs. insert/remove — the Phase 18 Change

**Before Phase 18**, a type swap did:

```
canvas.remove(oldComponent)   // marks the slot's area as dirty (shows canvas bg)
canvas.add(newComponent)      // adds with stale/zero bounds, not yet painted
setBounds(...)                // now positioned
bind(...)                     // now populated
```

The dirty region from `remove()` could paint canvas background (white on light theme,
visible against dark rows) before `newComponent` covered it.

**After Phase 18**, a type swap does:

```
setVisible(false)             // hidden, stays in canvas, no gap created
pool.release(oldComponent)    // available for reuse but physically still in canvas
checkoutSlot(newPool)         // borrows component; if previously used it is already
                              //   in the canvas at a hidden position
setBounds(...)                // repositioned
bind(...)                     // populated
setVisible(true)              // already present in doRefresh(), unchanged
```

No component is ever ejected from the canvas after its first insertion. The canvas
always contains the full complement of pool components that have ever been activated —
they are either visible (currently showing a row) or invisible (idle in their pool).
`checkoutSlot` enforces the "add once" contract via `c.getParent() != canvas`.

The scroll mode change (`JViewport.SIMPLE_SCROLL_MODE`) is the other half: instead of
bitblt-scrolling the existing pixels and repainting only the exposed strip (which could
show canvas background), the entire viewport is repainted from scratch on every scroll
event. With only ~16 live components this is trivially fast, and it means there is
never an "exposed strip waiting to be painted."

Together these make the component lifecycle:

> **created → added to canvas once → shown/hidden indefinitely → never removed**

---

## Large Dataset Considerations

### Two Layers, Two Different Answers

SmartGrid's scalability story depends entirely on which layer you are asking about.

**The rendering layer is already large-dataset-safe.** The virtual canvas maintains
roughly `viewportHeight / rowHeight + 2` live slot components regardless of dataset
size — typically 12–18 components. Scrolling through 150 rows or 150,000 rows
produces the same number of `bind()` calls per scroll event and the same component
memory footprint. There is nothing to improve here; the architecture is intentionally
O(1) with respect to row count.

**The model layer scales linearly with row count.** `DefaultGridModel` holds one
`GridRow` object in memory for every record, always. A `GridRow` is essentially a
small `HashMap` — approximately 300–500 bytes each when populated with typical
column values. The table below shows what that means in practice:

| Row count | Approximate model memory |
|-----------|--------------------------|
| 1,000     | 0.3–0.5 MB               |
| 50,000    | 15–25 MB                 |
| 500,000   | 150–250 MB               |
| 1,000,000 | 300–500 MB               |

At tens of thousands of rows `DefaultGridModel` is fine. At hundreds of thousands
it becomes a concern. At millions it is the wrong tool.

### Why GridRow Is Not the Problem

The natural instinct is to see `GridRow` as the cost that must be paid for real
Swing components — the richer model you accept in exchange for richer rendering.
That framing is partly true but misses an important nuance.

`GridRow` is richer than a flat `TableModel` for three specific reasons:

1. **Named keys** (`row.get("status")`) rather than column index (`getValueAt(r, 3)`)
2. **Typed values** — a cell can hold a `DayData`, `Color`, or any domain object, not
   just a displayable primitive
3. **Type tags** (`fnd-type`) — this is the load-bearing difference; it lets SmartGrid
   dispatch a row index to completely different component types
   (`StandardRowPanel` vs. `CalendarWeekRowPanel` vs. `GroupHeaderRowPanel`)

JTable's `TableModel` is minimal because its rubber-stamp renderer only needs a
value for a moment, then discards it. But `TableModel` has exactly the same O(n)
memory problem — `DefaultTableModel` stores a `Vector` of `Vector` rows. The
interface happens to be simpler, but the problem is identical.

More importantly: `GridRow` as an object actually *helps* lazy loading. Because
`bind(GridRow, int)` is the entire contract between the model and the renderer,
a lazy model can construct a `GridRow` ephemerally for that call — populating it
from a database cursor or network response — and let it be garbage-collected
immediately after `bind()` returns. A raw `getValueAt(row, col)` interface would
require the lazy model to keep the fetched record alive across N separate column
calls for the same row.

### The Solution: A Lazy GridModel Implementation

SmartGrid already depends on `GridModel` as an interface (or can be extracted to
one cleanly). A `LazyGridModel` or `VirtualGridModel` implementation would present
the same API surface as `DefaultGridModel` while only materializing rows on demand:

```java
// Contract
int getTotalRowCount();              // returns N from a DB count query, for example
GridRow getRow(int index);          // fetches only this record, builds a GridRow, returns it

// What the virtual canvas calls per scroll event
for each visible slot i:
    GridRow row = model.getRow(firstRow + i);  // only ~16 calls, not N
    slot.bind(row, firstRow + i);
```

With this model, the memory footprint becomes O(1) at the model layer to match the
already-O(1) rendering layer. The rendering architecture requires zero changes.

The implementation challenge shifts to the backing store: it must support efficient
random access by index (SQL `OFFSET`/`LIMIT`, or an indexed cache). Sequential-only
sources (streams, message queues) need a windowed buffer to simulate random access.

### The GridRow Object Lifecycle Under Lazy Loading

Under eager loading: GridRow objects live forever in `DefaultGridModel`.

Under lazy loading: GridRow objects are created in `getRow()`, passed to `bind()`,
and immediately become eligible for GC. GC pressure from short-lived objects is
negligible for JVM generational collectors — these are textbook short-lived allocations
that live and die entirely within the young generation.

Alternatively, a lazy model can maintain a fixed-size `GridRow` cache (e.g., 200
rows around the current scroll position) to avoid re-fetching on rapid scroll-back.
The cache evicts by LRU or simple ring-buffer. This is an implementation detail of
the lazy model; SmartGrid's rendering layer is indifferent to it.

### Comparison to JTable at Scale

JTable's `AbstractTableModel` can be implemented with a lazy backing store — it has
no opinion about how `getValueAt(row, col)` is implemented. This is one genuine
area where JTable's minimalism is an advantage: the interface is so thin that lazy
implementation is natural.

SmartGrid's equivalent is a `LazyGridModel` implementation — slightly more surface
area (named keys, type tags) but structurally the same pattern. The real-component
advantage (live Swing components vs. rubber-stamp painting) is preserved regardless
of which model implementation is used.

### Roadmap Note

GlazedLists integration (roadmap item #19) is the "production answer" to this
problem for datasets that are already in memory but need efficient sorting, filtering,
and change notification without full model rebuilds. For datasets that do not fit in
memory at all, `LazyGridModel` / `VirtualGridModel` is the required path and is not
yet implemented.

---

## Row Height — The Uniform-Height Constraint and Workarounds

### Why All Rows Must Be the Same Height

SmartGrid enforces a single global `rowHeight` for every row in the canvas. This
is not an oversight — it is a load-bearing requirement of the virtualization engine.
All three core scrolling calculations depend on it:

| Calculation | Code |
|---|---|
| Virtual canvas height | `effectiveRows * rowHeight` |
| First visible row index | `scrollY / rowHeight` |
| Visible slot count | `(vpHeight / rowHeight) + 2` |

These are simple integer divisions. If rows could have variable heights, jumping to
"scroll position 500" would require summing all prior row heights — a fundamentally
different (and heavier) data structure. The uniform-height contract is what makes
virtualization cheap.

### What Actually Enforces It

The constraint lives in a single `setBounds()` call in `SmartGrid.java`:

```java
slots[i].setBounds(leadX, rowIdx * rowHeight, totalColWidth, rowHeight);
```

The row panel is force-sized to `rowHeight` before `bind()` is ever called.
`getPreferredSize()` is **never consulted** on row components — so a custom renderer
that returns a different preferred height has no effect. The content is simply clipped
to `rowHeight` pixels.

### Workarounds That Don't Require Variable Height

Because SmartGrid rows are **real live Swing components** (not just paint calls like
JTable), there are two practical workarounds:

**1. Increase global `rowHeight`**
Call `setRowHeight(64)` (or any value). All rows get taller, giving complex renderers
more room. The downside is that *every* row gets taller — simple rows waste the extra
vertical space.

**2. Embed a JScrollPane inside the row**
A row that stays at the standard `rowHeight` can contain a `JScrollPane` wrapping
a tall component (`JTextArea`, `JTextPane`, a nested panel). The user scrolls
*within the cell* rather than the grid revealing more content. Because the row is a
live component, the embedded `JScrollPane` receives real mouse and keyboard events
— scroll, focus, selection — with no extra wiring.

**3. The hybrid**
A slightly taller `rowHeight` (e.g. 64px) combined with an embedded `JScrollPane`
in complex rows gives a content preview at normal scroll speed, while the internal
scrollbar exposes the rest on demand. Simple rows just gain extra padding or a larger
font at no cost.

### The Real Advantage Over JTable

This brainstorming exposes a genuine SmartGrid strength. In JTable, the cell renderer
is a rubber stamp — there is no actual component in the cell to receive events. You
cannot put a real `JScrollPane` in a JTable cell because there is nothing to scroll.
In SmartGrid, the embedded `JScrollPane` is a first-class Swing component: it gets
focus, mouse wheel events, keyboard shortcuts, and accessibility support automatically.
The uniform-height constraint is real, but the workaround space is much richer than
JTable offers.

---

## Key Contracts for Custom Row Components

Any class registered via `grid.registerRowRenderer(fndType, supplier)` must implement
`Recyclable` and should implement `Selectable`:

```java
public interface Recyclable {
    void prepareForReuse();          // reset to blank state; called on pool checkout
    void bind(GridRow row, int idx); // populate with data; called every scroll step
}

public interface Selectable {
    void setSelected(boolean selected); // called after every bind()
}
```

**`prepareForReuse()` must be idempotent and leave the component visually blank.**
It is called before the component is reassigned to a new row — any state from the
previous row must be gone.

**`bind()` must be fast.** It is called on the EDT for every visible slot on every
scroll event. Anything expensive (parsing, network I/O, heavy layout) must be done
upstream, not inside `bind()`.

**Layout inside `bind()`.** Because the canvas uses null layout and positions slots
with `setBounds()`, child components inside a custom row panel must also be positioned
explicitly. Override `doLayout()` to recompute child bounds from `getWidth()` /
`getHeight()` so that resize (JFrame maximize) works correctly without requiring a
`bind()` call.

**`isValidateRoot()`.** If a custom row panel contains a component with its own
internal layout engine (e.g., `JTextPane`), override `isValidateRoot()` to return
`true`. This prevents `revalidate()` calls fired by that component's internal state
changes from cascading up to the VirtualCanvas and interfering with the refresh cycle.

**Document mutation in JTextPane rows.** Build the `DefaultStyledDocument` while it is
*detached* from the `JTextPane`, then install it with a single `setDocument()` call.
Each `insertString()` on an attached document fires a `DocumentEvent` that triggers
the JTextPane's View hierarchy to rebuild — multiplied across 20+ slots and 30+
insertions per JSON row, this causes multi-second scroll delays. A detached document
has no UI listener, so insertions are pure data operations. `setDocument()` installs
the finished document in one shot, triggering exactly one View rebuild per row.

---

## Writing High-Performance bind() Methods

`bind()` is called on the EDT for every visible slot on every scroll event — typically
16–20 calls per scroll step. At 60 fps smooth scrolling that is potentially 1,000+
calls per second. The following practices keep it fast and correct.

### 1. Allocate in the constructor, read in bind()

Every object that does not change per-row belongs in the constructor: `Color`,
`Font`, `SimpleAttributeSet`, `Border`, fixed-width layout constants. `bind()` should
read pre-built state, not create it.

```java
// Constructor — once
private static final Color WARN_BG = new Color(0xFF, 0xF8, 0xDC);
private final SimpleAttributeSet keyAttr = makeAttr(JSON_KEY, true);

// bind() — zero allocation
label.setBackground(WARN_BG);
doc.insertString(0, text, keyAttr);
```

If `bind()` contains `new Color(...)`, `new Font(...)`, or `new SimpleAttributeSet()`,
move those to the constructor.

### 2. Never block the EDT in bind()

`bind()` runs on the Swing Event Dispatch Thread. Any blocking operation — database
query, file read, network call, synchronization on a contested lock — will freeze the
entire UI for its duration. Data must already be available in the `GridRow` when
`bind()` is called. Fetch, parse, and pre-compute in a background thread; `bind()`
only reads.

### 3. Pre-compute search-sensitive and display-ready data at load time

Avoid per-bind string transformations like `toLowerCase()`, `substring()`, or number
formatting. These create short-lived heap objects that accumulate GC pressure at
scroll speed. Instead:

- Store display-ready strings in the `GridRow` at load time
  (`row.put("salaryDisplay", "$" + formatted)`)
- Store a pre-lowercased search key alongside the display value if filtering is needed
- Formatters registered via `registerFormatter()` run once per bind — keep them cheap

### 4. Update listeners, never accumulate them

Any `ActionListener`, `MouseListener`, or other listener added in `bind()` must be
removed before a new one is added. The component is recycled across rows — without
cleanup, each scroll step adds another listener pointing at a stale row.

```java
// Wrong — listener count grows unboundedly
button.addActionListener(e -> doSomethingWith(row));

// Correct
for (ActionListener al : button.getActionListeners()) {
    button.removeActionListener(al);
}
button.addActionListener(e -> doSomethingWith(row));
```

The same applies to `MouseListener`, `FocusListener`, and any other listener type
attached in `bind()`.

### 5. Reuse existing child components (the CellRenderer recycling pattern)

When a `CellRenderer` or custom panel contains sub-components, check `existing`
before creating new ones. Swing component construction is expensive relative to a
field update.

```java
if (existing instanceof JPanel) {
    cell      = (JPanel)  existing;
    nameLabel = (JLabel)  cell.getClientProperty("nameLabel");
    msgButton = (JButton) cell.getClientProperty("msgButton");
} else {
    // construct once, store references via putClientProperty
}
nameLabel.setText(value.toString()); // update, not recreate
```

### 6. Keep bind() idempotent

Calling `bind(row, idx)` twice with the same arguments must produce the same visual
result. `prepareForReuse()` followed by `bind()` must also produce the same result
as `bind()` alone on a component already showing that row. Stateful side effects
(opening dialogs, firing events, modifying the model) do not belong in `bind()`.

### 7. Store rowIndex for setSelected()

`setSelected(boolean)` is called after every `bind()`. It needs to know the parity
of the current row to restore the correct alternating background when deselected.
Store `rowIndex` as an instance field in `bind()` so `setSelected()` can compute it.

```java
// In bind():
this.lastRowIndex = rowIndex;

// In setSelected():
Color bg = selected ? BG_SELECTED : (lastRowIndex % 2 == 0 ? BG_EVEN : BG_ODD);
setBackground(bg);
contentPane.setBackground(bg); // keep opaque children in sync
```

### 8. Keep opaque child backgrounds in sync

If a child component is `setOpaque(true)` (required when `setOpaque(false)` is
unreliable under the system LAF), its background must be updated in `bind()`,
`setSelected()`, and `prepareForReuse()` to match the parent panel's current
background. An out-of-sync opaque child will paint the wrong color over borders
or selection highlights.

### 9. Position children in doLayout(), not only in bind()

With null layout, child bounds set in `bind()` are correct at bind time. But if the
viewport is resized without triggering a new `bind()` call, children keep stale
bounds. Override `doLayout()` to recompute positions from `getWidth()` / `getHeight()`
so resize is always correct regardless of when `bind()` last ran.

### 10. Contain revalidation with isValidateRoot()

If a child component has its own internal layout engine (`JTextPane`, `JEditorPane`),
its internal state changes fire `revalidate()` calls that propagate up the component
tree. Without a boundary, these reach `VirtualCanvas` and can re-enter the refresh
cycle mid-bind. Override `isValidateRoot()` on the row panel to return `true` —
this makes the row panel the revalidation boundary, containing the event within the
slot.

### Summary — bind() checklist

| Check | Rule |
|-------|------|
| No `new Color/Font/AttributeSet` | Allocate in constructor |
| No blocking I/O or DB calls | Data must be pre-fetched |
| No raw `toLowerCase()` per row | Pre-compute at load time |
| All listeners removed before adding | Prevent stale-row listener accumulation |
| `existing` checked before constructing | Recycle sub-components |
| `lastRowIndex` stored | setSelected() needs it |
| Opaque child backgrounds updated | Match parent on every bind/select |
| `doLayout()` overridden | Resize works without bind() |
| `isValidateRoot()` returns true | Contains JTextPane revalidation |

---

## Search Field Design — Placement and Integration Quality

### Why Header-Embedded Search Outperforms Toolbar Search

The placement of a search field matters as much as its existence. Putting the search
field inside the column header rather than in a separate toolbar communicates that
search is a property of the column, not a separate tool being operated on the list.
Users read it as "this column is filterable" rather than "there is a search bar
somewhere." That distinction — column-intrinsic vs. externally attached — is a subtle
but real UX improvement that many commercial components get wrong by placing a global
filter in a toolbar that feels disconnected from the data.

The absence of a label reinforces this. A "Filter:" prefix or placeholder-heavy
convention signals an afterthought. A search field flush-right in the column header
with no label looks native — it is what a polished desktop application would do.

### What Separates SmartGrid's Implementation

Live search/filter is table stakes for any list or grid today. What separates
commercial-quality implementations is the integration quality, not the feature itself:

- Filter fires on every keystroke with no manual model repopulation
- The summary row count updates immediately and accurately
- The result is smooth at any scroll position without external wiring
- No `TableModel` events, no `fireTableDataChanged()`, no repaint management

Many open-source Swing components support search but require the caller to
re-populate the model, manually fire repaint events, and manage the count display
independently. SmartGrid's `setFilter(GridModelFilter)` → `computeVisibleRows()` →
`modelReset()` → `refresh()` → `updateSummaryCount()` chain handles all of it as a
single EDT-synchronous operation with no caller involvement beyond providing the
predicate.

### The Pattern Generalizes

Any single-column SmartGrid — a tag picker, a file chooser, a command palette, a
language selector — can use this header-search pattern with zero new infrastructure.
The multi-column equivalent (`setColumnFiltersVisible(true)`) already exists for
the Table use case. The list demo proves that the single-column presentation is
clean enough to stand without a toolbar.

The combination of header-embedded search, live summary row, and no external chrome
overhead is genuinely difficult to match without a commercial component — and those
typically cost hundreds of dollars and require a week of integration work.

---

## Semantic Typography — Monospace for Data, Proportional for Chrome

### The Principle

Use a monospaced font for all cell values — data that originates outside the
application, from a database, file, or API — and a proportional font for UI chrome:
headers, labels, descriptions, tooltips. This trains users quickly and
subconsciously: monospace means "this is your data," proportional means "this is
the application talking to you."

This is *semantic typography* — using a visual property to encode meaning rather
than aesthetics. Most modern UI uses typography decoratively, choosing fonts for
feel. Semantic typography carries real information, and once a user internalises
the convention it becomes a powerful pre-attentive signal that requires no conscious
reading.

### Why It Works

**Alignment.** Numbers, codes, identifiers, and dates scan more naturally in
monospace because every character occupies identical width. Proportional fonts in
data columns create subtle misalignment that fatigues users scanning for patterns
without them knowing why. This is why spreadsheets, terminals, and database tools
have always defaulted to monospace for values.

**Distinction without color.** Color is the most common way to separate data from
chrome, but it requires a coherent palette and can fail under accessibility
constraints. A font change costs nothing and survives any color scheme.

**Precedent in high-stakes environments.** Financial terminals, trading floors,
database administration tools, log viewers, and monitoring dashboards all use
monospace for data. Bloomberg's terminal is famously dense by modern UI standards
and also famously indispensable. The font convention signals seriousness about
data fidelity — a quality power users recognize and trust.

### Where It Fits in SmartGrid

The `registerCellRenderer` mechanism already supports local application of this
convention. The Codes demo uses a monospace-bold renderer for the code column
specifically. Phase 20 of the roadmap formalizes this as a grid-level `setDataFont`
property that flows through to `StandardRowPanel` and all default cell rendering,
so individual renderers do not need to repeat the configuration.

### The Modern Objection and the Answer

Material Design, Fluent, and Apple HIG all assume proportional fonts everywhere,
so deviating reads as "unpolished" to eyes trained on those systems. But those
guidelines were built for consumer applications where approachability matters
more than data fidelity. For power-user and technical desktop applications —
forensic data display, administrative tools, data-intensive workflows — the
consumer convention is the wrong target. Monospace for data is not a failure to
follow modern conventions; it is a deliberate choice of the right convention for
the audience.

---

## SmartGrid as a Calendar Engine

### The Week-as-Row Mapping

A full-year calendar view is a natural fit for SmartGrid. The mapping is direct:

- **Row = one week** (~53 rows for a full year — trivial for SmartGrid, no virtualization pressure)
- **Columns = Mon through Sun** (7 fixed columns, equal width)
- **Cell value = a `DayData` object** holding a list of `CalendarEvent` items for that day
- **Row renderer = `CalendarWeekRowPanel`** — a custom `Recyclable` containing seven `DayCellPanel` instances

With ~53 rows, SmartGrid's entire virtual canvas fits comfortably in memory with no pooling pressure. The infrastructure exists for 150,000 rows; at 53 it is simply not a constraint.

### Why SmartGrid Beats a Table for This

JTable renders cells with a rubber stamp — there is no live component inside a cell.
A JTable calendar cell can hold a string and paint some colors. It cannot contain
a `JButton`, respond to a click on a specific chip, or embed a `JScrollPane` for
overflow events.

Because SmartGrid rows are real Swing components, each `DayCellPanel` contains:
- A live `JLabel` for the primary event (clickable, colored background)
- Live `JButton` chip instances for secondary events (real click → detail panel update)
- Tooltip support on each chip, no extra wiring

The detail panel to the right updates when any chip or primary label is clicked.
None of this is possible in JTable without deep customization of the event dispatch.

### Day Cell Layout

Each `DayCellPanel` divides its `rowHeight` into two vertical sections:

```
+----------------------------------------+
|  [primary event name............] [DD] |  ← TOP_HEIGHT px; colored bg = category
+----------------------------------------+
|  [■] [■] [■]                           |  ← remainder; chip buttons, one per extra event
+----------------------------------------+
```

- The top section shows the first event with a category-colored background and white text.
  Clicking it populates the detail panel.
- The bottom section shows remaining events as small square chip buttons colored by category.
  Clicking a chip populates the detail panel.
- `DD` is the day-of-month number, small and muted, top-right corner.
- Days outside the year boundary (partial first/last week) render with a grayed-out background
  and no events.

### Event Categories and Colors

| Category | Color | Meaning |
|---|---|---|
| `RELEASE` | Blue `#4285F4` | Planned software releases |
| `HOTFIX` | Red `#EA4335` | Emergency patches |
| `MEETING` | Green `#34A853` | Scheduled meetings / reviews |
| `DEADLINE` | Amber `#FF9900` | Hard deadlines, freeze dates |

Color is the primary visual signal at the calendar level — users scan for red chips
(hotfixes) or amber chips (deadlines) without reading text.

### Partial Week Handling

The year does not start on Monday or end on Sunday in the general case. The grid
includes the full weeks that contain January 1 and December 31:

```java
LocalDate weekStart = jan1.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
LocalDate yearEnd   = dec31.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
```

Days outside the target year are marked `inYear = false` in their `DayData`. The
renderer shows them with a muted background and no events — they are present for
visual continuity but do not distract.

### Event Detail Panel

A fixed-width panel on the right side of the vapp (BorderLayout.EAST) shows:
- A category-colored stripe across the top
- Event name (bold)
- Formatted date
- Category label (colored to match stripe)
- Free-text description

The panel is populated by a `Consumer<CalendarEvent>` lambda passed into every
`DayCellPanel` at construction time. The consumer is the only coupling between the
calendar grid and the detail view — no shared state, no listeners to wire up.

### Extension Opportunities

This prototype establishes the pattern. Natural extensions include:

- **Month labels**: insert `GroupHeaderRowPanel` rows before the first week of each month
- **Scroll-to-today**: compute `weekIndex * rowHeight` and set the JScrollPane position on open
- **Overflow events**: embed a `JScrollPane` inside `DayCellPanel`'s chip section for days with
  many events (the row height / JScrollPane brainstorm from earlier applies directly here)
- **Event loading from a real model**: replace `generateEvents()` with a `CalendarModel` interface
  that queries a database or iCal source
- **Drag-to-reschedule**: SmartGrid rows are live components, so mouse drag listeners are possible
  without any framework support

---

## Extensibility Analysis — Interface Definitions and Design Patterns

### What Is Already Good and Should Not Change

The current extensibility foundation is solid. `Recyclable`, `Selectable`,
`CellRenderer`, `HeaderCellRenderer`, `FooterCellRenderer`, `GridModel`,
`GridModelFilter`, and `Strip` are all clean interface definitions. What is
missing is mostly the **layered defaults** pattern that separates "I want the
smart behaviour without implementing anything" from "I want full control." That
is the gap between a component library and a component framework.

`Recyclable` + `Selectable` as separate interfaces is exactly right — composition
over inheritance. `CellRenderer`'s `existing` recycling parameter is the correct
Android-style pattern. `Strip` as an interface is clean and proven. `GridModel`
as the data contract is solid. The `fnd-type` dispatch mechanism, while
string-keyed, is intentionally loose enough to support the scripting use case.
These do not need touching.

---

### Gap 1 — FooterAggregator (the highest-impact missing abstraction)

`FooterCellRenderer` is a good interface but it is all-or-nothing. The user
implements the whole thing or gets a blank cell. A registration-based aggregator
layer would let the component be smart by default:

```java
public interface FooterAggregator {
    void reset();
    void accumulate(Object value);
    JComponent render(ColumnDef col);
}
```

A `SmartFooterCellRenderer` (a `FooterCellRenderer` implementation) holds a
registry of `FooterAggregator` instances keyed by column key or Java type. It
loops through `pageRows`, accumulates, and delegates rendering. Users register
their aggregators and get intelligent footers without implementing the full
`FooterCellRenderer` contract.

Default implementations ship with SmartGrid:
- `NumericAggregator` — sum + average, formatted appropriately
- `CategoricalAggregator` — frequency map for distinct values
- `HeatmapAggregator` — bucket distribution for numeric ranges (see Phase 24)

Someone building a log viewer registers a `SeverityAggregator` that counts by
level. That is the "cell-level smartness as an interface" pattern — each column
gets a strategy, not a full renderer.

---

### Gap 2 — `GridModelFilter` Composition

`GridModelFilter` is already a functional interface. Adding `default` methods
enables readable composition without manual `&&`:

```java
default GridModelFilter and(GridModelFilter other) {
    return row -> this.accept(row) && other.accept(row);
}
default GridModelFilter or(GridModelFilter other) {
    return row -> this.accept(row) || other.accept(row);
}
static GridModelFilter not(GridModelFilter f) {
    return row -> !f.accept(row);
}
```

This is a one-file change with zero breaking impact.

---

### Gap 3 — `CellClickListener` (currently forced into every renderer)

Anyone who wants to respond to a cell click currently has to embed a
`MouseAdapter` inside their `CellRenderer`. That is fine for fully custom row
types like `LogRowPanel`, but for a standard column with a formatted value it is
invasive. A grid-level listener handles it cleanly:

```java
public interface CellClickListener {
    void cellClicked(CellClickEvent e); // row, col, GridRow, source JComponent, MouseEvent
}
grid.addCellClickListener(listener);
```

`StandardRowPanel` fires this to the grid's registered listeners on click,
passing the column index so the listener knows which cell. This enables the
heatmap footer → click → filter interaction cleanly without any component
embedding.

---

### Gap 4 — `RowDecorator` (signal/response architecture for visual styling)

#### The two decoration models

**Tag-based decoration (current)** encodes the decoration decision *in the data*.
A `GridRow` carries a `fnd-style` tag set by whoever loaded or analysed the data
— including BeanShell scripts operating at runtime. This is the right model when:
- Data is populated generically from an external source (database, API, BeanShell)
- Interesting rows are *identified* at runtime through pattern analysis
- The decoration decision is meaningful as data (should be serialisable, persistent,
  or communicable across subsystems)

Workflow: load data → analyse → `row.setTag("fnd-style", "critical")` → `notifyDataChanged()` → grid repaints.
The BeanShell script author defines the *semantic category*; they do not need to know anything about Swing.

**RowDecorator (proposed)** encodes the decoration decision *as a rule*.
A predicate registered at setup time that recomputes fresh on every `bind()`.
This is the right model when:
- The rule is a simple condition on the row's values and does not need to be stored
- The rule is always active and does not change at runtime

```java
public interface RowDecorator {
    void decorate(JComponent panel, GridRow row, int rowIndex, boolean selected);
}
grid.addRowDecorator((panel, row, idx, sel) ->
    panel.setBackground("Inactive".equals(row.get("status")) ? INACTIVE_BG : null));
```

#### The clean synthesis: tags as signals, decorators as interpreters

The two models are complementary. A `RowDecorator` can *read* tags:

```java
grid.addRowDecorator((panel, row, idx, sel) -> {
    String style = row.getTag("fnd-style");
    if ("critical".equals(style)) {
        panel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
    } else if ("flagged".equals(style)) {
        panel.setBackground(new Color(0xFF, 0xF8, 0xDC));
    }
});
```

The BeanShell script still just sets a tag. The visual treatment is registered
separately in Java. `StandardRowPanel` no longer needs hardcoded knowledge of
any specific tag names. New tag values work automatically as long as a decorator
responds to them.

A built-in `TagStyleDecorator` pre-registered on every grid handles the existing
`fnd-style` tags (`warning-glow`, `error`, etc.) for backward compatibility.
User-registered decorators run in order after it.

#### Registration and execution

`addRowDecorator(RowDecorator)` on SmartGrid maintains an ordered `List<RowDecorator>`.
Decorators are called in `doRefresh()` *after* `bind()` and `setSelected()` on each slot,
so they layer on top of the row panel's base state:

```java
((Recyclable) slots[i]).bind(row, modelIdx);
if (slots[i] instanceof Selectable) {
    ((Selectable) slots[i]).setSelected(selectionModel.isSelectedIndex(modelIdx));
}
for (RowDecorator d : rowDecorators) {
    d.decorate(slots[i], row, modelIdx, selectionModel.isSelectedIndex(modelIdx));
}
```

Running at the SmartGrid level (not inside `StandardRowPanel`) means decorators
apply to *any* row type — `LogRowPanel`, `FeaturedRowPanel`, custom types — not
only the default renderer.

---

### Gap 5 — Builder Pattern for Construction

For a publishable component, a builder reduces the "20 setter calls" ceremony
and communicates which options are meaningful to set together:

```java
SmartGrid grid = SmartGrid.builder(model)
    .darkTheme(true)
    .rowHeight(64)
    .rowNumbers(true)
    .columnPadding(8, 2)
    .footerAggregator(new SmartFooterCellRenderer())
    .build();
```

The current setter approach works but does not communicate which options form a
coherent configuration, and it does not enable configuration objects to be
passed around or reused across grid instances. A builder also provides a natural
validation point — catch conflicting settings at construction time rather than
silently at render time.

---

### Gap 6 — `CellDecorator` and the `activeCell` State Model

#### Why RowDecorator alone is not sufficient

`RowDecorator` operates on the whole row panel — the right granularity for
row-level concerns (backgrounds, borders, selection). Cell focus, cell copy,
and keyboard navigation all require a second dimension: *which column within
the row* is the active unit. This inevitably points toward a `CellDecorator`.

The relationship is hierarchical:

```
RowDecorator            → operates on the whole row panel
  └── CellDecorator     → operates on a specific cell component within that row
```

#### The activeCell shared state

An `int[] activeCell = {rowIndex, colIndex}` held on SmartGrid (initialised to
`{-1, -1}`) follows the same shared mutable reference pattern as `columnWidths`
and `searchHolder`. `StandardRowPanel.bind()` reads it to know whether any cell
in this row is the active one, and which column index. Mouse click on a cell
computes the column index from `mouseX` and the `columnWidths` array, then
updates `activeCell` and triggers a rebind.

#### CellDecorator interface

```java
public interface CellDecorator {
    void decorate(JComponent cellComponent, ColumnDef col, Object value,
                  GridRow row, int rowIndex, int colIndex, boolean cellFocused);
}
```

`cellFocused` is `rowIndex == activeCell[0] && colIndex == activeCell[1]`.
The decorator that draws the focus ring checks this flag; all other decorators
ignore it.

`StandardRowPanel` applies registered `CellDecorator` instances in `bind()`,
after rendering each cell. It already iterates columns there, so the hook point
is natural. SmartGrid passes its `List<CellDecorator>` to `StandardRowPanel`
via the same shared-reference pattern used for `columnWidths` and `cellRenderers`.

#### Three consumers of activeCell

Cell focus, cell copy, and keyboard navigation are not three separate features —
they are one shared state with three consumers:

| Consumer | Behaviour |
|----------|-----------|
| **Visual** | `CellDecorator` draws focus ring / highlight on the active cell |
| **Clipboard** | Ctrl+C reads `row.get(cols.get(activeCell[1]).getKey())` and puts it on the system clipboard |
| **Navigation** | Arrow keys, Tab, Enter mutate `activeCell` indices and trigger rebind |

The `activeCell` state is the prerequisite for all three. Implementing it first
(as a shared `int[]`) unlocks the others incrementally.

#### Implementation notes

- `activeCell` must be cleared to `{-1, -1}` when the model resets or filter
  changes, since the previously focused row may no longer be visible.
- A `CellDecoratorRowAdapter` can wrap a list of `CellDecorator` instances as a
  single `RowDecorator`, enabling the same `addRowDecorator()` registration path
  if desired.
- `StandardRowPanel` will need to expose a way for SmartGrid-level code (or the
  `CellDecoratorRowAdapter`) to obtain individual cell components. A
  `CellDecoratable` interface with `getCell(int colIndex)` is the clean
  approach; direct component hierarchy traversal is the pragmatic fallback.
