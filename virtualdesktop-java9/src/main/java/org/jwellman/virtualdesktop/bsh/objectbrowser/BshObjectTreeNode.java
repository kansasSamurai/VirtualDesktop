package org.jwellman.virtualdesktop.bsh.objectbrowser;

import javax.swing.tree.DefaultMutableTreeNode;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.XThis;

/**
 * Tree node representing an object in the BeanShell namespace.
 *
 * <p>Supports lazy loading: children are only loaded when the node
 * is expanded, avoiding the cost of traversing the entire object graph.</p>
 *
 * @author Rick Wellman
 */
public class BshObjectTreeNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final String fullPath;
    private String typeName;
    private boolean loaded;
    private boolean isXThis;
    private final transient Interpreter interpreter;

    /**
     * Create a new tree node for a BeanShell object.
     *
     * @param name the variable name (e.g., "notepad")
     * @param fullPath the full path (e.g., "global.notepad")
     * @param interpreter the BeanShell interpreter for resolving objects
     */
    public BshObjectTreeNode(String name, String fullPath, Interpreter interpreter) {
        super(name);
        this.name = name;
        this.fullPath = fullPath;
        this.interpreter = interpreter;
        this.loaded = false;
        this.isXThis = false;
        this.typeName = "unknown";

        // Determine type information
        resolveTypeInfo();
    }

    /**
     * Resolve the object and determine its type.
     */
    private void resolveTypeInfo() {
        try {
            Object obj = interpreter.get(fullPath);
            if (obj != null) {
                this.typeName = obj.getClass().getName();
                this.isXThis = (obj instanceof XThis);
            } else {
                this.typeName = "null";
                this.isXThis = false;
            }
        } catch (EvalError e) {
            this.typeName = "error: " + e.getMessage();
            this.isXThis = false;
        }
    }

    /**
     * Load children for this node if it represents an XThis object.
     * This is called lazily when the node is expanded.
     */
    public void loadChildren() {
        if (loaded) {
            return;
        }
        loaded = true;

        if (!isXThis) {
            return;
        }

        try {
            Object obj = interpreter.get(fullPath);
            if (obj instanceof XThis) {
                XThis xthis = (XThis) obj;
                // Access the variables field via reflection or direct access
                String[] children = getVariables(xthis);
                if (children != null) {
                    java.util.Arrays.sort(children);
                    for (String childName : children) {
                        String childPath = fullPath + "." + childName;
                        BshObjectTreeNode childNode = new BshObjectTreeNode(
                            childName, childPath, interpreter);
                        add(childNode);
                    }
                }
            }
        } catch (EvalError e) {
            // Add an error node
            add(new DefaultMutableTreeNode("Error: " + e.getMessage()));
        }
    }

    /**
     * Get the variables array from an XThis object.
     * BeanShell XThis has a public 'variables' field.
     */
    private String[] getVariables(XThis xthis) {
        try {
            // XThis has a public field 'variables' that contains variable names
            java.lang.reflect.Field field = XThis.class.getField("variables");
            return (String[]) field.get(xthis);
        } catch (Exception e) {
            // Fallback: try to get via namespace
            try {
                return xthis.getNameSpace().getVariableNames();
            } catch (Exception e2) {
                return null;
            }
        }
    }

    /**
     * Check if children have been loaded.
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Force reload of children.
     */
    public void reload() {
        removeAllChildren();
        loaded = false;
        resolveTypeInfo();
        loadChildren();
    }

    @Override
    public boolean isLeaf() {
        // XThis nodes are not leaves (they can have children)
        // Non-XThis nodes are leaves
        return !isXThis;
    }

    @Override
    public boolean getAllowsChildren() {
        return isXThis;
    }

    // Getters

    public String getName() {
        return name;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean isXThis() {
        return isXThis;
    }

    @Override
    public String toString() {
        return name;
    }
}
