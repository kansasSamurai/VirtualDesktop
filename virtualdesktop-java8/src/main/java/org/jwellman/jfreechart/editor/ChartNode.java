package org.jwellman.jfreechart.editor;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jfree.chart.JFreeChart;

/**
 * Creates a tree node for JFreeChart chart objects
 * and creates child nodes corresponding to properties of the chart.
 * 
 * @author rwellman
 *
 */
public class ChartNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 1L;

    private JFreeChart chart;

    public ChartNode(JFreeChart jfc) {
        super(jfc, true);

        this.chart = jfc;
        this.addChildren();
    }

    private void addChildren() {
        // Title
        this.add(new TitleNode(chart.getTitle()));
        // Subtitles (if any)
        List<?> subtitleList = chart.getSubtitles();
        for (Object subtitle : subtitleList) {
            this.add(new SubtitleNode(subtitle));
        }
        // Legend
        this.add(new LegendNode(chart.getLegend()));
        // Border
        // Plot
        this.add(new PlotNode(chart.getPlot()));
    }

    @Override
    public String toString() {
        return JFCEditor.Card.Chart.toString();
    }

}
