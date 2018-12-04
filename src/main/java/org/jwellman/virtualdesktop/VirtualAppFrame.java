package org.jwellman.virtualdesktop;

import javax.swing.JInternalFrame;
import javax.swing.border.EmptyBorder;

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

        // TODO 9/2/2018... setSize() and setLocation() should probably be removed;
        // this should be handled by the framework that create vappframe(s).

        //...Then set the window size or call pack...
        setSize(300,300);

        //Set the window's location.
        setLocation(xOffset*openFrameCount, yOffset*openFrameCount);

        // This is a workaround for weblaf
        setBorder(new EmptyBorder(2, 2, 3, 3)); // this is a workaround for weblaf
        //LookAndFeel.installProperty(this, WebLookAndFeel.PROPERTY_HONOR_USER_BORDER, Boolean.TRUE);

    }

    @Override protected void finalize() throws Throwable {
        System.out.println("finalizing VirtualAppFrame");
        super.finalize();
    }

}