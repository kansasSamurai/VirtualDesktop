package org.jwellman.console.impl;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.NameCompletion;

import org.jwellman.console.CompletionProvider;
import org.jwellman.console.InterpreterAdapter;
import org.jwellman.console.InterpreterException;

/**
 * Adapter that wraps a BeanShell Interpreter for use with GenericConsole.
 *
 * <p>Can either use a provided interpreter instance or create its own.
 * When integrating with BeanShellService, pass the shared interpreter.</p>
 *
 * @author Rick Wellman
 */
public class BeanShellAdapter implements InterpreterAdapter {

    private static final String DEFAULT_PROMPT = "bsh % ";

    private final Interpreter interpreter;
    private final BeanShellCompletionProvider completionProvider;
    private String prompt;
    private boolean ownsInterpreter;

    /**
     * Create an adapter with a new interpreter.
     */
    public BeanShellAdapter() {
        this(new Interpreter(), true);
    }

    /**
     * Create an adapter wrapping an existing interpreter.
     *
     * <p>Use this when integrating with BeanShellService to share
     * the interpreter across multiple components.</p>
     *
     * @param interpreter the interpreter to wrap
     */
    public BeanShellAdapter(Interpreter interpreter) {
        this(interpreter, false);
    }

    private BeanShellAdapter(Interpreter interpreter, boolean ownsInterpreter) {
        this.interpreter = interpreter;
        this.ownsInterpreter = ownsInterpreter;
        this.prompt = DEFAULT_PROMPT;
        this.completionProvider = new BeanShellCompletionProvider(interpreter);
    }

    @Override
    public Object eval(String command) throws InterpreterException {
        try {
            return interpreter.eval(command);
        } catch (EvalError e) {
            throw new InterpreterException(e.getMessage(), e,
                e.getErrorLineNumber(), -1);
        }
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    /**
     * Set the prompt string.
     *
     * @param prompt the prompt to display
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public CompletionProvider getCompletionProvider() {
        return completionProvider;
    }

    @Override
    public boolean isComplete(String input) {
        // Simple heuristic: check for balanced braces/brackets/parens
        // This is a basic implementation; BeanShell doesn't expose a proper API
        if (input == null || input.trim().isEmpty()) {
            return true;
        }

        int braces = 0;
        int brackets = 0;
        int parens = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"' && !inChar) {
                inString = !inString;
                continue;
            }

            if (c == '\'' && !inString) {
                inChar = !inChar;
                continue;
            }

            if (inString || inChar) {
                continue;
            }

            switch (c) {
                case '{': braces++; break;
                case '}': braces--; break;
                case '[': brackets++; break;
                case ']': brackets--; break;
                case '(': parens++; break;
                case ')': parens--; break;
            }
        }

        return braces == 0 && brackets == 0 && parens == 0 && !inString && !inChar;
    }

    @Override
    public String getName() {
        return "BeanShell";
    }

    @Override
    public void set(String name, Object value) throws InterpreterException {
        try {
            interpreter.set(name, value);
        } catch (EvalError e) {
            throw new InterpreterException("Failed to set variable: " + name, e);
        }
    }

    @Override
    public Object get(String name) throws InterpreterException {
        try {
            return interpreter.get(name);
        } catch (EvalError e) {
            throw new InterpreterException("Failed to get variable: " + name, e);
        }
    }

    @Override
    public void reset() {
        try {
            interpreter.eval("clear();");
        } catch (EvalError e) {
            // Ignore reset failures
        }
    }

    @Override
    public void shutdown() {
        // Only clean up if we own the interpreter
        // If shared (e.g., from BeanShellService), leave it running
        if (ownsInterpreter) {
            // BeanShell doesn't have an explicit shutdown
            // Just clear the namespace
            reset();
        }
    }

    /**
     * Get the underlying BeanShell interpreter.
     *
     * <p>Use with caution; direct manipulation may interfere
     * with the adapter's operation.</p>
     *
     * @return the wrapped Interpreter
     */
    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     * Set the NameCompletion implementation for tab completion.
     *
     * <p>This allows using BeanShell's built-in completion
     * when connected to a console.</p>
     *
     * @param nameCompletion the name completion provider
     */
    public void setNameCompletion(NameCompletion nameCompletion) {
        completionProvider.setNameCompletion(nameCompletion);
    }

}
