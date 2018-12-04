package org.jwellman.virtualdesktop.vapps;

import bsh.ConsoleInterface;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 *
 * @author Rick Wellman
 */
public class SpecBeanShell extends VirtualAppSpec {

    public SpecBeanShell() {
        this.height = 450;
        this.width = 750;

        boolean trymine = true;
        final JComponent c = trymine
                ? new org.jwellman.bsh.JConsole()
                : new JConsole();

        final Interpreter i = new Interpreter((ConsoleInterface)c);

        // start a thread to call the run() method
        new Thread( i, "BeanShell Interpreter" ).start();

        try {

            // give namespace access to the interpreter for advanced features/methods which require interpreter as a parameter
            i.set("_interpreter", i);
            i.set("_console", c);

            // Note: using "/src/..." works on Windows but not Linux
            // TODO try to put this in some sort of callback to speed up display of vapp
            i.source("src/main/resources/jvdClassBrowser.bsh");

        } catch (FileNotFoundException ex) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EvalError ex) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.setTitle("BeanShell Console");
        this.setContent(this.createDefaultContent(c));
    }

}
