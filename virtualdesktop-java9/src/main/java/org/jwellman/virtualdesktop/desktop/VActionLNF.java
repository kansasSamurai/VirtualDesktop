package org.jwellman.virtualdesktop.desktop;

import java.awt.event.ActionEvent;
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
public class VActionLNF extends AbstractAction implements Runnable {

    private final JFrame frame;

    private final String lnfClassName;

    private static VActionLNF lastChosen;

    public VActionLNF(String name, Icon icon, String clazz, JFrame f) {
        super(name, icon);
        this.frame = f;
        this.lnfClassName = clazz;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.setEnabled(false);

        if (lastChosen != null) lastChosen.setEnabled(true);
        lastChosen = this;

        SwingUtilities.invokeLater(this);
    }

    @Override
    public void run() {
        try {
            UIManager.setLookAndFeel(this.lnfClassName);
            SwingUtilities.updateComponentTreeUI(frame);
            frame.validate(); // frame.pack();
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
