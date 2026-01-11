package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;

public class LoadingLabelDemo extends JFrame {

    private int animationStep = 0;

    private static final long serialVersionUID = 1L;

    public LoadingLabelDemo() {
        setTitle("Loading Label Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout(FlowLayout.LEFT));

        // Optional: Add a stop button
        JButton stopButton = new JButton("Stop Animation");
        add(stopButton);

        JLabel loadingLabel = new JLabel("Loading");
        loadingLabel.setFont(new Font("Consolas", Font.PLAIN, 24));
        // These fonts do not have the unicode characters I want
        // Consolas, Courier New
        add(loadingLabel);

        // Array of animation frames \u25FC \u26AB
        // u2022 bullet u25aa black small square u25A0 black square
        String[] frames = { "\u25A0", "\u2022", "\u2022\u25A0", "\u2022\u2022", "\u2022\u2022\u25A0", "\u2022\u2022\u2022" };
        // String[] frames = { "\u25FC", ".", ".\u25FC", "..", "..\u25FC", "..." };
        // String[] frames = { "◼️", ".", ".◼️", "..", "..◼️", "..." };
        
        // Timer that updates every 500ms
        Timer timer = new Timer(500, e -> {
            loadingLabel.setText("Loading " + frames[animationStep]);
            animationStep = (animationStep + 1) % frames.length;
        });

        timer.start();

        stopButton.addActionListener(e -> timer.stop());

        setSize(300, 150);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoadingLabelDemo().setVisible(true);
        });
    }

}
