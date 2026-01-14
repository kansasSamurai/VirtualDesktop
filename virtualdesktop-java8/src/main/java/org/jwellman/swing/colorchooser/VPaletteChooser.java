package org.jwellman.swing.colorchooser;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jwellman.jfreechart.editor.Shade;
import org.jwellman.swing.icon.ColorIcon;
import org.jwellman.swing.jbutton.ColorSwatchToggleButton;
import org.jwellman.swing.jbutton.FlatButton;

/**
 * Note:  This class WILL be moved to swingutils.
 * 
 * This composite component is an "inline" color chooser
 * that lets the user select a color from a set of color swatches.
 * <p>
 * It offers the following features:
 * 1) Allow the user to open a conventional color chooser and
 *    choose a user chosen value not present in the swatches.
 *    This feature can be disabled.
 * <p>
 * 2) Allow the user to open a popup to choose the palette of
 *    colors in the swatches.  This popup will include pre-determined
 *    color palettes and eventually let the user build a new palette.
 *    This feature can be disabled.
 * 3) Displays the selected color name below the color swatches.
 *    This feature will be disabled by default as its use in an
 *    "inline" mode seems unlikely.  This feature may also be
 *    removed eventually since it can be easily emulated by code
 *    outside this component.  It is provided as a proof of concept.
 *    This feature can be enabled.
 *    
 * @author rwellman
 *
 */
public class VPaletteChooser extends JPanel {

    public JPanel pnlButtons = new JPanel();
    public JLabel selectedLabel = new JLabel("Selected: None");
    public FlatButton btnChooserOpener = new FlatButton("...");
    public FlatButton btnPaletteOpener = new FlatButton();
    public ColorSwatchToggleButton btnUserDefinedSwatch;
    public Shade[] selectedPalette;

    private int buttonMode;
    private Color selectedColor;
    private boolean nameLabelEnabled = false;
    private boolean paletteOpenerEnabled = false;
    private boolean chooserOpenerEnabled = true;
    private ButtonGroup group = new ButtonGroup();

    private static final long serialVersionUID = 1L;

    public VPaletteChooser() {
        this(1);
    }

    public VPaletteChooser(int mode) {
        this.setLayout(new BorderLayout());
        this.buttonMode = mode;

        pnlButtons.setLayout(new FlowLayout(FlowLayout.RIGHT));
        pnlButtons.setBackground(Color.black);

            // Create some color swatch toggle buttons
            @SuppressWarnings("unused")
            Color[] colors = { Color.RED, Color.ORANGE, Color.YELLOW,
                    Color.GREEN, Color.BLUE, 
                    // new Color(0x4B0082), // Indigo 
                    new Color(128, 0, 128), // Violet
                    Color.MAGENTA
                    // Color.CYAN,
                     };

            // These are my jfreechart palette:
            Color[] paint = new Color[] {
                    Color.decode("#CD4E3C"), // red
                    Color.decode("#579340"), // green
                    Color.decode("#5582A6"), // blue
                    Color.decode("#CA4FCC"), // magenta
                    Color.decode("#7D6EC7"), // purple
                    Color.decode("#A87829")  // brown
                };

            Shade[] shades = new Shade[] {
                    Shade.decode("#CD4E3C", "red"), // red
                    Shade.decode("#579340", "green"), // green
                    Shade.decode("#5582A6", "blue"), // blue
                    Shade.decode("#CA4FCC", "magenta"), // magenta
                    Shade.decode("#7D6EC7", "purple"), // purple
                    Shade.decode("#A87829", "brown")  // brown
                };
            selectedPalette = shades;

            for (Shade color : selectedPalette) {
                ColorSwatchToggleButton button = new ColorSwatchToggleButton(color, 21, mode);

                // Add action listener to show selected color
                button.addActionListener(e -> {
                    this.setSelectedColor(button.getColor());
                    if (this.isNameLabelEnabled()) {
                      selectedLabel.setText("Selected: " + color.getName());
                    }
                });

                group.add(button);
                pnlButtons.add(button);
            }
            btnUserDefinedSwatch = new ColorSwatchToggleButton(Color.white);
            if (this.isChooserOpenerEnabled()) {
                group.add(btnUserDefinedSwatch);
                pnlButtons.add(btnUserDefinedSwatch);
            }

            // TODO get a better icon for this
            btnPaletteOpener.setIcon(new ColorIcon(Color.DARK_GRAY, 15));
            if (this.isPaletteOpenerEnabled()) {
                pnlButtons.add(btnPaletteOpener);
            }

            // TODO I like the text only decoration but maybe provide 
            // a decent icon option if the user does not have an icon handy.
            // b.setSize(81, 81);
            // b.setIcon(new ColorIcon(Color.LIGHT_GRAY, 15));
            btnChooserOpener.addActionListener(e -> {this.openUserColorChooser();});
            if (this.isChooserOpenerEnabled()) {
                pnlButtons.add(btnChooserOpener);
            }

            this.add(pnlButtons, BorderLayout.NORTH);

            if (this.isNameLabelEnabled()) {
                this.add(selectedLabel, BorderLayout.SOUTH);
            }


    }

    public void openUserColorChooser() {
        Color chosenColor = JColorChooser.showDialog(
                null, // Parent component (can be null for no parent)
                "Choose a User Defined Color ...", // Dialog title
                btnUserDefinedSwatch.getColor() // Initial color
        );
        if (chosenColor != null) {
            selectedColor = chosenColor;
            btnUserDefinedSwatch.setColor(chosenColor);
        }
    }

    private class ColorStateChange implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
        }
    }

    public void setSelectedColor(Color c) {
        this.selectedColor = c;
        // TODO update the corresponding color swatch/button group
    }

    public Color getSelectedColor() {
        return this.selectedColor;
    }

    public boolean isNameLabelEnabled() {
        return nameLabelEnabled;
    }

    public void setNameLabelEnabled(boolean nameLabelEnabled) {
        this.nameLabelEnabled = nameLabelEnabled;
    }

    public boolean isPaletteOpenerEnabled() {
        return paletteOpenerEnabled;
    }

    public void setPaletteOpenerEnabled(boolean paletteOpenerEnabled) {
        this.paletteOpenerEnabled = paletteOpenerEnabled;
    }

    public boolean isChooserOpenerEnabled() {
        return chooserOpenerEnabled;
    }

    public void setChooserOpenerEnabled(boolean chooserOpenerEnabled) {
        this.chooserOpenerEnabled = chooserOpenerEnabled;
    }

    public int getButtonMode() {
        return buttonMode;
    }

    public void setButtonMode(int buttonMode) {
        this.buttonMode = buttonMode;
    }

}
