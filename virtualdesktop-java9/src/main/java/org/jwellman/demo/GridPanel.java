package org.jwellman.demo;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

/**
 * 
 * @author rwellman
 *
 */
public class GridPanel extends JPanel {

    private int gridSize = 10;

    private boolean showGrid = true;

    @SuppressWarnings("unused")
    private boolean snapToGrid = true;

    private static final long serialVersionUID = 1L;

    public GridPanel(SerializableAbsoluteLayout layout) {
        super(layout);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Draw grid
        if (showGrid) {
            drawGrid(g2d);
        }

        // Draw alignment guides
//        if (showAlignmentGuides && !activeGuides.isEmpty()) {
//            drawAlignmentGuides(g2d);
//        }
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(230, 230, 230));
        
        int width = getWidth();
        int height = getHeight();
        
        // Vertical lines
        for (int x = 0; x < width; x += gridSize) {
            g2d.drawLine(x, 0, x, height);
        }
        
        // Horizontal lines
        for (int y = 0; y < height; y += gridSize) {
            g2d.drawLine(0, y, width, y);
        }
    }

}
