package org.jwellman.swing.thirdparty;

import javax.swing.JPanel;

/**
 * A class that utilizes the Batik MemoryMonitor to expose the functionality
 * of the Panel class so that it can be used without the overhead of
 * an entire JFrame.
 * 
 * @author rwellman
 * 
 */
public class BetterMemoryMonitor extends org.apache.batik.util.gui.MemoryMonitor {

    private static final long serialVersionUID = 1L;

    /**
     * Create our own Panel object so we are not encumbered by the base
     * class decorations such as border, etc.
     */
    private Panel widget = new Panel();

    /**
     * We "can" use the full object as a JFrame if we want but that's not
     * really the purpose of this class.  Rather, if you're using this class
     * then you will want to get the Panel object via getMemoryMonitor().
     */
    public BetterMemoryMonitor() {
    }

    public JPanel getMemoryMonitor() {
        return this.widget;
    }

    public void start() {
        RepaintThread t = this.widget.getRepaintThread();
        if (!t.isAlive())
            t.start();
    }

}
