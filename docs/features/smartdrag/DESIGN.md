# SmartDrag — Design Reference

Architecture, constraints, and extension points for the SmartDrag DnD
infrastructure. Open this when you need to understand why it is structured
the way it is, or when wiring a new component into the drag/drop system.

---

## Core Classes

| Class | Role |
|---|---|
| `EmulatorPayload` | Generic envelope wrapping the dragged object; carries the shared `DataFlavor` |
| `SmartTransferHandler` | Unified handler for export, import, or both; the primary wiring point for all components |
| `SmartDragSource` | Makes any `JComponent` a pure drag source via a static factory; for components that drag but never receive drops |
| `DragImageFactory` | Renders drag-ghost images on demand from text + color; decoupled from any theme or component |

All four live in `org.jwellman.swing`.

---

## SmartTransferHandler: the central primitive

`SmartTransferHandler` handles all DnD responsibilities for a component through
lambdas, requiring no subclassing.

**Import-only** — the component accepts drops but never initiates drags:
```java
component.setTransferHandler(new SmartTransferHandler(data -> {
    if (data instanceof MyType) { ... }
}));
```

**Export + import** — the component both initiates drags and accepts drops:
```java
panel.setTransferHandler(new SmartTransferHandler(
    () -> draggingThing,       // payload supplier: evaluated lazily at drag-start
    this::handleDrop,          // drop action: called with the unwrapped payload
    () -> draggingThing = null // export-done: runs when drag ends (success or cancel)
));
```

**Export + import with a drag image** — adds a visual ghost shown while dragging:
```java
panel.setTransferHandler(new SmartTransferHandler(
    () -> draggingThing,
    this::handleDrop,
    () -> draggingThing = null,
    () -> DragImageFactory.forColoredLabel(draggingThing.name, draggingThing.color, 140)
));
```

The image supplier is evaluated at drag-start (lazily, same as the payload supplier)
so it always reflects the current item. Return `null` to fall back to the OS default
cursor with no ghost image.

Child components that *initiate* the drag call `exportAsDrag` on the parent panel,
which keeps the `TransferHandler` on one component:
```java
childLabel.addMouseListener(new MouseAdapter() {
    public void mousePressed(MouseEvent e) {
        if (canDrag()) {
            draggingThing = currentThing;
            panel.getTransferHandler().exportAsDrag(panel, e, TransferHandler.MOVE);
        }
    }
});
```

**Why the export-done callback matters**: `payloadSupplier` captures mutable state
(e.g. `draggingEvent`). Without cleanup, that state is left set if the user cancels
a drag or drops onto a non-accepting target. `onExportDone` fires unconditionally,
clearing it.

---

## DragImageFactory: drag ghost images

`DragImageFactory` renders `BufferedImage` instances on demand for use as drag
ghosts. Images are created fresh each drag so they always match the current item's
state (color, text).

```java
BufferedImage img = DragImageFactory.forColoredLabel(text, categoryColor, 140);
```

`forColoredLabel` produces an 18 px tall image — matching the height of a
`DayCellPanel` event banner — with a solid background fill and white text in
`SansSerif 11pt`. Width is caller-supplied; **140 px is the standard fixed value**
used throughout this project, chosen to be comfortably wider than most event names
without exceeding a typical calendar column.

The factory is intentionally decoupled from themes and components. The caller
supplies the color (typically sourced from the event category or theme at the
call-site) rather than the factory reaching into any global state. This makes it
safe to use from any component without creating unwanted coupling.

`SmartTransferHandler` positions the ghost so the cursor tip aligns with the
bottom-left corner of the image: `offset = (0, -height)`. The image floats
entirely above the cursor, keeping the drop target visible beneath it while
the label travels above.

---

## SmartDragSource: pure drag sources

Use `SmartDragSource.makeDraggable()` for components that only ever *initiate* drags
and have no drop responsibility — icons, thumbnails, simple labels. It installs a
`TransferHandler` and a `MouseListener` in one call:

```java
SmartDragSource.makeDraggable(iconLabel, () -> myDataObject);
```

Do **not** call `makeDraggable` on a component that also needs to accept drops —
`makeDraggable` sets a `TransferHandler`, and any subsequent `setTransferHandler`
call on the same component silently overwrites it. Use the export+import constructor
of `SmartTransferHandler` on the parent panel instead (see above).

`SmartDragSource.forPayload()` is package-private: it is the internal bridge used
by `SmartTransferHandler.createTransferable()` and is not part of the public API.

---

## Transport Boundary: JVM-Local Only

`EmulatorPayload.FLAVOR` uses MIME type `application/x-java-jvm-local-objectref`.
This is a deliberate choice. The JVM passes the `EmulatorPayload` as a live object
reference — no serialization, no byte-copying. This means:

- Payloads can carry anything: Swing component references, mutable models, lambdas.
- The OS never sees the transfer; it is completely invisible to Windows/Mac/Linux.

The practical consequence is an asymmetry in what the current infrastructure supports:

| Direction | Status | Mechanism |
|---|---|---|
| Emulator → Emulator | **Supported** | JVM-local object reference via `EmulatorPayload.FLAVOR` |
| OS → Emulator | **Supported** | `SmartTransferHandler.canImport()` also accepts `DataFlavor.javaFileListFlavor` |
| Emulator → OS | **Not supported** | OS does not speak `javaJVMLocalObjectMimeType` |

---

## Why Emulator → OS Is Non-Trivial

To drag an emulator object *out* to a host OS application (e.g. Windows Explorer),
`SmartDragSource` would need to advertise a standard flavor such as
`javaFileListFlavor` or `stringFlavor`, and `getTransferData()` would need to
materialize the payload as something the OS understands.

There is no generic way to do that. A `CalendarEvent` and a `VShortcut` would
produce completely different OS representations. The conversion is inherently
type-specific.

A stub `convertToHostFiles()` in the base class cannot work — returning `null`
would cause a `NullPointerException` in the OS DnD pipeline the moment any
external application tried to receive the drop.

---

## Extending for Emulator → OS Drag-Out

When a specific component needs drag-out support, the right approach is to add an
optional file-export supplier to `makeDraggable` rather than baking it into the
base class:

```java
// Proposed overload — only adds javaFileListFlavor if the supplier is non-null
SmartDragSource.makeDraggable(component, payloadSupplier, fileListSupplier);
```

`SmartDragSource` would then:
1. Advertise `javaFileListFlavor` in `getTransferDataFlavors()` only when `fileListSupplier != null`
2. Delegate to `fileListSupplier.get()` in `getTransferData()` for that flavor

This keeps the base case lean and places the OS-specific conversion responsibility
on the caller, who is the only party that knows what the payload means in OS terms.

---

## CalendarDemo Integration

CalendarDemo is the first consumer of this infrastructure and serves as the
reference implementation for same-JVM DnD between two instances of the same
component type.

`DayCellPanel` uses the full 4-arg constructor of `SmartTransferHandler`:

```java
setTransferHandler(new SmartTransferHandler(
    () -> draggingEvent != null
        ? new CalendarEventTransfer(draggingEvent, DayCellPanel.this) : null,
    this::onEventDropped,
    () -> draggingEvent = null,
    () -> draggingEvent != null
        ? DragImageFactory.forColoredLabel(
            draggingEvent.getName(),
            draggingEvent.getCategory().getColor(),
            140)
        : null
));
```

- `primaryLabel` and chip buttons — trigger drag on `DayCellPanel` via `mousePressed`;
  carry no `TransferHandler` of their own
- `DayCellPanel` — owns the unified handler; `onEventDropped()` removes the event
  from the source cell's `DayData`, adds it to the target cell's `DayData`, and
  repopulates both cells
- Cursor — `primaryLabel` cursor is set to `HAND_CURSOR` in `populate()` only when
  an event is present; reset to `DEFAULT` otherwise

`CalendarEventTransfer` (package-private inner class of `DayCellPanel`) bundles the
`CalendarEvent` with a back-reference to its source `DayCellPanel` so the drop
handler can update both sides of the move in one step. Because `DayCellPanel` owns
the `TransferHandler` (not the child labels), drops landing anywhere inside the
target cell — including over the event label — are handled consistently.
