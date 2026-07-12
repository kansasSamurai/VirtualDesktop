package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jwellman.diagram.api.EdgeAttributes.LineStyle;

/**
 * Small square button representing one {@link LineStyle} choice (Solid / Dashed),
 * painted as a short line sample. Click to select; the onSelected callback receives
 * this button so the caller can manage exclusive-selection state across a row.
 */
class LineStyleButton extends JPanel {

    private static final int SIZE = 34;

    private final LineStyle lineStyle;
    private boolean active = false;
    private boolean hovered = false;

    private static final long serialVersionUID = 1L;

    LineStyleButton(LineStyle lineStyle, String tooltip, Consumer<LineStyleButton> onSelected) {
        this.lineStyle = lineStyle;

        setToolTipText(tooltip);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelected != null) {
                    onSelected.accept(LineStyleButton.this);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    LineStyle getLineStyleValue() {
        return lineStyle;
    }

    void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();

        g.setColor(backgroundColor());
        g.fillRect(0, 0, w, h);
        g.setColor(borderColor());
        g.drawRect(0, 0, w - 1, h - 1);

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(inkColor());
            int midY = h / 2;
            int margin = 6;
            if (lineStyle == LineStyle.DASHED) {
                g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    10f, new float[]{4, 3}, 0f));
            } else {
                g2d.setStroke(new BasicStroke(1.5f));
            }
            g2d.drawLine(margin, midY, w - margin, midY);
        } finally {
            g2d.dispose();
        }
    }

    private Color backgroundColor() {
        if (active) {
            Color sel = UIManager.getColor("List.selectionBackground");
            return sel != null ? sel : Color.LIGHT_GRAY;
        }
        if (hovered) {
            return blendedHover();
        }
        Color bg = UIManager.getColor("Panel.background");
        return bg != null ? bg : Color.WHITE;
    }

    private Color blendedHover() {
        Color sel = UIManager.getColor("List.selectionBackground");
        Color bg  = UIManager.getColor("Panel.background");
        if (sel == null || bg == null) {
            return bg != null ? bg : Color.WHITE;
        }
        int r = (sel.getRed()   * 30 + bg.getRed()   * 70) / 100;
        int g = (sel.getGreen() * 30 + bg.getGreen() * 70) / 100;
        int b = (sel.getBlue()  * 30 + bg.getBlue()  * 70) / 100;
        return new Color(r, g, b);
    }

    private static Color borderColor() {
        Color c = UIManager.getColor("Separator.foreground");
        return c != null ? c : Color.GRAY;
    }

    private static Color inkColor() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : Color.DARK_GRAY;
    }
}
