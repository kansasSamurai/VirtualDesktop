package org.jwellman.console;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

/**
 * Theme configuration for console appearance.
 *
 * <p>Provides font, color, and margin settings with preset themes
 * for common configurations (dark, light, solarized).</p>
 *
 * <p>Uses builder pattern for fluent configuration:</p>
 * <pre>
 * ConsoleTheme theme = ConsoleTheme.builder()
 *     .font(new Font("Consolas", Font.PLAIN, 14))
 *     .background(Color.BLACK)
 *     .foreground(Color.GREEN)
 *     .build();
 * </pre>
 *
 * @author Rick Wellman
 */
public class ConsoleTheme {

    private final Font font;
    private final Color background;
    private final Color foreground;
    private final Color errorColor;
    private final Color promptColor;
    private final Color outputColor;
    private final Color selectionBackground;
    private final Color selectionForeground;
    private final Color caretColor;
    private final Insets margin;

    private ConsoleTheme(Builder builder) {
        this.font = builder.font;
        this.background = builder.background;
        this.foreground = builder.foreground;
        this.errorColor = builder.errorColor;
        this.promptColor = builder.promptColor;
        this.outputColor = builder.outputColor;
        this.selectionBackground = builder.selectionBackground;
        this.selectionForeground = builder.selectionForeground;
        this.caretColor = builder.caretColor;
        this.margin = builder.margin;
    }

    // ========== Getters ==========

    public Font getFont() {
        return font;
    }

    public Color getBackground() {
        return background;
    }

    public Color getForeground() {
        return foreground;
    }

    public Color getErrorColor() {
        return errorColor;
    }

    public Color getPromptColor() {
        return promptColor;
    }

    public Color getOutputColor() {
        return outputColor;
    }

    public Color getSelectionBackground() {
        return selectionBackground;
    }

    public Color getSelectionForeground() {
        return selectionForeground;
    }

    public Color getCaretColor() {
        return caretColor;
    }

    public Insets getMargin() {
        return margin;
    }

    // ========== Preset Themes ==========

    /**
     * Create a dark theme with light text on dark background.
     *
     * @return a dark theme
     */
    public static ConsoleTheme dark() {
        return builder()
            .font(new Font("Consolas", Font.PLAIN, 14))
            .background(new Color(0x1E1E1E))
            .foreground(new Color(0xD4D4D4))
            .errorColor(new Color(0xF44747))
            .promptColor(new Color(0x569CD6))
            .outputColor(new Color(0xCE9178))
            .selectionBackground(new Color(0x264F78))
            .selectionForeground(new Color(0xFFFFFF))
            .caretColor(new Color(0xAEAFAD))
            .margin(new Insets(7, 5, 7, 5))
            .build();
    }

    /**
     * Create a light theme with dark text on light background.
     *
     * @return a light theme
     */
    public static ConsoleTheme light() {
        return builder()
            .font(new Font("Consolas", Font.PLAIN, 14))
            .background(new Color(0xFAFAFA))
            .foreground(new Color(0x383A42))
            .errorColor(new Color(0xE45649))
            .promptColor(new Color(0x4078F2))
            .outputColor(new Color(0x50A14F))
            .selectionBackground(new Color(0xBFCEFF))
            .selectionForeground(new Color(0x383A42))
            .caretColor(new Color(0x526FFF))
            .margin(new Insets(7, 5, 7, 5))
            .build();
    }

    /**
     * Create a solarized dark theme.
     *
     * @return a solarized dark theme
     */
    public static ConsoleTheme solarizedDark() {
        return builder()
            .font(new Font("Consolas", Font.PLAIN, 14))
            .background(new Color(0x002B36))
            .foreground(new Color(0x839496))
            .errorColor(new Color(0xDC322F))
            .promptColor(new Color(0x268BD2))
            .outputColor(new Color(0x2AA198))
            .selectionBackground(new Color(0x073642))
            .selectionForeground(new Color(0x93A1A1))
            .caretColor(new Color(0xD33682))
            .margin(new Insets(7, 5, 7, 5))
            .build();
    }

    /**
     * Create the classic BeanShell console theme (light gray paper).
     *
     * @return a classic theme matching JConsole defaults
     */
    public static ConsoleTheme classic() {
        return builder()
            .font(new Font("Consolas", Font.PLAIN, 16))
            .background(new Color(0xDADADA))
            .foreground(Color.BLACK)
            .errorColor(Color.RED)
            .promptColor(Color.BLACK)
            .outputColor(Color.BLACK)
            .selectionBackground(new Color(0xA0A0FF))
            .selectionForeground(Color.WHITE)
            .caretColor(Color.BLACK)
            .margin(new Insets(7, 5, 7, 5))
            .build();
    }

    // ========== Builder ==========

    /**
     * Create a new builder with default values.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder initialized from an existing theme.
     *
     * @param theme the theme to copy
     * @return a new Builder with copied values
     */
    public static Builder builder(ConsoleTheme theme) {
        return new Builder()
            .font(theme.font)
            .background(theme.background)
            .foreground(theme.foreground)
            .errorColor(theme.errorColor)
            .promptColor(theme.promptColor)
            .outputColor(theme.outputColor)
            .selectionBackground(theme.selectionBackground)
            .selectionForeground(theme.selectionForeground)
            .caretColor(theme.caretColor)
            .margin(theme.margin);
    }

    /**
     * Builder for ConsoleTheme.
     */
    public static class Builder {
        private Font font = new Font("Consolas", Font.PLAIN, 14);
        private Color background = new Color(0xDADADA);
        private Color foreground = Color.BLACK;
        private Color errorColor = Color.RED;
        private Color promptColor = Color.BLACK;
        private Color outputColor = Color.BLACK;
        private Color selectionBackground = new Color(0xA0A0FF);
        private Color selectionForeground = Color.WHITE;
        private Color caretColor = Color.BLACK;
        private Insets margin = new Insets(7, 5, 7, 5);

        public Builder font(Font font) {
            this.font = font;
            return this;
        }

        public Builder background(Color background) {
            this.background = background;
            return this;
        }

        public Builder foreground(Color foreground) {
            this.foreground = foreground;
            return this;
        }

        public Builder errorColor(Color errorColor) {
            this.errorColor = errorColor;
            return this;
        }

        public Builder promptColor(Color promptColor) {
            this.promptColor = promptColor;
            return this;
        }

        public Builder outputColor(Color outputColor) {
            this.outputColor = outputColor;
            return this;
        }

        public Builder selectionBackground(Color selectionBackground) {
            this.selectionBackground = selectionBackground;
            return this;
        }

        public Builder selectionForeground(Color selectionForeground) {
            this.selectionForeground = selectionForeground;
            return this;
        }

        public Builder caretColor(Color caretColor) {
            this.caretColor = caretColor;
            return this;
        }

        public Builder margin(Insets margin) {
            this.margin = margin;
            return this;
        }

        public ConsoleTheme build() {
            return new ConsoleTheme(this);
        }
    }

}
