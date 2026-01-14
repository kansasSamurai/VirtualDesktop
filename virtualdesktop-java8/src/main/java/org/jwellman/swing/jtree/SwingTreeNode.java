package org.jwellman.swing.jtree;

import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

@SuppressWarnings("serial")
public class SwingTreeNode extends DefaultMutableTreeNode {

    // Allow some types to specify a better toString() value (such as JPanel)
    private String tostring;

    public SwingTreeNode(JComponent c) {
        super(c, true);

        Component[] children = c.getComponents();
        for (Object obj : children) {
            if (obj instanceof JComponent) {
                final JComponent inner = (JComponent)obj;

                if (c instanceof JPanel) {
                    final JPanel p = (JPanel)c;
                    LayoutManager layout = p.getLayout();
                    this.tostring = "JPanel [" + layout.getClass().getSimpleName() + "]";
                }

                this.add(new SwingTreeNode(inner));
            }
        }

    }

    /**
     * Convenience method for getting user object already cast to JComponent.
     * 
     * @return the JComponent that this node represents
     */
    public JComponent getWrapped() {
        return (JComponent)this.getUserObject();
    }

    @Override
    public boolean isLeaf() {
        return this.getWrapped().getComponentCount() == 0;
    }

    @Override
    public String toString() {
        if (tostring != null)
            return tostring;

        return this.getUserObject().getClass().getSimpleName();
    }

}
