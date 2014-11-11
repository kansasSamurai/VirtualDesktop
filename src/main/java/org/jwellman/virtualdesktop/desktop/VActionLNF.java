package org.jwellman.virtualdesktop.desktop;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author rwellman
 */
public class VActionLNF extends AbstractAction {
    
    private String lnfClassName;
    private JFrame frame;
    
    public VActionLNF(String name, Icon icon, String clazz, JFrame f) {
        super(name, icon);
        this.frame = f;
        this.lnfClassName = clazz;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            UIManager.setLookAndFeel(this.lnfClassName);
            SwingUtilities.updateComponentTreeUI(frame);
            frame.pack();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(VActionLNF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(VActionLNF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(VActionLNF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(VActionLNF.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
}
