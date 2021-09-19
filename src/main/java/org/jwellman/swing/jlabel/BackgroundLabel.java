package org.jwellman.swing.jlabel;

import java.io.File;

import javax.swing.Icon;
import javax.swing.JLabel;

/**
 * A simple JLabel extension to support using filenavigator.bsh
 * to change the background image of anything[A] that supports
 * the "backgroundFile" property.
 * 
 * [A] In this case, we are specifically using the VDesktopPane.
 * 
 * FWIW, the reason we need an extra property and we don't simply
 * leverage the existing "icon" property is that we want the
 * target (i.e. drop target) to decide how to handle the image
 * file.  We cannot assume that the target simply wants the
 * iconified version of the image file that this label 
 * typically* displays. (*and does in the .bsh script)
 * 
 * @author rwellman
 *
 */
public class BackgroundLabel extends JLabel {

    private File backgroundFile;
    
    public BackgroundLabel(File f, Icon i) {
        super(i);
        
        this.backgroundFile = f;
    }
    
    public File getBackgroundFile() {
        return this.backgroundFile;
    }
    
    private static final long serialVersionUID = 1L;

}
