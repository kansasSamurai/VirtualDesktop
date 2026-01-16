# VirtualDesktop Glossary

This glossary documents terminology used in the VirtualDesktop project to maintain consistency and clarity in design discussions.

---

## Core Application Concepts

### VirtualDesktop / jPad
**Synonyms:** VirtualDesktop, jPad (these terms are interchangeable)

The entire application - a Java-based desktop environment that runs within a single JVM, providing a consistent cross-platform desktop metaphor independent of the underlying OS.

**Naming:**
- **VirtualDesktop** - The historical/technical name used throughout the codebase, package names, and repository
- **jPad** - The branding/marketing name for the application
  - Tongue-in-cheek reference to being "one better than an iPad"
  - Acronym: **J**ava **P**owered **A**lternative **D**esktop

### Vapp / Tool (Virtual Application)
**Synonyms:** Tool, Vapp, Virtual Application, Integrated Tool

A JPanel-based application that runs within the virtual desktop environment. Examples: BeanShell console, HyperSQL manager, JFreeChart tool.

**Naming Convention:**
- **Tool** - Preferred user-facing term (used in menus, documentation)
- **Vapp** - Historical codebase term (used in code, class names like `VirtualAppSpec`)

**Key Characteristics:**
- Extends or uses `VirtualAppSpec` as its descriptor
- Runs within a `JInternalFrame` on the desktop pane
- Does not call `System.exit()`
- Does not set its own Look and Feel
- Should be portable and framework-agnostic

### VirtualAppSpec (Spec)
**Synonyms:** Spec, App Spec, Virtual App Specification

The descriptor/configuration class for a vapp. Contains metadata (title, icon, dimensions) and lifecycle methods. Serves as the base class or template for defining vapps.

**Location:** `org.jwellman.virtualdesktop.vapps.VirtualAppSpec`

### VirtualAppFrame
**Synonyms:** Internal Frame, App Frame

A thin wrapper around `JInternalFrame` that hosts a vapp within the desktop pane. Provides windowing functionality (title bar, resize, minimize, maximize, close).

**Location:** `org.jwellman.virtualdesktop.VirtualAppFrame`

---

## Desktop Infrastructure

### Desktop / Desktop Pane
**Technical Type:** `VDesktopPane` (extends `JDesktopPane`)

The main canvas/workspace where `VirtualAppFrame` instances are displayed. The scrollable area containing all open vapps.

### DesktopManager
**Synonyms:** Manager

The singleton orchestrator responsible for:
- Creating vapps and their frames
- Managing frame lifecycle
- Tracking open vapps
- Coordinating between specs and frames

**Location:** `org.jwellman.virtualdesktop.DesktopManager`

### Desktop Shortcut / Icon
**Technical Type:** `VShortcut`

Visual icons on the desktop representing files, directories, or applications. Can be saved/loaded (JSON format). Supports drag-and-drop operations.

---

## Docking Framework Terminology

### Docking / Docking Framework
The infrastructure that allows UI components to be dragged, docked, and arranged within frames. Currently implemented using Bibliothek Docking Frames 1.1.3, but abstracted for future replacement.

### Dockable
**Technical Type:** `Dockable` interface

A single UI component that can be docked, positioned, hidden, or moved within a workspace. Wraps a `JComponent` with docking-specific operations.

### Docking Workspace
**Technical Type:** `DockingWorkspace` interface

A container within a `VirtualAppFrame` that manages the layout of dockable components. Each internal frame typically has one workspace.

**Note:** Different from "desktop" - a workspace is internal to a single frame.

### Docking Service
**Technical Type:** `DockingService` interface

The singleton facade providing access to docking functionality. Manages initialization, theme, and workspace creation.

### Docking Provider
**Technical Type:** `DockingProvider` interface (SPI)

The Service Provider Interface for plugging in different docking framework implementations. Current implementation: `BibliothekDockingProvider`.

---

## Architecture Patterns

### SPI (Service Provider Interface)
A design pattern allowing pluggable implementations. Used for the docking framework to enable future replacement without changing application code.

### Provider
An implementation of an SPI. Example: `BibliothekDockingProvider` implements `DockingProvider`.

### Adapter
A design pattern wrapping a third-party library behind an abstraction. Example: The `impl.bibliothek` package adapts Docking Frames to our interfaces.

### Facade
A simplified interface to a complex subsystem. Example: `DockingService` provides a simple API to the docking framework.

---

## Look and Feel Terminology

### LAF / Look and Feel
**Synonyms:** Theme (in UI context, not docking context)

The visual appearance of Swing components. VirtualDesktop supports multiple LAFs:
- FlatLaf (default)
- JTattoo (Aluminium)
- WebLAF
- Nimbus
- Metal
- Napkin
- System native

**Note:** Controlled by `App.CHOSEN_LAF` constant.

### Docking Theme
The visual style of the docking framework (separate from LAF). Options: FLAT, ECLIPSE, SMOOTH, BASIC, BUBBLE.

**Note:** Not the same as application Look and Feel.

---

## Service and Utility Concepts

### DSP (Desktop Services Provider)
**Location:** `org.jwellman.dsp` package

Provides shared services like:
- Icon management (`DSP.Icons`)
- Font services
- Border caching
- Color palette

### ActionFactory
**Location:** `org.jwellman.virtualdesktop.vapps.ActionFactory`

Factory class for creating desktop actions. Contains `registeredApps` - the list of available vapps that appear in menus.

### DesktopAction
**Synonyms:** Action

Base action class for desktop operations. Extends Swing's `Action` for menu items and toolbar buttons.

---

## Platform and Compatibility

### Java 8 Compatibility
A key design constraint - the codebase must remain compatible with Java 8 due to legacy dependencies. No Java 9+ language features or APIs should be used.

### Dual Version Strategy (Future)
Planned approach to maintain two parallel versions:
1. Java 8 compatible
2. Java 9+ compatible (leveraging modern features)

---

## Integration Components

### Internal Frame Provider
A vapp that needs direct access to its `JInternalFrame` for advanced customization. Uses `populateInternalFrame()` callback. Discouraged except for legacy/special cases.

### Launch Aware
**Technical Type:** `LaunchAware` interface

Vapps implementing this interface can perform initialization when launched by `DesktopManager`.

---

## File Types and Data

### Layout Data
Serialized desktop layout information (shortcut positions, etc.). Saved to `layout.dat`.

### Configuration
- **Global config:** `~/.xionde/global.properties`
- **File manager config:** `~/.xionde/fm.properties`
- **Main app config:** Currently hardcoded (needs config framework)

---

## Common Abbreviations

- **vapp** - Virtual Application
- **spec** - VirtualAppSpec
- **LAF** - Look and Feel
- **DSP** - Desktop Services Provider
- **SPI** - Service Provider Interface
- **JIF** - JInternalFrame
- **FM** - File Manager (XionFM)

---

## Terms to Avoid / Clarify

### "Application" (Ambiguous)
Can mean:
1. The entire VirtualDesktop application
2. A vapp running within the desktop
3. An external OS application

**Prefer:** Use "VirtualDesktop application" or "vapp" for clarity.

### "Window" (Ambiguous)
Can mean:
1. The main JFrame
2. A JInternalFrame
3. A floating dockable
4. An OS window

**Prefer:** Use "main frame", "internal frame", "VirtualAppFrame", or "floating dockable".

### "Tool" (Somewhat Ambiguous)
Usually refers to a vapp, but could mean:
1. A utility vapp
2. A toolbar item
3. A development tool

**Prefer:** Use "vapp" for virtual applications.

---

## Framework-Specific Terms

### Bibliothek / Docking Frames
The third-party docking framework (version 1.1.3) currently used, now abstracted behind our interfaces.

### CControl
The Bibliothek framework's main controller (now wrapped by `BibliothekDockingProvider`).

### CContentArea
The Bibliothek framework's content area (now wrapped by `BibliothekWorkspace`).

### SingleCDockable
The Bibliothek framework's dockable component (now wrapped by `BibliothekDockable`).

---

## Notes

- This glossary should be updated as the project evolves
- When introducing new terminology, add it here
- Prefer established terms over inventing new ones
- When terms conflict, document the preferred usage
