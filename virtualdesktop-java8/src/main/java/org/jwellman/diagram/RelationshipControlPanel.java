package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.RelationshipType;

/**
 * Visual row tile for one UML relationship type — analogous to LayerControlPanel.
 * Shows a mini line preview on the left and the relationship name on the right.
 * Click to select; the onSelected callback receives this panel so the caller can
 * manage the exclusive-selection state across a list of tiles.
 */
class RelationshipControlPanel extends JPanel {

    private static final int PREVIEW_W   = 80;
    private static final int PREVIEW_H   = 28;
    private static final int DIAMOND_SZ  = 7;
    private static final int ARROW_SZ    = 7;

    private final RelationshipType relType;
    private boolean active = false;

    private static final long serialVersionUID = 1L;

    public RelationshipControlPanel(RelationshipType relType,
                                    Consumer<RelationshipControlPanel> onSelected) {
        this.relType = relType;

        setLayout(new BorderLayout(6, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        updateBackground();

        JPanel preview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintPreview((Graphics2D) g.create());
            }
        };
        preview.setOpaque(false);
        preview.setPreferredSize(new Dimension(PREVIEW_W, PREVIEW_H));
        add(preview, BorderLayout.WEST);

        JLabel nameLabel = new JLabel(relType.name);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        add(nameLabel, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelected != null) {
                    onSelected.accept(RelationshipControlPanel.this);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!active) {
                    setBackground(hoverBackground());
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateBackground();
                repaint();
            }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        updateBackground();
        repaint();
    }

    public RelationshipType getRelationshipType() {
        return relType;
    }

    // ---------------------------------------------------------------
    // Preview painting
    // ---------------------------------------------------------------

    private void paintPreview(Graphics2D g) {
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int midY   = PREVIEW_H / 2;
            int leftX  = 4;
            int rightX = PREVIEW_W - 4;

            Color ink = UIManager.getColor("Label.foreground");
            if (ink == null) {
                ink = Color.DARK_GRAY;
            }
            Color bg = getBackground();

            EdgeAttributes attrs = relType.attributes;

            // Compute drawable extents based on decorations present
            int lineStart = leftX;
            int lineEnd   = rightX;
            boolean hasSourceDiamond = attrs.getSourceArrowType() == EdgeAttributes.ArrowType.OPEN_DIAMOND
                || attrs.getSourceArrowType() == EdgeAttributes.ArrowType.FILLED_DIAMOND;
            boolean hasTargetArrow = attrs.getArrowType() != EdgeAttributes.ArrowType.NONE;

            if (hasSourceDiamond) {
                lineStart = leftX + DIAMOND_SZ * 2 + 1;
            }
            if (hasTargetArrow) {
                lineEnd = rightX - ARROW_SZ - 1;
            }

            // Line
            g.setColor(ink);
            if (attrs.getLineStyle() == EdgeAttributes.LineStyle.DASHED) {
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    10f, new float[]{5, 3}, 0f));
            } else {
                g.setStroke(new BasicStroke(1.5f));
            }
            g.drawLine(lineStart, midY, lineEnd, midY);
            g.setStroke(new BasicStroke(1.5f));

            // Source diamond (left end)
            if (hasSourceDiamond) {
                int tipX = leftX;
                int cx   = leftX + DIAMOND_SZ;
                Polygon diamond = new Polygon();
                diamond.addPoint(tipX,              midY);
                diamond.addPoint(cx,                midY - DIAMOND_SZ / 2);
                diamond.addPoint(cx + DIAMOND_SZ,   midY);
                diamond.addPoint(cx,                midY + DIAMOND_SZ / 2);

                if (attrs.getSourceArrowType() == EdgeAttributes.ArrowType.FILLED_DIAMOND) {
                    g.setColor(ink);
                    g.fill(diamond);
                } else {
                    g.setColor(bg);
                    g.fill(diamond);
                    g.setColor(ink);
                    g.draw(diamond);
                }
            }

            // Target arrowhead (right end)
            if (hasTargetArrow) {
                g.setColor(ink);
                Polygon arrow = new Polygon();
                arrow.addPoint(rightX,             midY);
                arrow.addPoint(rightX - ARROW_SZ,  midY - ARROW_SZ / 2);
                arrow.addPoint(rightX - ARROW_SZ,  midY + ARROW_SZ / 2);

                if (attrs.getArrowType() == EdgeAttributes.ArrowType.OPEN) {
                    g.setColor(bg);
                    g.fill(arrow);
                    g.setColor(ink);
                    g.draw(arrow);
                } else {
                    g.fill(arrow);
                }
            }
        } finally {
            g.dispose();
        }
    }

    // ---------------------------------------------------------------
    // Background helpers
    // ---------------------------------------------------------------

    private void updateBackground() {
        if (active) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("Panel.background"));
        }
    }

    private Color hoverBackground() {
        Color sel = UIManager.getColor("List.selectionBackground");
        Color bg  = UIManager.getColor("Panel.background");
        if (sel == null || bg == null) {
            return UIManager.getColor("Panel.background");
        }
        int r = (sel.getRed()   * 30 + bg.getRed()   * 70) / 100;
        int g = (sel.getGreen() * 30 + bg.getGreen() * 70) / 100;
        int b = (sel.getBlue()  * 30 + bg.getBlue()  * 70) / 100;
        return new Color(r, g, b);
    }
}
