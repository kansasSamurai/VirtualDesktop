package org.jwellman.demo;

import javax.swing.*;

import org.jwellman.demo.animation.Engine;
import org.jwellman.demo.gauge.VRadialIndicator;

import java.awt.*;

public class TweenComparisonDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Tween Engine Proof of Concept: Linear vs Sine");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(5, 15, 30));
            
            // 3 rows, 2 columns (Left Column = Linear, Right Column = Sine)
            frame.setLayout(new GridLayout(3, 2, 20, 20));

            long animationDuration = 2500; // All animations run for exactly 2.5 seconds

            for (int row = 0; row < 3; row++) {
                // --- 1. Create a Linear Indicator (Left Column) ---
                VRadialIndicator linearIndicator = new VRadialIndicator();
                frame.add(linearIndicator);

                Engine.getInstance().register(animationDuration, Engine.Easing.LINEAR, context -> {
                    if (!linearIndicator.isDisplayable()) return false;
                    
                    // context.value yields the raw linear progress (0.0 to 1.0)
                    linearIndicator.setPercentage(context.value * 100.0);
                    return true;
                });

                // --- 2. Create a Sine Eased Indicator (Right Column) ---
                VRadialIndicator sineIndicator = new VRadialIndicator();
                frame.add(sineIndicator);

                Engine.getInstance().register(animationDuration, Engine.Easing.SINE_WAVE, context -> {
                    if (!sineIndicator.isDisplayable()) return false;
                    
                    // context.value yields the sine-curved progress (accel/decel feeling)
                    sineIndicator.setPercentage(context.value * 100.0);
                    return true;
                });
            }

            frame.setSize(500, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
