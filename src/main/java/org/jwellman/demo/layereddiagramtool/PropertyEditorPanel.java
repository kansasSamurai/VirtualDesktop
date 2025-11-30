package org.jwellman.demo.layereddiagramtool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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

    private static final long serialVersionUID = 1L;

    public PropertyEditorPanel(DiagramLayeredPane diagramPane) {
        this.diagramPane = diagramPane;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Properties"));

        // Create font properties panel
        createFontPropertiesPanel();

        // Initially show "No selection" message
        showNoSelectionMessage();
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

    public void setSelectedComponent(Component component) {
        this.selectedComponent = component;

        if (component == null) {
            showNoSelectionMessage();
        } else if (component instanceof DiagramText) {
            DiagramText textComponent = (DiagramText) component;
            loadFontProperties(textComponent);
            showFontProperties();
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
}
