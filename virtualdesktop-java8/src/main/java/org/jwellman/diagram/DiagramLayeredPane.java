package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.api.CanvasTheme;
import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.EdgeRouter;
import org.jwellman.diagram.api.GraphEdge;
import org.jwellman.diagram.api.GraphNode;
import org.jwellman.diagram.core.WhiteprintCanvasTheme;
import org.jwellman.diagram.core.CanvasOverlayPanel;
import org.jwellman.diagram.core.CanvasThemeRegistry;
import org.jwellman.diagram.core.DefaultGraphEdge;
import org.jwellman.diagram.core.EdgeRenderPanel;
import org.jwellman.diagram.core.NodeHostPanel;
import org.jwellman.diagram.core.OrthogonalRouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Custom JLayeredPane with grid, layer management, and graph model support.
 */
public class DiagramLayeredPane extends JLayeredPane implements Scrollable {

    // Define layer constants
    public static final Integer GRID_LAYER       = new Integer(0);
    public static final Integer BACKGROUND_LAYER = new Integer(100);
    public static final Integer SHADOW_LAYER     = new Integer(150);
    public static final Integer SHAPE_LAYER      = new Integer(200);
    public static final Integer TEXT_LAYER       = new Integer(300);
    public static final Integer CONNECTION_LAYER = new Integer(400);
    public static final Integer OVERLAY_LAYER    = new Integer(500);

    /** Alignment edge/axis for {@link #alignSelected(Alignment)}. */
    public enum Alignment { LEFT, CENTER_HORIZONTAL, RIGHT, TOP, MIDDLE_VERTICAL, BOTTOM }

    private GridPanel gridPanel;
    @SuppressWarnings("unused")
    private boolean showGrid = true;
    private boolean snapToGrid = true;
    private int gridSize = 20;
    private Set<Component> selectedComponents = new LinkedHashSet<>();
    private Point marqueeStart;
    private GraphEdge selectedEdge = null;
    private Integer activeLayer = SHAPE_LAYER;
    private EdgeAttributes activeEdgeAttributes = new EdgeAttributes();
    private String activeEdgeType; // "type" property applied to newly created edges; null = no named preset active
    private Map<Integer, Boolean> layerVisibility = new HashMap<>();
    private Runnable modificationListener;
    private java.util.function.Consumer<Component> selectionListener;
    private java.util.function.Consumer<GraphEdge> edgeSelectionListener;

    // Infrastructure panels
    private ShadowLayerPanel shadowPanel;
    private boolean shadowsEnabled = true;

    // Canvas theme — controls all colors on the canvas surface and its nodes
    private CanvasTheme theme = new WhiteprintCanvasTheme();

    // Name of a theme referenced by a loaded file that could not be resolved;
    // cleared by getAndClearThemeWarning() once the caller has shown the warning.
    private String unresolvedThemeName;

    // Graph model
    private EdgeRenderPanel edgePanel;
    private CanvasOverlayPanel overlayPanel;
    private Map<String, GraphNode> graphNodes = new LinkedHashMap<>();
    private EdgeRouter edgeRouter = new OrthogonalRouter();
    private CanvasComponentFactory componentFactory;
    private String domainType;

    private static final long serialVersionUID = 1L;

    private static final Dimension CANVAS_SIZE = new Dimension(2000, 1500);

    public DiagramLayeredPane() {
        setPreferredSize(CANVAS_SIZE);
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
            edge -> addGraphEdge(withActiveType(edge)),
            coord -> isSnapToGrid() ? snapToGrid(coord) : coord,
            () -> notifyModified(),
            () -> activeEdgeAttributes
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

    /** Stamps the currently active named relationship type (if any) onto a freshly created edge. */
    private GraphEdge withActiveType(GraphEdge edge) {
        if (activeEdgeType != null) {
            edge.setEdgeType(activeEdgeType);
        }
        return edge;
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

    public String getDomainType() {
        return domainType;
    }

    public void setDomainType(String domainType) {
        this.domainType = domainType;
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

    /**
     * Reads only the {@code domainType} field from a diagram file without fully loading it.
     * Returns {@code null} if the field is absent, null in JSON, or the file cannot be read.
     * Used by the caller to set the correct factory before a full load.
     */
    public static String peekDomainType(java.io.File file) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(file);
            JsonNode dt = root.get("domainType");
            return (dt != null && !dt.isNull()) ? dt.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void saveDiagram(java.io.File file) throws Exception {
        DiagramData diagram = new DiagramData();
        diagram.setVersion(FileVersion.current());
        diagram.setDomainType(domainType);
        diagram.setThemeName(theme.getThemeName());
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
                if (attrs.getSourceArrowType() != EdgeAttributes.ArrowType.NONE) {
                    ed.setSourceArrowType(attrs.getSourceArrowType().name());
                }
                ed.setType(edge.getEdgeType());
                if (!edge.getProperties().isEmpty()) {
                    ed.setProperties(edge.getProperties());
                }
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

        domainType = diagram.getDomainType();
        gridSize = diagram.getGridSize();
        snapToGrid = diagram.isSnapToGrid();
        activeLayer = diagram.getActiveLayer();

        unresolvedThemeName = null;
        String themeName = diagram.getThemeName();
        if (themeName != null) {
            CanvasTheme loadedTheme = CanvasThemeRegistry.byName(themeName);
            if (loadedTheme != null) {
                setTheme(loadedTheme);
            } else {
                unresolvedThemeName = themeName;
            }
        }

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
                if (ed.getSourceArrowType() != null) {
                    attrs.setSourceArrowType(EdgeAttributes.ArrowType.valueOf(ed.getSourceArrowType()));
                }
                Map<String, Object> edgeProps = ed.getProperties() != null ? ed.getProperties() : new HashMap<>();
                String edgeType = ed.getType();
                if (edgeType == null && edgeProps.get("type") instanceof String) {
                    // Migrates files saved during the brief window "type" lived in
                    // properties instead of as its own field.
                    edgeType = (String) edgeProps.remove("type");
                }
                addGraphEdge(new DefaultGraphEdge(
                    ed.getId(),
                    ed.getSourceNodeId(), ed.getSourcePortId(),
                    ed.getTargetNodeId(), ed.getTargetPortId(),
                    attrs, edgeType, edgeProps));
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

    public void setEdgeSelectionListener(java.util.function.Consumer<GraphEdge> listener) {
        this.edgeSelectionListener = listener;
    }

    public GraphEdge getSelectedEdge() {
        return selectedEdge;
    }

    /**
     * Sets the active relationship attributes used when drawing new edges, and
     * applies them to the currently selected edge (if any). Used by the generic
     * (non-UML) line-style/arrow picker, which has no named relationship type.
     */
    public void applyRelationship(EdgeAttributes attrs) {
        applyRelationship(attrs, null);
    }

    /**
     * Same as {@link #applyRelationship(EdgeAttributes)}, plus a named relationship
     * type (e.g. "Composition") — set on new edges going forward and, if non-null,
     * on the currently selected edge's {@link GraphEdge#setEdgeType(String)}. A
     * null typeName leaves the selected edge's existing type untouched (so tweaking
     * line style/arrow directly doesn't erase a previously chosen preset).
     */
    public void applyRelationship(EdgeAttributes attrs, String typeName) {
        activeEdgeAttributes = new EdgeAttributes(attrs);
        activeEdgeType = typeName;
        if (selectedEdge != null) {
            EdgeAttributes target = selectedEdge.getAttributes();
            target.setLineStyle(attrs.getLineStyle());
            target.setArrowType(attrs.getArrowType());
            target.setSourceArrowType(attrs.getSourceArrowType());
            target.setColor(attrs.getColor());
            target.setStrokeWidth(attrs.getStrokeWidth());
            if (typeName != null) {
                selectedEdge.setEdgeType(typeName);
            }
            edgePanel.repaint();
            notifyModified();
        }
    }

    public void notifyModified() {
        if (modificationListener != null) {
            modificationListener.run();
        }
    }

    private void notifySelectionChanged() {
        if (selectionListener != null) {
            Component target = (selectedComponents.size() == 1) ? selectedComponents.iterator().next() : null;
            selectionListener.accept(target);
        }
    }

    private void notifyEdgeSelectionChanged() {
        if (edgeSelectionListener != null) {
            edgeSelectionListener.accept(selectedEdge);
        }
    }

    private void setupMouseListeners() {
        MouseAdapter canvasHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Component comp = getComponentAt(e.getPoint());
                if (comp == DiagramLayeredPane.this || comp == gridPanel) {
                    GraphEdge hitEdge = edgePanel.findEdgeAt(e.getX(), e.getY());
                    if (hitEdge != null) {
                        selectEdge(hitEdge);
                        return;
                    }
                    if (!e.isControlDown()) {
                        deselectAll();
                    }
                    marqueeStart = e.getPoint();
                    overlayPanel.setMarqueeRect(new Rectangle(marqueeStart));
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (marqueeStart == null) {
                    return;
                }
                overlayPanel.setMarqueeRect(normalizedRect(marqueeStart, e.getPoint()));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (marqueeStart == null) {
                    return;
                }
                Rectangle rect = normalizedRect(marqueeStart, e.getPoint());
                marqueeStart = null;
                overlayPanel.setMarqueeRect(null);
                if (rect.width > 2 || rect.height > 2) {
                    selectComponentsIn(rect, e.isControlDown());
                }
            }
        };
        addMouseListener(canvasHandler);
        addMouseMotionListener(canvasHandler);

        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE
                        || e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE) {
                    deleteSelected();
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_A && e.isControlDown()) {
                    selectAll();
                }
            }
        });
    }

    private static Rectangle normalizedRect(Point a, Point b) {
        int x = Math.min(a.x, b.x);
        int y = Math.min(a.y, b.y);
        int w = Math.abs(a.x - b.x);
        int h = Math.abs(a.y - b.y);
        return new Rectangle(x, y, w, h);
    }

    private void selectComponentsIn(Rectangle rect, boolean addToExisting) {
        Set<Component> hits = new LinkedHashSet<>();
        if (addToExisting) {
            hits.addAll(selectedComponents);
        }
        for (Component comp : getComponents()) {
            if (isSelectable(comp) && isLayerVisible(getLayer(comp)) && comp.getBounds().intersects(rect)) {
                hits.add(comp);
            }
        }
        setSelection(hits);
    }

    private boolean isSelectable(Component comp) {
        return comp instanceof DiagramShape || comp instanceof DiagramText || comp instanceof GraphNode;
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
        // Selection + context menu — registered before the drag handler so a
        // group drag (started by the same mousePressed gesture) sees the
        // click-updated selection rather than whatever was selected before it.
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (e.isControlDown()) {
                        toggleSelection(component);
                    } else if (!selectedComponents.contains(component)) {
                        selectComponent(component);
                    }
                    // else: already part of the selection — preserve the group
                    // so the drag that follows moves every selected component
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    if (!selectedComponents.contains(component)) {
                        selectComponent(component);
                    }
                    // Only show context menu for decorative elements
                    if (!(component instanceof GraphNode)) {
                        showComponentPopupMenu(component, e.getX(), e.getY());
                    }
                }
            }
        });

        // Drag
        DragHandler dragHandler = new DragHandler(this);
        component.addMouseListener(dragHandler);
        component.addMouseMotionListener(dragHandler);
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
            if (selectedComponents.remove(component)) {
                overlayPanel.setSelection(selectedComponents);
                notifySelectionChanged();
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
        Set<Component> single = new LinkedHashSet<>();
        single.add(comp);
        setSelection(single);
    }

    private void toggleSelection(Component comp) {
        Set<Component> updated = new LinkedHashSet<>(selectedComponents);
        if (!updated.remove(comp)) {
            updated.add(comp);
        }
        setSelection(updated);
    }

    /** Replaces the current multi-selection, clearing any edge selection. */
    private void setSelection(Set<Component> newSelection) {
        selectedComponents.clear();
        selectedComponents.addAll(newSelection);
        selectedEdge = null;
        edgePanel.setSelectedEdge(null);
        overlayPanel.setSelection(selectedComponents);
        repaint();
        // Edge-clear notification must fire first: it always carries null and
        // LayeredDiagramTool's edge listener treats a null edge as "blank the
        // property editor" — firing it after notifySelectionChanged() would
        // wipe out the property editor content that call just populated.
        notifyEdgeSelectionChanged();
        notifySelectionChanged();
    }

    public void selectAll() {
        Set<Component> all = new LinkedHashSet<>();
        for (Component comp : getComponents()) {
            if (isSelectable(comp)) {
                all.add(comp);
            }
        }
        setSelection(all);
    }

    public Set<Component> getSelectedComponents() {
        return java.util.Collections.unmodifiableSet(selectedComponents);
    }

    private void selectEdge(GraphEdge edge) {
        if (edge == selectedEdge) {
            return;
        }
        selectedComponents.clear();
        overlayPanel.setSelection(selectedComponents);
        selectedEdge = edge;
        edgePanel.setSelectedEdge(edge);
        repaint();
        notifySelectionChanged();
        notifyEdgeSelectionChanged();
    }

    private void deselectAll() {
        setSelection(java.util.Collections.<Component>emptySet());
    }

    public void deleteSelected() {
        if (selectedEdge != null) {
            removeGraphEdge(selectedEdge.getEdgeId());
            selectedEdge = null;
            edgePanel.setSelectedEdge(null);
            notifyEdgeSelectionChanged();
            return;
        }
        if (!selectedComponents.isEmpty()) {
            java.util.List<Component> toRemove = new java.util.ArrayList<>(selectedComponents);
            for (Component comp : toRemove) {
                if (comp instanceof GraphNode) {
                    removeGraphNode(((GraphNode) comp).getNodeId());
                } else {
                    remove(comp);
                }
            }
            selectedComponents.clear();
            overlayPanel.setSelection(selectedComponents);
            revalidate();
            repaint();
            notifyModified();
            notifySelectionChanged();
        }
    }

    public void bringSelectedForward() {
        if (selectedComponents.isEmpty()) {
            return;
        }
        for (Component comp : selectedComponents) {
            Integer currentLayer = getLayer(comp);
            setLayer(comp, currentLayer + 1);
        }
        repaint();
        notifyModified();
    }

    public void sendSelectedBack() {
        if (selectedComponents.isEmpty()) {
            return;
        }
        for (Component comp : selectedComponents) {
            Integer currentLayer = getLayer(comp);
            if (currentLayer > GRID_LAYER + 1) {
                setLayer(comp, currentLayer - 1);
            }
        }
        repaint();
        notifyModified();
    }

    // ---------------------------------------------------------------
    // Align / distribute (Phase 5 — multi-select group operations)
    // ---------------------------------------------------------------

    /** Aligns all selected components to the given edge/axis of their combined bounding box. No-op below 2 selected. */
    public void alignSelected(Alignment alignment) {
        if (selectedComponents.size() < 2) {
            return;
        }
        Rectangle bbox = computeBoundingBox(selectedComponents);
        for (Component comp : selectedComponents) {
            int newX = comp.getX();
            int newY = comp.getY();
            switch (alignment) {
                case LEFT:
                    newX = bbox.x;
                    break;
                case RIGHT:
                    newX = bbox.x + bbox.width - comp.getWidth();
                    break;
                case CENTER_HORIZONTAL:
                    newX = bbox.x + (bbox.width - comp.getWidth()) / 2;
                    break;
                case TOP:
                    newY = bbox.y;
                    break;
                case BOTTOM:
                    newY = bbox.y + bbox.height - comp.getHeight();
                    break;
                case MIDDLE_VERTICAL:
                    newY = bbox.y + (bbox.height - comp.getHeight()) / 2;
                    break;
                default:
                    break;
            }
            moveComponentTo(comp, newX, newY);
        }
        revalidate();
        repaint();
        notifyModified();
    }

    /** Spaces the selected components evenly between the outermost two along one axis. No-op below 3 selected. */
    public void distributeSelected(boolean horizontal) {
        if (selectedComponents.size() < 3) {
            return;
        }
        java.util.List<Component> sorted = new java.util.ArrayList<>(selectedComponents);
        if (horizontal) {
            java.util.Collections.sort(sorted, new java.util.Comparator<Component>() {
                @Override
                public int compare(Component a, Component b) {
                    return Integer.compare(a.getX(), b.getX());
                }
            });
            Component first = sorted.get(0);
            Component last = sorted.get(sorted.size() - 1);
            int span = (last.getX() + last.getWidth()) - first.getX();
            int occupied = 0;
            for (Component c : sorted) {
                occupied += c.getWidth();
            }
            double gap = (double) (span - occupied) / (sorted.size() - 1);
            double cursor = first.getX() + first.getWidth();
            for (int i = 1; i < sorted.size() - 1; i++) {
                Component c = sorted.get(i);
                int newX = (int) Math.round(cursor + gap);
                moveComponentTo(c, newX, c.getY());
                cursor = newX + c.getWidth();
            }
        } else {
            java.util.Collections.sort(sorted, new java.util.Comparator<Component>() {
                @Override
                public int compare(Component a, Component b) {
                    return Integer.compare(a.getY(), b.getY());
                }
            });
            Component first = sorted.get(0);
            Component last = sorted.get(sorted.size() - 1);
            int span = (last.getY() + last.getHeight()) - first.getY();
            int occupied = 0;
            for (Component c : sorted) {
                occupied += c.getHeight();
            }
            double gap = (double) (span - occupied) / (sorted.size() - 1);
            double cursor = first.getY() + first.getHeight();
            for (int i = 1; i < sorted.size() - 1; i++) {
                Component c = sorted.get(i);
                int newY = (int) Math.round(cursor + gap);
                moveComponentTo(c, c.getX(), newY);
                cursor = newY + c.getHeight();
            }
        }
        revalidate();
        repaint();
        notifyModified();
    }

    private void moveComponentTo(Component comp, int newX, int newY) {
        comp.setLocation(newX, newY);
        if (comp instanceof GraphNode) {
            GraphNode gn = (GraphNode) comp;
            gn.invalidatePortCache();
            edgePanel.nodeUpdated(gn.getNodeId());
        }
    }

    private Rectangle computeBoundingBox(java.util.Collection<Component> comps) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (Component c : comps) {
            Rectangle b = c.getBounds();
            minX = Math.min(minX, b.x);
            minY = Math.min(minY, b.y);
            maxX = Math.max(maxX, b.x + b.width);
            maxY = Math.max(maxY, b.y + b.height);
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public CanvasTheme getTheme() {
        return theme;
    }

    /**
     * Swaps the active theme and cascades the new palette to the canvas
     * background, grid, edges, the component factory, and every live graph node.
     */
    public void setTheme(CanvasTheme newTheme) {
        this.theme = newTheme;
        setBackground(newTheme.getCanvasBackground());
        gridPanel.setGridLineColor(newTheme.getGridLineColor());
        edgePanel.setTheme(newTheme);

        if (componentFactory != null) {
            componentFactory.setTheme(newTheme);
            for (GraphNode node : graphNodes.values()) {
                if (node instanceof NodeHostPanel) {
                    NodeHostPanel nhp = (NodeHostPanel) node;
                    JPanel newContent = componentFactory.createContentFor(
                        nhp.getNodeType(), nhp.getProperties(), () -> notifyModified());
                    nhp.swapContent(newContent);
                }
            }
        }

        revalidate();
        repaint();
        notifyModified();
    }

    /**
     * Returns the name of a theme referenced by the last loaded file that could not
     * be resolved, clearing it in the process. Returns {@code null} if the last load
     * had no unresolved theme (or no file has been loaded yet).
     */
    public String getAndClearThemeWarning() {
        String warning = unresolvedThemeName;
        unresolvedThemeName = null;
        return warning;
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
    // Scrollable — without this, JScrollPane falls back to a tiny default
    // unit increment (a few px), making mouse-wheel scrolling over the
    // 2000x1500 canvas feel unusably slow.
    // ---------------------------------------------------------------

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return gridSize * 2;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return (orientation == SwingConstants.VERTICAL) ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
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

        public void setGridLineColor(Color gridLineColor) {
            this.gridLineColor = gridLineColor;
            repaint();
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
