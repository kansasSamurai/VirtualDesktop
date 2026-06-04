package org.jwellman.virtualdesktop.vapps;

import java.awt.GridLayout;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.jwellman.console.ConsoleTheme;
import org.jwellman.console.impl.BeanShellAdapter;
import org.jwellman.virtualdesktop.bsh.BeanShellService;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * BeanShell Console vapp using the new GenericConsole architecture.
 *
 * <p>This is an alternative to {@link SpecBeanShell} that uses the
 * interpreter-agnostic GenericConsole with a BeanShellAdapter.</p>
 *
 * <p>This implementation shares the global interpreter from
 * {@link BeanShellService}, so variables set here are visible in
 * {@code SpecObjectBrowser} and {@code SpecScriptTester} and vice versa.
 * Output is redirected to the GenericConsole streams after interpreter
 * construction rather than via the BeanShell ConsoleInterface constructor.</p>
 *
 * @author Rick Wellman
 */
public class SpecBeanShellConsole extends SpecGenericConsole implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SpecBeanShellConsole.class.getName());

    /** Temp bridge: set by createAdapterFromPrompt() before super() returns. */
    private static boolean lastChoiceWasGlobal = true;

    private boolean usingGlobalInterpreter;
    private Interpreter interpreter;
    private Thread interpreterThread;

    /**
     * Create a BeanShell console with default theme.
     */
    public SpecBeanShellConsole() {
        this(ConsoleTheme.dark());
    }

    /**
     * Create a BeanShell console with specified theme.
     *
     * @param theme the visual theme
     */
    public SpecBeanShellConsole(ConsoleTheme theme) {
        super("BeanShell Console (New)", createAdapterFromPrompt(), theme);

        // createAdapterFromPrompt() set lastChoiceWasGlobal as a side effect
        this.usingGlobalInterpreter = lastChoiceWasGlobal;

        // Get the interpreter from our adapter
        this.interpreter = ((BeanShellAdapter) adapter).getInterpreter();

        // Configure the interpreter to use our console's streams
        configureInterpreter();

        // Start the interpreter's REPL loop in a background thread
        startInterpreterThread();
    }

    private static boolean promptForInterpreterChoice() {
        JRadioButton rbCommon = new JRadioButton("Common interpreter  (shared with Object Browser, Script Tester)", true);
        JRadioButton rbStandalone = new JRadioButton("Standalone interpreter  (isolated namespace)");

        ButtonGroup group = new ButtonGroup();
        group.add(rbCommon);
        group.add(rbStandalone);

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(rbCommon);
        panel.add(rbStandalone);

        int result = JOptionPane.showConfirmDialog(
            null, panel, "BeanShell Interpreter",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        // Default to common on cancel or close
        return result != JOptionPane.OK_OPTION || rbCommon.isSelected();
    }

    private static BeanShellAdapter createAdapterFromPrompt() {
        lastChoiceWasGlobal = promptForInterpreterChoice();
        return lastChoiceWasGlobal
            ? new BeanShellAdapter(BeanShellService.get().getInterpreter())
            : new BeanShellAdapter();
    }

    private void configureInterpreter() {
        try {
            // Redirect interpreter output to the console pipe so that BeanShell's
            // built-in print() goes to the GUI rather than System.out.
            interpreter.setOut(console.getOut());
            interpreter.setErr(console.getOut());

            // Expose references in the namespace
            interpreter.set("_interpreter", interpreter);
            interpreter.set("_console", console);
            interpreter.set("_adapter", adapter);

            // Try to expose the desktop manager if available
            try {
                Class<?> dmClass = Class.forName("org.jwellman.virtualdesktop.DesktopManager");
                Object desktop = dmClass.getMethod("get").invoke(null);
                interpreter.set("_desktop", desktop);
            } catch (Exception e) {
                // DesktopManager not available, skip
            }

        } catch (EvalError e) {
            LOGGER.log(Level.WARNING, "Failed to configure BeanShell namespace", e);
        }

        if (usingGlobalInterpreter) {
            try {
                interpreter.source("src/main/resources/jvdClassBrowser.bsh");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to source jvdClassBrowser.bsh", e);
            }
        }
    }

    private void startInterpreterThread() {
        // Create an interpreter thread that reads from our console's streams
        interpreterThread = new Thread(this, "BeanShell-GenericConsole");
        interpreterThread.setDaemon(true);
        interpreterThread.start();
    }

    /**
     * Interpreter read-eval-print loop.
     */
    @Override
    public void run() {
        console.printPrompt();

        try {
            // Read from the console's input stream
            java.io.InputStream in = console.getInputStream();
            java.io.PrintStream out = console.getOut();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(in, "UTF-8"));

            String line;
            StringBuilder buffer = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                // Skip empty lines and semicolons only
                if (line.trim().isEmpty() || line.trim().equals(";")) {
                    console.printPrompt();
                    continue;
                }

                buffer.append(line);

                // Check if the input is complete (balanced braces, etc.)
                if (adapter.isComplete(buffer.toString())) {
                    String command = buffer.toString();
                    buffer.setLength(0);  // Clear buffer

                    console.setWaitFeedback(true);
                    try {
                        Object result = interpreter.eval(command);
                        if (result != null) {
                            out.println(result);
                        }
                    } catch (EvalError e) {
                        out.println("Error: " + e.getMessage());
                    } finally {
                        console.setWaitFeedback(false);
                    }

                    console.printPrompt();
                } else {
                    // Multi-line input, show continuation prompt
                    out.print("... ");
                }
            }

        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                // Normal shutdown
                Thread.currentThread().interrupt();
            } else {
                LOGGER.log(Level.SEVERE, "BeanShell REPL error", e);
                console.error("Console error: " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * Evaluate a BeanShell expression.
     *
     * @param expression the expression to evaluate
     * @return the result
     * @throws EvalError if evaluation fails
     */
    public Object eval(String expression) throws EvalError {
        return interpreter.eval(expression);
    }

    /**
     * Set a variable in the BeanShell namespace.
     *
     * @param name the variable name
     * @param value the value
     * @throws EvalError if setting fails
     */
    public void set(String name, Object value) throws EvalError {
        interpreter.set(name, value);
    }

    /**
     * Get a variable from the BeanShell namespace.
     *
     * @param name the variable name
     * @return the value
     * @throws EvalError if retrieval fails
     */
    public Object get(String name) throws EvalError {
        return interpreter.get(name);
    }

    /**
     * Get the BeanShell interpreter.
     *
     * @return the interpreter
     */
    public Interpreter getInterpreter() {
        return interpreter;
    }

}
