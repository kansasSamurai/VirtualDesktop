# VirtualDesktop Requirements

This document captures design decisions, requirements, and rationale for the VirtualDesktop project. It serves as a reference for maintaining consistency across development sessions.

---

## Vision & Goals

VirtualDesktop is a Java-based virtual desktop application providing a consistent, cross-platform desktop metaphor independent of the underlying OS. Key goals:

- **Self-contained desktop environment** - Not a remote desktop, but a standalone application with integrated tools
- **Java 8 compatibility** - Maintains compatibility with legacy dependencies (with future Java 9+ version planned)
- **Integrated development tools** - BeanShell, Groovy console, database tools, charting, etc.
- **Extensibility** - Easy to add new tools via configuration or scripting

---

## Terminology

| Term | Usage | Context |
|------|-------|---------|
| **tool** | User-facing | UI labels, documentation, user messages |
| **vapp** | Internal only | Class names, code comments referring to internal artifacts |
| **VirtualAppSpec** | Internal | Base class for tool specifications |

**Rationale:** Users understand "tool" intuitively. "vapp" (virtual app) is an implementation detail.

---

## Features

- [BeanShell Integration](#beanshell-integration)
- [External Applications](#external-applications)
- [Tool Configuration](#tool-configuration)
- [UI/UX Guidelines](#uiux-guidelines)

### BeanShell Integration

#### Shared Interpreter Architecture

**Decision:** All BeanShell-enabled tools share a single interpreter instance via `BeanShellService` singleton.

**Rationale:**
- Memory efficiency (one interpreter vs N)
- Inter-tool communication via shared `global.*` namespace
- Console can inspect/modify any tool's state
- Variables persist across tool launches

**Constraints:**
- Console must be injected BEFORE interpreter creation (BeanShell's `setConsole()` is incomplete)
- `initializeEnvironment()` currently disabled due to console initialization conflicts
- Namespace collisions possible - scripts should use unique names or scoped objects

#### Script-Backed Tools

**Decision:** Tools can be defined entirely by BeanShell scripts via `SpecBeanShellScript`.

**Patterns:**
1. **Simple** - Script returns a JComponent directly (no lifecycle support)
2. **Advanced** - Script returns `this` object with lifecycle methods (recommended)

**Lifecycle methods** (all optional):
- `getContent()` - Returns the JComponent to display
- `configure(attrs)` - Called with configuration attributes from vapps-config.json
- `launch()` - Called when the tool window is displayed

**Example advanced pattern:**
```bsh
myTool() {
    // ... create UI ...
    getContent() { return panel; }
    configure(attrs) { /* handle config */ }
    launch() { /* on display */ }
    return this;
}
global.myTool = myTool();
return global.myTool;  // Return "this" for lifecycle support
```

---

### External Applications

#### Placeholder Panel Design

**Decision:** Tools that launch external processes or display in external JFrames show a consistent placeholder panel in the virtual desktop.

**Design elements:**
- Centered layout with 20px padding
- Bold title describing the tool type
- Italicized path/command information
- Blue underlined "click here" link for re-launch/bring-to-front
- Consistent styling across: `SpecHtmlViewer`, `ExternalAppSpec`, `AbstractExternalApp`

**Rationale:** Provides visual consistency and user affordance to re-access external content.

---

### Tool Configuration

#### vapps-config.json Structure

**Location:** `config/vapps-config.json`

**Key elements:**
- `menuStructure` - Defines menu organization and tool entries
- `desktopShortcuts` - Desktop icon definitions
- `attrs` - Custom attributes passed to `Configurable` tools

**Example entry with attributes:**
```json
{
  "class": "org.jwellman.virtualdesktop.vapps.SpecBeanShellScript",
  "title": "Scripted Notepad",
  "icon": "document176",
  "enabled": true,
  "attrs": {
    "scriptPath": "src/main/resources/org/jwellman/bsh/scripts/notepad.bsh",
    "defaultText": "Welcome text here..."
  }
}
```

---

### UI/UX Guidelines

#### Portable Tool Design

Tools should follow these guidelines for framework compatibility:

1. Do NOT set a Look and Feel (let framework handle it)
2. Do NOT use your own container (JFrame/JInternalFrame/JDialog)
3. Use JPanel for content (framework handles container placement)
4. Do NOT call `System.exit()` directly
5. Do NOT use modal/non-modal dialogs directly; use portable interface
6. OS-specific behavior must check for OS first and handle gracefully

---

## Architecture Decisions

### Docking Framework

**Decision:** All VirtualAppSpecs are dockable by default (as of Oct 2022).

**Implementation:** Uses Docking Frames library with configurable themes (ECLIPSE, FLAT, etc.).

---

## Open Questions / Future Considerations

- [ ] Configuration framework for main app (currently hardcoded)
- [ ] Desktop layout persistence format
- [ ] Dual-version strategy (Java 8 + Java 9+) implementation details

---

## Change Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-01-16 | Shared BeanShell interpreter | Memory efficiency, inter-tool communication |
| 2026-01-16 | "tool" vs "vapp" terminology | User-facing clarity |
| 2026-01-16 | Consistent placeholder panels | UX consistency for external tools |
| 2026-01-16 | Script lifecycle via returning "this" | Enable configure() and launch() callbacks |
