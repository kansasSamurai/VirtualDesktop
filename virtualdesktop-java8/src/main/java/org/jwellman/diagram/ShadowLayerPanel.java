package org.jwellman.diagram;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import org.jwellman.diagram.api.GraphNode;
import org.jwellman.diagram.domain.generic.GenericGraphFactory;

/**
 * Transparent full-canvas panel at SHADOW_LAYER that paints a soft multi-pass
 * drop shadow behind every visible graph node — a rounded-rect silhouette by
 * default, an oval for {@code GenericGraphFactory.NODE_TYPE_CIRCLE} nodes.
 *
 * The circle check is a deliberate, narrow exception to "framework doesn't
 * depend on domain classes": it's a node-type string comparison, not an import
 * of domain painting/behavior, and it's the simplest way to keep a circular
 * node's shadow from looking like a rectangle poking out from behind it. If a
 * third shape-aware shadow silhouette is ever needed, promote this to a
 * GraphNode-level hint instead of adding more type-string branches here.
 *
 * Always passes mouse events through to layers below.
 * Hidden during node drag (suspended by DiagramLayeredPane) to avoid recomputing
 * shadow geometry on every mouseDragged event.
 */
public class ShadowLayerPanel extends JPanel {

    private static final int SHADOW_SIZE = 12;
    private static final int SHADOW_OFFSET_Y = 6;
    private static final float MAX_OPACITY = 0.04f;
    private static final int CORNER_RADIUS = 8;
    private static final long serialVersionUID = 1L;

    public ShadowLayerPanel() {
        setOpaque(false);
        setLayout(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        JLayeredPane pane = (JLayeredPane) getParent();
        if (pane == null) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (Component comp : pane.getComponents()) {
                if (comp instanceof GraphNode && comp.isVisible()) {
                    boolean oval = GenericGraphFactory.NODE_TYPE_CIRCLE.equals(((GraphNode) comp).getNodeType());
                    paintMultiPassShadow(g2d, comp.getX(), comp.getY(),
                        comp.getWidth(), comp.getHeight(), oval);
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private void paintMultiPassShadow(Graphics2D g2d, int x, int y, int w, int h, boolean oval) {
        for (int i = SHADOW_SIZE; i >= 0; i--) {
            float progress = (float) i / SHADOW_SIZE;
            float opacity = MAX_OPACITY * (1.0f - (progress * progress));
            g2d.setColor(new Color(0, 0, 0, opacity));
            int expand = i * 2;
            int sx = x - i;
            int sy = y - i + SHADOW_OFFSET_Y;
            int sw = w + expand;
            int sh = h + expand;
            if (oval) {
                g2d.fillOval(sx, sy, sw, sh);
            } else {
                g2d.fillRoundRect(sx, sy, sw, sh, CORNER_RADIUS + expand, CORNER_RADIUS + expand);
            }
        }
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }
}
