package org.jwellman.diagram;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.jwellman.diagram.api.GraphNode;

/**
 * Drag handler that works with JLayeredPane and grid snapping (fixes jitter)
 */
class DragHandler extends MouseAdapter {

    private Point pressPoint;
    private boolean dragging = false;
    private DiagramLayeredPane layeredPane;

    public DragHandler(DiagramLayeredPane layeredPane) {
        this.layeredPane = layeredPane;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragging = true;
        pressPoint = e.getPoint();
        Component comp = e.getComponent();
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) {
            return;
        }

        Point currentPoint = e.getPoint();
        Component comp = e.getComponent();
        Rectangle currentBounds = comp.getBounds();

        // Calculate offset from press point
        final int dx = currentPoint.x - pressPoint.x;
        final int dy = currentPoint.y - pressPoint.y;

        int newX = currentBounds.x + dx;
        int newY = currentBounds.y + dy;
        if (layeredPane.isSnapToGrid()) {
            newX = layeredPane.snapToGrid(newX);
            newY = layeredPane.snapToGrid(newY);
        }

        Rectangle newBounds = new Rectangle(
            newX,
            newY,
            currentBounds.width,
            currentBounds.height
        );
        comp.setBounds(newBounds);

        if (comp instanceof GraphNode) {
            GraphNode gn = (GraphNode) comp;
            gn.invalidatePortCache();
            layeredPane.notifyNodeMoved(gn.getNodeId());
        }

        layeredPane.revalidate();
        layeredPane.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;

        Component comp = e.getComponent();
        comp.setCursor(Cursor.getDefaultCursor());

        layeredPane.repaint();
        layeredPane.notifyModified();
    }

}
