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
