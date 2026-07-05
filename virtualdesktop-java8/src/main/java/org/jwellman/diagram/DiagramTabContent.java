package org.jwellman.diagram;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jwellman.diagram.api.CanvasComponentFactory;

/**
 * Bundles the per-tab state for one diagram editor instance.
 * Package-private — only LayeredDiagramTool needs direct access.
 */
class DiagramTabContent {

    final String name;                   // stable CardLayout key; never changes
    String displayName;                  // label shown on the tab button; updated on save/load
    final DiagramLayeredPane diagramPane;
    final JPanel tabCard;                // full card: toolbar + canvas + right panel
    final PropertyEditorPanel propertyEditor;
    final JToggleButton nodesTabButton;
    final JPanel nodesPanel;
    final JPanel detailsPane;            // content area of the Details tab; populated on node selection
    CanvasComponentFactory factory;
    DiagramTabButton tabBtn;             // diagram tab bar button; set by LayeredDiagramTool.addTabCard()
    java.io.File sourceFile;             // non-null when this tab was loaded from or saved to a file

    DiagramTabContent(String name, DiagramLayeredPane diagramPane, JPanel tabCard,
                      PropertyEditorPanel propertyEditor,
                      JToggleButton nodesTabButton, JPanel nodesPanel,
                      JPanel detailsPane) {
        this.name = name;
        this.displayName = name;
        this.diagramPane = diagramPane;
        this.tabCard = tabCard;
        this.propertyEditor = propertyEditor;
        this.nodesTabButton = nodesTabButton;
        this.nodesPanel = nodesPanel;
        this.detailsPane = detailsPane;
    }
}
