package org.jwellman.console.impl;

import java.io.PrintStream;
import java.io.PrintWriter;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jwellman.console.CompletionProvider;
import org.jwellman.console.InterpreterAdapter;
import org.jwellman.console.InterpreterException;

/**
 * Adapter for the Nashorn JavaScript engine.
 *
 * <p>Uses the javax.script API to integrate Nashorn (available in Java 8)
 * as an interpreter for the GenericConsole.</p>
 *
 * <p>Note: Nashorn was deprecated in Java 11 and removed in Java 15.
 * For Java 8 compatibility this remains useful.</p>
 *
 * @author Rick Wellman
 */
public class NashornAdapter implements InterpreterAdapter {

    private static final String DEFAULT_PROMPT = "js> ";

    private final ScriptEngine engine;
    private final NashornCompletionProvider completionProvider;
    private String prompt;

    /**
     * Create a Nashorn adapter with default settings.
     *
     * @throws InterpreterException if Nashorn engine is not available
     */
    public NashornAdapter() throws InterpreterException {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.engine = manager.getEngineByName("nashorn");

        if (this.engine == null) {
            // Try alternative names
            ScriptEngine alt = manager.getEngineByName("JavaScript");
            if (alt == null) {
                throw new InterpreterException(
                    "Nashorn JavaScript engine not available. " +
                    "Make sure you are running on Java 8-14.");
            }
            throw new InterpreterException(
                "Nashorn engine not found. Found: " + alt.getFactory().getEngineName());
        }

        this.prompt = DEFAULT_PROMPT;
        this.completionProvider = new NashornCompletionProvider(engine);

        // Initialize with some helpful functions
        initializeEnvironment();
    }

    /**
     * Create a Nashorn adapter with an existing engine.
     *
     * @param engine the ScriptEngine to use
     */
    public NashornAdapter(ScriptEngine engine) {
        this.engine = engine;
        this.prompt = DEFAULT_PROMPT;
        this.completionProvider = new NashornCompletionProvider(engine);
    }

    private void initializeEnvironment() {
        try {
            // Nashorn's built-in print() function respects the ScriptContext's writer,
            // so we don't need to override it. Just add helper functions.

            // Add a simple help function
            engine.eval(
                "var help = function() { " +
                "  print('JavaScript Console (Nashorn)'); " +
                "  print('Available functions:'); " +
                "  print('  print(...)  - Print to console'); " +
                "  print('  load(file) - Load and execute a script file'); " +
                "  print('  quit()     - Exit (not recommended in vapp)'); " +
                "  print(''); " +
                "  print('Java interop:'); " +
                "  print('  var HashMap = Java.type(\"java.util.HashMap\");'); " +
                "  print('  var map = new HashMap();'); " +
                "}"
            );
        } catch (ScriptException e) {
            System.err.println("Failed to initialize Nashorn environment: " + e.getMessage());
        }
    }

    @Override
    public Object eval(String command) throws InterpreterException {
        try {
            return engine.eval(command);
        } catch (ScriptException e) {
            throw new InterpreterException(e.getMessage(), e,
                e.getLineNumber(), e.getColumnNumber());
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

    /**
     * Set the output stream for print() and other output.
     * This redirects Nashorn's output to the specified stream.
     *
     * @param out the PrintStream to use for output
     */
    public void setOutput(PrintStream out) {
        if (out != null) {
            engine.getContext().setWriter(new PrintWriter(out, true));
            engine.getContext().setErrorWriter(new PrintWriter(out, true));
        }
    }

    @Override
    public CompletionProvider getCompletionProvider() {
        return completionProvider;
    }

    @Override
    public boolean isComplete(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }

        // Check for balanced braces/brackets/parens
        int braces = 0;
        int brackets = 0;
        int parens = 0;
        boolean inString = false;
        boolean inSingleString = false;
        boolean inTemplate = false;
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

            // Handle template literals
            if (c == '`') {
                inTemplate = !inTemplate;
                continue;
            }

            if (c == '"' && !inSingleString && !inTemplate) {
                inString = !inString;
                continue;
            }

            if (c == '\'' && !inString && !inTemplate) {
                inSingleString = !inSingleString;
                continue;
            }

            if (inString || inSingleString || inTemplate) {
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

        return braces == 0 && brackets == 0 && parens == 0
            && !inString && !inSingleString && !inTemplate;
    }

    @Override
    public String getName() {
        return "JavaScript (Nashorn)";
    }

    @Override
    public void set(String name, Object value) throws InterpreterException {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(name, value);
    }

    @Override
    public Object get(String name) throws InterpreterException {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        return bindings.get(name);
    }

    @Override
    public void reset() {
        // Create new bindings to clear state
        Bindings bindings = engine.createBindings();
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        initializeEnvironment();
    }

    @Override
    public void shutdown() {
        // Nashorn doesn't require explicit shutdown
    }

    /**
     * Get the underlying ScriptEngine.
     *
     * @return the Nashorn engine
     */
    public ScriptEngine getEngine() {
        return engine;
    }

}
