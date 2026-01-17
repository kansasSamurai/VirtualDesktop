package org.jwellman.virtualdesktop.state.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Immutable state representing all running tool instances.
 *
 * Maintains two indexes:
 * - toolsById: for looking up tools by their unique ID
 * - toolsByType: for grouping tools by their class name
 *
 * @author rwellman
 */
public final class ToolsState {

    private final Map<String, ToolInstance> toolsById;
    private final Map<String, Set<String>> toolsByType;

    private ToolsState(Map<String, ToolInstance> toolsById, Map<String, Set<String>> toolsByType) {
        this.toolsById = Collections.unmodifiableMap(new HashMap<>(toolsById));
        // Deep copy the toolsByType map
        Map<String, Set<String>> typesCopy = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : toolsByType.entrySet()) {
            typesCopy.put(entry.getKey(), Collections.unmodifiableSet(new HashSet<>(entry.getValue())));
        }
        this.toolsByType = Collections.unmodifiableMap(typesCopy);
    }

    /**
     * Create an empty tools state.
     */
    public static ToolsState empty() {
        return new ToolsState(
            Collections.<String, ToolInstance>emptyMap(),
            Collections.<String, Set<String>>emptyMap()
        );
    }

    // ========== Getters ==========

    public Map<String, ToolInstance> getToolsById() {
        return toolsById;
    }

    public Map<String, Set<String>> getToolsByType() {
        return toolsByType;
    }

    public ToolInstance getTool(String toolId) {
        return toolsById.get(toolId);
    }

    public Set<String> getToolsOfType(String toolType) {
        Set<String> ids = toolsByType.get(toolType);
        return ids != null ? ids : Collections.<String>emptySet();
    }

    public int getToolCount() {
        return toolsById.size();
    }

    // ========== Copy-on-write modifiers ==========

    public ToolsState withToolAdded(ToolInstance tool) {
        Map<String, ToolInstance> newById = new HashMap<>(toolsById);
        newById.put(tool.getId(), tool);

        Map<String, Set<String>> newByType = new HashMap<>(toolsByType);
        Set<String> typeSet = newByType.get(tool.getToolType());
        if (typeSet == null) {
            typeSet = new HashSet<>();
        } else {
            typeSet = new HashSet<>(typeSet);
        }
        typeSet.add(tool.getId());
        newByType.put(tool.getToolType(), typeSet);

        return new ToolsState(newById, newByType);
    }

    public ToolsState withToolRemoved(String toolId) {
        ToolInstance tool = toolsById.get(toolId);
        if (tool == null) {
            return this;
        }

        Map<String, ToolInstance> newById = new HashMap<>(toolsById);
        newById.remove(toolId);

        Map<String, Set<String>> newByType = new HashMap<>(toolsByType);
        Set<String> typeSet = newByType.get(tool.getToolType());
        if (typeSet != null) {
            typeSet = new HashSet<>(typeSet);
            typeSet.remove(toolId);
            if (typeSet.isEmpty()) {
                newByType.remove(tool.getToolType());
            } else {
                newByType.put(tool.getToolType(), typeSet);
            }
        }

        return new ToolsState(newById, newByType);
    }

    public ToolsState withToolUpdated(ToolInstance updatedTool) {
        if (!toolsById.containsKey(updatedTool.getId())) {
            return this;
        }

        Map<String, ToolInstance> newById = new HashMap<>(toolsById);
        newById.put(updatedTool.getId(), updatedTool);

        // toolsByType doesn't need to change since tool type is immutable
        return new ToolsState(newById, toolsByType);
    }

    @Override
    public String toString() {
        return "ToolsState{count=" + toolsById.size() + ", types=" + toolsByType.keySet() + "}";
    }

}
