package org.jwellman.console.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.jwellman.console.CompletionProvider;

/**
 * Basic completion provider for Nashorn JavaScript.
 *
 * <p>Provides completion based on variables in the engine's bindings
 * and common JavaScript globals.</p>
 *
 * @author Rick Wellman
 */
public class NashornCompletionProvider implements CompletionProvider {

    /** Common JavaScript globals to include in completion. */
    private static final String[] JS_GLOBALS = {
        "Array", "Boolean", "Date", "Error", "Function", "JSON",
        "Math", "Number", "Object", "RegExp", "String",
        "console", "print", "load", "quit", "exit",
        "parseInt", "parseFloat", "isNaN", "isFinite",
        "encodeURI", "decodeURI", "encodeURIComponent", "decodeURIComponent",
        "undefined", "null", "true", "false", "NaN", "Infinity",
        // Nashorn-specific
        "Java", "Packages", "JavaImporter",
        // Common keywords
        "var", "let", "const", "function", "return", "if", "else",
        "for", "while", "do", "switch", "case", "break", "continue",
        "try", "catch", "finally", "throw", "new", "this", "typeof",
        "instanceof", "in", "delete", "void"
    };

    private final ScriptEngine engine;

    /**
     * Create a completion provider for the given engine.
     *
     * @param engine the Nashorn engine
     */
    public NashornCompletionProvider(ScriptEngine engine) {
        this.engine = engine;
    }

    @Override
    public String[] complete(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        // Extract the part to complete
        String part = extractCompletionPart(text);
        if (part.length() < 1) {
            return new String[0];
        }

        List<String> matches = new ArrayList<String>();

        // Check if this is a property access (contains '.')
        int dotIndex = part.lastIndexOf('.');
        if (dotIndex > 0) {
            // Property completion - more complex
            String objPart = part.substring(0, dotIndex);
            String propPart = part.substring(dotIndex + 1);
            completeProperty(objPart, propPart, matches);
        } else {
            // Simple identifier completion
            completeIdentifier(part, matches);
        }

        Collections.sort(matches);
        return matches.toArray(new String[0]);
    }

    @Override
    public String[] complete(String fullLine, int cursorPosition) {
        if (cursorPosition > fullLine.length()) {
            cursorPosition = fullLine.length();
        }
        return complete(fullLine.substring(0, cursorPosition));
    }

    private void completeIdentifier(String prefix, List<String> matches) {
        String lowerPrefix = prefix.toLowerCase();

        // Add matching globals
        for (String global : JS_GLOBALS) {
            if (global.toLowerCase().startsWith(lowerPrefix)) {
                matches.add(global);
            }
        }

        // Add matching bindings
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        if (bindings != null) {
            Set<String> keys = bindings.keySet();
            for (String key : keys) {
                if (key.toLowerCase().startsWith(lowerPrefix) && !matches.contains(key)) {
                    matches.add(key);
                }
            }
        }
    }

    private void completeProperty(String objExpr, String propPrefix, List<String> matches) {
        try {
            // Try to evaluate the object expression
            Object obj = engine.eval(objExpr);
            if (obj == null) {
                return;
            }

            String lowerPrefix = propPrefix.toLowerCase();

            // For Java objects, try to get properties/methods
            Class<?> clazz = obj.getClass();

            // Get methods
            java.lang.reflect.Method[] methods = clazz.getMethods();
            for (java.lang.reflect.Method m : methods) {
                String name = m.getName();
                if (name.toLowerCase().startsWith(lowerPrefix)) {
                    String completion = objExpr + "." + name;
                    if (!matches.contains(completion)) {
                        matches.add(completion);
                    }
                }
            }

            // Get fields
            java.lang.reflect.Field[] fields = clazz.getFields();
            for (java.lang.reflect.Field f : fields) {
                String name = f.getName();
                if (name.toLowerCase().startsWith(lowerPrefix)) {
                    String completion = objExpr + "." + name;
                    if (!matches.contains(completion)) {
                        matches.add(completion);
                    }
                }
            }

        } catch (Exception e) {
            // Evaluation failed, can't provide property completions
        }
    }

    /**
     * Extract the part to complete from the input text.
     *
     * @param text the input text
     * @return the portion to complete
     */
    private String extractCompletionPart(String text) {
        int i = text.length() - 1;

        // Walk backward to find start of identifier/property chain
        while (i >= 0) {
            char c = text.charAt(i);
            if (Character.isJavaIdentifierPart(c) || c == '.') {
                i--;
            } else {
                break;
            }
        }

        return text.substring(i + 1);
    }

}
