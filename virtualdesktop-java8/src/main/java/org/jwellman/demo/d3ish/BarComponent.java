package org.jwellman.demo.d3ish;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

import javax.swing.JToggleButton;

@SuppressWarnings("serial")
public class BarComponent extends JToggleButton { 

    private double value; 
    private Color currentColor;
    private Color targetColor;
    private final Transition colorFade = new Transition();
    private final String label;

    public BarComponent(String label, double value) {
        this.label = label;
        this.value = value;

        // Tooltip is now "Free" and stays updated
        setToolTipText(label + ": " + (int) (value * 100) + "%");

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setText(label);

        this.currentColor = new Color(74, 144, 226);
        this.targetColor = currentColor;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        // We use inline CSS for the "White Box" and "Drop Shadow" feel
        // Note: Swing's HTML renderer is roughly equivalent to HTML 3.2 / CSS 1.0, 
        // so we use classic table/div styling for reliability.
        
        String hexColor = String.format("#%02x%02x%02x", 
            currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());

        return "<html>"
             + "<div style='background-color: white; color: #333333; padding: 8px; "
             + "border: 1px solid #cccccc; border-radius: 4px;'>"
             + "<b style='color: " + hexColor + "; font-size: 11pt;'>" + label + "</b><br/>"
             + "<hr style='border: 0; border-top: 1px solid #eeeeee; margin: 4px 0;'/>"
             + "<span style='font-family: sans-serif;'>Value: <b>" + (int)(value * 100) + "%</b></span>"
             + "</div>"
             + "</html>";
    }

    @Override
    public Point getToolTipLocation(MouseEvent event) {
        // We want the tooltip to appear centered over the TOP of the bar,
        // regardless of where the mouse actually clicked.
        int barTopY = (int) (getHeight() - ((getHeight() - 40) * value) - 60);

        // Center it horizontally over the bar
        return new Point(getWidth() / 4, barTopY);
    }

    public void animateToColor(Color newColor) {
        this.targetColor = newColor;
        colorFade.start(0, 1.0, 800); 
        repaint();
    }

    public boolean isAnimating() {
        return colorFade.isRunning();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // 1. Color Interpolation Logic
        if (colorFade.isRunning()) {
            this.currentColor = ColorInterpolator.interpolate(currentColor, targetColor, colorFade.getValue());
        } else {
            this.currentColor = targetColor;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // THE FIX: Define a "Gutter" of 5 pixels on each side
        int gutter = 5;
        int w = getWidth() - (gutter * 2); // Shrink the drawable width
        int h = getHeight() - 40;         // Leave room for label at bottom
        
        int barHeight = (int) (h * value);
        int x = gutter;                   // Start drawing 5px in
        int y = getHeight() - barHeight - 30;

        // 1. The "Selected" Glow (Now stays inside the clip)
        if (isSelected()) {
            // Subtle Glow: 3 pixels wider than the bar, but still within the gutter
            g2.setColor(new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), 80));
            g2.fillRoundRect(x - 3, y - 3, w + 6, barHeight + 6, 14, 14); 
            
            // High-contrast inner border
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(x, y, w, barHeight, 12, 12);
        }

        // 2. Draw the Bar
        g2.setColor(this.currentColor);
        g2.fillRoundRect(x, y, w, barHeight, 12, 12);

        // 3. Draw the Label (Centered in the component)
        g2.setColor(Color.DARK_GRAY);
        g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
        FontMetrics fm = g2.getFontMetrics();
        int labelX = (getWidth() / 2) - (fm.stringWidth(label) / 2);
        g2.drawString(label, labelX, getHeight() - 10);

        if (isSelected()) {
            // Graphics2D g2 = (Graphics2D) g.create();
            // g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. CALCULATE CENTER
            // Using the bar's current x, y, w, barHeight from your existing code
            int centerX = x + (w / 2);
            int centerY = y + (barHeight / 2);
            int circleRadius = Math.min(w, 24) / 2; // Cap the size so it fits skinny bars

            // 2. DRAW THE BACKGROUND CIRCLE (The "Cutout")
            // We use the parent's background color to make it look like a hole in the bar
            g2.setColor(getParent().getBackground());
            g2.fillOval(centerX - circleRadius, centerY - circleRadius, circleRadius * 2, circleRadius * 2);

            // 3. DRAW THE UNICODE CHECKMARK
            String checkMark = "\u2705"; // 2713, 2714, 2705, 2611 Heavy Check Mark
            // g2.setFont(new Font("SansSerif", Font.BOLD, (int) (circleRadius * 1.2)));
            g2.setFont(new Font("SegoeUI", Font.BOLD, (int) (circleRadius * 1.2)));
            g2.setColor(currentColor); // Use the bar's current (interpolated) color

            // Centering text in Java2D is a bit of a "Seasoned" task
            // FontMetrics fm = g2.getFontMetrics();
            int adjust = -2;
            int textX = centerX - (fm.stringWidth(checkMark) / 2) + adjust;
            // We use the Ascent to vertically center since Unicode glyphs sit on a baseline
            int textY = centerY + (fm.getAscent() / 2) - (fm.getDescent() / 2);

            g2.drawString(checkMark, textX, textY);

            g2.dispose();
        }

    }

    protected double getValue() {
        return value;
    }

    protected void setValue(double value) {
        this.value = value;
    }

}
