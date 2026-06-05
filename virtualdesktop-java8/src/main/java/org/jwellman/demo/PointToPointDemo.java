package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;

public class PointToPointDemo {

    private static class IndicatorState {
        double startValue = 0.0;
        double targetValue = 0.0;
        double currentValue = 0.0;

        /**
         * Computes the current position along the vector flight path.
         * @param progress The cooked/eased progression value from 0.0 to 1.0
         * @return The freshly calculated current value
         */
        public double update(double progress) {
            double delta = this.targetValue - this.startValue;
            this.currentValue = this.startValue + (delta * progress);
            return this.currentValue;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Interactive Premium Tween Dashboard");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(5, 15, 30));
            frame.setLayout(new GridLayout(1, 2, 20, 20));

            long animationDuration = 6283 / 2; // Bumped to 2.0s so you can play with interruptions easily

            // --- LEFT COLUMN: LINEAR TRANSITION ---
            IndicatorState leftState = new IndicatorState();
            VRadialIndicator leftIndicator = new VRadialIndicator();
            JPanel leftPanel = createControlCell("LINEAR TRANSITION", leftIndicator, leftState, (newValue) -> {
                leftState.startValue = leftState.currentValue;
                leftState.targetValue = newValue;

                Engine.getInstance().register(animationDuration, 
                        Engine.Easing.LINEAR, 
                        Engine.LoopMode.ONCE, c -> {
                    if (!leftIndicator.isDisplayable()) return false;
                    leftIndicator.setPercentage(leftState.update(c.value));                    return true;
                });
            });
            frame.add(leftPanel);

            // --- RIGHT COLUMN: SINE TRANSITION ---
            IndicatorState rightState = new IndicatorState();
            VRadialIndicator rightIndicator = new VRadialIndicator();
            JPanel rightPanel = createControlCell("SINE TRANSITION", rightIndicator, rightState, (newValue) -> {
                rightState.startValue = rightState.currentValue;
                rightState.targetValue = newValue;

                Engine.getInstance().register(animationDuration, 
                        Engine.Easing.SINE_WAVE, 
                        Engine.LoopMode.ONCE, c -> {
                    if (!rightIndicator.isDisplayable()) return false;
                    rightIndicator.setPercentage(rightState.update(c.value));                    return true;
                });
            });
            frame.add(rightPanel);

            // Trigger initialization sweep (0 -> 75%)
            ((java.util.function.Consumer<Double>) leftPanel.getClientProperty("trigger")).accept(75.0);
            ((java.util.function.Consumer<Double>) rightPanel.getClientProperty("trigger")).accept(75.0);

            frame.setSize(800, 550);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static JPanel createControlCell(String title, VRadialIndicator indicator, IndicatorState state, java.util.function.Consumer<Double> onUpdateTrigger) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header Title
        JLabel headerLabel = new JLabel(title, SwingConstants.CENTER);
        headerLabel.setForeground(new Color(0, 180, 255));
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Indicator Area
        indicator.setPreferredSize(new Dimension(200, 200));
        mainPanel.add(indicator, BorderLayout.CENTER);

        // Bottom Controls Container holding two separate quick action rows
        JPanel controlsLayout = new JPanel(new GridLayout(2, 1, 5, 5));
        controlsLayout.setOpaque(false);

        // Preset Channel A
        controlsLayout.add(createQuickInputRow("15", onUpdateTrigger));
        // Preset Channel B
        controlsLayout.add(createQuickInputRow("90", onUpdateTrigger));

        mainPanel.add(controlsLayout, BorderLayout.SOUTH);
        mainPanel.putClientProperty("trigger", onUpdateTrigger);

        return mainPanel;
    }

    /**
     * Helper to assemble a clean, self-contained text box + button row.
     */
    private static JPanel createQuickInputRow(String defaultVal, java.util.function.Consumer<Double> trigger) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        row.setOpaque(false);

        JTextField field = new JTextField(defaultVal, 3);
        field.setFont(new Font("Monospaced", Font.BOLD, 13));
        field.setBackground(new Color(10, 25, 45));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createLineBorder(new Color(0, 130, 220), 1));
        field.setHorizontalAlignment(JTextField.CENTER);

        JButton btn = new JButton("GO");
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setBackground(new Color(0, 50, 100));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 150, 255), 1),
            BorderFactory.createEmptyBorder(4, 15, 4, 15)
        ));

        btn.addActionListener(e -> {
            try {
                double target = Double.parseDouble(field.getText().trim());
                trigger.accept(target);
            } catch (NumberFormatException ex) {
                Toolkit.getDefaultToolkit().beep();
            }
        });

        row.add(field);
        row.add(btn);
        return row;
    }

}
