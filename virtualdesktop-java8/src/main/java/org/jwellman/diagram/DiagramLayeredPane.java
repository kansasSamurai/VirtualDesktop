package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.api.CanvasTheme;
import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.EdgeRouter;
import org.jwellman.diagram.api.GraphEdge;
import org.jwellman.diagram.api.GraphNode;
import org.jwellman.diagram.core.BlueprintCanvasTheme;
import org.jwellman.diagram.core.WhiteprintCanvasTheme;
import org.jwellman.diagram.core.CanvasOverlayPanel;
import org.jwellman.diagram.core.DefaultGraphEdge;
import org.jwellman.diagram.core.EdgeRenderPanel;
import org.jwellman.diagram.core.NodeHostPanel;
import org.jwellman.diagram.core.OrthogonalRouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Custom JLayeredPane with grid, layer management, and graph model support.
 */
public class DiagramLayeredPane extends JLayeredPane {

    // Define layer constants
    public static final Integer GRID_LAYER       = new Integer(0);
    public static final Integer BACKGROUND_LAYER = new Integer(100);
    public static final Integer SHADOW_LAYER     = new Integer(150);
    public static final Integer SHAPE_LAYER      = new Integer(200);
    public static final Integer TEXT_LAYER       = new Integer(300);
    public static final Integer CONNECTION_LAYER = new Integer(400);
    public static final Integer OVERLAY_LAYER    = new Integer(500);

    private GridPanel gridPanel;
    @SuppressWarnings("unused")
    private boolean showGrid = true;
    private boolean snapToGrid = true;
    private int gridSize = 20;
    private Component selectedComponent = null;
    private Integer activeLayer = SHAPE_LAYER;
    private Map<Integer, Boolean> layerVisibility = new HashMap<>();
    private Runnable modificationListener;
    private java.util.function.Consumer<Component> selectionListener;

    // Infrastructure panels
    private ShadowLayerPanel shadowPanel;
    private boolean shadowsEnabled = true;

    // Canvas theme — controls all colors on the canvas surface and its nodes
    private CanvasTheme theme = new WhiteprintCanvasTheme();

    // Graph model
    private EdgeRenderPanel edgePanel;
    private CanvasOverlayPanel overlayPanel;
    private Map<String, GraphNode> graphNodes = new LinkedHashMap<>();
    private EdgeRouter edgeRouter = new OrthogonalRouter();
    private CanvasComponentFactory componentFactory;

    private static final long serialVersionUID = 1L;

    public DiagramLayeredPane() {
        setPreferredSize(new Dimension(2000, 1500));
        setLayout(null); // Required for JLayeredPane

        // Canvas background is owned by the pane itself so it persists when the grid is hidden
        setOpaque(true);
        setBackground(theme.getCanvasBackground());

        // Initialize layer visibility (all visible by default)
        layerVisibility.put(GRID_LAYER, true);
        layerVisibility.put(BACKGROUND_LAYER, true);
        layerVisibility.put(SHAPE_LAYER, true);
        layerVisibility.put(TEXT_LAYER, true);
        layerVisibility.put(CONNECTION_LAYER, true);
        layerVisibility.put(OVERLAY_LAYER, true);

        // Add grid panel at the bottom layer — paints grid lines only; background is on the pane itself
        gridPanel = new GridPanel(gridSize, theme.getGridLineColor());
        gridPanel.setBounds(0, 0, 2000, 1500);
        add(gridPanel, GRID_LAYER);

        // Shadow layer — paints drop shadows behind graph nodes; suspended during drag
        shadowPanel = new ShadowLayerPanel();
        shadowPanel.setBounds(0, 0, 2000, 1500);
        add(shadowPanel, SHADOW_LAYER);

        // Edge render panel — transparent, passes mouse events through
        edgePanel = new EdgeRenderPanel(edgeRouter, graphNodes, theme);
        edgePanel.setBounds(0, 0, 2000, 1500);
        add(edgePanel, CONNECTION_LAYER);

        // Canvas overlay — handles port anchors, edge-creation drag, and selection handles
        overlayPanel = new CanvasOverlayPanel(
            graphNodes, edgePanel,
            edge -> addGraphEdge(edge),
            coord -> isSnapToGrid() ? snapToGrid(coord) : coord,
            () -> notifyModified()
        );
        overlayPanel.setBounds(0, 0, 2000, 1500);
        add(overlayPanel, OVERLAY_LAYER);

        setupMouseListeners();

        // Enable event dispatching for proper mouse handling
        setFocusable(true);
    }

    // ---------------------------------------------------------------
    // Graph model — public API
    // ---------------------------------------------------------------

    public void addGraphNode(NodeHostPanel node, Integer layer) {
        installInteractionHandlers(node);
        graphNodes.put(node.getNodeId(), node);
        add(node, layer, 0);  // position 0 = top of layer
        node.setVisible(isLayerVisible(layer));
        revalidate();
        repaint();
        notifyModified();
    }

    public void addGraphEdge(GraphEdge edge) {
        edgePanel.addEdge(edge);
        notifyModified();
    }

    public void removeGraphNode(String nodeId) {
        GraphNode node = graphNodes.remove(nodeId);
        if (node != null) {
            remove(node.getVisualComponent());
            revalidate();
            repaint();
            notifyModified();
        }
    }

    public void removeGraphEdge(String edgeId) {
        edgePanel.removeEdge(edgeId);
        notifyModified();
    }

    public void setComponentFactory(CanvasComponentFactory factory) {
        this.componentFactory = factory;
    }

    public void enterEdgeCreationMode() {
        overlayPanel.enterEdgeCreationMode();
    }

    public void exitEdgeCreationMode() {
        overlayPanel.exitEdgeCreationMode();
    }

    /** Called by DragHandler after a GraphNode's bounds change. */
    void notifyNodeMoved(String nodeId) {
        edgePanel.nodeUpdated(nodeId);
        shadowPanel.repaint();
    }

    public void setShadowsEnabled(boolean enabled) {
        shadowsEnabled = enabled;
        shadowPanel.setVisible(enabled);
    }

    /** Hides shadows for the duration of a drag; preserves the user's enabled preference. */
    void suspendShadows() {
        shadowPanel.setVisible(false);
    }

    /** Restores shadow visibility to the user's current preference after drag ends. */
    void resumeShadows() {
        shadowPanel.setVisible(shadowsEnabled);
    }

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    public void saveDiagram(java.io.File file) throws Exception {
        DiagramData diagram = new DiagramData();
        diagram.setVersion(FileVersion.current());
        diagram.setGridSize(gridSize);
        diagram.setSnapToGrid(snapToGrid);
        diagram.setActiveLayer(activeLayer);

        // Organize decorative components by layer
        Map<Integer, java.util.List<ComponentData>> layerMap = new HashMap<>();

        for (Component comp : getComponents()) {
            if (comp == gridPanel || comp == shadowPanel || comp == edgePanel || comp == overlayPanel) {
                continue;
            }
            if (comp instanceof GraphNode) {
                continue; // handled separately below
            }

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

        java.util.List<LayerData> layers = new java.util.ArrayList<>();
        for (Map.Entry<Integer, java.util.List<ComponentData>> entry : layerMap.entrySet()) {
            LayerData layerData = new LayerData();
            layerData.setLayerDepth(entry.getKey());
            layerData.setVisible(isLayerVisible(entry.getKey()));
            layerData.setComponents(entry.getValue());
            layers.add(layerData);
        }
        diagram.setLayers(layers);

        // Save semantic graph if non-empty
        if (!graphNodes.isEmpty()) {
            SemanticGraphData graphData = new SemanticGraphData();

            for (GraphNode node : graphNodes.values()) {
                NodeHostPanel nhp = (NodeHostPanel) node;
                GraphNodeData nd = new GraphNodeData();
                nd.setId(nhp.getNodeId());
                nd.setType(nhp.getNodeType());
                nd.setProperties(nhp.getProperties());
                nd.setX(nhp.getX());
                nd.setY(nhp.getY());
                nd.setW(nhp.getWidth());
                nd.setH(nhp.getHeight());
                nd.setLayer(getLayer(nhp));
                graphData.getNodes().add(nd);
            }

            for (GraphEdge edge : edgePanel.getEdges()) {
                GraphEdgeData ed = new GraphEdgeData();
                ed.setId(edge.getEdgeId());
                ed.setSourceNodeId(edge.getSourceNodeId());
                ed.setSourcePortId(edge.getSourcePortId());
                ed.setTargetNodeId(edge.getTargetNodeId());
                ed.setTargetPortId(edge.getTargetPortId());
                EdgeAttributes attrs = edge.getAttributes();
                ed.setLineStyle(attrs.getLineStyle().name());
                ed.setArrowType(attrs.getArrowType().name());
                graphData.getEdges().add(ed);
            }

            diagram.setSemanticGraph(graphData);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(file, diagram);
    }

    public void loadDiagram(java.io.File file) throws Exception {
        // Clear existing diagram
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp != gridPanel && comp != shadowPanel && comp != edgePanel && comp != overlayPanel) {
                remove(comp);
            }
        }
        graphNodes.clear();

        ObjectMapper mapper = new ObjectMapper();
        DiagramData diagram = mapper.readValue(file, DiagramData.class);

        validateFormat(diagram.getVersion());

        gridSize = diagram.getGridSize();
        snapToGrid = diagram.isSnapToGrid();
        activeLayer = diagram.getActiveLayer();

        // Restore decorative layers
        if (diagram.getLayers() != null) {
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
        }

        // Restore semantic graph (only if a factory is configured)
        SemanticGraphData graphData = diagram.getSemanticGraph();
        if (graphData != null && componentFactory != null) {
            for (GraphNodeData nd : graphData.getNodes()) {
                javax.swing.JPanel content = componentFactory.createContentFor(
                    nd.getType(), nd.getProperties());
                NodeHostPanel nhp = new NodeHostPanel(
                    nd.getId(), nd.getType(), nd.getProperties(), content);
                nhp.setBounds(nd.getX(), nd.getY(), nd.getW(), nd.getH());
                addGraphNode(nhp, nd.getLayer());
            }
            for (GraphEdgeData ed : graphData.getEdges()) {
                EdgeAttributes attrs = new EdgeAttributes();
                if (ed.getLineStyle() != null) {
                    attrs.setLineStyle(EdgeAttributes.LineStyle.valueOf(ed.getLineStyle()));
                }
                if (ed.getArrowType() != null) {
                    attrs.setArrowType(EdgeAttributes.ArrowType.valueOf(ed.getArrowType()));
                }
                addGraphEdge(new DefaultGraphEdge(
                    ed.getId(),
                    ed.getSourceNodeId(), ed.getSourcePortId(),
                    ed.getTargetNodeId(), ed.getTargetPortId(),
                    attrs));
            }
        }

        revalidate();
        repaint();
    }

    private void validateFormat(FileVersion version) throws UnsupportedFormatException {
        int major = (version != null) ? version.getMajor() : 0;
        if (major > FileVersion.CURRENT_MAJOR) {
            int minor = (version != null) ? version.getMinor() : 0;
            throw new UnsupportedFormatException(
                "File format version " + major + "." + minor
                + " is too new for this build (supports up to "
                + FileVersion.CURRENT_MAJOR + ".x).");
        }
    }

    // ---------------------------------------------------------------
    // Layer management
    // ---------------------------------------------------------------

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

        for (Component comp : getComponents()) {
            if (layer.equals(this.getLayer(comp))) {
                comp.setVisible(visible);
            }
        }

        repaint();
        notifyModified();
    }

    public void setModificationListener(Runnable listener) {
        this.modificationListener = listener;
    }

    public void setSelectionListener(java.util.function.Consumer<Component> listener) {
        this.selectionListener = listener;
    }

    public void notifyModified() {
        if (modificationListener != null) {
            modificationListener.run();
        }
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            selectionListener.accept(selectedComponent);
        }
    }

    private void setupMouseListeners() {
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

    // ---------------------------------------------------------------
    // Component management (decorative elements)
    // ---------------------------------------------------------------

    public void addDiagramComponent(JComponent component, Integer layer) {
        installInteractionHandlers(component);
        add(component, layer, 0);  // position 0 = top of layer, so new shapes appear in front
        component.setVisible(isLayerVisible(layer));
        revalidate();
        repaint();
        notifyModified();
    }

    private void installInteractionHandlers(final JComponent component) {
        // Drag
        DragHandler dragHandler = new DragHandler(this);
        component.addMouseListener(dragHandler);
        component.addMouseMotionListener(dragHandler);

        // Selection + context menu
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    selectComponent(component);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    selectComponent(component);
                    // Only show context menu for decorative elements
                    if (!(component instanceof GraphNode)) {
                        showComponentPopupMenu(component, e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void showComponentPopupMenu(JComponent component, int x, int y) {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenu moveToLayerMenu = new JMenu("Move to Layer");
        Integer currentLayer = getLayer(component);

        addLayerMenuItem(moveToLayerMenu, component, OVERLAY_LAYER,    "Overlay Layer",    currentLayer);
        addLayerMenuItem(moveToLayerMenu, component, CONNECTION_LAYER, "Connection Layer", currentLayer);
        addLayerMenuItem(moveToLayerMenu, component, TEXT_LAYER,       "Text Layer",       currentLayer);
        addLayerMenuItem(moveToLayerMenu, component, SHAPE_LAYER,      "Shape Layer",      currentLayer);
        addLayerMenuItem(moveToLayerMenu, component, BACKGROUND_LAYER, "Background Layer", currentLayer);
        popupMenu.add(moveToLayerMenu);

        popupMenu.addSeparator();

        JMenuItem bringForwardItem = new JMenuItem("Bring Forward");
        bringForwardItem.addActionListener(e -> {
            setLayer(component, currentLayer + 1);
            repaint();
            notifyModified();
        });
        popupMenu.add(bringForwardItem);

        JMenuItem sendBackItem = new JMenuItem("Send Back");
        sendBackItem.addActionListener(e -> {
            if (currentLayer > GRID_LAYER + 1) {
                setLayer(component, currentLayer - 1);
                repaint();
                notifyModified();
            }
        });
        sendBackItem.setEnabled(currentLayer > GRID_LAYER + 1);
        popupMenu.add(sendBackItem);

        popupMenu.addSeparator();

        if (component instanceof DiagramColorable) {
            JMenuItem changeFillColorItem = new JMenuItem("Change Fill Color...");
            changeFillColorItem.addActionListener(e -> {
                DiagramColorable colorable = (DiagramColorable) component;
                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                Color newColor = JColorChooser.showDialog(parentWindow, "Choose Fill Color",
                    colorable.getFillColor());
                if (newColor != null) {
                    colorable.setFillColor(newColor);
                    notifyModified();
                }
            });
            popupMenu.add(changeFillColorItem);

            JMenuItem changeBorderColorItem = new JMenuItem("Change Border Color...");
            changeBorderColorItem.addActionListener(e -> {
                DiagramColorable colorable = (DiagramColorable) component;
                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                Color newColor = JColorChooser.showDialog(parentWindow, "Choose Border Color",
                    colorable.getBorderColor());
                if (newColor != null) {
                    colorable.setBorderColor(newColor);
                    notifyModified();
                }
            });
            popupMenu.add(changeBorderColorItem);

            popupMenu.addSeparator();
        }

        if (component instanceof DiagramTextAware) {
            JMenuItem changeTextColorItem = new JMenuItem("Change Text Color...");
            changeTextColorItem.addActionListener(e -> {
                DiagramTextAware textAware = (DiagramTextAware) component;
                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                Color newColor = JColorChooser.showDialog(parentWindow, "Choose Text Color",
                    textAware.getTextColor());
                if (newColor != null) {
                    textAware.setTextColor(newColor);
                    notifyModified();
                }
            });
            popupMenu.add(changeTextColorItem);

            popupMenu.addSeparator();
        }

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> {
            remove(component);
            if (selectedComponent == component) {
                selectedComponent = null;
            }
            revalidate();
            repaint();
            notifyModified();
        });
        popupMenu.add(deleteItem);

        popupMenu.show(component, x, y);
    }

    private void addLayerMenuItem(JMenu menu, JComponent component, Integer targetLayer,
            String layerName, Integer currentLayer) {
        JMenuItem item = new JMenuItem(layerName);
        if (targetLayer.equals(currentLayer)) {
            item.setText("✓ " + layerName);
            item.setEnabled(false);
        }
        item.addActionListener(e -> {
            setLayer(component, targetLayer);
            component.setVisible(isLayerVisible(targetLayer));
            repaint();
            notifyModified();
        });
        menu.add(item);
    }

    private void selectComponent(Component comp) {
        if (comp == selectedComponent) {
            return;
        }
        deselectAll();
        selectedComponent = comp;
        overlayPanel.setSelectedComponent(comp);
        notifySelectionChanged();
    }

    private void deselectAll() {
        selectedComponent = null;
        overlayPanel.setSelectedComponent(null);
        repaint();
        notifySelectionChanged();
    }

    public void deleteSelected() {
        if (selectedComponent != null && selectedComponent != gridPanel) {
            if (selectedComponent instanceof GraphNode) {
                removeGraphNode(((GraphNode) selectedComponent).getNodeId());
            } else {
                remove(selectedComponent);
                selectedComponent = null;
                revalidate();
                repaint();
                notifyModified();
            }
        }
    }

    public void bringSelectedForward() {
        if (selectedComponent != null) {
            Integer currentLayer = getLayer(selectedComponent);
            setLayer(selectedComponent, currentLayer + 1);
            repaint();
            notifyModified();
        }
    }

    public void sendSelectedBack() {
        if (selectedComponent != null) {
            Integer currentLayer = getLayer(selectedComponent);
            if (currentLayer > GRID_LAYER + 1) {
                setLayer(selectedComponent, currentLayer - 1);
                repaint();
                notifyModified();
            }
        }
    }

    public CanvasTheme getTheme() {
        return theme;
    }

    public void setShowGrid(boolean show) {
        this.showGrid = show;
        gridPanel.setVisible(show);
        repaint();
        notifyModified();
    }

    public void setSnapToGrid(boolean snap) {
        this.snapToGrid = snap;
        notifyModified();
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
            if (comp != gridPanel && comp != shadowPanel && comp != edgePanel && comp != overlayPanel) {
                Integer layer = getLayer(comp);
                counts.put(layer, counts.getOrDefault(layer, 0) + 1);
            }
        }
        return counts;
    }

    // ---------------------------------------------------------------
    // Grid panel
    // ---------------------------------------------------------------

    private static class GridPanel extends JPanel {

        private int gridSize;
        private Color gridLineColor;

        private static final long serialVersionUID = 1L;

        public GridPanel(int gridSize, Color gridLineColor) {
            this.gridSize = gridSize;
            this.gridLineColor = gridLineColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            g2d.setColor(gridLineColor);
            g2d.setStroke(new BasicStroke(1));

            int width = getWidth();
            int height = getHeight();

            for (int x = 0; x < width; x += gridSize) {
                g2d.drawLine(x, 0, x, height);
            }

            for (int y = 0; y < height; y += gridSize) {
                g2d.drawLine(0, y, width, y);
            }
        }
    }
}
