package org.jwellman.demo.telemetry;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jwellman.demo.gauge.VRadialIndicator;

public class TelemetryStreamDemo {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("High-Frequency Telemetry Smoothing");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(5, 15, 30));
            frame.setLayout(new FlowLayout(FlowLayout.CENTER, 40, 40));

            VRadialIndicator indicator = new VRadialIndicator();
            indicator.setPreferredSize(new Dimension(250, 250));
            frame.add(indicator);

            // Configure our new TweenState: 
            // 1000ms animation transitions, with a 500ms sampling/breathing limit 
            // (Your exact ad-hoc rule of half the animation duration!)
            Engine.TweenState telemetryState = new Engine.TweenState(
                50.0, 
                1000, 
                500, 
                Engine.Easing.SINE_WAVE
            );

            frame.setSize(400, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // --- SPARK THE BACKGROUND TELEMETRY MACHINE ---
            // Simulates a firehose of raw metrics coming in from a network socket or serial port
            Thread telemetryFirehose = new Thread(() -> {
                Random rand = new Random();
                while (true) {
                    try {
                        // Generates a massive, erratic value spike every 10 milliseconds
                        double rawTelemetryValue = 20.0 + (rand.nextDouble() * 60.0);
                        
                        // Blindly feed the value into the state manager on the fly
                        SwingUtilities.invokeLater(() -> 
                            telemetryState.updateTelemetry(rawTelemetryValue, indicator)
                        );

                        Thread.sleep(10); 
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            telemetryFirehose.setDaemon(true);
            telemetryFirehose.start();
        });
    }

}
