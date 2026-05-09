package org.jwellman.demo.d3ish;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class BarChartPanelTwo extends JPanel {

    private Timer animator;
    private final List<BarComponent> bars = new ArrayList<>();

    public BarChartPanelTwo() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 60 FPS Animation Timer
        animator = new Timer(16, e -> {
            repaint(); // Global repaint of the panel and all child bars

            // Check if any BarComponent child is still fading colors
            boolean stillMoving = Arrays.stream(getComponents()).filter(c -> c instanceof BarComponent)
                    .map(c -> (BarComponent) c).anyMatch(BarComponent::isAnimating);

            if (!stillMoving) {
                animator.stop();
            }
        });
    }

    public BarComponent addBar(String label, double value) {
        BarComponent bar = new BarComponent(label, value);
        // Pull color from the registry's current iterator
        bar.setBackground(StyleRegistry.getActivePalette().next());
        bars.add(bar);
        add(bar);

        // Inside your BarChartPanel setup
        bar.addActionListener(e -> {
            // If you want "Radio" behavior (one at a time), unselect others.
            // If you want "Multi-select", just let the toggle do its thing!
            // System.out.println("Selected: " + bar.getText() + " Status: " + bar.isSelected());
            repaint();
        });

        return bar;
    }

    public void refreshStyles() {
        CircularArrayIterator<Color> palette = StyleRegistry.getActivePalette();
        for (BarComponent bar : bars) {
            bar.animateToColor(palette.next());
        }

        // Start the global animation timer to repaint frames
        // (This is the same javax.swing.Timer from our first POC)
        if (!animator.isRunning()) {
            animator.start();
        }
    }
    
    public void refreshStyles_original() {
        // Reset the registry's iterator to the start of the palette
        // so colors are assigned predictably
        CircularArrayIterator<Color> palette = StyleRegistry.getActivePalette();
        for (BarComponent bar : bars) {
            // In a more advanced version, this would trigger an animation 
            // from Color A to Color B
            bar.setBackground(palette.next());
        }
        repaint();
    }
}