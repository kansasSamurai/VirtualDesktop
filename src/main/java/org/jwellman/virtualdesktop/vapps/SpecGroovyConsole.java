package org.jwellman.virtualdesktop.vapps;

import groovy.ui.Console;
import java.util.HashMap;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

/**
 *
 * @author rwellman
 */
public class SpecGroovyConsole extends VirtualAppSpec {
    
    public SpecGroovyConsole() {
        super();
        this.setTitle("Groovy Console");
        this.setContent(new JPanel());

        Console c = new Console();
        c.run();
    }
    
    @Override
    public void populateInternalFrame(JInternalFrame frame) {
        final HashMap map = new HashMap();
        map.put("rootContainerDelegate", frame);
        
        Console c = new Console();
        c.run(map); // this is a hack! not sure if it will even work
        
        // This works sorta but trying something else
        // this.setContent(this.createDefaultContent(c.getFrame().getContentPane()));        

    }
    
}
