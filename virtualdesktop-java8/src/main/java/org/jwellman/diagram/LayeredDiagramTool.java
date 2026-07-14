package org.jwellman.diagram;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jwellman.diagram.api.CanvasComponentFactory;
import org.jwellman.diagram.api.CanvasTheme;
import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.GraphEdge;
import org.jwellman.diagram.api.RelationshipType;
import org.jwellman.diagram.core.CanvasThemeRegistry;
import org.jwellman.diagram.core.NodeHostPanel;
import org.jwellman.swing.layout.FluidConstraint;
import org.jwellman.swing.layout.FluidLayout;

/**
 * Complete diagram tool with multiple independent tabs, plus a leftmost,
 * non-closable "Projects..." tab that hosts the New Diagram launcher.
 * Each diagram tab owns its toolbar, canvas, and right panel — switching tabs
 * is a single CardLayout call. The diagram tab bar is the only truly global component.
 */
public class LayeredDiagramTool extends JPanel {

    private static final int FIXED_TAB_WIDTH    = 160;   // ~20 chars at 12pt
    private static final int COLLAPSED_BOTTOM_HEIGHT = 30;

    private static final Color     ACCENT_COLOR          = new Color(60, 100, 150);
    private static final Dimension RIGHT_PANEL_SIZE      = new Dimension(280, 0);
    private static final Dimension DETAILS_PANEL_SIZE    = new Dimension(0, 160);
    private static final Dimension COLLAPSED_PANEL_SIZE  = new Dimension(0, COLLAPSED_BOTTOM_HEIGHT);
    private static final Dimension FILE_BROWSER_SIZE     = new Dimension(640, 300);
    private static final Dimension FILE_TILE_SIZE        = new Dimension(1, 64);
    private static final Dimension NEW_DIAGRAM_TILE_SIZE = new Dimension(1, 56);
    private static final Dimension NEW_DIAGRAM_PANEL_SIZE = new Dimension(240, 300);

    private static final String PROJECTS_CARD_KEY = "__projects__";

    private static final javax.swing.border.Border EDGE_EDITOR_BORDER     = BorderFactory.createEmptyBorder(8, 8, 8, 8);
    private static final javax.swing.border.Border RELATIONSHIPS_BORDER   = BorderFactory.createEmptyBorder();
    private static final javax.swing.border.Border TILES_PANEL_BORDER     = BorderFactory.createEmptyBorder(4, 4, 4, 4);
    private static final javax.swing.border.Border HEADER_ROW_BORDER      = BorderFactory.createEmptyBorder(0, 0, 6, 0);
    private static final javax.swing.border.Border DIALOG_CONTENT_BORDER  = BorderFactory.createEmptyBorder(12, 12, 12, 12);
    private static final javax.swing.border.Border FILE_PLACEHOLDER_BORDER = BorderFactory.createEmptyBorder(16, 16, 16, 16);
    private static final javax.swing.border.Border FILE_TILE_INNER_BORDER  = BorderFactory.createEmptyBorder(6, 8, 6, 8);

    // ---------------------------------------------------------------
    // Diagram type registry
    // ---------------------------------------------------------------

    private static final class DiagramTypeEntry {
        final String name;
        final String description;
        final Function<DiagramLayeredPane, CanvasComponentFactory> factoryFn;

        DiagramTypeEntry(String name, String description, Function<DiagramLayeredPane, CanvasComponentFactory> fn) {
            this.name = name;
            this.description = description;
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
        registerDiagramType(name, "", factoryFn);
    }

    public void registerDiagramType(String name, String description,
                                    Function<DiagramLayeredPane, CanvasComponentFactory> factoryFn) {
        DiagramTypeEntry entry = new DiagramTypeEntry(name, description, factoryFn);
        diagramTypes.add(entry);
        if (newDiagramTilesPanel != null) {
            addDiagramTypeTile(entry);
        }
    }

    private void addDiagramTypeTile(DiagramTypeEntry entry) {
        newDiagramTilesPanel.add(buildDiagramTypeTile(entry), FluidConstraint.FULLWIDTH);
        newDiagramTilesPanel.revalidate();
        newDiagramTilesPanel.repaint();
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

    private JButton addTabButton;
    private DiagramTabButton projectsTabBtn;
    private JPanel newDiagramTilesPanel;

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

        addProjectsTab();
        addTabCard(tabs.get(0));
        addTabButton = buildAddTabButton();
        diagramTabBar.add(addTabButton);
        selectProjectsTab();

        setLayout(new BorderLayout());
        add(diagramTabBar, BorderLayout.NORTH);
        add(diagramCardPanel, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------
    // Projects tab (leftmost, non-closable; hosts the New Diagram launcher)
    // ---------------------------------------------------------------

    private void addProjectsTab() {
        diagramCardPanel.add(buildProjectsPanel(), PROJECTS_CARD_KEY);

        projectsTabBtn = new DiagramTabButton("Projects...", FIXED_TAB_WIDTH);
        projectsTabBtn.addActionListener(e -> selectProjectsTab());
        diagramTabGroup.add(projectsTabBtn);
        diagramTabBar.add(projectsTabBtn);
    }

    private void selectProjectsTab() {
        diagramCardLayout.show(diagramCardPanel, PROJECTS_CARD_KEY);
        projectsTabBtn.setSelected(true);
    }

    /**
     * West: "New Diagram" launcher tiles, stacked one per row via
     * {@code FluidConstraint.FULLWIDTH} (same FluidLayout the file browser's
     * tiles use, just constrained to always be 1-per-row instead of wrapping),
     * scrolled — future-proofing for when the type list outgrows one screen,
     * though it's nowhere near that yet. Center: an embedded copy of the
     * "Open Diagram" file browser (see buildFileBrowserPanel()) so opening an
     * existing file doesn't require the separate modal dialog.
     */
    private JPanel buildProjectsPanel() {
        newDiagramTilesPanel = new JPanel(new FluidLayout(8, 8));
        newDiagramTilesPanel.setBorder(TILES_PANEL_BORDER);
        for (DiagramTypeEntry entry : diagramTypes) {
            newDiagramTilesPanel.add(buildDiagramTypeTile(entry), FluidConstraint.FULLWIDTH);
        }

        JScrollPane newDiagramScroll = new JScrollPane(newDiagramTilesPanel);
        newDiagramScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        newDiagramScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        newDiagramScroll.setPreferredSize(NEW_DIAGRAM_PANEL_SIZE);

        JPanel newDiagramPanel = new JPanel(new BorderLayout());
        newDiagramPanel.setBorder(BorderFactory.createTitledBorder("New Diagram"));
        newDiagramPanel.add(newDiagramScroll, BorderLayout.CENTER);

        JPanel openPanel = buildFileBrowserPanel(defaultDiagramDirectory(), this::openDiagramFile);
        openPanel.setBorder(BorderFactory.createTitledBorder("Open Diagram"));

        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBorder(DIALOG_CONTENT_BORDER);
        panel.add(newDiagramPanel, BorderLayout.WEST);
        panel.add(openPanel, BorderLayout.CENTER);
        return panel;
    }

    /** Visually mirrors buildFileTile()'s "Open Diagram" tiles: bold name, small description underneath. */
    private JPanel buildDiagramTypeTile(DiagramTypeEntry entry) {
        JPanel tile = new JPanel(new BorderLayout(4, 2));
        tile.setPreferredSize(NEW_DIAGRAM_TILE_SIZE);
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(separatorColor()),
            FILE_TILE_INNER_BORDER));

        JLabel nameLabel = new JLabel("<html><b>" + entry.name + "</b></html>");
        tile.add(nameLabel, BorderLayout.NORTH);

        if (entry.description != null && !entry.description.isEmpty()) {
            JLabel descLabel = new JLabel("<html><small>" + entry.description + "</small></html>");
            tile.add(descLabel, BorderLayout.CENTER);
        }

        Color defaultBg = tile.getBackground();
        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                createNewTab(entry.factoryFn);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Color sel = UIManager.getColor("List.selectionBackground");
                if (sel != null) {
                    tile.setBackground(sel);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tile.setBackground(defaultBg);
            }
        });

        return tile;
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
        JButton btn = new JButton("Open");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 11f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(ACCENT_COLOR);
        btn.setToolTipText("Open a diagram file from Documents");
        btn.addActionListener(e -> openFileBrowser());
        return btn;
    }

    // ---------------------------------------------------------------
    // Per-tab content factory
    // ---------------------------------------------------------------

    private DiagramTabContent createTabContent(String name) {
        DiagramLayeredPane pane = new DiagramLayeredPane();
        PropertyEditorPanel propEditor = new PropertyEditorPanel(pane);

        // --- Top half: domain-specific tabs (Types + Relationships) ---
        JPanel nodesPanel = new JPanel(new BorderLayout());
        // Populated below via updateRelationshipsPanel() once the tab exists — starts
        // as the generic picker (no factory yet) and is swapped to the UML preset list
        // if/when a class-diagram factory is assigned.
        JPanel relationshipsPanel = new JPanel(new BorderLayout());

        CardLayout topCardLayout = new CardLayout();
        JPanel topCardPanel = new JPanel(topCardLayout);
        topCardPanel.add(nodesPanel,        "types");
        topCardPanel.add(relationshipsPanel, "relationships");

        DiagramTabButton typesTabBtn = new DiagramTabButton("Types");
        DiagramTabButton relationsTabBtn = new DiagramTabButton("Relationships");
        typesTabBtn.addActionListener(e -> topCardLayout.show(topCardPanel, "types"));
        relationsTabBtn.addActionListener(e -> topCardLayout.show(topCardPanel, "relationships"));
        ButtonGroup topGroup = new ButtonGroup();
        topGroup.add(typesTabBtn);
        topGroup.add(relationsTabBtn);
        typesTabBtn.setSelected(true);

        JPanel topTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topTabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor()));
        topTabBar.add(typesTabBtn);
        topTabBar.add(relationsTabBtn);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(topTabBar,   BorderLayout.NORTH);
        topPanel.add(topCardPanel, BorderLayout.CENTER);

        // --- Bottom half: infrastructure tabs (Layers + Properties) ---
        JPanel layerListPanel = new JPanel();
        layerListPanel.setLayout(new BoxLayout(layerListPanel, BoxLayout.Y_AXIS));
        addLayerRows(layerListPanel, pane);
        JScrollPane layerScroll = new JScrollPane(layerListPanel);
        layerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        layerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        CardLayout bottomCardLayout = new CardLayout();
        JPanel bottomCardPanel = new JPanel(bottomCardLayout);
        bottomCardPanel.add(layerScroll, "layers");
        bottomCardPanel.add(propEditor,  "properties");

        DiagramTabButton layersTabBtn     = new DiagramTabButton("Layers");
        DiagramTabButton propertiesTabBtn = new DiagramTabButton("Properties");
        layersTabBtn.addActionListener(e -> bottomCardLayout.show(bottomCardPanel, "layers"));
        propertiesTabBtn.addActionListener(e -> bottomCardLayout.show(bottomCardPanel, "properties"));
        ButtonGroup bottomGroup = new ButtonGroup();
        bottomGroup.add(layersTabBtn);
        bottomGroup.add(propertiesTabBtn);
        propertiesTabBtn.setSelected(true);   // Properties is the default shown tab
        bottomCardLayout.show(bottomCardPanel, "properties");

        JPanel bottomTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomTabBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor()));
        bottomTabBar.add(layersTabBtn);
        bottomTabBar.add(propertiesTabBtn);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(bottomTabBar,   BorderLayout.NORTH);
        bottomPanel.add(bottomCardPanel, BorderLayout.CENTER);

        // --- Split pane: domain tools (top) over infrastructure (bottom) ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, bottomPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(5);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(RIGHT_PANEL_SIZE);
        rightPanel.add(splitPane, BorderLayout.CENTER);

        // --- Bottom details panel (Details / Documentation / Presentation) ---
        final int[] savedBottomH = {160};
        final boolean[] bottomOpen  = {true};

        CardLayout detailsCardLayout = new CardLayout();
        JPanel detailsCardPanel = new JPanel(detailsCardLayout);
        JPanel detailsContentPane = new JPanel(new BorderLayout());
        detailsCardPanel.add(detailsContentPane, "details");
        detailsCardPanel.add(new JPanel(), "documentation");
        detailsCardPanel.add(new JPanel(), "presentation");

        DiagramTabButton detailsBtn = new DiagramTabButton("Details");
        DiagramTabButton docsBtn    = new DiagramTabButton("Documentation");
        DiagramTabButton presentBtn = new DiagramTabButton("Presentation");
        detailsBtn.addActionListener(e -> detailsCardLayout.show(detailsCardPanel, "details"));
        docsBtn.addActionListener(e    -> detailsCardLayout.show(detailsCardPanel, "documentation"));
        presentBtn.addActionListener(e -> detailsCardLayout.show(detailsCardPanel, "presentation"));
        ButtonGroup detailsGroup = new ButtonGroup();
        detailsGroup.add(detailsBtn);
        detailsGroup.add(docsBtn);
        detailsGroup.add(presentBtn);
        detailsBtn.setSelected(true);

        JButton detailsCollapseBtn = new JButton("▼");
        detailsCollapseBtn.setFont(detailsCollapseBtn.getFont().deriveFont(Font.BOLD, 10f));
        detailsCollapseBtn.setFocusPainted(false);
        detailsCollapseBtn.setBorderPainted(false);
        detailsCollapseBtn.setContentAreaFilled(false);
        detailsCollapseBtn.setForeground(ACCENT_COLOR);
        detailsCollapseBtn.setToolTipText("Collapse details panel");

        JPanel detailsBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        detailsBtnRow.add(detailsBtn);
        detailsBtnRow.add(docsBtn);
        detailsBtnRow.add(presentBtn);

        JPanel detailsTabBar = new JPanel(new BorderLayout());
        detailsTabBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, separatorColor()));
        detailsTabBar.add(detailsBtnRow, BorderLayout.CENTER);
        detailsTabBar.add(detailsCollapseBtn, BorderLayout.EAST);

        JPanel detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setPreferredSize(DETAILS_PANEL_SIZE);
        detailsPanel.setMinimumSize(COLLAPSED_PANEL_SIZE);
        detailsPanel.add(detailsTabBar,   BorderLayout.NORTH);
        detailsPanel.add(detailsCardPanel, BorderLayout.CENTER);

        JSplitPane canvasSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(pane), detailsPanel);
        canvasSplit.setResizeWeight(0.75);
        canvasSplit.setDividerSize(5);

        detailsCollapseBtn.addActionListener(e -> {
            if (bottomOpen[0]) {
                savedBottomH[0] = canvasSplit.getHeight()
                    - canvasSplit.getDividerLocation() - canvasSplit.getDividerSize();
                canvasSplit.setDividerLocation(
                    canvasSplit.getHeight() - COLLAPSED_BOTTOM_HEIGHT - canvasSplit.getDividerSize());
                detailsCollapseBtn.setText("▲");
                detailsCollapseBtn.setToolTipText("Expand details panel");
                bottomOpen[0] = false;
            } else {
                canvasSplit.setDividerLocation(Math.max(0,
                    canvasSplit.getHeight() - savedBottomH[0] - canvasSplit.getDividerSize()));
                detailsCollapseBtn.setText("▼");
                detailsCollapseBtn.setToolTipText("Collapse details panel");
                bottomOpen[0] = true;
            }
        });

        ToolBarBundle toolBarBundle = buildToolBar(pane);
        JPanel card = new JPanel(new BorderLayout());
        card.add(toolBarBundle.toolBar, BorderLayout.NORTH);
        card.add(canvasSplit,        BorderLayout.CENTER);
        card.add(rightPanel,         BorderLayout.EAST);

        DiagramTabContent tab = new DiagramTabContent(
            name, pane, card, propEditor, typesTabBtn, nodesPanel, relationshipsPanel, detailsContentPane);
        tab.saveBtn = toolBarBundle.saveBtn;
        tab.themeCombo = toolBarBundle.themeCombo;
        wireTabListeners(tab);
        updateRelationshipsPanel(tab);
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

                // Property editor (right panel)
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
                } else {
                    tab.propertyEditor.setSelectedComponent(component);
                }

                // Details tab (bottom panel)
                Runnable onCommit = () -> {
                    Runnable onMod = () -> tab.diagramPane.notifyModified();
                    JPanel newContent = tab.factory.createContentFor(
                        node.getNodeType(), node.getProperties(), onMod);
                    node.swapContent(newContent);
                    tab.diagramPane.notifyModified();
                };
                JPanel detailsPanel = tab.factory.createDetailsPanelFor(
                    node.getNodeType(), node.getProperties(), onCommit);
                tab.detailsPane.removeAll();
                if (detailsPanel != null) {
                    tab.detailsPane.add(detailsPanel, BorderLayout.CENTER);
                }
                tab.detailsPane.revalidate();
                tab.detailsPane.repaint();
                return;
            }
            tab.propertyEditor.setSelectedComponent(component);
            tab.detailsPane.removeAll();
            tab.detailsPane.revalidate();
            tab.detailsPane.repaint();
        });
        tab.diagramPane.setEdgeSelectionListener(edge -> {
            if (edge != null) {
                tab.propertyEditor.showNodeEditor(buildEdgePropertyEditor(edge, tab));
                if (tab.syncRelationshipSelection != null) {
                    tab.syncRelationshipSelection.accept(edge.getEdgeType());
                }
            } else {
                tab.propertyEditor.setSelectedComponent(null);
            }
            tab.detailsPane.removeAll();
            tab.detailsPane.revalidate();
            tab.detailsPane.repaint();
        });
    }

    private JPanel buildEdgePropertyEditor(GraphEdge edge, DiagramTabContent tab) {
        EdgeAttributes attrs = edge.getAttributes();

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(EDGE_EDITOR_BORDER);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Edge");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        form.add(title);

        String edgeType = edge.getEdgeType();
        String typeText = (edgeType != null && !edgeType.isEmpty()) ? edgeType : "(none)";
        JLabel typeLabel = new JLabel("Type: " + typeText);
        typeLabel.setForeground(Color.GRAY);
        form.add(typeLabel);
        form.add(Box.createVerticalStrut(8));

        // Line style
        JLabel lineLabel = new JLabel("Line style:");
        lineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lineLabel);

        JPanel lineRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        lineRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup lineGroup = new ButtonGroup();
        JRadioButton solidBtn = new JRadioButton("Solid",
                attrs.getLineStyle() == EdgeAttributes.LineStyle.SOLID);
        JRadioButton dashedBtn = new JRadioButton("Dashed",
                attrs.getLineStyle() == EdgeAttributes.LineStyle.DASHED);
        lineGroup.add(solidBtn);
        lineGroup.add(dashedBtn);
        lineRow.add(solidBtn);
        lineRow.add(dashedBtn);
        solidBtn.addActionListener(e -> {
            attrs.setLineStyle(EdgeAttributes.LineStyle.SOLID);
            tab.diagramPane.repaint();
            tab.diagramPane.notifyModified();
        });
        dashedBtn.addActionListener(e -> {
            attrs.setLineStyle(EdgeAttributes.LineStyle.DASHED);
            tab.diagramPane.repaint();
            tab.diagramPane.notifyModified();
        });
        form.add(lineRow);
        form.add(Box.createVerticalStrut(6));

        // Arrow type
        JLabel arrowLabel = new JLabel("Arrow:");
        arrowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(arrowLabel);

        JPanel arrowRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        arrowRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ButtonGroup arrowGroup = new ButtonGroup();
        JRadioButton noneBtn   = new JRadioButton("None",
                attrs.getArrowType() == EdgeAttributes.ArrowType.NONE);
        JRadioButton openBtn   = new JRadioButton("Open",
                attrs.getArrowType() == EdgeAttributes.ArrowType.OPEN);
        JRadioButton filledBtn = new JRadioButton("Filled",
                attrs.getArrowType() == EdgeAttributes.ArrowType.FILLED);
        arrowGroup.add(noneBtn);
        arrowGroup.add(openBtn);
        arrowGroup.add(filledBtn);
        arrowRow.add(noneBtn);
        arrowRow.add(openBtn);
        arrowRow.add(filledBtn);
        noneBtn.addActionListener(e -> {
            attrs.setArrowType(EdgeAttributes.ArrowType.NONE);
            tab.diagramPane.repaint();
            tab.diagramPane.notifyModified();
        });
        openBtn.addActionListener(e -> {
            attrs.setArrowType(EdgeAttributes.ArrowType.OPEN);
            tab.diagramPane.repaint();
            tab.diagramPane.notifyModified();
        });
        filledBtn.addActionListener(e -> {
            attrs.setArrowType(EdgeAttributes.ArrowType.FILLED);
            tab.diagramPane.repaint();
            tab.diagramPane.notifyModified();
        });
        form.add(arrowRow);
        form.add(Box.createVerticalStrut(8));

        // Text labels — auto-positioned at render time (center, and near each end)
        form.add(buildEdgeLabelRow("Label:", edge, "label", tab));
        form.add(Box.createVerticalStrut(4));
        form.add(buildEdgeLabelRow("Source label:", edge, "sourceLabel", tab));
        form.add(Box.createVerticalStrut(4));
        form.add(buildEdgeLabelRow("Target label:", edge, "targetLabel", tab));

        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    /**
     * One labeled text field bound to an edge's {@code properties} map, committing
     * on focus-lost — same pattern as {@code GenericGraphFactory}'s node label field.
     */
    private JPanel buildEdgeLabelRow(String labelText, GraphEdge edge, String propertyKey, DiagramTabContent tab) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.add(new JLabel(labelText), BorderLayout.WEST);

        Map<String, Object> properties = edge.getProperties();
        JTextField field = new JTextField((String) properties.getOrDefault(propertyKey, ""));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String val = field.getText().trim();
                if (!val.equals(properties.getOrDefault(propertyKey, ""))) {
                    properties.put(propertyKey, val);
                    tab.diagramPane.repaint();
                    tab.diagramPane.notifyModified();
                }
            }
        });
        field.addActionListener(e -> field.transferFocus());
        row.add(field, BorderLayout.CENTER);

        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
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

        JPanel tabWrapper = wrapTabWithCloseButton(btn, tab);

        int insertIdx = diagramTabBar.getComponentCount();
        if (insertIdx > 0 && diagramTabBar.getComponent(insertIdx - 1) == addTabButton) {
            insertIdx--;  // insert before the "+" button
        }
        diagramTabBar.add(tabWrapper, insertIdx);
        btn.setSelected(true);
        diagramTabBar.revalidate();
        diagramTabBar.repaint();
    }

    private JPanel wrapTabWithCloseButton(DiagramTabButton tabBtn, DiagramTabContent tab) {
        JButton closeBtn = new JButton("×");
        closeBtn.setFont(closeBtn.getFont().deriveFont(Font.BOLD, 12f));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setMargin(new java.awt.Insets(0, 2, 0, 6));
        closeBtn.setToolTipText("Close diagram");
        closeBtn.addActionListener(e -> closeTab(tab));

        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapper.setOpaque(false);
        wrapper.add(tabBtn);
        wrapper.add(closeBtn);
        return wrapper;
    }

    private void closeTab(DiagramTabContent tab) {
        int option = JOptionPane.showConfirmDialog(
            this,
            "Save changes to \"" + tab.displayName + "\" before closing?",
            "Close Diagram",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
            return;
        }
        if (option == JOptionPane.YES_OPTION) {
            if (tab.sourceFile != null) {
                quickSaveDiagram(tab.diagramPane);
            } else {
                saveDiagram(tab.diagramPane);
            }
        }
        removeTab(tab);
    }

    private void removeTab(DiagramTabContent tab) {
        diagramCardPanel.remove(tab.tabCard);
        diagramTabBar.remove(tab.tabBtn.getParent());
        diagramTabGroup.remove(tab.tabBtn);
        tabs.remove(tab);

        if (tabs.isEmpty()) {
            createNewTab(pane -> null);
        } else if (activeTab == tab) {
            DiagramTabContent fallback = tabs.get(tabs.size() - 1);
            selectTab(fallback);
            fallback.tabBtn.setSelected(true);
        }

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

    private void createNewTab(Function<DiagramLayeredPane, CanvasComponentFactory> factoryFn) {
        String name = "Diagram " + (tabs.size() + 1);
        DiagramTabContent tab = createTabContent(name);
        CanvasComponentFactory factory = factoryFn.apply(tab.diagramPane);
        if (factory != null) {
            tab.factory = factory;
            tab.diagramPane.setComponentFactory(factory);
            updateNodesPanel(tab);
            updateRelationshipsPanel(tab);
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
            tab.nodesTabButton.setText("Types");
        }
        tab.nodesPanel.revalidate();
        tab.nodesPanel.repaint();
    }

    // ---------------------------------------------------------------
    // Relationships panel
    // ---------------------------------------------------------------

    /**
     * Rebuilds the Relationships tab content for the current domain type: the
     * factory's named preset list (e.g. UML relationships for class diagrams) if
     * it declares one, or the generic line-style + per-end arrow picker
     * otherwise (plain diagrams, or any factory that opts out). Called whenever
     * a tab's factory is assigned or changes.
     */
    private void updateRelationshipsPanel(DiagramTabContent tab) {
        tab.relationshipsPanel.removeAll();
        tab.syncRelationshipSelection = null; // no tiles yet; UML branch below re-sets it
        List<RelationshipType> presets = (tab.factory != null)
            ? tab.factory.getRelationshipPresets()
            : null;
        JPanel content = (presets != null && !presets.isEmpty())
            ? buildUmlRelationshipsPanel(tab, presets)
            : buildGenericRelationshipsPanel(tab.diagramPane);
        tab.relationshipsPanel.add(content, BorderLayout.CENTER);
        tab.relationshipsPanel.revalidate();
        tab.relationshipsPanel.repaint();
    }

    private JPanel buildUmlRelationshipsPanel(DiagramTabContent tab, List<RelationshipType> presets) {
        DiagramLayeredPane pane = tab.diagramPane;
        final List<RelationshipControlPanel> tiles = new ArrayList<>();

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        Consumer<RelationshipControlPanel> onSelected = selected -> {
            for (RelationshipControlPanel tile : tiles) {
                tile.setActive(tile == selected);
            }
            RelationshipType rt = selected.getRelationshipType();
            pane.applyRelationship(rt.attributes, rt.name);
        };

        for (RelationshipType rt : presets) {
            RelationshipControlPanel tile = new RelationshipControlPanel(rt, onSelected);
            tiles.add(tile);
            listPanel.add(tile);
            listPanel.add(Box.createVerticalStrut(1));
        }

        // Display-only sync, called on edge selection with the selected edge's type —
        // highlights the matching tile (or none, if no tile's name matches). Never
        // calls applyRelationship(), so merely selecting an edge can't overwrite its
        // attributes or change the active type for the next new edge.
        tab.syncRelationshipSelection = typeName -> {
            for (RelationshipControlPanel tile : tiles) {
                tile.setActive(tile.getRelationshipType().name.equals(typeName));
            }
        };

        // Pre-select the first preset as the default active relationship
        if (!tiles.isEmpty()) {
            tiles.get(0).setActive(true);
            RelationshipType defaultRt = tiles.get(0).getRelationshipType();
            pane.applyRelationship(defaultRt.attributes, defaultRt.name);
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(RELATIONSHIPS_BORDER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Generic relationship picker used whenever the active factory has no named
     * presets (plain diagrams today; any future factory that doesn't override
     * {@code getRelationshipPresets()}): a line-style row (Solid / Dashed)
     * followed by independent Left Arrow / Right Arrow rows, each offering all
     * five {@code EdgeAttributes.ArrowType} values. Left maps to the edge's
     * source end, Right to its target end, matching the left/right convention
     * already used by RelationshipControlPanel's preview painting.
     */
    private JPanel buildGenericRelationshipsPanel(DiagramLayeredPane pane) {
        EdgeAttributes attrs = new EdgeAttributes();
        attrs.setLineStyle(EdgeAttributes.LineStyle.SOLID);
        attrs.setArrowType(EdgeAttributes.ArrowType.NONE);
        attrs.setSourceArrowType(EdgeAttributes.ArrowType.NONE);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // --- Line style ---
        JLabel lineLabel = new JLabel("Line Style");
        lineLabel.setFont(lineLabel.getFont().deriveFont(Font.BOLD, 11f));
        lineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lineLabel);
        content.add(Box.createVerticalStrut(4));

        JPanel lineRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        lineRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        final List<LineStyleButton> lineButtons = new ArrayList<>();
        Consumer<LineStyleButton> onLineSelected = selected -> {
            for (LineStyleButton b : lineButtons) {
                b.setActive(b == selected);
            }
            attrs.setLineStyle(selected.getLineStyleValue());
            pane.applyRelationship(attrs);
        };
        LineStyleButton solidBtn  = new LineStyleButton(EdgeAttributes.LineStyle.SOLID,  "Solid line",  onLineSelected);
        LineStyleButton dashedBtn = new LineStyleButton(EdgeAttributes.LineStyle.DASHED, "Dashed line", onLineSelected);
        lineButtons.add(solidBtn);
        lineButtons.add(dashedBtn);
        lineRow.add(solidBtn);
        lineRow.add(dashedBtn);
        content.add(lineRow);
        content.add(Box.createVerticalStrut(10));

        // --- Left arrow (source end) / Right arrow (target end) ---
        content.add(buildArrowRow("Left Arrow", false, attrs, pane,
            (a, type) -> a.setSourceArrowType(type)));
        content.add(Box.createVerticalStrut(10));
        content.add(buildArrowRow("Right Arrow", true, attrs, pane,
            (a, type) -> a.setArrowType(type)));

        // Pre-select defaults (Solid / None / None) and apply once, after all
        // three rows exist, so a single applyRelationship() call covers the lot.
        solidBtn.setActive(true);
        pane.applyRelationship(attrs);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(RELATIONSHIPS_BORDER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildArrowRow(String label, boolean pointRight, EdgeAttributes attrs,
            DiagramLayeredPane pane, BiConsumer<EdgeAttributes, EdgeAttributes.ArrowType> setter) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rowLabel = new JLabel(label);
        rowLabel.setFont(rowLabel.getFont().deriveFont(Font.BOLD, 11f));
        rowLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.add(rowLabel);
        section.add(Box.createVerticalStrut(4));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        final List<ArrowEndButton> buttons = new ArrayList<>();
        Consumer<ArrowEndButton> onSelected = selected -> {
            for (ArrowEndButton b : buttons) {
                b.setActive(b == selected);
            }
            setter.accept(attrs, selected.getArrowTypeValue());
            pane.applyRelationship(attrs);
        };

        EdgeAttributes.ArrowType[] types = {
            EdgeAttributes.ArrowType.NONE,
            EdgeAttributes.ArrowType.OPEN,
            EdgeAttributes.ArrowType.FILLED,
            EdgeAttributes.ArrowType.OPEN_DIAMOND,
            EdgeAttributes.ArrowType.FILLED_DIAMOND
        };
        String[] tooltips = { "None", "Open", "Filled", "Open Diamond", "Filled Diamond" };

        ArrowEndButton defaultBtn = null;
        for (int i = 0; i < types.length; i++) {
            ArrowEndButton btn = new ArrowEndButton(types[i], pointRight, tooltips[i], onSelected);
            buttons.add(btn);
            row.add(btn);
            if (types[i] == EdgeAttributes.ArrowType.NONE) {
                defaultBtn = btn;
            }
        }
        if (defaultBtn != null) {
            defaultBtn.setActive(true);
        }

        section.add(row);
        return section;
    }

    // ---------------------------------------------------------------
    // Per-tab toolbar
    // ---------------------------------------------------------------

    /** Bundles the toolbar with the handful of buttons other methods need to reach later. */
    private static final class ToolBarBundle {
        final JToolBar toolBar;
        final JButton saveBtn;
        final JComboBox<String> themeCombo;

        ToolBarBundle(JToolBar toolBar, JButton saveBtn, JComboBox<String> themeCombo) {
            this.toolBar = toolBar;
            this.saveBtn = saveBtn;
            this.themeCombo = themeCombo;
        }
    }

    private ToolBarBundle buildToolBar(DiagramLayeredPane pane) {
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

        tb.addSeparator();

        tb.add(new JLabel("Theme:"));
        JComboBox<String> themeCombo = new JComboBox<>(CanvasThemeRegistry.names());
        themeCombo.setSelectedItem(pane.getTheme().getThemeName());
        themeCombo.addActionListener(e -> {
            CanvasTheme selected = CanvasThemeRegistry.byName((String) themeCombo.getSelectedItem());
            if (selected != null) {
                pane.setTheme(selected);
            }
        });
        tb.add(themeCombo);

        tb.addSeparator();

        JButton bringForwardBtn = new JButton("Bring Forward");
        bringForwardBtn.addActionListener(e -> pane.bringSelectedForward());
        tb.add(bringForwardBtn);

        JButton sendBackBtn = new JButton("Send Back");
        sendBackBtn.addActionListener(e -> pane.sendSelectedBack());
        tb.add(sendBackBtn);

        tb.addSeparator();

        JButton alignBtn = new JButton("Align / Distribute");
        alignBtn.addActionListener(e -> showAlignMenu(pane, alignBtn));
        tb.add(alignBtn);

        tb.addSeparator();

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> pane.deleteSelected());
        tb.add(deleteBtn);

        tb.addSeparator();

        JButton loadBtn = new JButton("Load");
        loadBtn.addActionListener(e -> loadDiagram(pane));
        tb.add(loadBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.setEnabled(false);   // enabled once the tab has a source file (loaded or Saved As)
        saveBtn.addActionListener(e -> quickSaveDiagram(pane));
        tb.add(saveBtn);

        JButton saveAsBtn = new JButton("Save as...");
        saveAsBtn.addActionListener(e -> saveDiagram(pane));
        tb.add(saveAsBtn);

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

        return new ToolBarBundle(tb, saveBtn, themeCombo);
    }

    /** Popup menu of align/distribute operations; items disable themselves based on selection size. */
    private void showAlignMenu(DiagramLayeredPane pane, Component invoker) {
        int count = pane.getSelectedComponents().size();
        boolean canAlign = count >= 2;
        boolean canDistribute = count >= 3;

        JPopupMenu menu = new JPopupMenu();
        addAlignMenuItem(menu, "Align Left",   pane, DiagramLayeredPane.Alignment.LEFT,             canAlign);
        addAlignMenuItem(menu, "Align Center", pane, DiagramLayeredPane.Alignment.CENTER_HORIZONTAL, canAlign);
        addAlignMenuItem(menu, "Align Right",  pane, DiagramLayeredPane.Alignment.RIGHT,             canAlign);
        menu.addSeparator();
        addAlignMenuItem(menu, "Align Top",    pane, DiagramLayeredPane.Alignment.TOP,             canAlign);
        addAlignMenuItem(menu, "Align Middle", pane, DiagramLayeredPane.Alignment.MIDDLE_VERTICAL, canAlign);
        addAlignMenuItem(menu, "Align Bottom", pane, DiagramLayeredPane.Alignment.BOTTOM,           canAlign);
        menu.addSeparator();

        JMenuItem distributeH = new JMenuItem("Distribute Horizontally");
        distributeH.setEnabled(canDistribute);
        distributeH.addActionListener(e -> pane.distributeSelected(true));
        menu.add(distributeH);

        JMenuItem distributeV = new JMenuItem("Distribute Vertically");
        distributeV.setEnabled(canDistribute);
        distributeV.addActionListener(e -> pane.distributeSelected(false));
        menu.add(distributeV);

        menu.show(invoker, 0, invoker.getHeight());
    }

    private void addAlignMenuItem(JPopupMenu menu, String label, DiagramLayeredPane pane,
            DiagramLayeredPane.Alignment alignment, boolean enabled) {
        JMenuItem item = new JMenuItem(label);
        item.setEnabled(enabled);
        item.addActionListener(e -> pane.alignSelected(alignment));
        menu.add(item);
    }

    // ---------------------------------------------------------------
    // File browser — opens a diagram file into a new tab
    // ---------------------------------------------------------------

    /** Prefers OneDrive\Documents; falls back to plain Documents if absent. */
    private static java.io.File defaultDiagramDirectory() {
        java.io.File dir = new java.io.File(
            System.getProperty("user.home"),
            "OneDrive" + java.io.File.separator + "Documents");
        if (!dir.exists()) {
            dir = new java.io.File(System.getProperty("user.home"), "Documents");
        }
        return dir;
    }

    private void openFileBrowser() {
        JDialog dialog = new JDialog(
            SwingUtilities.getWindowAncestor(this),
            "Open Diagram  (*.dgx, *.json)", Dialog.ModalityType.APPLICATION_MODAL);

        JPanel content = buildFileBrowserPanel(defaultDiagramDirectory(), f -> {
            dialog.dispose();
            openDiagramFile(f);
        });

        dialog.add(content);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Directory field + scrollable file tiles, shared by the modal "Open Diagram"
     * dialog and the Projects tab's embedded copy. Directory contents are read once
     * on build and again only when the user re-enters the directory field (Enter) —
     * no polling, matching how the dialog already behaved.
     */
    private JPanel buildFileBrowserPanel(java.io.File initialDir, Consumer<java.io.File> onFileChosen) {
        JPanel tilesPanel = new JPanel(new FluidLayout(8, 8));
        tilesPanel.setBorder(TILES_PANEL_BORDER);

        JScrollPane scroll = new JScrollPane(tilesPanel);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(FILE_BROWSER_SIZE);

        javax.swing.JTextField dirField = new javax.swing.JTextField(
            initialDir.getAbsolutePath());
        dirField.setFont(dirField.getFont().deriveFont(Font.BOLD));
        dirField.addActionListener(e -> {
            java.io.File chosen = new java.io.File(dirField.getText().trim());
            populateTiles(tilesPanel, chosen, onFileChosen);
            tilesPanel.revalidate();
            tilesPanel.repaint();
            scroll.getVerticalScrollBar().setValue(0);
        });

        populateTiles(tilesPanel, initialDir, onFileChosen);

        JPanel headerRow = new JPanel(new BorderLayout(6, 0));
        headerRow.setBorder(HEADER_ROW_BORDER);
        headerRow.add(new JLabel("Directory:"), BorderLayout.WEST);
        headerRow.add(dirField, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBorder(DIALOG_CONTENT_BORDER);
        content.add(headerRow, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    private void populateTiles(JPanel tilesPanel, java.io.File dir, Consumer<java.io.File> onFileChosen) {
        tilesPanel.removeAll();

        // xs=12 (1/row), sm=6 (2/row), med=4 (3/row), lg=3 (4/row), xl=3 (4/row)
        FluidConstraint tileConstraint = new FluidConstraint(12, 6, 4, 3, 3);

        if (!dir.exists() || !dir.isDirectory()) {
            JLabel msg = new JLabel(
                "<html><i>Directory not found:<br>" + dir.getAbsolutePath() + "</i></html>");
            msg.setBorder(FILE_PLACEHOLDER_BORDER);
            tilesPanel.add(msg, FluidConstraint.FULLWIDTH);
            return;
        }

        java.io.File[] found = dir.listFiles((d, n) ->
            n.endsWith(".dgx") || n.endsWith(".json"));
        java.io.File[] diagrams = (found != null) ? found : new java.io.File[0];
        Arrays.sort(diagrams, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        if (diagrams.length > 0) {
            for (java.io.File f : diagrams) {
                tilesPanel.add(buildFileTile(f, onFileChosen), tileConstraint);
            }
        } else {
            JLabel empty = new JLabel(
                "<html><i>No diagram files (.dgx / .json) found.</i></html>");
            empty.setBorder(FILE_PLACEHOLDER_BORDER);
            tilesPanel.add(empty, FluidConstraint.FULLWIDTH);
        }
    }

    private JPanel buildFileTile(java.io.File f, Consumer<java.io.File> onFileChosen) {
        JPanel tile = new JPanel(new BorderLayout(4, 2));
        tile.setPreferredSize(FILE_TILE_SIZE);
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(separatorColor()),
            FILE_TILE_INNER_BORDER));

        String fullName  = f.getName();
        int dot          = fullName.lastIndexOf('.');
        String baseName  = dot > 0 ? fullName.substring(0, dot) : fullName;
        String ext       = dot > 0 ? fullName.substring(dot + 1).toUpperCase() : "";
        long   kb        = Math.max(1, f.length() / 1024);
        String date      = new java.text.SimpleDateFormat("yyyy-MM-dd").format(
            new java.util.Date(f.lastModified()));

        JLabel nameLabel = new JLabel("<html><b>" + baseName + "</b></html>");
        JLabel infoLabel = new JLabel(
            "<html><small>" + ext + "  •  " + kb + " KB<br>" + date + "</small></html>");

        tile.add(nameLabel, BorderLayout.NORTH);
        tile.add(infoLabel, BorderLayout.CENTER);

        Color defaultBg = tile.getBackground();
        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onFileChosen.accept(f);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                Color sel = UIManager.getColor("List.selectionBackground");
                if (sel != null) {
                    tile.setBackground(sel);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                tile.setBackground(defaultBg);
            }
        });

        return tile;
    }

    private void openDiagramFile(java.io.File file) {
        String name = "Diagram " + (tabs.size() + 1);
        DiagramTabContent tab = createTabContent(name);
        preApplyFactory(file, tab.diagramPane, tab);
        if (tab.factory != null) {
            updateNodesPanel(tab);
            updateRelationshipsPanel(tab);
        }
        tabs.add(tab);
        addTabCard(tab);
        selectTab(tab);
        try {
            tab.diagramPane.loadDiagram(file);
            tab.sourceFile = file;
            tab.saveBtn.setEnabled(true);
            updateTabDisplayName(tab, fileBaseName(file.getName()));
            syncThemeAfterLoad(tab);
            setModified(false);   // last: syncThemeAfterLoad's combo sync can itself trigger a spurious modification
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error loading diagram: " + ex.getMessage(),
                "Load Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * Reflects the pane's post-load theme in the toolbar selector, and warns the user
     * if the file referenced a theme name that could not be resolved (the pane keeps
     * whatever theme it already had — effectively the default for a fresh tab).
     */
    private void syncThemeAfterLoad(DiagramTabContent tab) {
        tab.themeCombo.setSelectedItem(tab.diagramPane.getTheme().getThemeName());
        String themeWarning = tab.diagramPane.getAndClearThemeWarning();
        if (themeWarning != null) {
            JOptionPane.showMessageDialog(this,
                "Theme \"" + themeWarning + "\" was not found. Loaded the diagram with the default theme instead.",
                "Theme Not Found", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ---------------------------------------------------------------
    // Save / load (per-tab pane)
    // ---------------------------------------------------------------

    private void quickSaveDiagram(DiagramLayeredPane pane) {
        DiagramTabContent tab = findTabForPane(pane);
        if (tab == null || tab.sourceFile == null) {
            return;
        }
        try {
            pane.setDomainType(tab.factory != null ? tab.factory.getDomainTypeId() : null);
            pane.saveDiagram(tab.sourceFile);
            setModified(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving diagram: " + ex.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void saveDiagram(DiagramLayeredPane pane) {
        DiagramTabContent tab = findTabForPane(pane);

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Diagram");
        fc.setFileFilter(new FileNameExtensionFilter("Diagram files (*.dgx)", "dgx"));
        if (tab != null && tab.sourceFile != null) {
            fc.setCurrentDirectory(tab.sourceFile.getParentFile());
            fc.setSelectedFile(tab.sourceFile);
        } else {
            fc.setSelectedFile(new java.io.File("diagram.dgx"));
        }

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fc.getSelectedFile();
            String n = file.getName();
            if (!n.endsWith(".dgx") && !n.endsWith(".json")) {
                file = new java.io.File(file.getParentFile(), n + ".dgx");
            }
            if (file.exists()) {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "\"" + file.getName() + "\" already exists. Overwrite?",
                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            try {
                pane.setDomainType(tab != null && tab.factory != null
                    ? tab.factory.getDomainTypeId() : null);
                pane.saveDiagram(file);
                setModified(false);
                if (tab != null) {
                    tab.sourceFile = file;
                    tab.saveBtn.setEnabled(true);
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
                if (tab != null) {
                    tab.sourceFile = file;
                    tab.saveBtn.setEnabled(true);
                    updateTabDisplayName(tab, fileBaseName(file.getName()));
                    updateNodesPanel(tab);
                    updateRelationshipsPanel(tab);
                    syncThemeAfterLoad(tab);
                }
                setModified(false);   // last: syncThemeAfterLoad's combo sync can itself trigger a spurious modification
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
        updateRelationshipsPanel(activeTab);
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
