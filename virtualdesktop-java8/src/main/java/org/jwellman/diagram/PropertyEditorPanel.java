package org.jwellman.diagram;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Property editor panel for editing component properties
 */
public class PropertyEditorPanel extends JPanel {

    private Component selectedComponent;
    private DiagramLayeredPane diagramPane;

    // Font property control
    private FontPickerPanel fontPicker;

    // Color property panel
    private ColorPropertyPanel colorPropertyPanel;

    // Corner radius controls
    private JSlider cornerRadiusSlider;
    private JLabel cornerRadiusLabel;
    private JPanel cornerRadiusPanel;

    private static final long serialVersionUID = 1L;

    public PropertyEditorPanel(DiagramLayeredPane diagramPane) {
        this.diagramPane = diagramPane;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Properties"));

        // Create property panels
        createFontPropertiesPanel();
        createColorPropertyPanel();
        createCornerRadiusPanel();

        // Initially show "No selection" message
        showNoSelectionMessage();
    }

    private void createFontPropertiesPanel() {
        fontPicker = new FontPickerPanel("Segoe UI", 16, java.awt.Font.BOLD, font -> {
            if (selectedComponent instanceof DiagramText) {
                DiagramText textComponent = (DiagramText) selectedComponent;
                textComponent.setFontName(font.getName());
                textComponent.setFontSize(font.getSize());
                textComponent.setFontStyle(font.getStyle());
                diagramPane.notifyModified();
            }
        });
    }

    private void createColorPropertyPanel() {
        colorPropertyPanel = new ColorPropertyPanel();
        colorPropertyPanel.setModificationListener(v -> diagramPane.notifyModified());
    }

    private void createCornerRadiusPanel() {
        cornerRadiusPanel = new JPanel(new BorderLayout(5, 5));
        cornerRadiusLabel = new JLabel("Corners: 0");
        cornerRadiusPanel.add(cornerRadiusLabel, BorderLayout.WEST);

        cornerRadiusSlider = new JSlider(0, 40, 0);
        cornerRadiusSlider.addChangeListener(e -> {
            cornerRadiusLabel.setText("Corners: " + cornerRadiusSlider.getValue());
            if (!cornerRadiusSlider.getValueIsAdjusting()) {
                updateCornerRadius();
            }
        });
        cornerRadiusPanel.add(cornerRadiusSlider, BorderLayout.CENTER);
    }

    private void loadCornerRadius() {
        if (!(selectedComponent instanceof DiagramRoundable)) {
            return;
        }
        javax.swing.event.ChangeListener[] listeners = cornerRadiusSlider.getChangeListeners();
        for (javax.swing.event.ChangeListener l : listeners) {
            cornerRadiusSlider.removeChangeListener(l);
        }
        int radius = ((DiagramRoundable) selectedComponent).getCornerRadius();
        cornerRadiusSlider.setValue(radius);
        cornerRadiusLabel.setText("Corners: " + radius);
        for (javax.swing.event.ChangeListener l : listeners) {
            cornerRadiusSlider.addChangeListener(l);
        }
    }

    private void updateCornerRadius() {
        if (selectedComponent instanceof DiagramRoundable) {
            ((DiagramRoundable) selectedComponent).setCornerRadius(cornerRadiusSlider.getValue());
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
        add(fontPicker, BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    private void showColorProperties() {
        removeAll();
        JPanel combinedPanel = new JPanel();
        combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.Y_AXIS));
        combinedPanel.add(colorPropertyPanel);
        if (selectedComponent instanceof DiagramRoundable) {
            combinedPanel.add(Box.createVerticalStrut(5));
            combinedPanel.add(cornerRadiusPanel);
        }
        add(combinedPanel, BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    private void showFontAndColorProperties() {
        removeAll();
        JPanel combinedPanel = new JPanel();
        combinedPanel.setLayout(new BoxLayout(combinedPanel, BoxLayout.Y_AXIS));
        combinedPanel.add(fontPicker);
        combinedPanel.add(Box.createVerticalStrut(10));
        combinedPanel.add(colorPropertyPanel);
        if (selectedComponent instanceof DiagramRoundable) {
            combinedPanel.add(Box.createVerticalStrut(5));
            combinedPanel.add(cornerRadiusPanel);
        }
        add(combinedPanel, BorderLayout.NORTH);
        revalidate();
        repaint();
    }

    /**
     * Displays a domain-provided property editor for a selected graph node.
     * The framework never constructs this panel; it is entirely domain-owned.
     * No domain imports are required here — the panel is treated as an opaque JPanel.
     */
    public void showNodeEditor(JPanel domainEditorPanel) {
        removeAll();
        add(domainEditorPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void setSelectedComponent(Component component) {
        this.selectedComponent = component;

        // Update the color property panel with the selected component
        colorPropertyPanel.setSelectedComponent(component);

        if (component == null) {
            showNoSelectionMessage();
        } else if (component instanceof DiagramText) {
            DiagramText textComponent = (DiagramText) component;
            fontPicker.setDisplayedFont(textComponent.getFontName(), textComponent.getFontSize(),
                textComponent.getFontStyle());
            loadCornerRadius();
            showFontAndColorProperties();
        } else if (component instanceof DiagramColorable) {
            loadCornerRadius();
            showColorProperties();
        } else {
            showNoSelectionMessage();
        }
    }

}
