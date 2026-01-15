package org.jwellman.virtualdesktop.vapps;

import org.jwellman.applet.scar.Scar;

/**
 *
 * @author rwellman
 */
public class SpecScar extends VirtualAppSpec {
    
    public SpecScar() {
        super();

        Scar scar = new Scar();
        this.setTitle("Search and Replace");
        this.setContent(scar.createView());
    }
    
}
