/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jwellman.jfreechart;

import java.awt.Color;
import java.text.SimpleDateFormat;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.renderer.xy.XYSplineRenderer.FillType;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;

/**
 *
 * @author Rick
 */
public class TimeSeriesTest {

    public JFreeChart chart;

    public XYPlot plot;

    public XYItemRenderer renderer;

    private Dataset dataset;

    public TimeSeriesTest() {

        this.createDummyXYTimeTable();

        final DateAxis domainAxis = new DateAxis("-Domain-");
        domainAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
        domainAxis.setDateFormatOverride(new SimpleDateFormat("yyMM"));
        domainAxis.setVerticalTickLabels(false);
        domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, 1));

        final ValueAxis rangeAxis = new NumberAxis("-Range-");

        plot = new XYPlot((XYDataset)this.dataset, domainAxis, rangeAxis, this.createBaseRenderer());

        this.chart = ( new JFreeChart("A-Time-Series-Chart", plot) );
    }

    private XYItemRenderer createBaseRenderer() {
        return this.renderer = new XYLineAndShapeRenderer(true, true);
    }

    private void createDummyXYTimeTable() {
        TimeTableXYDataset ds = new TimeTableXYDataset();

        final int daycount = 5; // you can adjust this for more data
        final Day[] days = new Day[daycount];
        for (int i=0; i<daycount; i++) {
            days[i] = new Day(i+1, 3, 2005);
        }

        final String[] categories = {"Series 1", "Series 2", "Series 3"};
        for (int cat=0; cat<categories.length; cat++) {
            for (int i=0; i<daycount; i++) {
                ds.add(days[i], Math.random()*categories.length, categories[cat]);
            }
        }

        this.dataset = ds;
    }

    public XYSplineRenderer getSplineWithFill(int prec, int type) {
        switch (type) {
            case 2: return new XYSplineRenderer(prec, FillType.TO_LOWER_BOUND);
            case 3: return new XYSplineRenderer(prec, FillType.TO_UPPER_BOUND);
        }
        return new XYSplineRenderer(prec, FillType.TO_ZERO);
    }

    public void setChartTextColor(Color c) {
        // bsh % test.chart.getTitle().setPaint(Color.lightGray);
        this.chart.getTitle().setPaint(c);
        this.chart.getLegend().setItemPaint(c);
        plot.getDomainAxis().setLabelPaint(c);
        plot.getDomainAxis().setTickLabelPaint(c);
        plot.getRangeAxis().setLabelPaint(c);
        plot.getRangeAxis().setTickLabelPaint(c);
    }

}
