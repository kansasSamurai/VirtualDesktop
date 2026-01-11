package org.jwellman.demo;

import javax.swing.*;
import java.awt.*;

/**
 * This demo shows that CardLayout **is** capable of managing multiple panels
 * as long as ALL components added use names that are unique. (Note that the
 * CardLayout will NOT enforce that uniqueness natively).  
 * 
 * However, I think this behavior is an unintended side effect of the design.
 * Probably best to use one CardLayout per container(JPanel).
 * For an improved API, use BetterCardLayout in swing-utils.
 * 
 * @author rwellman
 *
 */
public class SharedCardLayoutDemo extends JFrame {

    private static final long serialVersionUID = 1L;

    public SharedCardLayoutDemo() {
        setTitle("Shared CardLayout Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create a SINGLE CardLayout instance
        CardLayout sharedCardLayout = new CardLayout();

        // Create FIRST panel with the shared CardLayout
        JPanel panel1 = new JPanel(sharedCardLayout);
        panel1.setBorder(BorderFactory.createTitledBorder("Panel 1"));
        panel1.add(createColorPanel("Panel 1 - Red", Color.RED), "red");
        panel1.add(createColorPanel("Panel 1 - Blue", Color.BLUE), "blue");
        panel1.add(createColorPanel("Panel 1 - Green", Color.GREEN), "green");

        // Create SECOND panel with the SAME CardLayout instance
        JPanel panel2 = new JPanel(sharedCardLayout);
        panel2.setBorder(BorderFactory.createTitledBorder("Panel 2"));
        panel2.add(createColorPanel("Panel 2 - Yellow", Color.YELLOW), "yellow");
        panel2.add(createColorPanel("Panel 2 - Orange", Color.ORANGE), "orange");
        panel2.add(createColorPanel("Panel 2 - Pink", Color.PINK), "pink");
        
        // Add both panels to the frame
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        centerPanel.add(panel1);
        centerPanel.add(panel2);
        add(centerPanel, BorderLayout.CENTER);
        
        // Control panel with buttons
        JPanel controlPanel = new JPanel();
        
        JButton showRedBtn = new JButton("Show 'blue' in Panel 1");
        showRedBtn.addActionListener(e -> sharedCardLayout.show(panel1, "blue"));
        
        JButton showBlueBtn = new JButton("Show 'green' in Panel 1");
        showBlueBtn.addActionListener(e -> sharedCardLayout.show(panel1, "green"));
        
        JButton showGreenBtn = new JButton("Show 'orange' in Panel 2");
        showGreenBtn.addActionListener(e -> sharedCardLayout.show(panel2, "orange"));
        
        JButton showOrangeBtn = new JButton("Show 'pink' in Panel 2");
        showOrangeBtn.addActionListener(e -> sharedCardLayout.show(panel2, "pink"));
        
        controlPanel.add(showRedBtn);
        controlPanel.add(showBlueBtn);
        controlPanel.add(showGreenBtn);
        controlPanel.add(showOrangeBtn);
        
        add(controlPanel, BorderLayout.SOUTH);
        
        setSize(800, 400);
        setLocationRelativeTo(null);
    }
    
    private JPanel createColorPanel(String text, Color color) {
        JPanel panel = new JPanel();
        panel.setBackground(color);
        panel.add(new JLabel(text));
        return panel;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SharedCardLayoutDemo().setVisible(true);
        });
    }
}
