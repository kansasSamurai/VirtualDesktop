package org.jwellman.vswing;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

/**
 * Objects of this class are automatically created in each VSwing component.  
 * This class is a decorator for JComponents.
 * 
 * @author rwellman
 */
public class VComponent {
   
    private JComponent decorated;
    
    private Border highlighter;
    
    private Border blackline = BorderFactory.createLineBorder(Color.black);
    
    public VComponent(JComponent c) {
        this.decorated = c;
        this.highlighter = BorderFactory.createTitledBorder(blackline, c.getClass().getSuperclass().getSimpleName());
        c.setBorder(highlighter);
    }
    
}
