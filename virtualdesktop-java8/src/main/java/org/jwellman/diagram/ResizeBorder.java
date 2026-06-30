package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;

/**
 * Custom border that shows resize handles
 */
public class ResizeBorder extends AbstractBorder {

    @SuppressWarnings("unused")
    private JComponent component;

    private static final int HANDLE_SIZE = 8;

    private static final Insets INSETS = new Insets(3, 3, 3, 3);

    private static final long serialVersionUID = 1L;

    public ResizeBorder(JComponent component) {
        this.component = component;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;

        // Draw border
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x + 1, y + 1, width - 2, height - 2);

        // Draw resize handles
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // NW
        g2d.fillRect(x + width - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // NE
        g2d.fillRect(x - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // SW
        g2d.fillRect(x + width - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // SE
        g2d.fillRect(x + width/2 - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // N
        g2d.fillRect(x + width/2 - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // S
        g2d.fillRect(x - HANDLE_SIZE/2, y + height/2 - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // W
        g2d.fillRect(x + width - HANDLE_SIZE/2, y + height/2 - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // E

        // Draw handle borders
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(x - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width/2 - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width/2 - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x - HANDLE_SIZE/2, y + height/2 - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width - HANDLE_SIZE/2, y + height/2 - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return INSETS;
    }
}
