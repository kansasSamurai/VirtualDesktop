package org.jwellman.demo.d3ish;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class BarComponentButton extends JButton {

    private double value; // 0.0 to 1.0
    private Color currentColor;
    private Color targetColor;
    private final Transition colorFade = new Transition();
    private final String label;

    public BarComponentButton(String label, double value) {
        this.label = label;
        this.value = value;
        // The "Headless" setup
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(true); // Keep this for accessibility

        this.currentColor = new Color(74, 144, 226); // Default
        this.targetColor = currentColor;
    }

    public void animateToColor(Color newColor) {
        this.targetColor = newColor;
        // Start an 800ms fade from wherever we are now to the new target
        colorFade.start(0, 1.0, 800); 

        // Safety check: ensure the component knows it needs to start redrawing
        repaint(); 
    }
    
    public boolean isAnimating() {
        return colorFade.isRunning();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Update the current color based on animation progress
        if (colorFade.isRunning()) {
            this.currentColor = ColorInterpolator.interpolate(currentColor, targetColor, colorFade.getValue());
        } else {
            this.currentColor = targetColor;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // State Machine logic (Free from JButton!)
        if (getModel().isPressed()) {
            /* Shift color slightly */ }
        if (isSelected()) {
            /* Draw highlight border */ }

        // D3-style Drawing logic
        int h = getHeight() - 30;
        int barHeight = (int) (h * value);
        g2.setColor(this.currentColor);
        g2.fillRoundRect(0, getHeight() - barHeight - 30, getWidth() - 10, barHeight, 12, 12);

        g2.dispose();
    }

}
