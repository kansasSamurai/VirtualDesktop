package org.jwellman.virtualdesktop.vapps;

import javax.swing.SwingUtilities;

import ext.com.jediterm.PtyMain;

/**
 * 
 * @author rwellman
 *
 */
public class SpecJediTerm extends AbstractExternalApp implements Runnable {

    public SpecJediTerm() {
        super("JEDI Terminal");

        SwingUtilities.invokeLater( this );
    }

    public void run() {
        //BasicTerminalExample.main(null);
        PtyMain.main(null);        
    }
    
}
