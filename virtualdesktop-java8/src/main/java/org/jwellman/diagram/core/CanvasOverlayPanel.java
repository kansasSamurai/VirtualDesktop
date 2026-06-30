package org.jwellman.diagram.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JPanel;

import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.GraphEdge;
import org.jwellman.diagram.api.GraphNode;

/**
 * Transparent full-canvas panel at the OVERLAY_LAYER.
 *
 * In EDGE_CREATION mode it renders port anchors on all nodes and
 * lets the user drag from one port to another to create an edge.
 * In IDLE mode it is completely invisible to both paint and mouse events.
 */
public class CanvasOverlayPanel extends JPanel {

    private enum State { IDLE, EDGE_CREATION, EDGE_DRAGGING }

    private State state = State.IDLE;

    private final Map<String, GraphNode> nodeIndex;
    private final EdgeRenderPanel edgePanel;
    private final Consumer<GraphEdge> onEdgeCreated;

    // Edge-drag state
    private GraphNode sourceNode;
    private String sourcePortId;
    private Point dragCurrent;

    private static final int PORT_HIT_RADIUS = 8;
    private static final int PORT_ANCHOR_RADIUS = 5;
    private static final Color PORT_COLOR = new Color(60, 130, 220);
    private static final Color RUBBER_BAND_COLOR = new Color(60, 130, 220, 180);

    private static final long serialVersionUID = 1L;

    public CanvasOverlayPanel(Map<String, GraphNode> nodeIndex,
                               EdgeRenderPanel edgePanel,
                               Consumer<GraphEdge> onEdgeCreated) {
        this.nodeIndex = nodeIndex;
        this.edgePanel = edgePanel;
        this.onEdgeCreated = onEdgeCreated;

        setOpaque(false);
        setLayout(null);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { handlePressed(e); }
            @Override
            public void mouseDragged(MouseEvent e) { handleDragged(e); }
            @Override
            public void mouseReleased(MouseEvent e) { handleReleased(e); }
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

    // ---------------------------------------------------------------
    // Mouse handling
    // ---------------------------------------------------------------

    private void handlePressed(MouseEvent e) {
        if (state != State.EDGE_CREATION) {
            return;
        }
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
        if (state != State.EDGE_DRAGGING) {
            return;
        }
        dragCurrent = e.getPoint();
        repaint();
    }

    private void handleReleased(MouseEvent e) {
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
    // Painting
    // ---------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (state == State.IDLE) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw port anchors on every node
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

            // Draw rubber-band line while dragging
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

    // ---------------------------------------------------------------
    // Event transparency in IDLE state
    // ---------------------------------------------------------------

    @Override
    public boolean contains(int x, int y) {
        return state != State.IDLE && super.contains(x, y);
    }
}
