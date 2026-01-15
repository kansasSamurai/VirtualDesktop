package org.jwellman.jfreechart;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryPointerAnnotation;
import org.jfree.chart.axis.SubCategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.chart.renderer.category.LevelRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.LengthAdjustmentType;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.graphics2d.svg.SVGGraphics2D;
import org.jfree.graphics2d.svg.SVGUtils;
// import org.junit.Test;

public class AnyChartTest {

	private final int CHART_WIDTH = (1000);
	private final int CHART_HEIGHT = (300); // length of annotation will vary based on this value - trial and error

	public static final Font FONT_ITEM_LABEL = new Font("Verdana", Font.PLAIN, 19); 
	// FontUtil.getFont(FontUtil.VERDANA, Font.BOLD, 19);
	public static final Color ORANGE_SAFETY = new Color(0xFFA836); // I like the marker against blue but the label against white is too light
	public static final Color ORANGE_PUMPKIN = new Color(0xF5761A); // better than metallic, not as good as spanish.
	public static final Color ORANGE_SPANISH = new Color(0xF06105); // good. better than metallic because it is darker.  also(IMO) the orange does not connotate "warning"
	public static final Color ORANGE_METALLIC = new Color(0xE26310); // not "fantastic" but acceptable
	public static final Color ANNOTATION = ORANGE_SAFETY;

	public JFreeChart chart;
	public ValueMarker upperMarker;
	public ValueMarker lowerMarker;
	public CategoryPointerAnnotation annotation;
	public CategoryItemRenderer scoreRenderer;

	private boolean showOptional = false;
	private final int[] studentscore = {8}; // {8, 5};
	private final String[] colkey = {"student1"}; // {"student1","student2"};



	public void createChart() throws IOException, CloneNotSupportedException {

		CategoryDataset chartData = getChartData();

		// Stacked Bar Chart, horizontal
		chart = ChartFactory.createStackedBarChart(
			showOptional ? "[chart-title-goes-here]" : "", // chart title
			"domain", // domain axis label - does not seem to affect this chart
			"range", // range axis label - does not seem to affect this chart
			chartData, // data
			PlotOrientation.HORIZONTAL, // the plot orientation
			false, // legend
			false, // tooltips
			false // urls
		);

		final SubCategoryAxis domainAxis = getDomainAxis();
		final GroupedStackedBarRenderer renderer = getBarRenderer();

		CategoryPlot plot = chart.getCategoryPlot();
		plot.setRenderer(renderer); // Apply all of our renderer settings to the chart we're creating
		plot.setDomainAxis(0, domainAxis); // Apply our domainAxis settings to the chart plot
		plot.setBackgroundPaint(Color.WHITE); // Default is gray, but we need a white background

		// Scale the vertical axis to +/- our calculated chart max range.
		// This also ensures our '0' point is always centered so we can line it up with 
		// the black proficiency line beside the chart in the final PDF
		//plot.getRangeAxis().setRange(0-chartData.getChartMaxRange(), chartData.getChartMaxRange());
// do these make a difference? A: they don't seem to really make a difference
//		plot.getRangeAxis().setUpperMargin(0);
//		plot.getRangeAxis().setLowerMargin(0);
		plot.getRangeAxis().setRange(-2, 10); // hardcoded for now, delta 2
		plot.getRangeAxis().setVisible(showOptional); // remove numbers from left(vert)/top(horz) sidebar

		// Adds the solid black line across the chart at the '0' point of the graph
		ValueMarker lowerMarker = createLowerMarker();
		plot.addRangeMarker(lowerMarker);

		plot.addRangeMarker(createMarker(2.0,""));
		plot.addRangeMarker(createMarker(6.0,""));

		ValueMarker upperMarker = createUpperMarker();
		plot.addRangeMarker(upperMarker);  

		// AxisSpace space = new AxisSpace();
		// space.setTop(100);
		// plot.setFixedRangeAxisSpace(space);

		// Add a fixed space to the bottom of the chart to hold the year/grade/subject labels
		// This lets us *know* where the black line across the chart will be because we know its
		// centered in the graph and we have a known space below the graph for the labels, regardless of their size
		/*AxisSpace space = new AxisSpace();
		space.setBottom(LEGEND_HEIGHT);
		plot.setFixedDomainAxisSpace(space);
		*/

		// Border around the chart
		plot.setOutlineVisible(false);
		//plot.setOutlineStroke(new BasicStroke(1.0f));

		// Spacing around the chart/alignment. 
		// chart.setPadding(new RectangleInsets(-6, -8, -6, -8));
		// plot.setAxisOffset(RectangleInsets.ZERO_INSETS);

		LevelRenderer localLevelRenderer = new LevelRenderer();
		localLevelRenderer.setDefaultStroke(new BasicStroke(5.0f));
		localLevelRenderer.setAutoPopulateSeriesStroke(false);
		// localLevelRenderer.setSeriesStroke(2, new BasicStroke(2.0f));
		// localLevelRenderer.setDefault
		// localLevelRenderer.setSeriesPaint(2, Color.red);
		localLevelRenderer.setDefaultPaint(Color.green);
		localLevelRenderer.setDefaultItemLabelFont(FONT_ITEM_LABEL);
		localLevelRenderer.setDefaultItemLabelsVisible(true);
		localLevelRenderer.setDefaultPositiveItemLabelPosition(
				new ItemLabelPosition(ItemLabelAnchor.OUTSIDE12, TextAnchor.BOTTOM_CENTER));
		localLevelRenderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
		localLevelRenderer.setItemLabelAnchorOffset(20);

		// plot.setDataset(1, getDataset2());
		// plot.setRenderer(1, localLevelRenderer);
		// SubCategoryAxis domainAxis2 = (SubCategoryAxis) domainAxis.clone();
		// domainAxis2.setLowerMargin(0.1);
		// domainAxis2.setUpperMargin(0.1);
		// plot.setDomainAxis(1, domainAxis2);

		// ====================================
		// Overlay the student score
		// ====================================

		plot.setDataset(1, getDataset3(studentscore));

		CategoryItemRenderer renderer3 = getStudentScoreRenderer();
		plot.setRenderer(1, renderer3);

		IntStream.range(0, studentscore.length)
			.forEach(index -> {
				CategoryPointerAnnotation anno = getCategoryPointer(studentscore[index], colkey[index]);
				plot.addAnnotation(anno);
				annotation = anno; // yes, this will overwrite the instance object but when there is only one it's fine, just for prototyping
			} );

		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		// Space left/right of the first/last bars in the graph
		// plot.getDomainAxis(1).setLowerMargin(0.1);
		// plot.getDomainAxis(1).setUpperMargin(0.1);

		// ====================================
		// Create an .svg file (optional if using desktop)
		// ====================================

		// Draw our chart into an SVG with a fixed size (ratio, the chart may be scaled in the PDF itself)
		SVGGraphics2D g2 = new SVGGraphics2D(CHART_WIDTH, CHART_HEIGHT);
		Rectangle r = new Rectangle(0, 0, CHART_WIDTH, CHART_HEIGHT);
		chart.draw(g2, r);

		// Write the svg to file
		String chartName = "chartTest_";
		File f = File.createTempFile(chartName, ".svg");
		SVGUtils.writeToSVG(f, g2.getSVGElement());

		// Make/move to dev unit tests folder
		Path devfile = Paths.get("C:\\dev\\env\\unittests\\chart_tests", chartName + ".svg");

		// as a convenience, make the directory if it does not exist
		Path folder = devfile.getParent();
		FileUtils.forceMkdir(folder.toFile());

		// copy the temp file to local path
		File destination = devfile.toFile();
		FileUtils.copyFile(f, destination);

		// delete temp file
		FileUtils.deleteQuietly(f);
	}

	private ValueMarker createMarker(double value, String label) {
		final ValueMarker marker = new ValueMarker(value);
		marker.setPaint(Color.BLACK);
		marker.setStroke(new BasicStroke(1.0f));
		// marker.setOutlinePaint(Color.BLACK);
		marker.setLabel(label);
		marker.setLabelFont(FONT_ITEM_LABEL);
		marker.setLabelBackgroundColor(Color.white);
		marker.setLabelAnchor(RectangleAnchor.TOP); // TOP_RIGHT 
		marker.setLabelTextAnchor(TextAnchor.TOP_CENTER); // TOP_LEFT
		return marker;
	}

	private ValueMarker createUpperMarker() {
		upperMarker = createMarker(8.0, "8 Points Earned");
		return upperMarker;
	}

	private ValueMarker createLowerMarker() {
		lowerMarker = createMarker(0.0, "0 Points Earned");
		return lowerMarker;
	}

	private CategoryPointerAnnotation getCategoryPointer(int studentscore, String colkey) {

		// TODO get numbers from actual properties
		final boolean overhalf = studentscore > (4/2);

		final int direction = overhalf ? 225 : (225+90);
		final TextAnchor anchor = overhalf ? TextAnchor.BOTTOM_RIGHT : TextAnchor.BOTTOM_LEFT;
		final CategoryPointerAnnotation anno = new CategoryPointerAnnotation(
				"Student's score", colkey, studentscore, Math.toRadians(direction)); // 4.71239);
		anno.setFont(FONT_ITEM_LABEL);
		anno.setPaint(ANNOTATION);

		anno.setLabelOffset(5);
		anno.setTextAnchor(anchor); // (TextAnchor.BOTTOM_CENTER);
		// anno.setCategoryAnchor(CategoryAnchor.MIDDLE);

		anno.setTipRadius(20);
		anno.setBaseRadius(75); // 70 // this value depends on both chart height and angle of the arrowannotation
		anno.setArrowWidth(5);
		anno.setArrowLength(10);
		anno.setArrowPaint(ANNOTATION);
		anno.setArrowStroke(new BasicStroke(3.0f)); // 5.0

		return anno;
	}

	private CategoryItemRenderer getStudentScoreRenderer() {
		scoreRenderer = new LineAndShapeRenderer();

		Double ellipse = new Ellipse2D.Double(-15d, -15d, 30d, 30d);
		IntStream.range(0, 7)
			.forEach(index -> {
				scoreRenderer.setSeriesShape(index, ellipse);
				scoreRenderer.setSeriesPaint(0, ANNOTATION);
			} );

		return scoreRenderer;
	}

	private SubCategoryAxis getDomainAxis() {
		// No overall label on the domain axis, but we'll be setting the labels of the
		// bars themselves here
		SubCategoryAxis domainAxis = new SubCategoryAxis("");
		// SubCategoryAxis domainAxis2 = new SubCategoryAxis("");

		// Spacing between items in different categories (between grades/subjects)
		domainAxis.setCategoryMargin(0.20);

		// Add the labels for the current/prev year sub-categories so they display below the bars
		//domainAxis.addSubCategory(data.getFormattedPrevYearLabel());
		//domainAxis.addSubCategory(data.getFormattedCurrentYearLabel());

		// Set the font for all of the labels under the bar graph
		//domainAxis.setLabelFont(itemLabelFont);
		//domainAxis.setSubLabelFont(itemLabelFont);
		//domainAxis.setTickLabelFont(itemLabelFont);

		domainAxis.setVisible(showOptional);
		domainAxis.setAxisLineVisible(showOptional);

		// Limit how much the labels under the chart can wrap to new lines,
		// as this could throw off the vertical alignment of our '0' axis bar
		domainAxis.setMaximumCategoryLabelLines(3);

		// Space left/right of the first/last bars in the graph
		domainAxis.setLowerMargin(0.2);
		domainAxis.setUpperMargin(0.4);

		return domainAxis;
	}

	private GroupedStackedBarRenderer getBarRenderer() {
		final GroupedStackedBarRenderer renderer = 
				// new GroupedStackedBarRenderer();
				new CustomLineAndShapeRenderer();

		// Set bar renderer to standard bar painter to get rid of the metallic reflections on the bars
		((BarRenderer) renderer).setBarPainter(new StandardBarPainter());
		// Remove the drop shadows from the bars, which is enabled by default
		// renderer.setShadowVisible(false);

		// Get group mappings from the chartData, this is what keeps current/prev year data paired together properly
		// renderer.setSeriesToGroupMap(chartData.getSeriesGroupMap());

		// Spacing between items in a category (between current and prev year bars)
		// renderer.setItemMargin(0.5);

		renderer.setDefaultPositiveItemLabelPosition(
				new ItemLabelPosition(ItemLabelAnchor.OUTSIDE6, TextAnchor.TOP_CENTER));

		// START OF Set colors and fonts of bars and their labels

		// Prev year data is graphed by series 0-4
		renderer.setSeriesPaint(0, Color.decode("#dceaf7")); // approaching/first negative/second lowest
		renderer.setSeriesPaint(1, Color.decode("#83cbeb")); // emerging/second negative/lowest
		renderer.setSeriesPaint(2, Color.decode("#4e95d9")); // at target/first positive/second highest

		// Use itemLabelFont for all bar labels, except override series 4 and 9 (the
		// percents that sit on top of the bars)
		// as they need to be a bit smaller to make sure they never are hidden by being
		// wider than the bar.
		renderer.setDefaultItemLabelFont(FONT_ITEM_LABEL);
		// renderer.setSeriesItemLabelFont(4, percentLabelFont);
		// renderer.setSeriesItemLabelFont(9, percentLabelFont);

		// Black labels by default, override white labels on the 2 dark bars for contrast
		renderer.setDefaultItemLabelPaint(Color.BLACK);

		renderer.setDefaultOutlinePaint(Color.BLACK);
		renderer.setDefaultOutlineStroke(new BasicStroke(2.0f));
		renderer.setDrawBarOutline(true);

		// renderer.setItemLabelInsets(null);

		// END OF Set colors and fonts of bars and their labels

		// Configure the render to use our custom label generator that will override the labels of the "percent" bar with
		// the percent of students at or above target, including the percent sign
		//renderer.setDefaultItemLabelGenerator(new DlmAggLabelGenerator(chartData));
		renderer.setDefaultItemLabelGenerator(new DescriptionGenerator());
		renderer.setDefaultItemLabelsVisible(true);

		return renderer;
	}

	private CategoryDataset getChartData() {

		DefaultCategoryDataset data = new DefaultCategoryDataset();

		IntStream.range(0, colkey.length)
		.forEach(index -> {
			data.addValue(2, "BELOW", colkey[index]);
			data.addValue(4, "NEAR", colkey[index]);
			data.addValue(2, "BEYOND", colkey[index]);
		} );

//		data.addValue(2, "Below many students\nat grade level", "student1");
//		data.addValue(4, "Near many students\nat grade level", "student1");
//		data.addValue(2, "Beyond many students\nat grade level", "student1");

		return data;
	}

	@SuppressWarnings("unused")
	private CategoryDataset getDataset2() {
		DefaultCategoryDataset data = new DefaultCategoryDataset();

		data.addValue(3, "studenta", "student1");
		// data.addValue(8, "studentd", "student1");

		return data;
	}

	// This is the value of the student score
	private CategoryDataset getDataset3(int[] studentscore) {
		DefaultCategoryDataset data = new DefaultCategoryDataset();

		String[] rowkey = {"studenta","studentb"};
		IntStream.range(0, studentscore.length)
			.forEach(index -> {
				data.addValue(studentscore[index], rowkey[index], colkey[index]); // 5
			} );

		return data;
	}

	static class CustomLineAndShapeRenderer extends GroupedStackedBarRenderer {

		private static final long serialVersionUID = 1L;

		// TODO create property/constructor
		private double percent = 0.1;

		@Override
		public void drawRangeMarker(Graphics2D g2, CategoryPlot plot, ValueAxis axis, Marker marker, Rectangle2D dataArea) {

			final PlotOrientation orientation = plot.getOrientation();

			// very slightly modified from original:  AbstractCategoryItemRenderer
			if (marker instanceof ValueMarker) {
				final ValueMarker vm = (ValueMarker) marker;
				if (!axis.getRange().contains(vm.getValue())) {
					return;
				}

				final Composite savedComposite = g2.getComposite();
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, marker.getAlpha()));

				final double v = axis.valueToJava2D(vm.getValue(), dataArea, plot.getRangeAxisEdge());

				// This is the original line - no longer drawn but used later for the text placement
				Line2D line = null;
				if (orientation == PlotOrientation.HORIZONTAL) {
					line = new Line2D.Double(v, dataArea.getMinY(), v, dataArea.getMaxY());
				} else if (orientation == PlotOrientation.VERTICAL) {
					line = new Line2D.Double(dataArea.getMinX(), v, dataArea.getMaxX(), v);
				} else {
					throw new IllegalStateException();
				}

				// Calculate an adjusted line to draw as the marker
				double linestart = (orientation == PlotOrientation.HORIZONTAL) ? dataArea.getMinY() : dataArea.getMinX();
				double lineend = (orientation == PlotOrientation.HORIZONTAL) ? dataArea.getMaxY() : dataArea.getMaxX();
				double length = lineend - linestart;
				double adjust = length * percent;
				linestart = linestart + adjust;
				lineend = lineend - (adjust*3);

				Line2D adjustlineStart = null, adjustlineEnd = null;
				if (orientation == PlotOrientation.HORIZONTAL) {
					adjustlineStart = new Line2D.Double(v, linestart, v, linestart+adjust);
					adjustlineEnd = new Line2D.Double(v, lineend-adjust, v, lineend);
				} else if (orientation == PlotOrientation.VERTICAL) {
					adjustlineStart = new Line2D.Double(linestart, v, linestart+adjust, v);
					adjustlineEnd = new Line2D.Double(lineend-adjust, v, lineend, v);
				} else {
					throw new IllegalStateException();
				}
				// end - Calculate an adjusted line to draw as the marker

				g2.setPaint(marker.getPaint());
				g2.setStroke(marker.getStroke());
				// draw the adjusted line, not the original
				g2.draw(adjustlineStart);
				g2.draw(adjustlineEnd);

				String label = marker.getLabel();
				RectangleAnchor anchor = marker.getLabelAnchor();
				if (label != null) {
					Font labelFont = marker.getLabelFont();
					g2.setFont(labelFont);
					Point2D coordinates = calculateRangeMarkerTextAnchorPoint(g2, orientation, dataArea,
							line.getBounds2D(), marker.getLabelOffset(), LengthAdjustmentType.EXPAND, anchor);
					Rectangle2D rect = TextUtils.calcAlignedStringBounds(label, g2, (float) coordinates.getX(),
							(float) coordinates.getY(), marker.getLabelTextAnchor());
					g2.setPaint(marker.getLabelBackgroundColor());
					g2.fill(rect);
					g2.setPaint(marker.getLabelPaint());
					TextUtils.drawAlignedString(label, g2, (float) coordinates.getX(), (float) coordinates.getY(),
							marker.getLabelTextAnchor());
				}
				g2.setComposite(savedComposite);
			}
		}

	}

	static class DescriptionGenerator extends StandardCategoryItemLabelGenerator {

		private static final long serialVersionUID = 1L;

		
		/**
		 * Creates a new label generator that displays the item value, replacing the label with the correct aggregate percent
		 * value when encountering "(Percent)" values, and suppressing the label when its less than the nonDisplayThreshold
		 * 
		 * @param category the category index (zero-based).
		 */
		public DescriptionGenerator() {
		}

		/**
		 * Generates a label for the specified item. The label is typically the data value, 
		 * but "(Percent)" labels are replaced with their corresponding value from aggregatePercents.
		 *
		 * @param dataset  the dataset (<code>null</code> not permitted).
		 * @param series   the series index (zero-based).
		 * @param category the category index (zero-based).
		 *
		 * @return the label (possibly <code>null</code>).
		 */
		@Override
		public String generateLabel(CategoryDataset dataset, int row, int column) {
			return (String) dataset.getRowKey(row); // "BELOW"; // "Below many students\nat grade level";//dataset.getRowKey(row).toString();
		}

		/**
		 * Not implemented
		 */
		public Object clone() {
			return null;
		}

	}

}
