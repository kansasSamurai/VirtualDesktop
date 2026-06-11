package org.jwellman.demo.connectablepanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Demo application showing how to use ConnectablePanel 
 */
public class ConnectablePanelDemo extends JFrame {

    private JPanel canvas;

    private List<ConnectablePanel> connectablePanels = new ArrayList<>();

    private static final long serialVersionUID = 1L;

    public ConnectablePanelDemo() {
        setTitle("ConnectablePanel Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);

        canvas = new JPanel(null); // null layout for absolute positioning
        canvas.setBackground(new Color(240, 240, 240));

        createDemoPanels();
        createToolBar();

        add(new JScrollPane(canvas), BorderLayout.CENTER);
        setLocationRelativeTo(null);
    }

    private void createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton addPanelBtn = new JButton("Add Panel");
        addPanelBtn.addActionListener(e -> addNewPanel());
        toolBar.add(addPanelBtn);

        JButton addButtonPanelBtn = new JButton("Add Button Panel");
        addButtonPanelBtn.addActionListener(e -> addButtonPanel());
        toolBar.add(addButtonPanelBtn);

        toolBar.addSeparator();

        JLabel infoLabel = new JLabel("  Hover over panels to see connection ports");
        toolBar.add(infoLabel);

        add(toolBar, BorderLayout.NORTH);
    }

    private void createDemoPanels() {
        // Create a few demo panels
        ConnectablePanel panel1 = createStyledPanel("Database", new Color(173, 216, 230));
        panel1.setBounds(50, 50, 150, 100);
        addDraggablePanel(panel1);

        ConnectablePanel panel2 = createStyledPanel("Application", new Color(144, 238, 144));
        panel2.setBounds(250, 50, 150, 100);
        addDraggablePanel(panel2);

        ConnectablePanel panel3 = createStyledPanel("Cache", new Color(255, 218, 185));
        panel3.setBounds(150, 200, 150, 100);
        addDraggablePanel(panel3);

        // Create a panel with custom content
        ConnectablePanel panel4 = new ConnectablePanel(new BorderLayout());
        panel4.setComponentId("Custom Panel");
        panel4.setBackground(new Color(230, 200, 250));

        JLabel titleLabel = new JLabel("UI Layer", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel4.add(titleLabel, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea("Contains UI\ncomponents");
        textArea.setEditable(false);
        textArea.setBackground(panel4.getBackground());
        panel4.add(textArea, BorderLayout.CENTER);

        panel4.setBounds(450, 50, 150, 100);
        addDraggablePanel(panel4);
    }

    private ConnectablePanel createStyledPanel(String title, Color bgColor) {
        ConnectablePanel panel = new ConnectablePanel(new BorderLayout());
        panel.setComponentId(title);
        panel.setBackground(bgColor);

        JLabel label = new JLabel(title, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }

    private void addNewPanel() {
        ConnectablePanel panel = createStyledPanel("Panel " + (connectablePanels.size() + 1), new Color(200, 200, 255));
        panel.setBounds(100 + connectablePanels.size() * 20, 100 + connectablePanels.size() * 20, 150, 100);
        addDraggablePanel(panel);
    }

    private void addButtonPanel() {
        ConnectablePanel panel = new ConnectablePanel(new FlowLayout());
        panel.setComponentId("Button Panel");
        panel.setBackground(new Color(255, 240, 200));

        panel.add(new JButton("Action 1"));
        panel.add(new JButton("Action 2"));

        panel.setBounds(100, 350, 200, 80);
        addDraggablePanel(panel);
    }

    private void addDraggablePanel(ConnectablePanel panel) {
        // Add drag capability
        PanelDragHandler dragHandler = new PanelDragHandler(panel);
        panel.addMouseListener(dragHandler);
        panel.addMouseMotionListener(dragHandler);

        connectablePanels.add(panel);
        canvas.add(panel);
        canvas.revalidate();
        canvas.repaint();
    }

    /**
     * Makes ConnectablePanel draggable within the canvas
     */
    public static class PanelDragHandler extends MouseAdapter {
        private ConnectablePanel panel;
        private Point pressPoint;
        private boolean isDragging = false;

        public PanelDragHandler(ConnectablePanel panel) {
            this.panel = panel;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (!panel.isDraggable())
                return;

            // Don't drag if clicking on a port
            if (panel.getHoveredPort() != null) {
                // Port clicked - could start connection drawing here
                System.out.println("Port clicked: " + panel.getHoveredPort());
                return;
            }

            pressPoint = e.getPoint();
            isDragging = true;
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!isDragging || !panel.isDraggable())
                return;

            int dx = e.getX() - pressPoint.x;
            int dy = e.getY() - pressPoint.y;

            Point location = panel.getLocation();
            panel.setLocation(location.x + dx, location.y + dy);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            isDragging = false;
            if (panel.getHoveredPort() == null) {
                panel.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ConnectablePanelDemo().setVisible(true);
        });
    }
}
