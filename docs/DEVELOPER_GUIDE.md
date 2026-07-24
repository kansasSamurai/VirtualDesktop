# Developer Guide: The Desktop Paradigm

This guide describes the **intended** desktop architecture for VirtualDesktop (jPad): what the application *means* by a desktop, which **interfaces** define that meaning, and which **implementation classes** realize it today (or will realize it next).

It is a narrative companion to:

- [`docs/analysis.html`](analysis.html) — strengths and leaks in the current code
- [`docs/paradigm.html`](paradigm.html) — contract list and gap map
- [`docs/REQUIREMENTS.md`](REQUIREMENTS.md) — view substitutability philosophy
- [`docs/features/DESKTOP.md`](features/DESKTOP.md) / [`docs/features/TASKBAR.md`](features/TASKBAR.md) — feature-level migration notes

Throughout, **interfaces** (and immutable model types) are the paradigm. **Implementation classes** are replaceable realizations. If a type is both today, that is called out as transitional debt.

---

## 1. What the desktop paradigm is

The core of the desktop paradigm is to:

1. **Define which tools we *can* launch**
2. **Give the user one or more ways to launch each tool**
3. **Manage the display of that tool once it is launched**

Everything else — wallpaper, taskbar chrome, Look and Feel — is either a *view* of that story or presentation layered beside it. **Docking** is different: for this product it is a *fundamental* layout rule (see §3.8), not an optional add-on.

A **tool** (user-facing word; historically “vapp” in code) is not “a `JInternalFrame`.” A tool is something the environment knows how to start, and for which it always hosts **at least one panel** (`JPanel` / `JComponent`) after launch.

That rule still holds for **external applications**: the OS process is started as a side effect, but the desktop still creates and manages a panel that represents that launch inside the environment. In-process UI is the common case, not a hard requirement of the paradigm.

---

## 2. Three verbs, three layers

It helps to keep three verbs separate:

| Verb | Question | Lives in |
| :--- | :--- | :--- |
| **Catalog** | What *can* we launch? | Definitions + catalog API |
| **Launch** | How does the user ask to start one? | Launch surfaces (views) → lifecycle service |
| **Host** | How do we show and manage an open one? | Runtime instances + windows + host/views |

Views never invent tools. They read catalog and/or open-instance state, then ask a **service interface** to open, focus, minimize, or close.

---

## 3. Interfaces (the paradigm contract)

These are the types a developer should program to. Some already exist as interfaces; some exist only as concrete classes today and should be extracted; some are planned.

### 3.1 What we can launch

**`ToolDefinition`** *(target model type; today approximated by config DTOs)*  
An immutable catalog entry: identity, title, icon key(s), how to instantiate (e.g. class name), optional attributes, flags such as “desktop shortcut only.”

**Existing implementations / stand-ins:** `VappConfig`, `DesktopShortcut`, `ExternalAppConfig` (JSON DTOs loaded by `ActionFactory`). These are good *data shapes* but are not yet unified behind one catalog type.

**`ToolCatalog`** *(target interface)*  
Queryable set of definitions: list all, find by id, walk menu structure, etc. No Swing types in the API.

**Existing stand-in:** `ActionFactory` — it *loads* the catalog correctly from JSON, but it *exposes* `List<DesktopAction>` (Swing), not definitions. Treat `ActionFactory` as a transitional loader, not the paradigm API.

### 3.2 How a tool is built when launched

**`ToolSpec`** *(target interface; closest ancestor: `VirtualAppSpec`)*  
The launch *recipe*: metadata (title, icon) plus **`getContent()`** returning the panel the environment will host. Optional capabilities stay as separate interfaces:

- **`Configurable`** *(existing interface)* — `configure(Map<String,String>)` after construction  
- **`LaunchAware`** *(existing interface)* — `launch()` for side effects such as starting an OS process  

A Spec is **not** the open window, **not** the docking framework, and **not** a Swing listener. It answers: “If you start me, here is my panel (and any extra launch work).”

**Existing implementation:** abstract class `VirtualAppSpec` implements `ToolSpec`. Docking session ownership lives on the host (`VirtualAppFrame` / `DockingSession` keyed by `toolId`); Spec authors that only need a panel never touch `DockingService`.

### 3.3 What “open” means at runtime

**`ToolInstance`** *(existing immutable model)*  
One running tool: id, type/definition reference, title, frame state, docking-related state, etc. This is the store’s notion of “open.”

**`ToolsState` / `AppState`** *(existing immutable models)*  
The application’s open-tool map and related UI preferences (e.g. window-list grouping).

**`AppStore`** *(existing implementation of the store idea; treat the *pattern* as the interface)*  
Dispatch actions → reducers → subscribers. Views and controllers subscribe; they do not mutate tool lists ad hoc.

### 3.4 Lifecycle operations

**`ToolService`** *(target interface)*  
Application-level operations: open (by definition id or spec), close, activate, minimize, restore (and later maximize if needed). Signatures should not require `JInternalFrame`, `JList`, or `JDesktopPane`.

**Existing stand-in:** concrete class `DesktopManager`. It *performs* lifecycle work, but it is not an interface, and it is entangled with Swing (see §5). Call sites that need “manage tools” should eventually depend on `ToolService`, not on `DesktopManager` by name.

### 3.5 Hosting chrome for one open tool

**`ToolWindow`** *(target interface)*  
The host chrome around one instance’s content: identity (`toolId`), show/hide, attach content, window decorations. The paradigm cares that content is hosted; it does not require that the host *be* a `JInternalFrame`.

**Existing implementation:** `VirtualAppFrame` extends `JInternalFrame`. Useful as the default Swing host; the frame class *is* the view leak until a thin `ToolWindow` interface sits in front of it.

### 3.6 Launch and management surfaces (views)

Views are **interfaces**. Concrete Swing widgets implement them. Controllers sit between store/service and views.

| View interface | Reads | User intents go to | Example implementations |
| :--- | :--- | :--- | :--- |
| **`DesktopView`** *(target)* | Shortcut layout (`DesktopState`) | open tool, move/select shortcut | Today: ad hoc `VDesktopPane` + `VShortcut` (not yet a view interface) |
| **`WindowListView`** *(existing)* | Open tools (`ToolsState` + `WindowListState`) | activate, close, grouping UX | `SmartGridWindowListView`, `JListWindowListView` |
| **`ToolBrowserView`** *(target, optional)* | Catalog ⊕ which tools are open | launch, or focus if already open | Today: Tools menu (approximate only) |

**`WindowListViewListener`** *(existing)* is the intent channel for the window list. Desktop and browser views should get analogous listener interfaces.

**Controllers** (e.g. existing `WindowListController`; planned `DesktopController`) are implementation classes that: subscribe to the store, push props into a view interface, and call `ToolService` on user intents. They should not be the place that owns docking frameworks or paints pixels.

### 3.7 Desktop layout (shortcuts as data)

**`DesktopState` / `Shortcut`** *(target model; described in DESKTOP.md)*  
Where shortcuts sit, what they point at (definition id), selection, later persistence. This is *data*, not a `JLabel`.

**Existing stand-in:** `DesktopShortcut` config DTO (definition only, no live positions). Runtime tiles are `VShortcut` widgets created in `App` with hardcoded coordinates.

### 3.8 Docking (fundamental; single-host is a special case)

From a generic “desktop toolkit” perspective, docking can sound optional. **For this product’s goals, docking is fundamental:** every open tool’s content is a dockable unit that lives in some host container (frame / workspace). Hosts are temporary — a panel may start in the frame created at open and later move into a workspace frame (or another host).

What an end user might experience as “no docking” is just the **degenerate special case**: a tool remains for its whole life in a single container and is never relocated. The UX looks like a classic one-tool-one-window desktop; the model still treats content as dockable and the container as a host, not as the identity of the tool.

**Close implication:** closing a tool means disposing that tool’s content (and deregistering it from docking) **wherever it currently lives**, then dropping the `ToolInstance` — not necessarily closing the original `JInternalFrame` that opened it. The durable correlation for lifecycle is **`toolId` → content**, with host location as current placement. A realizer/registry behind `ToolService` owns that map; `ToolInstance` stays Swing-free.

**Interfaces (existing SPI):** `DockingService`, `DockingProvider`, `DockingWorkspace`, `Dockable`, …

**Implementations:** Bibliothek adapters under `docking.impl.bibliothek`.

**Rule of thumb:** dockable / content identity should be stable and keyed by `toolId` (sessions owned beside or inside the host layer, not by `VirtualAppSpec`). Spec remains “panel + launch”; docking is “where that panel is arranged among hosts.”

---

## 4. A walk through a launch (intended flow)

1. **Catalog** — Configuration is loaded into `ToolDefinition` records held by a `ToolCatalog` implementation.
2. **Launch surface** — The user double-clicks a desktop shortcut (`DesktopView`), picks a menu item (`ToolBrowserView`), or activates a row in the taskbar if the tool is already open (`WindowListView`).
3. **Service** — The surface does not call `new Spec…()` itself for product logic. It asks **`ToolService.open(definitionId)`** (or equivalent).
4. **Recipe** — The service resolves the definition, instantiates the `ToolSpec` implementation, applies `Configurable` if present, and obtains `getContent()`.
5. **Host** — The service creates/obtains a `ToolWindow`, attaches the content, makes it visible, and records a **`ToolInstance`** in the store (`TOOL_OPENED`).
6. **Side effects** — If the spec is `LaunchAware`, `launch()` runs (e.g. external process). The managed panel still exists.
7. **Views update** — Subscribers (taskbar, later desktop) re-render from store state. They do not scrape a private list of frames to learn what is open.

Today steps 1–2 and 4–6 roughly happen, but through `DesktopAction` → `DesktopManager` → `VirtualAppFrame`, with the store updated as a *side projection* of Swing events. The guide’s end state makes the store + `ToolService` the spine.

---

## 5. Implementation reality (read this before changing code)

### What already matches the paradigm

| Paradigm piece | Implementation status |
| :--- | :--- |
| Capability interfaces | **`Configurable`**, **`LaunchAware`** — keep |
| Open-tool model | **`ToolInstance`** (includes `definitionId` / `iconKey`), **`ToolsState`**, **`WindowListState`** |
| Store pattern | **`AppStore`**, reducers, subscribers — keep |
| Substitutable open-list view | **`WindowListView`** + **`WindowListController`** — reference pattern for Desktop |
| Docking library boundary | **`DockingService` / `DockingProvider` SPI** — keep; fix *ownership* |

### What is transitional (concrete class standing in for an interface)

| Ideal interface | Current class | Why it’s transitional |
| :--- | :--- | :--- |
| `ToolCatalog` | `ActionFactory` | Loads JSON well; publishes Swing `DesktopAction`s |
| `ToolSpec` | `VirtualAppSpec` | Implements ToolSpec; docking owned by DockingSession on VirtualAppFrame |
| `ToolService` | `DesktopManager` | Performs lifecycle; also is Swing listener + frame roster owner |
| `ToolWindow` | `VirtualAppFrame` | Has `toolId`; *is* a `JInternalFrame` |
| Catalog entries | `VappConfig` / `DesktopShortcut` / … | Good DTOs; not one `ToolDefinition` API |

### About `DesktopManager` specifically

The issue is **not only** that `DesktopManager` implements `ListSelectionListener` and `InternalFrameListener`. Those listeners are symptoms.

The deeper problem is that `DesktopManager` conflates:

1. **Paradigm service** — create/show/manage tools (what should become `ToolService`)
2. **Swing host adapter** — talk to `JDesktopPane`, listen to internal frames, sync selection to a `JList`
3. **Authoritative registry** — `EventList<VirtualAppFrame>` as the live list of what is open (should become a realizer cache behind the store)

Listening to frames inside a Swing adapter implementation is fine. Making that adapter the type every feature programs to — and treating its frame list as source of truth — is what we want to unwind.

### Dual registry (today)

- **Swing:** `DesktopManager`’s list of `VirtualAppFrame`
- **Model:** `AppStore` → `ToolsState`

The window list view path reads open tools from the store (including icon keys) and routes activate/close through `ToolService`. It no longer reaches into DesktopManager frames for display.

---

## 6. Migration narrative (same six steps, in story form)

These steps are **ordered by dependency**. They are not six unrelated cleanups. Doing “store as sole registry” first fights the still-Swing-centric manager; doing catalog/service seams first makes later flips smaller.

### Step 1 — Catalog becomes data, Actions become adapters

Introduce **`ToolDefinition`** and a **`ToolCatalog`** interface. Teach the JSON loaders to populate the catalog. Keep `DesktopAction` (implementation) as a thin adapter: on click, call `ToolService.open(defId)` instead of being the catalog itself.

*You have finished this step when* menus and shortcuts can be described without saying “the list of Actions *is* the product catalog.”

### Step 2 — Lifecycle has an application interface

Declare **`ToolService`**. Make **`DesktopManager` the first implementation** (it may still create `VirtualAppFrame`s and listen to them internally). Point launch surfaces and controllers at the interface.

*You have finished this step when* feature code can manage tools without importing `DesktopManager` — even if the runtime object is still that class behind the interface.

### Step 3 — Spec means recipe + panel again ✅

Narrow **`VirtualAppSpec`** toward **`ToolSpec`**: title, icon, content, optional `Configurable` / `LaunchAware`. Relocate docking session ownership to a toolId-scoped host (beside or inside the tool window implementation). Leave the docking **SPI interfaces** as they are.

*Done when* a new tool author never touches `DockingService` to “just show a panel.”

### Step 4 — Open instances carry what views need ✅

Extend **`ToolInstance`** with definition id and icon key (or icon reference). Route activate/close through **`ToolService`**. Remove **`WindowListController`’s `frameCache`** as a display dependency.

*Done when* the taskbar can render and request lifecycle changes without asking `DesktopManager.getFrames()`.

### Step 5 — Desktop surface follows the taskbar pattern

Add **`DesktopState`** / shortcut model, a **`DesktopView`** interface + listener, a **`DesktopController`**, and reduce **`VShortcut`** to a pure rendering/input widget (implementation of the view, not the model).

*You have finished this step when* a second desktop look can ship by implementing `DesktopView` only.

### Step 6 — Store becomes the sole open-tool registry

Make **`ToolsState`** authoritative for what is open. The frame map becomes an internal realizer cache for the `ToolService` / Swing host implementation. Align close/hide semantics so UI and store cannot diverge (`HIDE_ON_CLOSE` vs `TOOL_CLOSED` is today’s sharp edge).

*You have finished this step when* no view or controller treats `EventList<VirtualAppFrame>` as the product’s list of open tools.

---

## 7. Practical rules of thumb

1. **If it names Swing in its public API, it is probably an implementation**, not the paradigm interface (`ToolService`, `ToolCatalog`, `ToolSpec`, view interfaces).
2. **If a view needs a `VirtualAppFrame` to know the title or icon**, the model is incomplete — fix `ToolInstance` (or the projection DTO), don’t grow another frame cache.
3. **Spec subclasses should look boring:** construct content, set title/icon, maybe implement `Configurable` / `LaunchAware`. Anything that sounds like window management or docking control belongs elsewhere.
4. **External tools are not exceptions to hosting:** always a managed panel; process launch is extra.
5. **Docking is fundamental** to this desktop; a single always-docked-in-one-container tool is a special case that may *look* like “no docking” to users. Do not model “tool identity” as “the frame that opened it.”
6. **Prefer evolving names in place** (`VirtualAppSpec` → implements `ToolSpec`) over a big-bang rewrite. Interfaces first; swap implementations when the seams exist.

---

## 8. Glossary bridge

| Guide term | Prefer in new code | Historical / UI |
| :--- | :--- | :--- |
| Tool | tool | vapp (code legacy) |
| Definition | `ToolDefinition` | `VappConfig`, shortcut config |
| Spec / recipe | `ToolSpec` | `VirtualAppSpec` |
| Open instance | `ToolInstance` | “frame” when you mean the window chrome |
| Lifecycle API | `ToolService` | `DesktopManager` (impl) |
| Host chrome | `ToolWindow` | `VirtualAppFrame` (impl) |
| Open-list UI | `WindowListView` | taskbar / window list |
| Shortcut surface | `DesktopView` | desktop pane + icons |

See also [`GLOSSARY.md`](../GLOSSARY.md) for project-wide terminology.

---

## 9. Where to go next

- Implementing seams: start with **Step 1 or Step 2** (catalog or `ToolService` interface) — both are low drama and unlock the rest.
- Desktop-only work: follow **DESKTOP.md** once Step 2 exists so shortcut open goes through `ToolService`.
- Deep docking Redux: wait until Step 3 (ownership) and stable `toolId` bridging — otherwise the model mirrors incomplete events.

When in doubt, ask: *Could I replace this Swing class with another implementation without changing the story in §1?* If no, you are editing an implementation detail that has leaked into the paradigm — extract an interface first.
