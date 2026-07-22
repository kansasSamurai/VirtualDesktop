package org.jwellman.virtualdesktop.tools;

import java.util.Collection;

/**
 * Queryable set of {@link ToolDefinition} entries — what the desktop can launch.
 *
 * <p>No Swing types. Launch surfaces and {@link ToolService} depend on this,
 * not on a list of Actions.</p>
 */
public interface ToolCatalog {

    /**
     * @param id definition id
     * @return the definition, or null if unknown
     */
    ToolDefinition findById(String id);

    /**
     * @return all definitions in registration order
     */
    Collection<ToolDefinition> getAll();

    /**
     * @return number of definitions
     */
    int size();

}
