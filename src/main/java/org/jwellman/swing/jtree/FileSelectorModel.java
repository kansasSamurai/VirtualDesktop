package org.jwellman.swing.jtree;

import java.util.Arrays;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

public class FileSelectorModel implements TreeModel {

    private FileNode root;

    /**
     * the constructor defines the root.
     */
    public FileSelectorModel(String directory) {
        root = new FileNode(directory);
        root.setRoot(true);
    }

    @Override
    public Object getRoot() {
        return root;
    }

    /**
     * returns the <code>parent</code>'s child located at index <code>index</code>.
     */
    @Override
    public Object getChild(Object parent, int index) {
        final FileNode parentNode = (FileNode) parent;
        return new FileNode(parentNode, parentNode.listFiles()[index].getName());
    }

    /**
     * returns the number of child nodes.  
     * 
     * If the node is not a directory, or its list of children is null, return 0.  
     * Otherwise, just return the number of files under the current file.
     * 
     */
    @Override
    public int getChildCount(Object parent) {
        FileNode parentNode = (FileNode) parent;
        if (parent == null 
                || !parentNode.isDirectory()
                || parentNode.listFiles() == null) {
            return 0;
        }

        return parentNode.listFiles().length;
    }

    /**
     * returns true if {{@link #getChildCount(Object)} is 0.
     */
    @Override
    public boolean isLeaf(Object node) {
        return (getChildCount(node) == 0);
    }

    /**
     * return the index of the child in the list of files under <code>parent</code>.
     */
    @Override
    public int getIndexOfChild(Object parent, Object child) {
        FileNode parentNode = (FileNode) parent;
        FileNode childNode = (FileNode) child;

        return Arrays.asList(parentNode.list()).indexOf(childNode.getName());
    }

    // The following methods are not implemented, as we won't need them for this example.

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
    }
    
}
