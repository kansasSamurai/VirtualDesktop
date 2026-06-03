# BeanShell Startup Sequence and Architecture

## Overview

VirtualDesktop integrates BeanShell as a shared scripting environment accessible from the console vapp. A single `Interpreter` instance is shared across the entire application via `BeanShellService`, allowing all BeanShell-enabled tools to inspect and interact with the same namespace.

---

## Startup Sequence

```
App launches
  └─> SpecBeanShell constructor (opened as a vapp)
        ├─> Creates JConsole (org.jwellman.bsh.JConsole)
        ├─> BeanShellService.get().setConsole(console)  // must precede getInterpreter()
        ├─> BeanShellService.get().getInterpreter()     // lazy-creates Interpreter with console
        ├─> new Thread(interpreter, "BeanShell Interpreter").start()  // starts REPL
        └─> SwingUtilities.invokeLater(this::run)
              └─> interpreter.source("src/main/resources/jvdClassBrowser.bsh")
```

The script path is relative to the working directory at runtime (`src/main/resources/jvdClassBrowser.bsh`). This works on Windows; Linux requires a different path strategy (see `SpecBeanShell.java` line 75 comment).

---

## Key Files

| File | Role |
|------|------|
| `virtualdesktop-java8/src/main/java/org/jwellman/virtualdesktop/vapps/SpecBeanShell.java` | Vapp entry point; wires console → service → script |
| `virtualdesktop-java8/src/main/java/org/jwellman/virtualdesktop/bsh/BeanShellService.java` | Singleton; owns the shared `Interpreter` |
| `virtualdesktop-java8/src/main/resources/jvdClassBrowser.bsh` | Primary startup script; sets up the `jvd` namespace |
| `virtualdesktop-java8/src/main/java/bsh/util/JClassBrowser.java` | Custom class browser (on-demand only) |

---

## `jvdClassBrowser.bsh` — What It Sets Up

The script defines two things at the top level, then executes two initialization lines:

### 1. `jvdClassBrowser()` function (lines 27–129)

Defines a BeanShell closure that, when called, instantiates and displays a `JClassBrowser`. This function is **not called automatically at startup** (disabled to save memory). To launch the class browser on demand:

```bsh
jvd.browser = jvdClassBrowser();
```

Internally the function:
- Sets up `bsh.system.icons` (bean, workspace, script, eye)
- Creates `new JClassBrowser(interpreter.getClassManager())` and calls `init()`
- Registers the browser as a VApp via `DesktopManager.get().createVApp(...)`
- Exposes `browse(obj)`, `go2(obj)`, `driveToClass(classname)` convenience methods

### 2. `_JVD()` function / `jvd` namespace (lines 131–156)

Creates the `jvd` object used throughout interactive sessions:

| Member | Description |
|--------|-------------|
| `jvd.app` | Reference to `App.getVSystem()` |
| `jvd.edt(closure)` | Convenience wrapper for `SwingUtilities.invokeLater()` |
| `jvd.classpath()` | Prints all URLs on the system classloader's classpath |
| `jvd.create(component, title)` | Creates a VApp window via `DesktopManager` |

### 3. Global utility functions (lines 169–203, java8 only)

Added experimentally (Jan 2026); these operate on BeanShell closures via `MethodInjector`:

| Function | Description |
|----------|-------------|
| `inject(target, script)` | Injects methods from `script` into `target` closure |
| `extend(target, script)` | Extends `target` closure's method set |
| `rms(target, name, paramTypes)` | Removes a method signature from a closure |
| `inspect(obj)` | Prints all variables and methods in a closure's namespace |
| `listWorkspaces()` | Prints active and available workspace names |

---

## Class Browser (`JClassBrowser`)

**Source:** `bsh/util/JClassBrowser.java`  
**Base class:** `bsh.util.ClassBrowser` (from the BeanShell distribution)

The custom `JClassBrowser` adds:
- Sorted display of packages, methods, fields, and constructors
- Async classpath loading in a background thread (prevents full EDT block)
- Initial load of `org.jwellman.virtualdesktop` package for a responsive first render

**Why it's disabled at startup:** `ClassBrowser.init()` must run on the EDT and loads the full JVM classpath into a tree model. In an embedded application this is a significant memory cost for a tool that is only occasionally useful.

**Known issue:** The class browser call fails silently under Java 11+ (see `jvdClassBrowser.bsh` line 212 comment). This is an undiagnosed issue, which is one reason it was moved to the last line of the script — other startup code runs successfully even if the browser fails.

---

## BeanShellService Design Notes

- **Singleton** — one interpreter for the whole application
- **Console must be injected before first `getInterpreter()` call** — BeanShell's `Interpreter.setConsole()` is incomplete; the console must be passed to the constructor for the REPL to work
- `initializeEnvironment_deferred()` exists in the source as a reference implementation but is not called due to console timing constraints
- Optional init script: `src/main/resources/org/jwellman/bsh/scripts/init.bsh` — sourced if it exists
