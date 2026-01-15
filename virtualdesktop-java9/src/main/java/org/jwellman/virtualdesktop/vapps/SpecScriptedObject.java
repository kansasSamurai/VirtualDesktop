package org.jwellman.virtualdesktop.vapps;

import java.awt.Container;

/**
 * A virtual app spec to support scripted objects.
 * (At the time it was written, BSH was the only script)
 * 
 * @author Rick Wellman
 *
 */
public class SpecScriptedObject extends VirtualAppSpec {
    
    public SpecScriptedObject(final Container c, final String title) {
     
        this.setTitle(title);
        this.setContent(this.createDefaultContent(c));
    }
    
}
