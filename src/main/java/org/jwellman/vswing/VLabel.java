package org.jwellman.vswing;

/**
 *
 * @author rwellman
 */
public class VLabel extends javax.swing.JLabel {

    private VComponent vext;

    public VLabel() {
        super();
        
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
