package org.jwellman.swing.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A responsive layout manager for Java Swing.
 * 
 * @author rwellman
 *
 */
public class ResponsiveLayout implements LayoutManager2, Serializable {

	private static final long serialVersionUID = 1L;
	
	private boolean uniformRowHeight;

	// === Constructors ===
	public ResponsiveLayout() {
		this(true);
	}
	
	public ResponsiveLayout(boolean urh) {
		this.uniformRowHeight = urh;
	}
	
	// === Properties ===
	
	public boolean isUniformRowHeight() {
		return uniformRowHeight;
	}

	public void setUniformRowHeight(boolean uniformRowHeight) {
		this.uniformRowHeight = uniformRowHeight;
	}

	// ===== LayoutManager interface
	
	/**
     * Adds the specified component to the layout.
     * Not used by this class.
	 */
	@Override
	public void addLayoutComponent(String name, Component comp) {
		// Intentionally empty; not used by this class.
	}

	/**
     * Removes the specified component from the layout.
     * Not used by this class.
	 */
	@Override
	public void removeLayoutComponent(Component comp) {
		// Intentionally empty; not used by this class.
	}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		System.out.print("preferred: [" + parent.getSize());
	    synchronized (parent.getTreeLock()) {
	        Dimension dim = new Dimension(0, 0);
	        
	        final int version = 3;
	        switch (version) {
	        case 1:
		        // TODO properly account for non-visible components; see FlowLayout
	        	// This version works but is un-necessarily large - it was my first guess;
	        	// it returns a region both wide enough for all components and 
	        	// tall enough for all components.
		        int nmembers = parent.getComponentCount();
		        for (int i = 0 ; i < nmembers ; i++) {
		            final Component m = parent.getComponent(i);
		            if (m.isVisible()) {
		                final Dimension d = m.getPreferredSize();
		                
		                dim.height += d.height;
		                dim.width += d.width;

		            }
		        } // end for
	        	break;
	        case 2:
	        	int maxwidth = 0;
	        	for (Component c : parent.getComponents()) {
	        		int w = c.getMinimumSize().width;
	        		if (w > maxwidth) {
	        			dim = c.getMinimumSize();
	        			maxwidth = w;
	        		}
	        	}
	        	break;
	        case 3:
	            // === put components into rows (storing row height as you go) ===
	            final int N = parent.getComponentCount();
	            int ROWS, sumWidths;
	            final int[] rowidx = new int[N];
	            final List<Integer> listRowHeight = new ArrayList<>(N);
	            
	            ROWS = sumWidths = 0;
	            int hgtRow = 0;
	            for (int ctrComponent=0; ctrComponent<N; ctrComponent++) {
	            	sumWidths += parent.getComponent(ctrComponent).getMinimumSize().width;
	            	if (sumWidths <= parent.getWidth()) {
	                    // put in the current row
	                    // rowidx[ctrComponent] = ctrRow;
	                    
	                    // store max row height
	                    hgtRow = Math.max(hgtRow, parent.getComponent(ctrComponent).getMinimumSize().height);
	            	} else {
	                    // put in the next row
	            		ROWS++;
	                    // rowidx[ctrComponent] = ctrRow;
	                    
	                    // store/reset max row height
	                    listRowHeight.add(hgtRow);
	                    hgtRow = parent.getComponent(ctrComponent).getMinimumSize().height;
	                    
	                    // store/reset the accumulator
	                	sumWidths = parent.getComponent(ctrComponent).getMinimumSize().width;
	            	}
	                rowidx[ctrComponent] = ROWS;
	            }
	            // Since we previously only add to the list on the completion of each row,
	            // then we have to add the "last" row height here:
	            listRowHeight.add(hgtRow);
	            // ============================================================
	            
	            dim.width = parent.getWidth();
	            for (int h : listRowHeight) { dim.height += h; }	            

	        	break;
	        default:
	        	dim = parent.getSize();
	        }

	        System.out.println("] " + dim);
	        return dim;
	    } // end synchronized
	}

	/**
	 * For this layout, the minimum size is the preferred size
	 */
	@Override
	public Dimension minimumLayoutSize(Container parent) {
		return this.preferredLayoutSize(parent);
	}

	@Override
	public void layoutContainer(Container target) {
		System.out.print("layout: [" + target.getSize());
		
        final Insets insets = target.getInsets();
        final int N = target.getComponentCount();
        int ROWS, sumWidths;
        // These are dynamic values; adjusted during layout
        int top = insets.top;
        // int bottom = target.getHeight() - insets.bottom;
        int left = insets.left;
        // int right = target.getWidth() - insets.right;

        
        // === put components into rows (storing row height as you go) ===
        final int[] rowidx = new int[N];
        final List<Integer> listRowHeight = new ArrayList<>(N);
        
        ROWS = sumWidths = 0;
        int hgtRow = 0;
        for (int ctrComponent=0; ctrComponent<N; ctrComponent++) {
        	sumWidths += target.getComponent(ctrComponent).getMinimumSize().width;
        	if (sumWidths <= target.getWidth()) {
                // put in the current row
                // rowidx[ctrComponent] = ctrRow;
                
                // store max row height
                hgtRow = Math.max(hgtRow, target.getComponent(ctrComponent).getMinimumSize().height);
        	} else {
                // put in the next row
        		ROWS++;
                // rowidx[ctrComponent] = ctrRow;
                
                // store/reset max row height
                listRowHeight.add(hgtRow);
                hgtRow = target.getComponent(ctrComponent).getMinimumSize().height;
                
                // store/reset the accumulator
            	sumWidths = target.getComponent(ctrComponent).getMinimumSize().width;
        	}
            rowidx[ctrComponent] = ROWS;
        }
        // Since we previously only add to the list on the completion of each row,
        // then we have to add the "last" row height here:
        listRowHeight.add(hgtRow);
        
        // === now do the actual layout because we have rows and columns
        int ctrRow = 0, ptrRowHeight = listRowHeight.get(0);
        for (int i=0; i<N; i++) {
        	if (rowidx[i] != ctrRow) {
        		top += ptrRowHeight;
        		left = insets.left;
        		ptrRowHeight = listRowHeight.get(++ctrRow);
        	}
        	
        	final Component c = target.getComponent(i);
        	int w = c.getMinimumSize().width; // W / 1; // TODO 1 := (components per row ctrRow);
        	c.setSize(w, uniformRowHeight ? ptrRowHeight : c.getMinimumSize().height); // w,h
        	c.setBounds(left, top, w, uniformRowHeight ? ptrRowHeight : c.getMinimumSize().height); // x,y,w,h
        	
        	left += w;
        }
        
	}

	// ===== LayoutManager2 interface
	
	@Override
	public void addLayoutComponent(Component comp, Object constraints) {
		// Intentionally empty; not used by this class.
	}

	@Override
	public float getLayoutAlignmentX(Container target) {
        return 0.5f;
	}

	@Override
	public float getLayoutAlignmentY(Container target) {
        return 0.5f;
	}

	@Override
	public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
		// return this.preferredLayoutSize(target);
	}

	@Override
	public void invalidateLayout(Container target) {
		// Intentionally empty; not used by this class.
	}

}
