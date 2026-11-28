package org.katacode.agents.auditor.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.border.AbstractBorder;

/**
 * Reserves the left edge of a list cell for a short rounded marker bar that
 * indicates "selected" or "waived". A null colour hides the bar but keeps its
 * space, so cell content never shifts.
 */
@SuppressWarnings("serial")
class StatusMarkerBorder extends AbstractBorder {

    private static final int MARKER_X = 9;
    private static final int MARKER_WIDTH = 4;
    private static final int MARKER_HEIGHT = 24;
    private static final int CONTENT_GAP = 12;
    private static final int RIGHT_PADDING = 11;

    private Color color;

    void setColor(Color color) {
        this.color = color;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        if (color == null) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillRoundRect(x + MARKER_X, y + (height - MARKER_HEIGHT) / 2,
                MARKER_WIDTH, MARKER_HEIGHT, MARKER_WIDTH, MARKER_WIDTH);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, new Insets(0, 0, 0, 0));
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(0, MARKER_X + MARKER_WIDTH + CONTENT_GAP, 0, RIGHT_PADDING);
        return insets;
    }
}
