package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/**
 * Custom shape component for diagram
 */
public class DiagramShape extends JComponent implements DiagramColorable {

    private ShapeType type;
    private Color fillColor;
    private Color borderColor;

    private static final long serialVersionUID = 1L;

    public DiagramShape(ShapeType type) {
        this.type = type;
        this.fillColor = new Color(173, 216, 230, 200);
        this.borderColor = new Color(70, 130, 180);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Fill shape
        g2d.setColor(fillColor);
        switch (type) {
            case RECTANGLE:
                g2d.fillRoundRect(5, 5, width - 10, height - 10, 10, 10);
                break;
            case CIRCLE:
                g2d.fillOval(5, 5, width - 10, height - 10);
                break;
            case TRIANGLE:
                int[] xPoints = {width / 2, width - 5, 5};
                int[] yPoints = {5, height - 5, height - 5};
                g2d.fillPolygon(xPoints, yPoints, 3);
                break;
        }

        // Draw border
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2));
        switch (type) {
            case RECTANGLE:
                g2d.drawRoundRect(5, 5, width - 10, height - 10, 10, 10);
                break;
            case CIRCLE:
                g2d.drawOval(5, 5, width - 10, height - 10);
                break;
            case TRIANGLE:
                int[] xPoints = {width / 2, width - 5, 5};
                int[] yPoints = {5, height - 5, height - 5};
                g2d.drawPolygon(xPoints, yPoints, 3);
                break;
        }
    }

    public Enum<ShapeType> getShapeType() {
        return this.type;
    }

    @Override
    public Color getFillColor() {
        return this.fillColor;
    }

    @Override
    public void setFillColor(Color color) {
        this.fillColor = color;
        repaint();
    }

    @Override
    public Color getBorderColor() {
        return this.borderColor;
    }

    @Override
    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

}
