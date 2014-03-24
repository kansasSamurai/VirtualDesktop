package org.jwellman.virtualdesktop;

import javax.swing.JInternalFrame;

/**
 *
 * @author Rick Wellman
 */
public class VirtualAppFrame extends JInternalFrame {
    
    static int openFrameCount = 0;
    static final int xOffset = 30, yOffset = 30;

    public VirtualAppFrame() {
        this("Document #" + (++openFrameCount), false);
    }
    
    public VirtualAppFrame(String title) {
        this(title, true);
    }
    
    private VirtualAppFrame(String title, boolean updateCount) {        
        super(title, 
              true, //resizable
              true, //closable
              true, //maximizable
              true);//iconifiable

        if (updateCount) { ++openFrameCount; }
        
        //...Create the GUI and put it in the window...

        //...Then set the window size or call pack...
        setSize(300,300);

        //Set the window's location.
        setLocation(xOffset*openFrameCount, yOffset*openFrameCount);
        
    }
    
}