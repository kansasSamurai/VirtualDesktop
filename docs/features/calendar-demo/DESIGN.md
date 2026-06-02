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
┌──────────────────────────────┐
│                          [dd]│  ← day-number badge (top-right, 12 px high)
│ [Event Name banner          ]│  ← primaryLabel — 18 px high, at TOP_HEIGHT/2-9
├──────────────────────────────┤  ← TOP_HEIGHT = 36 px
│ [■][■][■]                    │  ← chipsPanel — 14×14 px color chips (events 2+)
└──────────────────────────────┘
```

The primary event banner fills `w - 26` pixels wide (leaving room for the day badge)
and is positioned at `TOP_HEIGHT / 2 - 9` from the top. Additional events appear as
small square color chips below the 36 px header zone.

#### Future Empty Day Marker

When highlight mode is on and a day is after today but still within the current ISO
week and has no events, its `primaryLabel` is rendered as a small hot-pink banner
(`TODAY_ACCENT` background, white text) containing `"<<<"`. The banner occupies the
same position and dimensions as a standard event banner, so it reads visually as a
placeholder event rather than a special cell state. The cell background remains the
normal white of any in-year weekday.

#### Today Badge

The day-number label for today is rendered as a filled hot-pink circle (`TODAY_ACCENT`
background, white bold text, opaque, centered) to make today immediately visible.

### Strip Decorator System

Strips are pluggable left-gutter columns registered via `grid.addStrip(strip)` in
`CalendarDemo`. All strips implement `org.jwellman.swing.grid.Strip`:

| Strip | Width | Visual |
|-------|-------|--------|
| `WeekNumberStrip` | 34 px | ISO week label ("W22"); current week gets a 4 px hot-pink top border |
| `OnCallStrip` | 70 px | Person's name for the week |
| `SprintStrip` | 52 px | Sprint number; background cycles through a 7-color palette |

`WeekNumberStrip` uses a compound `MatteBorder` (4 px top in `ACCENT`, 1 px
bottom/right in border gray, inner padding) for the current-week marker. This keeps
the label background a consistent gray across all weeks while the narrow top stripe
provides a clear, non-disruptive accent.

### Highlight / Accent Mode Toggle

A `boolean[] highlightOn` array (single-element, shared by reference) acts as a live
toggle passed into `DayCellPanel`, `WeekNumberStrip`, and any other component that
participates in highlight rendering. When `highlightOn[0]` is `false`, all cells
render in their neutral state regardless of date.

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
