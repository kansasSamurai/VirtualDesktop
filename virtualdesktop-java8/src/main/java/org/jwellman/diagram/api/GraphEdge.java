package org.jwellman.diagram.api;

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
}
