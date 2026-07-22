package org.jwellman.virtualdesktop.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple {@link ToolCatalog} backed by a LinkedHashMap (insertion order).
 */
public final class InMemoryToolCatalog implements ToolCatalog {

    private final Map<String, ToolDefinition> byId = new LinkedHashMap<String, ToolDefinition>();

    /**
     * Registers a definition. If the id already exists, the existing entry is kept
     * (first registration wins) and this call is ignored.
     *
     * @param definition the definition to add
     * @return the definition stored under that id (existing or newly added)
     */
    public ToolDefinition register(ToolDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        ToolDefinition existing = byId.get(definition.getId());
        if (existing != null) {
            return existing;
        }
        byId.put(definition.getId(), definition);
        return definition;
    }

    @Override
    public ToolDefinition findById(String id) {
        if (id == null) {
            return null;
        }
        return byId.get(id);
    }

    @Override
    public Collection<ToolDefinition> getAll() {
        return Collections.unmodifiableCollection(byId.values());
    }

    @Override
    public int size() {
        return byId.size();
    }

}
