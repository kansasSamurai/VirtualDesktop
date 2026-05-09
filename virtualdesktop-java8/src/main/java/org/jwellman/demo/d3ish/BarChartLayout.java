package org.jwellman.demo.d3ish;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

public class BarChartLayout implements LayoutManager {

    private final int gap;

    public BarChartLayout(int gap) {
        this.gap = gap;
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int maxWidth = parent.getWidth() - (insets.left + insets.right);
        int maxHeight = parent.getHeight() - (insets.top + insets.bottom);
        
        Component[] components = parent.getComponents();
        if (components.length == 0) return;

        // Calculate width of each bar based on available space
        int barWidth = (maxWidth - (gap * (components.length - 1))) / components.length;
        
        int x = insets.left;
        for (Component comp : components) {
            // Every bar gets the full height of the container
            // The BarComponent itself handles the internal "value" height drawing
            comp.setBounds(x, insets.top, barWidth, maxHeight);
            x += barWidth + gap;
        }
    }

    // Required by interface, but can be simplified for a POC
    @Override public Dimension preferredLayoutSize(Container p) { return new Dimension(400, 300); }
    @Override public Dimension minimumLayoutSize(Container p) { return new Dimension(100, 100); }
    @Override public void addLayoutComponent(String name, Component comp) {}
    @Override public void removeLayoutComponent(Component comp) {}

}