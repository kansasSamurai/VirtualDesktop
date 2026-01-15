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
    private List<Color> userColors;

    private static final int SWATCH_SIZE = 24;

    // First row: 10 predefined colors (vibrant colors)
    private static final Color[] PREDEFINED_COLORS = {
        new Color(255, 0, 0),      // Red
        new Color(255, 127, 0),    // Orange
        new Color(255, 255, 0),    // Yellow
        new Color(0, 255, 0),      // Green
        new Color(0, 255, 255),    // Cyan
        new Color(0, 0, 255),      // Blue
        new Color(127, 0, 255),    // Purple
        new Color(255, 0, 255),    // Magenta
        new Color(127, 255, 0),    // Lime
        new Color(139, 69, 19)     // Brown
    };

    // Second row: 10 shades of grey from black to white
    private static final Color[] GREY_SHADES = {
        new Color(0, 0, 0),        // Black
        new Color(28, 28, 28),
        new Color(57, 57, 57),
        new Color(85, 85, 85),
        new Color(113, 113, 113),
        new Color(142, 142, 142),
        new Color(170, 170, 170),
        new Color(198, 198, 198),
        new Color(227, 227, 227),
        new Color(255, 255, 255)   // White
    };

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new color property panel
     */
    public ColorPropertyPanel() {
        this.userColors = new ArrayList<>();

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

        colorSwatchPanel = new JPanel();
        colorSwatchPanel.setLayout(new BoxLayout(colorSwatchPanel, BoxLayout.Y_AXIS));
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

        // Create first row for predefined colors
        JPanel predefinedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        for (Color color : PREDEFINED_COLORS) {
            ColorSwatch swatch = new ColorSwatch(color, SWATCH_SIZE);
            swatch.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    applyColor(color);
                }
            });
            predefinedRow.add(swatch);
        }
        colorSwatchPanel.add(predefinedRow);

        // Create second row for grey shades
        JPanel greyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        for (Color color : GREY_SHADES) {
            ColorSwatch swatch = new ColorSwatch(color, SWATCH_SIZE);
            swatch.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    applyColor(color);
                }
            });
            greyRow.add(swatch);
        }
        colorSwatchPanel.add(greyRow);

        // Create rows for user-defined colors (10 per row)
        int userColorCount = userColors.size();
        for (int i = 0; i < userColorCount; i += 10) {
            JPanel userRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
            int endIndex = Math.min(i + 10, userColorCount);
            for (int j = i; j < endIndex; j++) {
                Color color = userColors.get(j);
                ColorSwatch swatch = new ColorSwatch(color, SWATCH_SIZE);
                swatch.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        applyColor(color);
                    }
                });
                userRow.add(swatch);
            }
            colorSwatchPanel.add(userRow);
        }

        colorSwatchPanel.revalidate();
        colorSwatchPanel.repaint();
    }

    private void addUserColor(Color color) {
        // Don't add if it already exists in predefined colors
        for (Color predefined : PREDEFINED_COLORS) {
            if (predefined.equals(color)) {
                return;
            }
        }

        // Don't add if it already exists in grey shades
        for (Color grey : GREY_SHADES) {
            if (grey.equals(color)) {
                return;
            }
        }

        // Don't add if it already exists in user colors
        if (userColors.contains(color)) {
            return;
        }

        // Add to user colors
        userColors.add(color);

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
            addUserColor(newColor);
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
