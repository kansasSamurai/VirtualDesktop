package org.jwellman.console.ui;

import javax.swing.text.JTextComponent;

import org.jwellman.console.ConsoleTheme;

/**
 * Factory for creating console text components.
 *
 * <p>Supports both standard JTextPane-based (ConsoleTextPane) and
 * RSyntaxTextArea-based (SyntaxConsoleTextPane) text components.</p>
 *
 * <p>Note: Returns JTextComponent since ConsoleTextPane extends JTextPane
 * while SyntaxConsoleTextPane extends RSyntaxTextArea (which extends JTextArea).
 * JTextComponent is the common ancestor.</p>
 *
 * @author Rick Wellman
 */
public class TextPaneFactory {

    /**
     * Text pane type enumeration.
     */
    public enum TextPaneType {
        /** Standard JTextPane with styled document. */
        STANDARD,

        /** RSyntaxTextArea with Java syntax highlighting. */
        SYNTAX_JAVA,

        /** RSyntaxTextArea with JavaScript syntax highlighting. */
        SYNTAX_JAVASCRIPT,

        /** RSyntaxTextArea with no syntax highlighting. */
        SYNTAX_NONE
    }

    /**
     * Create a console text component of the specified type.
     *
     * @param type the text pane type
     * @return the text component (ConsoleTextPane or SyntaxConsoleTextPane)
     */
    public static JTextComponent create(TextPaneType type) {
        switch (type) {
            case SYNTAX_JAVA:
                return new SyntaxConsoleTextPane(SyntaxConsoleTextPane.SyntaxMode.JAVA);

            case SYNTAX_JAVASCRIPT:
                return new SyntaxConsoleTextPane(SyntaxConsoleTextPane.SyntaxMode.JAVASCRIPT);

            case SYNTAX_NONE:
                return new SyntaxConsoleTextPane(SyntaxConsoleTextPane.SyntaxMode.NONE);

            case STANDARD:
            default:
                return new ConsoleTextPane();
        }
    }

    /**
     * Create a console text component with theme applied.
     *
     * @param type the text pane type
     * @param theme the theme to apply
     * @return the themed text component
     */
    public static JTextComponent create(TextPaneType type, ConsoleTheme theme) {
        JTextComponent component = create(type);

        if (component instanceof ConsoleTextPane) {
            ((ConsoleTextPane) component).applyTheme(theme);
        } else if (component instanceof SyntaxConsoleTextPane) {
            ((SyntaxConsoleTextPane) component).applyTheme(theme);
        }

        return component;
    }

    /**
     * Check if RSyntaxTextArea is available.
     *
     * @return true if RSyntaxTextArea classes can be loaded
     */
    public static boolean isSyntaxHighlightingAvailable() {
        try {
            Class.forName("org.fife.ui.rsyntaxtextarea.RSyntaxTextArea");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Get the recommended type for the given interpreter.
     *
     * @param interpreterName the interpreter name
     * @return the recommended text pane type
     */
    public static TextPaneType getRecommendedType(String interpreterName) {
        if (!isSyntaxHighlightingAvailable()) {
            return TextPaneType.STANDARD;
        }

        if (interpreterName == null) {
            return TextPaneType.STANDARD;
        }

        String lower = interpreterName.toLowerCase();
        if (lower.contains("beanshell") || lower.contains("java")) {
            return TextPaneType.SYNTAX_JAVA;
        } else if (lower.contains("javascript") || lower.contains("nashorn") || lower.contains("js")) {
            return TextPaneType.SYNTAX_JAVASCRIPT;
        }

        return TextPaneType.STANDARD;
    }

    // Private constructor to prevent instantiation
    private TextPaneFactory() {}

}
