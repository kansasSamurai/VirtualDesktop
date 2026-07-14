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
import java.util.function.Supplier;
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
    private final Runnable onDirectEditComplete;
    private final Supplier<EdgeAttributes> edgeAttrsSupplier;

    // Selection / resize state — a single selected component gets resize handles;
    // a multi-selection gets a plain highlight outline per component and no resize.
    private java.util.List<Component> selection = java.util.Collections.emptyList();
    private Component resizeTarget;
    private ResizeDirection resizeDirection = ResizeDirection.NONE;
    private Point pressPoint;
    private Rectangle startBounds;
    private boolean isResizing = false;

    // Rubber-band selection rectangle, in canvas coordinates; null when not marqueeing
    private Rectangle marqueeRect;

    // Edge-drag state (new-edge creation, via the Connect toggle)
    private GraphNode sourceNode;
    private String sourcePortId;
    private Point dragCurrent;

    // Selected-edge endpoint-drag state — an independent track, mirroring how
    // isResizing sits alongside (not inside) the State enum: this is driven by
    // edge *selection*, not by a toolbar mode, so it must not interfere with
    // IDLE/EDGE_CREATION/EDGE_DRAGGING semantics used for new-edge creation.
    private GraphEdge selectedEdge;
    private boolean isDraggingEndpoint;
    private boolean draggingSourceEnd;
    private Point endpointDragCurrent;

    private enum EndpointEnd { SOURCE, TARGET }

    private static final int PORT_HIT_RADIUS = 8;
    private static final int PORT_ANCHOR_RADIUS = 5;
    private static final int HANDLE_SIZE = 8;
    private static final int HANDLE_HIT_TOLERANCE = HANDLE_SIZE;
    private static final int ENDPOINT_HANDLE_RADIUS = 6;
    private static final int ENDPOINT_HANDLE_HIT_RADIUS = 8;
    private static final Color PORT_COLOR = new Color(60, 130, 220);
    private static final Color RUBBER_BAND_COLOR = new Color(60, 130, 220, 180);
    private static final Color HANDLE_FILL = Color.WHITE;
    private static final Color HANDLE_BORDER = Color.BLUE;
    private static final Color SELECTION_BORDER = Color.BLUE;
    private static final Color MARQUEE_FILL = new Color(60, 130, 220, 40);
    private static final Color MARQUEE_BORDER = new Color(60, 130, 220, 200);
    private static final Color ENDPOINT_HANDLE_FILL = new Color(30, 90, 200);

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
     * @param onDirectEditComplete called once when a resize drag or an edge
     *                        endpoint drag ends — any interactive geometry edit
     *                        made directly on this overlay, not just resize
     */
    public CanvasOverlayPanel(Map<String, GraphNode> nodeIndex,
                               EdgeRenderPanel edgePanel,
                               Consumer<GraphEdge> onEdgeCreated,
                               UnaryOperator<Integer> snapFn,
                               Runnable onDirectEditComplete,
                               Supplier<EdgeAttributes> edgeAttrsSupplier) {
        this.nodeIndex = nodeIndex;
        this.edgePanel = edgePanel;
        this.onEdgeCreated = onEdgeCreated;
        this.snapFn = snapFn;
        this.onDirectEditComplete = onDirectEditComplete;
        this.edgeAttrsSupplier = edgeAttrsSupplier;

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
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    public void exitEdgeCreationMode() {
        state = State.IDLE;
        setCursor(Cursor.getDefaultCursor());
        clearDragState();
        repaint();
    }

    /**
     * Sets the components whose selection outline should be painted. A single
     * component gets full resize handles; two or more get a plain highlight
     * outline each with no resize affordance. Pass an empty collection to clear.
     */
    public void setSelection(java.util.Collection<Component> comps) {
        this.selection = new java.util.ArrayList<>(comps);
        repaint();
    }

    /** Sets the rubber-band selection rectangle, in canvas coordinates. Pass null to clear. */
    public void setMarqueeRect(Rectangle rect) {
        this.marqueeRect = rect;
        repaint();
    }

    /**
     * Sets the edge whose endpoint grip handles should be painted (and made
     * draggable). Pass null to clear — mirrors setSelection()'s role for
     * component selection, but edge and component selection are mutually
     * exclusive by construction in DiagramLayeredPane, so at most one of
     * {@code selection} / {@code selectedEdge} is ever non-empty.
     */
    public void setSelectedEdge(GraphEdge edge) {
        this.selectedEdge = edge;
        repaint();
    }

    /**
     * Resolves an edge endpoint's current pixel location, or null if the node
     * is missing (e.g. deleted while this edge remained selected — node
     * deletion doesn't prune edges that reference it, so this is a real,
     * already-possible state, not just defensive boilerplate) or not yet laid
     * out.
     */
    private Point endpointLocation(String nodeId, String portId) {
        GraphNode node = nodeIndex.get(nodeId);
        return (node != null) ? node.getPortLocation(portId) : null;
    }

    /** Returns the sole selected component when exactly one is selected, else null. */
    private Component resizableComponent() {
        return (selection.size() == 1) ? selection.get(0) : null;
    }

    // ---------------------------------------------------------------
    // Mouse handling
    // ---------------------------------------------------------------

    private void handlePressed(MouseEvent e) {
        if (state == State.IDLE) {
            Component target = resizableComponent();
            if (target != null) {
                resizeDirection = getHandleAt(e.getX(), e.getY());
                if (resizeDirection != ResizeDirection.NONE) {
                    pressPoint = e.getPoint();
                    startBounds = target.getBounds();
                    resizeTarget = target;
                    isResizing = true;
                    return;
                }
            }
            EndpointEnd hitEnd = getEndpointHandleAt(e.getX(), e.getY());
            if (hitEnd != null) {
                isDraggingEndpoint = true;
                draggingSourceEnd = (hitEnd == EndpointEnd.SOURCE);
                endpointDragCurrent = e.getPoint();
                repaint();
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
        if (isResizing && resizeTarget != null) {
            int dx = e.getX() - pressPoint.x;
            int dy = e.getY() - pressPoint.y;
            Rectangle newBounds = calculateNewBounds(resizeDirection, startBounds, dx, dy);
            newBounds.x = snapFn.apply(newBounds.x);
            newBounds.y = snapFn.apply(newBounds.y);
            newBounds.width = snapFn.apply(newBounds.width);
            newBounds.height = snapFn.apply(newBounds.height);
            newBounds.width = Math.max(newBounds.width, 30);
            newBounds.height = Math.max(newBounds.height, 30);
            resizeTarget.setBounds(newBounds);
            // revalidate() re-runs the layout manager (e.g. BorderLayout in NodeHostPanel)
            // so inner content fills the new size immediately during drag
            resizeTarget.revalidate();
            if (resizeTarget instanceof GraphNode) {
                GraphNode gn = (GraphNode) resizeTarget;
                gn.invalidatePortCache();
                edgePanel.nodeUpdated(gn.getNodeId());
            }
            // repaint the parent (JLayeredPane) so the component visually updates;
            // this also repaints the overlay (a sibling child) via the normal Swing pass
            if (resizeTarget.getParent() != null) {
                resizeTarget.getParent().repaint();
            }
            e.consume();
            return;
        }
        if (isDraggingEndpoint) {
            endpointDragCurrent = e.getPoint();
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
            resizeTarget = null;
            if (onDirectEditComplete != null) {
                onDirectEditComplete.run();
            }
            repaint();
            return;
        }
        if (isDraggingEndpoint) {
            PortHit target = findNearestPort(e.getPoint());
            if (target != null && !isSameAsFixedEnd(target)) {
                if (draggingSourceEnd) {
                    selectedEdge.setSourceEndpoint(target.node.getNodeId(), target.portId);
                } else {
                    selectedEdge.setTargetEndpoint(target.node.getNodeId(), target.portId);
                }
                edgePanel.repaint();
                if (onDirectEditComplete != null) {
                    onDirectEditComplete.run();
                }
            }
            isDraggingEndpoint = false;
            endpointDragCurrent = null;
            repaint();
            e.consume();
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
        if (state == State.IDLE && resizableComponent() != null) {
            ResizeDirection dir = getHandleAt(e.getX(), e.getY());
            setCursor(getCursorForDirection(dir));
        } else if (state == State.IDLE && selectedEdge != null) {
            setCursor(getEndpointHandleAt(e.getX(), e.getY()) != null
                ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                : Cursor.getDefaultCursor());
        } else if (state == State.EDGE_CREATION) {
            PortHit hit = findNearestPort(e.getPoint());
            if (hit != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private boolean isSamePort(PortHit target) {
        return target.node == sourceNode && target.portId.equals(sourcePortId);
    }

    /**
     * True if {@code target} is identical (same node and port) to the
     * currently-dragged edge's OTHER, fixed end — dropping there would
     * collapse the edge to a zero-length self-connection. Same-node,
     * different-port self-loops remain allowed, matching isSamePort()'s
     * existing semantics for brand-new edge creation.
     */
    private boolean isSameAsFixedEnd(PortHit target) {
        String fixedNodeId = draggingSourceEnd ? selectedEdge.getTargetNodeId() : selectedEdge.getSourceNodeId();
        String fixedPortId = draggingSourceEnd ? selectedEdge.getTargetPortId() : selectedEdge.getSourcePortId();
        return target.node.getNodeId().equals(fixedNodeId) && target.portId.equals(fixedPortId);
    }

    private void commitEdge(PortHit target) {
        String edgeId = "edge-" + System.currentTimeMillis();
        EdgeAttributes attrs = (edgeAttrsSupplier != null)
            ? new EdgeAttributes(edgeAttrsSupplier.get())
            : new EdgeAttributes();
        GraphEdge edge = new DefaultGraphEdge(
            edgeId,
            sourceNode.getNodeId(), sourcePortId,
            target.node.getNodeId(), target.portId,
            attrs
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
        Component target = resizableComponent();
        if (target == null) {
            return false;
        }
        Rectangle[] handles = getHandleRects(target.getBounds());
        int t = HANDLE_HIT_TOLERANCE / 2;
        for (Rectangle h : handles) {
            if (new Rectangle(h.x - t, h.y - t, h.width + t * 2, h.height + t * 2).contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private ResizeDirection getHandleAt(int x, int y) {
        Component target = resizableComponent();
        if (target == null) {
            return ResizeDirection.NONE;
        }
        Rectangle[] handles = getHandleRects(target.getBounds());
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

    /** True if (x,y) is within grab range of either of the selected edge's endpoint handles. */
    private boolean isNearEndpointHandle(int x, int y) {
        return getEndpointHandleAt(x, y) != null;
    }

    /** Returns which end of the selected edge (x,y) is near, or null if neither/no edge selected. */
    private EndpointEnd getEndpointHandleAt(int x, int y) {
        if (selectedEdge == null) {
            return null;
        }
        Point p = new Point(x, y);
        Point sourceLoc = endpointLocation(selectedEdge.getSourceNodeId(), selectedEdge.getSourcePortId());
        if (sourceLoc != null && p.distance(sourceLoc) <= ENDPOINT_HANDLE_HIT_RADIUS) {
            return EndpointEnd.SOURCE;
        }
        Point targetLoc = endpointLocation(selectedEdge.getTargetNodeId(), selectedEdge.getTargetPortId());
        if (targetLoc != null && p.distance(targetLoc) <= ENDPOINT_HANDLE_HIT_RADIUS) {
            return EndpointEnd.TARGET;
        }
        return null;
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

        Component singleSelection = resizableComponent();
        boolean hasSingleSelection = (singleSelection != null);
        boolean hasMultiSelection = (selection.size() > 1);
        boolean inActiveMode = (state != State.IDLE);
        boolean hasMarquee = (marqueeRect != null);
        // IDLE-only: see contains()'s comment on why endpoint handles don't show
        // (or hit-test) while Connect mode happens to be on with an edge selected.
        boolean hasSelectedEdge = (selectedEdge != null && state == State.IDLE);

        if (!hasSingleSelection && !hasMultiSelection && !inActiveMode && !hasMarquee && !hasSelectedEdge) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (hasSingleSelection) {
                paintSelectionHandles(g2d, singleSelection);
            } else if (hasMultiSelection) {
                paintMultiSelectionHighlights(g2d);
            }

            if (hasMarquee) {
                paintMarquee(g2d);
            }

            if (hasSelectedEdge) {
                paintEndpointHandles(g2d);
            }

            if (!inActiveMode && !isDraggingEndpoint) {
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

            // Rubber-band line while dragging a brand-new edge (Connect toggle)
            if (state == State.EDGE_DRAGGING && sourceNode != null && dragCurrent != null) {
                Point sourceLoc = sourceNode.getPortLocation(sourcePortId);
                if (sourceLoc != null) {
                    g2d.setColor(RUBBER_BAND_COLOR);
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND, 10f, new float[]{5, 3}, 0f));
                    g2d.drawLine(sourceLoc.x, sourceLoc.y, dragCurrent.x, dragCurrent.y);
                }
            }

            // Rubber-band line while dragging a selected edge's endpoint to a new port
            if (isDraggingEndpoint && endpointDragCurrent != null && selectedEdge != null) {
                Point fixedLoc = draggingSourceEnd
                    ? endpointLocation(selectedEdge.getTargetNodeId(), selectedEdge.getTargetPortId())
                    : endpointLocation(selectedEdge.getSourceNodeId(), selectedEdge.getSourcePortId());
                if (fixedLoc != null) {
                    g2d.setColor(RUBBER_BAND_COLOR);
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND, 10f, new float[]{5, 3}, 0f));
                    g2d.drawLine(fixedLoc.x, fixedLoc.y, endpointDragCurrent.x, endpointDragCurrent.y);
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    /** Paints a small filled grip circle at each of the selected edge's current endpoints. */
    private void paintEndpointHandles(Graphics2D g2d) {
        if (isDraggingEndpoint) {
            // The dragged end already has a rubber-band tip following the cursor;
            // painting a static grip there too would just be visual clutter.
            paintEndpointHandle(g2d, draggingSourceEnd
                ? endpointLocation(selectedEdge.getTargetNodeId(), selectedEdge.getTargetPortId())
                : endpointLocation(selectedEdge.getSourceNodeId(), selectedEdge.getSourcePortId()));
            return;
        }
        paintEndpointHandle(g2d, endpointLocation(selectedEdge.getSourceNodeId(), selectedEdge.getSourcePortId()));
        paintEndpointHandle(g2d, endpointLocation(selectedEdge.getTargetNodeId(), selectedEdge.getTargetPortId()));
    }

    private void paintEndpointHandle(Graphics2D g2d, Point loc) {
        if (loc == null) {
            return;
        }
        g2d.setColor(ENDPOINT_HANDLE_FILL);
        g2d.fillOval(loc.x - ENDPOINT_HANDLE_RADIUS, loc.y - ENDPOINT_HANDLE_RADIUS,
            ENDPOINT_HANDLE_RADIUS * 2, ENDPOINT_HANDLE_RADIUS * 2);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(loc.x - ENDPOINT_HANDLE_RADIUS, loc.y - ENDPOINT_HANDLE_RADIUS,
            ENDPOINT_HANDLE_RADIUS * 2, ENDPOINT_HANDLE_RADIUS * 2);
    }

    private void paintSelectionHandles(Graphics2D g2d, Component comp) {
        Rectangle b = comp.getBounds();

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

    /** Multi-selection gets a plain outline per component — no resize handles. */
    private void paintMultiSelectionHighlights(Graphics2D g2d) {
        g2d.setColor(SELECTION_BORDER);
        g2d.setStroke(new BasicStroke(2f));
        for (Component comp : selection) {
            Rectangle b = comp.getBounds();
            g2d.drawRect(b.x + 1, b.y + 1, b.width - 2, b.height - 2);
        }
    }

    private void paintMarquee(Graphics2D g2d) {
        g2d.setColor(MARQUEE_FILL);
        g2d.fillRect(marqueeRect.x, marqueeRect.y, marqueeRect.width, marqueeRect.height);
        g2d.setColor(MARQUEE_BORDER);
        g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_ROUND, 10f, new float[]{4, 3}, 0f));
        g2d.drawRect(marqueeRect.x, marqueeRect.y, marqueeRect.width, marqueeRect.height);
    }

    // ---------------------------------------------------------------
    // Event transparency
    // ---------------------------------------------------------------

    @Override
    public boolean contains(int x, int y) {
        if (state != State.IDLE) {
            return super.contains(x, y);
        }
        // In IDLE, only capture events near resize handles or a selected edge's
        // endpoint handles. (Endpoint handles are IDLE-only on purpose: toggling
        // Connect mode never clears selectedEdge, so gating on IDLE keeps a click
        // near a grip handle from being swallowed by new-edge-creation hit-testing
        // instead while Connect mode happens to be on.)
        return isNearAnyHandle(x, y) || isNearEndpointHandle(x, y);
    }
}
