package org.jwellman.virtualdesktop.vapps;

import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.swing.JPanel;

import com.xeiam.xchart.Chart;
import com.xeiam.xchart.ChartBuilder;
import com.xeiam.xchart.Series;
import com.xeiam.xchart.XChartPanel;
import com.xeiam.xchart.StyleManager.ChartTheme;
import com.xeiam.xchart.StyleManager.ChartType;

public class SpecXChartBarChart extends VirtualAppSpec {

    final Color[] PALETTE = {
            Color.decode("#CD4E3C"), // red
            Color.decode("#579340"), // green
            Color.decode("#5582A6"), // blue
            Color.decode("#CA4FCC"), // magenta
            Color.decode("#7D6EC7"), // purple
            Color.decode("#A87829")  // brown
        };

    final Color[] PALETTE1 = {
            Color.decode("#4A148C"), // purple900
            Color.decode("#62468D"), // purple800
            Color.decode("#886AB5"), // purple700
            Color.decode("#967BBD"), // purple600
            Color.decode("#AA99C4"), // purple500
            Color.decode("#B19DCE")  // purple400
        };
//    --cg1-1: #BEAED7;
//    --cg1-2: ;
//    --cg1-3: ;
//    --cg1-4: ;
//    --cg1-5: ;
//    --cg1-6: ;
//    --cg1-7: 

    public SpecXChartBarChart() {
        this.setTitle("XChart Barchart");
        this.setContent(this.getChart());
    }

    public JPanel getChart() {

        // Create Chart
        Chart chart = new ChartBuilder().chartType(ChartType.Bar)
//                .title("Demo")
                .width(400).height(300)
                .theme(ChartTheme.GGPlot2)
                .build();
        chart.getStyleManager().setLegendVisible(false);
        chart.getStyleManager().setDatePattern("EE");

        double value = Math.random()*80;
        for (int j=0; j<6; j++) {
            // generate data // remove generics for bsh script
            Collection<Date> xData = new ArrayList<>();
            Collection<Number> yData = new ArrayList<>();

            LocalDate date = LocalDate.now(); // start today
            for (int i=0; i<7; i++) {
                xData.add(java.sql.Date.valueOf(date));
                value = value - Math.random()*5;
                yData.add(value);
                date = date.plusDays(1);
            }

            Series s = chart.addDateSeries("S" + j, xData, yData);
            s.setLineColor(PALETTE1[j]);
        }

        XChartPanel p = new XChartPanel(chart);
        return p;
    }

}
