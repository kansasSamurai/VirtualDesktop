package org.jwellman.virtualdesktop.desktopmgr;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import org.jwellman.virtualdesktop.VirtualAppFrame;

/**
 * A customer list cell renderer for the list of VApp(s).
 * <p>Features:<ul>
 * <li>Custom list item icon based on the icon used for the vapp.</li>
 * </ul>
 * 
 * @author rwellman
 *
 */
@SuppressWarnings("serial")
public class VAppListCellRenderer extends DefaultListCellRenderer {

	@Override
    public Component getListCellRendererComponent( JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		final VirtualAppFrame myvalue = (VirtualAppFrame) value;
		
		final JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);		
		c.setIcon(myvalue.getFrameIcon());		
		return c;
    }
	
}
