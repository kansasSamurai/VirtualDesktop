# Icon System

## Overview

This document is a **current-state audit**, not a target-architecture proposal like
`DESKTOP.md`/`TASKBAR.md`. It exists because icon-related bugs (most recently: internal
vapp frames falling back to a generic "coffee cup" icon, then showing the wrong *size*
icon) kept requiring a fresh archaeology dig through the codebase to understand. This
doc is that dig, written down.

The short version: there is no single "icon system." There is one shared, unguarded
key→`Icon` registry (`DSP.Icons`), fed by **four independent registration mechanisms**
plus a **fifth, partially-adopted semantic-size wrapper**, plus a **sixth, completely
unrelated legacy icon system** scoped to the file manager. None of this is on fire, but
nothing enforces that these pieces agree with each other, and at least two of them
document/describe naming conventions the others don't use.

---

## Core Machinery — and it isn't even in this repo

`DSP` (`org.jwellman.dsp.DSP`) and the classes backing `DSP.Icons` are **not** part of
VirtualDesktop — they live in the sibling `swing-utils` project
(`swing-utils/src/main/java/org/jwellman/dsp/`) and are consumed as a Maven dependency.
Anyone auditing "the icon system" by grepping only `VirtualDesktop/` will miss the root
of it.

```java
// swing-utils: org.jwellman.dsp.DSP
public final static IconRepository Icons = new IconRepositoryImpl();
```

`IconRepositoryImpl` (`swing-utils/.../dsp/icons/IconRepositoryImpl.java`) holds three
flat maps:

```java
Map<String, IconSpecifier> registry;          // key -> "how to render this"
Map<String, IconProvider>  providerRegistry;   // provider name -> renderer
Map<String, Icon>          repository;         // key -> rendered Icon (lazy cache)
```

- `register(key, IconSpecifier)` — blind `Map.put()`. No collision detection; a later
  registration for an existing key silently overwrites the earlier one.
- `registerProvider(name, IconProvider)` — registers a renderer (e.g. "FontAwesome",
  "GoogleMaterial", "Directory") by name.
- `getIcon(key)` — looks up the `IconSpecifier`, hands it to the matching `IconProvider`,
  caches the rendered `Icon` in `repository`. **Does not null-check a missing key** —
  `registry.get(namespace)` can return `null`, and the very next line calls
  `s.getProvider()` on it, throwing a bare `NullPointerException`. This is why every call
  site in `ActionFactory` wraps `DSP.Icons.getIcon(...)` in `try { } catch (Exception ex)`
  — they're catching an NPE, not a documented "not found" signal.

`IconSpecifier` (`swing-utils/.../dsp/icons/IconSpecifier.java`) is a plain value class:
`provider`, `iconName`, `specifier`, `size`, `foreground`, `background`. One dead field:
the constructor takes a `specifier` parameter but never assigns `this.specifier` — the
field is permanently `null` regardless of what's passed in.

`IconProvider` is the renderer interface. Three implementations exist, all registered in
`App.createTheme()`:

| Provider name | Class | Renders |
|---|---|---|
| `"FontAwesome"` | `org.jwellman.dsp.FontAwesomeIconProvider` | JIconFont FontAwesome glyphs |
| `"GoogleMaterial"` | `org.jwellman.dsp.GoogleMaterialIconProvider` | JIconFont Material Design glyphs |
| `"Directory"` | `org.jwellman.dsp.DirectoryIconProvider` | SVG/PNG/JPG/GIF/BMP files from a classpath directory |

---

## Four Independent Registration Mechanisms

All four write into the *same* flat `DSP.Icons` namespace described above.

| # | Mechanism | Entry point | Key style | Example keys |
|---|---|---|---|---|
| 1 | **Filesystem auto-discovery** | `DirectoryIconProvider.discoverAndRegisterIcons()`, invoked from `App.createTheme()` | filename *is* the key; registers both a semantic-size key and a pixel-size key per file | `home156-small`, `home156-16` |
| 2 | **Hardcoded one-offs** | 8 `DSP.Icons.register("jpad.xxx", ...)` calls in `App.java` (~line 506-513) | dot-namespaced, single fixed size, always FontAwesome | `jpad.java`, `jpad.eye`, `jpad.cog` |
| 3 | **Config-driven, two-layer** | `IconRegistryLoader.load()`, invoked from `App.createTheme()`, reading `config/icon-theme.json` + `config/providers/{provider}-registry.json` | plain semantic keys × a `sizes` map (currently `small`=18px, `large`=48px) | `home-small`, `diagram-large` |
| 4 | **Legacy file-manager theme** | `fx.filemanager.IconTheme` | `.jar`-packaged theme, `.properties` index, mime-type resolution | n/a — doesn't touch `DSP.Icons` at all |

### 1 — Filesystem auto-discovery (`DirectoryIconProvider`)

```java
// App.createTheme()
DirectoryIconProvider directoryProvider =
    new DirectoryIconProvider("org/jwellman/virtualdesktop/images/global_ui");
DSP.Icons.registerProvider("Directory", directoryProvider);

directoryProvider.discoverAndRegisterIcons("Directory",
    IconSize.SMALL, IconSize.MENU, IconSize.TOOLBAR, IconSize.LARGE, IconSize.XLARGE);
```

`discoverAndRegisterIcons()` recursively scans
`org/jwellman/virtualdesktop/images/global_ui/` on the classpath for `.svg`/`.png`/`.jpg`/
`.gif`/`.bmp` files. Each discovered filename (minus extension) becomes an icon name —
e.g. a file called `home156.svg` yields the icon name `"home156"`. For every discovered
name × every requested `IconSize`, it registers **two** keys:

- a semantic key: `"home156-small"` (via `IconSize.toThemeKey()`)
- a pixel key: `"home156-16"` (for backward compatibility, per the method's own doc
  comment)

This only works when running from the filesystem (`dirURL.getProtocol().equals("file")`)
— auto-discovery is explicitly a no-op when running from a packaged JAR, per a
`System.out.println` warning in the source.

**Origin/intent (per project author, not derived from code):** this was a first cut,
put in place to give the overall desktop a working set of icons before the
`FontAwesome`/`GoogleMaterial` providers and the config-driven path (mechanism #3) were
finished. It was never intended to be the primary delivery mechanism for desktop-level
icons — that role has since moved to `IconRegistryLoader`/`icon-theme.json`. It may still
prove useful going forward for tools/apps that ship bundled with their own icon assets,
just not as the default path for the desktop's own icon set. See "Recommended
Consolidation Path" below.

### 2 — Hardcoded one-offs (`App.createTheme()`)

```java
Color iconColor = Color.lightGray;
DSP.Icons.register("jpad.java", new IconSpecifier("FontAwesome", "COFFEE", 18, null, iconColor, Color.white));
DSP.Icons.register("jpad.eye", new IconSpecifier("FontAwesome", "EYE", 18, null, iconColor, Color.white));
DSP.Icons.register("jpad.bsh_class_browser", new IconSpecifier("FontAwesome", "EYE", 18, null, iconColor, Color.white));
DSP.Icons.register("jpad.calendar", new IconSpecifier("FontAwesome", "CALENDAR", 18, null, iconColor, Color.white));
DSP.Icons.register("jpad.cog", new IconSpecifier("FontAwesome", "COG", 18, null, iconColor, Color.white));
DSP.Icons.register("jpad.leaf", new IconSpecifier("FontAwesome", "LEAF", 18, null, iconColor, Color.white));
DSP.Icons.register("jpad.check", new IconSpecifier("FontAwesome", "CHECK", 18, null, iconColor, Color.white));
DSP.Icons.register("jpad.clock", new IconSpecifier("FontAwesome", "CLOCK_O", 14, null, Color.white, Color.white));
```

Eight one-shot registrations, single fixed pixel size each (mostly 18px, one 14px), no
`-small`/`-large` suffix at all. `"jpad.java"` (a literal FontAwesome `COFFEE` glyph) is
the fallback icon `DesktopManager` applies to any `VirtualAppSpec` whose `getIcon()`
returns `null` when building a frame — this is the "coffee cup" icon from the bug this
doc originated from.

### 3 — Config-driven, two-layer (`IconRegistryLoader`)

```java
// IconRegistryLoader.load(), invoked from App.createTheme()
for (IconEntry entry : config.getIcons()) {
    Map<String, String> registry = registryCache.computeIfAbsent(entry.getProvider(), IconRegistryLoader::loadProviderRegistry);
    String iconName = registry.get(entry.getKey());
    Color color = resolveColor(entry.getColor(), config.getColorRoles());
    for (Map.Entry<String, Integer> sizeEntry : sizes.entrySet()) {
        String dspKey = entry.getKey() + "-" + sizeEntry.getKey();
        DSP.Icons.register(dspKey, new IconSpecifier(providerName, iconName, sizeEntry.getValue(), null, color, color));
    }
}
```

Two-layer by design (per the class's own doc comment): `config/icon-theme.json` defines
*which* provider handles each semantic key plus the size table;
`config/providers/{provider}-registry.json` maps each semantic key to that provider's
actual icon/glyph name. Current `config/icon-theme.json`:

```json
{
  "sizes": { "large": 48, "small": 18 },
  "colorRoles": { "desktop": "desktopIcon.foreground" },
  "icons": [
    { "key": "home", "provider": "FontAwesome", "color": "desktop" },
    { "key": "diagram", "provider": "FontAwesome", "color": "desktop" },
    { "key": "terminal", "provider": "GoogleMaterial", "color": "desktop" }
    /* ... */
  ]
}
```

and `config/providers/fontawesome-registry.json` maps `"diagram"` → `"PICTURE_O"`, etc.

**This is the mechanism that actually backs `config/vapps-config.json` today** — the real
icon keys in that file (`"home"`, `"diagram"`, `"terminal"`, `"calendar"`, ...) are exactly
the `key` values declared in `icon-theme.json`. `ActionFactory` (see below) is the
consumer.

### 4 — Legacy file-manager theme (`fx.filemanager.IconTheme`)

A wholly separate, vendored subsystem (`fx/filemanager/IconTheme.java`, header comment:
*"XionDE.fm - XionDE File Manager, Copyright (C) 2007 XionDE TEAM"*, GPL). Loads
`.jar`-packaged icon theme files, resolves icons by MIME type via a `.properties` index,
caches rendered `ImageIcon`s in a `Vector`/`Properties` pair. **Does not use `DSP.Icons`
at all** — it's a closed system scoped entirely to the `fx.filemanager` package (the
XionDE-derived file manager tool). Easy to mistake for part of the same icon
infrastructure because of the shared name "IconTheme," but architecturally unrelated to
everything else in this document.

---

## A Fifth Layer: Semantic Sizes (Partially Adopted)

`org.jwellman.virtualdesktop.theme.IconSize` is an enum (`SMALL`, `MENU`, `TOOLBAR`,
`LARGE`, `XLARGE`) with default pixel mappings (16/18/24/48/64) documented directly in
its Javadoc, explicitly framed as the "good" replacement for hardcoded pixel suffixes:

```java
// Before (bad - hard-coded pixel size):
Icon icon = DSP.Icons.getIcon("home156-16");

// After (good - semantic size):
Icon icon = ThemeManager.getIcon("home156", IconSize.SMALL);
```

`ThemeManager.getIcon(String, IconSize)` (`theme/ThemeManager.java:254`) is a thin
wrapper — it still just builds `name + "-" + size.toThemeKey()` and calls
`DSP.Icons.getIcon(key)`. Same flat registry, same NPE-on-miss behavior, no added
validation or caching beyond what `IconRepositoryImpl` already provides.

**`ActionFactory` predates this and never adopted it.** It builds keys via raw string
concatenation (`iconKey + "-small"`, `iconKey + "-large"`), bypassing `IconSize` entirely
— which also means `ActionFactory` has no way to request a `MENU`, `TOOLBAR`, or
`XLARGE` icon even though the enum supports them.

There's also a quiet numeric mismatch: `icon-theme.json`'s `"small"` size is **18px**,
but `IconSize.SMALL`'s documented default is **16px** (18px is actually `IconSize.MENU`'s
default). The JSON's size *names* and the enum's size *names* are not the same axis —
they just happen to share the literal string `"small"`.

---

## Consumer: `ActionFactory` (the code that actually matters for desktop/menu icons)

`org.jwellman.virtualdesktop.vapps.ActionFactory` is where all of the above gets turned
into the icons users actually see on desktop shortcuts and Tools-menu items. It reads
`config/vapps-config.json` and, per entry, calls `DSP.Icons.getIcon(iconKey + "-small")`
/ `"-large"`, catching the (NPE-driven) miss case and falling back to `"add196-small"` /
`"add196-large"`. The resulting `Icon`s are stored on the `DesktopAction` (a Swing
`Action`) via `Action.SMALL_ICON` / `Action.LARGE_ICON_KEY` — **not** on the
`VirtualAppSpec` that will eventually back the launched tool's `JInternalFrame`.

Two historical bugs lived exactly at this seam (both already fixed as of this writing):

1. **Internal vapp frames always showed the generic "coffee cup" (`jpad.java`) icon,
   regardless of launch path (desktop shortcut or Tools menu).** `DesktopAction.run()`
   instantiated the `VirtualAppSpec` and handed it straight to `DesktopManager` without
   ever copying the `Action`'s `SMALL_ICON`/`LARGE_ICON_KEY` onto it. Since
   `VirtualAppSpec.getIcon()` defaults to `null`, `DesktopManager.createVApp()` always
   fell back to `jpad.java`. `ExternalAppAction` (used only for `external-apps.json`
   entries) *did* do this transfer in its own `run()` override, which is why external
   tools looked correct and made the bug look like a "desktop vs. menu" difference at
   first — it wasn't; it was "external app vs. internal vapp." Fixed by adding a shared
   `DesktopAction.applyIconToSpec(VirtualAppSpec)` helper, called from the base `run()`
   and reused by `ExternalAppAction`.

2. **After fix #1, desktop-shortcut-launched frames showed the correct icon but at the
   wrong (large) size.** `ActionFactory.registerDesktopShortcut()` only ever loaded and
   set `Action.LARGE_ICON_KEY` (correct for the 32px desktop tile) — it never loaded a
   `-small` variant the way `registerVapp()` (Tools menu) already did. `applyIconToSpec()`
   prefers `SMALL_ICON` and only falls back to `LARGE_ICON_KEY` when small isn't
   available, so desktop-shortcut vapps fell through to the large icon every time. Fixed
   by mirroring the `-small` load into `registerDesktopShortcut()`.

Both fixes only patched the *Action → Spec → Frame* handoff. Neither touched any of the
four registration mechanisms above — those were already producing correct icons; the
`Icon` objects just weren't reaching the frame.

### `registerExternalApp()` diverged from the other two consumers — fixed

`ActionFactory.registerExternalApp()` (`ActionFactory.java:260-294`, backing
`config/external-apps.json`) used to load icons differently from `registerVapp()` and
`registerDesktopShortcut()` in two ways:

- It falls back to a **different** generic icon key — `"winking18-large"`/
  `"winking18-small"` — instead of the `"add196-*"` pair the other two consumers use for
  the same "icon key not found" case. This difference is intentional/pre-existing and was
  left as-is.
- Its two `DSP.Icons.getIcon(...)` calls were **not** individually wrapped in `try/catch`,
  unlike every other call site in this file — it did a plain `null` check instead. Since
  `IconRepositoryImpl.getIcon()` throws rather than returning `null` for an unregistered
  key (Deficiency #2 below), the `winking18` fallback branch was **unreachable**: a
  genuinely missing icon key in `external-apps.json` threw an uncaught
  `NullPointerException` that propagated to the method's own outer `catch`, silently
  aborting registration of the **entire external app** — no desktop shortcut, no menu
  item — instead of falling back to a generic icon the way `registerVapp()`/
  `registerDesktopShortcut()` do.

**Fixed:** both the primary lookup and the `winking18` fallback lookup are now each
wrapped in their own `try/catch`, mirroring the defensive pattern already used in
`registerVapp()`/`registerDesktopShortcut()`. A missing icon key now logs a warning and
falls back to `winking18-*` as originally intended, instead of aborting the whole
external app's registration.

---

## Architectural Assessment: Core Mechanism vs. Usage

It's worth separating "is `DSP.Icons` a sound piece of architecture" from "is it being
used consistently" — they're different questions with different answers.

**The core mechanism holds up fine.** The `IconProvider` abstraction (pluggable renderers
per source — FontAwesome glyphs, Material glyphs, filesystem images), `IconSpecifier` as
a declarative "recipe" registered under a string key, and lazy-render-with-cache-on-first-
access are all reasonable, unremarkable choices for a service-locator-style registry at
this app's scale. Nothing about that shape needs to change.

Two things genuinely are weaknesses in `DSP`/`IconRepositoryImpl` itself (the
`swing-utils` layer), not just in how VirtualDesktop uses it:

1. **The missing-key contract is bad API design** — throwing a bare
   `NullPointerException` instead of returning `null` or a documented "not found" result
   forces every caller into defensive `catch (Exception)`, and it's what makes the
   `registerExternalApp()` bug (Deficiency #9 below) possible: a caller that *didn't*
   defend against it just silently loses the whole registration.
2. **`register()` has no collision guard** — a blind `Map.put()`. That's a defensible
   simplification for a registry with one disciplined writer, but this registry now has
   (at the application level) four uncoordinated writers.

Everything else in the deficiency list below — four independent registration mechanisms
with two different naming vocabularies, `ActionFactory` never adopting the newer
`IconSize`/`ThemeManager` layer, inconsistent try/catch discipline between
`registerVapp()` and `registerExternalApp()`, `fx.filemanager.IconTheme` being an
unrelated same-named legacy system, the stale doc — none of that is `DSP`'s fault. That's
organic growth at the VirtualDesktop application layer: each new icon-adjacent feature
(menu icons, desktop shortcuts, external apps, filesystem auto-discovery, semantic
theming) got its own registration code path bolted on over time, with no single place
that owns "here is the catalog of valid icon keys." The registry never pushed back
because it isn't designed to.

**Takeaway:** keep the `DSP.Icons` shape as-is; consider hardening its two contract
weaknesses now that it has multiple writers. If the *usage* side is ever worth de-
scattering, the highest-leverage move is consolidating registration through one path —
mechanism #3 (`IconRegistryLoader`) is already the "modern" one — rather than redesigning
`DSP` itself.

---

## Recommended Consolidation Path (not yet actioned)

Raised in discussion: should `ActionFactory` become the central source of truth for
icons? Short answer: no — not because the idea is wrong, but because `ActionFactory` is
structurally a *consumer* of `DSP.Icons` (it only ever calls `getIcon()`, never
`register()`). It isn't a candidate to define what icons exist; it's a candidate to be a
clean, uniform *client* of whatever already defines them.

**Who should own "what icons exist":** mechanism #3 (`icon-theme.json` +
`IconRegistryLoader`) is already the intended source of truth, and its shape is sound —
theme/config declares the semantic key, provider, and size table; the registry renders
and caches on demand. The other two registration mechanisms predate it:

- The hardcoded `jpad.*` calls in `App.createTheme()` are internal fallback/status icons
  the desktop manager depends on directly — a reasonable case for staying literal/code-
  level rather than moving to config, since they're framework-owned, not
  config-selectable.
- `DirectoryIconProvider` auto-discovery (mechanism #1) was a first cut, added to give the
  desktop *some* working icon set before the `FontAwesome`/`GoogleMaterial` providers and
  the config-driven path were finished — not intended as the primary desktop-level
  delivery mechanism, and superseded by mechanism #3 for that role. It likely still has a
  legitimate future use for tools/apps that ship bundled with their own icon assets
  (drop-in icon files rather than a config entry), just not as the default path for the
  desktop's own icon set. If it's kept for that narrower purpose, its scope and intent
  should probably be documented at the call site so a future reader doesn't mistake it for
  the primary mechanism (as this document initially did).

**What should change in `ActionFactory`:** nothing about *authority* — but
`registerVapp()`, `registerDesktopShortcut()`, and `registerExternalApp()` currently
hand-roll three near-identical copies of "look up `key-size`, catch a miss, fall back to
a default key, catch that too, log." (Deficiency #9 was exactly this duplication drifting
out of sync.) Collapsing that into one shared helper — e.g.
`resolveIcon(String key, IconSize size, String fallbackKey)` — would remove the
duplication without granting `ActionFactory` any new authority; it would just be a
disciplined client.

**On collision handling (author's stated direction, not yet implemented):** warn-only,
no overwrite, is a reasonable conservative default — it surfaces bad configuration
without changing runtime behavior. One thing worth deciding explicitly before
implementing: today `IconRepositoryImpl.register()` is silently **last-write-wins**, and
the current startup order in `App.createTheme()` is `jpad.*` hardcoded → `Directory`
auto-discovery → `IconRegistryLoader` (config-driven) last. Switching to "warn and keep
the first registration" flips this to **first-write-wins** — which seems like the safer
default, since it means a config-driven collision can't silently clobber one of the
`jpad.*` icons `DesktopManager` depends on directly. Worth confirming that's the intended
semantics (rather than an incidental side effect of whichever `Map` operation is used to
implement the guard) when this is picked up.

---

## Known Design Deficiencies

Each item is tagged by where the problem actually lives: **[DSP core]** (in
`swing-utils`, affects any consumer) or **[App usage]** (organic growth in how
VirtualDesktop calls into `DSP.Icons`).

1. **[App usage, exposed by a DSP contract gap] Unguarded shared namespace.** Four
   independent mechanisms (`DirectoryIconProvider` auto-discovery, hardcoded `jpad.*`,
   `IconRegistryLoader` config-driven, plus whatever ad-hoc `DSP.Icons.register()` calls
   exist elsewhere) all write into one `Map<String, IconSpecifier>` with no collision
   detection. A later registration silently overwrites an earlier one. This hasn't caused
   a visible bug yet only because the naming conventions (`home156` vs `home` vs
   `jpad.foo`) don't currently overlap — nothing enforces that they won't. The missing
   guard is a `DSP`-level gap; having four uncoordinated writers to expose it is an
   application-level choice.

2. **[DSP core] Missing-key lookups throw a bare `NullPointerException`**, not a
   documented "not found" result (`IconRepositoryImpl.getIcon()`). Every call site that
   might hit a missing key has to catch generic `Exception`, which also silently
   swallows genuinely unexpected failures.

3. **[App usage] Two unrelated "small"/"large" size vocabularies.** `icon-theme.json`'s
   literal size names (`small`=18px, `large`=48px) and `IconSize`'s semantic names
   (`SMALL`=16px, `MENU`=18px, `TOOLBAR`=24px, `LARGE`=48px, `XLARGE`=64px) look like the
   same concept and partially share pixel values by coincidence, but are two
   independently-defined tables.

4. **[App usage] `IconSize`/`ThemeManager.getIcon()` is a documented "preferred" API
   that older callers (`ActionFactory`) never migrated to.** New code and old code build
   the same kind of key two different ways.

5. **[App usage] Stale documentation.** `docs/vapps-config.md`'s example icon keys
   (`home156`, `laptop123`, `download171`) describe mechanism #1's
   (`DirectoryIconProvider`) auto-discovered filename convention, but the actual shipped
   `config/vapps-config.json` uses mechanism #3's (`IconRegistryLoader`) plain semantic
   keys (`home`, `diagram`, `terminal`). Anyone following that doc today would reference
   icon keys that don't exist in the current config.

6. **[App usage] `config/vapps-config.json`'s `"rubbish1"` desktop-shortcut icon key is
   not declared anywhere in `config/icon-theme.json`.** It silently falls through
   `ActionFactory`'s `add196` fallback rather than the icon presumably intended for the
   Trash shortcut.

7. **[App usage] `fx.filemanager.IconTheme` shares a name with, but no code or data
   with, the `DSP.Icons` system** — a source of confusion for anyone searching the
   codebase for "IconTheme" expecting one system.

8. **[DSP core, minor] `IconSpecifier.specifier` field is dead** — the constructor
   parameter is accepted but never assigned, so `getSpecifier()` always returns `null`.

9. **[App usage, triggered by DSP core Deficiency #2 — FIXED] Two different fallback
   icons for the same "icon key not found" case, one of which was unreachable.**
   `registerVapp()`/`registerDesktopShortcut()` fall back to `add196-*`;
   `registerExternalApp()` falls back to `winking18-*`. It previously lacked the
   individual `try/catch` per lookup that the other two consumers use, so — since a
   missing key *throws* rather than returning `null` (Deficiency #2) — its fallback
   branch could never actually run; a missing icon in `external-apps.json` aborted
   registration of the whole external app silently. Fixed by wrapping both lookups
   individually, matching the other two consumers. The `add196` vs. `winking18` naming
   split itself was left as-is (intentional/pre-existing, not a bug). See
   "`registerExternalApp()` diverged..." above for the full trace.

---

## Related Files

**`swing-utils` (sibling repo):**
- `org.jwellman.dsp.DSP` — the `DSP.Icons` singleton
- `org.jwellman.dsp.icons.IconRepository` / `IconRepositoryImpl` — the registry
- `org.jwellman.dsp.icons.IconSpecifier` — per-icon render spec
- `org.jwellman.dsp.icons.IconProvider` — renderer interface

**VirtualDesktop:**
- `org.jwellman.dsp.FontAwesomeIconProvider`, `GoogleMaterialIconProvider`,
  `DirectoryIconProvider` — the three registered providers
- `org.jwellman.virtualdesktop.App` — `createTheme()`, the startup sequence that wires
  all four registration mechanisms together
- `org.jwellman.virtualdesktop.desktop.IconRegistryLoader` — config-driven registration
  (mechanism #3)
- `org.jwellman.virtualdesktop.theme.IconSize`, `ThemeManager` — the semantic-size layer
- `org.jwellman.virtualdesktop.vapps.ActionFactory` — reads `vapps-config.json` /
  `external-apps.json`, the main consumer that turns icon keys into `Action` properties
- `org.jwellman.virtualdesktop.vapps.DesktopAction`, `ExternalAppAction` — the
  Action → `VirtualAppSpec` → frame icon handoff (site of the two fixed bugs)
- `org.jwellman.virtualdesktop.DesktopManager` — applies `spec.getIcon()` (or the
  `jpad.java` fallback) to the `JInternalFrame`
- `fx.filemanager.IconTheme` — unrelated legacy system, file-manager-scoped only
- `config/icon-theme.json`, `config/providers/*.json` — mechanism #3's data
- `config/vapps-config.json`, `config/external-apps.json` — the actual icon keys used by
  desktop shortcuts, Tools-menu entries, and external app shortcuts
- `docs/vapps-config.md` — **stale**; describes mechanism #1's key convention, not the
  one the current config actually uses (see Deficiency #5)
