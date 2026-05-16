# SmartGrid — Design Reference

Architecture, internals, and behavioral contracts for the SmartGrid virtual-scroll
component. Open this when you want to understand *why it works the way it does* or
when onboarding a contributor. For what is planned or what has been completed, see
[ROADMAP.md](ROADMAP.md).

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
