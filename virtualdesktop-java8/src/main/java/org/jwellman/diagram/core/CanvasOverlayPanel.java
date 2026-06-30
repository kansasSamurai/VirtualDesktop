package org.jwellman.diagram.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import javax.swing.JPanel;

import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.GraphEdge;
import org.jwellman.diagram.api.GraphNode;

/**
 * Transparent full-canvas panel at the OVERLAY_LAYER.
 *
 * Responsibilities:
 * 1. Paints selection handles for the currently selected component (any state).
 * 2. In EDGE_CREATION mode, renders port anchors on all nodes and lets the user
 *    drag from one port to another to create an edge.
 *
 * In IDLE mode with no selection this panel is fully transparent to both paint
 * and mouse events. In IDLE mode with a selected component it captures mouse
 * events only within the 8 resize-handle zones, so resize drag works without
 * ever calling setBorder() on the selected component.
 */
public class CanvasOverlayPanel extends JPanel {

    private enum State { IDLE, EDGE_CREATION, EDGE_DRAGGING }

    private enum ResizeDirection { NONE, NW, N, NE, E, SE, S, SW, W }

    private State state = State.IDLE;

    private final Map<String, GraphNode> nodeIndex;
    private final EdgeRenderPanel edgePanel;
    private final Consumer<GraphEdge> onEdgeCreated;
    private final UnaryOperator<Integer> snapFn;
    private final Runnable onResizeComplete;

    // Selection / resize state
    private Component selectedComponent;
    private ResizeDirection resizeDirection = ResizeDirection.NONE;
    private Point pressPoint;
    private Rectangle startBounds;
    private boolean isResizing = false;

    // Edge-drag state
    private GraphNode sourceNode;
    private String sourcePortId;
    private Point dragCurrent;

    private static final int PORT_HIT_RADIUS = 8;
    private static final int PORT_ANCHOR_RADIUS = 5;
    private static final int HANDLE_SIZE = 8;
    private static final int HANDLE_HIT_TOLERANCE = HANDLE_SIZE;
    private static final Color PORT_COLOR = new Color(60, 130, 220);
    private static final Color RUBBER_BAND_COLOR = new Color(60, 130, 220, 180);
    private static final Color HANDLE_FILL = Color.WHITE;
    private static final Color HANDLE_BORDER = Color.BLUE;
    private static final Color SELECTION_BORDER = Color.BLUE;

    private static final ResizeDirection[] HANDLE_DIRECTIONS = {
        ResizeDirection.NW, ResizeDirection.N, ResizeDirection.NE, ResizeDirection.E,
        ResizeDirection.SE, ResizeDirection.S, ResizeDirection.SW, ResizeDirection.W
    };

    private static final long serialVersionUID = 1L;

    /**
     * @param nodeIndex       live map of all graph nodes (used for port anchors)
     * @param edgePanel       edge render panel (notified when a node is resized)
     * @param onEdgeCreated   called when the user completes an edge drag
     * @param snapFn          applied to each coordinate during resize; returns the
     *                        value unchanged when snap-to-grid is disabled
     * @param onResizeComplete called once when a resize drag ends
     */
    public CanvasOverlayPanel(Map<String, GraphNode> nodeIndex,
                               EdgeRenderPanel edgePanel,
                               Consumer<GraphEdge> onEdgeCreated,
                               UnaryOperator<Integer> snapFn,
                               Runnable onResizeComplete) {
        this.nodeIndex = nodeIndex;
        this.edgePanel = edgePanel;
        this.onEdgeCreated = onEdgeCreated;
        this.snapFn = snapFn;
        this.onResizeComplete = onResizeComplete;

        setOpaque(false);
        setLayout(null);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { handlePressed(e); }
            @Override
            public void mouseDragged(MouseEvent e) { handleDragged(e); }
            @Override
            public void mouseReleased(MouseEvent e) { handleReleased(e); }
            @Override
            public void mouseMoved(MouseEvent e) { handleMoved(e); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    // ---------------------------------------------------------------
    // Mode control
    // ---------------------------------------------------------------

    public void enterEdgeCreationMode() {
        state = State.EDGE_CREATION;
        repaint();
    }

    public void exitEdgeCreationMode() {
        state = State.IDLE;
        clearDragState();
        repaint();
    }

    /** Sets the component whose selection handles should be painted. Pass null to clear. */
    public void setSelectedComponent(Component comp) {
        this.selectedComponent = comp;
        repaint();
    }

    // ---------------------------------------------------------------
    // Mouse handling
    // ---------------------------------------------------------------

    private void handlePressed(MouseEvent e) {
        if (state == State.IDLE) {
            if (selectedComponent != null) {
                resizeDirection = getHandleAt(e.getX(), e.getY());
                if (resizeDirection != ResizeDirection.NONE) {
                    pressPoint = e.getPoint();
                    startBounds = selectedComponent.getBounds();
                    isResizing = true;
                }
            }
            return;
        }
        // EDGE_CREATION mode — look for a nearby port
        PortHit hit = findNearestPort(e.getPoint());
        if (hit != null) {
            sourceNode = hit.node;
            sourcePortId = hit.portId;
            dragCurrent = e.getPoint();
            state = State.EDGE_DRAGGING;
            repaint();
        }
    }

    private void handleDragged(MouseEvent e) {
        if (isResizing && selectedComponent != null) {
            int dx = e.getX() - pressPoint.x;
            int dy = e.getY() - pressPoint.y;
            Rectangle newBounds = calculateNewBounds(resizeDirection, startBounds, dx, dy);
            newBounds.x = snapFn.apply(newBounds.x);
            newBounds.y = snapFn.apply(newBounds.y);
            newBounds.width = snapFn.apply(newBounds.width);
            newBounds.height = snapFn.apply(newBounds.height);
            newBounds.width = Math.max(newBounds.width, 30);
            newBounds.height = Math.max(newBounds.height, 30);
            selectedComponent.setBounds(newBounds);
            if (selectedComponent instanceof GraphNode) {
                GraphNode gn = (GraphNode) selectedComponent;
                gn.invalidatePortCache();
                edgePanel.nodeUpdated(gn.getNodeId());
            }
            repaint();
            e.consume();
            return;
        }
        if (state != State.EDGE_DRAGGING) {
            return;
        }
        dragCurrent = e.getPoint();
        repaint();
    }

    private void handleReleased(MouseEvent e) {
        if (isResizing) {
            isResizing = false;
            resizeDirection = ResizeDirection.NONE;
            if (onResizeComplete != null) {
                onResizeComplete.run();
            }
            repaint();
            return;
        }
        if (state != State.EDGE_DRAGGING) {
            return;
        }
        PortHit target = findNearestPort(e.getPoint());
        if (target != null && !isSamePort(target)) {
            commitEdge(target);
        }
        clearDragState();
        state = State.EDGE_CREATION;
        repaint();
    }

    private void handleMoved(MouseEvent e) {
        if (state == State.IDLE && selectedComponent != null) {
            ResizeDirection dir = getHandleAt(e.getX(), e.getY());
            setCursor(getCursorForDirection(dir));
        }
    }

    private boolean isSamePort(PortHit target) {
        return target.node == sourceNode && target.portId.equals(sourcePortId);
    }

    private void commitEdge(PortHit target) {
        String edgeId = "edge-" + System.currentTimeMillis();
        GraphEdge edge = new DefaultGraphEdge(
            edgeId,
            sourceNode.getNodeId(), sourcePortId,
            target.node.getNodeId(), target.portId,
            new EdgeAttributes()
        );
        onEdgeCreated.accept(edge);
    }

    private void clearDragState() {
        sourceNode = null;
        sourcePortId = null;
        dragCurrent = null;
    }

    // ---------------------------------------------------------------
    // Port hit-testing
    // ---------------------------------------------------------------

    private PortHit findNearestPort(Point p) {
        for (GraphNode node : nodeIndex.values()) {
            for (String portId : node.getPortIds()) {
                Point portLoc = node.getPortLocation(portId);
                if (portLoc != null && p.distance(portLoc) <= PORT_HIT_RADIUS) {
                    return new PortHit(node, portId);
                }
            }
        }
        return null;
    }

    private static final class PortHit {
        final GraphNode node;
        final String portId;

        PortHit(GraphNode node, String portId) {
            this.node = node;
            this.portId = portId;
        }
    }

    // ---------------------------------------------------------------
    // Handle geometry and hit-testing
    // ---------------------------------------------------------------

    /**
     * Returns the 8 handle rectangles for the given component bounds, in the
     * same order as HANDLE_DIRECTIONS: NW, N, NE, E, SE, S, SW, W.
     */
    private Rectangle[] getHandleRects(Rectangle b) {
        int h2 = HANDLE_SIZE / 2;
        return new Rectangle[] {
            new Rectangle(b.x - h2,              b.y - h2,               HANDLE_SIZE, HANDLE_SIZE), // NW
            new Rectangle(b.x + b.width/2 - h2,  b.y - h2,               HANDLE_SIZE, HANDLE_SIZE), // N
            new Rectangle(b.x + b.width - h2,    b.y - h2,               HANDLE_SIZE, HANDLE_SIZE), // NE
            new Rectangle(b.x + b.width - h2,    b.y + b.height/2 - h2,  HANDLE_SIZE, HANDLE_SIZE), // E
            new Rectangle(b.x + b.width - h2,    b.y + b.height - h2,    HANDLE_SIZE, HANDLE_SIZE), // SE
            new Rectangle(b.x + b.width/2 - h2,  b.y + b.height - h2,    HANDLE_SIZE, HANDLE_SIZE), // S
            new Rectangle(b.x - h2,              b.y + b.height - h2,    HANDLE_SIZE, HANDLE_SIZE), // SW
            new Rectangle(b.x - h2,              b.y + b.height/2 - h2,  HANDLE_SIZE, HANDLE_SIZE), // W
        };
    }

    private boolean isNearAnyHandle(int x, int y) {
        if (selectedComponent == null) {
            return false;
        }
        Rectangle[] handles = getHandleRects(selectedComponent.getBounds());
        int t = HANDLE_HIT_TOLERANCE / 2;
        for (Rectangle h : handles) {
            if (new Rectangle(h.x - t, h.y - t, h.width + t * 2, h.height + t * 2).contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private ResizeDirection getHandleAt(int x, int y) {
        if (selectedComponent == null) {
            return ResizeDirection.NONE;
        }
        Rectangle[] handles = getHandleRects(selectedComponent.getBounds());
        int t = HANDLE_HIT_TOLERANCE / 2;
        for (int i = 0; i < handles.length; i++) {
            Rectangle expanded = new Rectangle(
                handles[i].x - t, handles[i].y - t,
                handles[i].width + t * 2, handles[i].height + t * 2);
            if (expanded.contains(x, y)) {
                return HANDLE_DIRECTIONS[i];
            }
        }
        return ResizeDirection.NONE;
    }

    private Cursor getCursorForDirection(ResizeDirection dir) {
        switch (dir) {
            case NW: case SE: return Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
            case NE: case SW: return Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
            case N:  case S:  return Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
            case E:  case W:  return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            default:          return Cursor.getDefaultCursor();
        }
    }

    private Rectangle calculateNewBounds(ResizeDirection dir, Rectangle start, int dx, int dy) {
        Rectangle bounds = new Rectangle(start);
        switch (dir) {
            case NW: bounds.x += dx; bounds.y += dy; bounds.width -= dx; bounds.height -= dy; break;
            case N:  bounds.y += dy; bounds.height -= dy; break;
            case NE: bounds.y += dy; bounds.width += dx; bounds.height -= dy; break;
            case E:  bounds.width += dx; break;
            case SE: bounds.width += dx; bounds.height += dy; break;
            case S:  bounds.height += dy; break;
            case SW: bounds.x += dx; bounds.width -= dx; bounds.height += dy; break;
            case W:  bounds.x += dx; bounds.width -= dx; break;
            default: break;
        }
        return bounds;
    }

    // ---------------------------------------------------------------
    // Painting
    // ---------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        boolean hasSelection = (selectedComponent != null);
        boolean inActiveMode = (state != State.IDLE);

        if (!hasSelection && !inActiveMode) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (hasSelection) {
                paintSelectionHandles(g2d);
            }

            if (!inActiveMode) {
                return;
            }

            // Port anchors on all graph nodes
            for (GraphNode node : nodeIndex.values()) {
                for (String portId : node.getPortIds()) {
                    Point loc = node.getPortLocation(portId);
                    if (loc != null) {
                        g2d.setColor(Color.WHITE);
                        g2d.fillOval(loc.x - PORT_ANCHOR_RADIUS, loc.y - PORT_ANCHOR_RADIUS,
                            PORT_ANCHOR_RADIUS * 2, PORT_ANCHOR_RADIUS * 2);
                        g2d.setColor(PORT_COLOR);
                        g2d.setStroke(new BasicStroke(1.5f));
                        g2d.drawOval(loc.x - PORT_ANCHOR_RADIUS, loc.y - PORT_ANCHOR_RADIUS,
                            PORT_ANCHOR_RADIUS * 2, PORT_ANCHOR_RADIUS * 2);
                    }
                }
            }

            // Rubber-band line while dragging an edge
            if (state == State.EDGE_DRAGGING && sourceNode != null && dragCurrent != null) {
                Point sourceLoc = sourceNode.getPortLocation(sourcePortId);
                if (sourceLoc != null) {
                    g2d.setColor(RUBBER_BAND_COLOR);
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND, 10f, new float[]{5, 3}, 0f));
                    g2d.drawLine(sourceLoc.x, sourceLoc.y, dragCurrent.x, dragCurrent.y);
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    private void paintSelectionHandles(Graphics2D g2d) {
        Rectangle b = selectedComponent.getBounds();

        // Selection border
        g2d.setColor(SELECTION_BORDER);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRect(b.x + 1, b.y + 1, b.width - 2, b.height - 2);

        // 8 resize handles
        Rectangle[] handles = getHandleRects(b);
        for (Rectangle h : handles) {
            g2d.setColor(HANDLE_FILL);
            g2d.fillRect(h.x, h.y, h.width, h.height);
            g2d.setColor(HANDLE_BORDER);
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawRect(h.x, h.y, h.width, h.height);
        }
    }

    // ---------------------------------------------------------------
    // Event transparency
    // ---------------------------------------------------------------

    @Override
    public boolean contains(int x, int y) {
        if (state != State.IDLE) {
            return super.contains(x, y);
        }
        // In IDLE, only capture events near resize handles
        return isNearAnyHandle(x, y);
    }
}
