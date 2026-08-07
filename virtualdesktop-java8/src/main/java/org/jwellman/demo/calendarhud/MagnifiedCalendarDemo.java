package org.jwellman.demo.calendarhud;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

@SuppressWarnings("serial")
public class MagnifiedCalendarDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            JFrame frame = new JFrame("Focus + Context Lens Calendar Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(850, 650);
            frame.setLocationRelativeTo(null);

            // Add the modular JPanel component
            frame.add(new MagnifiedCalendarPanel());

            frame.setVisible(true);
        });
    }

    // =========================================================================
    // 1. REUSABLE CONTAINER PANEL (EXTENDS JPanel)
    // =========================================================================
    public static class MagnifiedCalendarPanel extends JPanel {

        private final JLayeredPane layeredPane;
        private final JScrollPane scrollPane;
        private final JPanel calendarStreamPanel;
        private final MagnifierOverlayPanel overlayPanel;

        public MagnifiedCalendarPanel() {
            setLayout(new BorderLayout());

            layeredPane = new JLayeredPane();

            // --- A. Build Scrollable Stream (Bottom Layer) ---
            calendarStreamPanel = new JPanel();
            calendarStreamPanel.setLayout(new BoxLayout(calendarStreamPanel, BoxLayout.Y_AXIS));
            calendarStreamPanel.setBackground(new Color(245, 245, 248));

            populateCalendarStream();

            scrollPane = new JScrollPane(calendarStreamPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            scrollPane.getVerticalScrollBar().setUnitIncrement(20);
            scrollPane.setBorder(null);

            layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);

            // --- B. Build Lens Overlay (Top Layer) ---
            overlayPanel = new MagnifierOverlayPanel();
            overlayPanel.setOpaque(false);

            layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);

            // --- C. Sync Component Bounds on Window Resize ---
            layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    Rectangle b = layeredPane.getBounds();
                    scrollPane.setBounds(0, 0, b.width, b.height);

                    // Position lens across the vertical center
                    int scrollBarWidth = scrollPane.getVerticalScrollBar().isVisible() 
                            ? scrollPane.getVerticalScrollBar().getWidth() : 0;
                    int lensHeight = 270; // Height of 4-5 week rows + header
                    int lensY = Math.max(0, (b.height - lensHeight) / 2);

                    overlayPanel.setBounds(0, lensY, b.width - scrollBarWidth, lensHeight);

                    layeredPane.revalidate();
                    layeredPane.repaint();
                    updateMonthHeader();
                }
            });

            add(layeredPane, BorderLayout.CENTER);

            // Wire viewport changes to update the floating month header on scroll
            scrollPane.getViewport().addChangeListener(e -> updateMonthHeader());

            // Initial position: scroll down slightly to center on current dates
            SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(new Point(0, 1200)));
        }

        private void populateCalendarStream() {
            // Generate ~52 weeks anchored around the current date
            LocalDate startMon = LocalDate.now().minusMonths(6).with(java.time.DayOfWeek.MONDAY);

            for (int i = 0; i < 52; i++) {
                LocalDate weekStart = startMon.plusWeeks(i);
                calendarStreamPanel.add(new WeekRowPanel(weekStart));
            }
        }

        @SuppressWarnings("unused")
        private void updateMonthHeader_java16() {
            if (scrollPane.getViewport().getView() == null) return;

            // Find the Y point directly in the middle of the lens overlay
            Point viewPos = scrollPane.getViewport().getViewPosition();
            int lensCenterY = viewPos.y + (scrollPane.getHeight() / 2);

            // Inspect child week panels to determine which month dominates the lens
            for (Component comp : calendarStreamPanel.getComponents()) {
//                if (comp instanceof WeekRowPanel weekRow) {
//                    if (lensCenterY >= weekRow.getY() && lensCenterY <= (weekRow.getY() + weekRow.getHeight())) {
//                        overlayPanel.setMonthTitle(weekRow.getDominantMonthYear().toUpperCase());
//                        break;
//                    }
//                }
            }
        }

        private void updateMonthHeader() {
            if (scrollPane.getViewport().getView() == null) return;

            Point viewPos = scrollPane.getViewport().getViewPosition();
            int lensCenterY = viewPos.y + (scrollPane.getHeight() / 2);

            for (Component comp : calendarStreamPanel.getComponents()) {
                // Java 8 style: check first, cast second
                if (comp instanceof WeekRowPanel) {
                    WeekRowPanel weekRow = (WeekRowPanel) comp;
                    
                    if (lensCenterY >= weekRow.getY() && lensCenterY <= (weekRow.getY() + weekRow.getHeight())) {
                        overlayPanel.setMonthTitle(weekRow.getDominantMonthYear().toUpperCase());
                        break;
                    }
                }
            }
        }

    }

    // =========================================================================
    // 2. LENS OVERLAY PANEL (EXTENDS JPanel)
    // =========================================================================
    public static class MagnifierOverlayPanel extends JPanel {

        private String currentMonthTitle = "OCTOBER 2026";
        private final int headerHeight = 30;

        public MagnifierOverlayPanel() {
            setLayout(null); // Pure overlay
        }

        public void setMonthTitle(String title) {
            if (!this.currentMonthTitle.equals(title)) {
                this.currentMonthTitle = title;
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Top Dynamic Banner Header
            g2.setColor(new Color(32, 34, 40, 235));
            g2.fillRect(0, 0, w, headerHeight);

            g2.setColor(new Color(230, 235, 245));
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(currentMonthTitle, 16, 20);

            // 2. Upper and Lower Accent Lines Framing the Lens
            g2.setColor(new Color(0, 120, 215, 220)); // Accent blue
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(0, headerHeight, w, headerHeight);
            g2.drawLine(0, h - 1, w, h - 1);

            // 3. Subtle Inner Gradient Shadows
            GradientPaint topShadow = new GradientPaint(
                    0, headerHeight, new Color(0, 0, 0, 35),
                    0, headerHeight + 10, new Color(0, 0, 0, 0));
            g2.setPaint(topShadow);
            g2.fillRect(0, headerHeight, w, 10);

            GradientPaint bottomShadow = new GradientPaint(
                    0, h - 10, new Color(0, 0, 0, 0),
                    0, h - 1, new Color(0, 0, 0, 35));
            g2.setPaint(bottomShadow);
            g2.fillRect(0, h - 10, w, 10);

            g2.dispose();
        }

        // --- SECRET SAUCE FOR OVERLAY PANELS ---
        // Let mouse events pass straight through the central peephole to the calendar below!
        @Override
        public boolean contains(int x, int y) {
            if (y <= headerHeight) {
                return true; // Banner handles its own clicks (if needed)
            }
            return false; // Clicks fall through to the underlying JScrollPane!
        }
    }

    // =========================================================================
    // 3. WEEK ROW COMPONENT (EXTENDS JPanel)
    // =========================================================================
    public static class WeekRowPanel extends JPanel {

        private final LocalDate weekStartDate;
        private static final DateTimeFormatter MONTH_YEAR_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

        public WeekRowPanel(LocalDate weekStartDate) {
            this.weekStartDate = weekStartDate;
            setLayout(new GridLayout(1, 7, 2, 2));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            setPreferredSize(new Dimension(800, 50));
            setBackground(new Color(235, 238, 242));
            setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));

            LocalDate now = LocalDate.now();

            for (int i = 0; i < 7; i++) {
                LocalDate date = weekStartDate.plusDays(i);
                boolean isToday = date.equals(now);
                add(new DayCellPanel(date, isToday));
            }
        }

        public String getDominantMonthYear() {
            // Mid-week date (Thursday) defines the dominant month for this week row
            return weekStartDate.plusDays(3).format(MONTH_YEAR_FMT);
        }
    }

    // =========================================================================
    // 4. DAY CELL COMPONENT (EXTENDS JPanel)
    // =========================================================================
    public static class DayCellPanel extends JPanel {

        private final LocalDate date;
        private static final DateTimeFormatter DAY_NUM_FMT = DateTimeFormatter.ofPattern("d");
        private static final DateTimeFormatter MONTH_BADGE_FMT = DateTimeFormatter.ofPattern("MMM d");

        public DayCellPanel(LocalDate date, boolean isToday) {
            this.date = date;
            setLayout(new BorderLayout());
            setOpaque(true);

            // Distinct rendering for the 1st of the month vs standard days
            boolean isFirstOfMonth = (date.getDayOfMonth() == 1);

            if (isToday) {
                setBackground(new Color(230, 242, 255));
                setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215), 2));
            } else {
                setBackground(Color.WHITE);
                setBorder(BorderFactory.createLineBorder(new Color(225, 228, 232), 1));
            }

            // Cell Labeling
            String labelText = isFirstOfMonth 
                    ? date.format(MONTH_BADGE_FMT).toUpperCase() 
                    : date.format(DAY_NUM_FMT);

            JLabel dayLabel = new JLabel(labelText, SwingConstants.LEFT);
            dayLabel.setFont(new Font("SansSerif", isFirstOfMonth || isToday ? Font.BOLD : Font.PLAIN, 11));
            dayLabel.setForeground(isFirstOfMonth ? new Color(180, 40, 20) : (isToday ? new Color(0, 100, 200) : Color.DARK_GRAY));
            dayLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 0, 0));

            add(dayLabel, BorderLayout.NORTH);

            // Interactive Fallback Matrix Gesture Handling
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isAltDown()) {
                        // Alt + Click: Smart Copy
                        System.out.println("[ACTION: COPY] ISO Timestamp: " + date);
                    } else if (e.getClickCount() == 2) {
                        // Double Click: Broadcast Time Context
                        System.out.println("[ACTION: BROADCAST] System context set to: " + date);
                    } else {
                        // Single Click: Inspect Telemetry
                        System.out.println("[ACTION: INSPECT] Opening telemetry for: " + date);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isToday) setBackground(new Color(245, 248, 252));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isToday) setBackground(Color.WHITE);
                }
            });
        }
    }
}
