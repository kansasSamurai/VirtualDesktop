package org.jwellman.virtualdesktop.vapps;

import fx.Main;
import javax.swing.JPanel;

/**
 *
 * @author Rick Wellman
 */
public class SpecXionFM extends VirtualAppSpec {
    
    public SpecXionFM() {
        this.setTitle("Xion File Manager");
        this.setContent(new JPanel());
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {                    
                    int version = 1;
                    switch (version) {
                        case 1:
                            Main.main(null);
                            break;
                        case 2:
                            // tbd
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // for now, simply swallow the exception
                }
            }
        });        
        
    }
    
}
