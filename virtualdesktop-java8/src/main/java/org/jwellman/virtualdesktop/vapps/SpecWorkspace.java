package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jwellman.virtualdesktop.bsh.BeanShellService;
import org.jwellman.virtualdesktop.docking.DockableLocation;
import org.jwellman.virtualdesktop.docking.DockingWorkspace;

import bsh.EvalError;

/**
 * A workspace that launches multiple tools together as dockables
 * within a single virtual app frame.
 *
 * <p>Tools can be specified as Java vapp class names (resolved relative to
 * this package) or BeanShell commands prefixed with "bsh:". Each tool's
 * content panel is added as a dockable within the shared docking workspace.</p>
 *
 * <p>Configuration via {@code vapps-config.json} attrs:</p>
 * <ul>
 *   <li>{@code tools} - comma-separated list of tool identifiers</li>
 * </ul>
 *
 * @author Rick Wellman
 */
public class SpecWorkspace extends VirtualAppSpec implements Configurable {

    private static final Logger LOG = LoggerFactory.getLogger(SpecWorkspace.class);
    private static final String VAPPS_PACKAGE = "org.jwellman.virtualdesktop.vapps.";

    private String toolsList;

    public SpecWorkspace() {
        this.setTitle("Workspace");
    }

    @Override
    public void configure(Map<String, String> attrs) {
        if (attrs.containsKey("tools")) {
            this.toolsList = attrs.get("tools");
        }
        if (attrs.containsKey("title")) {
            this.setTitle(attrs.get("title"));
        }

        // Create content panel; tools are loaded after DesktopManager
        // adds the base content dockable (see addDockable override)
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(this.getTitle(), SwingConstants.CENTER), BorderLayout.CENTER);
        this.setContent(this.createDefaultContent(panel));
    }

    /**
     * Override to load tools after the base content dockable is established.
     * DesktopManager calls this to add the spec's content as the first dockable,
     * which initializes the content area. Tools are then added relative to it.
     */
    @Override
    public void addDockable(JComponent c) {
        super.addDockable(c);
        loadTools();
    }

    /**
     * Supports a relatively simple string format (semicolon delimited) for 
     * specifying a list of tools and their locations.
     * 
     * The tool title is derived from the vapp spec or from properties on the bsh
     * object when using scripted tools. The tool command is a fallback for the
     * title if none can be found.
     * 
     * Simple Example:
     * "SpecBeanShell,SpecObjectBrowser,SpecScriptTester,bsh:better()"
     * 
     * Positioning Example and bsh params:
     * "SpecObjectBrowser@top(0.5);SpecScriptTester;bsh:better(\"Better Editor\", true)"
     * 
     * Updated position parsing to use the new normal directional types. Also
     * supports an optional size parameter in the tools string syntax:
     * - SpecObjectBrowser@west — left side, 30% (default) 
     * - SpecObjectBrowser@west(0.4) — left side, 40% 
     * - bsh:better()@east(0.5) — right side, 50% 
     * - SpecScriptTester — center/tabbed (no position)
     * 
     */
    private void loadTools() {
        if (toolsList == null || toolsList.trim().isEmpty()) {
            return;
        }

        // Default size ratio for directional positioning
        double defaultSize = 0.3;

        // Use semicolon because bsh scripts might need parameters (thus commas)
        String[] tools = toolsList.split(";");
        for (String tool : tools) {
            tool = tool.trim();
            if (tool.isEmpty()) continue;

            // Parse optional position hint: tool@position or tool@position(size)
            String position = null;
            double size = defaultSize;
            int atIndex = tool.lastIndexOf('@');
            if (atIndex > 0) {
                position = tool.substring(atIndex + 1).trim().toLowerCase();
                tool = tool.substring(0, atIndex).trim();

                // Parse optional size: e.g. "west(0.4)"
                int parenStart = position.indexOf('(');
                if (parenStart > 0 && position.endsWith(")")) {
                    try {
                        size = Double.parseDouble(position.substring(parenStart + 1, position.length() - 1));
                    } catch (NumberFormatException e) {
                        // keep default size
                    }
                    position = position.substring(0, parenStart);
                }
            }
            LOG.info("loading {}, {}, {} ...", tool, position, size);

            // Resolve location from position hint (workspace attached by host)
            DockingWorkspace ws = getDockingWorkspace();
            DockableLocation location;
            if ("north".equals(position) || "top".equals(position)) {
                location = DockableLocation.northIn(ws, size);
            } else if ("south".equals(position) || "bottom".equals(position)) {
                location = DockableLocation.southIn(ws, size);
            } else if ("west".equals(position) || "left".equals(position)) {
                location = DockableLocation.westIn(ws, size);
            } else if ("east".equals(position) || "right".equals(position)) {
                location = DockableLocation.eastIn(ws, size);
            } else {
                location = DockableLocation.normalIn(ws);
            }

            try {
                if (tool.startsWith("bsh:")) {
                    addBshTool(tool.substring(4), location);
                } else {
                    addJavaTool(tool, location);
                }
            } catch (Exception e) {
                LOG.warn("Failed to load tool: " + tool, e);
            }
        }
    }

    private void addJavaTool(String className, DockableLocation location) throws Exception {
        if (!className.contains(".")) {
            className = VAPPS_PACKAGE + className;
        }

        Class<?> clazz = Class.forName(className);
        Object instance;

        // Try hosted constructor first, fall back to no-arg
        try {
            instance = clazz.getConstructor(boolean.class).newInstance(true);
        } catch (NoSuchMethodException e) {
            instance = clazz.newInstance();
        }

        if (instance instanceof VirtualAppSpec) {
            VirtualAppSpec spec = (VirtualAppSpec) instance;
            JPanel content = spec.getContent();
            if (content != null) {
                addDockable(spec.getTitle(), content, location);
            } else {
                LOG.warn("Tool returned null content: " + className);
            }
        }
    }

    private void addBshTool(String command, DockableLocation location) throws Exception {
        LOG.info("addBshTool(command): " + command);

        BeanShellService bsh = BeanShellService.get();
        Object result = bsh.eval(command);

        if (result instanceof JComponent) {
            addDockable(command, (JComponent) result, location);
            return;
        }

        if (result == null) {
            LOG.warn("BeanShell tool returned null: " + command);
            return;
        }

        // For BeanShell "this" objects, try to extract a panel
        bsh.getInterpreter().set("__workspace_result", result);

        // Try getContent() first (i.e. VirtualAppSpec)
        try {
            Object panel = bsh.eval("__workspace_result.getContent();");
            Object title = bsh.eval("__workspace_result.getTitle();");
            if (addPanel(panel, title, bsh, command, location))
                return;
        } catch (Exception ignore) { }

        // Try .panel property (like better.bsh uses) : must also have a title property
        try {
            Object panel = bsh.eval("__workspace_result.panel;");
            Object title = bsh.eval("__workspace_result.title;");
            if (addPanel(panel, title, bsh, command, location))
                return;
        } catch (Exception ignore) { }


        LOG.warn("BeanShell tool did not return a usable component: " + command);
    }

    private boolean addPanel(Object panel, Object title, BeanShellService bsh, String command, DockableLocation location) throws EvalError {
        if (panel instanceof JComponent) {
            addDockable((String) Optional.ofNullable(title).orElse(command) , (JComponent) panel, location);
            bsh.getInterpreter().unset("__workspace_result");
            return true;
        }
        return false;
    }

}
