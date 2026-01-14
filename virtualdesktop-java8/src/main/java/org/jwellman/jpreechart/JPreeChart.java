package org.jwellman.jpreechart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.util.DefaultShadowGenerator;
/* 1.0.17 (before 1.5.0 upgrade)
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleInsets;
*/

/**
 * AUG 2025 - JFreeChart has probably had themes for quite a while
 * and I just realized that this can almost surely be implemented as
 * an extension to the StandardChartTheme.  In my defense, I did create
 * this class around 10 years ago so maybe this does predate themes.
 * 
 * @author rwellman
 */
public class JPreeChart {

    private JFreeChart chart;

    private String fontName;

    protected String TITLE_FONT = "Segoe UI"; // Arial, Lucida Bright

    protected Font[] FONTS = {
        new Font(TITLE_FONT, Font.PLAIN, 12),
        new Font(TITLE_FONT, Font.PLAIN, 18),
        new Font(TITLE_FONT, Font.PLAIN, 24)
    };

    public StandardChartTheme theme = (StandardChartTheme)StandardChartTheme.createJFreeTheme();
    // Prevents the theme properties from being reset on subsequent runs
    public boolean isThemeInitialized = false;

    final public static Color slate = new Color(0x333333); // Color.decode("#333333");
    final public static Color midgray = new Color(0xC0C0C0); // Color.decode("#C0C0C0");
    final public static Color transparent = new Color(255,255,255,0);
    final public static BasicStroke STROKE_DEFAULT =
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);

    public JPreeChart() {
    }

    public void decorate(JFreeChart chart) {
        this.chart = chart;
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);

        if (isThemeInitialized) {
            // do nothing to the theme if it is marked as initialized
        } else {
            theme.setPlotBackgroundPaint(transparent);
            theme.setChartBackgroundPaint(transparent);
            theme.setLegendBackgroundPaint(transparent);

            theme.setAxisLabelPaint(slate);
            theme.setAxisOffset(RectangleInsets.ZERO_INSETS);
            theme.setTitlePaint(slate);
            theme.setTickLabelPaint(slate);
            theme.setSubtitlePaint(slate);
            theme.setLegendItemPaint(slate);
            theme.setRangeGridlinePaint(midgray);

            theme.setSmallFont(FONTS[0]);
            theme.setRegularFont(FONTS[0]); // axis labels
            theme.setLargeFont(FONTS[0]);
            theme.setExtraLargeFont(FONTS[1]);

            // Not sure if this will blow up if the chart is not a bar chart...
            theme.setBarPainter(new StandardBarPainter());

            isThemeInitialized = true;
        }
        theme.apply(chart);

        // Adjust the chart title
        chart.getTitle().setHorizontalAlignment(HorizontalAlignment.CENTER);
        final LegendTitle t = chart.getLegend();
        if (t != null) { // Some charts don't have legends (piechart)
            t.setBorder(0, 0, 0, 0);
            t.setBackgroundPaint(transparent);
            t.setItemFont(FONTS[0].deriveFont(Font.BOLD));
        }

        // Adjust the plot
        final Plot p = chart.getPlot();
        p.setOutlineVisible(false);
        p.setDrawingSupplier(this.createDrawingSupplier());

// Dec. 2020 - Not sure what I was doing here... copied line 2 then commented out:
//        Object o = p.getDrawingSupplier();
//        p.setDrawingSupplier(this.createDrawingSupplier());
//        o = p.getDrawingSupplier();

        if (p instanceof XYPlot) {
            XYPlot xyp = (XYPlot)p;
            this.customizePlot(xyp);

            XYItemRenderer r = xyp.getRenderer();
            if (r instanceof XYLineAndShapeRenderer) {
                XYLineAndShapeRenderer xyr = (XYLineAndShapeRenderer)r;
                xyr.clearSeriesPaints(true);

                // Clear any existing shapes on the renderer
                int i = 0;
                while (xyr.getSeriesShape(i) != null) {
                    xyr.setSeriesShape(i++, null);
                }

            }

        }

    }

    /** 
     * Set all painters.
     * 
     * A slight exaggeration... sets the text and tick painters
     * since these are often all the same color and this then
     * makes it easier to switch between light backgrounds/theme
     * to dark backgrounds/theme.
     * 
     * @param p
     */
    public void setAllPainters(Paint p) {
        theme.setTitlePaint(p);
        theme.setSubtitlePaint(p);
        theme.setAxisLabelPaint(p);
        theme.setTickLabelPaint(p);
        theme.setRangeGridlinePaint(p);
        theme.setLegendItemPaint(p);
    }

    public DrawingSupplier createDrawingSupplier() {

        double size = 2.0;
        double delta = 1.0; // := size / 2

        final Shape[] shapes = new Shape[1];
        shapes[0] = new Ellipse2D.Double(-delta, -delta, size, size);

        final Stroke[] strokes = new Stroke[1];
        strokes[0] = STROKE_DEFAULT;

        final Paint[] paintseq = {
                Color.decode("#CD4E3C"), // red
                Color.decode("#579340"), // green
                Color.decode("#5582A6"), // blue
                Color.decode("#CA4FCC"), // magenta
                Color.decode("#7D6EC7"), // purple
                Color.decode("#A87829")  // brown
            };

        final DrawingSupplier s = new DefaultDrawingSupplier(
            paintseq,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
            strokes,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
            shapes
        );

        return s;
    }

    public void customizePlot(Plot p) {
        // There aren't many properties that abstract Plot class
        // exposes that would be common for general use cases
        // (that I can identify as of yet)
    }

    public void customizePlot(XYPlot p) {
        p.setDomainGridlinesVisible(false);
        p.getRangeAxis().setLabelFont(FONTS[0].deriveFont(Font.BOLD));
        p.setShadowGenerator(new DefaultShadowGenerator());
    }

    public JFreeChart getChart() {
        return chart;
    }

    public void setChart(JFreeChart chart) {
        this.chart = chart;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;

        this.FONTS = new Font[] {
            new Font(this.fontName, Font.PLAIN, 12),
            new Font(this.fontName, Font.PLAIN, 18),
            new Font(this.fontName, Font.PLAIN, 24)
        };
    }

}
