# Application Stage (`org.katacode.appstage`)

A Java 8 Swing toolkit. An embeddable **stage** hosts tool content on a layered pane and exposes
small services for overlays. Tools talk to the **services**, not to `JLayeredPane` constants.

- Code: [`virtualdesktop-java8/src/main/java/org/katacode/appstage/`](../../../virtualdesktop-java8/src/main/java/org/katacode/appstage/)
- Implementation host: `PanelApplicationStage`
- Internal helpers: `org.katacode.appstage.internal.*` (not a public SPI)

## When to use it

- A `VirtualAppSpec` or tool UI needs toasts, modals, popovers or intra-tool drag ghosts
- Content should swap via cards without tearing down the overlay layers
- A standalone harness and an in-desktop tool should share one stage contract through constructor injection

To start a new stage-backed tool with an AI assistant, use the
[new tool shell prompt](../../prompts/new-tool-shell.md).

## Layers

`PanelApplicationStage` (`extends JPanel implements ApplicationStage`) wraps a private `JLayeredPane`.
Top of the table is the top of the stack:

| Layer | `JLayeredPane` constant | Contents |
|---|---|---|
| drag | `DRAG_LAYER` | Components being dragged |
| popup | `POPUP_LAYER` | Toasts, modal content, popovers |
| palette | `PALETTE_LAYER` | Modal backdrop |
| base | `DEFAULT_LAYER` | `CardLayout` cards (`addCard` / `showCard`) |

Dragged components paint over toasts and modals.

## Usage

```java
PanelApplicationStage stage = new PanelApplicationStage();
stage.addCard("main", createMainUi(stage));
stage.showCard("main");
this.setContent(stage);
```

1. Use the stage as the tool content root. It is already a `JPanel`.
2. Build the main UI as a card, and pass `ApplicationStage` (or a single service) into child panels.
3. Toolbars and actions call the services. Do not use `JOptionPane` for feedback when a stage is available.
4. The layer getters (`getDragLayer()` and so on) are escape hatches only.

Pass the stage in through constructors. Do **not** walk parents or use `getWindowAncestor` to find
a layered pane (`StageResolver` style).

## API

### `ApplicationStage`

- Layers: `getBaseLayer()`, `getPaletteLayer()`, `getDragLayer()`, `getPopupLayer()`
- Coordinates: `convertToStagePoint(Component source, Point p)`
- Cards: `addCard(String id, JComponent card)`, `showCard(String id)`
- Services: `getNotificationService()`, `getModalService()`, `getDragAndDropService()`

Method behavior, including exceptions and card replacement, is in the Javadoc.

### `NotificationService`

- `showToast(String title, String message, ToastType type)`
- `showToast(String message, int durationMillis)`
- `clearAllToasts()`

`ToastType`: `SUCCESS`, `INFO`, `WARN`, `ERROR`

### `ModalService`

- `showModal(JComponent modalContent)`: backdrop plus centered chrome with a Close button
- `showPopover(JComponent content, Component anchorComponent)`: content placed below the anchor, no backdrop
- `dismissActiveModal()`: dismisses the active modal or popover

Only one modal or popover is active at a time; showing another dismisses the current one.

### `DragAndDropService`

- `beginDrag(Component dragSubject, Point startPointOnSubject)`
- `updateDrag(Point currentMousePointOnStage)`
- `endDrag(Point endPointOnStage)`
- `isDragging()`

Point contract:

- `beginDrag` takes the grab point in **subject** coordinates
- `updateDrag` and `endDrag` take points in **stage** coordinates (`stage.convertToStagePoint(...)`)

Drags stay within one stage. v1 returns the component to its original parent afterwards; landing
policies are tool-specific and not yet generalized.

## Behavior notes

- The overlay panels are `PassThroughPanel`: empty areas do not take mouse events from the layers below.
- All layers resize with the stage, and the notification and modal services relayout.
- Popovers are not repositioned on resize.
- The drag service adds the dragged component straight to the layered pane, not into the panel
  returned by `getDragLayer()`.
- Text overlays use Look and Feel surfaces through the internal `LafColors` (live `JTextField`
  colors with `UIManager` fallbacks), not hardcoded light-theme greys. Semantic toast accents may
  stay fixed.

## Demo map

| Piece | Role |
|---|---|
| `SpecAppStageCalendarDemo` | VirtualDesktop tool ("App Stage Calendar"): dummy chrome plus stage services |
| `CalendarPoCPanel` | Nested layered-pane calendar PoC with its own drag |
| `CalendarPoCFrame` | Thin standalone launcher for the PoC panel |
| `SpecCalendarDemo` / Year Calendar | Separate SmartGrid demo, not appstage |

## Deferred work

Not in the tree yet; build when needed.

| Item | Intent |
|---|---|
| `JFrameStage` | Standalone window owns the stage |
| `JInternalFrameStage` | Per-tool MDI overlays |
| `MockStage` | Headless / unit tests mocking services |
| `DragStageContext` | Optional thin context if drag controllers should not see notifications or modals |
| Desktop shell as `ApplicationStage` | Unlocks a shared drag layer |
| Cross-tool / desktop-level DnD | Blocked until the desktop shell is an `ApplicationStage` |
| `VShortcut` drag on the stage drag layer | Migrate shortcut dragging |
| Carded "Desktop" as App content root | Re-root the App content pane onto a stage |
| Calendar PoC on `DragAndDropService` | Replace the PoC's own drag handling |
