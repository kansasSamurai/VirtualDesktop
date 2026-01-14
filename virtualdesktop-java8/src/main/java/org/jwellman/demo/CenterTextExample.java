package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * Demonstrates different methods to center text horizontally and vertically
 * on a specific x,y coordinate using Java2D.
 */
public class CenterTextExample extends JPanel {
    private String text = "Centered Text";
    private Font font = new Font("Arial", Font.BOLD, 24);
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable antialiasing for better text quality
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);
        
        // Center points to demonstrate centering
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        // Draw crosshairs to show the center point
        drawCrosshairs(g2d, centerX, centerY);
        
        // Method 1: Using FontMetrics (recommended for simple text)
        g2d.setColor(Color.BLUE);
        drawCenteredTextWithFontMetrics(g2d, text, centerX, centerY - 80);
        g2d.drawString("Method 1: FontMetrics", 10, 30);
        
        // Method 2: Using Rectangle2D bounds (more precise)
        g2d.setColor(Color.RED);
        drawCenteredTextWithBounds(g2d, text, centerX, centerY);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Method 2: Rectangle2D Bounds", 10, 50);
        
        // Method 3: Using GlyphVector (most precise for complex text)
        g2d.setColor(Color.GREEN.darker());
        drawCenteredTextWithGlyphVector(g2d, text, centerX, centerY + 80);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Method 3: GlyphVector", 10, 70);
        
        g2d.dispose();
    }
    
    /**
     * Method 1: Center text using FontMetrics
     * Good for simple text, widely used approach
     */
    private void drawCenteredTextWithFontMetrics(Graphics2D g2d, String text, int x, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        
        // Calculate the width and height of the text
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        
        // Calculate position to center the text
        int textX = x - textWidth / 2;
        int textY = y - textHeight / 2 + fm.getAscent();
        
        g2d.drawString(text, textX, textY);
    }
    
    /**
     * Method 2: Center text using Rectangle2D bounds
     * More accurate than FontMetrics, especially for different fonts
     */
    private void drawCenteredTextWithBounds(Graphics2D g2d, String text, int x, int y) {
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = font.getStringBounds(text, frc);
        
        // Calculate position to center the text
        int textX = (int) (x - bounds.getWidth() / 2);
        int textY = (int) (y - bounds.getHeight() / 2 - bounds.getY());
        
        g2d.drawString(text, textX, textY);
        
        // Optional: Draw the bounds rectangle to visualize
        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                     10, new float[]{2, 2}, 0));
        g2d.drawRect((int) (x - bounds.getWidth() / 2), 
                    (int) (y - bounds.getHeight() / 2),
                    (int) bounds.getWidth(), 
                    (int) bounds.getHeight());
    }
    
    /**
     * Method 3: Center text using GlyphVector
     * Most precise method, especially for complex text rendering
     */
    private void drawCenteredTextWithGlyphVector(Graphics2D g2d, String text, int x, int y) {
        FontRenderContext frc = g2d.getFontRenderContext();
        GlyphVector gv = font.createGlyphVector(frc, text);
        Rectangle2D bounds = gv.getVisualBounds();
        
        // Calculate position to center the text
        int textX = (int) (x - bounds.getWidth() / 2 - bounds.getX());
        int textY = (int) (y - bounds.getHeight() / 2 - bounds.getY());
        
        g2d.drawGlyphVector(gv, textX, textY);
    }
    
    /**
     * Helper method to draw crosshairs showing the center point
     */
    private void drawCrosshairs(Graphics2D g2d, int x, int y) {
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setStroke(new BasicStroke(1));
        
        // Horizontal line
        g2d.drawLine(x - 20, y, x + 20, y);
        // Vertical line
        g2d.drawLine(x, y - 20, x, y + 20);
        
        // Center dot
        g2d.fillOval(x - 2, y - 2, 4, 4);
    }
    
    /**
     * Utility method that can be used anywhere to center text on coordinates
     */
    public static void drawCenteredString(Graphics2D g2d, String text, int x, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x - fm.stringWidth(text) / 2;
        int textY = y - fm.getHeight() / 2 + fm.getAscent();
        g2d.drawString(text, textX, textY);
    }
    
    /**
     * More precise utility method using Rectangle2D bounds
     */
    public static void drawCenteredStringPrecise(Graphics2D g2d, String text, int x, int y) {
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = g2d.getFont().getStringBounds(text, frc);
        int textX = (int) (x - bounds.getWidth() / 2);
        int textY = (int) (y - bounds.getHeight() / 2 - bounds.getY());
        g2d.drawString(text, textX, textY);
        new Font("Segoe UI", Font.PLAIN, 12);
    }
    
    // Demo application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Java2D Text Centering Methods");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new CenterTextExample());
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
