package org.jwellman.diagram.core;

import java.util.HashMap;
import java.util.Map;

import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.GraphEdge;

/**
 * Simple immutable-identity GraphEdge used by the framework for in-memory and
 * persisted edges. "Immutable" applies to identity/endpoints only — attributes
 * and properties are mutated in place by the property editor.
 */
public class DefaultGraphEdge implements GraphEdge {

    private final String edgeId;
    private final String sourceNodeId;
    private final String sourcePortId;
    private final String targetNodeId;
    private final String targetPortId;
    private final EdgeAttributes attributes;
    private final Map<String, Object> properties;

    public DefaultGraphEdge(String edgeId,
                             String sourceNodeId, String sourcePortId,
                             String targetNodeId, String targetPortId,
                             EdgeAttributes attributes) {
        this(edgeId, sourceNodeId, sourcePortId, targetNodeId, targetPortId,
            attributes, new HashMap<>());
    }

    public DefaultGraphEdge(String edgeId,
                             String sourceNodeId, String sourcePortId,
                             String targetNodeId, String targetPortId,
                             EdgeAttributes attributes,
                             Map<String, Object> properties) {
        this.edgeId       = edgeId;
        this.sourceNodeId = sourceNodeId;
        this.sourcePortId = sourcePortId;
        this.targetNodeId = targetNodeId;
        this.targetPortId = targetPortId;
        // Defensive copy — callers may share an EdgeAttributes instance across edges;
        // each edge must own its attributes so mutations to one don't affect others.
        this.attributes   = new EdgeAttributes(attributes);
        this.properties   = properties != null ? properties : new HashMap<>();
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

    @Override
    public Map<String, Object> getProperties() { return properties; }
}
