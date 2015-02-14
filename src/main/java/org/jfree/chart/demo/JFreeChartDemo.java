package org.jfree.chart.demo;

/**
 * JFreeChartDemo
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * More than 150 demo applications are included with the JFreeChart Developer
 * Guide. For more information, see:
 *
 * JFreeChart Developer's Guide
 */
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * JFreeChartDemo
 * Modified very slightly to expose the JPanel 
 * and JFreeChart objects as properties.
 * Also, it no longer extends JFrame.
 * 
 * @author John B. Matthews
 */
public class JFreeChartDemo {

    private static final int MAX = 8;
    private static final Random random = new Random();
    
    private JFrame frame;
    private JPanel panel;
    private JFreeChart chart;

    /**
     * Construct a new frame
     *
     * @param title the frame title
     */
    public JFreeChartDemo(String title) {
        this.frame = new JFrame(title);
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // This is a bit unconventional but it works
        createMainUI(this);
        this.frame.setContentPane(this.panel);
    }
    
    /**
     * This constructor is used when the panel is being injected
     * into any application that supports JPanels.
     */
    public JFreeChartDemo() {
    }

    /**
     * This is really where the app is created;
     * this method allows this class to be used stand-alone or
     * injected into any application that supports JPanels.
     *
     * @return
     */
    public static JFreeChartDemo createMainUI(JFreeChartDemo demo) {
        if (demo == null) demo = new JFreeChartDemo();
        
        demo.panel = new JPanel(new BorderLayout());

        final DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("Series0", createSeries(0));
        dataset.addSeries("Series1", createSeries(1));

        demo.chart = createChart(dataset);

        ChartPanel chartPanel = new ChartPanel(demo.chart, false);
        chartPanel.setPreferredSize(new Dimension(640, 480));
        chartPanel.setBackground(Color.white);
        demo.panel.add(chartPanel, BorderLayout.CENTER);

        demo.chart.setBackgroundPaint(chartPanel.getBackground());

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Series");
        buttonPanel.add(addButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int n = dataset.getSeriesCount();
                dataset.addSeries("Series" + n, createSeries(n));
            }
        });
        JButton remButton = new JButton("Remove Series");
        buttonPanel.add(remButton);
        remButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int n = dataset.getSeriesCount() - 1;
                dataset.removeSeries("Series" + n);
            }
        });

        demo.panel.add(buttonPanel, BorderLayout.SOUTH);

        return demo;
    }

    /**
     * Create a series
     *
     * @ return the series
     */
    private static double[][] createSeries(int mean) {
        double[][] series = new double[2][MAX];
        for (int i = 0; i < MAX; i++) {
            series[0][i] = (double) i;
            series[1][i] = mean + random.nextGaussian() / 2;
        }
        return series;
    }

    /**
     * Create a chart.
     *
     * @param dataset the dataset
     * @return the chart
     */
    private static JFreeChart createChart(XYDataset dataset) {

        // create the chart...
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Serial Data", // chart title
                "Domain", // domain axis label
                "Range", // range axis label
                dataset, // initial series
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips?
                false // URLs?
                );

        // set chart background
        chart.setBackgroundPaint(Color.white);

        // set a few custom plot features
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.blue); //(new Color(0xffffe0));
        plot.setBackgroundAlpha(0.05f);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);

        // set the plot's axes to display integers
        TickUnitSource ticks = NumberAxis.createIntegerTickUnits();
        NumberAxis domain = (NumberAxis) plot.getDomainAxis();
        domain.setStandardTickUnits(ticks);
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setStandardTickUnits(ticks);

        Stroke stroke = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL);

        // render shapes and lines
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setBaseShapesVisible(true);
        renderer.setBaseShapesFilled(true);
        renderer.setBaseOutlineStroke(stroke); // set the renderer's stroke        

        // label the points
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        XYItemLabelGenerator generator =
                new StandardXYItemLabelGenerator(
                StandardXYItemLabelGenerator.DEFAULT_ITEM_LABEL_FORMAT,
                format, format);
        renderer.setBaseItemLabelGenerator(generator);
        renderer.setBaseItemLabelsVisible(true);

        plot.setRenderer(renderer);

        return chart;
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                JFreeChartDemo demo = new JFreeChartDemo("JFreeChartDemo");
                demo.frame.pack();
                demo.frame.setLocationRelativeTo(null);
                demo.frame.setVisible(true);
            }
        });
    }

    /**
     * @return the panel
     */
    public JPanel getPanel() {
        return panel;
    }

    /**
     * @param panel the panel to set
     */
    public void setPanel(JPanel panel) {
        this.panel = panel;
    }

    /**
     * @return the chart
     */
    public JFreeChart getChart() {
        return chart;
    }

    /**
     * @param chart the chart to set
     */
    public void setChart(JFreeChart chart) {
        this.chart = chart;
    }

}