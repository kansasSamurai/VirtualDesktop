package org.jwellman.jpreechart;

import java.awt.Color;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.demo.PieChartDemo1;
import org.jfree.chart.plot.PiePlot;

/**
 *
 * @author rwellman
 */
public class Demo {
    
    public static void main() {
        int jan = 2000;
        int feb = 1000;
        System.out.println(jan+feb);
        ChartPanel demo = (ChartPanel) PieChartDemo1.createDemoPanel();
        PiePlot plot = (PiePlot) demo.getChart().getPlot();
        plot.setSectionPaint(1, Color.red);
    }
    
}
