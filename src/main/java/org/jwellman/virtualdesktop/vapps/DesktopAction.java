package org.jwellman.virtualdesktop.vapps;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import org.jwellman.virtualdesktop.App;

/**
 *
 * @author rwellman
 */
public class DesktopAction extends AbstractAction implements Runnable {

    private static final long serialVersionUID = 1L;

    /** All instances share a reference to the desktop */
    private static App vdesktop;

    // ===== JavaBean Properties ======

    /** xxx */
    private boolean desktopOnly;

    private String clazzName;
    
    /** Class of virtual app to create */
    private Class<?> clazz;

    public DesktopAction(String title) {
        super(title);
    }

    public DesktopAction(String title, Icon icon) {
        super(title, icon);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            final String c = e.getActionCommand();
            this.clazz = Class.forName(c);
        } catch (ClassNotFoundException ex) {
            try {
                this.clazz = Class.forName(this.getClazzName());
            } catch (ClassNotFoundException e1) {
                Logger.getLogger(DesktopAction.class.getName()).log(Level.SEVERE, null, e1);
            }                
        }
        SwingUtilities.invokeLater(this);
    }

    @Override
    public void run() {
            try {
                vdesktop.createVApp( this.clazz.newInstance() );
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    public static void setDesktop(App a) { vdesktop = a; }

    /**
     * True if this action is only for desktop icons.
     *
     * @return the isDesktopOnly
     */
    public boolean isDesktopOnly() {
        return desktopOnly;
    }

    /**
     * If this action is meant only for desktop icons, set to true.
     *
     * @param value the isDesktopOnly to set
     */
    public void setDesktopOnly(boolean value) {
        this.desktopOnly = value;
    }

    public String getClazzName() {
        return clazzName;
    }

    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }

}
