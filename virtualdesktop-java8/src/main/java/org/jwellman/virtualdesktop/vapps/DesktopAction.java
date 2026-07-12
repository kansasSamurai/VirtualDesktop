package org.jwellman.virtualdesktop.vapps;

import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import org.jwellman.virtualdesktop.App;
import org.jwellman.virtualdesktop.DesktopManager;

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

    /** Configuration attributes for Configurable specs */
    private Map<String, String> attrs;

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
                Object spec = this.clazz.newInstance();

                // Apply configuration if spec supports it
                if (spec instanceof Configurable && this.attrs != null) {
                    ((Configurable) spec).configure(this.attrs);
                }

                if (spec instanceof VirtualAppSpec) {
                    applyIconToSpec((VirtualAppSpec) spec);
                }

                DesktopManager.get().createVApp(spec);
            } catch (InstantiationException | IllegalAccessException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
    }

    /**
     * Transfers this action's configured icon (as loaded from vapps-config.json /
     * external-apps.json) onto the spec that will back the resulting internal frame.
     * Without this, {@code VirtualAppSpec.getIcon()} stays {@code null} and
     * {@code DesktopManager} falls back to the generic "jpad.java" icon, even though
     * the launching desktop shortcut or Tools-menu item itself shows the correct icon.
     * A spec that already set its own icon (e.g. in its constructor) is left alone.
     */
    protected void applyIconToSpec(VirtualAppSpec spec) {
        if (spec.getIcon() != null) {
            return;
        }
        Icon icon = (Icon) getValue(SMALL_ICON);
        if (icon == null) {
            icon = (Icon) getValue(LARGE_ICON_KEY);
        }
        if (icon != null) {
            spec.setIcon(icon);
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

    public Map<String, String> getAttrs() {
        return attrs;
    }

    public void setAttrs(Map<String, String> attrs) {
        this.attrs = attrs;
    }

}
