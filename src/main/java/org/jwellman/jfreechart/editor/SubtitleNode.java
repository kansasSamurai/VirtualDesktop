package org.jwellman.jfreechart.editor;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;

public class SubtitleNode extends DefaultMutableTreeNode {

    // I may remove subtitle property eventually as it is
    // somewhat redundant with node.getUserObject() but
    // it is at least typed a little more strongly.
    private Title subtitle;
    private String type;

    private static final long serialVersionUID = 1L;

    public SubtitleNode(Object o) {
        super(o, true);

        this.subtitle = (Title) o;
        if (subtitle instanceof TextTitle) {
            type = "Text";
        } else if (subtitle instanceof LegendTitle) {
            type = "Legend";
        }
    }

    @Override
    public String toString() {
        return "Subtitle: " + type;
    }

}
