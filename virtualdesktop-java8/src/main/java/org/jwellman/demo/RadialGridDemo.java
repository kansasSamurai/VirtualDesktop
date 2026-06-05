package org.jwellman.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class RadialGridDemo {

    public static void main(String[] args) {
        // Run UI creation on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Master Engine Grid Proof of Concept");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Premium dark theme context matching your UI mockup
            frame.getContentPane().setBackground(new Color(5, 15, 30));
            
            // 3 rows, 3 columns, with an elegant 10-pixel gap between elements
            frame.setLayout(new GridLayout(3, 3, 10, 10));

            // Populate the grid matrix with 9 independent, fluidly scaling indicators
            for (int i = 0; i < 9; i++) {
                VRadialIndicator indicator = new VRadialIndicator();
                
                // Set an initial small preferred size so layout initializes properly,
                // but GridLayout will forcefully expand this space on resize.
                indicator.setPreferredSize(new Dimension(100, 100));
                frame.add(indicator);

                // Register this specific element to the single master engine loop
                Engine.getInstance().register(context -> {
                    // Safety valve: if the indicator is removed or window closes, kill the hook
                    if (!indicator.isDisplayable()) {
                        return false; 
                    }
                    
                    // Route the unified wave parameter directly to the component
                    indicator.setPercentage(context.sineWave * 100.0);
                    return true; 
                });
            }

            frame.setSize(600, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
