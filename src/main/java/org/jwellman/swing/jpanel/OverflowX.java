package org.jwellman.swing.jpanel;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import org.jwellman.swing.layout.ResponsiveLayout;

/**
 * A JPanel that enforces overflow in the X direction.
 * i.e. When placed in a JScrollPane, the viewport forces the JPanel to be
 * the same size as the viewport.
 *  
 * @author rwellman
 *
 */
public class OverflowX extends JPanel implements Scrollable {

	private static final long serialVersionUID = 3805622194014202694L;

	private boolean debug = false;
	
	public OverflowX() {
		this(new ResponsiveLayout());
	}
	
	public OverflowX(LayoutManager m) {
		super(m);
		
		// This class ONLY supports the ResponsiveLayout layout manager.
		if (m instanceof ResponsiveLayout) {
			// This is the required state so do nothing
		} else {
			throw new IllegalArgumentException("OverflowX ONLY supports the ResponsiveLayout layout manager.");
		}
	}


	/**
	 * This is very important for the design of this class/feature.
	 * We MUST override this method so that the preferred size
	 * of this panel is ALWAYS based on the current size
	 * of the viewport PLUS how the layout manager
	 * will place the components within that size.
	 */
	@Override
    public Dimension getPreferredSize() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
            return this.getLayout().preferredLayoutSize(parent);
        } else {
        	throw new UnsupportedOperationException("This panel MUST be within a JViewport");
        }
    }

	// ===== Scrollable Interface =====
	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return this.getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 10;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 10;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
        	final int parw = parent.getWidth();
        	final int prew = this.getPreferredSize().width;
        	final boolean vpLarger = parw > prew;
        	d("tvw: " + vpLarger + " >>> vp: " + parw + ", pref: " + prew);
            return vpLarger;
        }
        return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
        Container parent = SwingUtilities.getUnwrappedParent(this);
        if (parent instanceof JViewport) {
        	final int parh = parent.getHeight();
        	final int preh = this.getPreferredSize().height;
        	final boolean vpLarger = parh > preh;
        	d("tvh: " + vpLarger + " >>> vp: " + parh + ", pref: " + preh);
            return vpLarger;
        }
        return false;
	}
	
	private void d(String s) {
	    if (this.debug) {
	        System.out.println(s);
	    }
	}

}
