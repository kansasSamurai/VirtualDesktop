package org.jwellman.demo;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Makes components draggable within their parent container
 */
public class ComponentMover extends MouseAdapter {

    private Point pressPoint;
    private Component draggedComponent;
    private SerializableAbsoluteLayout layout;

    private int gridSize = 10;

    public ComponentMover(SerializableAbsoluteLayout layout) {
        this.layout = layout;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        pressPoint = e.getPoint();
        draggedComponent = e.getComponent();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (draggedComponent != null) {
            Point currentPoint = e.getPoint();
            int dx = currentPoint.x - pressPoint.x;
            int dy = currentPoint.y - pressPoint.y;

            boolean snaptogrid = true;
            Rectangle currentBounds = draggedComponent.getBounds();
            Rectangle newBounds = new Rectangle(
                (snaptogrid) ? this.snapToGrid(currentBounds.x + dx) : currentBounds.x + dx,
                (snaptogrid) ? this.snapToGrid(currentBounds.y + dy) : currentBounds.y + dy,
                currentBounds.width,
                currentBounds.height
            );

            draggedComponent.setBounds(newBounds);
            layout.updateComponentBounds(draggedComponent, newBounds);

        }
    }

    private int snapToGrid(int value) {
        int version = 1;
        switch (version) {
            case 1: return snapToGrid_old(value);
            case 2: return snapToGrid_distance(value);
        }

        return snapToGrid_old(value);
    }

    private int snapToGrid_distance(int value) {
        int snapped = Math.round((float) value / gridSize) * gridSize;

        // Only snap if within 3 pixels of grid line
        int distance = Math.abs(value - snapped);
        if (distance <= 3) {
            return snapped;
        } else {
            return value;
        }
    }

    private int snapToGrid_old(int value) {
        return Math.round((float) value / gridSize) * gridSize;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        draggedComponent = null;
    }

    public void makeComponentDraggable(Component component) {
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
    }

}