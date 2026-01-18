package org.jwellman.virtualdesktop.bsh;

import bsh.ConsoleInterface;
import bsh.EvalError;
import bsh.Interpreter;

import org.jwellman.virtualdesktop.DesktopManager;

/**
 * Singleton service managing a shared BeanShell interpreter.
 *
 * All BeanShell-enabled tools share this interpreter, providing:
 * - Memory efficiency (one interpreter vs N)
 * - Inter-tool communication via shared namespace
 * - Console integration (SpecBeanShell uses the same interpreter)
 *
 * <p>The interpreter is lazily initialized on first access. If a console
 * is set via {@link #setConsole(ConsoleInterface)} before the first call
 * to {@link #getInterpreter()}, the interpreter will be created with that
 * console (required for proper REPL functionality due to BeanShell's
 * incomplete setConsole() implementation).</p>
 *
 * @author rwellman
 */
public class BeanShellService {

    private static BeanShellService instance;
    private Interpreter interpreter;
    private ConsoleInterface console;

    private BeanShellService() {
        // Interpreter creation is deferred until getInterpreter() is called
    }

    /**
     * Get the singleton instance of the BeanShellService.
     *
     * @return the shared BeanShellService instance
     */
    public static synchronized BeanShellService get() {
        if (instance == null) {
            instance = new BeanShellService();
        }
        return instance;
    }

    /**
     * Set the console interface to use with the interpreter.
     *
     * <p>This must be called BEFORE the first call to {@link #getInterpreter()}
     * for the console to be properly connected. Due to limitations in BeanShell's
     * Interpreter.setConsole() implementation, the console must be passed to
     * the Interpreter constructor for the REPL to function correctly.</p>
     *
     * <p>This allows custom console implementations to be injected.</p>
     *
     * @param console the console interface to use
     * @throws IllegalStateException if interpreter has already been created
     */
    public void setConsole(ConsoleInterface console) {
        if (this.interpreter != null) {
            throw new IllegalStateException(
                "Cannot set console after interpreter has been created. " +
                "Call setConsole() before getInterpreter().");
        }
        this.console = console;
    }

    /**
     * Get the shared interpreter instance.
     *
     * <p>The interpreter is lazily created on first access. If a console
     * was set via {@link #setConsole(ConsoleInterface)}, it will be used;
     * otherwise a headless interpreter is created (suitable for script
     * execution but not interactive REPL).</p>
     *
     * @return the shared BeanShell Interpreter
     */
    public Interpreter getInterpreter() {
        if (interpreter == null) {
            if (console != null) {
                interpreter = new Interpreter(console);
            } else {
                interpreter = new Interpreter();
            }
            initializeEnvironment();
        }
        return interpreter;
    }

    /**
     * Check if a console has been set.
     *
     * @return true if a console is configured
     */
    public boolean hasConsole() {
        return console != null;
    }

    /**
     * Check if the interpreter has been initialized.
     *
     * @return true if the interpreter has been created
     */
    public boolean isInitialized() {
        return interpreter != null;
    }

    /**
     * Execute a script file, returning its result.
     *
     * @param scriptPath path to the script file
     * @return the result of script execution
     * @throws Exception if script execution fails
     */
    public Object source(String scriptPath) throws Exception {
        return getInterpreter().source(scriptPath);
    }

    /**
     * Evaluate a script string, returning its result.
     *
     * @param script the script to evaluate
     * @return the result of evaluation
     * @throws EvalError if evaluation fails
     */
    public Object eval(String script) throws EvalError {
        return getInterpreter().eval(script);
    }

    /**
     * Set a variable in the shared namespace.
     *
     * @param name variable name
     * @param value variable value
     * @throws EvalError if setting fails
     */
    public void set(String name, Object value) throws EvalError {
        getInterpreter().set(name, value);
    }

    /**
     * Get a variable from the shared namespace.
     *
     * @param name variable name
     * @return the variable value, or null if not found
     * @throws EvalError if retrieval fails
     */
    public Object get(String name) throws EvalError {
        return getInterpreter().get(name);
    }

    /**
     * Initialize the BeanShell environment with standard variables
     * and scripts available to all tools.
     *
     * <p><b>NOTE:</b> Currently disabled (no-op). Setting variables on the
     * interpreter immediately after construction causes issues with the
     * console initialization. The root cause is unclear but appears related
     * to BeanShell's internal state management during console setup.
     * See {@link #initializeEnvironment_deferred()} for the original impl.</p>
     *
     * <p>Workaround: Variables like _desktop and _bsh can be set manually
     * from scripts or after the console REPL thread has started.</p>
     */
    private void initializeEnvironment() {
        // Intentionally empty - see Javadoc above
    }

    /**
     * Original initialization code - preserved for future investigation.
     * DO NOT call directly; causes console initialization issues.
     */
    @SuppressWarnings("unused")
    private void initializeEnvironment_deferred() {
        try {
            // Expose desktop API to all scripts
            interpreter.set("_desktop", DesktopManager.get());

            // Expose the service itself for advanced use
            interpreter.set("_bsh", this);

            // Try to source the init script if it exists
            // Note: using relative path that works from project root
            try {
                interpreter.source("src/main/resources/org/jwellman/bsh/scripts/init.bsh");
            } catch (Exception e) {
                // Init script is optional - don't fail if not found
                System.out.println("BeanShellService: No init.bsh found (this is OK)");
            }

        } catch (EvalError e) {
            System.err.println("Failed to initialize BeanShell environment: " + e.getMessage());
        }
    }

    /**
     * Reset the shared interpreter to a clean state.
     * Use with caution as this will clear all variables.
     */
    public void reset() {
        try {
            getInterpreter().eval("clear();");
            initializeEnvironment();
        } catch (EvalError e) {
            System.err.println("Failed to reset BeanShell environment: " + e.getMessage());
        }
    }

}
