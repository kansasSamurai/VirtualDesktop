package org.jwellman.demo.timer;

import javax.swing.*;
import java.awt.*;

public class CentralTimerDemo extends JFrame {

    private int animationStep = 0;
    private JLabel loadingLabel;
    private JLabel counterLabel;
    private int counter = 0;

    // TID := TASK ID
    private final String TID_COUNTER = "counter";
    private final String TID_ANIMATE = "loading-animation";
    private final String TID_MESSAGE = "welcome-message";

    private static final long serialVersionUID = 1L;

    public CentralTimerDemo() {
        setTitle("Central Timer Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create UI components
        JPanel centerPanel = new JPanel(new GridLayout(2, 1));

        loadingLabel = new JLabel("Loading", SwingConstants.LEFT);
        loadingLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        centerPanel.add(loadingLabel);

        counterLabel = new JLabel("Counter: 0", SwingConstants.CENTER);
        counterLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        centerPanel.add(counterLabel);

        add(centerPanel, BorderLayout.CENTER);

        // Control buttons
        JPanel controlPanel = new JPanel();
        JButton pauseAnimButton = new JButton("Pause Animation");
        JButton resumeAnimButton = new JButton("Resume Animation");
        JButton stopCounterButton = new JButton("Stop Counter");

        controlPanel.add(pauseAnimButton);
        controlPanel.add(resumeAnimButton);
        controlPanel.add(stopCounterButton);
        add(controlPanel, BorderLayout.SOUTH);

        // Get central timer instance
        CentralTimer timer = CentralTimer.getInstance(50); // 50ms tick rate

        // Schedule animation task (500ms interval)
        int count = 14;
        String one = "\u25A0"; // black square
        String two = "\u2022"; // black circle
        String[] frames = new String[count];
        for (int i=0; i<count; i+=2) {
            String first = "";
            for (int j=0; j<i; j++) {
                first += two;
            }
            frames[i] = first + one;
            frames[i+1] = first + two;
        }
        timer.scheduleTask(TID_ANIMATE, 500, () -> {
            loadingLabel.setText("Loading " + frames[animationStep]);
            animationStep = (animationStep + 1) % frames.length;
        });

        // Schedule counter task (1000ms interval)
        timer.scheduleTask(TID_COUNTER, 1000, () -> {
            counter++;
            counterLabel.setText("Counter: " + counter);
        });

        // Schedule a one-time task (5 seconds delay)
        timer.scheduleOnce(TID_MESSAGE, 5000, () -> {
            JOptionPane.showMessageDialog(this, "This message appeared after 5 seconds!");
        });

        // Wire up control buttons
        pauseAnimButton.addActionListener(e -> timer.pauseTask(TID_ANIMATE));
        resumeAnimButton.addActionListener(e -> timer.resumeTask(TID_ANIMATE));
        stopCounterButton.addActionListener(e -> timer.cancelTask(TID_COUNTER));

        pack(); // setSize(400, 200);
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CentralTimerDemo().setVisible(true);
        });
    }

}
