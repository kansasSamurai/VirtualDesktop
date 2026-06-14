package org.jwellman.demo.chess;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JDialog;

@SuppressWarnings("serial")
public class PromotionChoiceDialog extends JDialog {

    private ChessPiece.Type selectedType = ChessPiece.Type.QUEEN; // Default safety fall-through

    public PromotionChoiceDialog(Frame parent, boolean isWhite, Consumer<ChessPiece.Type> callback) {
        super(parent, "Promote Pawn", true);
        this.setUndecorated(true); // Removes standard OS window borders for a custom feel
        this.setLayout(new FlowLayout());
        this.setBackground(new Color(0, 0, 0, 220)); // Match your dark theme accent

        // Populate options based on traditional choices
        ChessPiece.Type[] options = { ChessPiece.Type.QUEEN, ChessPiece.Type.ROOK, ChessPiece.Type.BISHOP, ChessPiece.Type.KNIGHT };

        for (ChessPiece.Type type : options) {
            // In your actual app, swap these text buttons for your stylized token graphics!
            JButton button = new JButton(type.name().substring(0, 1));
            button.setFont(new Font("SansSerif", Font.BOLD, 18));
            button.setPreferredSize(new Dimension(60, 60));
            
            button.addActionListener(e -> {
                selectedType = type;
                callback.accept(selectedType); // Pass choice straight to the controller
                this.dispose();
            });
            this.add(button);
        }

        this.pack();
        this.setLocationRelativeTo(parent);
    }

}
