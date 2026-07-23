package org.jwellman.virtualdesktop.tools;

import javax.swing.Icon;
import javax.swing.JPanel;

/**
 * Launch recipe for a tool: metadata plus the panel the environment hosts.
 *
 * <p>Optional capabilities stay as separate interfaces ({@code Configurable},
 * {@code LaunchAware}). Docking is <em>not</em> part of this contract — the host
 * attaches a toolId-scoped docking session when presenting the tool.</p>
 */
public interface ToolSpec {

    /**
     * @return display title for the tool window / dockable
     */
    String getTitle();

    /**
     * @return icon for the tool window, or null
     */
    Icon getIcon();

    /**
     * @return the panel to host; never null for a normal launch
     */
    JPanel getContent();

}
