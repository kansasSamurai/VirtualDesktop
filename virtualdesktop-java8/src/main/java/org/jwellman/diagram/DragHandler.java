package org.jwellman.diagram;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jwellman.diagram.api.GraphNode;

/**
 * Drag handler that works with JLayeredPane and grid snapping (fixes jitter).
 *
 * When the pressed component is part of a multi-selection, all selected
 * components move together: start bounds for every selected component are
 * captured once at mousePressed, and the same press-to-current delta is
 * applied to each of them on every drag event.
 */
class DragHandler extends MouseAdapter {

    private Point pressPoint;
    private boolean dragging = false;
    private DiagramLayeredPane layeredPane;
    private Map<Component, Rectangle> groupStartBounds;

    public DragHandler(DiagramLayeredPane layeredPane) {
        this.layeredPane = layeredPane;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragging = true;
        pressPoint = e.getPoint();
        Component comp = e.getComponent();
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        layeredPane.suspendShadows();

        groupStartBounds = null;
        Set<Component> selection = layeredPane.getSelectedComponents();
        if (selection.size() > 1 && selection.contains(comp)) {
            groupStartBounds = new LinkedHashMap<>();
            for (Component c : selection) {
                groupStartBounds.put(c, c.getBounds());
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) {
            return;
        }

        Point currentPoint = e.getPoint();
        final int dx = currentPoint.x - pressPoint.x;
        final int dy = currentPoint.y - pressPoint.y;

        if (groupStartBounds != null) {
            for (Map.Entry<Component, Rectangle> entry : groupStartBounds.entrySet()) {
                Rectangle start = entry.getValue();
                moveTo(entry.getKey(), start.x + dx, start.y + dy, start.width, start.height);
            }
        } else {
            Component comp = e.getComponent();
            Rectangle currentBounds = comp.getBounds();
            moveTo(comp, currentBounds.x + dx, currentBounds.y + dy,
                currentBounds.width, currentBounds.height);
        }

        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private void moveTo(Component comp, int rawX, int rawY, int width, int height) {
        int newX = rawX;
        int newY = rawY;
        if (layeredPane.isSnapToGrid()) {
            newX = layeredPane.snapToGrid(newX);
            newY = layeredPane.snapToGrid(newY);
        }
        comp.setBounds(newX, newY, width, height);

        if (comp instanceof GraphNode) {
            GraphNode gn = (GraphNode) comp;
            gn.invalidatePortCache();
            layeredPane.notifyNodeMoved(gn.getNodeId());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        groupStartBounds = null;

        Component comp = e.getComponent();
        comp.setCursor(Cursor.getDefaultCursor());

        layeredPane.resumeShadows();
        layeredPane.repaint();
        layeredPane.notifyModified();
    }

}
