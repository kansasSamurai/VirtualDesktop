package org.jwellman.jfreechart.editor;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jfree.chart.title.LegendTitle;

public class LegendNode extends DefaultMutableTreeNode {

    private LegendTitle legend;

    private static final long serialVersionUID = 1L;

    public LegendNode(LegendTitle o) {
        super(o, true);

        this.legend = o;
    }

    @Override
    public String toString() {
        return "Legend";
    }

}
