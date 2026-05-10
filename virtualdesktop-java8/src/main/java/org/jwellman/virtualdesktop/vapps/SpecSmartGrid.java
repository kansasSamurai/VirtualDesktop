package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.jwellman.demo.SmartGridDemo;

/**
 * VApp that hosts the SmartGrid four-tab demo inside a VirtualDesktop internal frame.
 * Tab content is provided by {@link SmartGridDemo#createDemoTabs()} so that the
 * demo logic lives in one place and is shared between the standalone runner and
 * this vapp.
 */
public class SpecSmartGrid extends VirtualAppSpec {

    public SpecSmartGrid() {
        super();
        this.setTitle("Smart Grid");
        this.setWidth(820);
        this.setHeight(570);

        JPanel content = new JPanel(new BorderLayout());
        content.add(SmartGridDemo.createDemoTabs(true), BorderLayout.CENTER);
        this.setContent(content);
    }

}
