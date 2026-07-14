package org.jwellman.diagram;

import java.util.Map;

/**
 * JSON-serializable snapshot of a graph edge.
 */
public class GraphEdgeData {

    private String id;
    private String sourceNodeId;
    private String sourcePortId;
    private String targetNodeId;
    private String targetPortId;
    private String lineStyle;       // EdgeAttributes.LineStyle name
    private String arrowType;       // EdgeAttributes.ArrowType name
    private String sourceArrowType; // EdgeAttributes.ArrowType name; absent = NONE (backward compat)
    private String type;            // named relationship kind, e.g. "Composition"; null = untyped
    private Map<String, Object> properties; // e.g. "label", "sourceLabel", "targetLabel"; absent on older files

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceNodeId() { return sourceNodeId; }
    public void setSourceNodeId(String sourceNodeId) { this.sourceNodeId = sourceNodeId; }

    public String getSourcePortId() { return sourcePortId; }
    public void setSourcePortId(String sourcePortId) { this.sourcePortId = sourcePortId; }

    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }

    public String getTargetPortId() { return targetPortId; }
    public void setTargetPortId(String targetPortId) { this.targetPortId = targetPortId; }

    public String getLineStyle() { return lineStyle; }
    public void setLineStyle(String lineStyle) { this.lineStyle = lineStyle; }

    public String getArrowType() { return arrowType; }
    public void setArrowType(String arrowType) { this.arrowType = arrowType; }

    public String getSourceArrowType() { return sourceArrowType; }
    public void setSourceArrowType(String sourceArrowType) { this.sourceArrowType = sourceArrowType; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
}
