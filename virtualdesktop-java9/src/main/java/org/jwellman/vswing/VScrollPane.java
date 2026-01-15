package org.jwellman.vswing;

/**
 *
 * @author rwellman
 */
public class VScrollPane extends javax.swing.JScrollPane {
    
    public VScrollPane() {
        super();
        
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
