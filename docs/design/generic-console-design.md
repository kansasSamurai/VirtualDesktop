# Generic Console/REPL Component Design Document

**Project:** VirtualDesktop
**Component:** Generic Console Framework
**Status:** Design Phase
**Last Updated:** 2026-01-20

---

## 1. Introduction

### 1.1 Purpose
Design and implement a modern, interpreter-agnostic console/REPL component for Java Swing that can be used with different command interpreters including BeanShell, JavaScript (Nashorn), and potentially others.

### 1.2 Goals
- **Interpreter Independence** - Clean abstraction allowing any interpreter to be plugged in
- **Modern Design** - Both API and visual design improvements over existing BeanShell console
- **Java 8 Compatible** - Must work with Java 8 (project requirement)
- **Embeddable** - Works within VirtualDesktop's vapp framework
- **Maintainable** - Clean separation of concerns, testable components

### 1.3 Non-Goals (Initial Release)
- Full terminal emulation (PTY, ANSI escape codes)
- Remote/networked interpreter connections
- IDE-level debugging integration

---

## 2. Background

### 2.1 Current State
The VirtualDesktop project has a BeanShell console (`org.jwellman.bsh.JConsole`) that:
- Is tightly coupled to BeanShell's `GUIConsoleInterface`
- Uses `bsh.util.NameCompletion` directly for tab completion
- Has hardcoded BeanShell-specific initialization
- Cannot be easily reused with other interpreters

### 2.2 Problems with Current Approach
1. **Coupling** - Cannot use the console with JavaScript, Groovy, etc.
2. **Duplication** - Would need to copy/modify 900+ lines for each interpreter
3. **Maintenance** - Bug fixes must be applied to each copy
4. **User Experience** - Dated visual appearance, limited customization

### 2.3 Open Source Alternatives Evaluated

| Library | License | Pros | Cons |
|---------|---------|------|------|
| DragonConsole | MIT | Has CommandProcessor abstraction | Kotlin, dormant since 2022 |
| swing-console | LGPL | Simple, embeddable | No command processing abstraction |
| Text-IO | Apache 2.0 | Nice factory pattern | Input-gathering focused, not REPL |
| RSyntaxTextArea | BSD | Excellent editor component | Not a console, building block only |

**Decision:** Build custom solution, using RSyntaxTextArea optionally for syntax highlighting.

---

## 3. Architecture

### 3.1 High-Level Design

```
┌─────────────────────────────────────────────────────────────┐
│                     GenericConsole (UI)                     │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐    │
│  │ConsoleText- │  │ Command     │  │  ConsoleTheme    │    │
│  │Pane         │  │ History     │  │  (colors, font)  │    │
│  └─────────────┘  └─────────────┘  └──────────────────┘    │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │InterpreterAdapter│ ◄─── Abstract interface
                    └────────┬────────┘
           ┌─────────────────┼─────────────────┐
           ▼                 ▼                 ▼
    ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
    │BeanShell    │  │Nashorn      │  │Groovy       │
    │Adapter      │  │Adapter      │  │Adapter      │
    └─────────────┘  └─────────────┘  └─────────────┘
```

### 3.2 Core Interfaces

#### 3.2.1 InterpreterAdapter
The central abstraction for interpreter integration.

```java
public interface InterpreterAdapter {
    /**
     * Execute a command/expression and return the result.
     * @param command The command string to execute
     * @return The result of execution (may be null)
     * @throws InterpreterException If execution fails
     */
    Object eval(String command) throws InterpreterException;

    /**
     * Get the prompt string for this interpreter.
     * @return Prompt like "bsh % " or "js> "
     */
    String getPrompt();

    /**
     * Get the completion provider for tab completion.
     * @return CompletionProvider, or null if not supported
     */
    CompletionProvider getCompletionProvider();

    /**
     * Check if the input forms a complete statement.
     * Used for multi-line input detection.
     * @param input The input so far
     * @return true if complete, false if more input needed
     */
    boolean isComplete(String input);

    /**
     * Get the interpreter name for display.
     */
    String getName();

    /**
     * Set a variable in the interpreter's namespace.
     */
    void set(String name, Object value) throws InterpreterException;

    /**
     * Get a variable from the interpreter's namespace.
     */
    Object get(String name) throws InterpreterException;

    /**
     * Reset/clear the interpreter state.
     */
    void reset();

    /**
     * Cleanup when console is closing.
     */
    void shutdown();
}
```

#### 3.2.2 CompletionProvider
Abstraction for tab completion, decoupled from BeanShell's NameCompletion.

```java
public interface CompletionProvider {
    /**
     * Get completions for partial text.
     * @param text The partial text (may include dots for method access)
     * @return Array of possible completions, empty if none
     */
    String[] complete(String text);

    /**
     * Get context-aware completions.
     * @param fullLine The complete line being edited
     * @param cursorPosition Position of cursor in line
     * @return Array of possible completions
     */
    String[] complete(String fullLine, int cursorPosition);
}
```

#### 3.2.3 CommandHistory
Abstraction allowing for persistence options.

```java
public interface CommandHistory {
    void add(String command);
    String getPrevious();
    String getNext();
    void reset();
    List<String> getAll();
    int size();

    // Optional persistence (default no-op)
    default void save() throws IOException {}
    default void load() throws IOException {}
}
```

#### 3.2.4 ConsoleTheme
Visual customization with builder pattern.

```java
public class ConsoleTheme {
    private Font font;
    private Color backgroundColor;
    private Color foregroundColor;
    private Color errorColor;
    private Color promptColor;
    private Color resultColor;
    private Insets margin;

    // Builder pattern
    public static Builder builder() { return new Builder(); }

    // Preset themes
    public static ConsoleTheme light() { ... }
    public static ConsoleTheme dark() { ... }
    public static ConsoleTheme solarizedLight() { ... }
    public static ConsoleTheme solarizedDark() { ... }

    public static class Builder {
        public Builder font(Font f) { ... }
        public Builder backgroundColor(Color c) { ... }
        public Builder foregroundColor(Color c) { ... }
        public Builder errorColor(Color c) { ... }
        public Builder promptColor(Color c) { ... }
        public Builder resultColor(Color c) { ... }
        public Builder margin(Insets i) { ... }
        public ConsoleTheme build() { ... }
    }
}
```

---

## 4. UI Components

### 4.1 GenericConsole
Main console component, extends JScrollPane (like existing JConsole).

**Responsibilities:**
- Host ConsoleTextPane
- Manage I/O streams (piped streams)
- Handle keyboard input (Enter, Up/Down, Tab, etc.)
- Display output with styling
- Context menu (cut/copy/paste)
- Coordinate with InterpreterAdapter

### 4.2 ConsoleTextPane
Protected JTextPane that prevents editing output.

**Features:**
- Tracks `cmdStart` position (where current command begins)
- Overrides cut() to copy-only before cmdStart
- Overrides paste() to force caret to end
- Styled document support

### 4.3 BlockingPipedInputStream
Thread-safe piped input stream (from existing JConsole).

**Purpose:** Handles ephemeral writer threads gracefully without "broken pipe" exceptions.

---

## 5. Interpreter Adapters

### 5.1 BeanShellAdapter
Wraps `bsh.Interpreter`, integrates with `BeanShellService`.

```java
public class BeanShellAdapter implements InterpreterAdapter {
    private final Interpreter interpreter;
    private final BeanShellCompletionProvider completionProvider;

    public BeanShellAdapter(Interpreter interpreter) {
        this.interpreter = interpreter;
        this.completionProvider = new BeanShellCompletionProvider(interpreter);
    }

    @Override
    public Object eval(String command) throws InterpreterException {
        try {
            return interpreter.eval(command);
        } catch (EvalError e) {
            throw new InterpreterException(e.getMessage(), e);
        }
    }

    @Override
    public String getPrompt() {
        return "bsh % ";
    }

    @Override
    public boolean isComplete(String input) {
        // BeanShell heuristic: count braces, check for trailing semicolon
        // or use interpreter's parser if available
    }

    // ... other methods
}
```

### 5.2 NashornAdapter
Uses `javax.script.ScriptEngine` (Java 8 standard).

```java
public class NashornAdapter implements InterpreterAdapter {
    private final ScriptEngine engine;

    public NashornAdapter() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("nashorn");
    }

    @Override
    public Object eval(String command) throws InterpreterException {
        try {
            return engine.eval(command);
        } catch (ScriptException e) {
            throw new InterpreterException(e.getMessage(), e);
        }
    }

    @Override
    public String getPrompt() {
        return "js> ";
    }

    // ... other methods
}
```

---

## 6. Feature Tracking

### 6.1 Core Features (MVP)

| Feature | Status | Priority | Notes |
|---------|--------|----------|-------|
| InterpreterAdapter interface | Planned | P0 | Core abstraction |
| CompletionProvider interface | Planned | P0 | Tab completion |
| CommandHistory interface | Planned | P0 | Up/down arrows |
| ConsoleTheme | Planned | P1 | Visual customization |
| GenericConsole UI | Planned | P0 | Main component |
| BeanShellAdapter | Planned | P0 | First adapter |
| NashornAdapter | Planned | P1 | Prove abstraction |
| VApp integration | Planned | P0 | SpecGenericConsole |

### 6.2 Enhanced Features (Future)

| Feature | Status | Priority | Notes |
|---------|--------|----------|-------|
| Multi-line input detection | Planned | P2 | Continuation prompts |
| Syntax highlighting (RSyntaxTextArea) | Planned | P2 | Optional mode |
| Persistent history | Planned | P3 | File-backed |
| Split-pane mode (editor + output) | Planned | P3 | Like IPython notebook |
| Magic commands | Planned | P3 | %history, %clear, etc. |
| Session recording | Planned | P3 | For tutorials |
| GraalJS adapter | Planned | P3 | Java 11+ option |
| Groovy adapter | Planned | P3 | Unify with GroovyConsole |

### 6.3 Progress Log

| Date | Milestone | Notes |
|------|-----------|-------|
| 2026-01-20 | Design document created | Initial architecture defined |
| | | |

---

## 7. Implementation Notes

### 7.1 Key Code to Port from JConsole

The existing `org.jwellman.bsh.JConsole` (923 lines) contains:

1. **Piped stream setup** (lines 152-174) - Keep as-is
2. **BlockingPipedInputStream** (lines 872-905) - Extract to own class
3. **Key handling** (lines 226-351) - Port with interpreter abstraction
4. **Command history** (lines 490-518) - Extract to CommandHistory
5. **Tab completion** (lines 353-409) - Adapt to use CompletionProvider
6. **Styled output** (lines 553-644) - Port to use ConsoleTheme
7. **Caret management** (lines 429-442) - Keep pattern in ConsoleTextPane

### 7.2 Thread Safety Considerations

- Output from interpreter runs on non-EDT thread
- Use `SwingUtilities.invokeAndWait()` for UI updates (existing pattern)
- BlockingPipedInputStream uses synchronized read with wait/notify
- Command execution should not block EDT

### 7.3 Backward Compatibility

```java
/**
 * Bridge for backward compatibility with code expecting
 * bsh.util.GUIConsoleInterface.
 */
public class LegacyBeanShellBridge implements GUIConsoleInterface {
    private final GenericConsole console;

    public LegacyBeanShellBridge(GenericConsole console) {
        this.console = console;
    }

    @Override
    public Reader getIn() {
        return console.getInputReader();
    }

    @Override
    public PrintStream getOut() {
        return console.getOutputStream();
    }

    // ... delegate all methods
}
```

### 7.4 Initial Implementation Files

```
virtualdesktop-java8/src/main/java/
└── org/jwellman/
    ├── console/                              # New package - Generic Console Framework
    │   │
    │   │   # Core Interfaces
    │   ├── InterpreterAdapter.java           # Central abstraction for interpreters
    │   ├── InterpreterException.java         # Unified exception type
    │   ├── CompletionProvider.java           # Tab completion interface
    │   ├── CommandHistory.java               # History navigation interface
    │   ├── ConsoleTheme.java                 # Visual theme with builder pattern
    │   │
    │   ├── impl/                             # Adapter Implementations
    │   │   ├── DefaultCommandHistory.java    # In-memory history with navigation
    │   │   ├── BeanShellAdapter.java         # Wraps bsh.Interpreter
    │   │   ├── BeanShellCompletionProvider.java  # Wraps bsh.util.NameCompletion
    │   │   ├── NashornAdapter.java           # Wraps javax.script.ScriptEngine
    │   │   └── NashornCompletionProvider.java    # JS completion with reflection
    │   │
    │   └── ui/                               # UI Components
    │       ├── GenericConsole.java           # Main console component (JScrollPane)
    │       ├── ConsoleTextPane.java          # Protected text pane (JTextPane)
    │       ├── SyntaxConsoleTextPane.java    # RSyntaxTextArea variant
    │       ├── TextPaneFactory.java          # Factory for text pane selection
    │       └── BlockingPipedInputStream.java # Thread-safe piped stream
    │
    └── virtualdesktop/vapps/                 # VApp Integration
        ├── SpecGenericConsole.java           # Base/factory for console vapps
        ├── SpecBeanShellConsole.java         # BeanShell using new architecture
        └── SpecJavaScriptConsole.java        # JavaScript (Nashorn) console
```

**File Count Summary:**
- Core interfaces: 5 files
- Implementations: 5 files
- UI components: 5 files
- VApp integration: 3 files
- **Total: 18 new files**

---

## 8. Testing Strategy

### 8.1 Unit Tests
- `InterpreterAdapterTest` - Mock interpreter, verify contract
- `CommandHistoryTest` - Test navigation, bounds
- `CompletionProviderTest` - Test completion logic
- `ConsoleThemeTest` - Test builder, presets

### 8.2 Integration Tests
- `BeanShellAdapterTest` - Real BeanShell evaluation
- `NashornAdapterTest` - Real Nashorn evaluation
- `GenericConsoleTest` - UI interaction tests

### 8.3 Visual/Manual Tests
- Create test vapp that switches between interpreters
- Verify styling, themes work correctly
- Test with different Look and Feels

---

## 9. Design Decisions (Confirmed)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Text Component | Both JTextPane (default) + RSyntaxTextArea (option) | Flexibility for users who want syntax highlighting |
| Initial Adapters | BeanShell + JavaScript (Nashorn) | Prove abstraction works with two interpreters |
| Visual Themes | Essential MVP feature | Modern appearance is a key goal |

## 10. Open Questions

1. **Interpreter lifecycle** - Should adapters manage interpreter lifecycle or just wrap existing instances? Current thinking: wrap existing for flexibility.

2. **Error display** - Should errors show stack traces by default or require explicit expansion? Current thinking: configurable via ConsoleTheme or separate flag.

3. **Multi-line handling** - How should continuation be indicated? Current thinking: secondary prompt (e.g., `... ` for BeanShell).

---

## 11. References

- Existing JConsole: `org/jwellman/bsh/JConsole.java`
- BeanShell service pattern: `org/jwellman/virtualdesktop/bsh/BeanShellService.java`
- VApp base: `org/jwellman/virtualdesktop/vapps/VirtualAppSpec.java`
- RSyntaxTextArea: https://github.com/bobbylight/RSyntaxTextArea
