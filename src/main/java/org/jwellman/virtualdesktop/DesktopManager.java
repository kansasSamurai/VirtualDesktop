package org.jwellman.virtualdesktop;

import java.awt.Container;
import java.beans.PropertyVetoException;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jwellman.dsp.DSP;
import org.jwellman.virtualdesktop.vapps.SpecScriptedObject;
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
public class DesktopManager implements ListSelectionListener, InternalFrameListener {

	private JDesktopPane desktop;
	
	private JList<VirtualAppFrame> observedJList;
	
	private EventList<VirtualAppFrame> frames = new BasicEventList<>();
	
	private static final String NEWLINE = "\n"; // System.getProperty("line.separator");

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
     * Currently (Oct. 2021), this is used solely by 
     * menu actions and desktop icons.
	 * 
	 * @param newInstance
	 */
    public void createVApp(Object newInstance) {
        this.createVApp_void((VirtualAppSpec)newInstance);
    }

    /**
     * Create a new application.
     *
     * Currently (Oct. 2021), this is only called by createVApp(Object newInstance).
     * 
     * @param spec
     */
    public void createVApp_void(final VirtualAppSpec spec) {

        if (spec.isInternalFrameProvider()) {
            // TODO I want to move this somehow into the definitive method;
            // I do not like having logic spread throughout all these createvapp methods.
        	System.out.println("createVApp() going to populateInternalFrame()");
            final VirtualAppFrame frame = this.createAppFrame(spec.getTitle());
            
            if (spec.getIcon() != null) {
                frame.setFrameIcon(spec.getIcon());            
            } else {
                frame.setFrameIcon(DSP.Icons.getIcon("jpad.java"));
            }

            desktop.add(frame);
            spec.populateInternalFrame(frame, desktop);
        } else {
        	System.out.println("createVApp() going to createVApp()");
            this.createVApp(spec);
        }

    }

    /**
     * Create a new application.
     * 
     * Currently (Oct. 2021), this is used by beanshell scripts since they
     * do not need the formality of creating a VirtualAppSpec object.
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
     * Update(Oct. 2022) Most BSH scripts don't use this version with the icon
     * but jvdClassBrowser.sh does.
     * 
     * Currently (Oct. 2021), this is used by beanshell scripts since they
     * do not need the formality of creating a VirtualAppSpec object.
     * 
     * @param c
     * @param title
     * @return
     */
    public VirtualAppFrame createVApp(final Container c, final String title, Icon icon) {
        VirtualAppSpec spec = new SpecScriptedObject(c, title);
        spec.setIcon(icon);
        return this.createVApp(spec);
    }

    /**
     * Create a new application.<br/>
     * !! All overloaded methods lead here !!<p>
     * Currently (Oct. 2021), there is one exception:<br/>  
     * VirtualAppSpec where internalframeprovider is true.
     * <p>
     * This public method allows internal apps to create internal apps/windows.
     * i.e. via beanshell or others 
     *
     * @param c
     * @param title
     * @param icon
     * @return
     */
    public VirtualAppFrame createVApp(final VirtualAppSpec spec) {

        Icon icon = spec.getIcon();
        String title = spec.getTitle();
        Container c = spec.getContent();
        
    	final VirtualAppFrame frame = this.createAppFrame(title); 
        if (icon != null) {
            if (title.equals("BeanShell Class Browser - jvd")) {
                frame.setFrameIcon(DSP.Icons.getIcon("jpad.bsh_class_browser"));                
            } else {
                frame.setFrameIcon(icon);                            
            }
        } else {
            frame.setFrameIcon(DSP.Icons.getIcon("jpad.java"));
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    // final VirtualAppFrame frame = new VirtualAppFrame(title);
                    if (spec.isDockable()) {
                        frame.setContentPane(spec.getDockableContent());
                        spec.addDockable(spec.getContent());
                    } else {
                        frame.setContentPane(c);                        
                    }

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
        
        // The default close operation is to HIDE (for now)...
        // Moved from the VirtualAppFrame constructor simply for process visibility
        frame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);
        // TODO decide how to handle internal frame lifecycle; hide vs. dispose etc.
        // for later research/design: 
        // https://docs.oracle.com/javase/7/docs/api/javax/swing/JInternalFrame.html
        // dispose() -- Makes this internal frame invisible, unselected, and closed.
        
        
        // Ensure that this desktop manager is a frame listener...
        frame.addInternalFrameListener(this);
        
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
	 * @param jlist the jlist to set
	 */
	public void setObservedJList(JList<VirtualAppFrame> jlist) {
		this.observedJList = jlist;
	}
	
	/**
	 * @return the frames
	 */
	public EventList<VirtualAppFrame> getFrames() {
		return frames;
	}

	private void displayMessage(String prefix, InternalFrameEvent e) {
		String s = prefix + " : " + e.getSource() + NEWLINE;
		System.out.println(s);
	}

	// ============= Begin InternalFrameListener =======================
	
	@Override
	public void internalFrameOpened(InternalFrameEvent e) {
		// TODO Auto-generated method stub
		displayMessage("IFRAME :: opened", e);
	}

	@Override
	public void internalFrameClosed(InternalFrameEvent e) {
		// This will not be called if the frames close action is "HIDE"; only when it is "DISPOSE"
		
		// TODO Auto-generated method stub
		displayMessage("IFRAME :: closed", e);
		
		// TODO Remove the object reference from the list so that it can be garbage collected

	}

	@Override
	public void internalFrameClosing(InternalFrameEvent e) {
		// TODO This is where we would add a hook to possibly veto the close/deactivate
		displayMessage("IFRAME :: closng", e);
	}

	@Override
	public void internalFrameIconified(InternalFrameEvent e) {
		// TODO Auto-generated method stub
		displayMessage("IFRAME :: iconfy", e);
		e.getInternalFrame().hide();
		
		if ( null == desktop.getSelectedFrame() ) {
		    this.observedJList.clearSelection();
		}
		boolean allframesareicons = true;
		final JInternalFrame[] array = desktop.getAllFrames();
		for (JInternalFrame f : array) {
		    if (!f.isIcon()) {
		        allframesareicons = false;
		        break;
		    }
		}
		if (allframesareicons) this.observedJList.clearSelection();
	}

	@Override
	public void internalFrameDeiconified(InternalFrameEvent e) {
		// TODO Auto-generated method stub
		displayMessage("IFRAME :: deicon", e);
	}

	@Override
	public void internalFrameActivated(InternalFrameEvent e) {
		// activated = "selected"; i.e. has the focus
		// TODO Auto-generated method stub
		displayMessage("IFRAME :: active", e);
		
		this.observedJList.setSelectedValue(e.getInternalFrame(), true);
	}

	@Override
	public void internalFrameDeactivated(InternalFrameEvent e) {
		// deactivated = "de-selected"; i.e. no longer has the focus
		displayMessage("IFRAME :: deactv", e);
		
	}

	// ============= Begin ListSelectionListener =======================
	
	/**
	 * Listen to selection events on the list of virtual apps;
	 * the selected item should have the effect of "de-iconify" and "restore"
	 * that virtual app.
	 * 
	 * Note:  I specifically use the verbiage above because it turns out that
	 * simply using the DesktopManager methods deiconifyFrame() and minimizeFrame()
	 * do not have the intended effect that you would expect.  See the code below
	 * for how this has to be implemented.
	 * 
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			// do nothing... wait until the user is finished selecting
		} else {
			final VirtualAppFrame frame = observedJList.getSelectedValue();
			if (null == frame) return;
			
			// frame.setSelected(true); // exception is on this line... 
			// it does not make sense to do the rest if an exception is thrown
			// so put the rest inside the try block.
			
			if ( ! frame.isVisible() ) {
				frame.setVisible(true);
			}

			// see [Note 1]
			final javax.swing.DesktopManager mgr = frame.getDesktopPane().getDesktopManager();
			boolean usingYourIntuition = true;
			if (usingYourIntuition) {
				/* All the documentation I read says to use the desktop manager;
				 * however, for testing and proof of concept, this is here
				 * to test the effects of using the JInternalFrame API directly.
				 */
				boolean usingFrameAPI = true;
				if (usingFrameAPI) {
					/* 11/26/2019: As of this time, I am using this approach over
					 * using the desktop manager.  The sole reason is that this approach
					 * does not modify the focus of the JList when you select the item.
					 * Honestly, this is a small detail and I reserve the right to change
					 * my mind but I'm going with it for now. 
					 */
					try {
						frame.setIcon(false); // exception is possible on this line...
						frame.moveToFront();
					} catch (PropertyVetoException e1) {
						e1.printStackTrace();
					}
				} else {
					// Verified that this does NOT work with the following LAFs:
					// Metal, JTattoo, FlatLAF, ...
					mgr.deiconifyFrame(frame);
					mgr.minimizeFrame(frame);													
				}
			} else {
//				mgr.activateFrame(frame); // this was tried in lieu of max/min... it did not work				
				mgr.maximizeFrame(frame);
				mgr.minimizeFrame(frame);				
			}
			
			// see [Note 1a]
			//frame.setSelected(true); // exception is possible on this line... 
			//frame.moveToFront();
			
//			try {
//			} catch (PropertyVetoException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			
			/* [Note 1]
			 * https://coderanch.com/t/336800/java/restore-jinternalframe-normal-state
			 * 
			 * Based on post by Ameer Tamboli on this site, this looks like the
			 * algorithm though I must say that Oracle's documentation and the 
			 * API itself are very unclear and/or misleading.  Further, I suspect
			 * that this might only be necessary for LAFs based on Metal;
			 * more research is necessary.
			 * 
			 * Also, FWIW, the call the maximize then minimize does not appear to result
			 * in any visual anomalies.  However, this is only based on my very limited
			 * number of test/development PCs so more research is necessary here as well.
			 * 
			 * [Note 1a]
			 * These calls are in the original blog post but do not seem necessary;
			 * it is quite possible this is due to a modification in the JVM over
			 * the years.
			 */

		}
	}

}
