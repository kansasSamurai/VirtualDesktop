package org.jwellman.vswing;

import java.awt.BorderLayout;

/**
 * JPanel with the following extensions:
 * + automatic BorderLayout
 * + vapp titled border
 * @author rwellman
 */
public class VPanel extends javax.swing.JPanel {
    
    private VComponent vext;
    
    public VPanel() {
        super();
        
        this.setLayout(new BorderLayout());        
        this.setVext(new VComponent(this));
    }

    /**
     * @return the vext
     */
    public VComponent getVext() {
        return vext;
    }

    /**
     * @param vext the vext to set
     */
    public void setVext(VComponent vext) {
        this.vext = vext;
    }
    
}
