package org.katacode.pipeline.ui.camel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * A layout designer structured specifically to map to true Apache Camel conventions.
 * Supports multiple distinct routes, nesting, and structural EIP branching elements.
 */
@SuppressWarnings("serial")
public class CamelRouteDesignerPanel extends JPanel {

    public CamelRouteDesignerPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(0xF5, 0xF7, 0xFA)); // Off-white canvas surface

        // Master container stacking multiple top-level Camel Routes
        JPanel routesContainer = new JPanel();
        routesContainer.setOpaque(false);
        routesContainer.setLayout(new BoxLayout(routesContainer, BoxLayout.Y_AXIS));
        routesContainer.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        // --- MOCK ROUTE 1: Inbound Processing ---
        CamelRouteWidget route1 = new CamelRouteWidget("route-id: transaction-ingest");
        route1.addNode(new CamelNodeWidget("from: file://orders/inbound", "Consumer / File Poller", "Blue"));
        route1.addNode(new CamelNodeWidget("marshal: json-jackson", "DataFormat Transformer", "Amber"));
        
        // Complex structural pattern branch (Choice / Content-Based Router)
        CamelChoiceBranchWidget choiceNode = new CamelChoiceBranchWidget("choice [Content-Based Router]");
        choiceNode.addBranch("when: ${header.origin} == 'DB'", new CamelNodeWidget("to: sql:insert-tx", "Producer / JDBC Sink", "Purple"));
        choiceNode.addBranch("otherwise", new CamelNodeWidget("to: jms:queue:dead-letter", "ActiveMQ Drop", "Purple"));
        route1.addNode(choiceNode);

        // --- MOCK ROUTE 2: Asynchronous Alerting (Separate Camel Route) ---
        CamelRouteWidget route2 = new CamelRouteWidget("route-id: dead-letter-monitor");
        route2.addNode(new CamelNodeWidget("from: jms:queue:dead-letter", "Consumer / JMS Queue", "Blue"));
        route2.addNode(new CamelNodeWidget("to: http://slack.com/services/hook", "External REST Call", "Purple"));

        routesContainer.add(route1);
        routesContainer.add(Box.createRigidArea(new Dimension(0, 40))); // Generous space between routes
        routesContainer.add(route2);

        JScrollPane mainScroll = new JScrollPane(routesContainer);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);
    }

    // =========================================================================
    // STRUCTURAL CONTAINER: Represents a single standalone Camel Route definition
    // =========================================================================
    private static class CamelRouteWidget extends JPanel {
        private final JPanel pipelineContainer;

        public CamelRouteWidget(String routeIdText) {
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(0xDC, 0xE1, 0xE6), 1, true));

            // Route Context Header
            JPanel routeHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            routeHeader.setBackground(new Color(0xEB, 0xEE, 0xF2));
            JLabel lblIcon = new JLabel("⚡");
            JLabel lblTitle = new JLabel(routeIdText);
            lblTitle.setFont(new Font("Monospaced", Font.BOLD, 12));
            lblTitle.setForeground(new Color(0x4A, 0x55, 0x68));
            routeHeader.add(lblIcon);
            routeHeader.add(lblTitle);
            add(routeHeader, BorderLayout.NORTH);

            // Sequential inner track container
            pipelineContainer = new JPanel();
            pipelineContainer.setOpaque(false);
            pipelineContainer.setLayout(new BoxLayout(pipelineContainer, BoxLayout.Y_AXIS));
            pipelineContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            add(pipelineContainer, BorderLayout.CENTER);
        }

        public void addNode(JComponent node) {
            if (pipelineContainer.getComponentCount() > 0) {
                pipelineContainer.add(new CamelTrackConnectorWidget()); // Linear path connector arrow
            }
            pipelineContainer.add(node);
        }
    }

    // =========================================================================
    // CONTENT ELEMENT: Single standard atomic Camel Component Node
    // =========================================================================
    private static class CamelNodeWidget extends JToggleButton {
        public CamelNodeWidget(String uriTitle, String categorySubtitle, String paletteType) {
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setLayout(new BorderLayout(14, 0));
            setBorder(new EmptyBorder(10, 14, 10, 14));

            // Determine soft palette colors based on parameters
            Color bgLight = new Color(0xE3, 0xD4, 0xEB);
            Color bgDark = new Color(0xF4, 0xEE, 0xF7);
            if ("Blue".equals(paletteType)) {
                bgLight = new Color(0xC5, 0xD7, 0xE8); bgDark = new Color(0xE2, 0xEC, 0xF5);
            } else if ("Amber".equals(paletteType)) {
                bgLight = new Color(0xFA, 0xDE, 0xC1); bgDark = new Color(0xFF, 0xF1, 0xE3);
            }

            // UI Label Building
            JPanel centerText = new JPanel(new GridLayout(2, 1, 0, 1));
            centerText.setOpaque(false);
            JLabel title = new JLabel(uriTitle);
            title.setFont(new Font("Monospaced", Font.BOLD, 13));
            JLabel sub = new JLabel(categorySubtitle);
            sub.setFont(new Font("SansSerif", Font.PLAIN, 11));
            sub.setForeground(new Color(0x71, 0x80, 0x96));
            centerText.add(title);
            centerText.add(sub);
            add(centerText, BorderLayout.CENTER);

            // Paint block override
            final Color bLight = bgLight; final Color bDark = bgDark;
            JPanel renderPane = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0, 0, bLight, 0, getHeight(), bDark));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    g2.setColor(isSelected() ? Color.ORANGE : new Color(0xCB, 0xD5, 0xE1));
                    g2.setStroke(new BasicStroke(isSelected() ? 2.2f : 1.2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                    g2.dispose();
                }
            };
            renderPane.setLayout(new BorderLayout());
            renderPane.add(centerText, BorderLayout.CENTER);
            renderPane.setBorder(new EmptyBorder(8, 12, 8, 12));
            add(renderPane, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredSize() { return new Dimension(440, 56); }
        @Override
        public Dimension getMaximumSize() { return getPreferredSize(); }
    }

    // =========================================================================
    // COMPLEX BRANCH ELEMENT: Represents structural EIP patterns (Choice / Split)
    // =========================================================================
    private static class CamelChoiceBranchWidget extends JPanel {
        private final JPanel branchesRow;

        public CamelChoiceBranchWidget(String title) {
            setLayout(new BorderLayout(0, 10));
            setOpaque(false);

            // Header EIP Identification bar
            JLabel header = new JLabel(" ❖ " + title);
            header.setFont(new Font("SansSerif", Font.BOLD, 12));
            header.setForeground(new Color(0x2D, 0x37, 0x48));
            header.setHorizontalAlignment(SwingConstants.CENTER);
            add(header, BorderLayout.NORTH);

            // Multi-column side-by-side split lane panel
            branchesRow = new JPanel(new GridLayout(1, 0, 16, 0));
            branchesRow.setOpaque(false);
            add(branchesRow, BorderLayout.CENTER);
        }

        public void addBranch(String conditionText, JComponent childNode) {
            JPanel column = new JPanel(new BorderLayout(0, 8));
            column.setOpaque(true);
            column.setBackground(new Color(0xF8, 0xFA, 0xFC));
            column.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2, 0xE8, 0xF0), 1, true),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));

            JLabel condLabel = new JLabel(conditionText);
            condLabel.setFont(new Font("Monospaced", Font.ITALIC, 11));
            condLabel.setForeground(Color.BLUE);
            column.add(condLabel, BorderLayout.NORTH);
            column.add(childNode, BorderLayout.CENTER);

            branchesRow.add(column);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(600, 120);
        }
    }

    // =========================================================================
    // VISUAL SEPARATED TRACK CONNECTOR
    // =========================================================================
    private static class CamelTrackConnectorWidget extends JPanel {
        public CamelTrackConnectorWidget() {
            setOpaque(false);
            setPreferredSize(new Dimension(20, 20));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0xA0, 0xAE, 0xC0));
            int midX = getWidth() / 2;
            g2.drawLine(midX, 0, midX, getHeight());
            g2.fillPolygon(new int[]{midX - 4, midX + 4, midX}, new int[]{getHeight() - 5, getHeight() - 5, getHeight() - 1}, 3);
            g2.dispose();
        }
    }

    // Frame harness preview loader
    public static void main(String[] args) {
        JFrame frame = new JFrame("Camel Visual Integration Spec Harness");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 650);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(new CamelRouteDesignerPanel());
        frame.setVisible(true);
    }

}
