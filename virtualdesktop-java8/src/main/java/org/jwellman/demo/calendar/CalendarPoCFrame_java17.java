package org.jwellman.demo.calendar;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import org.jwellman.demo.calendar.CalendarPoCFrame.SpatialStageLayout;
import org.jwellman.demo.calendar.CalendarPoCFrame.SpatialStageLayout.Constraint;

@SuppressWarnings("serial")
public class CalendarPoCFrame_java17 extends JFrame {

    private static final int DAYS = 7;
    private static final int HOURS = 12; // 8:00 AM to 8:00 PM

    public CalendarPoCFrame() {
        setTitle("Calendar PoC - Spatial Stage & Layered Pane");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // 1. Create the Root Layered Pane
        JLayeredPane layeredPane = new JLayeredPane();

        // 2. Base Layer: Holds background grid & event cards using SpatialStageLayout
        SpatialStageLayout spatialLayout = new SpatialStageLayout();
        JPanel baseGridPanel = new JPanel(spatialLayout);
        baseGridPanel.setBackground(Color.WHITE);

        // Create background grid visual (Days & Hours)
        JPanel gridBackground = createGridBackgroundPanel();
        baseGridPanel.add(gridBackground, new SpatialStageLayout.FullFillConstraint());

        // 3. Highlight Layer: Transparent overlay panel for time-slot snap guide
        SnapHighlightOverlay snapOverlay = new SnapHighlightOverlay();
        snapOverlay.setBounds(0, 0, 1000, 700);

        // Make baseGridPanel and snapOverlay track frame resizes via OverlayLayout
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        layeredPane.add(baseGridPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(snapOverlay, JLayeredPane.PALETTE_LAYER);

        // 4. Populate sample calendar events
        CalendarEventCard card1 = new CalendarEventCard("Team Sync", new Color(220, 235, 252), new Color(0, 120, 215));
        CalendarEventCard card2 = new CalendarEventCard("Design Review", new Color(254, 235, 226), new Color(216, 59, 1));

        baseGridPanel.add(card1, new SpatialStageLayout.GridCellConstraint(1, 1, HOURS, DAYS)); // Mon 9:00 AM
        baseGridPanel.add(card2, new SpatialStageLayout.GridCellConstraint(3, 3, HOURS, DAYS)); // Wed 11:00 AM

        // 5. Attach the Drag Controller
        CalendarDragController dragController = new CalendarDragController(
                layeredPane, baseGridPanel, snapOverlay, spatialLayout
        );
        card1.addMouseListener(dragController);
        card1.addMouseMotionListener(dragController);
        card2.addMouseListener(dragController);
        card2.addMouseMotionListener(dragController);

        add(layeredPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // 1. SPATIAL STAGE LAYOUT MANAGER
    // =========================================================================
    public static class SpatialStageLayout implements LayoutManager2 {
        public interface Constraint {
            Rectangle calculateBounds(Container parent);
        }

        public record GridCellConstraint(int row, int col, int totalRows, int totalCols) implements Constraint {
            @Override
            public Rectangle calculateBounds(Container parent) {
                int cellW = parent.getWidth() / totalCols;
                int cellH = parent.getHeight() / totalRows;
                return new Rectangle(col * cellW + 2, row * cellH + 2, cellW - 4, cellH - 4);
            }
        }

        public static class FullFillConstraint implements Constraint {
            @Override
            public Rectangle calculateBounds(Container parent) {
                return new Rectangle(0, 0, parent.getWidth(), parent.getHeight());
            }
        }

        private final Map<Component, Constraint> constraints = new HashMap<>();
        private final Map<Component, Boolean> transientComponents = new HashMap<>();

        public void setTransient(Component comp, boolean isTransient) {
            transientComponents.put(comp, isTransient);
        }

        public void setConstraint(Component comp, Constraint constraint) {
            constraints.put(comp, constraint);
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraint) {
            if (constraint instanceof Constraint c) constraints.put(comp, c);
        }

        @Override
        public void layoutContainer(Container parent) {
            synchronized (parent.getTreeLock()) {
                for (Component comp : parent.getComponents()) {
                    // EXEMPT TRANSIENT COMPONENTS (Actively being dragged!)
                    if (Boolean.TRUE.equals(transientComponents.get(comp))) continue;

                    Constraint c = constraints.get(comp);
                    if (c != null) comp.setBounds(c.calculateBounds(parent));
                }
            }
        }

        @Override public void addLayoutComponent(String name, Component comp) {}
        @Override public void removeLayoutComponent(Component comp) { constraints.remove(comp); transientComponents.remove(comp); }
        @Override public Dimension preferredLayoutSize(Container parent) { return parent.getSize(); }
        @Override public Dimension minimumLayoutSize(Container parent) { return new Dimension(200, 200); }
        @Override public Dimension maximumLayoutSize(Container parent) { return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); }
        @Override public float getLayoutAlignmentX(Container target) { return 0.5f; }
        @Override public float getLayoutAlignmentY(Container target) { return 0.5f; }
        @Override public void invalidateLayout(Container target) {}
    }

    // =========================================================================
    // 2. INTERACTION CONTROLLER (Lifting to DRAG_LAYER & Grid Snapping)
    // =========================================================================
    public static class CalendarDragController extends MouseAdapter {
        private final JLayeredPane stage;
        private final JPanel baseGridPanel;
        private final SnapHighlightOverlay snapOverlay;
        private final SpatialStageLayout spatialLayout;

        private CalendarEventCard activeCard;
        private Point dragOffset;
        private Point targetGridCell;

        public CalendarDragController(JLayeredPane stage, JPanel baseGridPanel,
                                      SnapHighlightOverlay snapOverlay, SpatialStageLayout spatialLayout) {
            this.stage = stage;
            this.baseGridPanel = baseGridPanel;
            this.snapOverlay = snapOverlay;
            this.spatialLayout = spatialLayout;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            activeCard = (CalendarEventCard) e.getSource();

            // Calculate mouse grab offset
            Point mouseOnStage = SwingUtilities.convertPoint(activeCard, e.getPoint(), stage);
            Point cardOnStage = SwingUtilities.convertPoint(baseGridPanel, activeCard.getLocation(), stage);
            dragOffset = new Point(mouseOnStage.x - cardOnStage.x, mouseOnStage.y - cardOnStage.y);

            // Mark transient in layout engine so it won't snap back on resize
            spatialLayout.setTransient(activeCard, true);

            // Lift to DRAG_LAYER
            baseGridPanel.remove(activeCard);
            activeCard.setBounds(cardOnStage.x, cardOnStage.y, activeCard.getWidth(), activeCard.getHeight());
            stage.add(activeCard, JLayeredPane.DRAG_LAYER);

            baseGridPanel.revalidate();
            stage.repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (activeCard == null) return;

            Point mouseOnStage = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), stage);
            activeCard.setLocation(mouseOnStage.x - dragOffset.x, mouseOnStage.y - dragOffset.y);

            // Calculate target grid cell on base panel
            Point mouseOnBase = SwingUtilities.convertPoint(stage, mouseOnStage, baseGridPanel);
            int col = Math.min(DAYS - 1, Math.max(0, mouseOnBase.x / (baseGridPanel.getWidth() / DAYS)));
            int row = Math.min(HOURS - 1, Math.max(0, mouseOnBase.y / (baseGridPanel.getHeight() / HOURS)));
            targetGridCell = new Point(col, row);

            // Update snap target highlight on PALETTE_LAYER
            int cellW = baseGridPanel.getWidth() / DAYS;
            int cellH = baseGridPanel.getHeight() / HOURS;
            snapOverlay.setSnapBounds(new Rectangle(col * cellW, row * cellH, cellW, cellH));
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (activeCard == null) return;

            // Remove from DRAG_LAYER
            stage.remove(activeCard);
            snapOverlay.clearSnapBounds();

            // Clear transient flag & update constraint
            spatialLayout.setTransient(activeCard, false);
            if (targetGridCell != null) {
                spatialLayout.setConstraint(activeCard, 
                    new SpatialStageLayout.GridCellConstraint(targetGridCell.y, targetGridCell.x, HOURS, DAYS));
            }

            // Return to Base Layer
            baseGridPanel.add(activeCard);
            baseGridPanel.revalidate();
            stage.repaint();

            activeCard = null;
            targetGridCell = null;
        }
    }

    // =========================================================================
    // 3. UI COMPONENTS & OVERLAYS
    // =========================================================================
    private static class CalendarEventCard extends JPanel {
        public CalendarEventCard(String title, Color bgColor, Color borderColor) {
            setLayout(new BorderLayout());
            setBackground(bgColor);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor, 1, true),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));
            JLabel label = new JLabel("<html><b>" + title + "</b><br><font size='2'>Drag to move</font></html>");
            label.setForeground(borderColor.darker());
            add(label, BorderLayout.NORTH);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    private static class SnapHighlightOverlay extends JPanel {
        private Rectangle snapBounds;

        public SnapHighlightOverlay() {
            setOpaque(false); // Let lower layers show through
        }

        public void setSnapBounds(Rectangle bounds) {
            this.snapBounds = bounds;
            repaint();
        }

        public void clearSnapBounds() {
            this.snapBounds = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (snapBounds != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 180, 136, 50)); // Soft green translucent snap fill
                g2.fillRect(snapBounds.x, snapBounds.y, snapBounds.width, snapBounds.height);
                g2.setColor(new Color(0, 180, 136, 200));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawRect(snapBounds.x, snapBounds.y, snapBounds.width, snapBounds.height);
                g2.dispose();
            }
        }
    }

    private JPanel createGridBackgroundPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(230, 230, 230));

                int cellW = getWidth() / DAYS;
                int cellH = getHeight() / HOURS;

                for (int i = 0; i <= DAYS; i++) g2.drawLine(i * cellW, 0, i * cellW, getHeight());
                for (int i = 0; i <= HOURS; i++) g2.drawLine(0, i * cellH, getWidth(), i * cellH);
            }
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalendarPoCFrame().setVisible(true));
    }

}
