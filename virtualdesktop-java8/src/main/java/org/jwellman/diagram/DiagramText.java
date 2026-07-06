package org.jwellman.diagram;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

/**
 * Text component for diagram (now draggable and resizable)
 */
public class DiagramText extends JPanel implements DiagramColorable, DiagramTextAware, DiagramRoundable {

    private Color textColor;
    private Color fillColor;
    private Color borderColor;
    private int cornerRadius = 5;

    private int fontSize;
    private int fontStyle;
    private String fontName;

    private JTextField textField;

    private static final long serialVersionUID = 1L;

    private static final Font   DEFAULT_FONT         = new Font("Segoe UI", Font.BOLD, 16);
    private static final Border DEFAULT_BORDER       = BorderFactory.createEmptyBorder(5, 5, 5, 5);
    private static final Color  DEFAULT_FILL_COLOR   = new Color(255, 255, 255, 200);
    private static final Color  DEFAULT_BORDER_COLOR = new Color(70, 130, 180);
    private static final Color  SELECTION_TINT       = new Color(100, 150, 255, 30);

    public DiagramText(String initialText) {
        setLayout(new BorderLayout());

        // Initialize colors
        this.fillColor = DEFAULT_FILL_COLOR;
        this.borderColor = DEFAULT_BORDER_COLOR;
        this.textColor = Color.BLACK;

        // Initialize font properties
        this.fontName = "Segoe UI";
        this.fontSize = 16;
        this.fontStyle = Font.BOLD;

        setOpaque(false); // Make transparent so we can paint custom background

        textField = new JTextField(initialText);
        textField.setFont(new Font(fontName, fontStyle, fontSize));
        textField.setForeground(textColor);
        textField.setBorder(null);
        // textField.setBorder(DEFAULT_BORDER);
        textField.setOpaque(false);
        textField.setHorizontalAlignment(JTextField.CENTER);

        // Prevent text field from consuming mouse events needed for dragging
        textField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Allow dragging when clicking outside the text editing area
                if (e.getClickCount() == 1) {
                    DiagramText.this.dispatchEvent(SwingUtilities.convertMouseEvent(textField, e, DiagramText.this));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                DiagramText.this.dispatchEvent(SwingUtilities.convertMouseEvent(textField, e, DiagramText.this));
            }
        });

        textField.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                DiagramText.this.dispatchEvent(SwingUtilities.convertMouseEvent(textField, e, DiagramText.this));
            }
        });

        add(textField, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Draw fill color
        g2d.setColor(fillColor);
        g2d.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        // Draw border
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);

        // Draw selection overlay when selected
        if (getBorder() instanceof ResizeBorder) {
            g2d.setColor(SELECTION_TINT);
            g2d.fillRect(0, 0, width, height);
        }
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    @Override
    public Color getFillColor() {
        return fillColor;
    }

    @Override
    public void setFillColor(Color color) {
        this.fillColor = color;
        repaint();
    }

    @Override
    public Color getBorderColor() {
        return borderColor;
    }

    @Override
    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    @Override
    public String getFontName() {
        return fontName;
    }

    @Override
    public void setFontName(String fontName) {
        this.fontName = fontName;
        updateTextFieldFont();
    }

    @Override
    public int getFontSize() {
        return fontSize;
    }

    @Override
    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        updateTextFieldFont();
    }

    @Override
    public int getFontStyle() {
        return fontStyle;
    }

    @Override
    public void setFontStyle(int fontStyle) {
        this.fontStyle = fontStyle;
        updateTextFieldFont();
    }

    @Override
    public Color getTextColor() {
        return textColor;
    }

    @Override
    public void setTextColor(Color color) {
        this.textColor = color;
        textField.setForeground(color);
        // repaint(); AI added this but it is probably not necessary; keeping via comment
    }

    @Override
    public int getCornerRadius() {
        return cornerRadius;
    }

    @Override
    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    private void updateTextFieldFont() {
        // only change font if all fields are valid
        if (fontName != null && fontStyle >= 0 && fontSize != 0) {
            textField.setFont(new Font(fontName, fontStyle, fontSize));
            revalidate();
            repaint();
        }
    }
}
