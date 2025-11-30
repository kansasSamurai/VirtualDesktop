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
    private JComboBox<String> fontStyleCombo;
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

        // Font Style
        JPanel fontStylePanel = new JPanel(new BorderLayout(5, 5));
        fontStylePanel.add(new JLabel("Style:"), BorderLayout.WEST);

        String[] styles = {"Plain", "Bold", "Italic", "Bold+Italic"};
        fontStyleCombo = new JComboBox<>(styles);
        fontStyleCombo.addActionListener(e -> updateFontStyle());
        fontStylePanel.add(fontStyleCombo, BorderLayout.CENTER);
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
        java.awt.event.ActionListener[] fontStyleListeners = fontStyleCombo.getActionListeners();

        for (java.awt.event.ActionListener listener : fontNameListeners) {
            fontNameCombo.removeActionListener(listener);
        }
        for (javax.swing.event.ChangeListener listener : fontSizeListeners) {
            fontSizeSlider.removeChangeListener(listener);
        }
        for (java.awt.event.ActionListener listener : fontStyleListeners) {
            fontStyleCombo.removeActionListener(listener);
        }

        // Load values
        fontNameCombo.setSelectedItem(textComponent.getFontName());

        int fontSize = textComponent.getFontSize();
        fontSizeSlider.setValue(fontSize);
        fontSizeLabel.setText("Size: " + fontSize);

        int style = textComponent.getFontStyle();
        if (style == Font.PLAIN) {
            fontStyleCombo.setSelectedIndex(0);
        } else if (style == Font.BOLD) {
            fontStyleCombo.setSelectedIndex(1);
        } else if (style == Font.ITALIC) {
            fontStyleCombo.setSelectedIndex(2);
        } else if (style == (Font.BOLD | Font.ITALIC)) {
            fontStyleCombo.setSelectedIndex(3);
        }

        // Re-add listeners
        for (java.awt.event.ActionListener listener : fontNameListeners) {
            fontNameCombo.addActionListener(listener);
        }
        for (javax.swing.event.ChangeListener listener : fontSizeListeners) {
            fontSizeSlider.addChangeListener(listener);
        }
        for (java.awt.event.ActionListener listener : fontStyleListeners) {
            fontStyleCombo.addActionListener(listener);
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
            int selectedIndex = fontStyleCombo.getSelectedIndex();
            int style;

            switch (selectedIndex) {
                case 0: style = Font.PLAIN; break;
                case 1: style = Font.BOLD; break;
                case 2: style = Font.ITALIC; break;
                case 3: style = Font.BOLD | Font.ITALIC; break;
                default: style = Font.PLAIN;
            }

            textComponent.setFontStyle(style);
            diagramPane.notifyModified();
        }
    }
}
