package org.jwellman.virtualdesktop.vapps.iconviewer;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class IconViewer extends JLabel {

    public JPanel view;
    
    public IconViewer() {
        super("< This is your icon");
        
    }
    
    public JComponent getView() {
        if (view == null) {
            view = new JPanel(new GridBagLayout());
            view.setBorder(BorderFactory.createTitledBorder("Icon Preview"));
            view.setOpaque(false);
            view.add(this);            
        }
        
        return view;
    }
    
    private static final long serialVersionUID = 1L;

}
