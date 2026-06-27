package org.katacode.pipeline.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Vector;

/**
 * A reusable, self-contained workspace panel for designing functional data pipelines.
 * Designed to be embedded into any top-level window or custom layout frame.
 */
@SuppressWarnings("serial")
public class PipelineDesignerPanel_v01 extends JPanel {

    private JList<String> paletteList;
    private JList<String> pipelineSequenceList;
    private JPanel inspectorPanel;
    
    // Core state data models
    private DefaultListModel<String> pipelineModel;

    public PipelineDesignerPanel_v01() {
        // Enforce an explicit root layout for the compound workspace
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(0x2B, 0x2B, 0x2B)); // Subtle dark mode core background

        initModels();
        initUI();
    }

    private void initModels() {
        pipelineModel = new DefaultListModel<>();
        // Mocking an initial pipeline state
        pipelineModel.addElement("DB Reader (Source: Analytics_DB)");
        pipelineModel.addElement("Regex Filter (Pattern: ^[0-9]+)");
        pipelineModel.addElement("JSON Transformer");
        pipelineModel.addElement("REST Dispatcher (Endpoint: /api/v2/upload)");
    }

    private void initUI() {
        // 1. Top Control Bar
        add(createTopToolBar(), BorderLayout.NORTH);

        // 2. Component Palette (Left Column)
        JPanel paletteWrapper = createSectionPanel("Component Palette", createPaletteComponent());
        paletteWrapper.setMinimumSize(new Dimension(200, 0));

        // 3. Central Pipeline Canvas (Center Column)
        JPanel canvasWrapper = createSectionPanel("Execution Flow Sequence", createPipelineCanvasComponent());
        canvasWrapper.setMinimumSize(new Dimension(400, 0));

        // 4. Properties Inspector (Right Column)
        inspectorPanel = new JPanel(new BorderLayout());
        JPanel inspectorWrapper = createSectionPanel("Properties Inspector", inspectorPanel);
        inspectorWrapper.setMinimumSize(new Dimension(250, 0));
        updateInspector("Select a block to configure parameters.");

        // Wire up Split Panes for flexible user scaling
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvasWrapper, inspectorWrapper);
        rightSplit.setResizeWeight(0.7); // Canvas gets majority of right side scaling
        rightSplit.setBorder(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, paletteWrapper, rightSplit);
        mainSplit.setResizeWeight(0.2); // Palette gets smaller share of initial space
        mainSplit.setBorder(null);

        add(mainSplit, BorderLayout.CENTER);
        
        // 5. Bottom Console/Status Bar
        add(createBottomStatusBar(), BorderLayout.SOUTH);
    }

    private JComponent createTopToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        JButton btnLoad = new JButton("Load Blueprint");
        JButton btnSave = new JButton("Save JSON");
        JButton btnRun = new JButton("Execute Pipeline");
        
        toolBar.add(btnLoad);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(btnSave);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(btnRun);
        
        return toolBar;
    }

    private JComponent createPaletteComponent() {
        Vector<String> blocks = new Vector<>();
        blocks.add("[Source] Database Reader");
        blocks.add("[Source] File Poller");
        blocks.add("[Filter] Regex Evaluator");
        blocks.add("[Filter] Null Field Dropper");
        blocks.add("[Transform] Template Mapping");
        blocks.add("[Transform] JSON Converter");
        blocks.add("[Sink] REST Endpoint");
        blocks.add("[Sink] File Writer");

        paletteList = new JList<>(blocks);
        paletteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paletteList.setBorder(new EmptyBorder(4, 4, 4, 4));
        
        // Setup listener to add block to canvas on double-click
        paletteList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selected = paletteList.getSelectedValue();
                    if (selected != null) {
                        pipelineModel.addElement(selected + " (Config Required)");
                    }
                }
            }
        });

        return new JScrollPane(paletteList);
    }

    private JComponent createPipelineCanvasComponent() {
        pipelineSequenceList = new JList<>(pipelineModel);
        pipelineSequenceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Custom Cell Renderer hook for the "Eye Candy" cards
        pipelineSequenceList.setCellRenderer(new PipelineCardCellRenderer());
        pipelineSequenceList.setFixedCellHeight(70); // Generous padding for component cards
        
        // Update inspector on block selection change
        pipelineSequenceList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedBlock = pipelineSequenceList.getSelectedValue();
                updateInspector(selectedBlock);
            }
        });

        JScrollPane scrollPane = new JScrollPane(pipelineSequenceList);
        scrollPane.setBorder(null);
        return scrollPane;
    }

    private void updateInspector(String targetBlock) {
        inspectorPanel.removeAll();
        if (targetBlock == null) {
            targetBlock = "No block selected.";
        }
        
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        
        JLabel nameLabel = new JLabel("Component ID:");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        fieldsPanel.add(nameLabel, gbc);
        
        gbc.gridy++;
        gbc.insets = new java.awt.Insets(2, 0, 12, 0);
        fieldsPanel.add(new JTextField(targetBlock), gbc);
        
        gbc.gridy++;
        gbc.insets = new java.awt.Insets(0, 0, 2, 0);
        fieldsPanel.add(new JLabel("Configuration Param (e.g. URI / Pattern):"), gbc);
        
        gbc.gridy++;
        fieldsPanel.add(new JTextField(), gbc);
        
        inspectorPanel.add(fieldsPanel, BorderLayout.NORTH);
        inspectorPanel.revalidate();
        inspectorPanel.repaint();
    }

    private JPanel createSectionPanel(String title, JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(4, 4, 4, 4));
        
        JLabel headerLabel = new JLabel(title);
        headerLabel.setBorder(new EmptyBorder(4, 6, 6, 6));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 12f));
        
        wrapper.add(headerLabel, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JComponent createBottomStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(new EmptyBorder(4, 8, 4, 8));
        bar.add(new JLabel("Status: Idle | Configuration Engine Validated"), BorderLayout.WEST);
        return bar;
    }

    // --- Custom Cell Renderer for Pipeline Steps ---
    private static class PipelineCardCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(6, 10, 6, 10),
                BorderFactory.createLineBorder(isSelected ? Color.ORANGE : Color.LIGHT_GRAY, 1, true)
            ));
            card.setBackground(isSelected ? new Color(0x3A, 0x3A, 0x3A) : new Color(0x40, 0x40, 0x40));

            JLabel title = new JLabel(value.toString());
            title.setForeground(Color.WHITE);
            title.setFont(title.getFont().deriveFont(Font.BOLD, 13f));
            
            JLabel stepIndex = new JLabel(String.format(" Step %02d ", index + 1));
            stepIndex.setOpaque(true);
            stepIndex.setBackground(new Color(0x55, 0x55, 0x55));
            stepIndex.setForeground(Color.CYAN);
            
            card.add(stepIndex, BorderLayout.WEST);
            card.add(title, BorderLayout.CENTER);

            return card;
        }
    }

    // --- Frame Harness Execution Context ---
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pipeline Composer - Component Harness");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);
            
            // Instantiating our newly decoupled custom component
            PipelineDesignerPanel_v01 designerPanel = new PipelineDesignerPanel_v01();
            frame.setContentPane(designerPanel);
            
            frame.setVisible(true);
        });
    }

}
