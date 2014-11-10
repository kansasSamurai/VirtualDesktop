package org.jwellman.virtualdesktop.vapps;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Rick Wellman
 */
public class SpecBeanShell extends VirtualAppSpec {

    public SpecBeanShell() {
        this.height = 450;
        this.width = 750;

        final JConsole c = new JConsole();
        final Interpreter i = new Interpreter(c);
        new Thread( i, "BeanShell Interpreter" ).start(); // start a thread to call the run() method
        try {
            // Note: using "/src/..." works on Windows but not Linux
            i.source("src/main/resources/jvdClassBrowser.bsh");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EvalError ex) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.setTitle("BeanShell");
        this.setContent(this.createDefaultContent(c));
    }

}
