package org.jwellman.swing.jtree;

import java.awt.LayoutManager;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

@SuppressWarnings("serial")
public class SwingTreeNode extends DefaultMutableTreeNode {

    public SwingTreeNode(JComponent c) {
        super(c, true);

        if (c instanceof JPanel) {
            final JPanel p = (JPanel)c;
            LayoutManager layout = p.getLayout();
            this.add(new DefaultMutableTreeNode(layout.getClass().getName(), false));
        }

        final int n = c.getComponentCount();
        for (int i=0; i < n; i++) {
            if (c.getComponent(i) instanceof JComponent) {
                final JComponent inner = (JComponent)c.getComponent(i);
                this.add(new SwingTreeNode(inner));
            }
        }
    }

    private JComponent getWrapped() {
        return (JComponent)this.getUserObject();
    }
    @Override
    public boolean isLeaf() {
        return this.getWrapped().getComponentCount() == 0;
    }

    @Override
    public String toString() {
        return this.getUserObject().getClass().getSimpleName();
    }

}
