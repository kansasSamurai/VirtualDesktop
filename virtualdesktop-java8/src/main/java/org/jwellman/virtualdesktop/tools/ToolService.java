package org.jwellman.virtualdesktop.tools;

/**
 * Application-level tool lifecycle operations.
 *
 * <p>Callers depend on this interface — not on DesktopManager. The first
 * implementation is DesktopManager, which may still create VirtualAppFrames and
 * listen to them internally.</p>
 *
 * <p>{@link #open(String)} takes a <em>catalog definition id</em>.
 * {@link #close(String)}, {@link #activate(String)}, {@link #minimize(String)},
 * and {@link #restore(String)} take a <em>running tool instance id</em>
 * ({@code ToolInstance.id} / {@code VirtualAppFrame.toolId}).</p>
 *
 * <p>Display data for open tools (title, icon key) lives on {@code ToolInstance}
 * in the store — views do not ask this service for icons. {@code ToolsState} is
 * the authoritative registry of what is open; the Swing host keeps only a
 * realizer cache of frames.</p>
 */
public interface ToolService {

    /**
     * Opens a tool from the catalog by definition id.
     *
     * @param definitionId id of a {@link ToolDefinition} in the catalog
     */
    void open(String definitionId);

    /**
     * Withdraws a running tool from view.
     *
     * <p><strong>Transitional:</strong> until tools have a cleanup / close SPI,
     * this minimizes (iconifies) rather than disposing. The instance remains in
     * {@code ToolsState}. Real destroy + {@code TOOL_CLOSED} is backlog.</p>
     *
     * @param toolId running instance id
     */
    void close(String toolId);

    /**
     * Brings a running tool to the front and selects it (de-iconifies if needed).
     *
     * @param toolId running instance id
     */
    void activate(String toolId);

    /**
     * Minimizes / iconifies a running tool.
     *
     * @param toolId running instance id
     */
    void minimize(String toolId);

    /**
     * Restores a minimized tool without necessarily selecting it as activate would.
     *
     * @param toolId running instance id
     */
    void restore(String toolId);

}
