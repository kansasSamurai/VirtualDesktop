package org.jwellman.vswing;

import javax.swing.JComponent;
import javax.swing.JSplitPane;

/**
 *
 * @author rwellman
 */
public class VSplitPane extends javax.swing.JSplitPane {
    
    public VSplitPane(JComponent lefttop, JComponent rightbottom) {
        super(JSplitPane.HORIZONTAL_SPLIT, lefttop, rightbottom);
        
        this.setVext(new VComponent(this));        
    }

    private VComponent vext;

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
