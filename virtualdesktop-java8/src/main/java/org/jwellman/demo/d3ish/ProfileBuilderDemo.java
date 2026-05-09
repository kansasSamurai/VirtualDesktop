package org.jwellman.demo.d3ish;

import javax.swing.*;
import java.awt.*;

@SuppressWarnings("serial")
public class ProfileBuilderDemo extends JFrame {

    private final JPanel previewCanvas;
    private boolean isDarkMode = true;

    public ProfileBuilderDemo() {
        setTitle("Foundation Profile Builder");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- STEP 1: INITIAL COMPONENT WIRING (LEVEL 3) ---
        // We do this once. Components are "wired" to intents.
        FoundationStyleEngine.mapComponent("app.background", "ui.surface");
        FoundationStyleEngine.mapComponent("bar.main", "ui.action");
        FoundationStyleEngine.mapComponent("text.primary", "ui.text");

        // --- STEP 2: UI CONTROLS ---
        previewCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(FoundationStyleEngine.getStyle("app.background"));
                g.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw a sample "Bar"
                g.setColor(FoundationStyleEngine.getStyle("bar.main"));
                g.fillRoundRect(50, 50, 200, 40, 15, 15);
            }
        };

        JColorChooser colorChooser = new JColorChooser(new Color(227, 24, 55)); // Chiefs Red
        colorChooser.getSelectionModel().addChangeListener(e -> updateLevels(colorChooser.getColor()));

        JButton modeToggle = new JButton("Toggle Dark/Light Mode");
        modeToggle.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            updateLevels(colorChooser.getColor());
        });

        // Layout
        add(colorChooser, BorderLayout.NORTH);
        add(previewCanvas, BorderLayout.CENTER);
        add(modeToggle, BorderLayout.SOUTH);

        setSize(700, 800);
        updateLevels(colorChooser.getColor()); // Initial run
    }

    private void updateLevels(Color brandColor) {
        // --- LEVEL 1: GENERATE PRIMITIVES ---
        Color[] scale = PaletteGenerator.generateScale(brandColor);
        FoundationStyleEngine.setPrimitive("p-washout", scale[0]);
        FoundationStyleEngine.setPrimitive("p-main", scale[2]);
        FoundationStyleEngine.setPrimitive("p-ink", scale[4]);
        FoundationStyleEngine.setPrimitive("white", Color.WHITE);
        FoundationStyleEngine.setPrimitive("black", Color.BLACK);

        // --- LEVEL 2: MAP SEMANTICS (The "Mode" Logic) ---
        FoundationStyleEngine.mapSemantic("ui.action", "p-main");
        
        if (isDarkMode) {
            FoundationStyleEngine.mapSemantic("ui.surface", "p-ink");
            FoundationStyleEngine.mapSemantic("ui.text", "white");
        } else {
            FoundationStyleEngine.mapSemantic("ui.surface", "p-washout");
            FoundationStyleEngine.mapSemantic("ui.text", "black");
        }

        previewCanvas.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProfileBuilderDemo().setVisible(true));
    }

}
