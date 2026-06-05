package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;

public class ContinuousLoopDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Continuous Multi-Wave Tween Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(5, 15, 30));
            
            // 3 Rows, 2 Columns
            frame.setLayout(new GridLayout(3, 2, 15, 15));

            long speed = 2000; // Each phase cycle takes exactly 2 seconds

            // --- ROW 1: THE ACCELERATION PROFILE COMPARISON (TRIANGLE WAVE) ---
            // Left Column: Linear Triangle (Moves up and down at a hard fixed rate)
            VRadialIndicator leftTri = createCell(frame, "Linear Triangle");
            Engine.getInstance().register(speed, Engine.Easing.LINEAR, Engine.LoopMode.TRIANGLE, c -> {
                if (!leftTri.isDisplayable()) return false;
                leftTri.setPercentage(c.value * 100.0);
                return true;
            });

            // Right Column: Sine Triangle (Smoothed cushioning at the top and bottom)
            VRadialIndicator rightTri = createCell(frame, "Sine Triangle");
            Engine.getInstance().register(speed, Engine.Easing.SINE_WAVE, Engine.LoopMode.TRIANGLE, c -> {
                if (!rightTri.isDisplayable()) return false;
                rightTri.setPercentage(c.value * 100.0);
                return true;
            });

            // --- ROW 2: THE INSTANT-RESET PATTERNS (SAWTOOTH WAVE) ---
            // Left Column: Linear Sawtooth (Fills uniformly, clips instantly back to 0)
            VRadialIndicator leftSaw = createCell(frame, "Linear Sawtooth");
            Engine.getInstance().register(speed, Engine.Easing.LINEAR, Engine.LoopMode.SAWTOOTH, c -> {
                if (!leftSaw.isDisplayable()) return false;
                leftSaw.setPercentage(c.value * 100.0);
                return true;
            });

            // Right Column: Sine Eased Sawtooth (Swell speeds up, then resets)
            VRadialIndicator rightSaw = createCell(frame, "Sine Sawtooth");
            Engine.getInstance().register(speed, Engine.Easing.SINE_WAVE, Engine.LoopMode.SAWTOOTH, c -> {
                if (!rightSaw.isDisplayable()) return false;
                rightSaw.setPercentage(c.value * 100.0);
                return true;
            });

            frame.setSize(600, 800);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // Quick helper to package components cleanly into the frame
    private static VRadialIndicator createCell(JFrame frame, String title) {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        
        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setForeground(new Color(0, 180, 255));
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        VRadialIndicator indicator = new VRadialIndicator();
        indicator.setPreferredSize(new Dimension(120, 120));
        
        container.add(indicator, BorderLayout.CENTER);
        container.add(label, BorderLayout.SOUTH);
        
        frame.add(container);
        return indicator;
    }

}
