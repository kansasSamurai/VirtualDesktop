---
name: appstage
description: >-
  Guides use of org.katacode.appstage (ApplicationStage, PanelApplicationStage,
  toasts, modals, intra-stage DnD) when building or refactoring VirtualDesktop
  tools. Use when the user mentions ApplicationStage, PanelApplicationStage,
  appstage, stage overlays, stage toasts/modals, or tool-hosted layered UI.
---

# Application Stage (`org.katacode.appstage`)

**Read [docs/features/appstage/README.md](../../../docs/features/appstage/README.md) first.** It holds the layers, usage pattern, service APIs, drag point contract, demo map and deferred work. This file only adds the working rules.

## Rules

- Prefer services over layer getters; inject the stage, never discover it through the component tree
- Do **not** re-root the App `JFrame` or migrate desktop shortcuts onto a stage unless explicitly asked
- Wrap the stage with `createDefaultContent` only if Spec sizing requires it
- Reference demo: `SpecAppStageCalendarDemo` + `CalendarPoCPanel` (the calendar keeps its own internal layered drag; that is OK)

## Out of scope (do not implement unless asked)

- Anything under "Deferred work" in the README
- Full theming of nested PoCs (e.g. year calendar / calendar PoC grids)

## Checklist for a new stage-backed tool

- [ ] Java 8 only; project formatting conventions
- [ ] `PanelApplicationStage` as content; cards for primary views
- [ ] Stage/services injected; no tree discovery
- [ ] Toasts/modals via services; LAF colors for text surfaces
- [ ] DnD (if any) uses `DragAndDropService` + stage-relative updates
- [ ] Register Spec in `config/vapps-config.json` when opening from Tools
- [ ] Nested components may keep private layered panes until deliberately migrated
