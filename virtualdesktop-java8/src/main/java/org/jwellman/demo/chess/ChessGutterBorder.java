package org.jwellman.demo.chess;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.border.EmptyBorder;

/**
 * A smart border that provides safe clipping gutters for dragging 
 * around the perimeter, while acting as a responsive canvas to render 
 * captured piece banks and status metrics outside the layout manager's grid.
 * captured piece bank slots, and dynamic player turn indicators in a single loop.
 */
@SuppressWarnings("serial")
public class ChessGutterBorder extends EmptyBorder {

    private final ChessGame game; // Authoritative source of truth for turn states
    private boolean flipped;

    private final Color gutterBgColor = new Color(45, 45, 45);
    private final Color slotBorderColor = new Color(140, 140, 140);
    private final Color slotFillColor = new Color(55, 55, 55);

    /**
     * Define the structural margin parameters.
     * Top/Bottom are given extra breathing room (e.g., 50px) to act as banks,
     * while Left/Right keep a defensive padding (e.g., 20px) to prevent drag-clipping.
     */
    public ChessGutterBorder(int size, ChessGame game) {
        super(size, size, size, size);
        this.game = game;
    }

    public void setFlipped(boolean flipped) {
        this.flipped = flipped;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
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

            // 2. High-Craft Turn Indicator Overlay (Merged)
            // Reads the domain flag to paint an explicit status indicator right onto the gutter canvas
            boolean whiteOnClock = game.isWhiteTurn();
            boolean indicatorAtBottom = flipped ? !whiteOnClock : whiteOnClock;
//            drawPlayerIndicator(x, y, width, height, g2, indicatorAtBottom);
            drawNewPlayerIndicator(x, y, width, height, g2, indicatorAtBottom);
            drawMaterialBadge(x, y, width, height, g2);
            
            
            
            
            // 3. Project responsive placeholder slots for captured elements
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

    private void drawNewPlayerIndicator(int x, int y, int width, int height, Graphics2D g2, boolean whiteOnClock) {
        // Fixed sizing for the indicator pill component
        int indicatorSize = 24; 
        Color turnIndicatorColor = new Color(59, 130, 246); // True Cobalt Blue Accent

        // Align it horizontally to the right edge of the checkerboard grid space
        // (Right at the boundary where the right-side padding begins, with a small 16px offset inward)
        int indicatorX = x + width - right - indicatorSize - 16;
        
        // Calculate the target vertical center based on who is playing
        int indicatorY;
        if (whiteOnClock) {
            // Centered perfectly within the bottom bank row
            indicatorY = y + height - bottom + (bottom - indicatorSize) / 2;
        } else {
            // Centered perfectly within the top bank row
            indicatorY = y + (top - indicatorSize) / 2;
        }

        // Draw the structural slot housing for the token
        g2.setColor(slotFillColor);
        g2.fillRoundRect(indicatorX, indicatorY, indicatorSize, indicatorSize, 8, 8);

        // Fill the slot with a solid cobalt core to show the active turn status
        g2.setColor(turnIndicatorColor);
        g2.fillRoundRect(indicatorX + 3, indicatorY + 3, indicatorSize - 6, indicatorSize - 6, 6, 6);

        // Finish with your premium crisp border ring
        g2.setColor(slotBorderColor);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(indicatorX, indicatorY, indicatorSize, indicatorSize, 8, 8);
    }

    private void drawMaterialBadge(int x, int y, int width, int height, Graphics2D g2) {
        int delta = game.getMaterialDelta();
        if (delta == 0) {
            return; // No badge at even material - chess.com convention
        }

        // Only the side WITH the advantage gets a badge, always shown as a positive number.
        boolean whiteAhead = delta > 0;
        boolean badgeAtBottom = flipped ? !whiteAhead : whiteAhead;
        String text = "+" + Math.abs(delta);

        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);

        int indicatorSize = 24; // matches drawNewPlayerIndicator's pill
        int badgeHeight = indicatorSize;
        int badgeWidth = textWidth + 14;

        int pillX = x + width - right - indicatorSize - 16;
        int badgeX = pillX - 8 - badgeWidth;

        int badgeY = badgeAtBottom
                ? y + height - bottom + (bottom - badgeHeight) / 2
                : y + (top - badgeHeight) / 2;

        g2.setColor(slotFillColor);
        g2.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 8, 8);

        g2.setColor(slotBorderColor);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 8, 8);

        g2.setColor(Color.WHITE);
        int textX = badgeX + (badgeWidth - textWidth) / 2;
        int textY = badgeY + ((badgeHeight - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(text, textX, textY);
    }

    private void drawPlayerIndicator(int x, int y, int width, int height, Graphics2D g2, boolean whiteOnClock) {
        int indicatorThickness = 4;

        if (whiteOnClock) {
            // Paint a crisp ice-white accent bar along the absolute bottom rim of the panel
            g2.setColor(new Color(240, 242, 245, 220)); 
            g2.fillRect(x, y + height - indicatorThickness, width, indicatorThickness);
            
            // Soft gradient wash rising up from the bottom rim into White's captured bank
            GradientPaint glow = new GradientPaint(
                0, y + height, new Color(255, 255, 255, 20),
                0, y + height - top, new Color(255, 255, 255, 0)
            );
            g2.setPaint(glow);
            g2.fillRect(x, y + height - bottom, width, bottom);
        } else {
            // Paint a rich cobalt blue accent bar along the absolute top rim of the panel for Black
            g2.setColor(new Color(59, 130, 246, 220)); 
            g2.fillRect(x, y, width, indicatorThickness);
            
            // Soft cobalt gradient wash bleeding down into Black's captured bank
            GradientPaint glow = new GradientPaint(
                0, y, new Color(59, 130, 246, 20),
                0, y + top, new Color(59, 130, 246, 0)
            );
            g2.setPaint(glow);
            g2.fillRect(x, y, width, top);
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
