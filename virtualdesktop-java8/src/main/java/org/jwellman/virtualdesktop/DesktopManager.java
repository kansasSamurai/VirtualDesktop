package org.jwellman.virtualdesktop;

import java.awt.Container;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jwellman.dsp.DSP;
import org.jwellman.virtualdesktop.docking.DockingSession;
import org.jwellman.virtualdesktop.state.actions.SimpleAction;
import org.jwellman.virtualdesktop.state.store.AppStore;
import org.jwellman.virtualdesktop.tools.ToolDefinition;
import org.jwellman.virtualdesktop.tools.ToolEnvironment;
import org.jwellman.virtualdesktop.tools.ToolLaunchKind;
import org.jwellman.virtualdesktop.tools.ToolService;
import org.jwellman.virtualdesktop.vapps.Configurable;
import org.jwellman.virtualdesktop.vapps.ExternalAppSpec;
import org.jwellman.virtualdesktop.vapps.LaunchAware;
import org.jwellman.virtualdesktop.vapps.SpecScriptedObject;
import org.jwellman.virtualdesktop.vapps.VirtualAppSpec;

/**
 * Singleton Swing host adapter for open tools.
 *
 * <p>Implements {@link ToolService}: feature code should call lifecycle methods via
 * {@code ToolEnvironment.service()} / {@link ToolService}, not via this concrete class.</p>
 *
 * <p><strong>Authority:</strong> {@code AppStore}/{@code ToolsState} is the product registry
 * of what is open. The frame map here is only a Swing realizer cache keyed by toolId.</p>
 *
 * @author rwellman
 */
public class DesktopManager implements ListSelectionListener, InternalFrameListener, ToolService {

    private static final Logger LOG = LoggerFactory.getLogger(DesktopManager.class);

    private JDesktopPane desktop;

    /**
     * Optional legacy JList sync (pre–WindowListController). Null in the current App path.
     */
    private JList<VirtualAppFrame> observedJList;

    /**
     * Swing realizer cache — not the product list of open tools.
     * Keyed by {@link VirtualAppFrame#getToolId()}.
     */
    private final Map<String, VirtualAppFrame> framesByToolId =
        Collections.synchronizedMap(new LinkedHashMap<String, VirtualAppFrame>());

	/**
	 * private constructor to enforce singleton pattern
	 */
	private DesktopManager() {
		// empty for now
	}

    private static DesktopManager SINGLETON;

	/**
	 * Gets the singleton instance of the DesktopManager
	 * 
	 * @return
	 */
	public static DesktopManager get() {
		if (SINGLETON == null) SINGLETON = new DesktopManager();
		return SINGLETON;
	}

    /**
     * Opens a tool by catalog definition id.
     * Instantiates the Spec, applies configuration/icons, and hosts it.
     */
    @Override
    public void open(String definitionId) {
        ToolDefinition definition = ToolEnvironment.catalog().findById(definitionId);
        if (definition == null) {
            LOG.error("Cannot open tool — unknown definition id: {}", definitionId);
            return;
        }

        try {
            VirtualAppSpec spec;
            if (definition.getLaunchKind() == ToolLaunchKind.EXTERNAL) {
                spec = new ExternalAppSpec(
                    definition.getTitle(),
                    definition.getCommand(),
                    definition.getWorkingDirectory(),
                    definition.isWaitForCompletion()
                );
            } else {
                Class<?> clazz = Class.forName(definition.getClassName());
                Object instance = clazz.newInstance();
                if (!(instance instanceof VirtualAppSpec)) {
                    LOG.error("Class {} is not a VirtualAppSpec", definition.getClassName());
                    return;
                }
                spec = (VirtualAppSpec) instance;
                if (instance instanceof Configurable && !definition.getAttrs().isEmpty()) {
                    ((Configurable) instance).configure(definition.getAttrs());
                }
            }

            applyIconFromDefinition(spec, definition);
            // Use Object overload so internalFrameProvider specs take the void path
            createVApp((Object) spec, definition);
        } catch (ClassNotFoundException ex) {
            LOG.error("Cannot open tool — class not found: {}", definition.getClassName(), ex);
        } catch (InstantiationException | IllegalAccessException ex) {
            LOG.error("Cannot open tool — failed to instantiate: {}", definition.getClassName(), ex);
        } catch (Exception ex) {
            LOG.error("Cannot open tool — definition id: {}", definitionId, ex);
        }
    }

    @Override
    public void close(String toolId) {
        // Transitional: product "close" = withdraw (minimize), not destroy.
        // Tools lack a cleanup SPI; disposing would leak unmanaged resources.
        minimize(toolId);
    }

    @Override
    public void activate(String toolId) {
        VirtualAppFrame frame = findFrameByToolId(toolId);
        if (frame == null) {
            LOG.debug("activate: no frame for toolId {}", toolId);
            return;
        }
        if (!frame.isVisible()) {
            frame.setVisible(true);
        }
        try {
            frame.setIcon(false);
            frame.moveToFront();
            frame.setSelected(true);
        } catch (PropertyVetoException ex) {
            LOG.debug("activate vetoed for toolId {}", toolId);
        }
    }

    @Override
    public void minimize(String toolId) {
        VirtualAppFrame frame = findFrameByToolId(toolId);
        if (frame == null) {
            LOG.debug("minimize: no frame for toolId {}", toolId);
            return;
        }
        try {
            frame.setIcon(true);
        } catch (PropertyVetoException ex) {
            LOG.debug("minimize vetoed for toolId {}", toolId);
        }
    }

    @Override
    public void restore(String toolId) {
        VirtualAppFrame frame = findFrameByToolId(toolId);
        if (frame == null) {
            LOG.debug("restore: no frame for toolId {}", toolId);
            return;
        }
        if (!frame.isVisible()) {
            frame.setVisible(true);
        }
        try {
            frame.setIcon(false);
        } catch (PropertyVetoException ex) {
            LOG.debug("restore vetoed for toolId {}", toolId);
        }
    }

    private VirtualAppFrame findFrameByToolId(String toolId) {
        if (toolId == null) {
            return null;
        }
        return framesByToolId.get(toolId);
    }

    private void registerFrame(VirtualAppFrame frame) {
        framesByToolId.put(frame.getToolId(), frame);
    }

    private void unregisterFrame(VirtualAppFrame frame) {
        framesByToolId.remove(frame.getToolId());
    }

    private void applyIconFromDefinition(VirtualAppSpec spec, ToolDefinition definition) {
        if (spec.getIcon() != null) {
            return;
        }
        String iconKey = definition.getIconKey();
        if (iconKey == null || iconKey.isEmpty()) {
            return;
        }
        try {
            Icon icon = DSP.Icons.getIcon(iconKey + "-small");
            if (icon != null) {
                spec.setIcon(icon);
                return;
            }
        } catch (Exception ex) {
            LOG.debug("Small icon not found for key {}", iconKey);
        }
        try {
            Icon icon = DSP.Icons.getIcon(iconKey + "-large");
            if (icon != null) {
                spec.setIcon(icon);
            }
        } catch (Exception ex) {
            LOG.debug("Large icon not found for key {}", iconKey);
        }
    }

	/**
	 * Create a new application.
	 *
     * Currently (Oct. 2021), this is used solely by
     * menu actions and desktop icons.  This is mainly because it is
     * the only path that calls createVApp_void() which
     * is also the only path that "respects" 'internalFrameProvider's such
     * as HyperSql, GroovyConsole, etc.
	 *
	 * @param newInstance
	 */
    public void createVApp(Object newInstance) {
        this.createVApp(newInstance, null);
    }

    /**
     * Create a new application from a catalog definition (carries definitionId / iconKey into state).
     *
     * @param newInstance VirtualAppSpec instance
     * @param definition catalog entry used to open, or null for ad-hoc opens
     */
    public void createVApp(Object newInstance, ToolDefinition definition) {
        this.createVApp_void((VirtualAppSpec) newInstance, definition);
    }

    /**
     * Create a new application.
     *
     * Currently (Oct. 2021), this is only called by createVApp(Object newInstance).
     * 
     * @param spec
     */
    public void createVApp_void(final VirtualAppSpec spec) {
        this.createVApp_void(spec, null);
    }

    /**
     * Create a new application, optionally linked to a catalog definition.
     *
     * @param spec launch recipe
     * @param definition catalog entry, or null
     */
    public void createVApp_void(final VirtualAppSpec spec, final ToolDefinition definition) {

        if (spec.isInternalFrameProvider()) {
            // TODO I want to move this somehow into the definitive method;
            // I do not like having logic spread throughout all these createvapp methods.
        	LOG.debug("createVApp() routing to populateInternalFrame() for: {}", spec.getTitle());
            final VirtualAppFrame frame = this.createAppFrame(spec.getTitle(), spec, definition);
            
            if (spec.getIcon() != null) {
                frame.setFrameIcon(spec.getIcon());            
            } else {
                frame.setFrameIcon(DSP.Icons.getIcon("jpad.java"));
            }

            desktop.add(frame);
            spec.populateInternalFrame(frame, desktop);
        } else {
        	LOG.debug("createVApp() routing to standard path for: {}", spec.getTitle());
            this.createVApp(spec, definition);
        }

    }

    /**
     * Create a new application.
     * 
     * Currently (Oct. 2021), this is used by beanshell scripts since they
     * do not need the formality of creating a VirtualAppSpec object.
     * 
     * @param c
     * @param title
     * @return
     */
    public VirtualAppFrame createVApp(final Container c, final String title) {
        return this.createVApp(c, title, null);
    }

    /**
     * Create a new application.
     * 
     * Update(Oct. 2022) Most BSH scripts don't use this version with the icon
     * but jvdClassBrowser.sh does.
     * 
     * Currently (Oct. 2021), this is used by beanshell scripts since they
     * do not need the formality of creating a VirtualAppSpec object.
     * 
     * @param c
     * @param title
     * @return
     */
    public VirtualAppFrame createVApp(final Container c, final String title, Icon icon) {
        VirtualAppSpec spec = new SpecScriptedObject(c, title);
        spec.setIcon(icon);
        return this.createVApp(spec);
    }

    /**
     * Create a new application.<br/>
     * !! All overloaded methods lead here !!<p>
     * Currently (Oct. 2021), there is one exception:<br/>  
     * VirtualAppSpec where internalframeprovider is true.
     * 
     * <p>
     * This public method allows internal apps to create internal apps/windows.
     * i.e. via beanshell or others 
     *
     * @param spec launch recipe
     * @return the created frame
     */
    public VirtualAppFrame createVApp(final VirtualAppSpec spec) {
        return this.createVApp(spec, null);
    }

    /**
     * Host a VirtualAppSpec, optionally recording catalog linkage in ToolsState.
     *
     * @param spec launch recipe
     * @param definition catalog entry used to open, or null for ad-hoc opens
     * @return the created frame
     */
    public VirtualAppFrame createVApp(final VirtualAppSpec spec, final ToolDefinition definition) {

        Icon icon = spec.getIcon();
        String title = spec.getTitle();
        Container c = spec.getContent();
        LOG.info("Creating vapp: {} [dockable={}, hosted={}]", title, spec.isDockable(), spec.isHosted());

        final VirtualAppFrame frame = this.createAppFrame(title, spec, definition);
        if (icon != null) {
            if (title.equals("BeanShell Class Browser - jvd")) {
                frame.setFrameIcon(DSP.Icons.getIcon("jpad.bsh_class_browser"));
            } else {
                frame.setFrameIcon(icon);
            }
        } else {
            frame.setFrameIcon(DSP.Icons.getIcon("jpad.java"));
        }

        // Docking ownership is toolId-scoped on the frame; Spec only holds a recipe.
        if (spec.isDockable()) {
            DockingSession session = DockingSession.open(frame.getToolId());
            frame.setDockingSession(session);
            spec.attachDockingSession(session);
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    if (spec.isDockable()) {
                        frame.setContentPane(spec.getDockableContent());
                        spec.addDockable(spec.getContent());
                    } else {
                        if (c == null) {
                            frame.setContentPane(new JPanel());
                        } else {
                            frame.setContentPane(c);
                        }
                    }

//                    if (icon != null) frame.setFrameIcon(icon);
//                    frame.pack(); // moved to following if/else; see Note [1] below

                    if ((c.getWidth() * c.getHeight()) != 0) {
                        frame.setSize(c.getWidth(), c.getHeight());
                        LOG.debug("Frame '{}' sized to {}x{}", title, c.getWidth(), c.getHeight());
                    } else {
                        LOG.debug("Frame '{}' using pack()", title);
                        frame.pack(); // see Note [1] below
                    }

                    desktop.add(frame);
                    frame.setVisible(true); //necessary as of 1.3
                    frame.setSelected(true);
                } catch (java.beans.PropertyVetoException e) {
                    LOG.warn("PropertyVetoException creating vapp: {}", title, e);
                } catch (Exception e) {
                    LOG.error("Failed to create vapp: {}", title, e);
                }
            }
        });

        // Check if this is an external application
        // TODO launch this in a separate thread
        if (spec instanceof LaunchAware) {
            try {
                ((LaunchAware) spec).launch();
                LOG.info("Launched external app: {}", spec.getTitle());
            } catch (Exception ex) {
                LOG.error("Failed to launch external app: {}", spec.getTitle(), ex);
            }
        }

        return frame;

        // Note [1]: For now I am removing this via comment as it has undesired
        // side effects.  However, I have a feeling that I should be using
        // pack() and the side effects are due to a design error elsewhere.
        // 9/2/2018:  Trying to always call pack() first, then setsize().
    }

    /**
     * A private convenience method to encapsulate things that MUST happen
     * when creating a new Virtual App; such as adding to list of frames, etc ...
     *
     * @param title
     * @return
     */
    @SuppressWarnings("unused")
    private VirtualAppFrame createAppFrame(String title) {
        return createAppFrame(title, null, null);
    }

    /**
     * A private convenience method to encapsulate things that MUST happen
     * when creating a new Virtual App; such as adding to list of frames, etc ...
     *
     * @param title
     * @param spec the VirtualAppSpec (may be null for legacy calls)
     * @return
     */
    private VirtualAppFrame createAppFrame(String title, VirtualAppSpec spec) {
        return createAppFrame(title, spec, null);
    }

    /**
     * Creates the frame and dispatches TOOL_OPENED with catalog linkage when available.
     *
     * @param title window title
     * @param spec launch recipe (may be null)
     * @param definition catalog entry (may be null)
     * @return new frame
     */
    private VirtualAppFrame createAppFrame(String title, VirtualAppSpec spec, ToolDefinition definition) {
        LOG.debug("Creating app frame: {}", title);
        final VirtualAppFrame frame = new VirtualAppFrame(title);

        // Set tool type from spec class name for Redux state tracking
        if (spec != null) {
            frame.setToolType(spec.getClass().getSimpleName());
        }

        // Transitional: hide on X (not dispose). CLOSING still fires so we can align ToolsState;
        // CLOSED does not. Real DISPOSE_ON_CLOSE waits on a tool cleanup SPI (DEVELOPER_GUIDE §9).
        frame.setDefaultCloseOperation(JInternalFrame.HIDE_ON_CLOSE);

        // Ensure that this desktop manager is a frame listener...
        frame.addInternalFrameListener(this);

        registerFrame(frame);

        String definitionId = definition != null ? definition.getId() : null;
        String iconKey = definition != null ? definition.getIconKey() : null;

        // ToolsState is authoritative; frame map is the Swing realizer only
        AppStore.get().dispatch(SimpleAction.toolOpened(
            frame.getToolId(),
            frame.getToolType(),
            title,
            frame.getToolId(),
            definitionId,
            iconKey
        ));

        return frame;
    }
    
	/**
	 * @return the desktop
	 */
	public JDesktopPane getDesktop() {
		return desktop;
	}

	/**
	 * @param desktop the desktop to set
	 */
	public void setDesktop(JDesktopPane desktop) {
		this.desktop = desktop;
	}

	/**
	 * @param jlist the jlist to set
	 */
	public void setObservedJList(JList<VirtualAppFrame> jlist) {
		this.observedJList = jlist;
	}
	
	/**
	 * Snapshot of Swing host frames (realizer cache only).
	 *
	 * <p>Not the product registry of open tools — use {@code AppStore.get().getState().getTools()}.
	 * Retained for BeanShell / diagnostics that need the JInternalFrame handle.</p>
	 *
	 * @return unmodifiable snapshot of realized frames
	 * @deprecated prefer ToolsState for “what is open”; this is host plumbing
	 */
	@Deprecated
	public Collection<VirtualAppFrame> getFrames() {
		synchronized (framesByToolId) {
			List<VirtualAppFrame> copy = new ArrayList<VirtualAppFrame>(framesByToolId.values());
			return Collections.unmodifiableList(copy);
		}
	}

    private void displayMessage(String prefix, InternalFrameEvent e) {
        LOG.trace("{} : {}", prefix, e.getSource());
    }

	// ============= Begin InternalFrameListener =======================
	
	@Override
	public void internalFrameOpened(InternalFrameEvent e) {
		displayMessage("IFRAME :: opened", e);
	}

	@Override
	public void internalFrameClosed(InternalFrameEvent e) {
		// Fires only when a frame is actually disposed (future real-close path).
		displayMessage("IFRAME :: closed", e);

		JInternalFrame source = e.getInternalFrame();
		if (source instanceof VirtualAppFrame) {
			VirtualAppFrame vaf = (VirtualAppFrame) source;
			unregisterFrame(vaf);
			vaf.releaseDockingSession();
			AppStore.get().dispatch(SimpleAction.toolClosed(vaf.getToolId()));
		}
	}

	@Override
	public void internalFrameClosing(InternalFrameEvent e) {
		displayMessage("IFRAME :: closng", e);
		// HIDE_ON_CLOSE will hide the frame after this; align store + iconify like ToolService.close
		JInternalFrame source = e.getInternalFrame();
		if (source instanceof VirtualAppFrame) {
			minimize(((VirtualAppFrame) source).getToolId());
		}
	}

	@Override
	public void internalFrameIconified(InternalFrameEvent e) {
		displayMessage("IFRAME :: iconfy", e);
		e.getInternalFrame().hide();

		JInternalFrame source = e.getInternalFrame();
		if (source instanceof VirtualAppFrame) {
			String toolId = ((VirtualAppFrame) source).getToolId();
			AppStore.get().dispatch(SimpleAction.toolMinimized(toolId));
		}

		if (observedJList == null) {
			return;
		}
		if (null == desktop.getSelectedFrame()) {
		    this.observedJList.clearSelection();
		}
		boolean allframesareicons = true;
		final JInternalFrame[] array = desktop.getAllFrames();
		for (JInternalFrame f : array) {
		    if (!f.isIcon()) {
		        allframesareicons = false;
		        break;
		    }
		}
		if (allframesareicons) {
			this.observedJList.clearSelection();
		}
	}

	@Override
	public void internalFrameDeiconified(InternalFrameEvent e) {
		displayMessage("IFRAME :: deicon", e);

		JInternalFrame source = e.getInternalFrame();
		if (source instanceof VirtualAppFrame) {
			String toolId = ((VirtualAppFrame) source).getToolId();
			AppStore.get().dispatch(SimpleAction.toolRestored(toolId));
		}
	}

	@Override
	public void internalFrameActivated(InternalFrameEvent e) {
		displayMessage("IFRAME :: active", e);

		JInternalFrame source = e.getInternalFrame();
		if (source instanceof VirtualAppFrame) {
			String toolId = ((VirtualAppFrame) source).getToolId();
			AppStore.get().dispatch(SimpleAction.toolActivated(toolId));
		}

		if (observedJList != null) {
			this.observedJList.setSelectedValue(e.getInternalFrame(), true);
		}
	}

	@Override
	public void internalFrameDeactivated(InternalFrameEvent e) {
		displayMessage("IFRAME :: deactv", e);

		JInternalFrame source = e.getInternalFrame();
		if (source instanceof VirtualAppFrame) {
			String toolId = ((VirtualAppFrame) source).getToolId();
			AppStore.get().dispatch(SimpleAction.toolDeactivated(toolId));
		}
	}

	// ============= Begin ListSelectionListener (legacy JList path) =======================
	
	/**
	 * Listen to selection events on the legacy list of virtual apps.
	 * Current App wiring uses WindowListController + ToolService instead.
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (observedJList == null) {
			return;
		}
		if (e.getValueIsAdjusting()) {
			return;
		}
		final VirtualAppFrame frame = observedJList.getSelectedValue();
		if (null == frame) {
			return;
		}

		if (frame.getToolId() != null) {
			activate(frame.getToolId());
		}
	}

}
