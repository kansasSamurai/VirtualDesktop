package org.jwellman.diagram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Timer;

/**
 * Visual control panel for each layer
 */
class LayerControlPanel extends JPanel {

    private JLabel nameLabel;
    private JLabel countLabel;
    private JToggleButton visibilityButton;

    private DiagramLayeredPane diagramPane;

    @SuppressWarnings("unused")
    private String layerName;
    private Integer layerDepth;
    private boolean isActive = false;

    private static final long serialVersionUID = 1L;

    public LayerControlPanel(String layerName, Integer layerDepth, DiagramLayeredPane diagramPane) {
        this.layerName = layerName;
        this.layerDepth = layerDepth;
        this.diagramPane = diagramPane;

        setLayout(new BorderLayout(5, 0));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Set initial active state
        isActive = layerDepth.equals(diagramPane.getActiveLayer());
        updateBackground();

        // Visibility toggle button (eye icon)
        visibilityButton = new JToggleButton();
        visibilityButton.setSelected(diagramPane.isLayerVisible(layerDepth));
        visibilityButton.setPreferredSize(new Dimension(30, 30));
        visibilityButton.setFocusPainted(false);
        updateVisibilityIcon();

        visibilityButton.addActionListener(e -> {
            boolean visible = visibilityButton.isSelected();
            diagramPane.setLayerVisible(layerDepth, visible);
            updateVisibilityIcon();
        });

        add(visibilityButton, BorderLayout.WEST);

        // Layer info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);

        nameLabel = new JLabel(layerName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        infoPanel.add(nameLabel, BorderLayout.NORTH);

        countLabel = new JLabel("0 items");
        countLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        countLabel.setForeground(Color.GRAY);
        infoPanel.add(countLabel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.CENTER);

        // Layer depth indicator
        JLabel depthLabel = new JLabel("" + layerDepth);
        depthLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        depthLabel.setForeground(Color.GRAY);
        add(depthLabel, BorderLayout.EAST);

        // Click handler to set active layer
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 &&
                    !visibilityButton.contains(e.getPoint())) {
                    setActiveLayer();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isActive) {
                    setBackground(new Color(240, 240, 255));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateBackground();
            }
        });

        // Update count periodically
        Timer timer = new Timer(1000, e -> updateCount());
        timer.start();
    }

    private void setActiveLayer() {
        diagramPane.setActiveLayer(layerDepth);

        // Update all layer panels
        Container parent = getParent();
        if (parent != null) {
            for (Component comp : parent.getComponents()) {
                if (comp instanceof LayerControlPanel) {
                    LayerControlPanel panel = (LayerControlPanel) comp;
                    panel.setActive(panel.layerDepth.equals(layerDepth));
                }
            }
        }
    }

    private void setActive(boolean active) {
        this.isActive = active;
        updateBackground();
        nameLabel.setForeground(active ? new Color(0, 100, 200) : Color.BLACK);
    }

    private void updateBackground() {
        if (isActive) {
            setBackground(new Color(200, 220, 255));
        } else {
            setBackground(Color.WHITE);
        }
    }

    private void updateVisibilityIcon() {
        if (visibilityButton.isSelected()) {
            visibilityButton.setText("👁");
            visibilityButton.setToolTipText("Layer visible - click to hide");
        } else {
            visibilityButton.setText("⊗");
            visibilityButton.setToolTipText("Layer hidden - click to show");
        }
    }

    private void updateCount() {
        Map<Integer, Integer> counts = diagramPane.getLayerCounts();
        int count = counts.getOrDefault(layerDepth, 0);
        countLabel.setText(count + (count == 1 ? " item" : " items"));
    }
}
