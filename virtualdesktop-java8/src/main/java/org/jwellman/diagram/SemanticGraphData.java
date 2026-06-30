package org.jwellman.diagram;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for the semantic graph portion of a saved diagram.
 */
public class SemanticGraphData {

    private List<GraphNodeData> nodes = new ArrayList<>();
    private List<GraphEdgeData> edges = new ArrayList<>();

    public List<GraphNodeData> getNodes() { return nodes; }
    public void setNodes(List<GraphNodeData> nodes) { this.nodes = nodes; }

    public List<GraphEdgeData> getEdges() { return edges; }
    public void setEdges(List<GraphEdgeData> edges) { this.edges = edges; }
}
