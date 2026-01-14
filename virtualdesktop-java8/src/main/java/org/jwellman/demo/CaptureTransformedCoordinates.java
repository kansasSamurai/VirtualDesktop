package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates how to capture transformed coordinates while drawing primitives
 * with different transforms, then later draw horizontal text at those positions.
 */
public class CaptureTransformedCoordinates extends JPanel {
    
    // List to store captured world coordinates for later text drawing
    private List<Point2D> capturedPoints = new ArrayList<>();
    private List<String> textLabels = new ArrayList<>();
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Clear previous captured points
        capturedPoints.clear();
        textLabels.clear();
        
        // Draw some primitives with different transforms, capturing coordinates
        drawPrimitivesWithTransforms(g2d);
        
        // Now draw horizontal text at the captured coordinates
        drawHorizontalTextAtCapturedPoints(g2d);
        
        g2d.dispose();
    }
    
    private void drawPrimitivesWithTransforms(Graphics2D g2d) {
        // Save the original transform
        AffineTransform originalTransform = g2d.getTransform();
        
        // Example 1: Draw rectangles in a circle pattern
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = 100;
        
        for (int i = 0; i < 8; i++) {
            // Calculate angle for this position
            double angle = i * Math.PI / 4; // 45 degrees each
            
            // Method 1: Capture coordinates using transform
            g2d.setTransform(originalTransform); // Reset to original
            g2d.translate(centerX, centerY);
            g2d.rotate(angle);
            g2d.translate(radius, 0);
            
            // Capture the current transformed position
            Point2D capturedPoint = captureCurrentPosition(g2d, 0, 0);
            capturedPoints.add(capturedPoint);
            textLabels.add("Rect " + (i + 1));
            
            // Draw the primitive (rectangle)
            g2d.setColor(Color.BLUE);
            g2d.fillRect(-10, -5, 20, 10);
            g2d.setColor(Color.BLACK);
            g2d.drawRect(-10, -5, 20, 10);
        }
        
        // Example 2: Draw circles with scaling and translation
        g2d.setTransform(originalTransform); // Reset
        
        for (int i = 0; i < 4; i++) {
            g2d.setTransform(originalTransform); // Reset to original
            
            // Apply transforms
            int x = 50 + i * 100;
            int y = 50;
            double scale = 1.0 + i * 0.3;
            
            g2d.translate(x, y);
            g2d.scale(scale, scale);
            
            // Capture center of circle
            Point2D circleCenter = captureCurrentPosition(g2d, 0, 0);
            capturedPoints.add(circleCenter);
            textLabels.add("Circle " + (i + 1));
            
            // Draw the primitive (circle)
            g2d.setColor(Color.RED);
            g2d.fillOval(-15, -15, 30, 30);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(-15, -15, 30, 30);
            
            // Also capture a point on the edge of the circle
            Point2D edgePoint = captureCurrentPosition(g2d, 20, 0);
            capturedPoints.add(edgePoint);
            textLabels.add("Edge " + (i + 1));
        }
        
        // Restore original transform
        g2d.setTransform(originalTransform);
    }
    
    /**
     * Method 1: Capture coordinates by transforming a point
     * This is the most straightforward approach
     */
    private Point2D captureCurrentPosition(Graphics2D g2d, double localX, double localY) {
        // Create a point in local coordinates
        Point2D localPoint = new Point2D.Double(localX, localY);
        
        // Transform it to world coordinates
        Point2D worldPoint = new Point2D.Double();
        g2d.getTransform().transform(localPoint, worldPoint);
        
        return worldPoint;
    }
    
    /**
     * Method 2: Capture coordinates using matrix math
     * Useful when you need more control or are doing complex calculations
     */
    private Point2D capturePositionWithTransform(AffineTransform transform, double localX, double localY) {
        Point2D localPoint = new Point2D.Double(localX, localY);
        Point2D worldPoint = new Point2D.Double();
        transform.transform(localPoint, worldPoint);
        return worldPoint;
    }
    
    /**
     * Method 3: Manual calculation approach
     * If you're tracking transforms manually
     */
    private Point2D manualTransformCalculation(double translateX, double translateY, 
                                             double rotation, double scaleX, double scaleY,
                                             double localX, double localY) {
        // Apply scaling
        double x = localX * scaleX;
        double y = localY * scaleY;
        
        // Apply rotation
        if (rotation != 0) {
            double cos = Math.cos(rotation);
            double sin = Math.sin(rotation);
            double rotatedX = x * cos - y * sin;
            double rotatedY = x * sin + y * cos;
            x = rotatedX;
            y = rotatedY;
        }
        
        // Apply translation
        x += translateX;
        y += translateY;
        
        return new Point2D.Double(x, y);
    }
    
    /**
     * Draw horizontal text at all captured points
     */
    private void drawHorizontalTextAtCapturedPoints(Graphics2D g2d) {
        // Save current transform
        AffineTransform currentTransform = g2d.getTransform();
        
        // Reset to identity transform to ensure horizontal text
        g2d.setTransform(new AffineTransform());
        
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        
        for (int i = 0; i < capturedPoints.size(); i++) {
            Point2D point = capturedPoints.get(i);
            String label = textLabels.get(i);
            
            // Draw horizontal text at the captured world coordinate
            drawCenteredString(g2d, label, (int) point.getX(), (int) point.getY());
            
            // Optional: Draw a small marker at the captured point
            g2d.setColor(Color.GREEN);
            g2d.fillOval((int) point.getX() - 2, (int) point.getY() - 2, 4, 4);
            g2d.setColor(Color.BLACK);
        }
        
        // Restore the transform
        g2d.setTransform(currentTransform);
    }
    
    /**
     * Utility method to draw centered horizontal text
     */
    private void drawCenteredString(Graphics2D g2d, String text, int x, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x - fm.stringWidth(text) / 2;
        int textY = y - fm.getHeight() / 2 + fm.getAscent();
        g2d.drawString(text, textX, textY);
    }
    
    /**
     * Alternative approach: Capture coordinates during a separate pass
     * Useful if you want to separate the coordinate capture from drawing
     */
    public List<Point2D> captureCoordinatesOnly(Graphics2D g2d) {
        List<Point2D> coordinates = new ArrayList<>();
        AffineTransform originalTransform = g2d.getTransform();
        
        // Simulate the same transform sequence without drawing
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = 100;
        
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            
            g2d.setTransform(originalTransform);
            g2d.translate(centerX, centerY);
            g2d.rotate(angle);
            g2d.translate(radius, 0);
            
            // Just capture, don't draw
            coordinates.add(captureCurrentPosition(g2d, 0, 0));
        }
        
        g2d.setTransform(originalTransform);
        return coordinates;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Capturing Transformed Coordinates");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new CaptureTransformedCoordinates());
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}