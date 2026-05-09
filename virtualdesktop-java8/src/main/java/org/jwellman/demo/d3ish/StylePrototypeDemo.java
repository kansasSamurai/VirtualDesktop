package org.jwellman.demo.d3ish;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

public class StylePrototypeDemo {

    public static void main(String[] args) {

        // This ensures the tooltip pane itself doesn't add extra margins
        UIManager.put("ToolTip.border", BorderFactory.createEmptyBorder());
        UIManager.put("ToolTip.background", new Color(0,0,0,0)); // Transparent wrapper
        
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

        frame.add(splitPane, BorderLayout.CENTER);
        frame.add(themeBtn, BorderLayout.SOUTH);
        
        frame.setSize(700, 450);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}
