package org.katacode.agents.auditor.ui;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import javax.swing.Icon;

/**
 * The toolbar's "A" mark, drawn from the design's 24-unit vector.
 */
class BrandIcon implements Icon {

    private static final int SIZE = 22;
    private static final double VIEW_BOX = 24.0;

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        g2.scale(SIZE / VIEW_BOX, SIZE / VIEW_BOX);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

        Path2D legs = new Path2D.Double();
        legs.moveTo(4, 20);
        legs.lineTo(12, 4);
        legs.lineTo(20, 20);
        g2.setColor(AuditorTheme.brandLight());
        g2.draw(legs);

        g2.setColor(AuditorTheme.accent());
        g2.draw(new Line2D.Double(8, 14, 16, 14));
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
