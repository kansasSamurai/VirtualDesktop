package org.jwellman.demo.d3ish;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class StylePrototypeDemo {

    public static void main(String[] args) {

        // This ensures the tooltip pane itself doesn't add extra margins
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder());
        UIManager.put("ToolTip.background", new Color(0, 0, 0, 0)); // Transparent wrapper

        JFrame frame = new JFrame("Foundation StyleRegistry Prototype");

        BarChartPanelTwo chart = new BarChartPanelTwo();
        chart.setLayout(new BarChartLayout(15));

        FoundationInspector inspector = new FoundationInspector();

        // Add some forensic sample data
        double[] values = {0.7, 0.4, 0.9, 0.2};
        String[] labels = {"Postgres", "HSQLDB", "XSL-FO", "Servlet"};
        for (int i=0; i<values.length; i++) {
            BarComponent bar = chart.addBar(labels[i], values[i]);
            bar.addActionListener(e -> {
                if (bar.isSelected()) {
                    inspector.update(bar);
                }
            });
        }

        // Setup the SplitPane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chart, inspector);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.8); // Chart gets 80% of the growth space
        splitPane.setBorder(null);

        // The "Theme Switcher" Button
        JButton themeBtn = new JButton("Rotate Palette");
        themeBtn.addActionListener(e -> {
            StyleRegistry.rotatePalette();
            chart.refreshStyles();
        });

        JButton exportBtn = new JButton("Export to SVG");
        exportBtn.addActionListener( e -> {
            exportPanelToSvg(chart, new File("/dev/env/unittests/chartpanel.svg"));
        });

        JPanel pnlButtons = new JPanel();
        pnlButtons.add(themeBtn);
        pnlButtons.add(exportBtn);

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(pnlButtons, BorderLayout.SOUTH);

        frame.setSize(700, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    
    // ... Inside your export action context ...
    public static void exportPanelToSvg(javax.swing.JPanel targetPanel, File outputFile) {
        try {
            // 1. Initialize Batik's SVG DOM pipeline
            String svgNS = "http://www.w3.org/2000/svg";
            DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
            Document document = domImpl.createDocument(svgNS, "svg", null);

            // 2. Build the default generation context instead of letting the engine guess
            SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(document);

            // 2. Create the specialized graphics context matching your canvas size
            // This tells Batik to serialize text as vector vector geometries, not raw text tags!
            SVGGraphics2D svgGenerator = new SVGGraphics2D(ctx, true);
            svgGenerator.setSVGCanvasSize(targetPanel.getSize());

            // --- THE VECTOR GLYPH FORCE ---
//            svgGenerator.setEmbeddedFontsOn(false); // Disables raw font embedding hooks
//            svgGenerator.setTextAsShapes(true);    // <--- THIS IS THE MAGIC LINE            

            // 3. THE MAGIC INTERCEPT: Force the Swing container and all its
            // polymorphic children to draw their current visual vectors into Batik!
            targetPanel.printAll(svgGenerator);

            // 4. Stream it cleanly to disk using UTF-8 to protect any text primitives
            try (Writer out = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
                // Use 'true' to use standard CSS inline styling formatting
                svgGenerator.stream(out, true);
            }

            System.out.println("Vector HUD snapshot successfully saved to: " + outputFile.getAbsolutePath());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
