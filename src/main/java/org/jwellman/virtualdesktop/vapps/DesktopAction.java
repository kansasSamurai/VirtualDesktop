package org.jwellman.virtualdesktop.vapps;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import org.jwellman.virtualdesktop.App;

/**
 *
 * @author rwellman
 */
public class DesktopAction extends AbstractAction {
    
    // ===== properties are at the bottom of this class ======
    
    /** All instances share a reference to the desktop */
    private static App vdesktop;
    
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
                final Class clazz = Class.forName(c);
                vdesktop.createVApp( clazz.newInstance() );
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
    public static void setDesktop(App a) { vdesktop = a; }

    private boolean desktopOnly;

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
    
}
