package org.jwellman.diagram;

import java.util.List;

/**
 * Layer data structure
 */
public class LayerData {
    private int layerDepth;
    private boolean visible;
    private List<ComponentData> components;

    public int getLayerDepth() {
        return layerDepth;
    }

    public void setLayerDepth(int layerDepth) {
        this.layerDepth = layerDepth;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<ComponentData> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentData> components) {
        this.components = components;
    }
}
