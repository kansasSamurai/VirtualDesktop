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
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.util.DefaultShadowGenerator;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleInsets;

/**
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

        theme.setPlotBackgroundPaint(transparent);
        theme.setChartBackgroundPaint(transparent);

        theme.setTitlePaint(slate);
        theme.setTickLabelPaint(slate);
        theme.setSubtitlePaint(slate);
        theme.setRangeGridlinePaint(midgray);

        theme.setSmallFont(FONTS[0]);
        theme.setRegularFont(FONTS[0]); // axis labels
        theme.setLargeFont(FONTS[0]);
        theme.setExtraLargeFont(FONTS[1]);

        theme.setAxisLabelPaint(slate);
        theme.setAxisOffset(RectangleInsets.ZERO_INSETS);
        //theme.setBarPainter(new StandardBarPainter());

        theme.apply(chart);

        chart.getTitle().setHorizontalAlignment(HorizontalAlignment.CENTER);

        final LegendTitle t = chart.getLegend();
        if (t != null) { // Some charts don't have legends (piechart)
            t.setBorder(0, 0, 0, 0);
            t.setBackgroundPaint(transparent);
            t.setItemFont(FONTS[0].deriveFont(Font.BOLD));
        }

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
