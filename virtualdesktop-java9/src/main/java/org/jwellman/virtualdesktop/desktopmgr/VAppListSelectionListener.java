package org.jwellman.virtualdesktop.desktopmgr;

import java.beans.PropertyVetoException;

import javax.swing.DesktopManager;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jwellman.virtualdesktop.VirtualAppFrame;

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
 * @author rwellman
 *
 */
public class VAppListSelectionListener implements ListSelectionListener {

	private JList<VirtualAppFrame> observedJList;
	
	public VAppListSelectionListener(JList<VirtualAppFrame> listeningto) {
		this.observedJList = listeningto;
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) {
			// do nothing... wait until the user is finished selecting
		} else {
			final VirtualAppFrame frame = observedJList.getSelectedValue();
			// frame.setSelected(true); // exception is on this line... 
			// it does not make sense to do the rest if an exception is thrown
			// so put the rest inside the try block.

			// see [Note 1]
			final DesktopManager mgr = frame.getDesktopPane().getDesktopManager();
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
