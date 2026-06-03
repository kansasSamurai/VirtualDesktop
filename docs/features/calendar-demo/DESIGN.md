# CalendarDemo — Design Reference

Architecture, component structure, and visual-design decisions for the CalendarDemo
feature. Open this when you want to understand *why it is built the way it is* or
when extending it with new capabilities.

---

## Introduction

CalendarDemo is a full-year, read-only calendar view built as a virtual application
(vapp) within VirtualDesktop. It was created to demonstrate SmartGrid's strip
decorator system and pluggable row rendering, while simultaneously serving as a
practical planning aid for tracking release events, sprints, on-call schedules, and
team deadlines.

The calendar renders the current year with one row per ISO week. Days within each
week are displayed left-to-right (Monday → Sunday). A configurable highlight mode
visually distinguishes today, the current week, and near-future empty days from the
rest of the calendar.

---

## Feature Summary

- **Year-at-a-glance layout** — 52–53 ISO week rows; days fill the grid columns
- **Event banners** — each day can carry a primary event banner (colored strip) and
  zero or more small color chips for additional events
- **Event categories** — Release (blue), Hotfix (red), Meeting (green), Deadline (orange)
- **Event detail panel** — clicking an event banner or chip opens a right-side panel
  with the event's full name, date, and category
- **Highlight / accent mode** — toggle that applies today's badge, current-week
  emphasis, and future-empty-day markers
- **Strip decorators** — pluggable left-gutter columns: week numbers, on-call
  names, sprint identifiers
- **SmartGrid integration** — the calendar grid is backed by SmartGrid's virtual
  scroll, filter, and strip infrastructure

---

## Design and Feature Details

### Component Hierarchy

```
CalendarDemo (JPanel — vapp entry point)
└── SmartGrid (grid component)
    ├── Strip — WeekNumberStrip    (left gutter: ISO week labels)
    ├── Strip — OnCallStrip        (left gutter: on-call person per week)
    ├── Strip — SprintStrip        (left gutter: sprint number + palette)
    └── CalendarWeekRowPanel       (one row per ISO week)
        └── DayCellPanel × 7       (one cell per day, Mon–Sun)
```

`CalendarDemo` builds the data model (year's worth of `GridRow` objects keyed by
`DayData`), configures the grid, attaches strips, and wires the event-detail panel.
`CalendarWeekRowPanel` holds seven `DayCellPanel` instances laid out in a horizontal
strip. `DayCellPanel` handles all per-day rendering.

### Data Model

| Class | Role |
|-------|------|
| `DayData` | Wraps a `LocalDate` and the list of `CalendarEvent` objects for that day; carries an `inYear` flag for padding days outside the display year |
| `CalendarEvent` | Name, date, and `EventCategory`; immutable value object |
| `EventCategory` | Enum of event types (`RELEASE`, `HOTFIX`, `MEETING`, `DEADLINE`) — each carries a display name and a `Color` |

`GridRow` keys used by the calendar:
- `"mon"` through `"sun"` — `DayData` for each weekday
- `"oncall"` — String for the on-call person name
- `"sprint"` — Integer sprint index (used by SprintStrip for palette selection)

### Visual Language

#### Color Palette

| Purpose | Color | Constant |
|---------|-------|----------|
| Accent / today / future marker | `#E91E8C` (hot pink) | `TODAY_ACCENT` |
| In-year weekday background | `#FFFFFF` (white) | `BG_IN_YEAR` |
| Past weekday background | `#E8F0FF` (pale blue) | `BG_PAST` |
| Weekend background | `#F4F4F4` (light gray) | `BG_WEEKEND` |
| Out-of-year padding | `#F0F0F0` (gray) | `BG_OUT_YEAR` |
| Release event | `#4285F4` (Google blue) | `EventCategory.RELEASE` |
| Hotfix event | `#EA4335` (Google red) | `EventCategory.HOTFIX` |
| Meeting event | `#34A853` (Google green) | `EventCategory.MEETING` |
| Deadline event | `#FF9900` (amber) | `EventCategory.DEADLINE` |

#### Day Cell Layout

```
┌──────────────────────────────┐  ← 2 px hot-pink frame on today's cell
│                          [dd]│  ← day-number badge (top-right)
│ [Event Name banner          ]│  ← primaryLabel — 18 px high, at TOP_HEIGHT/2-9
├──────────────────────────────┤  ← TOP_HEIGHT = 36 px
│ [■][■][■]                    │  ← chipsPanel — 14×14 px color chips (events 2+)
└──────────────────────────────┘
```

The primary event banner fills `w - 26` pixels wide (leaving room for the day badge)
and is positioned at `TOP_HEIGHT / 2 - 9` from the top. Additional events appear as
small square color chips below the 36 px header zone.

#### Day-Number Badge States

The badge in the top-right corner of each cell has three visual states, all gated on
highlight mode:

| Condition | Size | Background | Text | Font |
|-----------|------|------------|------|------|
| Today | 20 px, centered | `TODAY_ACCENT` (hot pink) | White | Bold 13 pt |
| Future day, current week | 20 px, centered | `Colors.DAY_NUMBER` (gray `#808080`) | White | Bold 13 pt |
| All other days | 12 px, top-right | Transparent | Gray | Plain 11 pt |

The two accented states share the same geometry (20 px tall, vertically centered in
the header zone) so the current-week row reads as a cohesive unit. The only
difference is background color: hot pink signals *now*, gray signals *soon*.

#### Today Cell Border

Today's `DayCellPanel` is given a 2 px `MatteBorder` on all four sides in
`TODAY_ACCENT`, replacing the normal 1 px gray grid border. The border is set
dynamically in `populate()` and reset in `clear()`, so it participates in the
highlight toggle. Because day cells use `setLayout(null)` with hardcoded
`setBounds()` positions, the thicker border does not shift any child component.

#### Future Empty Day Marker

When highlight mode is on and a day is after today but still within the current ISO
week and has no events, its `primaryLabel` is rendered as a small hot-pink banner
(`TODAY_ACCENT` background, white text) containing `"<<<"`. The banner occupies the
same position and dimensions as a standard event banner, so it reads visually as a
placeholder event rather than a special cell state. The cell background remains the
normal white of any in-year weekday.

### Strip Decorator System

Strips are pluggable left-gutter columns registered via `grid.addStrip(strip)` in
`CalendarDemo`. All strips implement `org.jwellman.swing.grid.Strip`:

| Strip | Width | Visual |
|-------|-------|--------|
| `WeekNumberStrip` | 34 px | ISO week label ("W22"); current week gets a 4 px hot-pink top border |
| `OnCallStrip` | 70 px | Person's name for the week; current week gets a 4 px hot-pink top border |
| `SprintStrip` | 52 px | Sprint number; background cycles through a 7-color palette; current week gets a 4 px hot-pink top border |

All three strips participate in the highlight toggle. When `highlightOn[0]` is
`true` and the row represents the current ISO week, the slot's border is swapped from
`SLOT_BORDER` to `ACCENT_SLOT_BORDER` — a compound `MatteBorder` of 4 px top in
`ACCENT` (hot pink), 1 px bottom/right in border gray, and inner side padding.

All slot borders include a 4 px empty top inset in their normal (`SLOT_BORDER`) state
as well. This ensures text alignment is identical whether the accent border is active
or not — the 4 px top space is either painted hot pink or left transparent, but it is
always present. This also leaves a reserved top margin for any future per-row
decoration that any strip might add.

### Highlight / Accent Mode Toggle

A `boolean[] highlightOn` array (single-element, shared by reference) acts as a live
toggle passed into `DayCellPanel`, `WeekNumberStrip`, `OnCallStrip`, and `SprintStrip`.
When `highlightOn[0]` is `false`, all cells and strips render in their neutral state
regardless of date. Affected visuals include: today's cell border, today's and
future-current-week day badges, the future-empty-day `"<<<"` banner, and the
current-week hot-pink top border on all three strips.

The toggle is wired to a toolbar button in `CalendarDemo` and triggers a full
`repopulate()` cycle to redraw affected cells.

### Event Detail Panel

`EventDetailPanel` occupies the right side of the `CalendarDemo` layout. Clicking
any event banner or color chip invokes the `Consumer<CalendarEvent> onEventClicked`
callback, which calls `EventDetailPanel.show(event)` to update its display.

The panel shows:
- Event name (large, styled label)
- Event date (formatted)
- Category badge (colored label matching the event's category color)

### Drag and Drop — Event Rescheduling

Events can be dragged from one `DayCellPanel` to another. The drag-and-drop wiring
uses the SmartDrag infrastructure (`SmartTransferHandler`, `SmartDragSource`) from
`org.jwellman.swing`.

#### How Swing is told that DayCellPanel owns DnD

The export and import directions are routed through completely different mechanisms.

**Export (drag-out)**: `primaryLabel` and chip buttons have no `TransferHandler`
of their own. When `mousePressed` fires on either, the handler is manually invoked
on the *panel*:

```java
panel.getTransferHandler().exportAsDrag(panel, e, TransferHandler.MOVE);
```

The first argument `panel` tells Swing "treat this as the drag source." We reach
past the child component to the panel's handler by hand.

**Import (drop-in)**: when `setTransferHandler(handler)` is called on `DayCellPanel`,
Swing quietly installs a `DropTarget` on the panel. The child labels and chips have
no `TransferHandler` and therefore no `DropTarget`. When a drop occurs, Swing's
lightweight dispatcher routes it to the innermost component that *has* a `DropTarget`
— and only `DayCellPanel` has one. Drops anywhere inside the cell, including over
the event label, are handled by the panel automatically.

This is why earlier designs that called `SmartDragSource.makeDraggable` on
`primaryLabel` broke drop behaviour: `makeDraggable` installs a `TransferHandler`
(and therefore a `DropTarget`) on the label, intercepting drops that should have
reached the panel.

| Direction | How routing works |
|---|---|
| Drag out | `mousePressed` on child explicitly calls `panel.getTransferHandler().exportAsDrag(panel, ...)` |
| Drop in | Swing auto-routes to the innermost component with a `DropTarget`; only the panel has one |

#### Payload

`DayCellPanel` uses the export+import constructor of `SmartTransferHandler`:

```java
setTransferHandler(new SmartTransferHandler(
    () -> draggingEvent != null
        ? new CalendarEventTransfer(draggingEvent, DayCellPanel.this) : null,
    this::onEventDropped,
    () -> draggingEvent = null
));
```

`draggingEvent` is set in `mousePressed` just before `exportAsDrag` is called. The
supplier evaluates it lazily when Swing calls `createTransferable()` a fraction of
a second later. `onExportDone` clears it unconditionally when the drag ends,
whether the drop succeeded or was cancelled.

`CalendarEventTransfer` (package-private inner class) carries both the `CalendarEvent`
and a reference to the source `DayCellPanel`, so the drop handler can update both
cells in one step: remove the event from the source cell's `DayData`, add it to the
target's, and repopulate both.
