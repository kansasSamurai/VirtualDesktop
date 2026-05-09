package org.jwellman.demo.d3ish;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class ProfileBuilderTool extends JPanel {

    private final JPanel previewPane = new JPanel();

    public ProfileBuilderTool() {
        setLayout(new BorderLayout());

        // INITIAL WIRING (Level 3)
        // This is done once: tell the system what semantic tokens the UI uses
        FoundationStyleEngine.mapComponent("bar.fill", "ui.primary");
        FoundationStyleEngine.mapComponent("panel.bg", "ui.background");

        // UI SETUP
        JColorChooser colorChooser = new JColorChooser(new Color(227, 24, 55)); // Chiefs Red
        colorChooser.getSelectionModel().addChangeListener(e -> updateTheme(colorChooser.getColor()));

        previewPane.setPreferredSize(new Dimension(300, 300));
        
        add(colorChooser, BorderLayout.CENTER);
        add(previewPane, BorderLayout.SOUTH);
        
        // Initial build
        updateTheme(colorChooser.getColor());
    }

    private void updateTheme(Color brandColor) {
        // LAYER 1: Generate the Primitives
        Color[] scale = ColorScaleGenerator.generateScale(brandColor);
        FoundationStyleEngine.setPrimitive("p-washout", scale[0]);
        FoundationStyleEngine.setPrimitive("p-main", scale[2]);
        FoundationStyleEngine.setPrimitive("p-ink", scale[4]);

        // LAYER 2: Map to Semantics (The "Mode" Logic)
        // To toggle Dark Mode, you'd just swap p-washout and p-ink here
        FoundationStyleEngine.mapSemantic("ui.primary", "p-main");
        FoundationStyleEngine.mapSemantic("ui.background", "p-ink");

        // APPLY TO PREVIEW
        previewPane.setBackground(FoundationStyleEngine.getStyle("panel.bg"));
        previewPane.repaint();
        
        System.out.println("Theme updated: " + brandColor);
    }

}
