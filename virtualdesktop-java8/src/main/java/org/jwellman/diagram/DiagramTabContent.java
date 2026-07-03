package org.jwellman.diagram;

import javax.swing.JPanel;

import org.jwellman.diagram.api.CanvasComponentFactory;

/**
 * Bundles the per-tab state for one diagram editor instance.
 * Package-private — only LayeredDiagramTool needs direct access.
 */
class DiagramTabContent {

    final String name;
    final DiagramLayeredPane diagramPane;
    final JPanel tabCard;        // BorderLayout: NORTH=toolbar, CENTER=JScrollPane(diagramPane)
    CanvasComponentFactory factory;  // null until setComponentFactory() is called

    DiagramTabContent(String name, DiagramLayeredPane diagramPane, JPanel tabCard) {
        this.name = name;
        this.diagramPane = diagramPane;
        this.tabCard = tabCard;
    }
}
