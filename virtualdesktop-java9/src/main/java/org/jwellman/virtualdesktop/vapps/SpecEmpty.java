package org.jwellman.virtualdesktop.vapps;

import javax.swing.JPanel;

/**
 *
 * @author Rick Wellman
 */
public class SpecEmpty extends VirtualAppSpec {
    
    public SpecEmpty() {
        super();
        this.setTitle(null);
        this.setClazz(null);
        this.setContent(new JPanel());
    }
    
}
