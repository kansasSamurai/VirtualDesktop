package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;

import bibliothek.gui.dock.common.CContentArea;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.SingleCDockable;
import bibliothek.gui.dock.common.theme.ThemeMap;

/**
 *
 * @author Rick Wellman
 */
@SuppressWarnings("rawtypes")
abstract public class VirtualAppSpec {

    private Icon icon;
    private String title;
	private Class clazz;
    private JPanel content;
    protected int height = 0;
    protected int width = 0;
    protected boolean internalFrameProvider = false;

    protected VirtualAppSpec() {
        // Intentionally Empty
    }
    
/* ==============================================
 * begin docking feature ...
 * Note: docking features might be better placed inside 
 * VirtualAppFrame and/or vapps/DesktopManager.
 * ============================================== */
    // Each instance gets its own content area (by API design)
    protected CContentArea dockingcontent = null;
    
    // In order to drag/drop between internal frames,
    // they must all share a controller (therefore it is static)
    protected static CControl control;

    public static void setJFrame(JFrame frame) {
        // Setup Docking Controller...
        if (control == null) {
            control = new CControl( frame );

            // Besides my visual preference for the flat theme,
            // it also does not use animations (which I also prefer).
            final ThemeMap themes = control.getThemes();
            themes.select(ThemeMap.KEY_FLAT_THEME);

        } else {
            System.out.println("Warning:  Tried to reinitialize Docking");
        }
    }

    public void addDockable(JComponent c) {
        // Locations cannot be set until:
        // 1) the Controller content area is added to a component
        // see DesktopManager.createVApp()
        
        // 2) the Dockable has been added to the Controller
        SingleCDockable dockable = new DefaultSingleCDockable(this.getTitle(), this.getTitle(), c);        
        control.addDockable( dockable );
        
        // now we can set the location
        dockable.setLocation( CLocation.base(dockingcontent).normal() );
        dockable.setVisible( true );

    }

    // Note:  there is some docking code in setContent();
    
// ================ end docking features ====================    
    
    /**
     * Utility/convenience method to wrap an
     * existing content/component/JComponent with
     * a JPanel for insertion into a JInternalFrame.
     * 
     * This method should not be called until setWidth/Height()
     * have been called (otherwise the defaults are zero). 
     * 
     * 10/6/2022 : I keep waffling on whether this method should setContent() automatically 
     * because it is always called like shown below.  For now I will not do it but
     * it is highly likely I will make this change in the future.
     * this.setContent(this.createDefaultContent(somecomponent));
     *  
     * @param o
     * @return
     */
    protected JPanel createDefaultContent(Component o) {
        final JPanel pnl = new JPanel(new BorderLayout());
        // Only do this when non-zero dimensions are set; this should be the exception
        // since most uses cases would normally just use pack() as the framework initializes
        // the internal app window.
        if ( width * height > 0) {
            pnl.setPreferredSize(new Dimension(width, height));            
        }
        pnl.add(o, BorderLayout.CENTER);

        return pnl;
    }

    /**
     * If a virtual app is marked as an "internal frame provider",
     * then this callback will be used to populate the contents of the JInternalFrame.
     * Note that very few virtual apps will probably use this and its use
     * is discouraged, but it is provided for the one-off's since not all
     * use cases can be envisioned nor can legacy apps be immediately refactored.
     * 
     * Note:  Users of this callback are responsible for all JInternalFrame
     * display mechanisms such as setSize(), pack(), and setVisible().
     * 
     * @param frame
     */
    public void populateInternalFrame(JInternalFrame frame, JDesktopPane desktop) {
        // empty default impl
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return the clazz
     */
    public Class getClazz() {
        return clazz;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @param clazz the clazz to set
     */
    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    /**
     * @return the content
     */
    public JPanel getContent() {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(JPanel content) {
        this.content = content; 
        if (this.dockingcontent == null) {
            String dockid = this.getTitle() == null ? "FIXME" : this.getTitle();
            
            // create a control area with unique name (there may be better algorithms)
            boolean added = false; int counter = 1;
            do {
                try {
                    this.dockingcontent = control.createContentArea(dockid);
                    added = true;
                } catch (Exception e) {
                    dockid += dockid + "-" + counter++;
                }
                
            } while (!added && counter < 100);
            // the limit on counter is just to prevent endless loops
            // it is not a great solution and if the loop counter hits, the app probably won't work right
        }
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isInternalFrameProvider() {
        return this.internalFrameProvider;
    }

    public boolean isDockable() {
        // for now (10/6/2022), all vappspecs are dockable
        return true;
    }

    public Container getDockableContent() {
        return this.dockingcontent;
    }

}
