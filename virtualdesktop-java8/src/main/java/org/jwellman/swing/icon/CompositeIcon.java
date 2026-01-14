package org.jwellman.swing.icon;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * CompositeIcon(s) make it possible to add multiple Icon objects
 * where a single Icon object is expected; i.e. the canonical use
 * case for this object is a JLabel.
 * 
 * Further, if you are using a JLabel (particularly its JTable
 * extension DefaultTableCellRenderer), and want a "right aligned"
 * composite icon, then give this object a reference to your JLabel
 * by calling setLabel().  
 * 
 * If your JLabel instance has getHorizontalAlignment() == LEFT
 * then this class will right align to the JLabel right edge
 * instead of the default.
 * 
 * @author rwellman
 *
 */
public class CompositeIcon implements Icon, SwingConstants {

	private List<Icon> icons = new ArrayList<>();
	private List<Integer> listX = new ArrayList<>();
	private List<Integer> listY = new ArrayList<>();
	private Integer height;
	private Integer width = -1;
	private Integer hgap = 0;
	private Integer vgap = 0;
	private Insets margin = new Insets(0, 0, 0, 0);
	private JLabel label;
	
	/**
	 * A convenience method for calling addHorizontalIcon()
	 * since this is by far the most common use case.
	 * 
	 * @param icon
	 */
	public void addIcon(Icon icon) {
		this.addHorizontalIcon(icon);
	}
	
	public void addIcon(Icon icon, int y) {
		this.addHorizontalIcon(icon, y);
	}
	
	public void addHorizontalIcon(Icon icon) {
	    this.addHorizontalIcon(icon, 0);
	}
	
	public void addHorizontalIcon(Icon icon, int y){
		int base = this.getIconWidth() + this.getHorizontalGap();

		icons.add(icon);
	    listX.add(base + 1);
	    listY.add(y);
	    
	    this.width = base + icon.getIconWidth();
	}
	
	@Override
	public void paintIcon(Component c, Graphics g, int i, int i1) {		
		int hbase = 0; // default
		if (this.label != null) {
			if (this.label.getHorizontalAlignment() == LEFT) {
				// I commented this out on 9/15/2019... it doesn't seem to make
				// a difference in a JTable... will have to research why I
				// did this... maybe for a non-JTable use case?
//				this.label.setHorizontalTextPosition(LEFT);
//				hbase = this.label.getWidth() - this.getIconWidth() - margin.right - margin.left;
			} else {
				// System.out.print("," + this.label.getHorizontalTextPosition());
			}
		}
		
	    for (int j = 0; j < icons.size(); j++){
	        icons.get(j).paintIcon(c, g, 
	        		hbase + listX.get(j)+margin.left, 
	        		listY.get(j)+margin.top);
	    }
	}
	
	@Override
	public int getIconWidth() {
		if (this.width == null) {
			this.width = 0;
			// Total Icon widths
		    for (int j = 0; j < icons.size(); j++){
		    	this.width += icons.get(j).getIconWidth();
		    }
		    // plus, total hgaps
		    this.width += (icons.size() - 1) * this.hgap;
		}
	    return this.width;
	}
	
	@Override
	public int getIconHeight() {
		if (this.height == null) {
			this.height = 0;
			// Find Max Icon height
		    for (int j = 0; j < icons.size(); j++){
		    	this.height = Math.max(this.height, icons.get(j).getIconHeight());
		    }
		}
		
	    return this.height;
	}

	public void setHorizontalGap(int px) {
		this.hgap = px;
	}
	
	public int getHorizontalGap() {
		if (this.hgap == null) return 0;
		return hgap;
	}
	
	public void setVerticalGap(int px) {
		this.vgap = px;
	}
	
	public int getVerticalGap() {
		if (this.vgap == null) return 0;
		return vgap;
	}

	public Insets getMargin() {
		return margin;
	}

	public void setMargin(Insets margin) {
		this.margin = margin;
	}

	public JLabel getLabel() {
		return label;
	}

	public void setLabel(JLabel label) {
		this.label = label;
		label.setIcon(this);
	}

}