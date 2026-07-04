package org.jwellman.swing.colorchooser;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/**
 * A reusable color selection panel: two rows of colour swatches (predefined + greys),
 * a "More Colors..." fallback to JColorChooser, and an optional opacity slider.
 *
 * The swatch rows use GridLayout so they always fill the full panel width regardless
 * of the enclosing layout manager.
 *
 * Usage:
 *   SwatchColorPicker picker = new SwatchColorPicker(initialColor, color -> { ... });
 *   // with opacity:
 *   SwatchColorPicker picker = new SwatchColorPicker(initialColor, color -> { ... }, true);
 *
 * TODO: migrate to org.jwellman:swing-utils when that project is next updated.
 */
public class SwatchColorPicker extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int SWATCH_HEIGHT = 24;

    // Matches the palette in ColorPropertyPanel for visual consistency.
    private static final Color[] PREDEFINED_COLORS = {
        new Color(255,   0,   0), new Color(255, 127,   0), new Color(255, 255,   0),
        new Color(  0, 255,   0), new Color(  0, 255, 255), new Color(  0,   0, 255),
        new Color(127,   0, 255), new Color(255,   0, 255), new Color(127, 255,   0),
        new Color(139,  69,  19)
    };

    private static final Color[] GREY_SHADES = {
        new Color(  0,   0,   0), new Color( 28,  28,  28), new Color( 57,  57,  57),
        new Color( 85,  85,  85), new Color(113, 113, 113), new Color(142, 142, 142),
        new Color(170, 170, 170), new Color(198, 198, 198), new Color(227, 227, 227),
        new Color(255, 255, 255)
    };

    private Color currentColor;
    private final Consumer<Color> onColorPicked;
    private final boolean showOpacity;

    private JLabel  opacityLabel;
    private JCheckBox opacityCheck;
    private JSlider opacitySlider;

    public SwatchColorPicker(Color initial, Consumer<Color> onColorPicked) {
        this(initial, onColorPicked, false);
    }

    public SwatchColorPicker(Color initial, Consumer<Color> onColorPicked, boolean showOpacity) {
        this.currentColor  = (initial != null) ? initial : Color.WHITE;
        this.onColorPicked = onColorPicked;
        this.showOpacity   = showOpacity;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        buildUI();
    }

    private void buildUI() {
        add(buildSwatchRow(PREDEFINED_COLORS));
        add(Box.createVerticalStrut(2));
        add(buildSwatchRow(GREY_SHADES));
        add(Box.createVerticalStrut(4));

        JButton moreBtn = new JButton("More Colors...");
        moreBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        moreBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, moreBtn.getPreferredSize().height));
        moreBtn.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(
                    SwingUtilities.getWindowAncestor(SwatchColorPicker.this),
                    "Choose Color", currentColor);
            if (chosen != null) {
                applyColor(chosen);
            }
        });
        add(moreBtn);

        if (showOpacity) {
            add(Box.createVerticalStrut(4));
            add(buildOpacityRow());
        }
    }

    // GridLayout distributes cells evenly across the full row width.
    private JPanel buildSwatchRow(Color[] colors) {
        JPanel row = new JPanel(new GridLayout(1, colors.length, 2, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, SWATCH_HEIGHT + 2));
        row.setPreferredSize(new Dimension(240, SWATCH_HEIGHT + 2));
        for (Color c : colors) {
            row.add(makeSwatch(c));
        }
        return row;
    }

    private JPanel makeSwatch(Color color) {
        JPanel swatch = new JPanel() {
            @Override
            public void paintComponent(Graphics g) {
                g.setColor(color);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.DARK_GRAY);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        swatch.setToolTipText(encodeHex(color));
        swatch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        swatch.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                applyColor(color);
            }
        });
        return swatch;
    }

    private JPanel buildOpacityRow() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        opacityLabel  = new JLabel("Opacity: 100%");
        opacityCheck  = new JCheckBox();
        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.setEnabled(false);

        opacityCheck.addActionListener(e -> {
            opacitySlider.setEnabled(opacityCheck.isSelected());
            if (!opacityCheck.isSelected()) {
                applyColor(opaqueOf(currentColor));
            }
        });

        opacitySlider.addChangeListener(e -> {
            opacityLabel.setText("Opacity: " + opacitySlider.getValue() + "%");
            if (opacityCheck.isSelected()) {
                currentColor = withAlpha(opaqueOf(currentColor), opacitySlider.getValue());
                onColorPicked.accept(currentColor);
            }
        });

        row.add(opacityLabel);
        row.add(Box.createHorizontalStrut(4));
        row.add(opacityCheck);
        row.add(Box.createHorizontalStrut(4));
        row.add(opacitySlider);
        return row;
    }

    private void applyColor(Color color) {
        Color base = opaqueOf(color);
        if (showOpacity && opacityCheck != null && opacityCheck.isSelected()) {
            currentColor = withAlpha(base, opacitySlider.getValue());
        } else {
            currentColor = base;
        }
        onColorPicked.accept(currentColor);
    }

    private static Color opaqueOf(Color c) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue());
    }

    private static Color withAlpha(Color c, int pct) {
        int alpha = (int) Math.round(pct / 100.0 * 255);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private static String encodeHex(Color c) {
        return String.format("#%06X", c.getRGB() & 0xFFFFFF);
    }
}
