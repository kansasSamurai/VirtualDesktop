# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VirtualDesktop is a Java-based virtual desktop application - not a "remote desktop", but a standalone application that provides a consistent desktop experience independent of the underlying OS and the gyrations that OS vendors impose on users.

### Design Philosophy

**Orchestrator, not replacement.** VirtualDesktop is not meant to replace every tool a developer uses - it is meant to *participate* and *orchestrate*. The guiding principle is "best tool for the job": when a browser renders documents better than an embedded panel, use the browser; when an external CLI does something better than a Java implementation, invoke it. VirtualDesktop provides the connective tissue.

**Polyglot by design.** The application is explicitly a polyglot environment. BeanShell scripting is the primary orchestration glue - a script that spins up a JPanel, queries a database, fires off a Lucene search, and opens a browser tab is exactly the kind of cross-tool workflow the desktop is designed to enable.

**Consistent cross-platform desktop metaphor.** The goal is a stable, predictable desktop experience that doesn't shift under the user when OS vendors change their UI paradigms.

This is in the proof-of-concept stage but provides personal utility, particularly with BeanShell and JCXConsole integration. Note: One misbehaving tool can crash the entire JVM, so save files frequently.

### Architectural Decisions Informed by This Philosophy

When deciding whether to build a feature inside the desktop vs. delegate to an external tool, ask: does internalizing this add value beyond what the external tool already provides? If not, delegate and orchestrate. Examples:
- Help/documentation viewing → delegate to the system browser (richer rendering, future-proof)
- Document search → Lucene embedded in-process (tight integration with the app's data)
- Scripting → BeanShell as the glue layer between Java components and external tools

## Build System

This is a Maven project targeting **Java 8**.

**IMPORTANT**: Java 8 compatibility is a key design goal due to legacy dependencies. Do not introduce Java 9+ language features or APIs.

### Roadmap: Dual Version Strategy

Future plans include maintaining two parallel versions:
1. **Java 8 compatible** - Maintains compatibility with legacy dependencies
2. **Java 9+ compatible** - Leverages modern Java features (targeting current Java releases)

### Building the Project

```bash
mvn clean compile
mvn package
```

### Running the Application

The main entry point is `org.jwellman.virtualdesktop.App`

### Manual Dependencies

Four JAR files must be manually installed to the local Maven repository before building:

```bash
mvn install:install-file -Dfile=weblaf-1.29.jar -DgroupId=com.mgarin -DartifactId=weblaf -Dversion=1.29 -Dpackaging=jar
mvn install:install-file -Dfile=jsilhouette-0.2.jar -DgroupId=org.kordamp -DartifactId=jsilhouette -Dversion=0.2 -Dpackaging=jar
mvn install:install-file -Dfile=graphicsbuilder-0.6.1.jar -DgroupId=org.codehaus.groovy-contrib -DartifactId=graphicsbuilder -Dversion=0.6.1 -Dpackaging=jar
mvn install:install-file -Dfile=dans-dbf-lib-1.0.0-beta-10.jar -DgroupId=nl.knaw.dans.common -DartifactId=dans-dbf-lib -Dversion=1.0.0-beta-10 -Dpackaging=jar
```

Also requires: `org.jwellman:swing-utils:0.0.1-SNAPSHOT` (custom library)

### JDK9/JDK11 Compatibility

When running on JDK9+ (due to Groovy console requirements), add this VM argument:

```bash
--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
```

## Architecture

### Core Packages

- **`org.jwellman.virtualdesktop`** - Main application package
  - `App.java` - Main application class and entry point; manages JFrame, desktop pane, menu system, and Look and Feel
  - `VirtualAppFrame.java` - Base class for "virtual applications" (vapps) that run within the desktop
  - `DesktopManager.java` - Manages the desktop and internal frames
  - `DesktopScrollPane.java` - Custom scrollable desktop pane

- **`org.jwellman.virtualdesktop.vapps`** - Virtual applications (integrated tools)
  - `SpecBeanShell` - BeanShell scripting console
  - `SpecJCXConsole` - Command console integration
  - `SpecHyperSQL` - HyperSQL database manager
  - `SpecJFreeChart` - Chart visualization
  - `SpecXChartDemo` - XChart demonstrations
  - `SpecXionFM` - File manager (Linux-targeted)
  - `ActionFactory` - Factory for creating desktop actions
  - `DesktopAction` - Base action class for desktop operations

- **`org.jwellman.virtualdesktop.desktop`** - Desktop infrastructure
  - `VShortcut` - Desktop shortcut/icon representation
  - `VActionLNF` - Look and Feel actions
  - `VException` - Custom exception handling

- **`org.jwellman.virtualdesktop.security`** - Security components
  - `NoExitSecurityManager` - Prevents System.exit() calls from vapps to avoid JVM crashes

- **`fx`** - XionDE File Manager (external project integration)
  - `Main.java` - File manager main class
  - `RootFrame.java` - File manager UI frame
  - `fx.filemanager` - File manager core functionality

- **`org.jwellman.swing`** - Custom Swing components and utilities
- **`org.jwellman.dsp`** - Desktop services provider, icon management
- **`org.jwellman.groovy`** - Groovy integration support
- **`org.jwellman.bsh`** - BeanShell utilities
- **`org.jwellman.jcx`** - JCXConsole implementation

### Third-Party Integrations

- **`com.tomtessier.scrollabledesktop`** - Scrollable desktop pane implementation
- **`ext.hsqldb.util`** - Extended HSQLDB utilities
- **`ext.com.jediterm`** - Terminal emulator integration
- **`org.jdesktop.swingx`** - SwingX components (MultiSplitPane/Layout)

### Application Architecture

VirtualDesktop uses a plugin-style architecture where "vapps" (virtual applications) are JPanel-based components that run within JInternalFrames on a JDesktopPane. Each vapp is registered with the system and accessible via the application menu.

Key architectural patterns:
- **No System.exit()**: Vapps must not call System.exit(); the NoExitSecurityManager prevents JVM termination
- **Panel-based**: Vapps extend JPanel (not JFrame) for portability and framework integration
- **Look and Feel agnostic**: Vapps should not set their own LAF
- **Modal dialog abstraction**: Use portable dialog interfaces instead of direct JDialog

### Look and Feel Support

Multiple LAF options are configured in `App.java`:
- FlatLaf (default: `LAF_FLATLAF`) - Modern flat design
- JTattoo (Aluminium) - Mac-like appearance
- WebLAF
- Nimbus
- Metal (with theme support via `MetalThemeManager`)
- Napkin
- System

The chosen LAF is controlled by `App.CHOSEN_LAF` constant.

### Desktop Icons and Shortcuts

Desktop shortcuts are represented by `VShortcut` objects and can be saved/loaded (JSON format via Jackson). The desktop supports drag-and-drop, file operations, and custom icon themes (via `IconTheme` in fx.filemanager).

### Configuration

- File manager config: Uses Properties files stored in `~/.xionde/` directory
  - `global.properties` - Global configuration
  - `fm.properties` - File manager specific settings
- Main app: Currently hardcoded; see todo.txt for configuration framework needs

## Key Dependencies

- **Groovy 2.3.0** - Scripting support (Groovy console)
- **BeanShell 2.0b5** - Embedded scripting
- **HSQLDB 2.3.0** - Embedded database
- **JFreeChart 1.5.0** - Charting
- **XChart 2.2.1** - Alternative charting
- **FlatLaf 3.1.1** - Modern Look and Feel
- **JTattoo 1.6.11** - Alternative Look and Feel
- **WebLAF 1.27** - Alternative Look and Feel
- **SwingX 1.6.2-2** - Extended Swing components
- **JIDE-OSS 3.6.18** - JIDE components
- **Docking Frames 1.1.3** - Docking framework
- **JediTerm 2.33** / **Pty4j 0.8.2** - Terminal emulator
- **JIconFont** - Icon font support (Font Awesome, Material Design)
- **Jackson 2.15.2** - JSON serialization
- **Apache Commons** - Various utilities (IO, Lang3, Collections4, Compress, Text, Exec, VFS2, Email)

## Development Guidelines

### Java Code Formatting

Standard Java formatting conventions must be followed throughout this codebase:

- **One statement per line** — never `rows.add(row); visibleRowsDirty = true; return this;` on a single line
- **Method bodies always use braces and line breaks** — never `public int getCount() { return n; }` inline
- **`@Override` on its own line** — not `@Override public void method() { ... }` compacted
- **`for`/`if`/`while` blocks always use braces** — even single-statement bodies
- Guard-clause `return` in `compareValues`-style helpers is acceptable (`if (a == null) return -1;`)
- Getters and setters follow normal multi-line format, not the `{ return x; }` single-line style

### Writing Portable Vapps

From todo.txt - guidelines for writing portable applications:
1. Do NOT use a look and feel
2. Do NOT use your own container (JFrame/JInternalFrame/JDialog)
3. Use JPanel for EVERYTHING (let the framework handle container placement)
4. Do NOT call System.exit() directly
5. Do NOT use modal/non-modal dialogs directly; use portable interface
6. OS-specific behavior must check for OS first and handle gracefully

### Services Pattern

Use services for:
- Fonts (via DSP - Desktop Services Provider)
- Borders (caching instances, color palette dependent)
- Color palette

### Icon Fonts

Icon fonts are managed via JIconFont library:
- Font Awesome integration via `FontAwesomeIconProvider`
- Google Material Design Icons
- Icons accessed through `IconSpecifier` interface

## File Layout Persistence

Desktop layout (shape positions, etc.) can be saved to `layout.dat` (currently appears in git status as untracked).
