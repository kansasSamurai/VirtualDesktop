package org.katacode.appstage.internal;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * Samples representative colors from the current Look and Feel.
 * Prefers a live {@link JTextField} (as the user typically sees text surfaces),
 * with {@link UIManager} keys as fallback.
 */
final class LafColors {

    private LafColors() {
    }

    static Color textBackground() {
        JTextField probe = new JTextField();
        Color c = probe.getBackground();
        if (c != null) {
            return c;
        }
        return firstNonNull(
                UIManager.getColor("TextField.background"),
                UIManager.getColor("Panel.background"),
                Color.WHITE);
    }

    static Color textForeground() {
        JTextField probe = new JTextField();
        Color c = probe.getForeground();
        if (c != null) {
            return c;
        }
        return firstNonNull(
                UIManager.getColor("TextField.foreground"),
                UIManager.getColor("Label.foreground"),
                Color.BLACK);
    }

    static Color border() {
        return firstNonNull(
                UIManager.getColor("Component.borderColor"),
                UIManager.getColor("TextField.shadow"),
                UIManager.getColor("controlShadow"),
                UIManager.getColor("Separator.foreground"),
                mix(textForeground(), textBackground(), 0.35f));
    }

    /**
     * Apply text surface colors to a subtree so labels / panels match the LAF.
     */
    static void applyTextSurface(Component root) {
        Color bg = textBackground();
        Color fg = textForeground();
        applyRecursive(root, bg, fg);
    }

    private static void applyRecursive(Component c, Color bg, Color fg) {
        if (c == null) {
            return;
        }
        c.setForeground(fg);
        if (c instanceof javax.swing.JPanel
                || c instanceof javax.swing.JLabel
                || c instanceof javax.swing.AbstractButton) {
            // Keep buttons on their LAF defaults for chrome; only force panel/label bg.
            if (!(c instanceof javax.swing.AbstractButton)) {
                c.setBackground(bg);
            }
        }
        if (c instanceof Container) {
            Component[] kids = ((Container) c).getComponents();
            for (int i = 0; i < kids.length; i++) {
                applyRecursive(kids[i], bg, fg);
            }
        }
    }

    private static Color firstNonNull(Color... colors) {
        if (colors == null) {
            return Color.GRAY;
        }
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] != null) {
                return colors[i];
            }
        }
        return Color.GRAY;
    }

    private static Color mix(Color a, Color b, float amountOfA) {
        float t = amountOfA;
        if (t < 0f) {
            t = 0f;
        }
        if (t > 1f) {
            t = 1f;
        }
        int r = Math.round(a.getRed() * t + b.getRed() * (1f - t));
        int g = Math.round(a.getGreen() * t + b.getGreen() * (1f - t));
        int bl = Math.round(a.getBlue() * t + b.getBlue() * (1f - t));
        return new Color(r, g, bl);
    }
}
