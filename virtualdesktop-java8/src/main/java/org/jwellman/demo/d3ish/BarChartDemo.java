package org.jwellman.demo.d3ish;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;

public class BarChartDemo {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Foundation Framework - D3 Swing Concept");
        BarChartPanel chart = new BarChartPanel();

        chart.addBar("Users", 0.4);
        chart.addBar("Orders", 0.8);
        chart.addBar("Logs", 0.2);

//        double[] stateA = { 0.9, 0.3, 0.6 };
//        double[] stateB = { 0.2, 0.8, 0.4 };
        double[][] dataStates = {
                {0.1, 0.5, 0.9}, // State A
                {0.8, 0.2, 0.4}, // State B
                {0.5, 0.5, 0.5}  // State C
            };
        CircularArrayIterator<double[]> states = new CircularArrayIterator<>(dataStates);

        JButton toggleBtn = new JButton("Toggle State");
        toggleBtn.addActionListener(e -> {
            // Swap between two arbitrary data states
            // Just a simple flip-flop for the POC
            chart.updateData(states.next());
        });

        frame.add(chart, BorderLayout.CENTER);
        frame.add(toggleBtn, BorderLayout.SOUTH);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}
