package org.jwellman.demo.chess;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JLayeredPane;
import javax.swing.JToggleButton;

import org.jwellman.demo.chess.ChessUiEngine.PieceFlightController;

/**
 * The Visual Token Primitive<br>
 * Inherits JToggleButton to capture focus, rollover hooks, and interaction states for free!
 *  
 * @author rwellman
 *
 */
@SuppressWarnings("serial")
public class ChessPieceToken extends JToggleButton {

    private final String glyph;
    private final Color pieceColor;

    // for now, needs to be package visibility until accessor is created 
    final ChessPiece piece;

    public ChessPieceToken(String glyph, Color pieceColor) {
        this.glyph = glyph;
        this.pieceColor = pieceColor;
        this.piece = null;

        // Strip default look-and-feel painting pipelines
        this.setContentAreaFilled(false);
        this.setBorderPainted(false);
        this.setFocusPainted(false);
        this.setRolloverEnabled(true);

        // Bind to the zero-allocation global flight singleton
        this.addMouseListener(PieceFlightController.INSTANCE);
        this.addMouseMotionListener(PieceFlightController.INSTANCE);
    }

    public ChessPieceToken(ChessPiece p, Color pieceColor) {
        this.glyph = p.getGlyph();
        this.pieceColor = pieceColor;
        this.piece = p;

        // Strip default look-and-feel painting pipelines
        this.setContentAreaFilled(false);
        this.setBorderPainted(false);
        this.setFocusPainted(false);
        this.setRolloverEnabled(true);

        // Bind to the zero-allocation global flight singleton
        this.addMouseListener(PieceFlightController.INSTANCE);
        this.addMouseMotionListener(PieceFlightController.INSTANCE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int pad = 6;
            int size = getWidth() - (pad * 2);

            // Check z-stack positioning via the LayeredPane hierarchy state
            JLayeredPane lp = (JLayeredPane) getParent();
            boolean isFlying = (lp != null && lp.getLayer((Component)this) == JLayeredPane.DRAG_LAYER);

            // Render dynamic vector dropshadow if flying in upper atmosphere!
            if (isFlying) {
                g2.setColor(new Color(0, 0, 0, 60));
                g2.fillOval(pad + 4, pad + 6, size, size);
            }

            // Smoothly color transition based on hover/selection interaction states
            if (getModel().isRollover()) {
                g2.setColor(new Color(240, 240, 240, 40));
                g2.fillOval(pad - 2, pad - 2, size + 4, size + 4);
            }

            // Render Base Body
            g2.setColor(pieceColor == Color.WHITE ? new Color(245, 245, 240) : new Color(50, 50, 50));
            g2.fillOval(pad, pad, size, size);

            // Render Rim Trim
            g2.setStroke(new BasicStroke(2.0f));
            g2.setColor(pieceColor == Color.WHITE ? new Color(160, 160, 150) : new Color(20, 20, 20));
            g2.drawOval(pad, pad, size, size);

            // Draw Text Glyph Character Symbol centered inside token
            g2.setFont(new Font("SansSerif", Font.BOLD, getWidth() / 3));
            g2.setColor(pieceColor == Color.WHITE ? Color.BLACK : Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(glyph)) / 2;
            int ty = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(glyph, tx, ty);

        } finally {
            g2.dispose();
        }
    }

}
