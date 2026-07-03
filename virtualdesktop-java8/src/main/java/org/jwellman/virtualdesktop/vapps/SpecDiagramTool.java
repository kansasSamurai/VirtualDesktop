package org.jwellman.virtualdesktop.vapps;

import org.jwellman.diagram.LayeredDiagramTool;
import org.jwellman.diagram.domain.cls.ClassDiagramDemo;
import org.jwellman.diagram.domain.cls.ClassDiagramFactory;

/**
 * VApp that hosts the LayeredDiagramTool inside a VirtualDesktop internal frame.
 */
public class SpecDiagramTool extends VirtualAppSpec {

    public SpecDiagramTool() {
        super();
        this.setTitle("Diagram Tool");
        this.setWidth(900);
        this.setHeight(500);

        LayeredDiagramTool tool = new LayeredDiagramTool();
        ClassDiagramFactory f = new ClassDiagramFactory(tool.getDiagramPane().getTheme());
        tool.setComponentFactory(f);
        ClassDiagramDemo.buildDemo(tool.getDiagramPane(), f);

        this.setContent(this.createDefaultContent(tool));
    }

}
