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
import javax.swing.UIManager;
import javax.swing.border.Border;

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

    private static final Font      LAYER_NAME_FONT = new Font("Arial", Font.BOLD, 12);
    private static final Font      LAYER_LABEL_FONT = new Font("Arial", Font.PLAIN, 10);
    private static final Dimension MAX_HEIGHT_SIZE  = new Dimension(Integer.MAX_VALUE, 40);
    private static final Border    PANEL_BORDER     = BorderFactory.createEmptyBorder(5, 5, 5, 5);

    public LayerControlPanel(String layerName, Integer layerDepth, DiagramLayeredPane diagramPane) {
        this.layerName = layerName;
        this.layerDepth = layerDepth;
        this.diagramPane = diagramPane;

        setLayout(new BorderLayout(5, 0));
        setMaximumSize(MAX_HEIGHT_SIZE);
        setBorder(PANEL_BORDER);

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
        nameLabel.setFont(LAYER_NAME_FONT);
        infoPanel.add(nameLabel, BorderLayout.NORTH);

        countLabel = new JLabel("0 items");
        countLabel.setFont(LAYER_LABEL_FONT);
        countLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        infoPanel.add(countLabel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.CENTER);

        // Layer depth indicator
        JLabel depthLabel = new JLabel("" + layerDepth);
        depthLabel.setFont(LAYER_LABEL_FONT);
        depthLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
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
                    setBackground(hoverBackground());
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
        nameLabel.setForeground(active
            ? UIManager.getColor("List.selectionForeground")
            : UIManager.getColor("Label.foreground"));
    }

    private void updateBackground() {
        if (isActive) {
            setBackground(UIManager.getColor("List.selectionBackground"));
        } else {
            setBackground(UIManager.getColor("Panel.background"));
        }
    }

    private Color hoverBackground() {
        Color sel = UIManager.getColor("List.selectionBackground");
        Color bg  = UIManager.getColor("Panel.background");
        int r = (sel.getRed()   * 30 + bg.getRed()   * 70) / 100;
        int g = (sel.getGreen() * 30 + bg.getGreen() * 70) / 100;
        int b = (sel.getBlue()  * 30 + bg.getBlue()  * 70) / 100;
        return new Color(r, g, b);
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
