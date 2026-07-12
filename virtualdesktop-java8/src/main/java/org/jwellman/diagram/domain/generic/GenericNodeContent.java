package org.jwellman.diagram.domain.generic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Pure-Swing content for a generic graph node: a custom-painted rectangle or
 * ellipse — visually identical to the decorative {@code DiagramShape} rectangle
 * and circle — with a centered text label. No fields/methods; this is the
 * minimal connectable node for plain (domain-less) diagrams.
 */
public class GenericNodeContent extends JPanel {

    private static final long serialVersionUID = 1L;

    private final String nodeType; // "RECT_NODE" or "CIRCLE_NODE"
    private final Color fillColor;
    private final Color borderColor;

    public GenericNodeContent(String nodeType, String label, Color fillColor, Color borderColor,
                              Color textColor) {
        this.nodeType    = nodeType;
        this.fillColor   = fillColor;
        this.borderColor = borderColor;

        setOpaque(false);
        setLayout(new BorderLayout());

        JLabel textLabel = new JLabel(label, SwingConstants.CENTER);
        textLabel.setFont(textLabel.getFont().deriveFont(Font.PLAIN, 12f));
        textLabel.setForeground(textColor);
        add(textLabel, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width  = getWidth();
        int height = getHeight();
        boolean isCircle = GenericGraphFactory.NODE_TYPE_CIRCLE.equals(nodeType);

        // Extend to (nearly) the full node bounds — not DiagramShape's 5px inset —
        // so the visible shape's edge lines up with ShadowLayerPanel's shadow, which
        // is always sized from the node's full bounds. 1px is just enough margin to
        // keep the 2px border stroke from being clipped at the component edge.
        int margin = 1;
        int x = margin;
        int y = margin;
        int w = width - margin * 2;
        int h = height - margin * 2;

        g2d.setColor(fillColor);
        if (isCircle) {
            g2d.fillOval(x, y, w, h);
        } else {
            g2d.fillRoundRect(x, y, w, h, 10, 10);
        }

        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2));
        if (isCircle) {
            g2d.drawOval(x, y, w, h);
        } else {
            g2d.drawRoundRect(x, y, w, h, 10, 10);
        }
    }
}
