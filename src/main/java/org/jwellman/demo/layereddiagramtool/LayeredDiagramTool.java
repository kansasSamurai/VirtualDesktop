package org.jwellman.demo.layereddiagramtool;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Complete diagram tool using JLayeredPane with grid, layers, and drag-and-drop
 */
public class LayeredDiagramTool extends JFrame {

    private JPanel layerPanel;
    private JToolBar toolBar;
    private DiagramLayeredPane diagramPane;

    private static final long serialVersionUID = 1L;

    public LayeredDiagramTool() {
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

        JButton addTriangleBtn = new JButton("Add Triangle");
        addTriangleBtn.addActionListener(e -> addShape(ShapeType.TRIANGLE));
        toolBar.add(addTriangleBtn);

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

        toolBar.addSeparator();

        JButton saveBtn = new JButton("Save Diagram");
        saveBtn.addActionListener(e -> saveDiagram());
        toolBar.add(saveBtn);

        JButton loadBtn = new JButton("Load Diagram");
        loadBtn.addActionListener(e -> loadDiagram());
        toolBar.add(loadBtn);
    
    }

    private void saveDiagram() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Diagram");
        fileChooser.setSelectedFile(new java.io.File("diagram.json"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                diagramPane.saveDiagram(file);
                JOptionPane.showMessageDialog(this, "Diagram saved successfully!", "Save Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving diagram: " + ex.getMessage(), "Save Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void loadDiagram() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Diagram");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                diagramPane.loadDiagram(file);
                JOptionPane.showMessageDialog(this, "Diagram loaded successfully!", "Load Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading diagram: " + ex.getMessage(), "Load Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void createLayerPanel() {
        layerPanel = new JPanel();
        layerPanel.setLayout(new BorderLayout());
        layerPanel.setBorder(BorderFactory.createTitledBorder("Layers"));
        layerPanel.setPreferredSize(new Dimension(250, 0));
        
        // Create scrollable layer list
        JPanel layerListPanel = new JPanel();
        layerListPanel.setLayout(new BoxLayout(layerListPanel, BoxLayout.Y_AXIS));
        
        // Add layer controls for each defined layer (from top to bottom)
        addLayerControl(layerListPanel, "Selection Layer", DiagramLayeredPane.SELECTION_LAYER);
        addLayerControl(layerListPanel, "Connection Layer", DiagramLayeredPane.CONNECTION_LAYER);
        addLayerControl(layerListPanel, "Text Layer", DiagramLayeredPane.TEXT_LAYER);
        addLayerControl(layerListPanel, "Shape Layer", DiagramLayeredPane.SHAPE_LAYER);
        addLayerControl(layerListPanel, "Background Layer", DiagramLayeredPane.BACKGROUND_LAYER);
        addLayerControl(layerListPanel, "Grid Layer", DiagramLayeredPane.GRID_LAYER);
        
        JScrollPane scrollPane = new JScrollPane(layerListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        layerPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Info panel at bottom
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("<html><small>Active layer is highlighted<br>" +
                                     "Click to set active layer<br>" +
                                     "Toggle visibility with eye icon</small></html>");
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        layerPanel.add(infoPanel, BorderLayout.SOUTH);
    }
    
    private void addLayerControl(JPanel parent, String layerName, Integer layerDepth) {
        LayerControlPanel control = new LayerControlPanel(layerName, layerDepth, diagramPane);
        parent.add(control);
        parent.add(Box.createVerticalStrut(2));
    }
    
    private void addShape(ShapeType type) {
        DiagramShape shape = new DiagramShape(type);
        shape.setBounds(100, 100, 120, 80);
        diagramPane.addDiagramComponent(shape, diagramPane.getActiveLayer());
    }
    
    private void addText() {
        DiagramText text = new DiagramText("Text");
        text.setBounds(100, 100, 150, 30);
        diagramPane.addDiagramComponent(text, diagramPane.getActiveLayer());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LayeredDiagramTool().setVisible(true);
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
    @SuppressWarnings("unused")
    private boolean showGrid = true;
    private boolean snapToGrid = true;
    private int gridSize = 20;
    private Component selectedComponent = null;
    private Integer activeLayer = SHAPE_LAYER;
    private Map<Integer, Boolean> layerVisibility = new HashMap<>();
    
    private static final long serialVersionUID = 1L;

    public DiagramLayeredPane() {
        setPreferredSize(new Dimension(2000, 1500));
        setLayout(null); // Required for JLayeredPane
        
        // Initialize layer visibility (all visible by default)
        layerVisibility.put(GRID_LAYER, true);
        layerVisibility.put(BACKGROUND_LAYER, true);
        layerVisibility.put(SHAPE_LAYER, true);
        layerVisibility.put(TEXT_LAYER, true);
        layerVisibility.put(CONNECTION_LAYER, true);
        layerVisibility.put(SELECTION_LAYER, true);
        
        // Add grid panel at the bottom layer
        gridPanel = new GridPanel(gridSize);
        gridPanel.setBounds(0, 0, 2000, 1500);
        add(gridPanel, GRID_LAYER);
        
        setupMouseListeners();
        
        // Enable event dispatching for proper mouse handling
        setFocusable(true);
    }

    /**
     * Save diagram to JSON file
     */
    public void saveDiagram(java.io.File file) throws Exception {
        DiagramData diagram = new DiagramData();
        diagram.setGridSize(gridSize);
        diagram.setSnapToGrid(snapToGrid);
        diagram.setActiveLayer(activeLayer);
        
        // Organize components by layer
        Map<Integer, java.util.List<ComponentData>> layerMap = new HashMap<>();
        
        for (Component comp : getComponents()) {
            if (comp == gridPanel) continue;
            
            Integer layer = getLayer(comp);
            ComponentData data = null;
            
            if (comp instanceof DiagramShape) {
                data = new ShapeData((DiagramShape) comp);
            } else if (comp instanceof DiagramText) {
                data = new TextData((DiagramText) comp);
            }
            
            if (data != null) {
                layerMap.computeIfAbsent(layer, k -> new java.util.ArrayList<>()).add(data);
            }
        }
        
        // Create layer data objects
        java.util.List<LayerData> layers = new java.util.ArrayList<>();
        for (Map.Entry<Integer, java.util.List<ComponentData>> entry : layerMap.entrySet()) {
            LayerData layerData = new LayerData();
            layerData.setLayerDepth(entry.getKey());
            layerData.setVisible(isLayerVisible(entry.getKey()));
            layerData.setComponents(entry.getValue());
            layers.add(layerData);
        }

        diagram.setLayers(layers);

        // Write to JSON file using Jackson
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, diagram);
    }

    /**
     * Load diagram from JSON file
     */
    public void loadDiagram(java.io.File file) throws Exception {
        // Clear existing diagram
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp != gridPanel) {
                remove(comp);
            }
        }

        // Read from JSON file using Jackson
        ObjectMapper mapper = new ObjectMapper();
        DiagramData diagram = mapper.readValue(file, DiagramData.class);

        // Restore diagram settings
        gridSize = diagram.getGridSize();
        snapToGrid = diagram.isSnapToGrid();
        activeLayer = diagram.getActiveLayer();

        // Restore layers and components
        for (LayerData layerData : diagram.getLayers()) {
            Integer layer = layerData.getLayerDepth();
            setLayerVisible(layer, layerData.isVisible());

            for (ComponentData componentData : layerData.getComponents()) {
                JComponent component = null;

                if (componentData instanceof ShapeData) {
                    component = ((ShapeData) componentData).createComponent();
                } else if (componentData instanceof TextData) {
                    component = ((TextData) componentData).createComponent();
                }

                if (component != null) {
                    addDiagramComponent(component, layer);
                }
            }
        }

        revalidate();
        repaint();
    }

    public Integer getActiveLayer() {
        return activeLayer;
    }
    
    public void setActiveLayer(Integer layer) {
        this.activeLayer = layer;
    }
    
    public boolean isLayerVisible(Integer layer) {
        return layerVisibility.getOrDefault(layer, true);
    }
    
    public void setLayerVisible(Integer layer, boolean visible) {
        layerVisibility.put(layer, visible);
        
        // Update visibility of all components in this layer
        for (Component comp : getComponents()) {
            if ( layer.equals(this.getLayer(comp)) ) {
                comp.setVisible(visible);
            }
        }
        
        repaint();
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
                if (e.getButton() == MouseEvent.BUTTON1) {
                    selectComponent(component);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    // Right-click - show popup menu
                    selectComponent(component);
                    showComponentPopupMenu(component, e.getX(), e.getY());
                }
            }
        });
        
        add(component, layer);
        
        // Set visibility based on layer visibility
        component.setVisible(isLayerVisible(layer));
        
        revalidate();
        repaint();
    }
    
    /**
     * Show popup menu for a component
     */
    private void showComponentPopupMenu(JComponent component, int x, int y) {
        JPopupMenu popupMenu = new JPopupMenu();
        
        // Move to Layer submenu
        JMenu moveToLayerMenu = new JMenu("Move to Layer");
        
        Integer currentLayer = getLayer(component);
        
        // Add menu items for each layer
        addLayerMenuItem(moveToLayerMenu, component, SELECTION_LAYER, "Selection Layer", currentLayer);
        addLayerMenuItem(moveToLayerMenu, component, CONNECTION_LAYER, "Connection Layer", currentLayer);
        addLayerMenuItem(moveToLayerMenu, component, TEXT_LAYER, "Text Layer", currentLayer);
        addLayerMenuItem(moveToLayerMenu, component, SHAPE_LAYER, "Shape Layer", currentLayer);
        addLayerMenuItem(moveToLayerMenu, component, BACKGROUND_LAYER, "Background Layer", currentLayer);
        
        popupMenu.add(moveToLayerMenu);
        
        popupMenu.addSeparator();
        
        // Bring Forward / Send Back
        JMenuItem bringForwardItem = new JMenuItem("Bring Forward");
        bringForwardItem.addActionListener(e -> {
            setLayer(component, currentLayer + 1);
            repaint();
        });
        popupMenu.add(bringForwardItem);
        
        JMenuItem sendBackItem = new JMenuItem("Send Back");
        sendBackItem.addActionListener(e -> {
            if (currentLayer > GRID_LAYER + 1) {
                setLayer(component, currentLayer - 1);
                repaint();
            }
        });
        sendBackItem.setEnabled(currentLayer > GRID_LAYER + 1);
        popupMenu.add(sendBackItem);
        
        popupMenu.addSeparator();
        
        // Change Color (for shapes only)
        if (component instanceof DiagramShape) {
            JMenuItem changeFillColorItem = new JMenuItem("Change Fill Color...");
            changeFillColorItem.addActionListener(e -> {
                DiagramShape shape = (DiagramShape) component;
                Color newColor = JColorChooser.showDialog(this, "Choose Fill Color", shape.getFillColor());
                if (newColor != null) {
                    shape.setFillColor(newColor);
                }
            });
            popupMenu.add(changeFillColorItem);
            
            JMenuItem changeBorderColorItem = new JMenuItem("Change Border Color...");
            changeBorderColorItem.addActionListener(e -> {
                DiagramShape shape = (DiagramShape) component;
                Color newColor = JColorChooser.showDialog(this, "Choose Border Color", shape.getBorderColor());
                if (newColor != null) {
                    shape.setBorderColor(newColor);
                }
            });
            popupMenu.add(changeBorderColorItem);
            
            popupMenu.addSeparator();
        }
        
        // Delete
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> {
            remove(component);
            if (selectedComponent == component) {
                selectedComponent = null;
            }
            revalidate();
            repaint();
        });
        popupMenu.add(deleteItem);
        
        // Show popup menu at component location
        popupMenu.show(component, x, y);
    }

    /**
     * Add a layer menu item with current layer indicator
     */
    private void addLayerMenuItem(JMenu menu, JComponent component, Integer targetLayer, String layerName,
            Integer currentLayer) {
        JMenuItem item = new JMenuItem(layerName);

        // Add checkmark if this is the current layer
        if (targetLayer.equals(currentLayer)) {
            item.setText("✓ " + layerName);
            item.setEnabled(false);
        }

        item.addActionListener(e -> {
            setLayer(component, targetLayer);
            component.setVisible(isLayerVisible(targetLayer));
            repaint();
        });

        menu.add(item);
    }

    private void selectComponent(Component comp) {
        if (comp == selectedComponent)
            return;

        deselectAll();
        selectedComponent = comp;
        
        // Visual feedback with resize border
        if (comp instanceof JComponent) {
            JComponent jcomp = (JComponent) comp;
            jcomp.setBorder(new ResizeBorder(jcomp));
            // ResizeBorder resizeBorder = new ResizeBorder(jcomp); //, this);

            // Add resize handler
            ResizeHandler resizeHandler = new ResizeHandler(jcomp, this);
            jcomp.addMouseListener(resizeHandler);
            jcomp.addMouseMotionListener(resizeHandler);
        }
    }
    
    private void deselectAll() {
        if (selectedComponent instanceof JComponent) {
            JComponent jcomp = (JComponent) selectedComponent;
            
            // Remove resize handlers
            MouseListener[] mouseListeners = jcomp.getMouseListeners();
            for (MouseListener ml : mouseListeners) {
                if (ml instanceof ResizeHandler) {
                    jcomp.removeMouseListener(ml);
                }
            }
            
            MouseMotionListener[] motionListeners = jcomp.getMouseMotionListeners();
            for (MouseMotionListener mml : motionListeners) {
                if (mml instanceof ResizeHandler) {
                    jcomp.removeMouseMotionListener(mml);
                }
            }
            
            jcomp.setBorder(null);
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
        
        private static final long serialVersionUID = 1L;

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
 * Drag handler that works with JLayeredPane and grid snapping (fixes jitter)
 */
class DragHandler extends MouseAdapter {

    private Point pressPoint;
//    private Point snappedStartLocation;
    private boolean dragging = false;
    private DiagramLayeredPane layeredPane;

    public DragHandler(DiagramLayeredPane layeredPane) {
        this.layeredPane = layeredPane;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragging = true;
        pressPoint = e.getPoint();
        Component comp = e.getComponent();
        comp.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        // Store the starting location and snap it once at the beginning
        // This was original AI code; replacing with code from AdvancedLayoutDemo
//        Point startLocation = comp.getLocation();
//        if (layeredPane.isSnapToGrid()) {
//            snappedStartLocation = new Point(
//                layeredPane.snapToGrid(startLocation.x),
//                layeredPane.snapToGrid(startLocation.y)
//            );
//        } else {
//            snappedStartLocation = new Point(startLocation);
//        }

    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) return;

        // System.out.println("DRAGGING...");

        Point currentPoint = e.getPoint();
        Component comp = e.getComponent();
        Rectangle currentBounds = comp.getBounds();

        // Calculate offset from press point
        final int dx = currentPoint.x - pressPoint.x;
        final int dy = currentPoint.y - pressPoint.y;
        
//        // Apply offset to snapped start location
//        int newX = snappedStartLocation.x + dx;
//        int newY = snappedStartLocation.y + dy;
//        
//        // Only snap the final position, not during drag
//        if (layeredPane.isSnapToGrid()) {
//            newX = layeredPane.snapToGrid(newX);
//            newY = layeredPane.snapToGrid(newY);
//        }
//
//        comp.setLocation(newX, newY);
        int newX = currentBounds.x + dx; // startBounds
        int newY = currentBounds.y + dy;
        if (layeredPane.isSnapToGrid()) {
            newX = layeredPane.snapToGrid(newX);
            newY = layeredPane.snapToGrid(newY);
        }

        Rectangle newBounds = new Rectangle(
            newX, //currentBounds.x + dx,
            newY, //currentBounds.y + dy,
            currentBounds.width,
            currentBounds.height
        );
        comp.setBounds(newBounds);

        layeredPane.revalidate(); // this might prove bad as it is added by me
        layeredPane.repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;

        Component comp = e.getComponent();
        comp.setCursor(Cursor.getDefaultCursor());

        // Final snap on release
//        if (layeredPane.isSnapToGrid()) {
//            Point loc = comp.getLocation();
//            comp.setLocation(layeredPane.snapToGrid(loc.x), 
//                           layeredPane.snapToGrid(loc.y));
//        }
//        

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
    
    private static final long serialVersionUID = 1L;

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

    public Enum<ShapeType> getShapeType() {
        return this.type;
    }

    public Color getFillColor() {
        return this.fillColor;
    }

    public Color getBorderColor() {
        return this.borderColor;
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
    }

}

/**
 * Text component for diagram (now draggable and resizable)
 */
class DiagramText extends JPanel {

    private JTextField textField;
//    private ResizeHandler resizeHandler;
    
    private static final long serialVersionUID = 1L;

    private static final Font DEFAULT_FONT = new Font("Segoe UI", Font.BOLD, 16);

    @SuppressWarnings("unused")
    private static final Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(5, 5, 5, 5);

    public DiagramText(String initialText) {
        setLayout(new BorderLayout());
        setBackground(new Color(255, 255, 255, 200));
        
        textField = new JTextField(initialText);
        textField.setFont(DEFAULT_FONT);
        // textField.setBorder(DEFAULT_BORDER);
        textField.setOpaque(false);
        textField.setHorizontalAlignment(JTextField.CENTER);

        // Prevent text field from consuming mouse events needed for dragging
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Allow dragging when clicking outside the text editing area
                if (e.getClickCount() == 1) {
                    DiagramText.this.dispatchEvent(SwingUtilities.convertMouseEvent(textField, e, DiagramText.this));
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                DiagramText.this.dispatchEvent(SwingUtilities.convertMouseEvent(textField, e, DiagramText.this));
            }
        });
        
        textField.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                DiagramText.this.dispatchEvent(SwingUtilities.convertMouseEvent(textField, e, DiagramText.this));
            }
        });
        
        add(textField, BorderLayout.CENTER);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Draw border when selected
        if (getBorder() instanceof ResizeBorder) {
            g2d.setColor(new Color(100, 150, 255, 50));
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
    
    private static final long serialVersionUID = 1L;

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

/**
 * Visual control panel for each layer
 */
class LayerControlPanel extends JPanel {

    private JLabel nameLabel;
    private JLabel countLabel;
    private JToggleButton visibilityButton;

    private DiagramLayeredPane diagramPane;

    @SuppressWarnings("unused")
    private String layerName;
    private Integer layerDepth;
    private boolean isActive = false;

    private static final long serialVersionUID = 1L;

    public LayerControlPanel(String layerName, Integer layerDepth, DiagramLayeredPane diagramPane) {
        this.layerName = layerName;
        this.layerDepth = layerDepth;
        this.diagramPane = diagramPane;

        setLayout(new BorderLayout(5, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // 40
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Set initial active state
        isActive = layerDepth.equals(diagramPane.getActiveLayer());
        updateBackground();

        // Visibility toggle button (eye icon)
        visibilityButton = new JToggleButton();
        visibilityButton.setSelected(diagramPane.isLayerVisible(layerDepth));
        visibilityButton.setPreferredSize(new Dimension(30, 30)); // 30
        visibilityButton.setFocusPainted(false);
        updateVisibilityIcon();

        visibilityButton.addActionListener(e -> {
            boolean visible = visibilityButton.isSelected();
            diagramPane.setLayerVisible(layerDepth, visible);
            updateVisibilityIcon();
        });

        add(visibilityButton, BorderLayout.WEST);

        // Layer info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);

        nameLabel = new JLabel(layerName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        infoPanel.add(nameLabel, BorderLayout.NORTH);

        countLabel = new JLabel("0 items");
        countLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        countLabel.setForeground(Color.GRAY);
        infoPanel.add(countLabel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.CENTER);
        
        // Layer depth indicator
        JLabel depthLabel = new JLabel("" + layerDepth);
        depthLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        depthLabel.setForeground(Color.GRAY);
        add(depthLabel, BorderLayout.EAST);
        
        // Click handler to set active layer
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && 
                    !visibilityButton.contains(e.getPoint())) {
                    setActiveLayer();
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isActive) {
                    setBackground(new Color(240, 240, 255));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                updateBackground();
            }
        });
        
        // Update count periodically
        Timer timer = new Timer(1000, e -> updateCount());
        timer.start();
    }
    
    private void setActiveLayer() {
        diagramPane.setActiveLayer(layerDepth);
        
        // Update all layer panels
        Container parent = getParent();
        if (parent != null) {
            for (Component comp : parent.getComponents()) {
                if (comp instanceof LayerControlPanel) {
                    LayerControlPanel panel = (LayerControlPanel) comp;
                    panel.setActive(panel.layerDepth.equals(layerDepth));
                }
            }
        }
    }
    
    private void setActive(boolean active) {
        this.isActive = active;
        updateBackground();
        nameLabel.setForeground(active ? new Color(0, 100, 200) : Color.BLACK);
    }
    
    private void updateBackground() {
        if (isActive) {
            setBackground(new Color(200, 220, 255));
        } else {
            setBackground(Color.WHITE);
        }
    }
    
    private void updateVisibilityIcon() {
        if (visibilityButton.isSelected()) {
            visibilityButton.setText("👁");
            visibilityButton.setToolTipText("Layer visible - click to hide");
        } else {
            visibilityButton.setText("⊗");
            visibilityButton.setToolTipText("Layer hidden - click to show");
        }
    }
    
    private void updateCount() {
        Map<Integer, Integer> counts = diagramPane.getLayerCounts();
        int count = counts.getOrDefault(layerDepth, 0);
        countLabel.setText(count + (count == 1 ? " item" : " items"));
    }
}

/**
 * Custom border that shows resize handles
 */
class ResizeBorder extends AbstractBorder {

    @SuppressWarnings("unused")
    private JComponent component;

    private static final int HANDLE_SIZE = 8;

    private static final Insets INSETS = new Insets(3, 3, 3, 3);

    private static final long serialVersionUID = 1L;

    public ResizeBorder(JComponent component) {
        this.component = component;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;

        // Draw border
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(x + 1, y + 1, width - 2, height - 2);

        // Draw resize handles
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // NW
        g2d.fillRect(x + width - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // NE
        g2d.fillRect(x - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // SW
        g2d.fillRect(x + width - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // SE
        g2d.fillRect(x + width/2 - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // N
        g2d.fillRect(x + width/2 - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // S
        g2d.fillRect(x - HANDLE_SIZE/2, y + height/2 - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // W
        g2d.fillRect(x + width - HANDLE_SIZE/2, y + height/2 - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE); // E

        // Draw handle borders
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(x - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width/2 - HANDLE_SIZE/2, y - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width/2 - HANDLE_SIZE/2, y + height - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x - HANDLE_SIZE/2, y + height/2 - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
        g2d.drawRect(x + width - HANDLE_SIZE/2, y + height/2 - HANDLE_SIZE/2, HANDLE_SIZE, HANDLE_SIZE);
    }
    
    @Override
    public Insets getBorderInsets(Component c) {
        return INSETS;
    }
}

/**
 * Handles resizing of components with resize handles
 */
class ResizeHandler extends MouseAdapter {

    private Point pressPoint;
    private Rectangle startBounds;
    private JComponent component;
    private ResizeDirection resizeDirection;
    private DiagramLayeredPane layeredPane;

    private boolean isResizing = false;

    private enum ResizeDirection {
        NONE, NW, N, NE, E, SE, S, SW, W
    }

    public ResizeHandler(JComponent component, DiagramLayeredPane layeredPane) {
        this.component = component;
        this.layeredPane = layeredPane;
        // System.out.print("new... ");
    }

    @Override
    public void mousePressed(MouseEvent e) {
        pressPoint = e.getPoint();
        startBounds = component.getBounds();
        resizeDirection = getResizeDirection(e.getPoint());
        // System.out.println("Handle: " + resizeDirection);

        if (resizeDirection != ResizeDirection.NONE) {
            // System.out.println("Resizing");
            isResizing = true;
            // e.consume();
        }
    }

    private void turnOffResizing() {
        isResizing = false;
        // System.out.println("off");
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // System.out.print("dragging... ");
        if (!isResizing)
            return;

        // System.out.println("Y");

        int dx = e.getX() - pressPoint.x;
        int dy = e.getY() - pressPoint.y;

        // Rectangle currentBounds = component.getBounds();
        // System.out.print("Drag, width start: ");
        // System.out.print(currentBounds.width);
        // System.out.print(", dx: ");
        // System.out.println(dx);

        Rectangle newBounds = calculateNewBounds(resizeDirection, startBounds, dx, dy);
        // Apply snap to grid
        if (layeredPane.isSnapToGrid()) {
            newBounds.x = layeredPane.snapToGrid(newBounds.x);
            newBounds.y = layeredPane.snapToGrid(newBounds.y);
            newBounds.width = layeredPane.snapToGrid(newBounds.width);
            newBounds.height = layeredPane.snapToGrid(newBounds.height);
        }

        // Enforce minimum size
        newBounds.width = Math.max(newBounds.width, 30);
        newBounds.height = Math.max(newBounds.height, 30);

        component.setBounds(newBounds);

        // layeredPane.revalidate(); probably not necessary
        layeredPane.repaint();
        e.consume();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        turnOffResizing(); // isResizing = false;
        resizeDirection = ResizeDirection.NONE;
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        ResizeDirection direction = getResizeDirection(e.getPoint());
        updateCursor(direction);
    }
    
    private ResizeDirection getResizeDirection(Point p) {
        int width = component.getWidth();
        int height = component.getHeight();
        int handleSize = 8;
        
        boolean onLeft = p.x <= handleSize;
        boolean onRight = p.x >= width - handleSize;
        boolean onTop = p.y <= handleSize;
        boolean onBottom = p.y >= height - handleSize;
        
        if (onTop && onLeft) return ResizeDirection.NW;
        if (onTop && onRight) return ResizeDirection.NE;
        if (onBottom && onLeft) return ResizeDirection.SW;
        if (onBottom && onRight) return ResizeDirection.SE;
        if (onTop) return ResizeDirection.N;
        if (onBottom) return ResizeDirection.S;
        if (onLeft) return ResizeDirection.W;
        if (onRight) return ResizeDirection.E;
        
        return ResizeDirection.NONE;
    }
    
    private void updateCursor(ResizeDirection direction) {
        Cursor cursor;
        
        switch (direction) {
            case NW: case SE: cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR); break;
            case NE: case SW: cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR); break;
            case N: case S: cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR); break;
            case E: case W: cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR); break;
            default: cursor = Cursor.getDefaultCursor(); break;
        }
        
        component.setCursor(cursor);
    }
    
    private Rectangle calculateNewBounds(ResizeDirection direction, Rectangle start, int dx, int dy) {
        Rectangle bounds = new Rectangle(start);
        
        switch (direction) {
            case NW:
                bounds.x += dx;
                bounds.y += dy;
                bounds.width -= dx;
                bounds.height -= dy;
                break;
            case N:
                bounds.y += dy;
                bounds.height -= dy;
                break;
            case NE:
                bounds.y += dy;
                bounds.width += dx;
                bounds.height -= dy;
                break;
            case E:
                bounds.width += dx;
                break;
            case SE:
                bounds.width += dx;
                bounds.height += dy;
                break;
            case S:
                bounds.height += dy;
                break;
            case SW:
                bounds.x += dx;
                bounds.width -= dx;
                bounds.height += dy;
                break;
            case W:
                bounds.x += dx;
                bounds.width -= dx;
                break;
        default:
            break;
        }
        
        return bounds;
    }

}

//============================================================================
//JSON Data Classes for Serialization
//============================================================================

/**
* Root diagram data structure
*/
class DiagramData {
 private int gridSize;
 private boolean snapToGrid;
 private int activeLayer;
 private java.util.List<LayerData> layers;
 
 public int getGridSize() { return gridSize; }
 public void setGridSize(int gridSize) { this.gridSize = gridSize; }
 
 public boolean isSnapToGrid() { return snapToGrid; }
 public void setSnapToGrid(boolean snapToGrid) { this.snapToGrid = snapToGrid; }
 
 public int getActiveLayer() { return activeLayer; }
 public void setActiveLayer(int activeLayer) { this.activeLayer = activeLayer; }
 
 public java.util.List<LayerData> getLayers() { return layers; }
 public void setLayers(java.util.List<LayerData> layers) { this.layers = layers; }
}

/**
* Layer data structure
*/
class LayerData {
 private int layerDepth;
 private boolean visible;
 private java.util.List<ComponentData> components;
 
 public int getLayerDepth() { return layerDepth; }
 public void setLayerDepth(int layerDepth) { this.layerDepth = layerDepth; }
 
 public boolean isVisible() { return visible; }
 public void setVisible(boolean visible) { this.visible = visible; }
 
 public java.util.List<ComponentData> getComponents() { return components; }
 public void setComponents(java.util.List<ComponentData> components) { this.components = components; }
}

/**
* Base class for component data with polymorphic JSON serialization
*/
@JsonTypeInfo(
 use = JsonTypeInfo.Id.NAME,
 include = JsonTypeInfo.As.PROPERTY,
 property = "type"
)
@JsonSubTypes({
 @JsonSubTypes.Type(value = ShapeData.class, name = "shape"),
 @JsonSubTypes.Type(value = TextData.class, name = "text")
})
abstract class ComponentData {
    private int x;
    private int y;
    private int width;
    private int height;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public abstract JComponent createComponent();

    protected void setBounds(Rectangle bounds) {
        this.x = bounds.x;
        this.y = bounds.y;
        this.width = bounds.width;
        this.height = bounds.height;
    }

    protected Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    protected String colorToHex(Color color) {
        return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    protected Color hexToColor(String hex) {
        if (hex.length() == 9) { // #RRGGBBAA
            return new Color(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16),
                    Integer.parseInt(hex.substring(5, 7), 16), Integer.parseInt(hex.substring(7, 9), 16));
        } else { // #RRGGBB
            return Color.decode(hex);
        }
    }

}

/**
* Shape component data
*/
class ShapeData extends ComponentData {

    private String shapeType;
    private String fillColor;
    private String borderColor;

    // Default constructor required for Jackson
    public ShapeData() {
    }

    public ShapeData(DiagramShape shape) {
        setBounds(shape.getBounds());
        this.shapeType = shape.getShapeType().name();
        this.fillColor = colorToHex(shape.getFillColor());
        this.borderColor = colorToHex(shape.getBorderColor());
    }

    public String getShapeType() {
        return shapeType;
    }

    public void setShapeType(String shapeType) {
        this.shapeType = shapeType;
    }

    public String getFillColor() {
        return fillColor;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    public String getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(String borderColor) {
        this.borderColor = borderColor;
    }

    @Override
    public JComponent createComponent() {
        DiagramShape shape = new DiagramShape(ShapeType.valueOf(shapeType));
        shape.setBounds(getBounds());
        shape.setFillColor(hexToColor(fillColor));
        shape.setBorderColor(hexToColor(borderColor));
        return shape;
    }

}

/**
* Text component data
*/
class TextData extends ComponentData {

    private String text;
    private String backgroundColor;

    // Default constructor required for Jackson
    public TextData() {
    }

    public TextData(DiagramText textComp) {
        setBounds(textComp.getBounds());
        this.text = textComp.getText();
        this.backgroundColor = colorToHex(textComp.getBackground());
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public JComponent createComponent() {
        DiagramText textComp = new DiagramText(text);
        textComp.setBounds(getBounds());
        textComp.setBackground(hexToColor(backgroundColor));
        return textComp;
    }

}
