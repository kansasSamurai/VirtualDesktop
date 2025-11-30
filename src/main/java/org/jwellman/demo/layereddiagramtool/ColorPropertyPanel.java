package org.jwellman.demo.layereddiagramtool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 * Panel for editing color properties of diagram components.
 * Provides a color swatch palette for quick color reuse and
 * toggle buttons to select which property to apply colors to.
 */
public class ColorPropertyPanel extends JPanel {

    private Component selectedComponent;
    private Consumer<Void> modificationListener;

    // Color property controls
    private JToggleButton fillColorButton;
    private JToggleButton borderColorButton;
    private JToggleButton textColorButton;
    private ButtonGroup colorTargetGroup;
    private JPanel colorSwatchPanel;
    private List<Color> recentColors;

    private static final int SWATCH_SIZE = 24;
    private static final int MAX_RECENT_COLORS = 20;

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new color property panel
     */
    public ColorPropertyPanel() {
        this.recentColors = new ArrayList<>();

        // Initialize with some default colors
        initializeDefaultColors();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Create color property controls
        createColorControls();
    }

    /**
     * Sets the listener to be notified when a color property is modified
     * @param listener the modification listener
     */
    public void setModificationListener(Consumer<Void> listener) {
        this.modificationListener = listener;
    }

    /**
     * Sets the component whose color properties should be edited
     * @param component the component to edit
     */
    public void setSelectedComponent(Component component) {
        this.selectedComponent = component;

        // Show/hide text color button based on component type
        if (component instanceof DiagramText) {
            textColorButton.setVisible(true);
        } else {
            textColorButton.setVisible(false);
        }
    }

    private void initializeDefaultColors() {
        // Add some common colors to start with
        recentColors.add(Color.BLACK);
        recentColors.add(Color.WHITE);
        recentColors.add(Color.RED);
        recentColors.add(Color.GREEN);
        recentColors.add(Color.BLUE);
        recentColors.add(Color.YELLOW);
        recentColors.add(Color.ORANGE);
        recentColors.add(Color.GRAY);
    }

    private void createColorControls() {
        // Color target selection buttons
        JPanel colorTargetPanel = new JPanel(new BorderLayout(5, 5));
        colorTargetPanel.add(new JLabel("Apply to:"), BorderLayout.WEST);

        JPanel targetButtonsPanel = new JPanel();
        targetButtonsPanel.setLayout(new BoxLayout(targetButtonsPanel, BoxLayout.X_AXIS));

        fillColorButton = new JToggleButton("Fill");
        borderColorButton = new JToggleButton("Border");
        textColorButton = new JToggleButton("Text");

        colorTargetGroup = new ButtonGroup();
        colorTargetGroup.add(fillColorButton);
        colorTargetGroup.add(borderColorButton);
        colorTargetGroup.add(textColorButton);

        // Default to Fill
        fillColorButton.setSelected(true);

        targetButtonsPanel.add(fillColorButton);
        targetButtonsPanel.add(Box.createHorizontalStrut(3));
        targetButtonsPanel.add(borderColorButton);
        targetButtonsPanel.add(Box.createHorizontalStrut(3));
        targetButtonsPanel.add(textColorButton);

        colorTargetPanel.add(targetButtonsPanel, BorderLayout.CENTER);
        add(colorTargetPanel);
        add(Box.createVerticalStrut(5));

        // Color swatch panel
        JPanel swatchContainerPanel = new JPanel(new BorderLayout());
        swatchContainerPanel.add(new JLabel("Colors:"), BorderLayout.NORTH);

        colorSwatchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        colorSwatchPanel.setBorder(BorderFactory.createEtchedBorder());
        updateColorSwatches();

        swatchContainerPanel.add(colorSwatchPanel, BorderLayout.CENTER);
        add(swatchContainerPanel);
        add(Box.createVerticalStrut(5));

        // More Colors button
        JButton moreColorsButton = new JButton("More Colors...");
        moreColorsButton.addActionListener(e -> chooseCustomColor());
        add(moreColorsButton);
    }

    private void updateColorSwatches() {
        colorSwatchPanel.removeAll();

        for (Color color : recentColors) {
            ColorSwatch swatch = new ColorSwatch(color, SWATCH_SIZE);
            swatch.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    applyColor(color);
                }
            });
            colorSwatchPanel.add(swatch);
        }

        colorSwatchPanel.revalidate();
        colorSwatchPanel.repaint();
    }

    private void addRecentColor(Color color) {
        // Remove if already exists
        recentColors.remove(color);

        // Add to front
        recentColors.add(0, color);

        // Limit size
        if (recentColors.size() > MAX_RECENT_COLORS) {
            recentColors.remove(recentColors.size() - 1);
        }

        updateColorSwatches();
    }

    private void chooseCustomColor() {
        Color currentColor = Color.WHITE;

        // Get current color based on selected target
        if (selectedComponent instanceof DiagramColorable) {
            DiagramColorable colorable = (DiagramColorable) selectedComponent;
            if (fillColorButton.isSelected()) {
                currentColor = colorable.getFillColor();
            } else if (borderColorButton.isSelected()) {
                currentColor = colorable.getBorderColor();
            } else if (textColorButton.isSelected() && selectedComponent instanceof DiagramText) {
                currentColor = ((DiagramText) selectedComponent).getTextColor();
            }
        }

        Color newColor = JColorChooser.showDialog(
            SwingUtilities.getWindowAncestor(this),
            "Choose Color",
            currentColor);
        if (newColor != null) {
            addRecentColor(newColor);
            applyColor(newColor);
        }
    }

    private void applyColor(Color color) {
        if (selectedComponent instanceof DiagramColorable) {
            DiagramColorable colorable = (DiagramColorable) selectedComponent;

            if (fillColorButton.isSelected()) {
                colorable.setFillColor(color);
            } else if (borderColorButton.isSelected()) {
                colorable.setBorderColor(color);
            } else if (textColorButton.isSelected() && selectedComponent instanceof DiagramText) {
                ((DiagramText) selectedComponent).setTextColor(color);
            }

            // Notify listener of modification
            if (modificationListener != null) {
                modificationListener.accept(null);
            }
        }
    }
}
