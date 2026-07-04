package org.jwellman.diagram.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.jwellman.diagram.api.CanvasTheme;
import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.EdgeRouter;
import org.jwellman.diagram.api.GraphEdge;
import org.jwellman.diagram.api.GraphNode;

/**
 * Transparent full-canvas panel that renders all graph edges.
 * Sits at the CONNECTION_LAYER so edges appear above shapes but below the overlay.
 * Always passes mouse events through to layers below.
 *
 * Path caching: computed paths are stored by edge ID after each paint so that
 * hit testing (findEdgeAt) uses the same geometry as rendering without recomputing.
 */
public class EdgeRenderPanel extends JPanel {

    private final List<GraphEdge> edges = new ArrayList<>();
    private final Map<String, GraphNode> nodeIndex;
    private final EdgeRouter router;
    private final CanvasTheme theme;

    // Populated during paintComponent, consumed by findEdgeAt.
    private final Map<String, Path2D> cachedPaths = new HashMap<>();

    private GraphEdge selectedEdge;

    private static final int   ARROW_SIZE      = 10;
    private static final float HIT_STROKE_WIDTH = 8f;  // 4px tolerance on each side
    private static final Color SELECTION_COLOR  = new Color(0, 160, 255, 130);

    private static final long serialVersionUID = 1L;

    public EdgeRenderPanel(EdgeRouter router, Map<String, GraphNode> nodeIndex, CanvasTheme theme) {
        this.router    = router;
        this.nodeIndex = nodeIndex;
        this.theme     = theme;
        setOpaque(false);
        setLayout(null);
    }

    // ---------------------------------------------------------------
    // Edge management
    // ---------------------------------------------------------------

    public void addEdge(GraphEdge edge) {
        edges.add(edge);
        repaint();
    }

    public void removeEdge(String edgeId) {
        edges.removeIf(e -> e.getEdgeId().equals(edgeId));
        cachedPaths.remove(edgeId);
        if (selectedEdge != null && selectedEdge.getEdgeId().equals(edgeId)) {
            selectedEdge = null;
        }
        repaint();
    }

    /** Called by DiagramLayeredPane after a node's bounds change. */
    public void nodeUpdated(String nodeId) {
        repaint();
    }

    public List<GraphEdge> getEdges() {
        return edges;
    }

    public void setSelectedEdge(GraphEdge edge) {
        this.selectedEdge = edge;
        repaint();
    }

    /**
     * Returns the edge whose rendered path is within HIT_STROKE_WIDTH/2 pixels of
     * (x, y), or null if no edge is near that point.
     * Requires at least one paintComponent call so the path cache is populated.
     */
    public GraphEdge findEdgeAt(int x, int y) {
        BasicStroke hitStroke = new BasicStroke(HIT_STROKE_WIDTH);
        for (GraphEdge edge : edges) {
            Path2D path = cachedPaths.get(edge.getEdgeId());
            if (path == null) {
                continue;
            }
            Shape hitArea = hitStroke.createStrokedShape(path);
            if (hitArea.contains(x, y)) {
                return edge;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (GraphEdge edge : edges) {
                paintEdge(g2d, edge);
            }
        } finally {
            g2d.dispose();
        }
    }

    private void paintEdge(Graphics2D g2d, GraphEdge edge) {
        GraphNode source = nodeIndex.get(edge.getSourceNodeId());
        GraphNode target = nodeIndex.get(edge.getTargetNodeId());
        if (source == null || target == null) {
            return;
        }

        Point startPt = source.getPortLocation(edge.getSourcePortId());
        Point endPt   = target.getPortLocation(edge.getTargetPortId());
        if (startPt == null || endPt == null) {
            return;
        }

        String srcPort = edge.getSourcePortId();
        String tgtPort = edge.getTargetPortId();

        // Compute and cache the path — reused by findEdgeAt without recomputing.
        Path2D path = router.calculatePath(startPt, srcPort, endPt, tgtPort);
        cachedPaths.put(edge.getEdgeId(), path);

        EdgeAttributes attrs     = edge.getAttributes();
        Color          edgeColor = attrs.getColor() != null ? attrs.getColor() : theme.getEdgeColor();

        // Selection highlight: wider translucent stroke drawn before the edge line.
        if (edge == selectedEdge) {
            g2d.setColor(SELECTION_COLOR);
            g2d.setStroke(new BasicStroke(attrs.getStrokeWidth() + 6f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(path);
        }

        // Normal edge stroke.
        g2d.setColor(edgeColor);
        if (attrs.getLineStyle() == EdgeAttributes.LineStyle.DASHED) {
            g2d.setStroke(new BasicStroke(attrs.getStrokeWidth(),
                    BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    10f, new float[]{6, 4}, 0f));
        } else {
            g2d.setStroke(new BasicStroke(attrs.getStrokeWidth()));
        }
        g2d.draw(path);

        if (attrs.getArrowType() != EdgeAttributes.ArrowType.NONE) {
            Point approachPt = router.getApproachPoint(startPt, srcPort, endPt, tgtPort);
            drawArrowhead(g2d, approachPt, endPt, attrs);
        }
    }

    private void drawArrowhead(Graphics2D g2d, Point start, Point end, EdgeAttributes attrs) {
        double angle = Math.atan2(end.y - start.y, end.x - start.x);

        int ax1 = (int) (end.x - ARROW_SIZE * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (end.y - ARROW_SIZE * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (end.x - ARROW_SIZE * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (end.y - ARROW_SIZE * Math.sin(angle + Math.PI / 6));

        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(end.x, end.y);
        arrowHead.addPoint(ax1, ay1);
        arrowHead.addPoint(ax2, ay2);

        g2d.setStroke(new BasicStroke(attrs.getStrokeWidth()));
        if (attrs.getArrowType() == EdgeAttributes.ArrowType.FILLED) {
            g2d.fill(arrowHead);
        } else {
            g2d.draw(arrowHead);
        }
    }

    // ---------------------------------------------------------------
    // Pass-through for mouse events
    // ---------------------------------------------------------------

    @Override
    public boolean contains(int x, int y) {
        return false;
    }
}
