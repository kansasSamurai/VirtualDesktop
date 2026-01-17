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

import org.jwellman.virtualdesktop.bsh.BeanShellService;

/**
 * BeanShell Console vapp using the shared BeanShellService interpreter.
 *
 * This console shares the interpreter with all other BeanShell-enabled tools,
 * allowing inter-tool communication and inspection of shared state.
 *
 * <p>Note: Due to limitations in BeanShell's Interpreter.setConsole()
 * implementation, the console must be injected into BeanShellService
 * BEFORE the interpreter is created. This class handles that by calling
 * setConsole() before getInterpreter().</p>
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
                : new bsh.util.JConsole();

        // Get the service and inject the console BEFORE getting the interpreter.
        // This is required because BeanShell's Interpreter.setConsole() is incomplete;
        // the console must be passed to the constructor for the REPL to work properly.
        BeanShellService service = BeanShellService.get();
        if (!service.isInitialized()) {
            service.setConsole((ConsoleInterface) c);
        }

        // Now get the shared interpreter (will be created with our console)
        interpreter = service.getInterpreter();

        // Start a thread to run the interpreter's read-eval-print loop
        new Thread(interpreter, "BeanShell Interpreter").start();

        try {
            // Expose references in the shared namespace
            interpreter.set("_interpreter", interpreter);
            interpreter.set("_console", c);

        } catch (EvalError ex) {
            Logger.getLogger(SpecBeanShell.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.setTitle("BeanShell Console");
        this.setContent(this.createDefaultContent(c));

        SwingUtilities.invokeLater(this);
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
