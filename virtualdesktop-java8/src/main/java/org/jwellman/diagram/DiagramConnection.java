package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/**
 * Example of adding connections between shapes
 */
class DiagramConnection extends JComponent {

    private Point startPoint;
    private Point endPoint;

    private static final long serialVersionUID = 1L;

    public DiagramConnection(Point start, Point end) {
        this.startPoint = start;
        this.endPoint = end;
        setOpaque(false);

        // Calculate bounds to encompass both points
        int minX = Math.min(start.x, end.x);
        int minY = Math.min(start.y, end.y);
        int maxX = Math.max(start.x, end.x);
        int maxY = Math.max(start.y, end.y);

        setBounds(minX - 10, minY - 10, maxX - minX + 20, maxY - minY + 20);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));

        // Convert to component-relative coordinates
        int x1 = startPoint.x - getX();
        int y1 = startPoint.y - getY();
        int x2 = endPoint.x - getX();
        int y2 = endPoint.y - getY();

        g2d.drawLine(x1, y1, x2, y2);

        // Draw arrow at end
        drawArrow(g2d, x1, y1, x2, y2);
    }

    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        int arrowSize = 10;
        double angle = Math.atan2(y2 - y1, x2 - x1);

        int ax1 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));

        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(x2, y2);
        arrowHead.addPoint(ax1, ay1);
        arrowHead.addPoint(ax2, ay2);

        g2d.fill(arrowHead);
    }
}
