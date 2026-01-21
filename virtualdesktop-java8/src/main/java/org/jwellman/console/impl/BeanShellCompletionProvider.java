package org.jwellman.console.impl;

import bsh.Interpreter;
import bsh.util.NameCompletion;

import org.jwellman.console.CompletionProvider;

/**
 * Completion provider for BeanShell using its NameCompletion API.
 *
 * @author Rick Wellman
 */
public class BeanShellCompletionProvider implements CompletionProvider {

    private NameCompletion nameCompletion;

    /**
     * Create a completion provider for the given interpreter.
     *
     * @param interpreter the BeanShell interpreter
     */
    public BeanShellCompletionProvider(Interpreter interpreter) {
        // NameCompletion is typically set externally when console connects
        // via setNameCompletion()
        this.nameCompletion = null;
    }

    /**
     * Set the BeanShell NameCompletion instance.
     *
     * <p>This is typically obtained from the interpreter's namespace
     * when connected to a GUI console.</p>
     *
     * @param nameCompletion the name completion provider
     */
    public void setNameCompletion(NameCompletion nameCompletion) {
        this.nameCompletion = nameCompletion;
    }

    @Override
    public String[] complete(String text) {
        if (nameCompletion == null || text == null) {
            return new String[0];
        }

        // Extract the identifier to complete from the end of text
        String part = extractCompletionPart(text);
        if (part.length() < 2) {
            // Require minimum length for meaningful completion
            return new String[0];
        }

        String[] completions = nameCompletion.completeName(part);
        return completions != null ? completions : new String[0];
    }

    @Override
    public String[] complete(String fullLine, int cursorPosition) {
        if (cursorPosition > fullLine.length()) {
            cursorPosition = fullLine.length();
        }
        // Use text up to cursor position
        return complete(fullLine.substring(0, cursorPosition));
    }

    /**
     * Extract the part to complete from the input text.
     *
     * <p>Walks backward from the end to find the start of the
     * identifier or method chain being completed.</p>
     *
     * @param text the input text
     * @return the portion to complete
     */
    private String extractCompletionPart(String text) {
        int i = text.length() - 1;

        // Walk backward to find start of identifier/method chain
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
