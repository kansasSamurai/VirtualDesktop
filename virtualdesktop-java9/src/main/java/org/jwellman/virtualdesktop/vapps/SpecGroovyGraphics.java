package org.jwellman.virtualdesktop.vapps;

import groovy.swing.j2d.app.GraphicsPad;
import javax.swing.JPanel;

/**
 *
 * @author rwellman
 */
public class SpecGroovyGraphics extends VirtualAppSpec {

    public SpecGroovyGraphics() {
        super();
        this.setTitle("Groovy Console");
        this.setContent(new JPanel());
        
        GraphicsPad gp = new GraphicsPad();
        gp.run();
    }
    
}
