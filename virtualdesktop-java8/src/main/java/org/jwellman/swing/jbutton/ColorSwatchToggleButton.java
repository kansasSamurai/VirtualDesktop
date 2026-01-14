package org.jwellman.swing.jbutton;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.jwellman.swing.icon.ColorIcon;

/**
 * A custom JToggleButton that displays a color swatch instead of text.
 * The button shows different visual states for selected/unselected states.
 * 
 * public methods marked as 'public /**' will enventually be private.
 * This technique allows me to use this class in beanshell.
 */
public class ColorSwatchToggleButton extends JToggleButton {

    public /**/ int swatchSize;
    public /**/ int swatchMode = 1; // 1/round, 2/square, tbd...
    public /**/ int borderWidth = 2;
    public /**/ Color color;
    public /**/ static final Stroke DASH1 = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f, new float[] {2f, 2f}, 0.0f);
    public /**/ static final Stroke STROKE1 = new BasicStroke(1);
    public /**/ static final Stroke STROKE15 = new BasicStroke(1.5f);
    public /**/ static final Stroke STROKE2 = new BasicStroke(2);

    private static final long serialVersionUID = 1L;

    // on modern displays, 11 is probably too small so 15 smallest
    public ColorSwatchToggleButton(Color color) {
        this(color, 21); // 21 will be used for my color chooser
    }

    public ColorSwatchToggleButton(Color color, int size) {
        this(color, size, 1);
    }

    /**
     * Creates a new toggle button with color, size, and "mode".
     * 
     * @param color
     * @param size
     * @param mode
     */
    public ColorSwatchToggleButton(Color color, int size, int mode) {
        this.color = color;
        this.swatchMode = mode;

        // required for metal look and feel; does not hurt other look and feels
        this.setRolloverEnabled(true);

        _setSwatchSize(size);
        initializeButton();
    }

    public /**/ void initializeButton() {
        // Remove default text and styling
        setText("");
        // turn off all these default so that we can completely control the user interface
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        // I don't like tabbing through every button so turning this off.
        // It can be turned back on by the end user if they want.
        setFocusable(false);

        // Add some padding
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final int x = borderWidth, y = borderWidth;
        final int size = swatchSize;
        int arcsize = 8;

        // Draw the color swatch
        if (getModel().isPressed()) {
            g2d.setColor(color.darker());
        } else {
            g2d.setColor(color);
        }

        if (swatchMode == 1) { // round

            // Draw border based on selection state
            if (isSelected()) {

                Shape border = new RoundRectangle2D.Double(x, y, size, size, arcsize, arcsize);

                // Draw swatch
                Shape rect = new RoundRectangle2D.Double(x+2, y+2, size-3, size-3, arcsize, arcsize);
                g2d.fill(rect);

                // Thick border for selected state
                g2d.setStroke(STROKE15);
                g2d.setColor(Color.WHITE);
                g2d.draw(border);

            } else {

                Shape border = new RoundRectangle2D.Double(x, y, size, size, arcsize, arcsize);

                g2d.fillRoundRect(x+2, y+2, size-3, size-3, arcsize, arcsize);

                if (getModel().isRollover()) {
                    // Add hover effect
                    //arcsize = arcSize(x-1);
                    g2d.setStroke(STROKE15);
                    g2d.setColor(Color.WHITE);
                    g2d.draw(border);
                } else {
                    if (this.hasFocus()) {
                        g2d.setStroke(DASH1);
                        g2d.setColor(Color.WHITE);
                        g2d.draw(border);
                    }
                    // Thin border for unselected state
//                    g2d.setStroke(STROKE1);
//                    g2d.setColor(Color.WHITE);
//                    g2d.drawRoundRect(x, y, size, size, arcsize, arcsize);
                }
            }

        } else if (swatchMode == 2) {

            // Draw border based on selection state
            if (isSelected()) {

//                g2d.fillRect(x, y, size, size);
                g2d.fillRect(x+2, y+2, size-3, size-3);

                // Thick border for selected state
                g2d.setStroke(STROKE1);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(x, y, size, size);
//                g2d.drawRoundRect(x, y, size, size, arcsize, arcsize);
//                g2d.drawRoundRect(x - borderWidth / 2, y - borderWidth / 2, size + borderWidth, size + borderWidth, arcsize, arcsize);

                // Inner highlight
//                g2d.setStroke(STROKE1);
//                g2d.setColor(Color.WHITE);
//                g2d.drawRoundRect(x + 1, y + 1, size - 2, size - 2, 6, 6);
            } else {

                g2d.fillRoundRect(x+2, y+2, size-3, size-3, arcsize, arcsize);

                if (getModel().isRollover()) {
                    // Add hover effect
                    arcsize = arcSize(x-1);
                    g2d.setStroke(STROKE1);
                    g2d.setColor(Color.WHITE);
                    g2d.drawRect(x, y, size, size);
                } else {
                    if (this.hasFocus()) {
                        g2d.setStroke(DASH1);
                        g2d.setColor(Color.WHITE);
                        g2d.drawRect(x, y, size, size);
                    }
                    // Thin border for unselected state
//                    g2d.setStroke(STROKE1);
//                    g2d.setColor(Color.WHITE);
//                    g2d.drawRoundRect(x, y, size, size, arcsize, arcsize);
                }
            }

        }

        g2d.dispose();
    }

    private int arcSize(Rectangle r) {
        return arcSize(r.height);
    }

    private int arcSize(int d) {
        return d * 3 / 4;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    public int getSwatchSize() {
        return swatchSize;
    }

    public /**/ void _setSwatchSize(int size) {
        this.swatchSize = size;
        Dimension buttonSize = new Dimension(size + borderWidth * 2, size + borderWidth * 2);
        setPreferredSize(buttonSize);
        setMinimumSize(buttonSize);
        setMaximumSize(buttonSize);
    }

    public void setSwatchSize(int size) {
        _setSwatchSize(size);
        repaint();
    }

    // Demo application
    public static void main(String[] args) {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel();
        buttons.setBackground(Color.black);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Color Swatch Toggle Button Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            // Create some color swatch toggle buttons
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

            ButtonGroup group = new ButtonGroup();
            JLabel selectedLabel = new JLabel("Selected: None");

            for (Color color : paint) {
                ColorSwatchToggleButton button = new ColorSwatchToggleButton(color);

                // Add action listener to show selected color
                button.addActionListener(e -> {
                    if (button.isSelected()) {
                        selectedLabel.setText("Selected: " + getColorName(color));
                    }
                });

                group.add(button);
                buttons.add(button);
            }
            FlatButton b = new FlatButton();
            // b.setPreferredSize(new Dimension(81,81));
            b.setIcon(new ColorIcon(Color.DARK_GRAY, 15));
            buttons.add(b);

            b = new FlatButton("...");
            // b.setSize(81, 81);
            // b.setIcon(new ColorIcon(Color.LIGHT_GRAY, 15));
//            btnColorChooser.setForeground(Color.LIGHT_GRAY);
//            btnColorChooser.setTextOnly(true);
            buttons.add(b);

            panel.add(buttons, BorderLayout.NORTH);
            panel.add(selectedLabel, BorderLayout.SOUTH);
            frame.add(panel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public /**/ static String getColorName(Color color) {
        if (color.equals(Color.RED)) return "Red";
        if (color.equals(Color.GREEN)) return "Green";
        if (color.equals(Color.BLUE)) return "Blue";
        if (color.equals(Color.YELLOW)) return "Yellow";
        if (color.equals(Color.MAGENTA)) return "Magenta";
        if (color.equals(Color.CYAN)) return "Cyan";
        if (color.equals(Color.ORANGE)) return "Orange";
        return "Purple";
    }

}
