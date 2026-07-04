package org.jwellman.diagram;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JToggleButton;
import javax.swing.UIManager;

/**
 * Toggle button styled as a flat tab.
 * The LAF renders backgrounds, text, and hover via {@code super.paintComponent()};
 * we only overlay a 3-pixel accent bar at the bottom when selected.
 *
 * When constructed with a {@code fixedWidth}, the button clips long labels with
 * the standard {@code …} ellipsis (handled automatically by the LAF's layout).
 * The full label is shown as a tooltip.
 */
class DiagramTabButton extends JToggleButton {

    private static final int UNDERLINE_H = 3;

    private static final long serialVersionUID = 1L;

    private final int fixedWidth;   // 0 = natural width

    DiagramTabButton(String text) {
        this(text, 0);
    }

    DiagramTabButton(String text, int fixedWidth) {
        super(text);
        this.fixedWidth = fixedWidth;
        setFocusPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        if (fixedWidth > 0) {
            setToolTipText(text);
        }
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        if (fixedWidth > 0) {
            setToolTipText(text);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        int w = (fixedWidth > 0) ? fixedWidth : d.width;
        return new Dimension(w, d.height + UNDERLINE_H);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isSelected()) {
            Color accent = UIManager.getColor("TabbedPane.underlineColor");
            if (accent == null) accent = UIManager.getColor("Component.accentColor");
            if (accent == null) accent = UIManager.getColor("TabbedPane.focus");
            if (accent == null) accent = getForeground();
            g.setColor(accent);
            g.fillRect(0, getHeight() - UNDERLINE_H, getWidth(), UNDERLINE_H);
        }
    }
}
