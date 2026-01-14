package org.jwellman.demo.connectablepanel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;

/**
 * A draggable JPanel with connection ports that appear on hover.
 * Ports can be used to visually connect components together.
 */
public class ConnectablePanel extends JPanel {

    private boolean showPorts = false;
    private List<ConnectionPort> ports = new ArrayList<>();
    private ConnectionPort hoveredPort = null;
    private PortBorder portBorder;
    private boolean draggable = true;
    private String componentId;

    // Port positions (can be customized)
    public enum PortPosition {
        TOP, RIGHT, BOTTOM, LEFT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private static final long serialVersionUID = 1L;

    public ConnectablePanel() {
        this(null);
    }
    
    public ConnectablePanel(LayoutManager layout) {
        super(layout);
        this.componentId = "Panel_" + System.currentTimeMillis();
        initializePorts();
        setupBorder();
        setupMouseHandlers();
    }
    
    private void initializePorts() {
        // Create default ports on all four sides
        ports.add(new ConnectionPort(PortPosition.TOP, this));
        ports.add(new ConnectionPort(PortPosition.RIGHT, this));
        ports.add(new ConnectionPort(PortPosition.BOTTOM, this));
        ports.add(new ConnectionPort(PortPosition.LEFT, this));
    }
    
    private void setupBorder() {
        portBorder = new PortBorder(this);
        setBorder(portBorder);
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
                // Only hide ports if we're not near any port
                Point p = e.getPoint();
                if (!isNearAnyPort(p)) {
                    showPorts = false;
                    hoveredPort = null;
                    repaint();
                }
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                ConnectionPort newHoveredPort = getPortAtPoint(e.getPoint());
                if (newHoveredPort != hoveredPort) {
                    hoveredPort = newHoveredPort;
                    
                    // Change cursor when over a port
                    if (hoveredPort != null) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                    
                    repaint();
                }
            }
        };
        
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }
    
    private boolean isNearAnyPort(Point p) {
        return getPortAtPoint(p) != null;
    }
    
    private ConnectionPort getPortAtPoint(Point p) {
        for (ConnectionPort port : ports) {
            Point portCenter = port.getAbsolutePosition();
            double distance = p.distance(portCenter);
            if (distance <= ConnectionPort.PORT_RADIUS + 5) {
                return port;
            }
        }
        return null;
    }
    
    public boolean isShowingPorts() {
        return showPorts;
    }
    
    public List<ConnectionPort> getPorts() {
        return Collections.unmodifiableList(ports);
    }
    
    public ConnectionPort getHoveredPort() {
        return hoveredPort;
    }
    
    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }
    
    public boolean isDraggable() {
        return draggable;
    }
    
    public String getComponentId() {
        return componentId;
    }
    
    public void setComponentId(String id) {
        this.componentId = id;
    }
    
    /**
     * Add a custom port at a specific position
     */
    public ConnectionPort addPort(PortPosition position) {
        ConnectionPort port = new ConnectionPort(position, this);
        ports.add(port);
        repaint();
        return port;
    }
    
    /**
     * Remove a port
     */
    public void removePort(ConnectionPort port) {
        ports.remove(port);
        if (hoveredPort == port) {
            hoveredPort = null;
        }
        repaint();
    }

    /**
     * Clear all ports
     */
    public void clearPorts() {
        ports.clear();
        hoveredPort = null;
        repaint();
    }

    /**
     * Custom border that draws connection ports
     */
    private static class PortBorder extends AbstractBorder {

        private ConnectablePanel panel;
        private static final int BORDER_WIDTH = 2;

        private static final long serialVersionUID = 1L;

        public PortBorder(ConnectablePanel panel) {
            this.panel = panel;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw border
            g2d.setColor(new Color(100, 100, 100));
            g2d.setStroke(new BasicStroke(BORDER_WIDTH));
            g2d.drawRect(x + 1, y + 1, width - 2, height - 2);
            
            // Draw ports if showing
            if (panel.isShowingPorts()) {
                for (ConnectionPort port : panel.getPorts()) {
                    drawPort(g2d, port, panel.getHoveredPort() == port);
                }
            }
            
            g2d.dispose();
        }
        
        private void drawPort(Graphics2D g2d, ConnectionPort port, boolean hovered) {
            Point pos = port.getRelativePosition();
            int radius = ConnectionPort.PORT_RADIUS;
            
            // Draw outer circle (port background)
            if (hovered) {
                g2d.setColor(new Color(100, 150, 255));
            } else {
                g2d.setColor(new Color(150, 150, 150, 200));
            }
            
            Ellipse2D circle = new Ellipse2D.Double(
                pos.x - radius, pos.y - radius,
                radius * 2, radius * 2
            );
            g2d.fill(circle);
            
            // Draw inner circle (port indicator)
            g2d.setColor(Color.WHITE);
            int innerRadius = radius - 2;
            Ellipse2D innerCircle = new Ellipse2D.Double(
                pos.x - innerRadius, pos.y - innerRadius,
                innerRadius * 2, innerRadius * 2
            );
            g2d.fill(innerCircle);
            
            // Draw outline
            g2d.setColor(hovered ? new Color(0, 100, 200) : Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.draw(circle);
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(5, 5, 5, 5);
        }
        
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = insets.top = insets.bottom = 5;
            return insets;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        JPanel canvas = new JPanel(null);

        // Create a connectable panel
        ConnectablePanel myPanel = new ConnectablePanel(new BorderLayout());
        myPanel.setComponentId("MyComponent");
        myPanel.setBackground(Color.LIGHT_GRAY);
        myPanel.setBounds(100, 100, 200, 150);

        // Add content to the panel
        myPanel.add(new JLabel("My Content", SwingConstants.CENTER), BorderLayout.CENTER);
        myPanel.add(new JButton("Action"), BorderLayout.SOUTH);

        // Customize ports if needed
        myPanel.clearPorts(); // Remove default ports
        myPanel.addPort(ConnectablePanel.PortPosition.TOP);
        myPanel.addPort(ConnectablePanel.PortPosition.BOTTOM);

        // Access ports programmatically
        for (ConnectionPort port : myPanel.getPorts()) {
            System.out.println("Port: " + port.getPosition());
            System.out.println("Position in parent: " + port.getPositionInParent());
        }

        canvas.add(myPanel);
        frame.add(canvas);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

}

/**
 * Represents a connection port on a ConnectablePanel
 */
class ConnectionPort {
    public static final int PORT_RADIUS = 6;
    
    private ConnectablePanel.PortPosition position;
    private ConnectablePanel owner;
    private String portId;
    private Object userData; // For storing custom data
    
    public ConnectionPort(ConnectablePanel.PortPosition position, ConnectablePanel owner) {
        this.position = position;
        this.owner = owner;
        this.portId = "Port_" + position + "_" + System.currentTimeMillis();
    }
    
    /**
     * Get the port position relative to the panel bounds
     */
    public Point getRelativePosition() {
        int width = owner.getWidth();
        int height = owner.getHeight();
        
        switch (position) {
            case TOP:
                return new Point(width / 2, 0);
            case RIGHT:
                return new Point(width, height / 2);
            case BOTTOM:
                return new Point(width / 2, height);
            case LEFT:
                return new Point(0, height / 2);
            case TOP_LEFT:
                return new Point(0, 0);
            case TOP_RIGHT:
                return new Point(width, 0);
            case BOTTOM_LEFT:
                return new Point(0, height);
            case BOTTOM_RIGHT:
                return new Point(width, height);
            default:
                return new Point(width / 2, height / 2);
        }
    }
    
    /**
     * Get the port position in absolute screen coordinates
     */
    public Point getAbsolutePosition() {
        Point relative = getRelativePosition();
        Point panelLocation = owner.getLocation();
        return new Point(panelLocation.x + relative.x, panelLocation.y + relative.y);
    }
    
    /**
     * Get the port position in parent container coordinates
     */
    public Point getPositionInParent() {
        Point relative = getRelativePosition();
        Point panelLocation = owner.getLocation();
        return new Point(panelLocation.x + relative.x, panelLocation.y + relative.y);
    }
    
    public ConnectablePanel.PortPosition getPosition() {
        return position;
    }
    
    public ConnectablePanel getOwner() {
        return owner;
    }
    
    public String getPortId() {
        return portId;
    }
    
    public void setUserData(Object data) {
        this.userData = data;
    }
    
    public Object getUserData() {
        return userData;
    }
    
    @Override
    public String toString() {
        return "Port[" + position + " on " + owner.getComponentId() + "]";
    }
}

/**
 * Example of using ConnectablePanel in your own application
 */
class ConnectablePanelUsageExample {


}
