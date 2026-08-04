# Application Stage — reference

Package: `org.katacode.appstage`  
Impl host: `PanelApplicationStage`  
Internal helpers: `org.katacode.appstage.internal.*` (not a public SPI)

## Interfaces

### `ApplicationStage`

- Layers: `getBaseLayer()`, `getPaletteLayer()`, `getDragLayer()`, `getPopupLayer()`
- Coords: `convertToStagePoint(Component source, Point p)` → layered-pane space
- Cards: `addCard(String id, JComponent card)`, `showCard(String id)`
- Services: notification, modal, drag-and-drop accessors

### `NotificationService`

- `showToast(String title, String message, ToastType type)`
- `showToast(String message, int durationMillis)`
- `clearAllToasts()`

`ToastType`: `SUCCESS`, `INFO`, `WARN`, `ERROR`

### `ModalService`

- `showModal(JComponent modalContent)` — centered chrome + dismiss
- `showPopover(JComponent content, Component anchorComponent)`
- `dismissActiveModal()`

### `DragAndDropService`

- `beginDrag(Component dragSubject, Point startPointOnSubject)`
- `updateDrag(Point currentMousePointOnStage)`
- `endDrag(Point endPointOnStage)`
- `isDragging()`

v1 demotes to the original parent after drag. Cross-component landing policies are tool-specific and not yet generalized.

## Implementation notes

- Overlay panels are `PassThroughPanel`: empty areas do not steal mouse events from the base card.
- Resize syncs all layer bounds; notification/modal services relayout on resize.
- Default toast/modal surfaces use `LafColors` (live `JTextField` bg/fg + `UIManager` fallbacks).

## Spec wiring example

```java
PanelApplicationStage stage = new PanelApplicationStage();
stage.addCard("main", createMainUi(stage));
stage.showCard("main");
this.setContent(stage);
```

Toolbar/actions take `ApplicationStage` and call services. Do not use `JOptionPane` for stage-owned feedback when a stage is available.

## Demo map

| Piece | Role |
|------|------|
| `SpecAppStageCalendarDemo` | VirtualDesktop tool: chrome + stage services |
| `CalendarPoCPanel` | Nested layered-pane calendar PoC (own drag) |
| `CalendarPoCFrame` | Thin standalone launcher for the PoC panel |
| `SpecCalendarDemo` / Year Calendar | Separate SmartGrid demo — not appstage |

## Future hosts (not in tree yet)

| Host | Intent |
|------|--------|
| `JFrameStage` | Standalone window owns the stage |
| `JInternalFrameStage` | Per-tool MDI overlays |
| `MockStage` | Headless / unit tests mocking services |

Optional later: thin `DragStageContext` if drag controllers should not see notifications/modals.

## Deferred product work

Documented also in `docs/DEVELOPER_GUIDE.md` §9:

- Desktop shell as `ApplicationStage` (unlocks shared drag layer / cross-tool DnD)
- Shortcut (`VShortcut`) drag on stage DRAG layer
- Carded “Desktop” as App content root
- Rewire calendar PoC onto `DragAndDropService`
