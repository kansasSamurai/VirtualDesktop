package org.jwellman.demo.layereddiagramtool;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;

/**
 * A color swatch component that displays a single color in a small square.
 * Provides visual hover feedback when the mouse enters the component.
 */
public class ColorSwatch extends JPanel {
    private Color color;
    private boolean hovered = false;
    private int swatchSize;

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_SWATCH_SIZE = 24;

    /**
     * Creates a color swatch with the default size (24x24 pixels)
     * @param color the color to display
     */
    public ColorSwatch(Color color) {
        this(color, DEFAULT_SWATCH_SIZE);
    }

    /**
     * Creates a color swatch with a custom size
     * @param color the color to display
     * @param swatchSize the size in pixels (width and height)
     */
    public ColorSwatch(Color color, int swatchSize) {
        this.color = color;
        this.swatchSize = swatchSize;
        setPreferredSize(new Dimension(swatchSize, swatchSize));
        setMinimumSize(new Dimension(swatchSize, swatchSize));
        setMaximumSize(new Dimension(swatchSize, swatchSize));

        // Add mouse listener for hover effect
        addMouseListener(new MouseAdapter() {
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

    /**
     * Gets the color displayed by this swatch
     * @return the current color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the color displayed by this swatch
     * @param color the new color
     */
    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Fill with color
        g.setColor(color);
        g.fillRect(2, 2, swatchSize - 4, swatchSize - 4);

        // Draw border
        if (hovered) {
            g.setColor(Color.BLUE);
            g.drawRect(1, 1, swatchSize - 3, swatchSize - 3);
            g.drawRect(0, 0, swatchSize - 1, swatchSize - 1);
        } else {
            g.setColor(Color.GRAY);
            g.drawRect(1, 1, swatchSize - 3, swatchSize - 3);
        }
    }
}
