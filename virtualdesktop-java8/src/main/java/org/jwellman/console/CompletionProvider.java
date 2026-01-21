package org.jwellman.console;

/**
 * Provider interface for command/name completion (tab completion).
 *
 * <p>Implementations provide completion suggestions based on the current
 * input text and cursor position.</p>
 *
 * @author Rick Wellman
 */
public interface CompletionProvider {

    /**
     * Get completions for the given text fragment.
     *
     * <p>This simple form assumes completion is needed at the end of the text.</p>
     *
     * @param text the text to complete
     * @return array of possible completions, or empty array if none
     */
    String[] complete(String text);

    /**
     * Get completions for the given line at the specified cursor position.
     *
     * <p>This form allows completion at any position within the line,
     * supporting more sophisticated completion scenarios.</p>
     *
     * @param fullLine the complete input line
     * @param cursorPosition the cursor position within the line
     * @return array of possible completions, or empty array if none
     */
    String[] complete(String fullLine, int cursorPosition);

}
