package org.jwellman.console.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bsh.BshMethod;
import bsh.Interpreter;
import bsh.NameSpace;

import org.jwellman.console.CompletionProvider;

/**
 * Completion provider for BeanShell that queries the live interpreter namespace.
 *
 * <p>Completes variable names, method names, and scripted-object members
 * from the current runtime state, so session variables like {@code jvd},
 * {@code wm}, etc. are available for Tab completion.</p>
 *
 * <p>For {@code foo.bar} style input, the prefix before the dot is resolved
 * to a scripted object and its own namespace is searched for {@code bar}.</p>
 *
 * @author Rick Wellman
 */
public class BeanShellCompletionProvider implements CompletionProvider {

    private final Interpreter interpreter;

    public BeanShellCompletionProvider(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public String[] complete(String text) {
        if (interpreter == null || text == null) {
            return new String[0];
        }

        String part = extractCompletionPart(text);
        if (part.length() < 2) {
            return new String[0];
        }

        int dotIdx = part.lastIndexOf('.');
        if (dotIdx >= 0) {
            return completeMember(part.substring(0, dotIdx), part.substring(dotIdx + 1));
        }

        return completeInNamespace(interpreter.getNameSpace(), part);
    }

    @Override
    public String[] complete(String fullLine, int cursorPosition) {
        if (cursorPosition > fullLine.length()) {
            cursorPosition = fullLine.length();
        }
        return complete(fullLine.substring(0, cursorPosition));
    }

    /**
     * Complete {@code memberPrefix} against the namespace of the scripted
     * object named {@code objectName} in the interpreter's namespace.
     */
    private String[] completeMember(String objectName, String memberPrefix) {
        try {
            Object obj = interpreter.get(objectName);
            if (obj instanceof bsh.This) {
                NameSpace ns = ((bsh.This) obj).getNameSpace();
                String[] raw = completeInNamespace(ns, memberPrefix);
                // Prefix results with "objectName." so the console can append correctly
                String[] prefixed = new String[raw.length];
                for (int i = 0; i < raw.length; i++) {
                    prefixed[i] = objectName + "." + raw[i];
                }
                return prefixed;
            }
        } catch (Exception e) {
            // Object not found or not a scripted object — fall through
        }
        return new String[0];
    }

    /**
     * Return all variable and method names in {@code ns} that start with {@code prefix}.
     */
    private String[] completeInNamespace(NameSpace ns, String prefix) {
        if (ns == null) {
            return new String[0];
        }

        List<String> matches = new ArrayList<String>();

        try {
            String[] varNames = ns.getVariableNames();
            if (varNames != null) {
                for (String name : varNames) {
                    if (name.startsWith(prefix)) {
                        matches.add(name);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        try {
            BshMethod[] methods = ns.getMethods();
            if (methods != null) {
                for (BshMethod m : methods) {
                    String name = m.getName();
                    if (name.startsWith(prefix) && !matches.contains(name)) {
                        matches.add(name);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        Collections.sort(matches);
        return matches.toArray(new String[0]);
    }

    /**
     * Walk backward from the end of {@code text} to find the identifier
     * or dotted chain being completed.
     */
    private String extractCompletionPart(String text) {
        int i = text.length() - 1;
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
