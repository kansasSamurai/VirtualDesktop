package org.jwellman.jfreechart.editor;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jfree.chart.title.TextTitle;

public class TitleNode extends DefaultMutableTreeNode {

    private TextTitle title;

    private static final long serialVersionUID = 1L;

    public TitleNode(TextTitle t) {
        super(t, true);

        this.title = t;
    }

    @Override
    public String toString() {
        return "Title";
    }

}
