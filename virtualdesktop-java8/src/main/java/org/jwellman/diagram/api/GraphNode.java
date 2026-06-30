package org.jwellman.diagram.api;

import java.awt.Point;
import java.util.Map;

import javax.swing.JComponent;

/**
 * A domain node placed on the diagram canvas.
 * Implemented by {@code NodeHostPanel}; domain code never extends this directly.
 */
public interface GraphNode {

    String getNodeId();

    String getNodeType();

    Map<String, Object> getProperties();

    /** Port IDs exposed by this node (e.g. "N", "S", "E", "W"). */
    String[] getPortIds();

    /**
     * Returns the canvas-coordinate location of the named port,
     * or {@code null} if the node has not yet been laid out.
     */
    Point getPortLocation(String portId);

    /** Called by the framework when the node's bounds change. */
    void invalidatePortCache();

    JComponent getVisualComponent();
}
