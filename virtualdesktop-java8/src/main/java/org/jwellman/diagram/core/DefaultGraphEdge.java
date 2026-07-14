package org.jwellman.diagram.core;

import java.util.HashMap;
import java.util.Map;

import org.jwellman.diagram.api.EdgeAttributes;
import org.jwellman.diagram.api.GraphEdge;

/**
 * Simple GraphEdge used by the framework for in-memory and persisted edges.
 * Only {@code edgeId} is truly immutable — endpoints, attributes, type, and
 * properties are all mutated in place (by the property editor or an
 * interactive endpoint drag), consistent with edges being shared live
 * references rather than copy-on-write value objects.
 */
public class DefaultGraphEdge implements GraphEdge {

    private final String edgeId;
    private String sourceNodeId;
    private String sourcePortId;
    private String targetNodeId;
    private String targetPortId;
    private final EdgeAttributes attributes;
    private final Map<String, Object> properties;
    private String edgeType;

    public DefaultGraphEdge(String edgeId,
                             String sourceNodeId, String sourcePortId,
                             String targetNodeId, String targetPortId,
                             EdgeAttributes attributes) {
        this(edgeId, sourceNodeId, sourcePortId, targetNodeId, targetPortId,
            attributes, null, new HashMap<>());
    }

    public DefaultGraphEdge(String edgeId,
                             String sourceNodeId, String sourcePortId,
                             String targetNodeId, String targetPortId,
                             EdgeAttributes attributes,
                             String edgeType,
                             Map<String, Object> properties) {
        this.edgeId       = edgeId;
        this.sourceNodeId = sourceNodeId;
        this.sourcePortId = sourcePortId;
        this.targetNodeId = targetNodeId;
        this.targetPortId = targetPortId;
        // Defensive copy — callers may share an EdgeAttributes instance across edges;
        // each edge must own its attributes so mutations to one don't affect others.
        this.attributes   = new EdgeAttributes(attributes);
        this.edgeType     = edgeType;
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
    public void setSourceEndpoint(String nodeId, String portId) {
        this.sourceNodeId = nodeId;
        this.sourcePortId = portId;
    }

    @Override
    public void setTargetEndpoint(String nodeId, String portId) {
        this.targetNodeId = nodeId;
        this.targetPortId = portId;
    }

    @Override
    public EdgeAttributes getAttributes() { return attributes; }

    @Override
    public String getEdgeType() { return edgeType; }

    @Override
    public void setEdgeType(String edgeType) { this.edgeType = edgeType; }

    @Override
    public Map<String, Object> getProperties() { return properties; }
}
