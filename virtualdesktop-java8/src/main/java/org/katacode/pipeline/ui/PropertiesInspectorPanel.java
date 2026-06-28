package org.katacode.pipeline.ui;

import org.katacode.pipeline.engine.PipelineStep;
import org.katacode.pipeline.engine.meta.PropertyDescriptor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/**
 * High-Craft Dynamic Properties Form Engine.
 * Introspects component descriptors to dynamically construct clean configuration fields.
 */
@SuppressWarnings("serial")
public class PropertiesInspectorPanel extends JPanel {

    private final JPanel formContainer;
    private final JLabel lblPlaceholder;
    private PipelineStep currentStep;

    public PropertiesInspectorPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(0xF5, 0xF7, 0xFA));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0xDC, 0xE1, 0xE6)),
            new EmptyBorder(16, 20, 16, 20)
        ));

        // Staggered top-aligned form block
        formContainer = new JPanel(new GridBagLayout());
        formContainer.setOpaque(false);

        lblPlaceholder = new JLabel("Select a component step to view properties.", SwingConstants.CENTER);
        lblPlaceholder.setFont(new Font("SansSerif", Font.ITALIC, 12));
        lblPlaceholder.setForeground(new Color(0xA0, 0xAE, 0xC0));
        
        add(lblPlaceholder, BorderLayout.CENTER);
    }

    /**
     * Tears down the old form inputs and re-hydrates the panel with 
     * target layout components mapped to the step definition.
     */
    public void inspect(PipelineStep step) {
        this.currentStep = step;
        
        // 1. Clear out active view components
        remove(lblPlaceholder);
        formContainer.removeAll();

        if (step == null) {
            add(lblPlaceholder, BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        add(formContainer, BorderLayout.NORTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 12, 0); // Spacing gap below elements
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Render Fixed Header Meta Info
        renderHeaderBlock(step, gbc);

        // 2. Loop over metadata fields and inject dynamic form fields
        List<PropertyDescriptor> descriptors = step.getPropertyDescriptors();
        for (PropertyDescriptor desc : descriptors) {
            
            // Render Label
            JLabel fieldLabel = new JLabel(desc.getDisplayName());
            fieldLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
            fieldLabel.setForeground(new Color(0x4A, 0x55, 0x68));
            gbc.weightx = 1.0;
            formContainer.add(fieldLabel, gbc);
            gbc.gridy++;

            // Render Field Type Control Engine
            String currentVal = step.getPropertyValues().getOrDefault(desc.getKey(), "");
            JComponent inputControl = buildInputComponent(desc, currentVal);
            
            formContainer.add(inputControl, gbc);
            gbc.gridy++;
        }

        // 3. Force frame layout pass update
        formContainer.revalidate();
        formContainer.repaint();
        revalidate();
        repaint();
    }

    private void renderHeaderBlock(PipelineStep step, GridBagConstraints gbc) {
        JLabel lblHeader = new JLabel("Component ID: " + step.getId());
        lblHeader.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblHeader.setForeground(new Color(0x1A, 0x20, 0x2C));
        formContainer.add(lblHeader, gbc);
        gbc.gridy++;

        JLabel lblType = new JLabel("Strategy: " + step.getComponentName());
        lblType.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblType.setForeground(new Color(0x71, 0x80, 0x96));
        formContainer.add(lblType, gbc);
        gbc.gridy++;
        
        // Horizontal separation bar rule
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xE2, 0xE8, 0xF0));
        formContainer.add(sep, gbc);
        gbc.gridy++;
    }

    private JComponent buildInputComponent(PropertyDescriptor desc, String initialValue) {
        switch (desc.getType()) {
            case COMPONENT_CHOICE:
                JComboBox<String> combo = new JComboBox<>(desc.getOptions());
                combo.setSelectedItem(initialValue);
                combo.addActionListener(e -> currentStep.setPropertyValue(desc.getKey(), (String) combo.getSelectedItem()));
                return combo;

            case BOOLEAN:
                JCheckBox checkBox = new JCheckBox("Enabled");
                checkBox.setSelected(Boolean.parseBoolean(initialValue));
                checkBox.setOpaque(false);
                checkBox.addActionListener(e -> currentStep.setPropertyValue(desc.getKey(), String.valueOf(checkBox.isSelected())));
                return checkBox;

            case TEXT:
            default:
                JTextField text = new JTextField(initialValue);
                text.putClientProperty("clearButton", Boolean.TRUE); // Modern look-and-feel hint
                text.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) { update(); }
                    public void removeUpdate(DocumentEvent e) { update(); }
                    public void changedUpdate(DocumentEvent e) { update(); }
                    private void update() {
                        currentStep.setPropertyValue(desc.getKey(), text.getText());
                    }
                });
                return text;
        }
    }

}
