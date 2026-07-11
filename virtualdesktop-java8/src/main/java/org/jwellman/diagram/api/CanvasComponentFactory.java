package org.jwellman.diagram.api;

import java.util.Map;
import java.util.function.BiConsumer;

import javax.swing.JPanel;

/**
 * Domain plugin point: creates Swing content panels for graph node types
 * and declares which ports each node type exposes.
 *
 * The framework wraps the returned JPanel in a NodeHostPanel shell;
 * domain implementations never extend framework classes.
 */
public interface CanvasComponentFactory {

    /**
     * Returns a pure-Swing JPanel representing the visual content for a node.
     * The framework will embed this inside a NodeHostPanel.
     */
    JPanel createContentFor(String nodeType, Map<String, Object> properties);

    /** Returns the port IDs that nodes of this type expose (e.g. "N","S","E","W"). */
    String[] getPortIds(String nodeType);

    /**
     * Creates content for a node and supplies an {@code onModified} callback that the
     * content panel should invoke after the user commits in-panel edits.
     * Domain factories that support in-panel editing should override this method.
     * The default delegates to the 2-arg form (callback is silently ignored).
     */
    default JPanel createContentFor(String nodeType,
                                    Map<String, Object> properties,
                                    Runnable onModified) {
        return createContentFor(nodeType, properties);
    }

    /**
     * Returns a JPanel property editor for a selected node, or {@code null} if this
     * factory does not support in-panel editing for the given type.
     *
     * @param nodeType   the node type string (e.g. "CLASS", "INTERFACE")
     * @param properties the live mutable properties map of the selected node;
     *                   the editor should mutate this directly before calling onChanged
     * @param onChanged  callback to invoke after properties are mutated; the framework
     *                   will rebuild and swap the node's visual content in response
     */
    default JPanel createPropertyEditorFor(String nodeType,
                                           Map<String, Object> properties,
                                           Runnable onChanged) {
        return null;
    }

    /**
     * Returns a JPanel details editor for a selected node, or {@code null} if this
     * factory does not provide a details panel for the given type.
     * Shown in the Details tab of the tool when a node is selected and cleared on deselection.
     *
     * @param nodeType   the node type string (e.g. "CLASS", "INTERFACE")
     * @param properties the live mutable properties map of the selected node
     * @param onCommit   callback to invoke after changes are committed; the framework
     *                   will rebuild and swap the node's visual content in response
     */
    default JPanel createDetailsPanelFor(String nodeType,
                                          Map<String, Object> properties,
                                          Runnable onCommit) {
        return null;
    }

    /**
     * Stable identifier used to persist the diagram's domain type.
     * Must match the name passed to {@code LayeredDiagramTool.registerDiagramType()}.
     * Returns {@code null} for plain/untyped diagrams.
     */
    default String getDomainTypeId() {
        return null;
    }

    /** Label for the right-panel "Nodes" toggle tab. */
    default String getNodePaletteTitle() {
        return "Nodes";
    }

    /**
     * Returns a palette panel for adding new nodes to the canvas, or {@code null} if
     * this factory has no node palette.
     *
     * @param addNode callback: {@code (nodeType, properties)} — the framework creates and
     *                places the node; domain code must not import framework canvas classes.
     */
    default JPanel createNodePalettePanel(BiConsumer<String, Map<String, Object>> addNode) {
        return null;
    }

    /**
     * Updates the theme this factory uses for subsequently created node content.
     * Called by {@code DiagramLayeredPane.setTheme()} during a runtime theme switch.
     * The default is a no-op for factories that do not hold a theme reference.
     */
    default void setTheme(CanvasTheme theme) {
    }
}
