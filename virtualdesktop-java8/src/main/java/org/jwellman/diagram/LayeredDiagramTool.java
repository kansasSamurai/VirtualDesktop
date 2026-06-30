package org.jwellman.diagram;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.jwellman.diagram.api.CanvasComponentFactory;

/**
 * Complete diagram tool using JLayeredPane with grid, layers, and drag-and-drop
 */
public class LayeredDiagramTool extends JPanel {

    private JPanel layerPanel;
    private JToolBar toolBar;
    private DiagramLayeredPane diagramPane;
    private PropertyEditorPanel propertyEditor;
    private boolean modified = false;

    private static final long serialVersionUID = 1L;

    public LayeredDiagramTool() {
        createDiagramPane();
        createToolBar();
        createPropertyEditor();
        createLayerPanel();

        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(diagramPane), BorderLayout.CENTER);
        add(layerPanel, BorderLayout.EAST);

        // Set up modification listener
        diagramPane.setModificationListener(() -> setModified(true));

        // Set up selection listener to update property editor
        diagramPane.setSelectionListener(component -> {
            propertyEditor.setSelectedComponent(component);
        });
    }

    private void createPropertyEditor() {
        propertyEditor = new PropertyEditorPanel(diagramPane);
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

        toolBar.addSeparator();

        JToggleButton connectBtn = new JToggleButton("Connect");
        connectBtn.addActionListener(e -> {
            if (connectBtn.isSelected()) {
                diagramPane.enterEdgeCreationMode();
            } else {
                diagramPane.exitEdgeCreationMode();
            }
        });
        toolBar.add(connectBtn);
    }

    public void setComponentFactory(CanvasComponentFactory factory) {
        diagramPane.setComponentFactory(factory);
    }

    public DiagramLayeredPane getDiagramPane() {
        return diagramPane;
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

    private void loadDiagram() {
        // Check for unsaved changes before loading
        if (!checkUnsavedChanges()) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Diagram");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                diagramPane.loadDiagram(file);
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

    private void createLayerPanel() {
        layerPanel = new JPanel();
        layerPanel.setLayout(new BorderLayout());
        layerPanel.setPreferredSize(new Dimension(280, 0));

        // Create layer controls section
        JPanel layersSection = new JPanel(new BorderLayout());
        layersSection.setBorder(BorderFactory.createTitledBorder("Layers"));

        // Create scrollable layer list
        JPanel layerListPanel = new JPanel();
        layerListPanel.setLayout(new BoxLayout(layerListPanel, BoxLayout.Y_AXIS));

        // Add layer controls for each defined layer (from top to bottom)
        addLayerControl(layerListPanel, "Overlay Layer", DiagramLayeredPane.OVERLAY_LAYER);
        addLayerControl(layerListPanel, "Connection Layer", DiagramLayeredPane.CONNECTION_LAYER);
        addLayerControl(layerListPanel, "Text Layer", DiagramLayeredPane.TEXT_LAYER);
        addLayerControl(layerListPanel, "Shape Layer", DiagramLayeredPane.SHAPE_LAYER);
        addLayerControl(layerListPanel, "Background Layer", DiagramLayeredPane.BACKGROUND_LAYER);
        addLayerControl(layerListPanel, "Grid Layer", DiagramLayeredPane.GRID_LAYER);

        JScrollPane layerScrollPane = new JScrollPane(layerListPanel);
        layerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        layerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        layerScrollPane.setPreferredSize(new Dimension(250, 200));

        layersSection.add(layerScrollPane, BorderLayout.CENTER);

        // Add sections to main panel
        layerPanel.add(layersSection, BorderLayout.NORTH);
        layerPanel.add(propertyEditor, BorderLayout.CENTER);
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
                saveDiagram();
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
