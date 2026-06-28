package org.katacode.pipeline.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.katacode.pipeline.engine.ComponentFactory;
import org.katacode.pipeline.engine.PipelineContext;
import org.katacode.pipeline.engine.PipelineStep;
import org.katacode.pipeline.scenario.TransactionExchange;

/**
 * A reusable, self-contained workspace panel for designing functional data pipelines.
 * Integrated directly with the functional PipelineContext engine and dynamic canvas rendering.
 */
@SuppressWarnings("serial")
public class PipelineDesignerPanel extends JPanel {

    private JList<String> paletteList;
    private PipelineCanvasPanel executionCanvasPanel;
    private PropertiesInspectorPanel inspectorPanel;

    // Core Engine Context instead of flat string models
    private PipelineContext pipelineContext;

    public PipelineDesignerPanel() {
        // Enforce an explicit root layout for the compound workspace
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(0xE1, 0xE4, 0xE6)); // Clean light theme core workspace frame

        initEngineContext();
        initUI();
    }

    /**
     * Rehydrates our explicit runtime state with actual functional steps 
     * instead of text strings.
     */
    private void initEngineContext() {
        pipelineContext = new PipelineContext();

        // Inject our Scenario A steps directly to seed the layout wrapper
        pipelineContext.addStep(new PipelineStep(
            "step-01", "DB Reader", "None", "ResultSet",
            (org.katacode.pipeline.engine.Message<TransactionExchange> msg) -> msg
        ));
        pipelineContext.addStep(new PipelineStep(
            "step-02", "Regex Filter", "ResultSet", "ResultSet",
            ComponentFactory.createFilter((TransactionExchange tx) -> !tx.getAccountCode().equalsIgnoreCase("TEST_ACC"))
        ));
        pipelineContext.addStep(new PipelineStep(
            "step-03", "JSON Transformer", "ResultSet", "JSON",
            ComponentFactory.createTransformer((TransactionExchange tx) -> "{}")
        ));
        pipelineContext.addStep(new PipelineStep(
            "step-04", "REST Dispatcher", "JSON", "None",
            (org.katacode.pipeline.engine.Message<String> msg) -> msg
        ));
    }

    private void initUI() {
        // 1. Top Control Bar
        add(createTopToolBar(), BorderLayout.NORTH);

        // 2. Component Palette (Left Column)
        JPanel paletteWrapper = createSectionPanel("Component Palette", createPaletteComponent());
        paletteWrapper.setMinimumSize(new Dimension(220, 0));

        // 3. Central Pipeline Canvas (Center Column)
        executionCanvasPanel = new PipelineCanvasPanel(pipelineContext);
        JScrollPane canvasScroll = new JScrollPane(executionCanvasPanel);
        canvasScroll.setBorder(null);
        canvasScroll.getVerticalScrollBar().setUnitIncrement(16); // Fluid wheel scroll

        JPanel canvasWrapper = createSectionPanel("Execution Flow Sequence", canvasScroll);
        canvasWrapper.setMinimumSize(new Dimension(500, 0));

        // 4. Properties Inspector (Right Column)
        this.inspectorPanel = new PropertiesInspectorPanel();
        JPanel inspectorWrapper = createSectionPanel("Properties Inspector", inspectorPanel);
        inspectorWrapper.setMinimumSize(new Dimension(280, 0));
        updateInspector(null);

        // Right Column Inspector Panel wrapped in a standard JScrollPane 
        // to handle massive parameter lists safely without truncation
        JScrollPane inspectorScroll = new JScrollPane(inspectorPanel);
        inspectorScroll.setBorder(null);
        inspectorScroll.getVerticalScrollBar().setUnitIncrement(16);
        inspectorScroll.setPreferredSize(new Dimension(320, 0));
        
        // Wire up Split Panes for flexible user scaling
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, canvasWrapper, inspectorWrapper);
        rightSplit.setResizeWeight(0.65); // Canvas gets majority of right side scaling
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
        toolBar.setBackground(new Color(0xEE, 0xF1, 0xF4));
        
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
        paletteList.setBackground(Color.WHITE);
        paletteList.setForeground(new Color(0x22, 0x22, 0x22));
        
        // Setup listener to append mock steps to our active canvas engine state on double-click
        paletteList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selected = paletteList.getSelectedValue();
                    if (selected != null) {
                        // Dynamically append a standard generic step into our engine context
                        String cleanName = selected.replaceAll("\\[.*?\\] ", "");
                        pipelineContext.addStep(new PipelineStep(
                            "step-" + (pipelineContext.getSteps().size() + 1),
                            cleanName, "JSON", "JSON",
                            (org.katacode.pipeline.engine.Message<Object> msg) -> msg
                        ));
                        // Force UI layout synchronization
                        executionCanvasPanel.refreshCanvas();
                    }
                }
            }
        });

        return new JScrollPane(paletteList);
    }

    /**
     * Centralized coordination endpoint invoked whenever a native 
     * JToggleButton card selection model event is triggered.
     */
    public void handleCardSelection(PipelineStep selectedStep) {
        if (selectedStep != null) {
            // Re-hydrate the property forms instantly based on the card's data dictionary
            inspectorPanel.inspect(selectedStep);
        } else {
            inspectorPanel.inspect(null);
        }
    }

    /**
     * Updated properties form context target binding.
     * Accessible by selecting individual custom cards directly from our canvas panel.
     */
    public void updateInspector(PipelineStep targetStep) {
        inspectorPanel.removeAll();
        
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        
        JLabel nameLabel = new JLabel("Component ID:");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        fieldsPanel.add(nameLabel, gbc);
        
        gbc.gridy++;
        gbc.insets = new java.awt.Insets(2, 0, 12, 0);
        
        String idText = (targetStep != null) ? targetStep.getId() : "No block selected.";
        String configLabelText = (targetStep != null) ? "Configuration Param (" + targetStep.getInputContract() + " -> " + targetStep.getOutputContract() + "):" : "Configuration Param:";
        
        fieldsPanel.add(new JTextField(idText), gbc);
        
        gbc.gridy++;
        gbc.insets = new java.awt.Insets(0, 0, 2, 0);
        fieldsPanel.add(new JLabel(configLabelText), gbc);
        
        gbc.gridy++;
        fieldsPanel.add(new JTextField((targetStep != null) ? targetStep.getComponentName() : ""), gbc);
        
        inspectorPanel.add(fieldsPanel, BorderLayout.NORTH);
        inspectorPanel.revalidate();
        inspectorPanel.repaint();
    }

    private JPanel createSectionPanel(String title, JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(4, 4, 4, 4));
        wrapper.setBackground(new Color(0xE1, 0xE4, 0xE6));
        
        JLabel headerLabel = new JLabel(title);
        headerLabel.setBorder(new EmptyBorder(4, 6, 6, 6));
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 12f));
        headerLabel.setForeground(new Color(0x33, 0x33, 0x33));
        
        wrapper.add(headerLabel, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JComponent createBottomStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(new EmptyBorder(4, 8, 4, 8));
        bar.setBackground(new Color(0xEE, 0xF1, 0xF4));
        
        JLabel lblStatus = new JLabel("Status: Idle | Configuration Engine Validated (Java 8 Baseline)");
        lblStatus.setForeground(new Color(0x55, 0x55, 0x55));
        bar.add(lblStatus, BorderLayout.WEST);
        return bar;
    }

    // --- Frame Harness Execution Context ---
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pipeline Composer - Component Harness");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1240, 820);
            frame.setLocationRelativeTo(null);
            
            PipelineDesignerPanel designerPanel = new PipelineDesignerPanel();
            frame.setContentPane(designerPanel);
            
            frame.setVisible(true);
        });
    }

}
