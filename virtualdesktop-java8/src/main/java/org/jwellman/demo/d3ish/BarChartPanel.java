package org.jwellman.demo.d3ish;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class BarChartPanel extends JPanel {

    private Timer animator;
    private final List<Bar> bars = new ArrayList<>();

    // Define your "Foundation Palette"
    private final CircularIterator<Color> palette = new CircularIterator<>(
            Arrays.asList(
            new Color(74, 144, 226), // Foundation Blue
            new Color(80, 227, 194), // Teal
            new Color(155, 126, 255), // Purple
            new Color(245, 166, 35) // Orange
    ));

    public BarChartPanel() {
        setBackground(Color.WHITE);
        // 60 FPS Animation Timer
        animator = new Timer(16, e -> {
            repaint();
            if (bars.stream().noneMatch(Bar::isAnimating)) animator.stop();
        });
    }

    public void addBar(String label, double value) {
        // Automatically picks the next color in the sequence
        Bar bar = new Bar(label, value);
        // bar.setColor(palette.next());
        bars.add(bar);
    }


    public void updateData(double[] newValues) {
        for (int i = 0; i < bars.size() && i < newValues.length; i++) {
            bars.get(i).animateTo(newValues[i]);
        }
        animator.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padding = 50;
        int barWidth = (getWidth() - (padding * 2)) / bars.size() - 20;
        int chartHeight = getHeight() - 100;

        for (int i = 0; i < bars.size(); i++) {
            int x = padding + (i * (barWidth + 20));
            bars.get(i).draw(g2, x, 50, barWidth, chartHeight);
        }
    }
}