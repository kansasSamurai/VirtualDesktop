package org.jwellman.swing.grid;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class GridRow {

    private final Map<String, Object> data = new LinkedHashMap<>();
    private int depth = 0;
    private boolean expanded = false;
    private boolean hasChildren = false;
    private String parentId = null;
    private boolean selected = false;
    private boolean processing = false;
    private final Map<String, String> tags = new HashMap<>();

    public GridRow() {}

    /** Fluent data setter; returns {@code this} for chaining. */
    public GridRow put(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public Object get(String key) { return data.get(key); }

    public String getTag(String key) { return tags.get(key); }

    public GridRow setTag(String key, String value) {
        tags.put(key, value);
        return this;
    }

    public Map<String, Object> getData() { return data; }
    public Map<String, String> getTags() { return tags; }

    public int getDepth() { return depth; }
    public GridRow setDepth(int depth) { this.depth = depth; return this; }

    public boolean isExpanded() { return expanded; }
    public GridRow setExpanded(boolean expanded) { this.expanded = expanded; return this; }

    public boolean isHasChildren() { return hasChildren; }
    public GridRow setHasChildren(boolean v) { this.hasChildren = v; return this; }

    public String getParentId() { return parentId; }
    public GridRow setParentId(String id) { this.parentId = id; return this; }

    public boolean isSelected() { return selected; }
    public GridRow setSelected(boolean selected) { this.selected = selected; return this; }

    public boolean isProcessing() { return processing; }
    public GridRow setProcessing(boolean processing) { this.processing = processing; return this; }
}
