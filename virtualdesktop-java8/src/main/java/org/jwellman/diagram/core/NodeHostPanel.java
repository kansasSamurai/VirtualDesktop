package org.jwellman.diagram.core;

import java.awt.BorderLayout;
import java.awt.Point;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.jwellman.diagram.api.GraphNode;

/**
 * Framework-owned wrapper that hosts a domain-provided JPanel as graph node content.
 * Port locations are computed lazily from current bounds — no stale cache issues.
 */
public class NodeHostPanel extends JPanel implements GraphNode {

    private final String nodeId;
    private final String nodeType;
    private final Map<String, Object> properties;
    private final String[] portIds;

    private static final long serialVersionUID = 1L;

    /** Constructs a node with the four cardinal ports N/S/E/W. */
    public NodeHostPanel(String id, String type, Map<String, Object> props, JPanel content) {
        this(id, type, props, content, new String[]{"N", "S", "E", "W"});
    }

    public NodeHostPanel(String id, String type, Map<String, Object> props,
                         JPanel content, String[] portIds) {
        super(new BorderLayout());
        this.nodeId = id;
        this.nodeType = type;
        this.properties = props;
        this.portIds = portIds;
        add(content, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------
    // GraphNode implementation
    // ---------------------------------------------------------------

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getNodeType() {
        return nodeType;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String[] getPortIds() {
        return portIds;
    }

    /**
     * Returns the canvas-coordinate location of the named port.
     * Computed directly from current component bounds — always up to date.
     */
    @Override
    public Point getPortLocation(String portId) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        if (w == 0 && h == 0) {
            return null;
        }
        if ("N".equals(portId)) { return new Point(x + w / 2, y); }
        if ("S".equals(portId)) { return new Point(x + w / 2, y + h); }
        if ("E".equals(portId)) { return new Point(x + w, y + h / 2); }
        if ("W".equals(portId)) { return new Point(x, y + h / 2); }
        return null;
    }

    @Override
    public void invalidatePortCache() {
        // No-op: port locations are computed lazily from current bounds
    }

    @Override
    public JComponent getVisualComponent() {
        return this;
    }

    /**
     * Replaces the current content panel with a freshly-built one.
     * Called by the framework after the domain editor mutates properties
     * and triggers the onChanged rebuild callback.
     */
    public void swapContent(JPanel newContent) {
        removeAll();
        add(newContent, BorderLayout.CENTER);
        revalidate();
        repaint();
    }
}
