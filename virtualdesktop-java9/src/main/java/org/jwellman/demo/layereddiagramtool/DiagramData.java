package org.jwellman.demo.layereddiagramtool;

import java.util.List;

/**
 * Root diagram data structure
 */
public class DiagramData {
    private int gridSize;
    private boolean snapToGrid;
    private int activeLayer;
    private List<LayerData> layers;

    public int getGridSize() {
        return gridSize;
    }

    public void setGridSize(int gridSize) {
        this.gridSize = gridSize;
    }

    public boolean isSnapToGrid() {
        return snapToGrid;
    }

    public void setSnapToGrid(boolean snapToGrid) {
        this.snapToGrid = snapToGrid;
    }

    public int getActiveLayer() {
        return activeLayer;
    }

    public void setActiveLayer(int activeLayer) {
        this.activeLayer = activeLayer;
    }

    public List<LayerData> getLayers() {
        return layers;
    }

    public void setLayers(List<LayerData> layers) {
        this.layers = layers;
    }
}
