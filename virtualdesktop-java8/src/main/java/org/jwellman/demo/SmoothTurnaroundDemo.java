package org.jwellman.demo;

import javax.swing.*;

import org.jwellman.demo.animation.Engine;
import org.jwellman.demo.gauge.VRadialIndicator;

import java.awt.*;

public class SmoothTurnaroundDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Smooth Turnaround Verification");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(5, 15, 30));
            frame.setLayout(new GridLayout(1, 2, 20, 20));

            long cycleDuration = 6283; // 3 seconds per complete loop round-trip

            // --- Left Column: The Old "Sine Triangle" ---
            JPanel leftPanel = createCellPanel("Sine Triangle (Abrupt Peak/Trough)");
            VRadialIndicator leftIndicator = new VRadialIndicator();
            leftPanel.add(leftIndicator, BorderLayout.CENTER);
            frame.add(leftPanel);

            Engine.getInstance().register(
                    cycleDuration / 2, 
                    Engine.Easing.SINE_WAVE, 
                    Engine.LoopMode.TRIANGLE, 
                    c -> {
                        if (!leftIndicator.isDisplayable())
                            return false;
                        leftIndicator.setPercentage(c.value * 100.0);
                        return true;
                    });

            // --- Right Column: The New True "Cosine Loop" ---
            JPanel rightPanel = createCellPanel("Sine Loop (True Smooth Fluidity)");
            VRadialIndicator rightIndicator = new VRadialIndicator();
            rightPanel.add(rightIndicator, BorderLayout.CENTER);
            frame.add(rightPanel);

            Engine.getInstance().register(
                    cycleDuration,  // Complete a full cycle every 2 seconds
                    Engine.Easing.LINEAR,  // Uniform velocity progression
                    Engine.LoopMode.SINE,// Continuous smooth harmonic timeline
                    context -> {
                        rightIndicator.setPercentage(context.value * 100.0);
                        return true;
                    });
            
            frame.setSize(600, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static JPanel createCellPanel(String description) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel(description, SwingConstants.CENTER);
        label.setForeground(new Color(0, 180, 255));
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(label, BorderLayout.SOUTH);
        return panel;
    }

}
