package org.jwellman.console;

/**
 * Adapter interface for connecting interpreters to the GenericConsole.
 *
 * <p>Implementations wrap specific interpreters (BeanShell, Nashorn, etc.)
 * to provide a uniform interface for the console component.</p>
 *
 * @author Rick Wellman
 */
public interface InterpreterAdapter {

    /**
     * Evaluate a command string and return the result.
     *
     * @param command the command to evaluate
     * @return the result of evaluation, or null if no result
     * @throws InterpreterException if evaluation fails
     */
    Object eval(String command) throws InterpreterException;

    /**
     * Get the prompt string to display.
     *
     * @return the prompt string (e.g., "bsh % " or "js> ")
     */
    String getPrompt();

    /**
     * Get the completion provider for tab completion.
     *
     * @return the completion provider, or null if not supported
     */
    CompletionProvider getCompletionProvider();

    /**
     * Check if the input is complete (for multi-line detection).
     *
     * <p>This allows interpreters to handle multi-line input where
     * a statement spans multiple lines (e.g., unclosed braces).</p>
     *
     * @param input the current input buffer
     * @return true if the input is complete and ready for evaluation
     */
    boolean isComplete(String input);

    /**
     * Get the display name of this interpreter.
     *
     * @return the interpreter name (e.g., "BeanShell", "JavaScript")
     */
    String getName();

    /**
     * Set a variable in the interpreter's namespace.
     *
     * @param name the variable name
     * @param value the variable value
     * @throws InterpreterException if setting fails
     */
    void set(String name, Object value) throws InterpreterException;

    /**
     * Get a variable from the interpreter's namespace.
     *
     * @param name the variable name
     * @return the variable value, or null if not found
     * @throws InterpreterException if retrieval fails
     */
    Object get(String name) throws InterpreterException;

    /**
     * Reset the interpreter to a clean state.
     *
     * <p>This clears all variables and restores initial state.</p>
     */
    void reset();

    /**
     * Shutdown the interpreter and release resources.
     *
     * <p>Called when the console is being disposed.</p>
     */
    void shutdown();

}
