package org.jwellman.demo.d3ish;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class ProfileBuilderPro extends JFrame {
    
    // UI Components
    private PreviewPanel previewPanel;
    private PalettePanel palettePanel;
    private DefinitionPanel definitionPanel;
    private JColorChooser colorChooser;
    
    private Color currentBrand = new Color(227, 24, 55); // Chiefs Red

    public ProfileBuilderPro() {
        setTitle("Foundation Architecture: Profile Builder");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Prepare for full screen
        
        // --- 1. INITIAL ARCHITECTURE WIRING (Level 3) ---
        FoundationStyleEngine.mapComponent("app.background", "ui.surface");
        FoundationStyleEngine.mapComponent("bar.primary", "ui.action");
        FoundationStyleEngine.mapComponent("text.header", "ui.text");
        FoundationStyleEngine.mapComponent("accent.line", "ui.highlight");

        setLayout(new BorderLayout());

        // --- 2. TOP SECTION: THE FLOW (Left to Right) ---
        JPanel topPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        topPanel.setBackground(new Color(30, 30, 30));
        topPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        previewPanel = new PreviewPanel();      // Left
        definitionPanel = new DefinitionPanel(); // Middle
        palettePanel = new PalettePanel();       // Right

        topPanel.add(previewPanel);
        topPanel.add(definitionPanel);
        topPanel.add(palettePanel);

        // --- 3. BOTTOM SECTION: THE INPUT ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.DARK_GRAY));
        
        colorChooser = new JColorChooser(currentBrand);
        colorChooser.getSelectionModel().addChangeListener(e -> updateEverything(colorChooser.getColor()));
        
        // Remove the default preview panel in the chooser to keep it clean
        colorChooser.setPreviewPanel(new JPanel()); 
        bottomPanel.add(colorChooser, BorderLayout.CENTER);

        add(topPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        updateEverything(currentBrand);
    }

    private void updateEverything(Color brand) {
Color[] scale = PaletteGenerator.generateScale(brand);
        
        // Level 1: Primitives
        FoundationStyleEngine.setPrimitive("p-washout", scale[0]);
        FoundationStyleEngine.setPrimitive("p-muted",   scale[1]);
        FoundationStyleEngine.setPrimitive("p-main",    scale[2]);
        FoundationStyleEngine.setPrimitive("p-deep",    scale[3]);
        FoundationStyleEngine.setPrimitive("p-ink",     scale[4]);

        // Level 2: Semantic Intent Mapping
        FoundationStyleEngine.mapSemantic("ui.action",    "p-main");
        FoundationStyleEngine.mapSemantic("ui.surface",   "p-ink");
        FoundationStyleEngine.mapSemantic("ui.text",      "p-washout");
        FoundationStyleEngine.mapSemantic("ui.highlight", "p-deep");
        FoundationStyleEngine.mapSemantic("ui.muted_semantic", "p-muted"); // For the middle button
        
        // --- REFRESH VISUALS ---
        palettePanel.update(scale);
        definitionPanel.refresh();
        previewPanel.repaint();
    }

    // --- INNER CLASSES FOR VISUALS ---

    private class PreviewPanel extends JPanel {
        public PreviewPanel() {
            setBorder(BorderFactory.createTitledBorder(null, "1. RENDERED PREVIEW", 
                       0, 0, null, Color.WHITE));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. DRAW SURFACE (Dungeon Background)
            g2.setColor(FoundationStyleEngine.getStyle("app.background"));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

            // 2. DRAW HEADER TEXT
            g2.setColor(FoundationStyleEngine.getStyle("text.header"));
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString("FOUNDATION FORENSICS", 30, 40);

            // 3. DRAW PRIMARY BAR (The "Hero" Task)
            int barY = 60;
            g2.setColor(FoundationStyleEngine.getStyle("bar.primary"));
            g2.fillRoundRect(30, barY, getWidth() - 60, 45, 12, 12);
            
            g2.setColor(FoundationStyleEngine.getStyle("app.background")); // Contrast Cutout
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("PRIMARY TASK: 78%", 45, barY + 28);

            // 4. DRAW FLAT UTILITY BUTTONS (Representing Primitives 0, 1, and 3)
            // Let's visualize "Muted", "Highlight", and "Washout" as button states
            int btnY = 130;
            int btnW = (getWidth() - 90) / 3;
            int btnH = 35;

            // Button A: Using 'ui.highlight' (Level 1 Variant 3 - Deep)
            drawFlatButton(g2, 30, btnY, btnW, btnH, "ui.highlight", "RETRY");

            // Button B: Using 'ui.muted' (Level 1 Variant 1 - Muted)
            drawFlatButton(g2, 45 + btnW, btnY, btnW, btnH, "ui.muted_semantic", "IGNORE");

            // Button C: Using 'ui.text' (Level 1 Variant 0 - Washout) 
            // Often used for "ghost" buttons or borders
            drawFlatButton(g2, 60 + (btnW * 2), btnY, btnW, btnH, "ui.text", "INFO");
            
            // 5. DECORATIVE ACCENT LINE (Level 1 Variant 3)
            g2.setColor(FoundationStyleEngine.getStyle("accent.line"));
            g2.fillRect(30, getHeight() - 40, getWidth() - 60, 2);
        }

        private void drawFlatButton(Graphics2D g2, int x, int y, int w, int h, String semanticKey, String label) {
            // Background
            g2.setColor(FoundationStyleEngine.getStyle(semanticKey));
            g2.fillRoundRect(x, y, w, h, 8, 8);
            
            // Label - Dynamically choosing contrast
            // Simple heuristic: if the button is the 'text' color (washout), use the 'background' color for text
            if (semanticKey.equals("ui.text")) {
                g2.setColor(FoundationStyleEngine.getStyle("app.background"));
            } else {
                g2.setColor(FoundationStyleEngine.getStyle("text.header"));
            }
            
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (w - fm.stringWidth(label)) / 2;
            int ty = y + (h + fm.getAscent()) / 2 - 2;
            g2.drawString(label, tx, ty);
        }
    }

    private class DefinitionPanel extends JPanel {
        private final DefaultListModel<String> model = new DefaultListModel<>();
        private final JList<String> list = new JList<>(model);

        public DefinitionPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder(null, "2. SEMANTIC DEFINITIONS", 0, 0, null, Color.WHITE));
            setOpaque(false);
            
            list.setBackground(new Color(45, 45, 45));
            list.setForeground(Color.WHITE);
            list.setFont(new Font("Monospaced", Font.PLAIN, 12));
            add(new JScrollPane(list));
        }

        public void refresh() {
            model.clear();
            model.addElement("ui.action    -> " + toHex(FoundationStyleEngine.getStyle("bar.primary")));
            model.addElement("ui.surface   -> " + toHex(FoundationStyleEngine.getStyle("app.background")));
            model.addElement("ui.text      -> " + toHex(FoundationStyleEngine.getStyle("text.header")));
        }
        
        private String toHex(Color c) {
            return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue()).toUpperCase();
        }
    }

    private class PalettePanel extends JPanel {
        private Color[] currentScale = new Color[0];

        public PalettePanel() {
            setBorder(BorderFactory.createTitledBorder(null, "3. LEVEL 1 PALETTE", 0, 0, null, Color.WHITE));
            setOpaque(false);
        }

        public void update(Color[] scale) {
            this.currentScale = scale;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int blockH = getHeight() / 6;
            for (int i = 0; i < currentScale.length; i++) {
                g.setColor(currentScale[i]);
                g.fillRect(20, 30 + (i * blockH), getWidth() - 40, blockH - 10);
                g.setColor(Color.WHITE);
                g.drawString("Level 1 Variant " + i, 30, 30 + (i * blockH) + 20);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProfileBuilderPro().setVisible(true));
    }

}
