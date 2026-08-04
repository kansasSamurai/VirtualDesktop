---
name: appstage
description: >-
  Guides use of org.katacode.appstage (ApplicationStage, PanelApplicationStage,
  toasts, modals, intra-stage DnD) when building or refactoring VirtualDesktop
  tools. Use when the user mentions ApplicationStage, PanelApplicationStage,
  appstage, stage overlays, stage toasts/modals, or tool-hosted layered UI.
---

# Application Stage (`org.katacode.appstage`)

Java 8 Swing toolkit: an embeddable **stage** hosts tool content on a layered pane and exposes micro-services for overlays. Tools talk to **services**, not `JLayeredPane` integers.

## When to use

- New or existing VirtualDesktop `VirtualAppSpec` / tool UI needs toasts, modals, popovers, or intra-tool drag ghosts
- Content should swap via cards without tearing down overlay layers
- Standalone harness and in-desktop tool should share the same stage contract via constructor injection

## Canonical host (v1)

Use **`PanelApplicationStage`** (`extends JPanel implements ApplicationStage`) as the tool content root (or wrap it with `createDefaultContent` only if Spec sizing requires it).

```text
PanelApplicationStage
  └── JLayeredPane
        ├── DEFAULT  → CardLayout base (addCard / showCard)
        ├── PALETTE  → modal backdrop (pass-through when empty)
        ├── DRAG     → drag ghosts (pass-through when empty)
        └── POPUP    → toasts, modal chrome, popovers
```

Do **not** re-root the App `JFrame` or migrate desktop shortcuts onto a stage unless explicitly asked.

## Tool pattern

1. Construct `PanelApplicationStage`.
2. Build main UI as a card; inject `ApplicationStage` (or services) into child panels.
3. `addCard("main", panel)` then `showCard("main")`.
4. `setContent(stage)` on the Spec (stage is already a `JPanel`).
5. Prefer services: `getNotificationService()`, `getModalService()`, `getDragAndDropService()`.
6. Layer getters (`getDragLayer()`, etc.) are escape hatches only.

Reference demo: `SpecAppStageCalendarDemo` + `CalendarPoCPanel` (calendar keeps its own internal layered drag; that is OK).

## Services (prefer these)

| Service | Role |
|--------|------|
| `NotificationService` | Toasts on POPUP; LAF-aware surfaces via internal `LafColors` |
| `ModalService` | Backdrop on PALETTE + content on POPUP; `dismissActiveModal()` |
| `DragAndDropService` | Promote / move / demote on **this stage only** |

### DnD point contract

- `beginDrag(subject, startPointOnSubject)` — grab point in **subject** coords
- `updateDrag` / `endDrag` — points in **stage** coords (`stage.convertToStagePoint(...)`)
- Intra-stage only; do not invent cross-tool drop targets

### Overlay UI colors

Text overlays must use LAF surfaces (probe `JTextField` / `UIManager`), not hardcoded light-theme greys. Reuse or mirror `org.katacode.appstage.internal.LafColors`. Semantic toast accents (success/warn/error) may stay fixed.

## Injection over discovery

Pass `ApplicationStage` (or a service) into constructors. Do **not** walk parents / `getWindowAncestor` to find a layered pane (`StageResolver`-style) for tool code.

## Out of scope (do not implement unless asked)

- `JFrameStage` / `JInternalFrameStage` / rich `MockStage`
- Cross-tool or desktop-level DnD (needs desktop as a shared `ApplicationStage`)
- Migrating `VShortcut` drag onto a stage
- App content-pane re-root onto a stage “Desktop” card
- Full theming of nested PoCs (e.g. year calendar / calendar PoC grids)

## Checklist for a new stage-backed tool

- [ ] Java 8 only; project formatting conventions
- [ ] `PanelApplicationStage` as content; cards for primary views
- [ ] Stage/services injected; no tree discovery
- [ ] Toasts/modals via services; LAF colors for text surfaces
- [ ] DnD (if any) uses `DragAndDropService` + stage-relative updates
- [ ] Register Spec in `config/vapps-config.json` when opening from Tools
- [ ] Nested components may keep private layered panes until deliberately migrated

## More detail

- API notes and deferred roadmap: [reference.md](reference.md)
- Code: `virtualdesktop-java8/src/main/java/org/katacode/appstage/`
- Dev notes: `docs/DEVELOPER_GUIDE.md` §9 (Application stage)
