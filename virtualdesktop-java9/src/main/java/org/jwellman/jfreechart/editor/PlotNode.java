package org.jwellman.jfreechart.editor;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jfree.chart.plot.Plot;

public class PlotNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;

    private Plot plot;

    public PlotNode(Plot p) {
        super(p, true);

        this.plot = p;
        this.addChildren();
    }

    private void addChildren() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String toString() {
        return "Plot: " + this.plot.getPlotType();
    }

}
