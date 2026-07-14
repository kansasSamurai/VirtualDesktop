package org.jwellman.virtualdesktop.vapps;

import org.jwellman.diagram.LayeredDiagramTool;
import org.jwellman.diagram.domain.cls.ClassDiagramFactory;
import org.jwellman.diagram.domain.cls.ToolDiagramDemo;
import org.jwellman.diagram.domain.generic.GenericGraphFactory;

/**
 * VApp that hosts the LayeredDiagramTool inside a VirtualDesktop internal frame.
 */
public class SpecDiagramTool extends VirtualAppSpec {

    public SpecDiagramTool() {
        super();
        this.setTitle("Diagram Tool");
        this.setWidth(1100);
        this.setHeight(620);

        LayeredDiagramTool tool = new LayeredDiagramTool();

        // Register available diagram types shown in the "+" new-diagram dialog
        tool.registerDiagramType("Plain Diagram",
            "Free-form shapes and connectors with no fixed vocabulary.",
            pane -> new GenericGraphFactory());
        tool.registerDiagramType("Class Diagram",
            "UML-style classes and relationships (association, inheritance, etc.).",
            pane -> new ClassDiagramFactory(pane.getTheme()));

        // Prime the initial tab with a class diagram factory and demo content
        ClassDiagramFactory f = new ClassDiagramFactory(tool.getDiagramPane().getTheme());
        tool.setComponentFactory(f);
        ToolDiagramDemo.buildDemo(tool.getDiagramPane(), f);

        this.setContent(this.createDefaultContent(tool));
    }

}
