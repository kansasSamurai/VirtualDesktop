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

import javax.swing.SwingUtilities;

import org.jwellman.diagram.api.GraphNode;

/**
 * Drag handler that works with JLayeredPane and grid snapping (fixes jitter).
 *
 * The mouse delta is measured in {@code layeredPane}'s coordinate space (via
 * {@link SwingUtilities#convertPoint}) rather than the dragged component's own
 * local coordinates. The pane never moves during a drag, so this delta is a
 * stable, uncorrupted measure of total mouse movement since the press —
 * unlike a component-relative delta, which shifts every time the component
 * itself moves and cannot be safely combined with a fixed start-bounds
 * snapshot (that combination is what caused group drags to undertrack the
 * mouse and jitter). The same delta is applied to every selected component's
 * captured start bounds, so single- and multi-component drags share one path.
 */
class DragHandler extends MouseAdapter {

    private DiagramLayeredPane layeredPane;
    private Point pressPoint;
    private Map<Component, Rectangle> startBounds;
    private boolean dragging = false;

    public DragHandler(DiagramLayeredPane layeredPane) {
        this.layeredPane = layeredPane;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragging = true;
        Component comp = e.getComponent();
        pressPoint = SwingUtilities.convertPoint(comp, e.getPoint(), layeredPane);
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        layeredPane.suspendShadows();

        startBounds = new LinkedHashMap<>();
        Set<Component> selection = layeredPane.getSelectedComponents();
        if (selection.contains(comp)) {
            for (Component c : selection) {
                startBounds.put(c, c.getBounds());
            }
        } else {
            startBounds.put(comp, comp.getBounds());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) {
            return;
        }

        Point currentPoint = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), layeredPane);
        final int dx = currentPoint.x - pressPoint.x;
        final int dy = currentPoint.y - pressPoint.y;

        for (Map.Entry<Component, Rectangle> entry : startBounds.entrySet()) {
            Rectangle start = entry.getValue();
            moveTo(entry.getKey(), start.x + dx, start.y + dy, start.width, start.height);
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
        startBounds = null;

        Component comp = e.getComponent();
        comp.setCursor(Cursor.getDefaultCursor());

        layeredPane.resumeShadows();
        layeredPane.repaint();
        layeredPane.notifyModified();
    }

}
