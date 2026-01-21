package org.jwellman.virtualdesktop.vapps;

import org.jwellman.console.ConsoleTheme;
import org.jwellman.console.InterpreterAdapter;
import org.jwellman.console.ui.GenericConsole;

/**
 * Factory/base class for creating Generic Console vapps.
 *
 * <p>This provides a flexible way to create console-based vapps
 * with any interpreter. For specific interpreters, use the
 * dedicated subclasses like {@link SpecBeanShellConsole} or
 * {@link SpecJavaScriptConsole}.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Create a console with a custom interpreter
 * InterpreterAdapter myAdapter = new MyCustomAdapter();
 * SpecGenericConsole spec = SpecGenericConsole.create(
 *     "My Console",
 *     myAdapter,
 *     ConsoleTheme.dark()
 * );
 * </pre>
 *
 * @author Rick Wellman
 */
public class SpecGenericConsole extends VirtualAppSpec {

    protected final GenericConsole console;
    protected final InterpreterAdapter adapter;

    /**
     * Create a generic console vapp.
     *
     * @param title the window title
     * @param adapter the interpreter adapter
     * @param theme the visual theme
     */
    protected SpecGenericConsole(String title, InterpreterAdapter adapter, ConsoleTheme theme) {
        this.adapter = adapter;
        this.height = 450;
        this.width = 750;

        this.console = new GenericConsole(adapter, theme);
        this.setTitle(title);
        this.setContent(this.createDefaultContent(console));

        // Print initial prompt
        if (adapter != null) {
            console.printPrompt();
        }
    }

    /**
     * Create a generic console vapp with default theme.
     *
     * @param title the window title
     * @param adapter the interpreter adapter
     */
    protected SpecGenericConsole(String title, InterpreterAdapter adapter) {
        this(title, adapter, ConsoleTheme.classic());
    }

    /**
     * Factory method to create a generic console vapp.
     *
     * @param title the window title
     * @param adapter the interpreter adapter
     * @param theme the visual theme
     * @return a new SpecGenericConsole
     */
    public static SpecGenericConsole create(String title, InterpreterAdapter adapter, ConsoleTheme theme) {
        return new SpecGenericConsole(title, adapter, theme);
    }

    /**
     * Factory method to create a generic console with default theme.
     *
     * @param title the window title
     * @param adapter the interpreter adapter
     * @return a new SpecGenericConsole
     */
    public static SpecGenericConsole create(String title, InterpreterAdapter adapter) {
        return new SpecGenericConsole(title, adapter);
    }

    /**
     * Get the console component.
     *
     * @return the GenericConsole
     */
    public GenericConsole getConsole() {
        return console;
    }

    /**
     * Get the interpreter adapter.
     *
     * @return the adapter
     */
    public InterpreterAdapter getAdapter() {
        return adapter;
    }

    /**
     * Apply a new theme to the console.
     *
     * @param theme the theme to apply
     */
    public void setTheme(ConsoleTheme theme) {
        console.setTheme(theme);
    }

    /**
     * Print a message to the console.
     *
     * @param message the message to print
     */
    public void print(String message) {
        console.print(message);
    }

    /**
     * Print a message with newline to the console.
     *
     * @param message the message to print
     */
    public void println(String message) {
        console.println(message);
    }

    /**
     * Clear the console output.
     */
    public void clear() {
        console.clear();
    }

}
