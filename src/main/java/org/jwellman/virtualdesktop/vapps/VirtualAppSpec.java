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

import org.jwellman.virtualdesktop.docking.Dockable;
import org.jwellman.virtualdesktop.docking.DockableLocation;
import org.jwellman.virtualdesktop.docking.DockingException;
import org.jwellman.virtualdesktop.docking.DockingService;
import org.jwellman.virtualdesktop.docking.DockingServiceFactory;
import org.jwellman.virtualdesktop.docking.DockingTheme;
import org.jwellman.virtualdesktop.docking.DockingWorkspace;
import org.jwellman.virtualdesktop.docking.impl.DockingServiceImpl;
import org.jwellman.virtualdesktop.docking.spi.DockingProvider;

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
    private String dockableId = null; // Track this instance's dockable ID for cleanup

    protected VirtualAppSpec() {
        // Intentionally Empty
    }
    
/* ==============================================
 * begin docking feature ...
 * Note: docking features might be better placed inside
 * VirtualAppFrame and/or vapps/DesktopManager.
 * ============================================== */
    // Each instance gets its own workspace (by API design)
    protected DockingWorkspace workspace = null;

    // In order to drag/drop between internal frames,
    // they must all share a service (therefore it is static)
    protected static DockingService dockingService = DockingServiceFactory.getInstance();

    public static void setJFrame(JFrame frame) {
        // Setup Docking Service...
        if (!dockingService.isInitialized()) {
            try {
                dockingService.initialize(frame);

                // Besides my visual preference for the flat theme,
                // it also does not use animations (which I also prefer).
                // DockingTheme.FLAT - this is my preference
                dockingService.setTheme(DockingTheme.ECLIPSE); // experimenting with this - I like it; especially with JTattoo LNF
                // DockingTheme.SMOOTH - this is almost like flat except has animations; I don't like the user experience
                // DockingTheme.BASIC - meh
                // DockingTheme.BUBBLE - I like the drag/drop highlighting but HATE the title bar :(

            } catch (DockingException e) {
                throw new RuntimeException("Failed to initialize docking service", e);
            }
        } else {
            System.out.println("Warning:  Tried to reinitialize Docking");
        }
    }

    // Track all dockable IDs globally to prevent duplicates across all workspaces
    private static final java.util.Set<String> usedDockableIds = new java.util.HashSet<String>();

    public void addDockable(JComponent c) {
        // Locations cannot be set until:
        // 1) the service workspace is added to a component
        // see DesktopManager.createVApp()

        // 2) the Dockable has been added to the workspace
        try {
            String dockid = this.getTitle();

            // Generate unique ID if needed (global check across all workspaces)
            int counter = 1;
            while (usedDockableIds.contains(dockid)) {
                dockid = this.getTitle() + "-" + counter++;
            }

            // Register this ID as used
            usedDockableIds.add(dockid);
            this.dockableId = dockid; // Store for cleanup

            // Get provider for builder
            DockingProvider provider = ((DockingServiceImpl) dockingService).getProvider();

            // Build and add dockable
            Dockable dockable = provider.createDockableBuilder()
                .withId(dockid)
                .withTitle(this.getTitle())
                .withComponent(c)
                .withLocation(DockableLocation.normalIn(workspace))
                .withVisible(true)
                .build();

            workspace.addDockable(dockable);

        } catch (DockingException e) {
            throw new RuntimeException("Failed to add dockable", e);
        }
    }

    /**
     * Release the dockable ID when this vapp is closed.
     * Should be called from frame close handlers to allow ID reuse.
     */
    public void releaseDockableId() {
        if (this.dockableId != null) {
            usedDockableIds.remove(this.dockableId);
            if (workspace != null) {
                workspace.removeDockable(this.dockableId);
            }
            this.dockableId = null;
        }
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
        if (this.workspace == null) {
            String workspaceId = this.getTitle() == null ? "FIXME" : this.getTitle();

            // create a workspace with unique name (there may be better algorithms)
            boolean created = false; int counter = 1;
            do {
                try {
                    this.workspace = dockingService.createWorkspace(workspaceId);
                    created = true;
                } catch (DockingException e) {
                    workspaceId = workspaceId + "-" + counter++;
                }

            } while (!created && counter < 100);
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
        return this.workspace != null ? this.workspace.getContainer() : null;
    }

}
