package org.jwellman.diagram;

import java.util.Map;

/**
 * JSON-serializable snapshot of a graph node's identity, type, domain properties,
 * and canvas position.
 */
public class GraphNodeData {

    private String id;
    private String type;
    private Map<String, Object> properties;
    private int x;
    private int y;
    private int w;
    private int h;
    private int layer;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getW() { return w; }
    public void setW(int w) { this.w = w; }

    public int getH() { return h; }
    public void setH(int h) { this.h = h; }

    public int getLayer() { return layer; }
    public void setLayer(int layer) { this.layer = layer; }
}
