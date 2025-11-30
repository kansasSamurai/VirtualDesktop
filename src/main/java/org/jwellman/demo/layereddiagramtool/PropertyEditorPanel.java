package org.jwellman.demo.layereddiagramtool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

/**
 * Property editor panel for editing component properties
 */
public class PropertyEditorPanel extends JPanel {

    private Component selectedComponent;
    private DiagramLayeredPane diagramPane;

    // Font property controls
    private JComboBox<String> fontNameCombo;
    private JSlider fontSizeSlider;
    private JLabel fontSizeLabel;
    private JToggleButton plainButton;
    private JToggleButton boldButton;
    private JToggleButton italicButton;
    private JPanel fontPropertiesPanel;

    // Color property controls
    private JToggleButton fillColorButton;
    private JToggleButton borderColorButton;
    private JToggleButton textColorButton;
    private ButtonGroup colorTargetGroup;
    private JPanel colorSwatchPanel;
    private JPanel colorPropertiesPanel;
    private List<Color> recentColors;

    private static final int SWATCH_SIZE = 24;
    private static final int MAX_RECENT_COLORS = 20;

    private static final long serialVersionUID = 1L;

    public PropertyEditorPanel(DiagramLayeredPane diagramPane) {
        this.diagramPane = diagramPane;
        this.recentColors = new ArrayList<>();

        // Initialize with some default colors
        initializeDefaultColors();

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Properties"));

        // Create property panels
        createFontPropertiesPanel();
        createColorPropertiesPanel();

        // Initially show "No selection" message
        showNoSelectionMessage();
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

    private void createFontPropertiesPanel() {
        fontPropertiesPanel = new JPanel();
        fontPropertiesPanel.setLayout(new BoxLayout(fontPropertiesPanel, BoxLayout.Y_AXIS));

        // Font Name
        JPanel fontNamePanel = new JPanel(new BorderLayout(5, 5));
        fontNamePanel.add(new JLabel("Font:"), BorderLayout.WEST);

        // Get available fonts
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();

        fontNameCombo = new JComboBox<>(fontNames);
        fontNameCombo.addActionListener(e -> updateFontName());
        fontNamePanel.add(fontNameCombo, BorderLayout.CENTER);
        fontPropertiesPanel.add(fontNamePanel);
        fontPropertiesPanel.add(Box.createVerticalStrut(5));

        // Font Size (slider)
        JPanel fontSizePanel = new JPanel(new BorderLayout(5, 5));
        fontSizeLabel = new JLabel("Size: 16");
        fontSizePanel.add(fontSizeLabel, BorderLayout.WEST);

        fontSizeSlider = new JSlider(10, 100, 16);
        fontSizeSlider.setMajorTickSpacing(10);
        fontSizeSlider.setMinorTickSpacing(5);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.addChangeListener(e -> {
            if (!fontSizeSlider.getValueIsAdjusting()) {
                updateFontSize();
            }
            // Update label while dragging
            fontSizeLabel.setText("Size: " + fontSizeSlider.getValue());
        });
        fontSizePanel.add(fontSizeSlider, BorderLayout.CENTER);
        fontPropertiesPanel.add(fontSizePanel);
        fontPropertiesPanel.add(Box.createVerticalStrut(5));

        // Font Style (toggle buttons)
        JPanel fontStylePanel = new JPanel(new BorderLayout(5, 5));
        fontStylePanel.add(new JLabel("Style:"), BorderLayout.WEST);

        JPanel styleButtonsPanel = new JPanel();
        styleButtonsPanel.setLayout(new BoxLayout(styleButtonsPanel, BoxLayout.X_AXIS));

        plainButton = new JToggleButton("Plain");
        boldButton = new JToggleButton("Bold");
        italicButton = new JToggleButton("Italic");

        // Add action listeners to handle Plain/Bold mutual exclusivity
        plainButton.addActionListener(e -> {
            if (plainButton.isSelected()) {
                boldButton.setSelected(false);
            }
            updateFontStyle();
        });

        boldButton.addActionListener(e -> {
            if (boldButton.isSelected()) {
                plainButton.setSelected(false);
            }
            updateFontStyle();
        });

        italicButton.addActionListener(e -> updateFontStyle());

        styleButtonsPanel.add(plainButton);
        styleButtonsPanel.add(Box.createHorizontalStrut(3));
        styleButtonsPanel.add(boldButton);
        styleButtonsPanel.add(Box.createHorizontalStrut(3));
        styleButtonsPanel.add(italicButton);

        fontStylePanel.add(styleButtonsPanel, BorderLayout.CENTER);
        fontPropertiesPanel.add(fontStylePanel);
    }

    private void createColorPropertiesPanel() {
        colorPropertiesPanel = new JPanel();
        colorPropertiesPanel.setLayout(new BoxLayout(colorPropertiesPanel, BoxLayout.Y_AXIS));

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
        colorPropertiesPanel.add(colorTargetPanel);
        colorPropertiesPanel.add(Box.createVerticalStrut(5));

        // Color swatch panel
        JPanel swatchContainerPanel = new JPanel(new BorderLayout());
        swatchContainerPanel.add(new JLabel("Colors:"), BorderLayout.NORTH);

        colorSwatchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        colorSwatchPanel.setBorder(BorderFactory.createEtchedBorder());
        updateColorSwatches();

        swatchContainerPanel.add(colorSwatchPanel, BorderLayout.CENTER);
        colorPropertiesPanel.add(swatchContainerPanel);
        colorPropertiesPanel.add(Box.createVerticalStrut(5));

        // More Colors button
        JButton moreColorsButton = new JButton("More Colors...");
        moreColorsButton.addActionListener(e -> chooseCustomColor());
        colorPropertiesPanel.add(moreColorsButton);
    }

    private void updateColorSwatches() {
        colorSwatchPanel.removeAll();

        for (Color color : recentColors) {
            ColorSwatch swatch = new ColorSwatch(color);
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

        Color newColor = JColorChooser.showDialog(this, "Choose Color", currentColor);
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

            diagramPane.notifyModified();
        }
    }

    private void showNoSelectionMessage() {
        removeAll();
        JLabel messageLabel = new JLabel("No component selected");
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        messageLabel.setForeground(java.awt.Color.GRAY);
        add(messageLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showFontProperties() {
        removeAll();
        add(fontPropertiesPanel, BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    private void showColorProperties() {
        removeAll();
        add(colorPropertiesPanel, BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    private void showFontAndColorProperties() {
        removeAll();
        JPanel combinedPanel = new JPanel();
        combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.Y_AXIS));
        combinedPanel.add(fontPropertiesPanel);
        combinedPanel.add(Box.createVerticalStrut(10));
        combinedPanel.add(colorPropertiesPanel);
        add(combinedPanel, BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    public void setSelectedComponent(Component component) {
        this.selectedComponent = component;

        if (component == null) {
            showNoSelectionMessage();
        } else if (component instanceof DiagramText) {
            DiagramText textComponent = (DiagramText) component;
            loadFontProperties(textComponent);
            // Show text color button for DiagramText
            textColorButton.setVisible(true);
            showFontAndColorProperties();
        } else if (component instanceof DiagramColorable) {
            // Hide text color button for shapes
            textColorButton.setVisible(false);
            showColorProperties();
        } else {
            showNoSelectionMessage();
        }
    }

    private void loadFontProperties(DiagramText textComponent) {
        // Temporarily disable listeners to prevent triggering updates while loading
        java.awt.event.ActionListener[] fontNameListeners = fontNameCombo.getActionListeners();
        javax.swing.event.ChangeListener[] fontSizeListeners = fontSizeSlider.getChangeListeners();
        java.awt.event.ActionListener[] plainListeners = plainButton.getActionListeners();
        java.awt.event.ActionListener[] boldListeners = boldButton.getActionListeners();
        java.awt.event.ActionListener[] italicListeners = italicButton.getActionListeners();

        for (java.awt.event.ActionListener listener : fontNameListeners) {
            fontNameCombo.removeActionListener(listener);
        }
        for (javax.swing.event.ChangeListener listener : fontSizeListeners) {
            fontSizeSlider.removeChangeListener(listener);
        }
        for (java.awt.event.ActionListener listener : plainListeners) {
            plainButton.removeActionListener(listener);
        }
        for (java.awt.event.ActionListener listener : boldListeners) {
            boldButton.removeActionListener(listener);
        }
        for (java.awt.event.ActionListener listener : italicListeners) {
            italicButton.removeActionListener(listener);
        }

        // Load values
        fontNameCombo.setSelectedItem(textComponent.getFontName());

        int fontSize = textComponent.getFontSize();
        fontSizeSlider.setValue(fontSize);
        fontSizeLabel.setText("Size: " + fontSize);

        // Set toggle button states based on font style
        int style = textComponent.getFontStyle();
        boolean isBold = (style & Font.BOLD) != 0;
        boolean isItalic = (style & Font.ITALIC) != 0;

        plainButton.setSelected(!isBold && !isItalic);
        boldButton.setSelected(isBold);
        italicButton.setSelected(isItalic);

        // Re-add listeners
        for (java.awt.event.ActionListener listener : fontNameListeners) {
            fontNameCombo.addActionListener(listener);
        }
        for (javax.swing.event.ChangeListener listener : fontSizeListeners) {
            fontSizeSlider.addChangeListener(listener);
        }
        for (java.awt.event.ActionListener listener : plainListeners) {
            plainButton.addActionListener(listener);
        }
        for (java.awt.event.ActionListener listener : boldListeners) {
            boldButton.addActionListener(listener);
        }
        for (java.awt.event.ActionListener listener : italicListeners) {
            italicButton.addActionListener(listener);
        }
    }

    private void updateFontName() {
        if (selectedComponent instanceof DiagramText) {
            DiagramText textComponent = (DiagramText) selectedComponent;
            String fontName = (String) fontNameCombo.getSelectedItem();
            textComponent.setFontName(fontName);
            diagramPane.notifyModified();
        }
    }

    private void updateFontSize() {
        if (selectedComponent instanceof DiagramText) {
            DiagramText textComponent = (DiagramText) selectedComponent;
            int fontSize = fontSizeSlider.getValue();
            textComponent.setFontSize(fontSize);
            diagramPane.notifyModified();
        }
    }

    private void updateFontStyle() {
        if (selectedComponent instanceof DiagramText) {
            DiagramText textComponent = (DiagramText) selectedComponent;
            int style = Font.PLAIN;

            // Compute style from toggle button states
            if (boldButton.isSelected()) {
                style = Font.BOLD;
            }

            if (italicButton.isSelected()) {
                style |= Font.ITALIC;
            }

            // If neither Bold nor Italic is selected, ensure Plain is selected
            if (!boldButton.isSelected() && !italicButton.isSelected()) {
                style = Font.PLAIN;
                if (!plainButton.isSelected()) {
                    plainButton.setSelected(true);
                }
            }

            textComponent.setFontStyle(style);
            diagramPane.notifyModified();
        }
    }

    /**
     * Inner class for rendering color swatches
     */
    private class ColorSwatch extends JPanel {
        private Color color;
        private boolean hovered = false;

        private static final long serialVersionUID = 1L;

        public ColorSwatch(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
            setMinimumSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));
            setMaximumSize(new Dimension(SWATCH_SIZE, SWATCH_SIZE));

            // Add mouse listener for hover effect
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Fill with color
            g.setColor(color);
            g.fillRect(2, 2, SWATCH_SIZE - 4, SWATCH_SIZE - 4);

            // Draw border
            if (hovered) {
                g.setColor(Color.BLUE);
                g.drawRect(1, 1, SWATCH_SIZE - 3, SWATCH_SIZE - 3);
                g.drawRect(0, 0, SWATCH_SIZE - 1, SWATCH_SIZE - 1);
            } else {
                g.setColor(Color.GRAY);
                g.drawRect(1, 1, SWATCH_SIZE - 3, SWATCH_SIZE - 3);
            }
        }
    }
}
