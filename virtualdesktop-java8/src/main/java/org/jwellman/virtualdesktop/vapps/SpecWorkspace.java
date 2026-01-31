package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jwellman.virtualdesktop.bsh.BeanShellService;

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

        String[] tools = toolsList.split(",");
        for (String tool : tools) {
            tool = tool.trim();
            if (tool.isEmpty()) continue;

            try {
                if (tool.startsWith("bsh:")) {
                    addBshTool(tool.substring(4));
                } else {
                    addJavaTool(tool);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to load tool: " + tool, e);
            }
        }
    }

    private void addJavaTool(String className) throws Exception {
        if (!className.contains(".")) {
            className = VAPPS_PACKAGE + className;
        }

        Class<?> clazz = Class.forName(className);
        Object instance = clazz.newInstance();

        if (instance instanceof VirtualAppSpec) {
            VirtualAppSpec spec = (VirtualAppSpec) instance;
            JPanel content = spec.getContent();
            if (content != null) {
                addDockable(spec.getTitle(), content);
            } else {
                LOG.warning("Tool returned null content: " + className);
            }
        }
    }

    private void addBshTool(String command) throws Exception {
        BeanShellService bsh = BeanShellService.get();
        Object result = bsh.eval(command);

        if (result instanceof JComponent) {
            addDockable(command, (JComponent) result);
            return;
        }

        if (result == null) {
            LOG.warning("BeanShell tool returned null: " + command);
            return;
        }

        // For BeanShell "this" objects, try to extract a panel
        bsh.getInterpreter().set("__workspace_result", result);

        // Try getContent() first
        try {
            Object panel = bsh.eval("__workspace_result.getContent();");
            if (panel instanceof JComponent) {
                addDockable(command, (JComponent) panel);
                return;
            }
        } catch (Exception ignore) { }

        // Try .panel property (like better.bsh uses)
        try {
            Object panel = bsh.eval("__workspace_result.panel;");
            if (panel instanceof JComponent) {
                addDockable(command, (JComponent) panel);
                return;
            }
        } catch (Exception ignore) { }

        LOG.warning("BeanShell tool did not return a usable component: " + command);
    }

}
