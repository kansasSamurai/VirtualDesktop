package org.jwellman.xchart;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import com.xeiam.xchart.Chart;
import com.xeiam.xchart.ChartBuilder;
import com.xeiam.xchart.SwingWrapper;
import com.xeiam.xchart.demo.charts.ExampleChart;

/** Day Scale */
public class DateChart05 implements ExampleChart {

	public static void main(String[] args) {

		ExampleChart exampleChart = new DateChart05();
		Chart chart = exampleChart.getChart();
		new SwingWrapper(chart).displayChart();
	}

	@Override
	public Chart getChart() {

		// Create Chart
		Chart chart = new ChartBuilder().width(800).height(600).title("Drive Space").build();
		chart.getStyleManager().setLegendVisible(false);

		// generate data // remove generics for bsh script
		Collection<Date> xData = new ArrayList<Date>();
		Collection<Number> yData = new ArrayList<Number>();

		Date date = new Date();
		xData.add(date);
		yData.add(104.5);

		chart.addDateSeries("C:", xData, yData);

		return chart;

	}
	
	public Chart getChart_original() {

		// Create Chart
		Chart chart = new ChartBuilder().width(800).height(600).title("Day Scale").build();
		chart.getStyleManager().setLegendVisible(false);

		// generate data
		Collection<Date> xData = new ArrayList<Date>();
		Collection<Number> yData = new ArrayList<Number>();

		Random random = new Random();

		DateFormat sdf = new SimpleDateFormat("MM-dd");
		Date date = null;
		for (int i = 1; i <= 14; i++) {
			try {
				date = sdf.parse("02-" + (6 * i + random.nextInt(2)));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			xData.add(date);
			yData.add(Math.random() * i);
		}

		chart.addDateSeries("blah", xData, yData);

		return chart;

	}

}
