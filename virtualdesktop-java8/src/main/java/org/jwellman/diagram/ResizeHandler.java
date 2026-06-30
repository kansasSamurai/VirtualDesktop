package org.jwellman.diagram;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

/**
 * Handles resizing of components with resize handles
 */
class ResizeHandler extends MouseAdapter {

    private Point pressPoint;
    private Rectangle startBounds;
    private JComponent component;
    private ResizeDirection resizeDirection;
    private DiagramLayeredPane layeredPane;

    private boolean isResizing = false;

    private enum ResizeDirection {
        NONE, NW, N, NE, E, SE, S, SW, W
    }

    public ResizeHandler(JComponent component, DiagramLayeredPane layeredPane) {
        this.component = component;
        this.layeredPane = layeredPane;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        pressPoint = e.getPoint();
        startBounds = component.getBounds();
        resizeDirection = getResizeDirection(e.getPoint());

        if (resizeDirection != ResizeDirection.NONE) {
            isResizing = true;
        }
    }

    private void turnOffResizing() {
        isResizing = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!isResizing) {
            return;
        }

        int dx = e.getX() - pressPoint.x;
        int dy = e.getY() - pressPoint.y;

        Rectangle newBounds = calculateNewBounds(resizeDirection, startBounds, dx, dy);
        // Apply snap to grid
        if (layeredPane.isSnapToGrid()) {
            newBounds.x = layeredPane.snapToGrid(newBounds.x);
            newBounds.y = layeredPane.snapToGrid(newBounds.y);
            newBounds.width = layeredPane.snapToGrid(newBounds.width);
            newBounds.height = layeredPane.snapToGrid(newBounds.height);
        }

        // Enforce minimum size
        newBounds.width = Math.max(newBounds.width, 30);
        newBounds.height = Math.max(newBounds.height, 30);

        component.setBounds(newBounds);

        layeredPane.repaint();
        e.consume();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isResizing) {
            layeredPane.notifyModified();
        }
        turnOffResizing();
        resizeDirection = ResizeDirection.NONE;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        ResizeDirection direction = getResizeDirection(e.getPoint());
        updateCursor(direction);
    }

    private ResizeDirection getResizeDirection(Point p) {
        int width = component.getWidth();
        int height = component.getHeight();
        int handleSize = 8;

        boolean onLeft = p.x <= handleSize;
        boolean onRight = p.x >= width - handleSize;
        boolean onTop = p.y <= handleSize;
        boolean onBottom = p.y >= height - handleSize;

        if (onTop && onLeft) { return ResizeDirection.NW; }
        if (onTop && onRight) { return ResizeDirection.NE; }
        if (onBottom && onLeft) { return ResizeDirection.SW; }
        if (onBottom && onRight) { return ResizeDirection.SE; }
        if (onTop) { return ResizeDirection.N; }
        if (onBottom) { return ResizeDirection.S; }
        if (onLeft) { return ResizeDirection.W; }
        if (onRight) { return ResizeDirection.E; }

        return ResizeDirection.NONE;
    }

    private void updateCursor(ResizeDirection direction) {
        Cursor cursor;

        switch (direction) {
            case NW: case SE: cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR); break;
            case NE: case SW: cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR); break;
            case N: case S:   cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR); break;
            case E: case W:   cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR); break;
            default:          cursor = Cursor.getDefaultCursor(); break;
        }

        component.setCursor(cursor);
    }

    private Rectangle calculateNewBounds(ResizeDirection direction, Rectangle start, int dx, int dy) {
        Rectangle bounds = new Rectangle(start);

        switch (direction) {
            case NW:
                bounds.x += dx;
                bounds.y += dy;
                bounds.width -= dx;
                bounds.height -= dy;
                break;
            case N:
                bounds.y += dy;
                bounds.height -= dy;
                break;
            case NE:
                bounds.y += dy;
                bounds.width += dx;
                bounds.height -= dy;
                break;
            case E:
                bounds.width += dx;
                break;
            case SE:
                bounds.width += dx;
                bounds.height += dy;
                break;
            case S:
                bounds.height += dy;
                break;
            case SW:
                bounds.x += dx;
                bounds.width -= dx;
                bounds.height += dy;
                break;
            case W:
                bounds.x += dx;
                bounds.width -= dx;
                break;
            default:
                break;
        }

        return bounds;
    }

}
