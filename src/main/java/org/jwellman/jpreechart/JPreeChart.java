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
    
    final protected static Color slate = Color.decode("#333333");
    final protected static Color midgray = Color.decode("#C0C0C0");
    final protected static Color transparent = new Color(255,255,255,0);
    
    final protected static String TITLE_FONT = "Arial"; // Lucida Bright
    
    final protected static BasicStroke STROKE_DEFAULT = 
            new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
    
    final protected static Font[] FONTS = {
        new Font(TITLE_FONT, Font.PLAIN, 12),
        new Font(TITLE_FONT, Font.PLAIN, 18),
        new Font(TITLE_FONT, Font.PLAIN, 24)
    };
    
    public JPreeChart() {
    }
    
    public void decorate(JFreeChart chart) {
        chart.setAntiAlias(true);
        chart.setTextAntiAlias(true);
        
        final StandardChartTheme theme = (StandardChartTheme)StandardChartTheme.createJFreeTheme();
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
        t.setBorder(0, 0, 0, 0);
        t.setBackgroundPaint(transparent);
        t.setItemFont(FONTS[0].deriveFont(Font.BOLD));
        
        final Plot p = chart.getPlot();
        p.setOutlineVisible(false);
        Object o = p.getDrawingSupplier();
        p.setDrawingSupplier(this.createDrawingSupplier());
        o = p.getDrawingSupplier();

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
        
        final Paint[] paintseq = {
            Color.decode("#CD4E3C"),
            Color.decode("#579340"),
            Color.decode("#5582A6"),
            Color.decode("#CA4FCC"),
            Color.decode("#7D6EC7"),
            Color.decode("#A87829")
        };
        
        final Stroke[] strokes = new Stroke[1];
        strokes[0] = STROKE_DEFAULT;
        
        final DrawingSupplier s = new DefaultDrawingSupplier(
            paintseq,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
            strokes,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
            shapes
        );
        
        return s;
    }

    private void customizePlot(Plot p) {
        
    }
    
    private void customizePlot(XYPlot p) {
        p.setDomainGridlinesVisible(false);
        p.getRangeAxis().setLabelFont(FONTS[0].deriveFont(Font.BOLD));
        p.setShadowGenerator(new DefaultShadowGenerator());
    }
    
}
