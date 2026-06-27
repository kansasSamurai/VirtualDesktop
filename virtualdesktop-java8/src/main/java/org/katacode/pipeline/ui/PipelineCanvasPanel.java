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

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.katacode.pipeline.engine.PipelineContext;
import org.katacode.pipeline.engine.PipelineStep;

/**
 * The execution flow canvas container panel.
 * Stacks individual PipelineCardPanel components and natively draws
 * the dashed workflow connector routes between them.
 */
@SuppressWarnings("serial")
public class PipelineCanvasPanel extends JPanel {

    private final PipelineContext pipelineContext;
    private final JPanel cardStackPanel;
    private int selectedStepIndex = -1;

    public PipelineCanvasPanel(PipelineContext pipelineContext) {
        this.pipelineContext = pipelineContext;
        
        setLayout(new BorderLayout());
        setBackground(new Color(0xF5, 0xF7, 0xFA)); // Canvas off-white background

        // 1. Create the container panel that will hold the actual vertical row layout
        cardStackPanel = new JPanel() {
            @Override
            protected void paintChildren(Graphics g) {
                // Draw the connector lines *behind* the components but *before* their text
                drawWorkflowConnectors((Graphics2D) g);
                super.paintChildren(g);
            }
        };
        cardStackPanel.setOpaque(false);
        cardStackPanel.setLayout(new BoxLayout(cardStackPanel, BoxLayout.Y_AXIS));
        cardStackPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // 2. Put the stack in the NORTH slot, and an empty spacer in the CENTER slot.
        // This stops BoxLayout from stretching cards vertically if there's extra room.
        add(cardStackPanel, BorderLayout.NORTH);
        
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        add(spacer, BorderLayout.CENTER);

        refreshCanvas();
    }

    /**
     * Clears the UI grid state and completely rebuilds the view rows 
     * based directly on the engine's active execution chain state.
     */
    public void refreshCanvas() {
        cardStackPanel.removeAll();
        List<PipelineStep> steps = pipelineContext.getSteps();

        for (int i = 0; i < steps.size(); i++) {
            PipelineStep step = steps.get(i);
            final int index = i;
            
            // Rehydrate our custom-rendered card block panel
            boolean isSelected = (i == selectedStepIndex);
            PipelineCardPanel card = new PipelineCardPanel(step, i + 1, isSelected);
            
            // Wire mouse interactions for step canvas targeting
            
            // new
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    setSelectedStepIndex(index);
                    
                    // Walk up the component ancestry tree to update our inspector input panel fields
                    Container parent = getParent();
                    while (parent != null && !(parent instanceof PipelineDesignerPanel)) {
                        parent = parent.getParent();
                    }
                    if (parent != null) {
                        ((PipelineDesignerPanel) parent).updateInspector(step);
                    }
                }
            });
            
            //old
//            card.addMouseListener(new java.awt.event.MouseAdapter() {
//                @Override
//                public void mousePressed(java.awt.event.MouseEvent e) {
//                    setSelectedStepIndex(index);
//                }
//            });

            cardStackPanel.add(card);

            // Add a fixed spacer panel between components to make room for drawing connector paths
            if (i < steps.size() - 1) {
                cardStackPanel.add(Box.createRigidArea(new Dimension(0, 24)));
            }
        }

        cardStackPanel.revalidate();
        cardStackPanel.repaint();
    }

    public void setSelectedStepIndex(int index) {
        this.selectedStepIndex = index;
        // In the future, this is where we will trigger an event to update the Data View and Properties Inspector
        refreshCanvas();
    }

    /**
     * Dynamically calculates child bounding footprints and paints clean,
     * native Java2D connect-the-dots flow vectors between the card boundaries.
     */
    private void drawWorkflowConnectors(Graphics2D g2) {
        Component[] comps = cardStackPanel.getComponents();
        if (comps.length < 3) return; // Need at least two cards and a rigid spacer to draw lines

        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Styling the line matching our visual prototype
        g2.setColor(new Color(0xB0, 0xC4, 0xDE)); // Muted steel blue arrow track
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
