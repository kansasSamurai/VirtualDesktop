package org.jwellman.swing.jtree.renderer;

import java.awt.Component;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * 
 * @author Rick Wellman
 *
 */
public class CountryTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;
	
	private static final Map<String,Icon> mapOfIcons = new HashMap<>();

	private Icon unknownIcon;
	
	public CountryTreeCellRenderer() {
		if (mapOfIcons.isEmpty()) {
			mapOfIcons.put("India", makeicon("in.png"));
			mapOfIcons.put("Singapore", makeicon("sg.png"));
			mapOfIcons.put("Indonesia", makeicon("id.png"));
			mapOfIcons.put("Vietnam", makeicon("vn.png"));
			mapOfIcons.put("United States", makeicon("us.png"));
			mapOfIcons.put("Canada", makeicon("ca.png"));
			mapOfIcons.put("Brazil", makeicon("br.png"));
			mapOfIcons.put("Argentina", makeicon("ar.png"));
			mapOfIcons.put("Uruguay", makeicon("uy.png"));
			mapOfIcons.put("United Kingdom", makeicon("gb.png"));
			mapOfIcons.put("Germany", makeicon("ge.png"));
			mapOfIcons.put("Spain", makeicon("es.png"));
			mapOfIcons.put("France", makeicon("fr.png"));
			mapOfIcons.put("Italy", makeicon("it.png"));
			unknownIcon = makeicon("us.png");
		}
    }

	private Icon makeicon(String resource) {
        final URL imageUrl = this.getClass().getResource("/com/famfamfam/icons/flags/" + resource);        
        if (imageUrl != null) { 
        	return new ImageIcon(imageUrl);
        }

		return null;        		
	}
	
    @Override
    public Component getTreeCellRendererComponent(
    		JTree tree, Object value, boolean selected, boolean expanded, 
    		boolean leaf, int row, boolean hasFocus) {
    	
        final Object o = ((DefaultMutableTreeNode) value).getUserObject();
        final boolean aware = o instanceof CountryAware;
        
        Icon icon = null;
        CountryAware country = null;
        if (aware) { 
        	// System.out.println("user object is countryaware");
        	country = (CountryAware) o; 
        	
        	if (null == country.getFlagIcon()) {
        		icon = mapOfIcons.get(country.getName());
        		if (icon == null) icon = unknownIcon;
        	}
        } else {
        	// System.out.println("user object is plain");
        }
            
        this.setIcon(aware ? icon : null );
        this.setText(aware ? country.getName() : (""+value) );
        
        return this;
    }
    
}
