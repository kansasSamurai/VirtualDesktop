package org.jwellman.demo.layereddiagramtoolv1;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/**
 * Complete diagram tool using JLayeredPane with grid, layers, and drag-and-drop
 */
public class LayeredDiagramToolv1 extends JFrame {
    private DiagramLayeredPane diagramPane;
    private JToolBar toolBar;
    private JPanel layerPanel;
    
    public LayeredDiagramToolv1() {
        setTitle("JLayeredPane Diagram Tool");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        
        createDiagramPane();
        createToolBar();
        createLayerPanel();
        
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(diagramPane), BorderLayout.CENTER);
        add(layerPanel, BorderLayout.EAST);
        
        setLocationRelativeTo(null);
    }
    
    private void createDiagramPane() {
        diagramPane = new DiagramLayeredPane();
    }
    
    private void createToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton addRectBtn = new JButton("Add Rectangle");
        addRectBtn.addActionListener(e -> addShape(ShapeType.RECTANGLE));
        toolBar.add(addRectBtn);
        
        JButton addCircleBtn = new JButton("Add Circle");
        addCircleBtn.addActionListener(e -> addShape(ShapeType.CIRCLE));
        toolBar.add(addCircleBtn);
        
        JButton addTextBtn = new JButton("Add Text");
        addTextBtn.addActionListener(e -> addText());
        toolBar.add(addTextBtn);
        
        toolBar.addSeparator();
        
        JCheckBox gridCheck = new JCheckBox("Show Grid", true);
        gridCheck.addActionListener(e -> diagramPane.setShowGrid(gridCheck.isSelected()));
        toolBar.add(gridCheck);
        
        JCheckBox snapCheck = new JCheckBox("Snap to Grid", true);
        snapCheck.addActionListener(e -> diagramPane.setSnapToGrid(snapCheck.isSelected()));
        toolBar.add(snapCheck);
        
        JButton bringForwardBtn = new JButton("Bring Forward");
        bringForwardBtn.addActionListener(e -> diagramPane.bringSelectedForward());
        toolBar.add(bringForwardBtn);
        
        JButton sendBackBtn = new JButton("Send Back");
        sendBackBtn.addActionListener(e -> diagramPane.sendSelectedBack());
        toolBar.add(sendBackBtn);
        
        toolBar.addSeparator();
        
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> diagramPane.deleteSelected());
        toolBar.add(deleteBtn);
    }
    
    private void createLayerPanel() {
        layerPanel = new JPanel();
        layerPanel.setLayout(new BoxLayout(layerPanel, BoxLayout.Y_AXIS));
        layerPanel.setBorder(BorderFactory.createTitledBorder("Layers"));
        layerPanel.setPreferredSize(new Dimension(200, 0));
        
        JButton refreshBtn = new JButton("Refresh Layer List");
        refreshBtn.addActionListener(e -> updateLayerList());
        layerPanel.add(refreshBtn);
        
        layerPanel.add(Box.createVerticalStrut(10));
        
        JLabel infoLabel = new JLabel("<html>Components are stacked<br>by layer depth.<br><br>" +
                                     "Higher numbers = on top</html>");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        layerPanel.add(infoLabel);
    }
    
    private void updateLayerList() {
        // Remove old layer info
        Component[] components = layerPanel.getComponents();
        for (int i = components.length - 1; i >= 2; i--) {
            if (components[i] instanceof JLabel && 
                ((JLabel)components[i]).getText().startsWith("Layer")) {
                layerPanel.remove(i);
            }
        }
        
        // Add current layer info
        Map<Integer, Integer> layerCounts = diagramPane.getLayerCounts();
        List<Integer> layers = new ArrayList<>(layerCounts.keySet());
        Collections.sort(layers, Collections.reverseOrder());
        
        for (Integer layer : layers) {
            JLabel layerLabel = new JLabel("Layer " + layer + ": " + 
                                          layerCounts.get(layer) + " components");
            layerLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 5));
            layerPanel.add(layerLabel);
        }
        
        layerPanel.revalidate();
        layerPanel.repaint();
    }
    
    private void addShape(ShapeType type) {
        DiagramShape shape = new DiagramShape(type);
        shape.setBounds(100, 100, 120, 80);
        diagramPane.addDiagramComponent(shape, DiagramLayeredPane.SHAPE_LAYER);
        updateLayerList();
    }
    
    private void addText() {
        DiagramText text = new DiagramText("Text");
        text.setBounds(100, 100, 150, 30);
        diagramPane.addDiagramComponent(text, DiagramLayeredPane.TEXT_LAYER);
        updateLayerList();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LayeredDiagramToolv1().setVisible(true);
        });
    }
}

/**
 * Custom JLayeredPane with grid and layer management
 */
class DiagramLayeredPane extends JLayeredPane {
    // Define layer constants
    public static final Integer GRID_LAYER = new Integer(0);
    public static final Integer BACKGROUND_LAYER = new Integer(100);
    public static final Integer SHAPE_LAYER = new Integer(200);
    public static final Integer TEXT_LAYER = new Integer(300);
    public static final Integer CONNECTION_LAYER = new Integer(400);
    public static final Integer SELECTION_LAYER = new Integer(500);
    
    private GridPanel gridPanel;
    private boolean showGrid = true;
    private boolean snapToGrid = true;
    private int gridSize = 20;
    private Component selectedComponent = null;
    
    public DiagramLayeredPane() {
        setPreferredSize(new Dimension(2000, 1500));
        setLayout(null); // Required for JLayeredPane
        
        // Add grid panel at the bottom layer
        gridPanel = new GridPanel(gridSize);
        gridPanel.setBounds(0, 0, 2000, 1500);
        add(gridPanel, GRID_LAYER);
        
        setupMouseListeners();
    }
    
    private void setupMouseListeners() {
        // Click on empty space to deselect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Component comp = getComponentAt(e.getPoint());
                if (comp == DiagramLayeredPane.this || comp == gridPanel) {
                    deselectAll();
                }
            }
        });
    }
    
    public void addDiagramComponent(JComponent component, Integer layer) {
        // Make component draggable
        DragHandler dragHandler = new DragHandler(this);
        component.addMouseListener(dragHandler);
        component.addMouseMotionListener(dragHandler);
        
        // Add selection handler
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectComponent(component);
            }
        });
        
        add(component, layer);
        revalidate();
        repaint();
    }
    
    private void selectComponent(Component comp) {
        deselectAll();
        selectedComponent = comp;
        
        // Visual feedback
        if (comp instanceof JComponent) {
            ((JComponent) comp).setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        }
    }
    
    private void deselectAll() {
        if (selectedComponent instanceof JComponent) {
            ((JComponent) selectedComponent).setBorder(null);
        }
        selectedComponent = null;
        repaint();
    }
    
    public void deleteSelected() {
        if (selectedComponent != null && selectedComponent != gridPanel) {
            remove(selectedComponent);
            selectedComponent = null;
            revalidate();
            repaint();
        }
    }
    
    public void bringSelectedForward() {
        if (selectedComponent != null) {
            Integer currentLayer = getLayer(selectedComponent);
            setLayer(selectedComponent, currentLayer + 1);
            repaint();
        }
    }
    
    public void sendSelectedBack() {
        if (selectedComponent != null) {
            Integer currentLayer = getLayer(selectedComponent);
            if (currentLayer > GRID_LAYER + 1) {
                setLayer(selectedComponent, currentLayer - 1);
                repaint();
            }
        }
    }
    
    public void setShowGrid(boolean show) {
        this.showGrid = show;
        gridPanel.setVisible(show);
        repaint();
    }
    
    public void setSnapToGrid(boolean snap) {
        this.snapToGrid = snap;
    }
    
    public boolean isSnapToGrid() {
        return snapToGrid;
    }
    
    public int getGridSize() {
        return gridSize;
    }
    
    public int snapToGrid(int value) {
        return Math.round((float) value / gridSize) * gridSize;
    }
    
    public Map<Integer, Integer> getLayerCounts() {
        Map<Integer, Integer> counts = new HashMap<>();
        
        for (Component comp : getComponents()) {
            if (comp != gridPanel) {
                Integer layer = getLayer(comp);
                counts.put(layer, counts.getOrDefault(layer, 0) + 1);
            }
        }
        
        return counts;
    }
    
    /**
     * Grid panel rendered at the bottom layer
     */
    private static class GridPanel extends JPanel {
        private int gridSize;
        
        public GridPanel(int gridSize) {
            this.gridSize = gridSize;
            setOpaque(false); // Transparent background
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            
            g2d.setColor(new Color(220, 220, 220));
            g2d.setStroke(new BasicStroke(1));
            
            int width = getWidth();
            int height = getHeight();
            
            // Draw vertical lines
            for (int x = 0; x < width; x += gridSize) {
                g2d.drawLine(x, 0, x, height);
            }
            
            // Draw horizontal lines
            for (int y = 0; y < height; y += gridSize) {
                g2d.drawLine(0, y, width, y);
            }
        }
    }
}

/**
 * Drag handler that works with JLayeredPane and grid snapping
 */
class DragHandler extends MouseAdapter {
    private DiagramLayeredPane layeredPane;
    private Point pressPoint;
    private Point componentLocation;
    private boolean dragging = false;
    
    public DragHandler(DiagramLayeredPane layeredPane) {
        this.layeredPane = layeredPane;
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        pressPoint = e.getPoint();
        Component comp = e.getComponent();
        componentLocation = comp.getLocation();
        dragging = true;
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) return;
        
        Component comp = e.getComponent();
        
        int dx = e.getX() - pressPoint.x;
        int dy = e.getY() - pressPoint.y;
        
        int newX = componentLocation.x + dx;
        int newY = componentLocation.y + dy;
        
        // Snap to grid if enabled
        if (layeredPane.isSnapToGrid()) {
            newX = layeredPane.snapToGrid(newX);
            newY = layeredPane.snapToGrid(newY);
        }
        
        comp.setLocation(newX, newY);
        layeredPane.repaint();
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        Component comp = e.getComponent();
        
        // Final snap on release
        if (layeredPane.isSnapToGrid()) {
            Point loc = comp.getLocation();
            comp.setLocation(layeredPane.snapToGrid(loc.x), 
                           layeredPane.snapToGrid(loc.y));
        }
        
        comp.setCursor(Cursor.getDefaultCursor());
        layeredPane.repaint();
    }
}

/**
 * Custom shape component for diagram
 */
enum ShapeType {
    RECTANGLE, CIRCLE, TRIANGLE
}

class DiagramShape extends JComponent {
    private ShapeType type;
    private Color fillColor;
    private Color borderColor;
    
    public DiagramShape(ShapeType type) {
        this.type = type;
        this.fillColor = new Color(173, 216, 230, 200);
        this.borderColor = new Color(70, 130, 180);
        setOpaque(false);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Fill shape
        g2d.setColor(fillColor);
        switch (type) {
            case RECTANGLE:
                g2d.fillRoundRect(5, 5, width - 10, height - 10, 10, 10);
                break;
            case CIRCLE:
                g2d.fillOval(5, 5, width - 10, height - 10);
                break;
            case TRIANGLE:
                int[] xPoints = {width / 2, width - 5, 5};
                int[] yPoints = {5, height - 5, height - 5};
                g2d.fillPolygon(xPoints, yPoints, 3);
                break;
        }
        
        // Draw border
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2));
        switch (type) {
            case RECTANGLE:
                g2d.drawRoundRect(5, 5, width - 10, height - 10, 10, 10);
                break;
            case CIRCLE:
                g2d.drawOval(5, 5, width - 10, height - 10);
                break;
            case TRIANGLE:
                int[] xPoints = {width / 2, width - 5, 5};
                int[] yPoints = {5, height - 5, height - 5};
                g2d.drawPolygon(xPoints, yPoints, 3);
                break;
        }
    }
    
    public void setFillColor(Color color) {
        this.fillColor = color;
        repaint();
    }
}

/**
 * Text component for diagram
 */
class DiagramText extends JPanel {
    private JTextField textField;
    
    public DiagramText(String initialText) {
        setLayout(new BorderLayout());
        setOpaque(false);
        
        textField = new JTextField(initialText);
        textField.setFont(new Font("Arial", Font.BOLD, 14));
        textField.setHorizontalAlignment(JTextField.CENTER);
        textField.setOpaque(false);
        textField.setBorder(null);
        
        add(textField, BorderLayout.CENTER);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw semi-transparent background when selected
        if (getBorder() != null) {
            g2d.setColor(new Color(255, 255, 200, 100));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    public String getText() {
        return textField.getText();
    }
    
    public void setText(String text) {
        textField.setText(text);
    }
}

/**
 * Example of adding connections between shapes
 */
class DiagramConnection extends JComponent {
    private Point startPoint;
    private Point endPoint;
    
    public DiagramConnection(Point start, Point end) {
        this.startPoint = start;
        this.endPoint = end;
        setOpaque(false);
        
        // Calculate bounds to encompass both points
        int minX = Math.min(start.x, end.x);
        int minY = Math.min(start.y, end.y);
        int maxX = Math.max(start.x, end.x);
        int maxY = Math.max(start.y, end.y);
        
        setBounds(minX - 10, minY - 10, maxX - minX + 20, maxY - minY + 20);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                            RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        
        // Convert to component-relative coordinates
        int x1 = startPoint.x - getX();
        int y1 = startPoint.y - getY();
        int x2 = endPoint.x - getX();
        int y2 = endPoint.y - getY();
        
        g2d.drawLine(x1, y1, x2, y2);
        
        // Draw arrow at end
        drawArrow(g2d, x1, y1, x2, y2);
    }
    
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        int arrowSize = 10;
        double angle = Math.atan2(y2 - y1, x2 - x1);
        
        int ax1 = (int) (x2 - arrowSize * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (y2 - arrowSize * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (x2 - arrowSize * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (y2 - arrowSize * Math.sin(angle + Math.PI / 6));
        
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint(x2, y2);
        arrowHead.addPoint(ax1, ay1);
        arrowHead.addPoint(ax2, ay2);
        
        g2d.fill(arrowHead);
    }
}