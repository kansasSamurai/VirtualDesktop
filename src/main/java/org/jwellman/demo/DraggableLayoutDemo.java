package org.jwellman.demo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class DraggableLayoutDemo extends JFrame {

    private GridPanel canvas;
    private SerializableAbsoluteLayout layout;
    private File layoutFile = new File("layout.dat");

    private static final long serialVersionUID = 1L;

    public DraggableLayoutDemo() {
        setTitle("Draggable Layout Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        // Create canvas with custom layout
        layout = new SerializableAbsoluteLayout();
        canvas = new GridPanel(layout);
        canvas.setBackground(Color.WHITE);

        // Create component mover
        ComponentMover mover = new ComponentMover(layout);

        // Add some components
        addDraggableButton("Button 1", 50, 50, mover);
        addDraggableButton("Button 2", 200, 100, mover);
        addDraggableButton("Button 3", 350, 150, mover);

        addDraggablePanel("Panel 1", 50, 250, mover);

        // Add menu for save/load
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem saveItem = new JMenuItem("Save Layout");
        saveItem.addActionListener(e -> saveLayout());

        JMenuItem loadItem = new JMenuItem("Load Layout");
        loadItem.addActionListener(e -> loadLayout());

        fileMenu.add(saveItem);
        fileMenu.add(loadItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        add(new JScrollPane(canvas));

        // Try to load existing layout
        if (layoutFile.exists()) {
            loadLayout();
        }

        setLocationRelativeTo(null);
    }
    
    private void addDraggablePanel(String title, int x, int y, ComponentMover mover) {
        JPanel p = new JPanel(new BorderLayout());
        p.setName(title);
        p.setBorder(BorderFactory.createLineBorder(Color.black));
        p.add(new JLabel(title), BorderLayout.NORTH);
        p.add(new JButton("Click"), BorderLayout.SOUTH);
        p.setBounds(y, y, 100, 50);

        layout.addLayoutComponent(p, p.getBounds());
        mover.makeComponentDraggable(p);
        canvas.add(p);
    }

    private void addDraggableButton(String text, int x, int y, ComponentMover mover) {
        JButton button = new JButton(text);
        button.setName(text); // Important for serialization
        button.setBounds(x, y, 100, 30);

        layout.addLayoutComponent(button, button.getBounds());
        mover.makeComponentDraggable(button);
        canvas.add(button);
    }

    private void saveLayout() {
        try {
            layout.saveLayout(layoutFile);
            JOptionPane.showMessageDialog(this, "Layout saved successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving layout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLayout() {
        try {
            layout.loadLayout(layoutFile, canvas);
            canvas.revalidate();
            canvas.repaint();
            JOptionPane.showMessageDialog(this, "Layout loaded successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading layout: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new DraggableLayoutDemo().setVisible(true);
        });
    }

}