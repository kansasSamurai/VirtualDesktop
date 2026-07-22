package org.jwellman.virtualdesktop.tools;

/**
 * Application-level tool lifecycle operations.
 *
 * <p>Step 1 of the migration exposes {@link #open(String)}. Close / activate /
 * minimize follow in later steps. Implementations must not require callers to
 * pass Swing types.</p>
 */
public interface ToolService {

    /**
     * Opens a tool from the catalog by definition id.
     *
     * @param definitionId id of a {@link ToolDefinition} in the catalog
     */
    void open(String definitionId);

}
