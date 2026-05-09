package org.jwellman.demo.d3ish;

import java.awt.Color;
import java.awt.Graphics2D;

public class Bar {
    private String label;
    private double value;
    private final Transition heightTransition = new Transition();

    // Modern "Foundation" Color Palette
    private final Color barColor = new Color(74, 144, 226);
    private final Color shadowColor = new Color(0, 0, 0, 40); // Soft transparency

    public Bar(String label, double initialValue) {
        this.label = label;
        this.value = initialValue;
        heightTransition.start(0, initialValue, 0);
    }

    public void animateTo(double newValue) {
        heightTransition.start(this.value, newValue, 800);
        this.value = newValue;
    }

    public void draw(Graphics2D g, int x, int y, int w, int h) {
        double currentVal = heightTransition.getValue();
        int barHeight = (int) (h * currentVal);
        int arc = 12; // Rounded corners

        // 1. Draw the Shadow first (Offset by 4px down and 2px right)
        // We lift the shadow slightly more as the bar gets taller
        int shadowOffset = 4;
        g.setColor(shadowColor);
        g.fillRoundRect(x + shadowOffset, y + (h - barHeight) + shadowOffset, w, barHeight, arc, arc);

        // 2. Draw the Main Bar
        g.setColor(barColor);
        g.fillRoundRect(x, y + (h - barHeight), w, barHeight, arc, arc);

        // 3. Subtle Highlight (Optional "Glass" effect)
        g.setColor(new Color(255, 255, 255, 30));
        g.fillRoundRect(x, y + (h - barHeight), w / 2, barHeight, arc, arc);

        // 4. Label
        g.setColor(new Color(60, 60, 60));
        g.drawString(label, x + (w / 4), y + h + 20);
    }

    public boolean isAnimating() {
        return heightTransition.isRunning();
    }
}
