package org.jwellman.demo.visualconnection;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import org.jwellman.demo.visualconnection.ConnectablePanel.PortPosition;

/**
 * Complete visual connection system with Bezier curves, arrows, and labels
 */
public class VisualConnectionSystem extends JFrame {

    private JToolBar toolBar;
    private ConnectionCanvas canvas;
    private ConnectionManager connectionManager;

    private static final long serialVersionUID = 1L;

    public VisualConnectionSystem() {
        setTitle("Visual Connection System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);

        connectionManager = new ConnectionManager();
        canvas = new ConnectionCanvas(connectionManager);

        createToolBar();
        createDemoPanels();

        add(new JScrollPane(canvas), BorderLayout.CENTER);
        setLocationRelativeTo(null);
    }

    private void createToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton addPanelBtn = new JButton("Add Component");
        addPanelBtn.addActionListener(e -> addNewPanel());
        toolBar.add(addPanelBtn);

        toolBar.addSeparator();

        JCheckBox bezierCheck = new JCheckBox("Bezier Curves", true);
        bezierCheck.addActionListener(e -> {
            connectionManager.setUseBezierCurves(bezierCheck.isSelected());
            canvas.repaint();
        });
        toolBar.add(bezierCheck);

        JCheckBox arrowsCheck = new JCheckBox("Show Arrows", true);
        arrowsCheck.addActionListener(e -> {
            connectionManager.setShowArrows(arrowsCheck.isSelected());
            canvas.repaint();
        });
        toolBar.add(arrowsCheck);

        JCheckBox labelsCheck = new JCheckBox("Show Labels", true);
        labelsCheck.addActionListener(e -> {
            connectionManager.setShowLabels(labelsCheck.isSelected());
            canvas.repaint();
        });
        toolBar.add(labelsCheck);

        toolBar.addSeparator();

        JButton deleteConnBtn = new JButton("Delete Selected Connection");
        deleteConnBtn.addActionListener(e -> canvas.deleteSelectedConnection());
        toolBar.add(deleteConnBtn);

        JButton clearAllBtn = new JButton("Clear All");
        clearAllBtn.addActionListener(e -> {
            canvas.clearAll();
            connectionManager.clearAll();
        });
        toolBar.add(clearAllBtn);

        toolBar.addSeparator();

        JLabel infoLabel = new JLabel("  Click port to start connection, click another to complete");
        toolBar.add(infoLabel);

        add(toolBar, BorderLayout.NORTH);
    }

    private void createDemoPanels() {
        // Create demo components
        ConnectablePanel panel1 = createStyledPanel("Client", new Color(173, 216, 230));
        panel1.setBounds(100, 100, 150, 100);
        canvas.addConnectablePanel(panel1);

        ConnectablePanel panel2 = createStyledPanel("API Gateway", new Color(255, 218, 185));
        panel2.setBounds(350, 100, 150, 100);
        canvas.addConnectablePanel(panel2);

        ConnectablePanel panel3 = createStyledPanel("Auth Service", new Color(144, 238, 144));
        panel3.setBounds(200, 250, 150, 100);
        canvas.addConnectablePanel(panel3);

        ConnectablePanel panel4 = createStyledPanel("Database", new Color(230, 200, 250));
        panel4.setBounds(450, 250, 150, 100);
        canvas.addConnectablePanel(panel4);

        ConnectablePanel panel5 = createStyledPanel("Cache", new Color(255, 240, 200));
        panel5.setBounds(600, 100, 150, 100);
        canvas.addConnectablePanel(panel5);
    }

    private ConnectablePanel createStyledPanel(String title, Color bgColor) {
        ConnectablePanel panel = new ConnectablePanel(new BorderLayout());
        panel.setComponentId(title);
        panel.setBackground(bgColor);

        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }

    private void addNewPanel() {
        int count = canvas.getConnectablePanels().size();
        ConnectablePanel panel = createStyledPanel("Component " + (count + 1), new Color(200, 200, 255));
        panel.setBounds(100 + count * 30, 100 + count * 30, 150, 100);
        canvas.addConnectablePanel(panel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VisualConnectionSystem().setVisible(true);
        });
    }
}

/**
 * Canvas that handles drawing connections and user interactions
 */
class ConnectionCanvas extends JPanel {

    private ConnectionManager connectionManager;
    private List<ConnectablePanel> connectablePanels = new ArrayList<>();
    private ConnectionPort connectionStartPort = null;
    private Point tempConnectionEndPoint = null;
    private Connection selectedConnection = null;

    private static final long serialVersionUID = 1L;

    public ConnectionCanvas(ConnectionManager manager) {
        this.connectionManager = manager;
        setLayout(null);
        setBackground(new Color(245, 245, 245));
        setPreferredSize(new Dimension(2000, 1500));
        
        setupMouseHandlers();
    }
    
    private void setupMouseHandlers() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Check if clicking on a connection
                Connection clickedConnection = connectionManager.getConnectionAtPoint(e.getPoint());
                if (clickedConnection != null) {
                    selectedConnection = clickedConnection;
                    repaint();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                // Update temp connection line while drawing
                if (connectionStartPort != null) {
                    tempConnectionEndPoint = e.getPoint();
                    repaint();
                }
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    public void addConnectablePanel(ConnectablePanel panel) {
        PanelDragHandler dragHandler = new PanelDragHandler(panel, this);
        panel.addMouseListener(dragHandler);
        panel.addMouseMotionListener(dragHandler);
        
        // Add port click handler
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                ConnectionPort clickedPort = panel.getHoveredPort();
                if (clickedPort != null) {
                    handlePortClick(clickedPort);
                    e.consume();
                }
            }
        });
        
        connectablePanels.add(panel);
        add(panel);
        revalidate();
        repaint();
    }

    private void handlePortClick(ConnectionPort port) {
        if (connectionStartPort == null) {
            // Start a new connection
            connectionStartPort = port;
            tempConnectionEndPoint = port.getPositionInParent();
        } else {
            // Complete the connection
            if (connectionStartPort != port && connectionStartPort.getOwner() != port.getOwner()) {
                // Create connection
                @SuppressWarnings("unused")
                Connection conn = connectionManager.createConnection(
                        connectionStartPort, port,
                        generateConnectionLabel(connectionStartPort, port));

                // Reset
                connectionStartPort = null;
                tempConnectionEndPoint = null;
                repaint();
            } else {
                // Cancel if same port or same panel
                connectionStartPort = null;
                tempConnectionEndPoint = null;
                repaint();
            }
        }
    }
    
    private String generateConnectionLabel(ConnectionPort from, ConnectionPort to) {
        return from.getOwner().getComponentId() + " → " + to.getOwner().getComponentId();
    }
    
    public void deleteSelectedConnection() {
        if (selectedConnection != null) {
            connectionManager.removeConnection(selectedConnection);
            selectedConnection = null;
            repaint();
        }
    }
    
    public void clearAll() {
        connectablePanels.clear();
        removeAll();
        connectionStartPort = null;
        tempConnectionEndPoint = null;
        selectedConnection = null;
        revalidate();
        repaint();
    }
    
    public List<ConnectablePanel> getConnectablePanels() {
        return connectablePanels;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw all connections
        connectionManager.drawConnections(g2d, selectedConnection);
        
        // Draw temporary connection line while dragging
        if (connectionStartPort != null && tempConnectionEndPoint != null) {
            drawTempConnection(g2d);
        }
        
        g2d.dispose();
    }
    
    private void drawTempConnection(Graphics2D g2d) {
        Point start = connectionStartPort.getPositionInParent();
        Point end = tempConnectionEndPoint;
        
        g2d.setColor(new Color(100, 100, 255, 150));
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                                     0, new float[]{5, 5}, 0));
        
        if (connectionManager.isUseBezierCurves()) {
            drawBezierCurve(g2d, start, end);
        } else {
            g2d.drawLine(start.x, start.y, end.x, end.y);
        }
    }

    @SuppressWarnings("unused")
    private void drawBezierCurve(Graphics2D g2d, Point start, Point end) {
        int dx = end.x - start.x;
        int dy = end.y - start.y;

        Point2D ctrl1 = new Point2D.Double(start.x + dx * 0.5, start.y);
        Point2D ctrl2 = new Point2D.Double(end.x - dx * 0.5, end.y);

        CubicCurve2D curve = new CubicCurve2D.Double(start.x, start.y, ctrl1.getX(), ctrl1.getY(), ctrl2.getX(),
                ctrl2.getY(), end.x, end.y);

        g2d.draw(curve);
    }

    /**
     * Drag handler for connectable panels
     */
    private static class PanelDragHandler extends MouseAdapter {
        private ConnectablePanel panel;
        private ConnectionCanvas canvas;
        private Point pressPoint;
        private boolean isDragging = false;
        
        public PanelDragHandler(ConnectablePanel panel, ConnectionCanvas canvas) {
            this.panel = panel;
            this.canvas = canvas;
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            if (panel.getHoveredPort() != null) return;
            
            pressPoint = e.getPoint();
            isDragging = true;
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            if (!isDragging || panel.getHoveredPort() != null) return;
            
            int dx = e.getX() - pressPoint.x;
            int dy = e.getY() - pressPoint.y;
            
            Point location = panel.getLocation();
            panel.setLocation(location.x + dx, location.y + dy);
            
            canvas.repaint();
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            isDragging = false;
            if (panel.getHoveredPort() == null) {
                panel.setCursor(Cursor.getDefaultCursor());
            }
        }
    }
}

/**
 * Manages all connections and their visual representation
 */
class ConnectionManager {
    private List<Connection> connections = new ArrayList<>();
    private boolean useBezierCurves = true;
    private boolean showArrows = true;
    private boolean showLabels = true;
    
    public Connection createConnection(ConnectionPort from, ConnectionPort to, String label) {
        Connection conn = new Connection(from, to, label);
        connections.add(conn);
        return conn;
    }
    
    public void removeConnection(Connection connection) {
        connections.remove(connection);
    }
    
    public void clearAll() {
        connections.clear();
    }
    
    public List<Connection> getConnections() {
        return Collections.unmodifiableList(connections);
    }
    
    public Connection getConnectionAtPoint(Point p) {
        for (Connection conn : connections) {
            if (conn.containsPoint(p, 10)) {
                return conn;
            }
        }
        return null;
    }
    
    public void drawConnections(Graphics2D g2d, Connection selectedConnection) {
        for (Connection conn : connections) {
            boolean isSelected = conn == selectedConnection;
            conn.draw(g2d, useBezierCurves, showArrows, showLabels, isSelected);
        }
    }
    
    public void setUseBezierCurves(boolean use) {
        this.useBezierCurves = use;
    }
    
    public boolean isUseBezierCurves() {
        return useBezierCurves;
    }
    
    public void setShowArrows(boolean show) {
        this.showArrows = show;
    }
    
    public void setShowLabels(boolean show) {
        this.showLabels = show;
    }
}

/**
 * Represents a connection between two ports
 */
class Connection implements Serializable {

    private ConnectionPort fromPort;
    private ConnectionPort toPort;
    private String label;
    private Color color;
    private transient Path2D cachedPath;

    private static final long serialVersionUID = 1L;

    public Connection(ConnectionPort from, ConnectionPort to, String label) {
        this.fromPort = from;
        this.toPort = to;
        this.label = label;
        this.color = new Color(80, 80, 80);
    }

    public void draw(Graphics2D g2d, boolean useBezier, boolean showArrow, boolean showLabel, boolean isSelected) {
        Point start = fromPort.getPositionInParent();
        Point end = toPort.getPositionInParent();

        // Set color and stroke
        if (isSelected) {
            g2d.setColor(new Color(255, 100, 100));
            g2d.setStroke(new BasicStroke(3));
        } else {
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(2));
        }

        if (useBezier) {
            drawBezierConnection(g2d, start, end, showArrow);
        } else {
            drawStraightConnection(g2d, start, end, showArrow);
        }

        // Draw label
        if (showLabel && label != null && !label.isEmpty()) {
            drawLabel(g2d, start, end);
        }
    }

    private void drawStraightConnection(Graphics2D g2d, Point start, Point end, boolean showArrow) {
        g2d.drawLine(start.x, start.y, end.x, end.y);

        cachedPath = new Path2D.Double();
        cachedPath.moveTo(start.x, start.y);
        cachedPath.lineTo(end.x, end.y);

        if (showArrow) {
            drawArrow(g2d, start, end);
        }
    }

    /* 
     * Here's an updated method that adjusts based on all 8 port positions
     * instead of just the original four(4).
     */
    private void drawBezierConnection(Graphics2D g2d, Point start, Point end, boolean showArrow) {
        int dx = end.x - start.x;
        int dy = end.y - start.y;

        double distance = Math.sqrt(dx * dx + dy * dy);
        double curveFactor = Math.min(distance * 0.4, 100);

        Point2D ctrl1, ctrl2;

        ConnectablePanel.PortPosition startPos = fromPort.getPosition();
        ConnectablePanel.PortPosition endPos = toPort.getPosition();

        // Calculate control point 1 based on start port position
        ctrl1 = getControlPoint(start, startPos, curveFactor);

        // Calculate control point 2 based on end port position
        ctrl2 = getControlPoint(end, endPos, curveFactor);

        CubicCurve2D curve = new CubicCurve2D.Double(start.x, start.y, 
                ctrl1.getX(), ctrl1.getY(), 
                ctrl2.getX(), ctrl2.getY(), 
                end.x, end.y);

        g2d.draw(curve);
        cachedPath = new Path2D.Double(curve);

        if (showArrow) {
            Point2D arrowStart = getPointOnCurve(curve, 0.9);
            Point2D arrowEnd = new Point2D.Double(end.x, end.y);
            drawArrowAt(g2d, arrowStart, arrowEnd);
        }
    }

    // Helper method to calculate control point based on port position
    private Point2D getControlPoint(Point portPos, 
            ConnectablePanel.PortPosition position, double curveFactor) {
        switch (position) {
        case TOP:
            return new Point2D.Double(portPos.x, portPos.y - curveFactor);
        case BOTTOM:
            return new Point2D.Double(portPos.x, portPos.y + curveFactor);
        case LEFT:
            return new Point2D.Double(portPos.x - curveFactor, portPos.y);
        case RIGHT:
            return new Point2D.Double(portPos.x + curveFactor, portPos.y);
        case TOP_LEFT:
            return new Point2D.Double(portPos.x - curveFactor * 0.7, portPos.y - curveFactor * 0.7);
        case TOP_RIGHT:
            return new Point2D.Double(portPos.x + curveFactor * 0.7, portPos.y - curveFactor * 0.7);
        case BOTTOM_LEFT:
            return new Point2D.Double(portPos.x - curveFactor * 0.7, portPos.y + curveFactor * 0.7);
        case BOTTOM_RIGHT:
            return new Point2D.Double(portPos.x + curveFactor * 0.7, portPos.y + curveFactor * 0.7);
        default:
            return new Point2D.Double(portPos.x, portPos.y);
        }
    }

    /* 
     * You're absolutely right! The Bezier curve control points were calculated
     * assuming horizontal flow. Here's an updated method that adjusts based on the
     * port positions.
     */
    @SuppressWarnings("unused")
    private void drawBezierConnection_old(Graphics2D g2d, Point start, Point end, boolean showArrow) {
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        
        // Calculate control points based on port orientations
        double distance = Math.sqrt(dx * dx + dy * dy);
        double curveFactor = Math.min(distance * 0.4, 100);
        
        Point2D ctrl1, ctrl2;
        
        // Determine start port orientation
        ConnectablePanel.PortPosition startPos = fromPort.getPosition();
        ConnectablePanel.PortPosition endPos = toPort.getPosition();
        
        // Calculate control points based on port positions
        if (startPos == ConnectablePanel.PortPosition.TOP || 
            startPos == ConnectablePanel.PortPosition.BOTTOM) {
            // Vertical start - control point extends vertically
            if (startPos == ConnectablePanel.PortPosition.TOP) {
                ctrl1 = new Point2D.Double(start.x, start.y - curveFactor);
            } else {
                ctrl1 = new Point2D.Double(start.x, start.y + curveFactor);
            }
        } else {
            // Horizontal start - control point extends horizontally
            if (startPos == ConnectablePanel.PortPosition.LEFT) {
                ctrl1 = new Point2D.Double(start.x - curveFactor, start.y);
            } else {
                ctrl1 = new Point2D.Double(start.x + curveFactor, start.y);
            }
        }
        
        if (endPos == ConnectablePanel.PortPosition.TOP || 
            endPos == ConnectablePanel.PortPosition.BOTTOM) {
            // Vertical end - control point extends vertically
            if (endPos == ConnectablePanel.PortPosition.TOP) {
                ctrl2 = new Point2D.Double(end.x, end.y - curveFactor);
            } else {
                ctrl2 = new Point2D.Double(end.x, end.y + curveFactor);
            }
        } else {
            // Horizontal end - control point extends horizontally
            if (endPos == ConnectablePanel.PortPosition.LEFT) {
                ctrl2 = new Point2D.Double(end.x - curveFactor, end.y);
            } else {
                ctrl2 = new Point2D.Double(end.x + curveFactor, end.y);
            }
        }
        
        CubicCurve2D curve = new CubicCurve2D.Double(
            start.x, start.y,
            ctrl1.getX(), ctrl1.getY(),
            ctrl2.getX(), ctrl2.getY(),
            end.x, end.y
        );
        
        g2d.draw(curve);
        cachedPath = new Path2D.Double(curve);
        
        if (showArrow) {
            // Calculate arrow at end of curve
            Point2D arrowStart = getPointOnCurve(curve, 0.9);
            Point2D arrowEnd = new Point2D.Double(end.x, end.y);
            drawArrowAt(g2d, arrowStart, arrowEnd);
        }
    }

    /*
     * You're absolutely right! The Bezier curve control points are calculated
     * assuming horizontal flow. Here's the updated method that adjusts based on the
     * port positions:
     * 
     */
    @SuppressWarnings("unused")
    private void drawBezierConnection_older(Graphics2D g2d, Point start, Point end, boolean showArrow) {
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        
        // Calculate control points for smooth curve
        double distance = Math.sqrt(dx * dx + dy * dy);
        double curveFactor = Math.min(distance * 0.4, 100);
        
        Point2D ctrl1 = new Point2D.Double(start.x + curveFactor, start.y);
        Point2D ctrl2 = new Point2D.Double(end.x - curveFactor, end.y);
        
        CubicCurve2D curve = new CubicCurve2D.Double(
            start.x, start.y,
            ctrl1.getX(), ctrl1.getY(),
            ctrl2.getX(), ctrl2.getY(),
            end.x, end.y
        );
        
        g2d.draw(curve);
        cachedPath = new Path2D.Double(curve);
        
        if (showArrow) {
            // Calculate arrow at end of curve
            Point2D arrowStart = getPointOnCurve(curve, 0.9);
            Point2D arrowEnd = new Point2D.Double(end.x, end.y);
            drawArrowAt(g2d, arrowStart, arrowEnd);
        }
    }
    
    private void drawArrow(Graphics2D g2d, Point start, Point end) {
        int arrowSize = 10;
        double angle = Math.atan2(end.y - start.y, end.x - start.x);
        
        int x1 = (int) (end.x - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (end.y - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (end.x - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (end.y - arrowSize * Math.sin(angle + Math.PI / 6));
        
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(end.x, end.y);
        arrowHead.addPoint(x1, y1);
        arrowHead.addPoint(x2, y2);
        
        g2d.fill(arrowHead);
    }
    
    private void drawArrowAt(Graphics2D g2d, Point2D start, Point2D end) {
        int arrowSize = 10;
        double angle = Math.atan2(end.getY() - start.getY(), end.getX() - start.getX());
        
        int x1 = (int) (end.getX() - arrowSize * Math.cos(angle - Math.PI / 6));
        int y1 = (int) (end.getY() - arrowSize * Math.sin(angle - Math.PI / 6));
        int x2 = (int) (end.getX() - arrowSize * Math.cos(angle + Math.PI / 6));
        int y2 = (int) (end.getY() - arrowSize * Math.sin(angle + Math.PI / 6));
        
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint((int) end.getX(), (int) end.getY());
        arrowHead.addPoint(x1, y1);
        arrowHead.addPoint(x2, y2);
        
        g2d.fill(arrowHead);
    }
    
    private void drawLabel(Graphics2D g2d, Point start, Point end) {
        // Calculate midpoint
        int midX = (start.x + end.x) / 2;
        int midY = (start.y + end.y) / 2;

        // Draw label background
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(label);
        int labelHeight = fm.getHeight();

        g2d.setColor(new Color(255, 255, 255, 220));
        g2d.fillRoundRect(midX - labelWidth / 2 - 4, midY - labelHeight / 2 - 2, 
                labelWidth + 8, labelHeight + 4, 5, 5);

        // Draw label border
        g2d.setColor(COLOR.GRAY);
        g2d.drawRoundRect(midX - labelWidth / 2 - 4, midY - labelHeight / 2 - 2, 
                labelWidth + 8, labelHeight + 4, 5, 5);

        // Draw label text
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.drawString(label, midX - labelWidth / 2, midY + fm.getAscent() / 2);
    }

    private Point2D getPointOnCurve(CubicCurve2D curve, double t) {
        double x = Math.pow(1 - t, 3) * curve.getX1() + 3 * Math.pow(1 - t, 2) * t * curve.getCtrlX1()
                + 3 * (1 - t) * Math.pow(t, 2) * curve.getCtrlX2() + Math.pow(t, 3) * curve.getX2();

        double y = Math.pow(1 - t, 3) * curve.getY1() + 3 * Math.pow(1 - t, 2) * t * curve.getCtrlY1()
                + 3 * (1 - t) * Math.pow(t, 2) * curve.getCtrlY2() + Math.pow(t, 3) * curve.getY2();

        return new Point2D.Double(x, y);
    }

    public boolean containsPoint(Point p, int tolerance) {
        if (cachedPath == null)
            return false;

        BasicStroke stroke = new BasicStroke(tolerance);
        Shape strokedShape = stroke.createStrokedShape(cachedPath);
        return strokedShape.contains(p);
    }

    public ConnectionPort getFromPort() {
        return fromPort;
    }

    public ConnectionPort getToPort() {
        return toPort;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
}

/**
 * ConnectablePanel - draggable panel with connection ports
 */
class ConnectablePanel extends JPanel {

    private String componentId;
    private PortBorder portBorder;

    private boolean showPorts = false;
    private ConnectionPort hoveredPort = null;
    private List<ConnectionPort> ports = new ArrayList<>();

    public enum PortPosition {
        TOP, RIGHT, BOTTOM, LEFT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private static final Border INNER = BorderFactory.createLineBorder(Color.black, 2);

    private static final long serialVersionUID = 1L;

    public ConnectablePanel(LayoutManager layout) {
        super(layout);

        this.componentId = "Panel_" + System.currentTimeMillis();
        initializePorts();
        setupBorder();
        setupMouseHandlers();
    }
    
    private void initializePorts() {
        ports.add(new ConnectionPort(PortPosition.TOP, this));
        ports.add(new ConnectionPort(PortPosition.RIGHT, this));
        ports.add(new ConnectionPort(PortPosition.BOTTOM, this));
        ports.add(new ConnectionPort(PortPosition.LEFT, this));
        ports.add(new ConnectionPort(PortPosition.TOP_LEFT, this));
        ports.add(new ConnectionPort(PortPosition.TOP_RIGHT, this));
        ports.add(new ConnectionPort(PortPosition.BOTTOM_LEFT, this));
        ports.add(new ConnectionPort(PortPosition.BOTTOM_RIGHT, this));
    }

    private void setupBorder() {
        portBorder = new PortBorder(this);
        
        CompoundBorder b = BorderFactory.createCompoundBorder(portBorder, INNER);
        setBorder(b);
    }

    private void setupMouseHandlers() {
        MouseAdapter mouseHandler = new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                showPorts = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {

                // This original AI code caused a bug:
//                Point p = e.getPoint();
//                if (!isNearAnyPort(p)) {
//                    showPorts = false;
//                    hoveredPort = null;
//                    repaint();
//                }

                // No need for the conditional?
                showPorts = false;
                hoveredPort = null;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                ConnectionPort newHoveredPort = getPortAtPoint(e.getPoint());
                if (newHoveredPort != hoveredPort) {
                    hoveredPort = newHoveredPort;
                    setCursor(hoveredPort != null 
                            ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                            : Cursor.getDefaultCursor());
                    repaint();
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    @SuppressWarnings("unused")
    // Needed for original AI mouseExited code
    private boolean isNearAnyPort(Point p) {
        return getPortAtPoint(p) != null;
    }

    private ConnectionPort getPortAtPoint(Point p) {
        for (ConnectionPort port : ports) {
            Point portCenter = port.getRelativePosition();
            if (p.distance(portCenter) <= ConnectionPort.PORT_RADIUS + 5) {
                return port;
            }
        }
        return null;
    }

    public boolean isShowingPorts() {
        return showPorts;
    }

    public List<ConnectionPort> getPorts() {
        return ports;
    }

    public ConnectionPort getHoveredPort() {
        return hoveredPort;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String id) {
        this.componentId = id;
    }

    private static class PortBorder extends AbstractBorder {

        private ConnectablePanel panel;

        // Currently, all instances can share the same instance as they are never changed.
        private static final Insets INSETS = new Insets(5, 5, 5, 5);

        // Currently, all instances can share the same instance as they are never changed.
        private static final Stroke BASIC_STROKE_2 = new BasicStroke(2);

        private static final long serialVersionUID = 1L;

        public PortBorder(ConnectablePanel panel) {
            this.panel = panel;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(COLOR.DARK_GRAY);
            g2d.setStroke(BASIC_STROKE_2 );
            g2d.drawRect(x + 1, y + 1, width - 2, height - 2);

            if (panel.isShowingPorts()) {
                for (ConnectionPort port : panel.getPorts()) {
                    drawPort(g2d, port, panel.getHoveredPort() == port);
                }
            }

            g2d.dispose();
        }

        /* I believe this works because Swing will automatically clip the ovals
         * according to the bounds of the current component. 
         * Note: I don't really like the corners as circles because they aren't
         * very big and also only draw a quarter circle.  It just doesn't really "pop".
         * I think maybe squared off corners will look better?
         */
        private void drawPort(Graphics2D g2d, ConnectionPort port, boolean hovered) {
            Point pos = port.getRelativePosition();
            int r = ConnectionPort.PORT_RADIUS;

            // External Fill (keep this)
//            g2d.setColor(hovered ? COLOR.WHAT : COLOR.GRAY_TRANSPARENT);
//            g2d.setColor(hovered ? COLOR.DARK_GRAY : COLOR.GRAY_TRANSPARENT);
            g2d.setColor(hovered ? COLOR.RED : COLOR.LIGHT_GRAY);
            g2d.fillOval(pos.x - r, pos.y - r, r * 2, r * 2);

            // External Inner Circle (probably will deprecate)
//            int ir = r - 2;
//            g2d.setColor(COLOR.DARK_GRAY); //(Color.WHITE);
//            g2d.fillOval(pos.x - ir, pos.y - ir, ir * 2, ir * 2);

            // Inset Outline
            g2d.setColor(hovered ? COLOR.RED : Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawOval(pos.x - r, pos.y - r, r * 2, r * 2);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return INSETS;
        }

    }

}

/**
 * Connection port on a panel
 */
class ConnectionPort {

    private String portId;
    private ConnectablePanel owner;
    private ConnectablePanel.PortPosition position;

    public static final int PORT_RADIUS = 6;

    public ConnectionPort(ConnectablePanel.PortPosition position, ConnectablePanel owner) {
        this.owner = owner;
        this.position = position;
        this.portId = "Port_" + position + "_" + System.currentTimeMillis();
    }

    public Point getRelativePosition() {
        int w = owner.getWidth();
        int h = owner.getHeight();
        
        switch (position) {
            case TOP: 
                return new Point(w / 2, 0);
            case RIGHT: 
                return new Point(w, h / 2);
            case BOTTOM: 
                return new Point(w / 2, h);
            case LEFT: 
                return new Point(0, h / 2);
            case TOP_LEFT: 
                return new Point(0, 0);
            case TOP_RIGHT: 
                return new Point(w, 0);
            case BOTTOM_LEFT: 
                return new Point(0, h);
            case BOTTOM_RIGHT: 
                return new Point(w, h);
            default: 
                return new Point(w / 2, h / 2);
        }
    }
    
    public Point getRelativePosition_old() {
        int w = owner.getWidth();
        int h = owner.getHeight();

        switch (position) {
        case TOP:
            return new Point(w / 2, 0);
        case RIGHT:
            return new Point(w, h / 2);
        case BOTTOM:
            return new Point(w / 2, h);
        case LEFT:
            return new Point(0, h / 2);
        default:
            return new Point(w / 2, h / 2);
        }
    }

    public Point getPositionInParent() {
        Point rel = getRelativePosition();
        Point loc = owner.getLocation();
        return new Point(loc.x + rel.x, loc.y + rel.y);
    }

    public String getPortId() {
        return portId;
    }

    public PortPosition getPosition() {
        return this.position;
    }

    public ConnectablePanel getOwner() {
        return owner;
    }

}

class COLOR {
    public static final Color GRAY = new Color(150, 150, 150);
    public static final Color DARK_GRAY = new Color(100, 100, 100);
    public static final Color LIGHT_GRAY = new Color(245, 245, 245); // Color.LIGHT_GRAY;
    public static final Color GRAY_TRANSPARENT = new Color(150, 150, 150, 200);
    public static final Color WHAT = new Color(100, 150, 255);
    public static final Color RED = Color.red;
    public static final Color YELLOW = Color.yellow;
    public static final Color OUTLINE = Color.green; // new Color(0, 100, 200);
}
