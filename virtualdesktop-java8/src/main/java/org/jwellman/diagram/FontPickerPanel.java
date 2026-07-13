package org.jwellman.diagram;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

/**
 * Reusable font picker: family combo box, size slider, and Plain/Bold/Italic
 * style toggle buttons — the same controls originally built inline for editing
 * {@link DiagramText}'s font, pulled out so any property editor (this tool's own
 * {@link PropertyEditorPanel} or a domain factory's) can offer the same
 * font-choice UI without duplicating it.
 *
 * Fires a single {@link Font}-valued callback on any change; callers destructure
 * via {@code getName()} (the family string as passed to the Font constructor),
 * {@code getSize()}, and {@code getStyle()}.
 */
public class FontPickerPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int MIN_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private final JComboBox<String> fontNameCombo;
    private final JSlider fontSizeSlider;
    private final JLabel fontSizeLabel;
    private final JToggleButton plainButton;
    private final JToggleButton boldButton;
    private final JToggleButton italicButton;

    // Nulled out during setDisplayedFont() so programmatic updates don't fire onChanged.
    private Consumer<Font> onChanged;

    public FontPickerPanel(String initialFontName, int initialFontSize, int initialFontStyle,
                           Consumer<Font> onChanged) {
        this.onChanged = onChanged;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Font name
        JPanel fontNameRow = new JPanel(new BorderLayout(5, 5));
        fontNameRow.add(new JLabel("Font:"), BorderLayout.WEST);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        fontNameCombo = new JComboBox<>(ge.getAvailableFontFamilyNames());
        fontNameCombo.setSelectedItem(initialFontName);
        fontNameCombo.addActionListener(e -> fireChanged());
        fontNameRow.add(fontNameCombo, BorderLayout.CENTER);
        add(fontNameRow);
        add(Box.createVerticalStrut(5));

        // Font size
        JPanel fontSizeRow = new JPanel(new BorderLayout(5, 5));
        fontSizeLabel = new JLabel("Size: " + initialFontSize);
        fontSizeRow.add(fontSizeLabel, BorderLayout.WEST);
        fontSizeSlider = new JSlider(MIN_SIZE, MAX_SIZE, initialFontSize);
        fontSizeSlider.setMajorTickSpacing(10);
        fontSizeSlider.setMinorTickSpacing(5);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.addChangeListener(e -> {
            fontSizeLabel.setText("Size: " + fontSizeSlider.getValue());
            if (!fontSizeSlider.getValueIsAdjusting()) {
                fireChanged();
            }
        });
        fontSizeRow.add(fontSizeSlider, BorderLayout.CENTER);
        add(fontSizeRow);
        add(Box.createVerticalStrut(5));

        // Font style
        JPanel fontStyleRow = new JPanel(new BorderLayout(5, 5));
        fontStyleRow.add(new JLabel("Style:"), BorderLayout.WEST);
        JPanel styleButtonsPanel = new JPanel();
        styleButtonsPanel.setLayout(new BoxLayout(styleButtonsPanel, BoxLayout.X_AXIS));
        plainButton  = new JToggleButton("Plain");
        boldButton   = new JToggleButton("Bold");
        italicButton = new JToggleButton("Italic");

        boolean isBold   = (initialFontStyle & Font.BOLD) != 0;
        boolean isItalic = (initialFontStyle & Font.ITALIC) != 0;
        plainButton.setSelected(!isBold);
        boldButton.setSelected(isBold);
        italicButton.setSelected(isItalic);

        // Plain/Bold are mutually exclusive (clicking one deselects the other); Italic
        // is independent. If both Bold and Italic end up off, Plain auto-reselects so
        // it always reflects "no style" even if the user cleared it via another path.
        plainButton.addActionListener(e -> {
            if (plainButton.isSelected()) {
                boldButton.setSelected(false);
            }
            syncPlainButton();
            fireChanged();
        });
        boldButton.addActionListener(e -> {
            if (boldButton.isSelected()) {
                plainButton.setSelected(false);
            }
            syncPlainButton();
            fireChanged();
        });
        italicButton.addActionListener(e -> {
            syncPlainButton();
            fireChanged();
        });

        styleButtonsPanel.add(plainButton);
        styleButtonsPanel.add(Box.createHorizontalStrut(3));
        styleButtonsPanel.add(boldButton);
        styleButtonsPanel.add(Box.createHorizontalStrut(3));
        styleButtonsPanel.add(italicButton);
        fontStyleRow.add(styleButtonsPanel, BorderLayout.CENTER);
        add(fontStyleRow);
    }

    /** Updates the displayed controls to reflect the given font, without firing onChanged. */
    public void setDisplayedFont(String fontName, int fontSize, int fontStyle) {
        Consumer<Font> saved = onChanged;
        onChanged = null;
        try {
            fontNameCombo.setSelectedItem(fontName);
            fontSizeSlider.setValue(fontSize);
            fontSizeLabel.setText("Size: " + fontSize);
            boolean isBold   = (fontStyle & Font.BOLD) != 0;
            boolean isItalic = (fontStyle & Font.ITALIC) != 0;
            plainButton.setSelected(!isBold);
            boldButton.setSelected(isBold);
            italicButton.setSelected(isItalic);
        } finally {
            onChanged = saved;
        }
    }

    /** Returns the currently selected font — family name, size, and style all reflect the live controls. */
    public Font getSelectedFont() {
        return new Font((String) fontNameCombo.getSelectedItem(), currentStyle(), fontSizeSlider.getValue());
    }

    private int currentStyle() {
        int style = Font.PLAIN;
        if (boldButton.isSelected()) {
            style = Font.BOLD;
        }
        if (italicButton.isSelected()) {
            style |= Font.ITALIC;
        }
        return style;
    }

    private void syncPlainButton() {
        if (!boldButton.isSelected() && !italicButton.isSelected() && !plainButton.isSelected()) {
            plainButton.setSelected(true);
        }
    }

    private void fireChanged() {
        if (onChanged != null) {
            onChanged.accept(getSelectedFont());
        }
    }
}
