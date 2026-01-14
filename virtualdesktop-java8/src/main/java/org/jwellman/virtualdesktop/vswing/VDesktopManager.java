package org.jwellman.virtualdesktop.vswing;

import javax.swing.DefaultDesktopManager;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;

/**
 * An extension/customization of the DefaultDesktopManager for this framework.
 * For now, it mainly serves to log program flow and provide customization points.
 * 
 * Ultimately, I want it to override behavior and provide a new, custom user experience
 * such as putting iconified internal frames inside a JTree instead of placing
 * icons on the desktop.
 * 
 * @author rwellman
 *
 */
public class VDesktopManager extends DefaultDesktopManager {

	private static final long serialVersionUID = 1L;

	/**
	 * 	Generally, indicate that this frame has focus.
	 */
	public void activateFrame(JInternalFrame f) {
		System.out.println("activateFrame");
		super.activateFrame(f);
	}	

	/**
	 * 	This method is normally called when the user has indicated that they will begin dragging a component around.
	 */
	public void	beginDraggingFrame(JComponent f) {
		System.out.println("beginDraggingFrame");
		super.beginDraggingFrame(f);
	}

	/**
	 * 	This methods is normally called when the user has indicated that they will begin resizing the frame.
	 */
	public void beginResizingFrame(JComponent f, int direction) {
		System.out.println("beginResizingFrame");
		super.beginResizingFrame(f, direction);
	}

	/**
	 * 	Generally, this call should remove the frame from it's parent.
	 */
	public void closeFrame(JInternalFrame f) {
		System.out.println("closeFrame");
		super.closeFrame(f);
	}

	/**
	 * 	Generally, indicate that this frame has lost focus.
	 */
	public void deactivateFrame(JInternalFrame f) {
		System.out.println("deactivateFrame");
		super.deactivateFrame(f);
	}
	
	/**
	 * 	Generally, remove any iconic representation that is present and restore the frame to it's original size and location.
	 */
	public void deiconifyFrame(JInternalFrame f) {
		System.out.println("deiconifyFrame");
		super.deiconifyFrame(f);
	}
	
	/**
	 * 	The user has moved the frame.
	 */
	public void dragFrame(JComponent f, int newX, int newY) {
		System.out.println("dragFrame");
		super.dragFrame(f, newX, newY);
	}
	
	/**
	 * 	This method signals the end of the dragging session.
	 */
	public void endDraggingFrame(JComponent f) {
		System.out.println("endDraggingFrame");
		super.endDraggingFrame(f);
	}
	
	/**
	 * 	This method signals the end of the resize session.
	 */
	public void endResizingFrame(JComponent f) {
		System.out.println("endResizingFrame");
		super.endResizingFrame(f);
	}

	/**
	 * 	Generally, remove this frame from it's parent and add an iconic representation.
	 */
	public void iconifyFrame(JInternalFrame f) {
		System.out.println("iconifyFrame");
		super.iconifyFrame(f);
	}
	
	/**
	 * 	Generally, the frame should be resized to match it's parents bounds.
	 */
	public void maximizeFrame(JInternalFrame f) {
		System.out.println("maximizeFrame");
		super.maximizeFrame(f);
	}
	
	/**
	 * 	Generally, this indicates that the frame should be restored to it's size and position prior to a maximizeFrame() call.
	 */
	public void minimizeFrame(JInternalFrame f) {
		System.out.println("minimizeFrame");
		super.minimizeFrame(f);
	}
	
	/**
	 * 	If possible, display this frame in an appropriate location.
	 */
	public void openFrame(JInternalFrame f) {
		System.out.println("openFrame");
		super.openFrame(f);
	}

	/**
	 * 	The user has resized the component.
	 */
	public void resizeFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
		System.out.println("resizeFrame");
		super.resizeFrame(f, newX, newY, newWidth, newHeight);
	}
	
	/**
	 * 	This is a primitive reshape method.
	 */
	public void setBoundsForFrame(JComponent f, int newX, int newY, int newWidth, int newHeight) {
		System.out.println("setBoundsForFrame");
		super.setBoundsForFrame(f, newX, newY, newWidth, newHeight);
	}
	
}
