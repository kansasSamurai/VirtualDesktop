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
     * Named relationship kind, e.g. "Composition" or "Inheritance" — mirrors
     * {@link GraphNode#getNodeType()}, but mutable: unlike a node's type (fixed
     * at creation by whichever factory built it), an edge's type can be reassigned
     * later by picking a different relationship preset. {@code null} means no
     * named type is set (e.g. edges on a domain with no relationship vocabulary).
     */
    String getEdgeType();

    void setEdgeType(String edgeType);

    /**
     * Domain data carried by the edge — e.g. "label", "sourceLabel", "targetLabel"
     * for rendered text. Mirrors {@link GraphNode#getProperties()}; the framework
     * treats this as an opaque bag and never interprets specific keys itself.
     * Reserved for optional, per-edge data — not for {@code type}, which is a
     * first-class field since every edge is expected to carry one.
     */
    Map<String, Object> getProperties();
}
