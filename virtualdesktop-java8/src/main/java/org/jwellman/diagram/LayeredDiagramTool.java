package org.jwellman.diagram;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.core.NodeHostPanel;

/**
 * Complete diagram tool with multiple independent tabs and a collapsible project panel.
 * Each tab owns its toolbar, canvas, and right panel — switching tabs is a single CardLayout call.
 * The diagram tab bar and left project panel are the only truly global components.
 */
public class LayeredDiagramTool extends JPanel {

    private static final int LEFT_HANDLE_WIDTH  = 22;
    private static final int LEFT_CONTENT_WIDTH = 200;
    private static final int FIXED_TAB_WIDTH    = 160;   // ~20 chars at 12pt

    // ---------------------------------------------------------------
    // Diagram type registry
    // ---------------------------------------------------------------

    private static final class DiagramTypeEntry {
        final String name;
        final Function<DiagramLayeredPane, CanvasComponentFactory> factoryFn;

        DiagramTypeEntry(String name, Function<DiagramLayeredPane, CanvasComponentFactory> fn) {
            this.name = name;
            this.factoryFn = fn;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final List<DiagramTypeEntry> diagramTypes = new ArrayList<>();

    public void registerDiagramType(String name,
                                    Function<DiagramLayeredPane, CanvasComponentFactory> factoryFn) {
        diagramTypes.add(new DiagramTypeEntry(name, factoryFn));
    }

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private final List<DiagramTabContent> tabs = new ArrayList<>();
    private DiagramTabContent activeTab;

    private JPanel diagramTabBar;
    private JPanel diagramCardPanel;
    private CardLayout diagramCardLayout;
    private ButtonGroup diagramTabGroup;

    private JSplitPane outerSplitPane;
    private JButton leftToggleBtn;
    private int savedLeftWidth = LEFT_CONTENT_WIDTH + LEFT_HANDLE_WIDTH;
    private boolean leftExpanded = true;

    private boolean modified = false;

    private static final long serialVersionUID = 1L;

    // ---------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------

    public LayeredDiagramTool() {
        createFirstTab();

        diagramTabGroup = new ButtonGroup();
        diagramTabBar = buildGlobalTabBar();
        diagramCardLayout = new CardLayout();
        diagramCardPanel = new JPanel(diagramCardLayout);

        addTabCard(tabs.get(0));
        diagramTabBar.add(buildAddTabButton());
        diagramCardLayout.show(diagramCardPanel, tabs.get(0).name);

        JPanel leftPanel = buildLeftPanel();
        outerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, diagramCardPanel);
        outerSplitPane.setDividerSize(4);
        outerSplitPane.setOneTouchExpandable(false);
        leftPanel.setPreferredSize(new Dimension(savedLeftWidth, 0));
        leftPanel.setMinimumSize(new Dimension(LEFT_HANDLE_WIDTH, 0));

        setLayout(new BorderLayout());
        add(diagramTabBar, BorderLayout.NORTH);
        add(outerSplitPane, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------
    // Left panel (project navigator stub)
    // ---------------------------------------------------------------

    private JPanel buildLeftPanel() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 0));

        JLabel title = new JLabel("Project");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        title.setBorder(BorderFactory.createEmptyBorder(2, 2, 8, 2));
        content.add(title, BorderLayout.NORTH);

        JLabel placeholder = new JLabel(
            "<html><center><font color='#888888'>(no project loaded)</font></center></html>");
        placeholder.setHorizontalAlignment(JLabel.CENTER);
        content.add(placeholder, BorderLayout.CENTER);

        // Always-visible collapse handle strip on the right edge
        JPanel handle = new JPanel(new BorderLayout());
        handle.setPreferredSize(new Dimension(LEFT_HANDLE_WIDTH, 0));
        handle.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, separatorColor()));

        leftToggleBtn = new JButton("«");  // «
        leftToggleBtn.setFont(leftToggleBtn.getFont().deriveFont(Font.BOLD, 11f));
        leftToggleBtn.setToolTipText("Collapse project panel");
        leftToggleBtn.setFocusPainted(false);
        leftToggleBtn.setBorderPainted(false);
        leftToggleBtn.setContentAreaFilled(false);
        leftToggleBtn.setForeground(new Color(60, 100, 150));
        leftToggleBtn.addActionListener(e -> toggleLeftPanel());
        handle.add(leftToggleBtn, BorderLayout.NORTH);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(content, BorderLayout.CENTER);
        leftPanel.add(handle, BorderLayout.EAST);
        return leftPanel;
    }

    private void toggleLeftPanel() {
        if (leftExpanded) {
            savedLeftWidth = outerSplitPane.getDividerLocation();
            outerSplitPane.setDividerLocation(LEFT_HANDLE_WIDTH);
            leftToggleBtn.setText("»");   // »
            leftToggleBtn.setToolTipText("Expand project panel");
            leftExpanded = false;
        } else {
            outerSplitPane.setDividerLocation(savedLeftWidth);
            leftToggleBtn.setText("«");   // «
            leftToggleBtn.setToolTipText("Collapse project panel");
            leftExpanded = true;
        }
    }

    // ---------------------------------------------------------------
    // Global tab bar helpers
    // ---------------------------------------------------------------

    private static Color separatorColor() {
        Color c = UIManager.getColor("Separator.foreground");
        if (c == null) c = UIManager.getColor("controlShadow");
        if (c == null) c = Color.GRAY;
        return c;
    }

    private JPanel buildGlobalTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor()));
        return bar;
    }

    private JButton buildAddTabButton() {
        JButton btn = new JButton("+");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(new Color(60, 100, 150));
        btn.setToolTipText("Add new diagram");
        btn.addActionListener(e -> promptNewTab());
        return btn;
    }

    // ---------------------------------------------------------------
    // Per-tab content factory
    // ---------------------------------------------------------------

    private DiagramTabContent createTabContent(String name) {
        DiagramLayeredPane pane = new DiagramLayeredPane();
        PropertyEditorPanel propEditor = new PropertyEditorPanel(pane);

        // Layers card
        JPanel layerListPanel = new JPanel();
        layerListPanel.setLayout(new BoxLayout(layerListPanel, BoxLayout.Y_AXIS));
        addLayerRows(layerListPanel, pane);
        JScrollPane layerScroll = new JScrollPane(layerListPanel);
        layerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        layerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Nodes card
        JPanel nodesPanel = new JPanel(new BorderLayout());
        DiagramTabButton nodesTabBtn = new DiagramTabButton("Nodes");

        // Right card layout
        CardLayout rightCardLayout = new CardLayout();
        JPanel rightCardPanel = new JPanel(rightCardLayout);
        rightCardPanel.add(layerScroll, "layers");
        rightCardPanel.add(nodesPanel, "nodes");

        // Right toggle bar
        ButtonGroup rightGroup = new ButtonGroup();
        DiagramTabButton layersTabBtn = new DiagramTabButton("Layers");
        layersTabBtn.addActionListener(e -> rightCardLayout.show(rightCardPanel, "layers"));
        nodesTabBtn.addActionListener(e -> rightCardLayout.show(rightCardPanel, "nodes"));
        rightGroup.add(layersTabBtn);
        rightGroup.add(nodesTabBtn);
        layersTabBtn.setSelected(true);

        JPanel rightTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rightTabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor()));
        rightTabBar.add(layersTabBtn);
        rightTabBar.add(nodesTabBtn);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightCardPanel, propEditor);
        splitPane.setResizeWeight(0.4);
        splitPane.setDividerSize(5);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(280, 0));
        rightPanel.add(rightTabBar, BorderLayout.NORTH);
        rightPanel.add(splitPane, BorderLayout.CENTER);

        JPanel card = new JPanel(new BorderLayout());
        card.add(buildToolBar(pane), BorderLayout.NORTH);
        card.add(new JScrollPane(pane), BorderLayout.CENTER);
        card.add(rightPanel, BorderLayout.EAST);

        DiagramTabContent tab = new DiagramTabContent(
            name, pane, card, propEditor, nodesTabBtn, nodesPanel);
        wireTabListeners(tab);
        return tab;
    }

    private void addLayerRows(JPanel parent, DiagramLayeredPane pane) {
        addLayerControl(parent, "Overlay Layer",    DiagramLayeredPane.OVERLAY_LAYER,    pane);
        addLayerControl(parent, "Connection Layer", DiagramLayeredPane.CONNECTION_LAYER, pane);
        addLayerControl(parent, "Text Layer",       DiagramLayeredPane.TEXT_LAYER,       pane);
        addLayerControl(parent, "Shape Layer",      DiagramLayeredPane.SHAPE_LAYER,      pane);
        addLayerControl(parent, "Background Layer", DiagramLayeredPane.BACKGROUND_LAYER, pane);
        addLayerControl(parent, "Grid Layer",       DiagramLayeredPane.GRID_LAYER,       pane);
    }

    private void addLayerControl(JPanel parent, String layerName,
                                  Integer layerDepth, DiagramLayeredPane pane) {
        parent.add(new LayerControlPanel(layerName, layerDepth, pane));
        parent.add(Box.createVerticalStrut(2));
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
                    Runnable onMod = () -> tab.diagramPane.notifyModified();
                    JPanel newContent = tab.factory.createContentFor(
                        node.getNodeType(), node.getProperties(), onMod);
                    node.swapContent(newContent);
                    tab.diagramPane.notifyModified();
                };
                JPanel editorPanel = tab.factory.createPropertyEditorFor(
                    node.getNodeType(), node.getProperties(), onChanged);
                if (editorPanel != null) {
                    tab.propertyEditor.showNodeEditor(editorPanel);
                    return;
                }
            }
            tab.propertyEditor.setSelectedComponent(component);
        });
    }

    private void createFirstTab() {
        DiagramTabContent tab = createTabContent("Diagram 1");
        tabs.add(tab);
        activeTab = tab;
    }

    private void addTabCard(DiagramTabContent tab) {
        diagramCardPanel.add(tab.tabCard, tab.name);

        DiagramTabButton btn = new DiagramTabButton(tab.displayName, FIXED_TAB_WIDTH);
        btn.addActionListener(e -> selectTab(tab));
        tab.tabBtn = btn;
        diagramTabGroup.add(btn);

        int insertIdx = diagramTabBar.getComponentCount();
        if (insertIdx > 0 && !(diagramTabBar.getComponent(insertIdx - 1) instanceof DiagramTabButton)) {
            insertIdx--;  // insert before the "+" button
        }
        diagramTabBar.add(btn, insertIdx);
        btn.setSelected(true);
        diagramTabBar.revalidate();
        diagramTabBar.repaint();
    }

    private void updateTabDisplayName(DiagramTabContent tab, String newName) {
        tab.displayName = newName;
        if (tab.tabBtn != null) {
            tab.tabBtn.setText(newName);
        }
    }

    private DiagramTabContent findTabForPane(DiagramLayeredPane pane) {
        for (DiagramTabContent tab : tabs) {
            if (tab.diagramPane == pane) {
                return tab;
            }
        }
        return null;
    }

    private static String fileBaseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot > 0) ? fileName.substring(0, dot) : fileName;
    }

    private void selectTab(DiagramTabContent tab) {
        activeTab = tab;
        diagramCardLayout.show(diagramCardPanel, tab.name);
    }

    private void promptNewTab() {
        if (diagramTypes.isEmpty()) {
            createNewTab(pane -> null);
            return;
        }
        DiagramTypeEntry[] options = diagramTypes.toArray(new DiagramTypeEntry[0]);
        Object chosen = JOptionPane.showInputDialog(
            this, "Select diagram type:", "New Diagram",
            JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (chosen instanceof DiagramTypeEntry) {
            createNewTab(((DiagramTypeEntry) chosen).factoryFn);
        }
    }

    private void createNewTab(Function<DiagramLayeredPane, CanvasComponentFactory> factoryFn) {
        String name = "Diagram " + (tabs.size() + 1);
        DiagramTabContent tab = createTabContent(name);
        CanvasComponentFactory factory = factoryFn.apply(tab.diagramPane);
        if (factory != null) {
            tab.factory = factory;
            tab.diagramPane.setComponentFactory(factory);
            updateNodesPanel(tab);
        }
        tabs.add(tab);
        addTabCard(tab);
        selectTab(tab);
    }

    // ---------------------------------------------------------------
    // Nodes panel (updated when factory is assigned)
    // ---------------------------------------------------------------

    private void updateNodesPanel(DiagramTabContent tab) {
        tab.nodesPanel.removeAll();
        if (tab.factory != null) {
            tab.nodesTabButton.setText(tab.factory.getNodePaletteTitle());
            BiConsumer<String, Map<String, Object>> addNode = (nodeType, props) -> {
                String id = UUID.randomUUID().toString();
                String[] portIds = tab.factory.getPortIds(nodeType);
                Runnable onMod = () -> tab.diagramPane.notifyModified();
                JPanel content = tab.factory.createContentFor(nodeType, props, onMod);
                NodeHostPanel node = new NodeHostPanel(id, nodeType, props, content, portIds);
                node.setBounds(60, 60, 200, 150);
                tab.diagramPane.addGraphNode(node, DiagramLayeredPane.SHAPE_LAYER);
                tab.diagramPane.notifyModified();
            };
            JPanel palette = tab.factory.createNodePalettePanel(addNode);
            if (palette != null) {
                tab.nodesPanel.add(palette, BorderLayout.NORTH);
            }
        } else {
            tab.nodesTabButton.setText("Nodes");
        }
        tab.nodesPanel.revalidate();
        tab.nodesPanel.repaint();
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
    // Save / load (per-tab pane)
    // ---------------------------------------------------------------

    private void saveDiagram(DiagramLayeredPane pane) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Diagram");
        fc.setFileFilter(new FileNameExtensionFilter("Diagram files (*.dgx)", "dgx"));
        fc.setSelectedFile(new java.io.File("diagram.dgx"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fc.getSelectedFile();
            String n = file.getName();
            if (!n.endsWith(".dgx") && !n.endsWith(".json")) {
                file = new java.io.File(file.getParentFile(), n + ".dgx");
            }
            try {
                DiagramTabContent tab = findTabForPane(pane);
                pane.setDomainType(tab != null && tab.factory != null
                    ? tab.factory.getDomainTypeId() : null);
                pane.saveDiagram(file);
                setModified(false);
                if (tab != null) {
                    updateTabDisplayName(tab, fileBaseName(file.getName()));
                }
                JOptionPane.showMessageDialog(this, "Diagram saved successfully!",
                    "Save Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving diagram: " + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void loadDiagram(DiagramLayeredPane pane) {
        if (!checkUnsavedChanges()) {
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Load Diagram");
        fc.setFileFilter(new FileNameExtensionFilter("Diagram files (*.dgx, *.json)", "dgx", "json"));

        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fc.getSelectedFile();
            try {
                DiagramTabContent tab = findTabForPane(pane);
                // Factory must be set before loading so graph node restoration works.
                preApplyFactory(file, pane, tab);
                pane.loadDiagram(file);
                setModified(false);
                if (tab != null) {
                    updateTabDisplayName(tab, fileBaseName(file.getName()));
                    updateNodesPanel(tab);
                }
                JOptionPane.showMessageDialog(this, "Diagram loaded successfully!",
                    "Load Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading diagram: " + ex.getMessage(),
                    "Load Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void preApplyFactory(java.io.File file, DiagramLayeredPane pane, DiagramTabContent tab) {
        String id = DiagramLayeredPane.peekDomainType(file);
        if (tab == null) {
            return;
        }
        if (id != null) {
            for (DiagramTypeEntry entry : diagramTypes) {
                if (entry.name.equals(id)) {
                    CanvasComponentFactory factory = entry.factoryFn.apply(pane);
                    tab.factory = factory;
                    pane.setComponentFactory(factory);
                    return;
                }
            }
        }
        // Null or unrecognised domain type → plain diagram; clear any existing factory.
        tab.factory = null;
        pane.setComponentFactory(null);
    }

    // ---------------------------------------------------------------
    // Public API (SpecDiagramTool compatibility)
    // ---------------------------------------------------------------

    public void setComponentFactory(CanvasComponentFactory factory) {
        activeTab.factory = factory;
        activeTab.diagramPane.setComponentFactory(factory);
        updateNodesPanel(activeTab);
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
        if (!modified) {
            return true;
        }
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
        }
        return option == JOptionPane.NO_OPTION;
    }
}
