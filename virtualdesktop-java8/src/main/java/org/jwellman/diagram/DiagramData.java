package org.jwellman.diagram;

import java.util.List;

/**
 * Root diagram data structure
 */
public class DiagramData {
    private FileVersion version;
    private String domainType;
    private String themeName;
    private int gridSize;
    private boolean snapToGrid;
    private int activeLayer;
    private List<LayerData> layers;
    private SemanticGraphData semanticGraph;

    public FileVersion getVersion() {
        return version;
    }

    public void setVersion(FileVersion version) {
        this.version = version;
    }

    public String getDomainType() {
        return domainType;
    }

    public void setDomainType(String domainType) {
        this.domainType = domainType;
    }

    public String getThemeName() {
        return themeName;
    }

    public void setThemeName(String themeName) {
        this.themeName = themeName;
    }

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

    public SemanticGraphData getSemanticGraph() {
        return semanticGraph;
    }

    public void setSemanticGraph(SemanticGraphData semanticGraph) {
        this.semanticGraph = semanticGraph;
    }
}
