# SmartDrag — Design Reference

Architecture, constraints, and extension points for the SmartDrag DnD
infrastructure. Open this when you need to understand why it is structured
the way it is, or when wiring a new component into the drag/drop system.

---

## Core Classes

| Class | Role |
|---|---|
| `EmulatorPayload` | Generic envelope wrapping the dragged object; carries the shared `DataFlavor` |
| `SmartDragSource` | Makes any `JComponent` a drag source via a static factory; implements `Transferable` |
| `SmartTransferHandler` | Makes any `JComponent` a drop target; unwraps the payload and dispatches to a `DropAction` lambda |

All three live in `org.jwellman.swing`.

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

## The Same-Component TransferHandler Conflict

A single `JComponent` can hold only one `TransferHandler`. `SmartDragSource.makeDraggable()`
sets one; `new SmartTransferHandler(...)` sets another. Calling both on the same
component silently overwrites the first.

**Pattern to avoid:**
```java
// BROKEN — second setTransferHandler overwrites the first
SmartDragSource.makeDraggable(this, payloadSupplier);   // sets export handler
this.setTransferHandler(new SmartTransferHandler(...)); // overwrites it
```

**Correct pattern — parent/child split:**
```java
// Child handles drag-out
SmartDragSource.makeDraggable(childLabel, payloadSupplier);

// Parent handles drop-in (different component, no conflict)
parentPanel.setTransferHandler(new SmartTransferHandler(this::onDrop));
```

The CalendarDemo demonstrates this split: `primaryLabel` and chip buttons are drag
sources; `DayCellPanel` (the parent) is the drop target.

One side-effect: drops that land precisely on a drag-source child component hit
that child's export-only `TransferHandler` and are silently rejected. In practice
the affected area is small; aim at the background of the target cell.

---

## CalendarDemo Integration

CalendarDemo is the first consumer of this infrastructure and serves as the
reference implementation for same-JVM DnD between two instances of the same
component type.

- `DayCellPanel.primaryLabel` — drag source (carries `CalendarEventTransfer`)
- Chip buttons — drag sources (same payload type)
- `DayCellPanel` — drop target; `onEventDropped()` removes the event from the
  source cell's `DayData` and adds it to the target cell's `DayData`, then
  repopulates both cells

`CalendarEventTransfer` (package-private inner class of `DayCellPanel`) bundles the
`CalendarEvent` with a back-reference to its source `DayCellPanel` so the drop
handler can update both sides of the move in one step.
