package org.katacode.pipeline.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;

import org.katacode.pipeline.engine.PipelineContext;
import org.katacode.pipeline.engine.PipelineStep;

/**
 * Enhanced canvas container panel leveraging native ButtonGroup mechanics
 * to enforce elegant, single-selection logic without custom event math.
 */
@SuppressWarnings("serial")
public class PipelineCanvasPanel extends JPanel {

    private final JPanel cardStackPanel;
    private final ButtonGroup selectionGroup;
    private final PipelineContext pipelineContext;

    public PipelineCanvasPanel(PipelineContext pipelineContext) {
        this.pipelineContext = pipelineContext;
        this.selectionGroup = new ButtonGroup();
        
        setLayout(new BorderLayout());
        setBackground(new Color(0xF5, 0xF7, 0xFA));

        cardStackPanel = new JPanel() {
            @Override
            protected void paintChildren(Graphics g) {
                drawWorkflowConnectors((Graphics2D) g);
                super.paintChildren(g);
            }
        };
        cardStackPanel.setOpaque(false);
        cardStackPanel.setLayout(new BoxLayout(cardStackPanel, BoxLayout.Y_AXIS));
        cardStackPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        add(cardStackPanel, BorderLayout.NORTH);
        
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        add(spacer, BorderLayout.CENTER);

        refreshCanvas();
    }

    public void refreshCanvas() {
        // Clear past iterations from both components and group trackers
        cardStackPanel.removeAll();
        java.util.Enumeration<AbstractButton> elements = selectionGroup.getElements();
        while (elements.hasMoreElements()) {
            selectionGroup.remove(elements.nextElement());
        }

        List<PipelineStep> steps = pipelineContext.getSteps();

        for (int i = 0; i < steps.size(); i++) {
            PipelineStep step = steps.get(i);
            
            // Rehydrate card as a native toggle entity
            PipelineCardPanel card = new PipelineCardPanel(step, i + 1);
            
            // Register inside our exclusion cluster
            selectionGroup.add(card);
            cardStackPanel.add(card);

            // Standard action listener fires automatically for clicks, taps, or hotkeys
            card.addActionListener(e -> {
                // 1. Locate the master coordinating panel by walking up the component tree
                Container parent = getParent();
                while (parent != null && !(parent instanceof PipelineDesignerPanel)) {
                    parent = parent.getParent();
                }

                // 2. Pass the selected step down to the coordinator
                if (parent != null) {
                    PipelineDesignerPanel designer = (PipelineDesignerPanel) parent;
                    designer.handleCardSelection(card.getStep());
                }

                // 3. Request a repaint to update our custom selection border lines instantly
                cardStackPanel.repaint();
            });
//            card.addActionListener(e -> {
//                // Instantly query parent container context to sync our inspector panel form fields
//                Container parent = getParent();
//                while (parent != null && !(parent instanceof PipelineDesignerPanel)) {
//                    parent = parent.getParent();
//                }
//                if (parent != null) {
//                    ((PipelineDesignerPanel) parent).updateInspector(card.getStep());
//                }
//                // Trigger canvas redraw to swap border lines cleanly
//                cardStackPanel.repaint();
//            });

            if (i < steps.size() - 1) {
                cardStackPanel.add(Box.createRigidArea(new Dimension(0, 24)));
            }
        }

        cardStackPanel.revalidate();
        cardStackPanel.repaint();
    }

    private void drawWorkflowConnectors(Graphics2D g2) {
        Component[] comps = cardStackPanel.getComponents();
        if (comps.length < 3) return;

        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(0xB0, 0xC4, 0xDE));
        float[] dashPattern = {6.0f, 4.0f};
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));

        for (int i = 0; i < comps.length; i++) {
            // We only look at actual PipelineCardPanel components, skipping the spacer gaps
            if (comps[i] instanceof PipelineCardPanel && (i + 2) < comps.length) {
                Component currentCard = comps[i];
                Component nextCard = comps[i + 2]; // Skip the rigid spacing instance

                // Calculate the exact spatial midpoint of the gap
                int centerX = currentCard.getX() + (currentCard.getWidth() / 2);
                int startY = currentCard.getY() + currentCard.getHeight();
                int endY = nextCard.getY();

                // Draw the vertical line between the bottom of card A and top of card B
                g2.drawLine(centerX, startY, centerX, endY);

                // Draw a small solid indicator arrowhead right above the downstream card entry gate
                Graphics2D arrowG = (Graphics2D) g2.create();
                arrowG.setStroke(new BasicStroke(1.5f));
                arrowG.setColor(new Color(0x9A, 0xB2, 0xCD));
                
                int arrowSize = 5;
                int tipY = endY - 2;
                int[] xPoints = {centerX - arrowSize, centerX + arrowSize, centerX};
                int[] yPoints = {tipY - arrowSize, tipY - arrowSize, tipY};
                arrowG.fillPolygon(xPoints, yPoints, 3);
                arrowG.dispose();
            }
        }
        g2.dispose();
    }

}
