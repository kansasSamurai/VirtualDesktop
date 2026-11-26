# Application Stage: roadmap

Toolkit reference: [README.md](README.md)

## Summary

| Item | Effort | Status |
|---|---|---|
| [Desktop on a stage](#desktop-on-a-stage-level-of-effort): 1. wrap the main window | 1–2 hours | Not started |
| Desktop on a stage: 2. let tools reach it | About half a day | Not started |
| Desktop on a stage: 3. harden the services | 1–2 days | Not started |
| Desktop on a stage: 4. new desktop features | Days each, scoped separately | Not started |

Other deferred work is listed in the [README](README.md#deferred-work).

## Desktop on a stage: level of effort

Assessed 2026-11-26.

**Goal:** put the main application's content in an `ApplicationStage`, so that tools can "bubble up"
notifications to the desktop if desired, among other benefits.

**The basic version, putting the desktop on a stage, is small and low-risk. The effort is in what is
built on it.** These are rough estimates of focused time, from reading the code, with nothing
prototyped.

| Stage of work | What you get | Effort |
|---|---|---|
| 1. Wrap the main window | The desktop lives in a stage | 1–2 hours, mostly testing |
| 2. Let tools reach it | Any tool can send a desktop toast | About half a day |
| 3. Harden the services | Reliable enough to depend on | 1–2 days |
| 4. New desktop features | Notification history, cross-tool drag and drop | Days each, scoped separately |

### 1. Wrap the main window

`App` builds one panel (the taskbar and desktop split pane) and passes it to `setContentPane`. That
panel would instead become a card:

```java
PanelApplicationStage stage = new PanelApplicationStage();
stage.addCard("desktop", p);
stage.showCard("desktop");
this.setContentPane(stage);
```

Internal frames and shortcut dragging stay inside the desktop pane, so they are not expected to
change. Nothing found in the Java code or scripts searched depends on `App`'s content pane. Docking
is attached to the `JFrame` itself, so it shouldn't care, but test it.

### 2. Let tools reach it

This is the main design decision. Specs are created with no arguments, so the stage can't simply be
passed to a constructor. Suggested: an opt-in interface, say `DesktopStageAware`, that
`DesktopManager` calls when opening a tool. That keeps the rule of injecting the stage rather than
looking for it. A tool then has two choices: its own stage for local messages, or the desktop's to
"bubble up". BeanShell scripts would need their own way in, such as a global variable.

### 3. Harden the services

They're fine for one tool, but a shared desktop asks more of them:

- **Background threads:** `showToast` doesn't switch to the Event Dispatch Thread, and background
  jobs are the likeliest senders.
- **Toast flood:** there's no limit on how many stack up.
- **Shared modal:** only one can be active, so one tool's modal silently dismisses another's.
- **Not truly modal:** the backdrop stops mouse clicks, but keyboard input and the menu bar still
  work, including Alt+X to exit.
- **Possible paint glitches:** the backdrop is marked opaque but has a translucent color, which can
  leave artifacts in Swing.
- **Popovers:** they don't move on resize.

### 4. Later features

Cross-tool drag and drop is the one item that is not straightforward. The current service only moves
a component and puts it back, so drop targets and data transfer would need designing. It also
restores the component without its layout constraints.

### Recommendation

If this is bumped in priority, do steps 1 and 2 plus the threading fix as one small deliverable, for
under a day. That gives desktop notifications from any tool, which should show whether the rest is
worth it. Leave desktop modals until step 3. It could also serve Agent Auditor early, if its work
runs in the background and should announce when it finishes.
