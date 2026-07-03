package org.jwellman.diagram;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.core.NodeHostPanel;

/**
 * Complete diagram tool using JLayeredPane with grid, layers, and drag-and-drop.
 * Supports multiple independent diagram tabs, each with its own canvas and toolbar.
 */
public class LayeredDiagramTool extends JPanel {

    // Multi-tab state
    private final List<DiagramTabContent> tabs = new ArrayList<>();
    private DiagramTabContent activeTab;

    // Center area
    private JPanel centerPanel;
    private JPanel diagramTabBar;
    private JPanel diagramCardPanel;
    private CardLayout diagramCardLayout;
    private ButtonGroup diagramTabGroup;

    // Right panel
    private JPanel rightPanel;
    private JPanel rightCardPanel;
    private CardLayout rightCardLayout;
    private JPanel layerListPanel;
    private JPanel nodesPanel;
    private JToggleButton nodesTabButton;

    // Shared components
    private PropertyEditorPanel propertyEditor;
    private boolean modified = false;

    private static final long serialVersionUID = 1L;

    public LayeredDiagramTool() {
        createFirstTab();
        createPropertyEditor();
        createCenterPanel();
        createRightPanel();

        setLayout(new BorderLayout());
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    // ---------------------------------------------------------------
    // Initialization
    // ---------------------------------------------------------------

    private void createFirstTab() {
        DiagramLayeredPane pane = new DiagramLayeredPane();
        JToolBar tb = buildToolBar(pane);
        JPanel card = new JPanel(new BorderLayout());
        card.add(tb, BorderLayout.NORTH);
        card.add(new JScrollPane(pane), BorderLayout.CENTER);

        DiagramTabContent tab = new DiagramTabContent("Diagram 1", pane, card);
        tabs.add(tab);
        activeTab = tab;
        wireTabListeners(tab);
    }

    private void createPropertyEditor() {
        propertyEditor = new PropertyEditorPanel(activeTab.diagramPane);
    }

    private void createCenterPanel() {
        diagramTabGroup = new ButtonGroup();
        diagramTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));

        diagramCardLayout = new CardLayout();
        diagramCardPanel = new JPanel(diagramCardLayout);

        addTabCard(tabs.get(0));

        JButton addTabBtn = new JButton("+");
        addTabBtn.setToolTipText("Add new diagram");
        addTabBtn.addActionListener(e -> createNewTab());
        diagramTabBar.add(addTabBtn);

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(diagramTabBar, BorderLayout.NORTH);
        centerPanel.add(diagramCardPanel, BorderLayout.CENTER);

        diagramCardLayout.show(diagramCardPanel, tabs.get(0).name);
    }

    private void createRightPanel() {
        ButtonGroup rightTabGroup = new ButtonGroup();
        JToggleButton layersTabBtn = new JToggleButton("Layers");
        nodesTabButton = new JToggleButton("Nodes");

        rightCardLayout = new CardLayout();
        rightCardPanel = new JPanel(rightCardLayout);

        layerListPanel = new JPanel();
        layerListPanel.setLayout(new BoxLayout(layerListPanel, BoxLayout.Y_AXIS));
        JScrollPane layerScroll = new JScrollPane(layerListPanel);
        layerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        layerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        rightCardPanel.add(layerScroll, "layers");

        nodesPanel = new JPanel(new BorderLayout());
        rightCardPanel.add(nodesPanel, "nodes");

        layersTabBtn.addActionListener(e -> rightCardLayout.show(rightCardPanel, "layers"));
        nodesTabButton.addActionListener(e -> rightCardLayout.show(rightCardPanel, "nodes"));
        rightTabGroup.add(layersTabBtn);
        rightTabGroup.add(nodesTabButton);
        layersTabBtn.setSelected(true);

        JPanel rightTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        rightTabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        rightTabBar.add(layersTabBtn);
        rightTabBar.add(nodesTabButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightCardPanel, propertyEditor);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(5);

        rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(280, 0));
        rightPanel.add(rightTabBar, BorderLayout.NORTH);
        rightPanel.add(splitPane, BorderLayout.CENTER);

        rebuildLayersPanel(activeTab);
        rebuildNodesPanel(activeTab);
    }

    // ---------------------------------------------------------------
    // Tab management
    // ---------------------------------------------------------------

    private void wireTabListeners(DiagramTabContent tab) {
        tab.diagramPane.setModificationListener(() -> setModified(true));
        tab.diagramPane.setSelectionListener(component -> {
            if (component instanceof NodeHostPanel && tab.factory != null) {
                NodeHostPanel node = (NodeHostPanel) component;
                Runnable onChanged = () -> {
                    JPanel newContent = tab.factory.createContentFor(
                        node.getNodeType(), node.getProperties());
                    node.swapContent(newContent);
                    tab.diagramPane.notifyModified();
                };
                JPanel editorPanel = tab.factory.createPropertyEditorFor(
                    node.getNodeType(), node.getProperties(), onChanged);
                if (editorPanel != null) {
                    propertyEditor.showNodeEditor(editorPanel);
                    return;
                }
            }
            propertyEditor.setSelectedComponent(component);
        });
    }

    private void addTabCard(DiagramTabContent tab) {
        diagramCardPanel.add(tab.tabCard, tab.name);

        JToggleButton btn = new JToggleButton(tab.name);
        btn.addActionListener(e -> selectTab(tab));
        diagramTabGroup.add(btn);

        int insertIdx = diagramTabBar.getComponentCount();
        if (insertIdx > 0 && diagramTabBar.getComponent(insertIdx - 1) instanceof JButton) {
            insertIdx--;
        }
        diagramTabBar.add(btn, insertIdx);
        btn.setSelected(true);
        diagramTabBar.revalidate();
        diagramTabBar.repaint();
    }

    private void selectTab(DiagramTabContent tab) {
        activeTab = tab;
        diagramCardLayout.show(diagramCardPanel, tab.name);
        rebuildLayersPanel(tab);
        rebuildNodesPanel(tab);
        propertyEditor.setDiagramPane(tab.diagramPane);
        propertyEditor.setSelectedComponent(null);
    }

    private void createNewTab() {
        String name = "Diagram " + (tabs.size() + 1);
        DiagramLayeredPane pane = new DiagramLayeredPane();
        JToolBar tb = buildToolBar(pane);
        JPanel card = new JPanel(new BorderLayout());
        card.add(tb, BorderLayout.NORTH);
        card.add(new JScrollPane(pane), BorderLayout.CENTER);

        DiagramTabContent tab = new DiagramTabContent(name, pane, card);
        if (activeTab.factory != null) {
            tab.factory = activeTab.factory;
            pane.setComponentFactory(activeTab.factory);
        }
        tabs.add(tab);
        wireTabListeners(tab);
        addTabCard(tab);
        selectTab(tab);
    }

    // ---------------------------------------------------------------
    // Right panel rebuilds (called on tab switch)
    // ---------------------------------------------------------------

    private void rebuildLayersPanel(DiagramTabContent tab) {
        layerListPanel.removeAll();
        addLayerControl("Overlay Layer",    DiagramLayeredPane.OVERLAY_LAYER,    tab.diagramPane);
        addLayerControl("Connection Layer", DiagramLayeredPane.CONNECTION_LAYER, tab.diagramPane);
        addLayerControl("Text Layer",       DiagramLayeredPane.TEXT_LAYER,       tab.diagramPane);
        addLayerControl("Shape Layer",      DiagramLayeredPane.SHAPE_LAYER,      tab.diagramPane);
        addLayerControl("Background Layer", DiagramLayeredPane.BACKGROUND_LAYER, tab.diagramPane);
        addLayerControl("Grid Layer",       DiagramLayeredPane.GRID_LAYER,       tab.diagramPane);
        layerListPanel.revalidate();
        layerListPanel.repaint();
    }

    private void addLayerControl(String layerName, Integer layerDepth, DiagramLayeredPane pane) {
        layerListPanel.add(new LayerControlPanel(layerName, layerDepth, pane));
        layerListPanel.add(Box.createVerticalStrut(2));
    }

    private void rebuildNodesPanel(DiagramTabContent tab) {
        nodesPanel.removeAll();
        if (tab.factory != null) {
            nodesTabButton.setText(tab.factory.getNodePaletteTitle());
            BiConsumer<String, Map<String, Object>> addNode = (nodeType, props) -> {
                String id = UUID.randomUUID().toString();
                String[] portIds = tab.factory.getPortIds(nodeType);
                JPanel content = tab.factory.createContentFor(nodeType, props);
                NodeHostPanel node = new NodeHostPanel(id, nodeType, props, content, portIds);
                node.setBounds(60, 60, 200, 150);
                tab.diagramPane.addGraphNode(node, DiagramLayeredPane.SHAPE_LAYER);
                tab.diagramPane.notifyModified();
            };
            JPanel palette = tab.factory.createNodePalettePanel(addNode);
            if (palette != null) {
                nodesPanel.add(palette, BorderLayout.NORTH);
            }
        } else {
            nodesTabButton.setText("Nodes");
        }
        nodesPanel.revalidate();
        nodesPanel.repaint();
    }

    // ---------------------------------------------------------------
    // Per-tab toolbar
    // ---------------------------------------------------------------

    private JToolBar buildToolBar(DiagramLayeredPane pane) {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        JButton addRectBtn = new JButton("Add Rectangle");
        addRectBtn.addActionListener(e -> {
            DiagramShape shape = new DiagramShape(ShapeType.RECTANGLE);
            shape.setBounds(100, 100, 120, 80);
            pane.addDiagramComponent(shape, pane.getActiveLayer());
        });
        tb.add(addRectBtn);

        JButton addTriangleBtn = new JButton("Add Triangle");
        addTriangleBtn.addActionListener(e -> {
            DiagramShape shape = new DiagramShape(ShapeType.TRIANGLE);
            shape.setBounds(100, 100, 120, 80);
            pane.addDiagramComponent(shape, pane.getActiveLayer());
        });
        tb.add(addTriangleBtn);

        JButton addCircleBtn = new JButton("Add Circle");
        addCircleBtn.addActionListener(e -> {
            DiagramShape shape = new DiagramShape(ShapeType.CIRCLE);
            shape.setBounds(100, 100, 120, 80);
            pane.addDiagramComponent(shape, pane.getActiveLayer());
        });
        tb.add(addCircleBtn);

        JButton addTextBtn = new JButton("Add Text");
        addTextBtn.addActionListener(e -> {
            DiagramText text = new DiagramText("Text");
            text.setBounds(100, 100, 150, 30);
            pane.addDiagramComponent(text, pane.getActiveLayer());
        });
        tb.add(addTextBtn);

        tb.addSeparator();

        JCheckBox gridCheck = new JCheckBox("Show Grid", true);
        gridCheck.addActionListener(e -> pane.setShowGrid(gridCheck.isSelected()));
        tb.add(gridCheck);

        JCheckBox snapCheck = new JCheckBox("Snap to Grid", true);
        snapCheck.addActionListener(e -> pane.setSnapToGrid(snapCheck.isSelected()));
        tb.add(snapCheck);

        JCheckBox shadowCheck = new JCheckBox("Shadows", true);
        shadowCheck.addActionListener(e -> pane.setShadowsEnabled(shadowCheck.isSelected()));
        tb.add(shadowCheck);

        JButton bringForwardBtn = new JButton("Bring Forward");
        bringForwardBtn.addActionListener(e -> pane.bringSelectedForward());
        tb.add(bringForwardBtn);

        JButton sendBackBtn = new JButton("Send Back");
        sendBackBtn.addActionListener(e -> pane.sendSelectedBack());
        tb.add(sendBackBtn);

        tb.addSeparator();

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> pane.deleteSelected());
        tb.add(deleteBtn);

        tb.addSeparator();

        JButton saveBtn = new JButton("Save Diagram");
        saveBtn.addActionListener(e -> saveDiagram(pane));
        tb.add(saveBtn);

        JButton loadBtn = new JButton("Load Diagram");
        loadBtn.addActionListener(e -> loadDiagram(pane));
        tb.add(loadBtn);

        tb.addSeparator();

        JToggleButton connectBtn = new JToggleButton("Connect");
        connectBtn.addActionListener(e -> {
            if (connectBtn.isSelected()) {
                pane.enterEdgeCreationMode();
            } else {
                pane.exitEdgeCreationMode();
            }
        });
        tb.add(connectBtn);

        return tb;
    }

    // ---------------------------------------------------------------
    // Save / load (operate on a specific pane)
    // ---------------------------------------------------------------

    private void saveDiagram(DiagramLayeredPane pane) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Diagram");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Diagram files (*.dgx)", "dgx"));
        fileChooser.setSelectedFile(new java.io.File("diagram.dgx"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            String name = file.getName();
            if (!name.endsWith(".dgx") && !name.endsWith(".json")) {
                file = new java.io.File(file.getParentFile(), name + ".dgx");
            }
            try {
                pane.saveDiagram(file);
                setModified(false);
                JOptionPane.showMessageDialog(this, "Diagram saved successfully!", "Save Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving diagram: " + ex.getMessage(), "Save Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void loadDiagram(DiagramLayeredPane pane) {
        if (!checkUnsavedChanges()) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Diagram");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Diagram files (*.dgx, *.json)", "dgx", "json"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                pane.loadDiagram(file);
                setModified(false);
                JOptionPane.showMessageDialog(this, "Diagram loaded successfully!", "Load Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading diagram: " + ex.getMessage(), "Load Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    // ---------------------------------------------------------------
    // Public API (SpecDiagramTool compatibility)
    // ---------------------------------------------------------------

    public void setComponentFactory(CanvasComponentFactory factory) {
        activeTab.factory = factory;
        activeTab.diagramPane.setComponentFactory(factory);
        if (nodesPanel != null) {
            rebuildNodesPanel(activeTab);
        }
    }

    public DiagramLayeredPane getDiagramPane() {
        return activeTab.diagramPane;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean checkUnsavedChanges() {
        if (modified) {
            int option = JOptionPane.showConfirmDialog(
                this,
                "You have unsaved changes. Do you want to save before closing?",
                "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (option == JOptionPane.YES_OPTION) {
                saveDiagram(activeTab.diagramPane);
                return !modified;
            } else if (option == JOptionPane.NO_OPTION) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }
}
