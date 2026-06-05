package org.jwellman.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class PointToPointDemo {

    // Simple tracker to bridge the starting point, target point, and current visual position
    private static class IndicatorState {
        double startValue = 0.0;
        double targetValue = 0.0;
        double currentValue = 0.0;
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Point-to-Point Easing Comparison");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(5, 15, 30));
            
            // 1 row, 2 columns for our side-by-side comparison panels
            frame.setLayout(new GridLayout(1, 2, 20, 20));

            long animationDuration = 1500; // 1.5 seconds target runtime

            // --- LEFT COLUMN: LINEAR TWEEN ---
            IndicatorState leftState = new IndicatorState();
            VRadialIndicator leftIndicator = new VRadialIndicator();
            JPanel leftPanel = createControlCell("LINEAR TRANSITION", leftIndicator, leftState, (newValue) -> {
                // Set the current position as the base for the new flight
                leftState.startValue = leftState.currentValue;
                leftState.targetValue = newValue;

                Engine.getInstance().register(animationDuration, Engine.Easing.LINEAR, Engine.LoopMode.ONCE, c -> {
                    if (!leftIndicator.isDisplayable()) return false;

                    // Linear interpolation (Lerp) using raw progression
                    double delta = leftState.targetValue - leftState.startValue;
                    leftState.currentValue = leftState.startValue + (delta * c.value);
                    
                    leftIndicator.setPercentage(leftState.currentValue);
                    return true;
                });
            });
            frame.add(leftPanel);

            // --- RIGHT COLUMN: SINE TRANSITION ---
            IndicatorState rightState = new IndicatorState();
            VRadialIndicator rightIndicator = new VRadialIndicator();
            JPanel rightPanel = createControlCell("SINE WAVE TRANSITION", rightIndicator, rightState, (newValue) -> {
                // Set the current position as the base for the new flight
                rightState.startValue = rightState.currentValue;
                rightState.targetValue = newValue;

                Engine.getInstance().register(animationDuration, Engine.Easing.SINE_WAVE, Engine.LoopMode.ONCE, c -> {
                    if (!rightIndicator.isDisplayable()) return false;

                    // Linear interpolation (Lerp) using eased sine progression
                    double delta = rightState.targetValue - rightState.startValue;
                    rightState.currentValue = rightState.startValue + (delta * c.value);
                    
                    rightIndicator.setPercentage(rightState.currentValue);
                    return true;
                });
            });
            frame.add(rightPanel);

            // --- INITIAL STATE SETUP ---
            // Animate both indicators from 0 to 75% on application initialization
            leftPanel.getClientProperty("trigger").hashCode(); // warm up reference hook
            ((java.util.function.Consumer<Double>) leftPanel.getClientProperty("trigger")).accept(75.0);
            ((java.util.function.Consumer<Double>) rightPanel.getClientProperty("trigger")).accept(75.0);

            frame.setSize(700, 500);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Builds a unified visual cell layout enclosing the indicator, text fields, and action hooks.
     */
    private static JPanel createControlCell(String title, VRadialIndicator indicator, IndicatorState state, java.util.function.Consumer<Double> onUpdateTrigger) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top Header
        JLabel headerLabel = new JLabel(title, SwingConstants.CENTER);
        headerLabel.setForeground(new Color(0, 180, 255));
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Center Graphic Component Area
        indicator.setPreferredSize(new Dimension(150, 150));
        mainPanel.add(indicator, BorderLayout.CENTER);

        // Bottom Control Array (Input box and submission trigger)
        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        controlRow.setOpaque(false);

        JTextField inputField = new JTextField("75", 4);
        inputField.setFont(new Font("Monospaced", Font.BOLD, 14));
        inputField.setBackground(new Color(10, 25, 45));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createLineBorder(new Color(0, 130, 220), 1));
        inputField.setHorizontalAlignment(JTextField.CENTER);

        JButton actionButton = new JButton("UPDATE");
        actionButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        actionButton.setBackground(new Color(0, 50, 100));
        actionButton.setForeground(Color.WHITE);
        actionButton.setFocusPainted(false);
        actionButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 150, 255), 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));

        // Button action fires the Consumer hook
        actionButton.addActionListener(e -> {
            try {
                double targetVal = Double.parseDouble(inputField.getText().trim());
                onUpdateTrigger.accept(targetVal);
            } catch (NumberFormatException ex) {
                Toolkit.getDefaultToolkit().beep(); // Fail silently for demo if bad text typed
            }
        });

        controlRow.add(inputField);
        controlRow.add(actionButton);
        mainPanel.add(controlRow, BorderLayout.SOUTH);

        // Store action trigger inside client properties for initialization lookup code
        mainPanel.putClientProperty("trigger", onUpdateTrigger);

        return mainPanel;
    }

}
