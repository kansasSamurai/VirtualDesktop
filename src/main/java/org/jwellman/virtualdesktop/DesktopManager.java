package org.jwellman.virtualdesktop;

import java.awt.Container;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.SwingUtilities;

import org.jwellman.virtualdesktop.vapps.VirtualAppSpec;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * This class is a SINGLETON... it must be.
 * 
 * A class to manage the lifecycle of Virtual Apps and how they appear on
 * the desktop.  Currently, its main purpose is to control the life cycle
 * and maintain the collection of active applications.
 * 
 * It is envisioned that this class will either help or morph into 
 * an actual Java Desktop Manager.
 * 
 * @author rwellman
 *
 */
public class DesktopManager {

	private JDesktopPane desktop;
	
	private EventList<VirtualAppFrame> frames = new BasicEventList<>();
	
	/**
	 * private constructor to enforce singleton pattern
	 */
	private DesktopManager() {
		// empty for now
	}
	
	private static DesktopManager SINGLETON;
	
	/**
	 * Gets the singleton instance of the DesktopManager
	 * 
	 * @return
	 */
	public static DesktopManager get() {
		if (SINGLETON == null) SINGLETON = new DesktopManager();
		return SINGLETON;
	}

	/**
	 * Create a new application.
	 * 
	 * @param newInstance
	 */
    public void createVApp(Object newInstance) {
        this.createVApp((VirtualAppSpec)newInstance);
    }

    /**
     * Create a new application.
     *
     * @param spec
     */
    public void createVApp(final VirtualAppSpec spec) {

        if (spec.isInternalFrameProvider()) {
            final VirtualAppFrame frame = this.createAppFrame(spec.getTitle());
            spec.populateInternalFrame(frame);
        } else {
            this.createVApp(spec.getContent(), spec.getTitle(), null);
        }

    }

    /**
     * Create a new application.
     * 
     * @param c
     * @param title
     * @return
     */
    public VirtualAppFrame createVApp(final Container c, final String title) {
        return this.createVApp(c, title, null);
    }

    /**
     * Create a new application.
     * 
     * This public method allows internal apps to create internal apps/windows.
     * i.e. via beanshell or others !! All overloaded methods lead here !!
     * [This is the definitive method of the overloaded versions.]
     *
     * @param c
     * @param title
     * @param icon
     * @return
     */
    public VirtualAppFrame createVApp(final Container c, final String title, final Icon icon) {

    	final VirtualAppFrame frame = this.createAppFrame(title); 
        if (icon != null) frame.setFrameIcon(icon);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    // final VirtualAppFrame frame = new VirtualAppFrame(title);
                    frame.setContentPane(c);

//                    if (icon != null) frame.setFrameIcon(icon);
//                    frame.pack(); // moved to following if/else; see Note [1] below
                    
                    if ((c.getWidth() * c.getHeight()) != 0) {
                        frame.setSize(c.getWidth(), c.getHeight());
                        System.out.println("JIF setSize()");
                    } else {
                        System.out.println("JIF pack()");
                        frame.pack(); // see Note [1] below
                    }

                    desktop.add(frame);
                    frame.setVisible(true); //necessary as of 1.3
                    frame.setSelected(true);
                } catch (java.beans.PropertyVetoException e) {
                    e.printStackTrace(); // for now, simply swallow the exception
                } catch (Exception e) {
                    e.printStackTrace(); // for now, simply swallow the exception
                }
            }
        });

        return frame;

        // Note [1]: For now I am removing this via comment as it has undesired
        // side effects.  However, I have a feeling that I should be using
        // pack() and the side effects are due to a design error elsewhere.
        // 9/2/2018:  Trying to always call pack() first, then setsize().
    }

    /**
     * A private convenience method to encapsulate things that MUST happen
     * when creating a new Virtual App; such as adding to list of frames, etc ...
     * 
     * @param title
     * @return
     */
    private VirtualAppFrame createAppFrame(String title) {
        final VirtualAppFrame frame = new VirtualAppFrame(title); 
        
        // It's probably easier/better to use GlazedLists proxies instead of
        // SwingUtilities.invokeLater() directly, but I have tried to use them 
        // and am not doing it right apparently :(
        SwingUtilities.invokeLater(
                new Runnable() { @Override public void run() {
                    // For example, this will throw an exception if not run on the EDT
                    // which is easily done when creating a UI via beanshell scripts such as better.bsh, etc.
                    frames.add(frame);                
                } }
        	);
        
        return frame;
    }
    
	/**
	 * @return the desktop
	 */
	public JDesktopPane getDesktop() {
		return desktop;
	}

	/**
	 * @param desktop the desktop to set
	 */
	public void setDesktop(JDesktopPane desktop) {
		this.desktop = desktop;
	}
	
	/**
	 * @return the frames
	 */
	public EventList<VirtualAppFrame> getFrames() {
		return frames;
	}

}
