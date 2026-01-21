package org.jwellman.console.ui;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jwellman.console.ConsoleTheme;

/**
 * Protected text pane for console input/output.
 *
 * <p>Provides special cut/paste behavior that respects the command
 * input area boundary (cmdStart). Text before cmdStart is output
 * and should not be modified.</p>
 *
 * @author Rick Wellman
 */
public class ConsoleTextPane extends JTextPane {

    private static final long serialVersionUID = 1L;

    /** Callback to get the current command start position. */
    private CommandStartProvider cmdStartProvider;

    /** Callback to ensure caret is in editable area before paste. */
    private Runnable forceCaretToEnd;

    /**
     * Create a console text pane.
     */
    public ConsoleTextPane() {
        super(new DefaultStyledDocument());
        setText("");
    }

    /**
     * Set the callback for getting command start position.
     *
     * @param provider the provider callback
     */
    public void setCommandStartProvider(CommandStartProvider provider) {
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
     * Apply a theme to this text pane.
     *
     * @param theme the theme to apply
     */
    public void applyTheme(ConsoleTheme theme) {
        setBackground(theme.getBackground());
        setCaretColor(theme.getCaretColor());
        setSelectionColor(theme.getSelectionBackground());
        setSelectedTextColor(theme.getSelectionForeground());
        setMargin(theme.getMargin());
        applyFont(theme.getFont(), theme.getForeground());
    }

    /**
     * Apply a font and foreground color to the entire document.
     *
     * @param font the font to apply
     * @param foreground the foreground color
     */
    public void applyFont(Font font, Color foreground) {
        MutableAttributeSet attrs = getInputAttributes();
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        StyleConstants.setBold(attrs, font.isBold());
        StyleConstants.setItalic(attrs, font.isItalic());
        if (foreground != null) {
            StyleConstants.setForeground(attrs, foreground);
        }

        StyledDocument doc = getStyledDocument();
        doc.setCharacterAttributes(0, doc.getLength() + 1, attrs, false);
    }

    /**
     * Set style attributes for subsequent text.
     *
     * @param color the foreground color
     */
    public void setTextStyle(Color color) {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        if (color != null) {
            StyleConstants.setForeground(attrs, color);
        }
        setCharacterAttributes(attrs, false);
    }

    /**
     * Set style with font and color.
     *
     * @param font the font (or null to keep current)
     * @param color the color (or null to keep current)
     */
    public void setTextStyle(Font font, Color color) {
        MutableAttributeSet attrs = new SimpleAttributeSet();
        if (color != null) {
            StyleConstants.setForeground(attrs, color);
        }
        if (font != null) {
            StyleConstants.setFontFamily(attrs, font.getFamily());
            StyleConstants.setFontSize(attrs, font.getSize());
            StyleConstants.setBold(attrs, font.isBold());
            StyleConstants.setItalic(attrs, font.isItalic());
        }
        setCharacterAttributes(attrs, false);
    }

    /**
     * Get the current style attributes.
     *
     * @return current attributes
     */
    public AttributeSet getCurrentStyle() {
        return getCharacterAttributes();
    }

    /**
     * Restore previous style attributes.
     *
     * @param attrs the attributes to restore
     */
    public void restoreStyle(AttributeSet attrs) {
        setCharacterAttributes(attrs, true);
    }

    private int getCmdStart() {
        if (cmdStartProvider != null) {
            return cmdStartProvider.getCommandStart();
        }
        return 0;
    }

    /**
     * Callback interface for getting command start position.
     */
    public interface CommandStartProvider {
        int getCommandStart();
    }

}
