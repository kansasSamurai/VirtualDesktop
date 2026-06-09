package org.jwellman.demo.chess;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.border.EmptyBorder;

/**
 * A smart border that provides safe clipping gutters for dragging 
 * around the perimeter, while acting as a responsive canvas to render 
 * captured piece banks and status metrics outside the layout manager's grid.
 */
@SuppressWarnings("serial")
public class ChessGutterBorder extends EmptyBorder {

    private final Color gutterBgColor = new Color(45, 45, 45);
    private final Color slotBorderColor = new Color(140, 140, 140); // new Color(70, 70, 70);
    private final Color slotFillColor = new Color(55, 55, 55);

    /**
     * Define the structural margin parameters.
     * Top/Bottom are given extra breathing room (e.g., 50px) to act as banks,
     * while Left/Right keep a defensive padding (e.g., 20px) to prevent drag-clipping.
     */
    public ChessGutterBorder(int size) {
        super(size, size, size, size);
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        // No super call needed—EmptyBorder's paint method is literally blank!
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Render the clean background wash over the gutter perimeter surfaces
            g2.setColor(gutterBgColor);

            // Top Gutter Block
            g2.fillRect(x, y, width, top);
            // Bottom Gutter Block
            g2.fillRect(x, y + height - bottom, width, bottom);
            // Left Perimeter Shield
            g2.fillRect(x, y + top, left, height - top - bottom);
            // Right Perimeter Shield
            g2.fillRect(x + width - right, y + top, right, height - top - bottom);

            // 2. Project responsive placeholder slots for captured elements
            int slotSize = Math.min(top, bottom) - 16; // Auto-scale slots to match the inset pad
            int totalSlots = 3;
            int gap = 8;

            // Calculate the total horizontal width required to center the slot array
            int totalBlockWidth = (slotSize * totalSlots) + (gap * (totalSlots - 1));
            int startX = x + (width - totalBlockWidth) / 2;

            // Render the Top Bank (e.g., Black Captured Pieces)
            int topY = y + (top - slotSize) / 2;
            drawCapturedSlots(g2, startX, topY, slotSize, totalSlots, gap);

            // Render the Bottom Bank (e.g., White Captured Pieces)
            int bottomY = y + height - bottom + (bottom - slotSize) / 2;
            drawCapturedSlots(g2, startX, bottomY, slotSize, totalSlots, gap);

        } finally {
            g2.dispose();
        }
    }

    /**
     * Renders a perfectly spaced sequence of vector indicator slots.
     */
    private void drawCapturedSlots(Graphics2D g2, int startX, int targetY, int size, int count, int gap) {
        int currentX = startX;
        for (int i = 0; i < count; i++) {
            // Draw smooth, modern rounded geometric slots
            g2.setColor(slotFillColor);
            g2.fillRoundRect(currentX, targetY, size, size, 8, 8);

            g2.setColor(slotBorderColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(currentX, targetY, size, size, 8, 8);

            currentX += size + gap;
        }
    }

}
