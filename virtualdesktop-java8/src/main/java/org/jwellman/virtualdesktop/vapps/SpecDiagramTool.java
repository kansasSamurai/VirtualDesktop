package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;

import javax.swing.JPanel;

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

        ClassDiagramFactory f = new ClassDiagramFactory();

        LayeredDiagramTool tool = new LayeredDiagramTool();
        tool.setComponentFactory(f);
        ClassDiagramDemo.buildDemo(tool.getDiagramPane(), f);

        JPanel content = new JPanel(new BorderLayout());
        content.add(tool, BorderLayout.CENTER);
        this.setContent(content);
    }

}
