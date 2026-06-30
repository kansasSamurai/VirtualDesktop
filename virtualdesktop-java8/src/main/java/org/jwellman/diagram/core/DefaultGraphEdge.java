package org.jwellman.diagram.core;

import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.GraphEdge;

/**
 * Simple immutable GraphEdge used by the framework for in-memory and persisted edges.
 */
public class DefaultGraphEdge implements GraphEdge {

    private final String edgeId;
    private final String sourceNodeId;
    private final String sourcePortId;
    private final String targetNodeId;
    private final String targetPortId;
    private final EdgeAttributes attributes;

    public DefaultGraphEdge(String edgeId,
                             String sourceNodeId, String sourcePortId,
                             String targetNodeId, String targetPortId,
                             EdgeAttributes attributes) {
        this.edgeId = edgeId;
        this.sourceNodeId = sourceNodeId;
        this.sourcePortId = sourcePortId;
        this.targetNodeId = targetNodeId;
        this.targetPortId = targetPortId;
        this.attributes = attributes;
    }

    @Override
    public String getEdgeId() { return edgeId; }

    @Override
    public String getSourceNodeId() { return sourceNodeId; }

    @Override
    public String getSourcePortId() { return sourcePortId; }

    @Override
    public String getTargetNodeId() { return targetNodeId; }

    @Override
    public String getTargetPortId() { return targetPortId; }

    @Override
    public EdgeAttributes getAttributes() { return attributes; }
}
