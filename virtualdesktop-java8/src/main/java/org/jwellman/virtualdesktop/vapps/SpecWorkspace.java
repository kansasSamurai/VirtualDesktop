package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jwellman.virtualdesktop.bsh.BeanShellService;
import org.jwellman.virtualdesktop.docking.DockableLocation;

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

    private static final Logger LOG = Logger.getLogger(SpecWorkspace.class.getName());
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

        // Create content panel and load tools
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(this.getTitle(), SwingConstants.CENTER), BorderLayout.CENTER);
        this.setContent(this.createDefaultContent(panel));

        loadTools();
    }

    private void loadTools() {
        if (toolsList == null || toolsList.trim().isEmpty()) {
            return;
        }

        // Track position index for each direction
        int northIndex = 0, southIndex = 0, westIndex = 0, eastIndex = 0;

        // Use semicolon because bsh scripts might need parameters (thus commas)
        String[] tools = toolsList.split(";");
        for (String tool : tools) {
            tool = tool.trim();
            if (tool.isEmpty()) continue;

            // Parse optional position hint: tool@position
            String position = null;
            int atIndex = tool.lastIndexOf('@');
            if (atIndex > 0) {
                position = tool.substring(atIndex + 1).trim().toLowerCase();
                tool = tool.substring(0, atIndex).trim();
            }

            // Resolve location from position hint
            DockableLocation location;
            if ("north".equals(position) || "top".equals(position)) {
                location = DockableLocation.minimalNorthIn(workspace, northIndex++);
            } else if ("south".equals(position) || "bottom".equals(position)) {
                location = DockableLocation.minimalSouthIn(workspace, southIndex++);
            } else if ("west".equals(position) || "left".equals(position)) {
                location = DockableLocation.minimalWestIn(workspace, westIndex++);
            } else if ("east".equals(position) || "right".equals(position)) {
                location = DockableLocation.minimalEastIn(workspace, eastIndex++);
            } else {
                location = DockableLocation.normalIn(workspace);
            }

            try {
                if (tool.startsWith("bsh:")) {
                    addBshTool(tool.substring(4), location);
                } else {
                    addJavaTool(tool, location);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to load tool: " + tool, e);
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
                LOG.warning("Tool returned null content: " + className);
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
            LOG.warning("BeanShell tool returned null: " + command);
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


        LOG.warning("BeanShell tool did not return a usable component: " + command);
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
