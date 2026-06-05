package org.jwellman.demo.engine;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.jwellman.demo.engine.Strategy.EaseInCubic;
import org.jwellman.demo.engine.Strategy.EaseInOutCubic;
import org.jwellman.demo.engine.Strategy.EaseInOutQuint;
import org.jwellman.demo.engine.Strategy.EaseInOutSine;
import org.jwellman.demo.engine.Strategy.EaseInQuint;
import org.jwellman.demo.engine.Strategy.EaseInSine;
import org.jwellman.demo.engine.Strategy.EaseOutCubic;
import org.jwellman.demo.engine.Strategy.EaseOutQuint;
import org.jwellman.demo.engine.Strategy.EaseOutSine;
import org.jwellman.demo.engine.Strategy.OnceStrategy;
import org.jwellman.demo.gauge.VRadialIndicator;

public class EasingMatrixDemo {

    // Internal cell handle to cleanly tie components to their unique strategies
    private static class EasingCell {
        final String name;
        final EasingStrategy strategy;
        final VRadialIndicator indicator = new VRadialIndicator();

        EasingCell(String name, EasingStrategy strategy) {
            this.name = name;
            this.strategy = strategy;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Polymorphic Easing Strategy Matrix");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().setBackground(new Color(5, 15, 30));
            frame.setLayout(new BorderLayout(15, 15));

            // Map out the exact 3x3 layout from the image file
            EasingCell[] cells = new EasingCell[] {
                new EasingCell("easeInSine", new EaseInSine()),
                new EasingCell("easeOutSine", new EaseOutSine()),
                new EasingCell("easeInOutSine", new EaseInOutSine()),
                
                new EasingCell("easeInCubic", new EaseInCubic()),
                new EasingCell("easeOutCubic", new EaseOutCubic()),
                new EasingCell("easeInOutCubic", new EaseInOutCubic()),
                
                new EasingCell("easeInQuint", new EaseInQuint()),
                new EasingCell("easeOutQuint", new EaseOutQuint()),
                new EasingCell("easeInOutQuint", new EaseInOutQuint())
            };

            // Main 3x3 visual display panel
            JPanel gridPanel = new JPanel(new GridLayout(3, 3, 15, 15));
            gridPanel.setOpaque(false);
            gridPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            for (EasingCell cell : cells) {
                gridPanel.add(buildVisualCell(cell));
            }
            frame.add(gridPanel, BorderLayout.CENTER);

            // Bottom trigger action row
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            actionPanel.setOpaque(false);
            actionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

            JButton triggerBtn = new JButton("TRIGGER ANIMATIONS");
            triggerBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
            triggerBtn.setBackground(new Color(0, 50, 120));
            triggerBtn.setForeground(Color.WHITE);
            triggerBtn.setFocusPainted(false);
            triggerBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 160, 255), 1),
                BorderFactory.createEmptyBorder(8, 25, 8, 25)
            ));

            // Execution trigger
            triggerBtn.addActionListener(e -> {
                long animationDuration = 2000; // 2.0 second visual sweep
                
                for (EasingCell cell : cells) {
                    Engine.getInstance().register(
                        animationDuration, 
                        new OnceStrategy(), // Use our point-to-point strategy
                        cell.strategy, 
                        context -> {
                            if (!cell.indicator.isDisplayable()) return false;
                            
                            // Map 0.0 -> 1.0 value smoothly to 0 -> 100 percentage
                            cell.indicator.setPercentage(context.value * 100.0);
                            return true;
                        }
                    );
                }
            });

            actionPanel.add(triggerBtn);
            frame.add(actionPanel, BorderLayout.SOUTH);

            frame.setSize(850, 750);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static JPanel buildVisualCell(EasingCell cell) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createLineBorder(new Color(15, 35, 60), 1));
        
        cell.indicator.setPreferredSize(new Dimension(120, 120));
        panel.add(cell.indicator, BorderLayout.CENTER);

        JLabel label = new JLabel(cell.name, SwingConstants.CENTER);
        label.setForeground(new Color(0, 180, 255));
        label.setFont(new Font("Monospaced", Font.BOLD, 12));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(label, BorderLayout.SOUTH);

        return panel;
    }

}
