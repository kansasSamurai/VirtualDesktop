package org.jwellman.virtualdesktop.vapps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import bsh.ConsoleInterface;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;

/**
 *
 * @author Rick Wellman
 */
public class SpecBeanShell extends VirtualAppSpec implements Runnable {

    private final Interpreter interpreter;
    
    public SpecBeanShell() {
        this.height = 450;
        this.width = 750;

        boolean trymine = true;
        final JComponent c = trymine
                ? new org.jwellman.bsh.JConsole()
                : new JConsole();

        // start a thread to call the run() method
        interpreter = new Interpreter((ConsoleInterface)c);
        new Thread( interpreter, "BeanShell Interpreter" ).start();

        try {

            // give namespace access to the interpreter for advanced features/methods which require interpreter as a parameter
            interpreter.set("_interpreter", interpreter);
            interpreter.set("_console", c);

        } catch (EvalError ex) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.setTitle("BeanShell Console");
        this.setContent(this.createDefaultContent(c));

        SwingUtilities.invokeLater( this );
    }

    public void run() {
        try {
            
            // Note: using "/src/..." works on Windows but not Linux
            interpreter.source("src/main/resources/jvdClassBrowser.bsh");
            
        } catch (FileNotFoundException e) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, e);
        } catch (EvalError e) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
}
