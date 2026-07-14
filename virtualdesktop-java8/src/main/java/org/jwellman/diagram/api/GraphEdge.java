package org.jwellman.diagram.api;

import java.util.Map;

/**
 * A typed, directed edge between two node ports.
 */
public interface GraphEdge {

    String getEdgeId();

    String getSourceNodeId();

    String getSourcePortId();

    String getTargetNodeId();

    String getTargetPortId();

    EdgeAttributes getAttributes();

    /**
     * Domain data carried by the edge — e.g. "label", "sourceLabel", "targetLabel"
     * for rendered text. Mirrors {@link GraphNode#getProperties()}; the framework
     * treats this as an opaque bag and never interprets specific keys itself.
     */
    Map<String, Object> getProperties();
}
