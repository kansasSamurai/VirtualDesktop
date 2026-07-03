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
}
