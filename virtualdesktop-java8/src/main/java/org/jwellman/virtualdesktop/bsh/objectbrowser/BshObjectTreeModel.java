package org.jwellman.virtualdesktop.bsh.objectbrowser;

import java.util.Arrays;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;

/**
 * Tree model for browsing the BeanShell namespace.
 *
 * <p>Implements lazy loading via TreeWillExpandListener: children
 * are only loaded when a node is about to be expanded.</p>
 *
 * @author Rick Wellman
 */
public class BshObjectTreeModel extends DefaultTreeModel implements TreeWillExpandListener {

    private final transient Interpreter interpreter;

    private static final long serialVersionUID = 1L;

    /**
     * Create a tree model for the given interpreter.
     *
     * @param interpreter the BeanShell interpreter
     */
    public BshObjectTreeModel(Interpreter interpreter) {
//        super(new BshObjectTreeNode("global", "global", interpreter));
// new [A]
        super(new BshObjectTreeNode(interpreter.getNameSpace().getName(), "global", interpreter));

        this.interpreter = interpreter;
        loadRootVariables();
    }

    /**
     * Load the root-level variables from global namespace.
     */
    private void loadRootVariables() {
        BshObjectTreeNode rootNode = (BshObjectTreeNode) getRoot();
        rootNode.removeAllChildren();

        try {
            // Get the global namespace
            NameSpace globalNs = interpreter.getNameSpace();
            System.out.println("loadRootVariables.globalNs : " + globalNs.getName());
            String[] varNames = globalNs.getVariableNames();

            if (varNames != null) {
                Arrays.sort(varNames);
                for (String varName : varNames) {
//                    String fullPath = "global." + varName;
// new [A]
                    String fullPath = varName;

                    BshObjectTreeNode childNode = new BshObjectTreeNode(
                        varName, fullPath, interpreter);
                    rootNode.add(childNode);
                }
            }
        } catch (Exception e) {
            rootNode.add(new javax.swing.tree.DefaultMutableTreeNode(
                "Error loading namespace: " + e.getMessage()));
        }

        // Mark root as loaded since we just loaded its children
        try {
            java.lang.reflect.Field loadedField = BshObjectTreeNode.class.getDeclaredField("loaded");
            loadedField.setAccessible(true);
            loadedField.setBoolean(rootNode, true);
        } catch (Exception e) {
            // Ignore reflection errors
        }
    }

    /**
     * Refresh the tree model, reloading all variables from the namespace.
     */
    public void refresh() {
// new [A]
        String nsName = interpreter.getNameSpace().getName();
        BshObjectTreeNode newRoot = new BshObjectTreeNode(nsName, "global", interpreter);
        setRoot(newRoot);

        loadRootVariables();
        nodeStructureChanged((BshObjectTreeNode) getRoot());
    }

    /**
     * Get a variable value by path.
     *
     * @param path the full path (e.g., "global.foo.bar")
     * @return the object value, or null
     */
    public Object getValueAt(String path) {
        try {
            // TODO this is probably where the "value" issue is coming from
            return interpreter.get(path);
        } catch (EvalError e) {
            return null;
        }
    }

    // TreeWillExpandListener implementation

    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
        TreePath path = event.getPath();
        Object lastComponent = path.getLastPathComponent();

        if (lastComponent instanceof BshObjectTreeNode) {
            BshObjectTreeNode node = (BshObjectTreeNode) lastComponent;
            if (!node.isLoaded()) {
                node.loadChildren();
                nodeStructureChanged(node);
            }
        }
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
        // No action needed on collapse
    }

}
