package org.jwellman.demo;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A layout manager that allows drag-and-drop positioning
 * and saves/restores component positions
 */
public class SerializableAbsoluteLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Component, Rectangle> componentBounds = new HashMap<>();

    @Override
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof Rectangle) {
            componentBounds.put(comp, (Rectangle) constraints);
        }
    }
    
    @Override
    public void layoutContainer(Container parent) {
        for (Component comp : parent.getComponents()) {
            Rectangle bounds = componentBounds.get(comp);
            if (bounds != null) {
                comp.setBounds(bounds);
            }
        }
    }
    
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        // Calculate based on component positions
        int maxX = 0, maxY = 0;
        for (Rectangle bounds : componentBounds.values()) {
            maxX = Math.max(maxX, bounds.x + bounds.width);
            maxY = Math.max(maxY, bounds.y + bounds.height);
        }
        return new Dimension(maxX, maxY);
    }
    
    public void updateComponentBounds(Component comp, Rectangle bounds) {
        componentBounds.put(comp, bounds);
    }
    
    public void saveLayout(File file) throws IOException {
        // Save positions using preferences or serialization
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            Map<String, Rectangle> namedBounds = new HashMap<>();
            for (Map.Entry<Component, Rectangle> entry : componentBounds.entrySet()) {
                namedBounds.put(entry.getKey().getName(), entry.getValue());
            }
            oos.writeObject(namedBounds);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void loadLayout(File file, Container container) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, Rectangle> namedBounds = (Map<String, Rectangle>) ois.readObject();
            for (Component comp : container.getComponents()) {
                Rectangle bounds = namedBounds.get(comp.getName());
                if (bounds != null) {
                    componentBounds.put(comp, bounds);
                }
            }
        }
    }
    
    // Required interface methods
    @Override public void addLayoutComponent(String name, Component comp) {}
    @Override public void removeLayoutComponent(Component comp) { componentBounds.remove(comp); }
    @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(0, 0); }
    @Override public Dimension maximumLayoutSize(Container target) { return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); }
    @Override public float getLayoutAlignmentX(Container target) { return 0.5f; }
    @Override public float getLayoutAlignmentY(Container target) { return 0.5f; }
    @Override public void invalidateLayout(Container target) {}

}