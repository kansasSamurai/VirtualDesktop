package org.jwellman.virtualdesktop.vapps;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jwellman.console.ConsoleTheme;
import org.jwellman.console.InterpreterException;
import org.jwellman.console.impl.NashornAdapter;
import org.jwellman.console.ui.GenericConsole;

/**
 * JavaScript Console vapp using Nashorn (Java 8's built-in JS engine).
 *
 * <p>Provides an interactive JavaScript REPL within the VirtualDesktop
 * environment. Uses the Nashorn engine via the GenericConsole architecture.</p>
 *
 * <p><b>Note:</b> Nashorn was deprecated in Java 11 and removed in Java 15.
 * This vapp is specifically for Java 8 compatibility.</p>
 *
 * @author Rick Wellman
 */
public class SpecJavaScriptConsole extends SpecGenericConsole implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SpecJavaScriptConsole.class.getName());

    private Thread interpreterThread;
    private volatile boolean running = true;

    /**
     * Create a JavaScript console with default theme.
     */
    public SpecJavaScriptConsole() {
        this(ConsoleTheme.dark());  // JS developers often prefer dark themes
    }

    /**
     * Create a JavaScript console with specified theme.
     *
     * @param theme the visual theme
     */
    public SpecJavaScriptConsole(ConsoleTheme theme) {
        super("JavaScript Console", createAdapter(), theme);

        // Configure the environment
        configureEnvironment();

        // Start the REPL thread
        startInterpreterThread();
    }

    private static NashornAdapter createAdapter() {
        try {
            return new NashornAdapter();
        } catch (InterpreterException e) {
            LOGGER.log(Level.SEVERE, "Failed to create Nashorn adapter", e);
            throw new RuntimeException("JavaScript engine not available: " + e.getMessage(), e);
        }
    }

    private void configureEnvironment() {
        try {
            // Expose the console itself
            adapter.set("_console", console);
            adapter.set("_adapter", adapter);

            // Try to expose the desktop manager if available
            try {
                Class<?> dmClass = Class.forName("org.jwellman.virtualdesktop.DesktopManager");
                Object desktop = dmClass.getMethod("get").invoke(null);
                adapter.set("_desktop", desktop);
            } catch (Exception e) {
                // DesktopManager not available, skip
            }

            // Print welcome message
            console.println("JavaScript Console (Nashorn)");
            console.println("Type 'help()' for available commands.");
            console.println("");

        } catch (InterpreterException e) {
            LOGGER.log(Level.WARNING, "Failed to configure JavaScript environment", e);
        }
    }

    private void startInterpreterThread() {
        interpreterThread = new Thread(this, "JavaScript-GenericConsole");
        interpreterThread.setDaemon(true);
        interpreterThread.start();
    }

    /**
     * JavaScript read-eval-print loop.
     */
    @Override
    public void run() {
        console.printPrompt();

        try {
            InputStream in = console.getInputStream();
            PrintStream out = console.getOut();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, "UTF-8"));

            StringBuilder buffer = new StringBuilder();
            String line;

            while (running && (line = reader.readLine()) != null) {
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    console.printPrompt();
                    continue;
                }

                buffer.append(line);

                // Check if the input is complete
                if (adapter.isComplete(buffer.toString())) {
                    String command = buffer.toString();
                    buffer.setLength(0);

                    console.setWaitFeedback(true);
                    try {
                        Object result = adapter.eval(command);
                        if (result != null && !"undefined".equals(String.valueOf(result))) {
                            out.println(result);
                        }
                    } catch (InterpreterException e) {
                        out.println("Error: " + e.getMessage());
                    } finally {
                        console.setWaitFeedback(false);
                    }

                    console.printPrompt();
                } else {
                    // Multi-line input
                    buffer.append("\n");
                    out.print("... ");
                }
            }

        } catch (Exception e) {
            if (running) {
                LOGGER.log(Level.SEVERE, "JavaScript REPL error", e);
                console.error("Console error: " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Evaluate a JavaScript expression.
     *
     * @param expression the expression to evaluate
     * @return the result
     * @throws InterpreterException if evaluation fails
     */
    public Object eval(String expression) throws InterpreterException {
        return adapter.eval(expression);
    }

    /**
     * Set a variable in the JavaScript scope.
     *
     * @param name the variable name
     * @param value the value
     * @throws InterpreterException if setting fails
     */
    public void set(String name, Object value) throws InterpreterException {
        adapter.set(name, value);
    }

    /**
     * Get a variable from the JavaScript scope.
     *
     * @param name the variable name
     * @return the value
     * @throws InterpreterException if retrieval fails
     */
    public Object get(String name) throws InterpreterException {
        return adapter.get(name);
    }

    /**
     * Get the Nashorn adapter.
     *
     * @return the adapter
     */
    public NashornAdapter getNashornAdapter() {
        return (NashornAdapter) adapter;
    }

    /**
     * Shutdown the console and stop the REPL thread.
     */
    public void shutdown() {
        running = false;
        console.shutdown();
        if (interpreterThread != null) {
            interpreterThread.interrupt();
        }
    }

}
