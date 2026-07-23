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

import org.jwellman.virtualdesktop.docking.DockableLocation;
import org.jwellman.virtualdesktop.docking.DockingBootstrap;
import org.jwellman.virtualdesktop.docking.DockingSession;
import org.jwellman.virtualdesktop.docking.DockingWorkspace;
import org.jwellman.virtualdesktop.tools.ToolSpec;

/**
 * Launch recipe for an in-desktop tool: title, icon, and content panel.
 *
 * <p>Implements {@link ToolSpec}. Docking is not owned here — the host
 * ({@code DesktopManager} / {@code VirtualAppFrame}) attaches a
 * toolId-scoped {@link DockingSession} after the frame exists.
 * Multi-dockable tools such as {@code SpecWorkspace} may add further
 * dockables via the attached session.</p>
 *
 * @author Rick Wellman
 */
@SuppressWarnings("rawtypes")
abstract public class VirtualAppSpec implements ToolSpec {

    private Icon icon;
    private String title;
    private Class clazz;
    private JPanel content;
    protected int height = 0;
    protected int width = 0;

    /**
     * !! USE WITH CAUTION !!
     * <p>
     * Only set to true for the rare edge case when you want DesktopManager
     * to create the internal frame but you want your spec instance to
     * provide the content directly.
     * DesktopManager will call populateInternalFrame().
     */
    protected boolean internalFrameProvider = false;

    /** When true, this instance content will be hosted by something other than DesktopManager */
    protected boolean hosted = false;

    /**
     * Attached by the host after the tool window / toolId exists.
     * Null until then — do not create docking from setContent().
     */
    private DockingSession dockingSession;

    /**
     * Mirror of the session workspace for SpecWorkspace location helpers.
     * Prefer {@link #getDockingWorkspace()}. Cleared when the session is released.
     */
    protected DockingWorkspace workspace = null;

    protected VirtualAppSpec() {
        this(false);
    }

    protected VirtualAppSpec(boolean hosted) {
        this.hosted = hosted;
    }

    /**
     * @deprecated use {@link DockingBootstrap#initialize(JFrame)}; kept for call-site compatibility
     */
    @Deprecated
    public static void setJFrame(JFrame frame) {
        DockingBootstrap.initialize(frame);
    }

    /**
     * Called by the host once a toolId-scoped docking session exists.
     *
     * @param session non-null session for this open tool
     */
    public void attachDockingSession(DockingSession session) {
        if (session == null) {
            throw new IllegalArgumentException("docking session is required");
        }
        this.dockingSession = session;
        this.workspace = session.getWorkspace();
    }

    /**
     * @return workspace for location helpers, or null if not yet attached
     */
    protected DockingWorkspace getDockingWorkspace() {
        return workspace;
    }

    /**
     * @return attached session, or null
     */
    public DockingSession getDockingSession() {
        return dockingSession;
    }

    /**
     * Adds the primary content dockable (id = toolId). Overridden by SpecWorkspace
     * to load additional tools after the primary is established.
     *
     * @param c primary content
     */
    public void addDockable(JComponent c) {
        requireSession().addPrimaryDockable(this.getTitle(), c);
    }

    public void addDockable(String title, JComponent c) {
        requireSession().addDockable(title, c);
    }

    public void addDockable(String title, JComponent c, DockableLocation location) {
        requireSession().addDockable(title, c, location);
    }

    /**
     * Releases docking resources for this recipe's session (if any).
     * Prefer frame-owned release via {@code VirtualAppFrame.releaseDockingSession()}.
     */
    public void releaseDockableId() {
        if (this.dockingSession != null) {
            this.dockingSession.release();
            this.dockingSession = null;
            this.workspace = null;
        }
    }

    private DockingSession requireSession() {
        if (dockingSession == null) {
            throw new IllegalStateException(
                "No docking session attached — host must call attachDockingSession before addDockable");
        }
        return dockingSession;
    }

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
        if (width * height > 0) {
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
    @Override
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
    @Override
    public JPanel getContent() {
        return content;
    }

    /**
     * Stores the panel the host will display. Does <em>not</em> create a docking workspace.
     *
     * @param content the content to set
     */
    public void setContent(JPanel content) {
        this.content = content;
    }

    @Override
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

    public boolean isHosted() {
        return this.hosted;
    }

    /**
     * Whether the host should present this tool via a docking session.
     * Hosted child specs (e.g. inside SpecWorkspace) return false for their own frame.
     */
    public boolean isDockable() {
        return !hosted && !internalFrameProvider;
    }

    /**
     * @return docking container once a session is attached; otherwise null
     */
    public Container getDockableContent() {
        return this.dockingSession != null ? this.dockingSession.getContainer() : null;
    }

}
