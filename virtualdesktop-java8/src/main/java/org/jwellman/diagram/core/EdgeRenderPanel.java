package org.jwellman.diagram.core;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
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
    private CanvasTheme theme;

    // Populated during paintComponent, consumed by findEdgeAt.
    private final Map<String, Path2D> cachedPaths = new HashMap<>();

    private GraphEdge selectedEdge;

    private static final int   ARROW_SIZE      = 10;
    private static final int   DIAMOND_SIZE    = 11;
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

    public void setTheme(CanvasTheme theme) {
        this.theme = theme;
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

        // Target-end decoration (triangle or diamond — both are valid on either end)
        Point targetApproach = router.getApproachPoint(startPt, srcPort, endPt, tgtPort);
        drawEndDecoration(g2d, attrs.getArrowType(), endPt, targetApproach, edgeColor, attrs.getStrokeWidth());

        // Source-end decoration
        Point sourceApproach = sourceApproachPoint(startPt, srcPort);
        drawEndDecoration(g2d, attrs.getSourceArrowType(), startPt, sourceApproach, edgeColor, attrs.getStrokeWidth());

        paintEdgeLabels(g2d, edge, path, startPt, srcPort, endPt, tgtPort);
    }

    // ---------------------------------------------------------------
    // Edge text labels ("label", "sourceLabel", "targetLabel" properties)
    // ---------------------------------------------------------------

    private static final Font LABEL_FONT  = new Font("SansSerif", Font.PLAIN, 11);
    private static final int  LABEL_GAP   = 4; // px between text and the line/port it hugs
    private static final int  LABEL_PAD_X = 4;
    private static final int  LABEL_PAD_Y = 1;

    /**
     * Positions are computed fresh from the current path/port locations on every
     * paint — nothing is persisted beyond the text itself, so labels track the
     * edge automatically as nodes move.
     *
     * The center label sits LABEL_GAP px off the line, on the side determined by
     * the local segment's orientation at its anchor (above a horizontal segment,
     * right of a vertical one). The end labels sit LABEL_GAP px off their own
     * port, on the side away from the node along the port's own axis — right of
     * the port for E, left for W, always above for both since both exit
     * horizontally; above the port for N, below for S, always to the right of
     * the line for both since both exit vertically. This only holds because the
     * router always exits/enters a port along its own axis (a horizontal stub
     * for E/W, a vertical stub for N/S) — true for every EdgeRouter in this
     * framework, not just the orthogonal one.
     */
    private void paintEdgeLabels(Graphics2D g2d, GraphEdge edge, Path2D path,
                                 Point sourcePt, String sourcePortId,
                                 Point targetPt, String targetPortId) {
        Map<String, Object> props = edge.getProperties();
        if (props == null || props.isEmpty()) {
            return;
        }

        String label = asLabelText(props.get("label"));
        if (label != null) {
            List<Point2D> points = flatten(path);
            if (points.size() >= 2) {
                PathAnchor anchor = pointAtDistance(points, pathLength(points) * 0.5);
                drawCenterLabel(g2d, label, anchor);
            }
        }

        String sourceLabel = asLabelText(props.get("sourceLabel"));
        if (sourceLabel != null) {
            drawEndLabel(g2d, sourceLabel, sourcePt, sourcePortId);
        }

        String targetLabel = asLabelText(props.get("targetLabel"));
        if (targetLabel != null) {
            drawEndLabel(g2d, targetLabel, targetPt, targetPortId);
        }
    }

    private static String asLabelText(Object value) {
        if (!(value instanceof String)) {
            return null;
        }
        String text = ((String) value).trim();
        return text.isEmpty() ? null : text;
    }

    /** Center label: hugs the line, above a horizontal segment or right of a vertical one. */
    private void drawCenterLabel(Graphics2D g2d, String text, PathAnchor anchor) {
        FontMetrics fm  = g2d.getFontMetrics(LABEL_FONT);
        int rectW = fm.stringWidth(text) + LABEL_PAD_X * 2;
        int rectH = fm.getHeight() + LABEL_PAD_Y * 2;
        int cx = (int) Math.round(anchor.point.getX());
        int cy = (int) Math.round(anchor.point.getY());

        int rx, ry;
        if (anchor.horizontal) {
            rx = cx - rectW / 2;
            ry = cy - LABEL_GAP - rectH;
        } else {
            rx = cx + LABEL_GAP;
            ry = cy - rectH / 2;
        }
        drawLabelRect(g2d, text, fm, rx, ry, rectW, rectH);
    }

    /** End label: hugs its own port, on the side away from the node along the port's axis. */
    private void drawEndLabel(Graphics2D g2d, String text, Point portPt, String portId) {
        FontMetrics fm  = g2d.getFontMetrics(LABEL_FONT);
        int rectW = fm.stringWidth(text) + LABEL_PAD_X * 2;
        int rectH = fm.getHeight() + LABEL_PAD_Y * 2;

        int rx, ry;
        if ("N".equals(portId) || "S".equals(portId)) {
            rx = portPt.x + LABEL_GAP;
            ry = "N".equals(portId) ? portPt.y - LABEL_GAP - rectH : portPt.y + LABEL_GAP;
        } else {
            rx = "W".equals(portId) ? portPt.x - LABEL_GAP - rectW : portPt.x + LABEL_GAP;
            ry = portPt.y - LABEL_GAP - rectH;
        }
        drawLabelRect(g2d, text, fm, rx, ry, rectW, rectH);
    }

    /** Fills the background matte and centers {@code text} within the given box. */
    private void drawLabelRect(Graphics2D g2d, String text, FontMetrics fm, int rx, int ry, int rectW, int rectH) {
        g2d.setColor(theme.getCanvasBackground());
        g2d.fillRect(rx, ry, rectW, rectH);

        g2d.setFont(LABEL_FONT);
        g2d.setColor(theme.getTextColor());
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, rx + (rectW - textWidth) / 2, ry + (rectH - fm.getHeight()) / 2 + fm.getAscent());
    }

    // ---------------------------------------------------------------
    // Path geometry helpers (arc-length walk; works for any router's Path2D)
    // ---------------------------------------------------------------

    /** A point on the path plus whether the segment it falls on is horizontal- or vertical-dominant. */
    private static final class PathAnchor {
        final Point2D point;
        final boolean horizontal;

        PathAnchor(Point2D point, boolean horizontal) {
            this.point      = point;
            this.horizontal = horizontal;
        }
    }

    private static List<Point2D> flatten(Path2D path) {
        List<Point2D> points = new ArrayList<>();
        double[] coords = new double[6];
        PathIterator it = path.getPathIterator(null);
        while (!it.isDone()) {
            int type = it.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                points.add(new Point2D.Double(coords[0], coords[1]));
            }
            it.next();
        }
        return points;
    }

    private static double pathLength(List<Point2D> points) {
        double total = 0;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i - 1).distance(points.get(i));
        }
        return total;
    }

    /** Walks {@code points} from the start by {@code distance} px; clamps to the final point past the end. */
    private static PathAnchor pointAtDistance(List<Point2D> points, double distance) {
        double remaining = Math.max(0, distance);
        for (int i = 1; i < points.size(); i++) {
            Point2D a = points.get(i - 1);
            Point2D b = points.get(i);
            double segLen = a.distance(b);
            if (segLen == 0) {
                continue;
            }
            boolean horizontal = Math.abs(b.getX() - a.getX()) >= Math.abs(b.getY() - a.getY());
            if (remaining <= segLen || i == points.size() - 1) {
                double t = Math.min(1.0, remaining / segLen);
                Point2D p = new Point2D.Double(
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t);
                return new PathAnchor(p, horizontal);
            }
            remaining -= segLen;
        }
        Point2D last = points.get(points.size() - 1);
        return new PathAnchor(last, true);
    }

    /** Dispatches to the correct glyph for one end's {@link EdgeAttributes.ArrowType}. Symmetric — usable on either end. */
    private void drawEndDecoration(Graphics2D g2d, EdgeAttributes.ArrowType type,
                                   Point tip, Point approach, Color color, float strokeWidth) {
        switch (type) {
            case OPEN:
            case FILLED:
                drawArrowhead(g2d, approach, tip, type == EdgeAttributes.ArrowType.FILLED, color, strokeWidth);
                break;
            case OPEN_DIAMOND:
            case FILLED_DIAMOND:
                drawDiamond(g2d, approach, tip, type == EdgeAttributes.ArrowType.FILLED_DIAMOND, color, strokeWidth);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void drawArrowhead(Graphics2D g2d, Point approach, Point tip,
                               boolean filled, Color color, float strokeWidth) {
        double angle = Math.atan2(tip.y - approach.y, tip.x - approach.x);

        int ax1 = (int) (tip.x - ARROW_SIZE * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (tip.y - ARROW_SIZE * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (tip.x - ARROW_SIZE * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (tip.y - ARROW_SIZE * Math.sin(angle + Math.PI / 6));

        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(tip.x, tip.y);
        arrowHead.addPoint(ax1, ay1);
        arrowHead.addPoint(ax2, ay2);

        g2d.setStroke(new BasicStroke(strokeWidth));
        if (filled) {
            g2d.setColor(color);
            g2d.fill(arrowHead);
        } else {
            // OPEN: hollow closed triangle — fill with canvas background then stroke
            g2d.setColor(theme.getCanvasBackground());
            g2d.fill(arrowHead);
            g2d.setColor(color);
            g2d.draw(arrowHead);
        }
    }

    /**
     * Returns a point DIAMOND_SIZE pixels from the port in the port's outward direction.
     * Despite the name, used as the approach point for any source-end decoration
     * (triangle or diamond) — unlike the target end, it is not router-path-aware.
     */
    private static Point sourceApproachPoint(Point portPt, String portId) {
        int d = DIAMOND_SIZE;
        if ("N".equals(portId)) {
            return new Point(portPt.x, portPt.y - d);
        }
        if ("S".equals(portId)) {
            return new Point(portPt.x, portPt.y + d);
        }
        if ("E".equals(portId)) {
            return new Point(portPt.x + d, portPt.y);
        }
        if ("W".equals(portId)) {
            return new Point(portPt.x - d, portPt.y);
        }
        return new Point(portPt.x, portPt.y - d);
    }

    /**
     * Draws a diamond decoration at {@code tip} (either end's port).
     * {@code approach} is a point along the edge outward from the port, used to
     * compute the diamond's orientation.
     */
    private void drawDiamond(Graphics2D g2d, Point approach, Point tip,
                              boolean filled, Color color, float strokeWidth) {
        double angle = Math.atan2(approach.y - tip.y, approach.x - tip.x);
        double halfW = DIAMOND_SIZE * 0.55;

        // Midpoint of the diamond (between front and back tips)
        int midX = (tip.x + approach.x) / 2;
        int midY = (tip.y + approach.y) / 2;
        // Back tip is the reflection of the front tip through the midpoint
        int backX = 2 * midX - tip.x;
        int backY = 2 * midY - tip.y;

        int leftX  = midX + (int) (halfW * Math.cos(angle + Math.PI / 2));
        int leftY  = midY + (int) (halfW * Math.sin(angle + Math.PI / 2));
        int rightX = midX + (int) (halfW * Math.cos(angle - Math.PI / 2));
        int rightY = midY + (int) (halfW * Math.sin(angle - Math.PI / 2));

        Polygon diamond = new Polygon();
        diamond.addPoint(tip.x, tip.y);
        diamond.addPoint(leftX,  leftY);
        diamond.addPoint(backX,  backY);
        diamond.addPoint(rightX, rightY);

        g2d.setStroke(new BasicStroke(strokeWidth));
        if (filled) {
            g2d.setColor(color);
            g2d.fill(diamond);
        } else {
            g2d.setColor(theme.getCanvasBackground());
            g2d.fill(diamond);
            g2d.setColor(color);
            g2d.draw(diamond);
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
