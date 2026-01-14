package org.jwellman.virtualdesktop.vapps;

import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 * This is under construction... 
 * right now, it only displays a message.
 * 
 * It is meant as a base class of common features
 * for vapps that require an external JFrame for whatever reason.
 * Eventually it should help with such things as:
 * 1) minimize/maximize/bringforward (hide?)
 * 2) try to shutdown (this will probably require a "hook"
 *    since each vapp will probably have custom shutdown 
 *    sequences/requirements.
 * 
 * @author rwellman
 *
 */
public class AbstractExternalApp extends VirtualAppSpec {
    
    public AbstractExternalApp(String title) {
        super();
        
        this.setTitle(title);
        this.setContent( this.createMessage() );

//        this.createDefaultContent(this.createMessage());
    }

    private JPanel createMessage() {
        JPanel p = new JPanel(new GridBagLayout());
        
        JTextPane textpane = new JTextPane();
        textpane.setText("This 'app' runs in the JVM\nbut is opened in an\nexternal JFrame.");
        p.add(textpane);
        
        return p;
    }

}
