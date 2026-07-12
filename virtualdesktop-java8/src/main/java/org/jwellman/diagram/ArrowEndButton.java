package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jwellman.diagram.api.EdgeAttributes.ArrowType;

/**
 * Small square button representing one {@link ArrowType} choice for a single end of
 * a connection, painted as a short line stub with the actual glyph (open/filled
 * triangle, open/filled diamond, or a bare line for NONE). The glyph mirrors
 * direction depending on which end of the connection this button represents, so a
 * row of these reads naturally as "what does this end look like." Click to select;
 * the onSelected callback receives this button so the caller can manage
 * exclusive-selection state across a row.
 */
class ArrowEndButton extends JPanel {

    private static final int SIZE       = 34;
    private static final int ARROW_SZ   = 7;
    private static final int DIAMOND_SZ = 7;

    private final ArrowType arrowType;
    private final boolean pointRight; // true = glyph tip points right (target/"Right Arrow" row)
    private boolean active = false;
    private boolean hovered = false;

    private static final long serialVersionUID = 1L;

    ArrowEndButton(ArrowType arrowType, boolean pointRight, String tooltip,
                   Consumer<ArrowEndButton> onSelected) {
        this.arrowType  = arrowType;
        this.pointRight = pointRight;

        setToolTipText(tooltip);
        setPreferredSize(new Dimension(SIZE, SIZE));
        setMinimumSize(new Dimension(SIZE, SIZE));
        setMaximumSize(new Dimension(SIZE, SIZE));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelected != null) {
                    onSelected.accept(ArrowEndButton.this);
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

    ArrowType getArrowTypeValue() {
        return arrowType;
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
            paintGlyph(g2d, w, h);
        } finally {
            g2d.dispose();
        }
    }

    private void paintGlyph(Graphics2D g2d, int w, int h) {
        Color ink = inkColor();
        Color bg  = backgroundColor();
        int midY   = h / 2;
        int margin = 5;

        int stubStart = pointRight ? margin : w - margin;
        int tipX       = pointRight ? w - margin : margin;
        int sign        = pointRight ? 1 : -1;

        g2d.setColor(ink);
        g2d.setStroke(new BasicStroke(1.5f));

        switch (arrowType) {
            case NONE:
                g2d.drawLine(stubStart, midY, tipX, midY);
                break;

            case OPEN:
            case FILLED: {
                int baseX = tipX - sign * ARROW_SZ;
                g2d.drawLine(stubStart, midY, baseX, midY);
                Polygon arrow = new Polygon();
                arrow.addPoint(tipX,  midY);
                arrow.addPoint(baseX, midY - ARROW_SZ / 2);
                arrow.addPoint(baseX, midY + ARROW_SZ / 2);
                if (arrowType == ArrowType.FILLED) {
                    g2d.fill(arrow);
                } else {
                    g2d.setColor(bg);
                    g2d.fill(arrow);
                    g2d.setColor(ink);
                    g2d.draw(arrow);
                }
                break;
            }

            case OPEN_DIAMOND:
            case FILLED_DIAMOND: {
                int cx    = tipX - sign * DIAMOND_SZ;
                int baseX = tipX - sign * DIAMOND_SZ * 2;
                g2d.drawLine(stubStart, midY, baseX, midY);
                Polygon diamond = new Polygon();
                diamond.addPoint(tipX,  midY);
                diamond.addPoint(cx,    midY - DIAMOND_SZ / 2);
                diamond.addPoint(baseX, midY);
                diamond.addPoint(cx,    midY + DIAMOND_SZ / 2);
                if (arrowType == ArrowType.FILLED_DIAMOND) {
                    g2d.fill(diamond);
                } else {
                    g2d.setColor(bg);
                    g2d.fill(diamond);
                    g2d.setColor(ink);
                    g2d.draw(diamond);
                }
                break;
            }

            default:
                break;
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
