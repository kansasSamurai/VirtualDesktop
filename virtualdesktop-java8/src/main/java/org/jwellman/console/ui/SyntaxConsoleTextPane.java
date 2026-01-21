package org.jwellman.console.ui;

import java.awt.Color;
import java.awt.Font;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;

import org.jwellman.console.ConsoleTheme;

/**
 * Console text pane using RSyntaxTextArea for syntax highlighting.
 *
 * <p>Provides syntax-aware editing for the input area with highlighting
 * for Java/BeanShell or JavaScript syntax.</p>
 *
 * <p>Note: RSyntaxTextArea is designed for editing source files, not
 * console I/O. This adapter provides basic integration but some console
 * features may work differently than with the standard ConsoleTextPane.</p>
 *
 * @author Rick Wellman
 */
public class SyntaxConsoleTextPane extends RSyntaxTextArea {

    private static final long serialVersionUID = 1L;

    /** The current syntax mode. */
    public enum SyntaxMode {
        JAVA,
        JAVASCRIPT,
        NONE
    }

    /** Callback to get the current command start position. */
    private ConsoleTextPane.CommandStartProvider cmdStartProvider;

    /** Callback to ensure caret is in editable area before paste. */
    private Runnable forceCaretToEnd;

    private SyntaxMode syntaxMode = SyntaxMode.JAVA;

    /**
     * Create a syntax console text pane with Java syntax.
     */
    public SyntaxConsoleTextPane() {
        this(SyntaxMode.JAVA);
    }

    /**
     * Create a syntax console text pane with specified syntax.
     *
     * @param mode the syntax highlighting mode
     */
    public SyntaxConsoleTextPane(SyntaxMode mode) {
        super();
        this.syntaxMode = mode;
        applySyntaxMode(mode);

        // Configure for console-like usage
        setCodeFoldingEnabled(false);
        setAntiAliasingEnabled(true);
        setAutoIndentEnabled(false);
        setCloseCurlyBraces(false);
        setCloseMarkupTags(false);
        setTabsEmulated(true);
        setTabSize(4);
    }

    /**
     * Set the syntax mode.
     *
     * @param mode the syntax mode
     */
    public void setSyntaxMode(SyntaxMode mode) {
        this.syntaxMode = mode;
        applySyntaxMode(mode);
    }

    /**
     * Get the current syntax mode.
     *
     * @return the syntax mode
     */
    public SyntaxMode getSyntaxMode() {
        return syntaxMode;
    }

    private void applySyntaxMode(SyntaxMode mode) {
        switch (mode) {
            case JAVA:
                setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                break;
            case JAVASCRIPT:
                setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            case NONE:
            default:
                setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                break;
        }
    }

    /**
     * Set the callback for getting command start position.
     *
     * @param provider the provider callback
     */
    public void setCommandStartProvider(ConsoleTextPane.CommandStartProvider provider) {
        this.cmdStartProvider = provider;
    }

    /**
     * Set the callback for forcing caret to editable area.
     *
     * @param callback the callback
     */
    public void setForceCaretToEnd(Runnable callback) {
        this.forceCaretToEnd = callback;
    }

    /**
     * Override cut to copy instead if in output area.
     */
    @Override
    public void cut() {
        int cmdStart = getCmdStart();
        if (getCaretPosition() < cmdStart) {
            super.copy();  // In output area, just copy
        } else {
            super.cut();
        }
    }

    /**
     * Override paste to ensure caret is in editable area.
     */
    @Override
    public void paste() {
        if (forceCaretToEnd != null) {
            forceCaretToEnd.run();
        }
        super.paste();
    }

    /**
     * Apply a ConsoleTheme to this text area.
     *
     * <p>Note: RSyntaxTextArea has its own theme system. This method
     * maps ConsoleTheme colors as best as possible.</p>
     *
     * @param theme the theme to apply
     */
    public void applyTheme(ConsoleTheme theme) {
        setBackground(theme.getBackground());
        setForeground(theme.getForeground());
        setCaretColor(theme.getCaretColor());
        setSelectionColor(theme.getSelectionBackground());
        setSelectedTextColor(theme.getSelectionForeground());
        setMargin(theme.getMargin());
        setFont(theme.getFont());

        // Try to apply matching RSyntaxTextArea theme based on background
        try {
            if (isDarkBackground(theme.getBackground())) {
                Theme rstaTheme = Theme.load(getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
                if (rstaTheme != null) {
                    rstaTheme.apply(this);
                    // Re-apply our custom colors on top
                    setBackground(theme.getBackground());
                    setCaretColor(theme.getCaretColor());
                }
            }
        } catch (Exception e) {
            // Theme loading failed, continue with basic colors
        }
    }

    /**
     * Apply a font to the text area.
     *
     * @param font the font to apply
     * @param foreground the foreground color (may be null)
     */
    public void applyFont(Font font, Color foreground) {
        setFont(font);
        if (foreground != null) {
            setForeground(foreground);
        }
    }

    private int getCmdStart() {
        if (cmdStartProvider != null) {
            return cmdStartProvider.getCommandStart();
        }
        return 0;
    }

    private boolean isDarkBackground(Color bg) {
        // Simple luminance check
        double luminance = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return luminance < 128;
    }

}
