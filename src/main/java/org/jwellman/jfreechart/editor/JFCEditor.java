package org.jwellman.jfreechart.editor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.lang.StringUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.Title;

/**
 * Note:  Unlike traditional classes, we expose most properties
 * and object for use by beanshell, etc.
 * 
 * @author rwellman
 *
 */
public class JFCEditor extends JPanel {

    public JTree tree;
    public JPanel cardPanel;
    public JFreeChart jFreeChart;
    public TitleEditor titleEditor = new TitleEditor();
    public SubtitleEditor subtitleEditor = new SubtitleEditor();
    public CardLayout cardLayout;

    private static class Fonts {
        private static final Font CARD_TITLE = new Font("Arial", Font.BOLD, 24);
        private static final Font CARD_CONTENT = new Font("Arial", Font.PLAIN, 14);
    }

    private static final long serialVersionUID = 1L;

    // Card names for the CardLayout
    protected enum Card {
        Welcome, Chart, Title, Subtitle, Plot, Legend, Border 
    }

    public JFCEditor(JFreeChart jfc) {
        this.jFreeChart = jfc;

        initializeObjects();
        initializeComponents();
        layoutComponents();
        setupTreeListener();
    }

    private void initializeObjects() {
//        objectMap = new HashMap<>();
//        objectMap.put("Settings", new EditableObject("Application Settings", "Configure your application preferences here"));
//        objectMap.put("Profile", new EditableObject("User Profile", "John Doe - Software Developer"));
//        objectMap.put("Documents", new EditableObject("Document Manager", "Manage your documents and files"));
//        objectMap.put("Reports", new EditableObject("Report Generator", "Generate and view system reports"));
//        objectMap.put("Help", new EditableObject("Help System", "Get assistance and documentation"));
    }
    
    private void initializeComponents() {
        // Create the tree with custom tree nodes
        tree = new JTree(new ChartNode(this.jFreeChart)); 
        tree.setRootVisible(true);
        tree.expandRow(0); // Expand root by default

        // Create CardLayout and card panel
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // Add cards to the panel
        cardPanel.add(createWelcomePanel(), Card.Welcome.toString());
        cardPanel.add(createUnderConstruction(), Card.Chart.toString());
        cardPanel.add(titleEditor, Card.Title.toString());
        cardPanel.add(subtitleEditor, Card.Subtitle.toString());
        cardPanel.add(createUnderConstruction(), Card.Plot.toString());
        cardPanel.add(createUnderConstruction(), Card.Legend.toString());
        titleEditor.setJfreechart(this.jFreeChart);

        // Show welcome panel by default
        this.showCard(Card.Welcome);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        // Add tree to west with scroll pane
        JScrollPane treeScrollPane = new JScrollPane(tree);
        treeScrollPane.setPreferredSize(new Dimension(200, 400));
        add(treeScrollPane, BorderLayout.WEST);

        // Add card panel to center
        add(cardPanel, BorderLayout.CENTER);
    }

    private void setupTreeListener() {
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = 
                (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

            if (selectedNode != null) {
                if (selectedNode instanceof SubtitleNode) {
                    SubtitleNode n = (SubtitleNode)selectedNode;
                    subtitleEditor.setSubtitle((Title) n.getUserObject());
                }
                if (selectedNode instanceof LegendNode) {
                    LegendNode n = (LegendNode)selectedNode;
                    subtitleEditor.setSubtitle((Title) n.getUserObject());
                }
                showCard(selectedNode.toString());
            }
        });
    }

    /**
     * Show card/view corresponding to Card enum value.
     * <p>
     * Note: This is called by the internal TreeSelectionListener.
     * 
     * @param card
     */
    public void showCard(String name) {
        // Some nodes contain a description. i.e. "Plot: Category Plot"
        if (name.contains(":")) {
            name = StringUtils.substringBefore(name, ":");
            cardLayout.show(cardPanel, name);
        } else {
            this.showCard(Card.valueOf(name));
        }
    }

    public void showCard(Card card) {
        cardLayout.show(cardPanel, card.toString());
    }

    private JPanel createUnderConstruction() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Under Construction", SwingConstants.CENTER);
        title.setFont(Fonts.CARD_TITLE);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JTextArea content = new JTextArea(
            "This property editor is under construction!\n\n");
        content.setEditable(false);
        content.setWrapStyleWord(true);
        content.setLineWrap(true);
        content.setFont(Fonts.CARD_CONTENT);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(content), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Welcome", SwingConstants.CENTER);
        title.setFont(Fonts.CARD_TITLE);
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JTextArea content = new JTextArea(
            "Welcome to the Object Editor!\n\n" +
            "Select a node from the tree on the left to edit its properties.\n" +
            "Each node has an associated object with editable string properties.\n\n" +
            "Available nodes:\n" +
            "• Settings - Edit application settings\n" +
            "• Profile - Edit user profile information\n" +
            "• Documents - Edit document manager settings\n" +
            "• Reports - Edit report generator settings\n" +
            "• Help - Edit help system information"
        );
        content.setEditable(false);
        content.setWrapStyleWord(true);
        content.setLineWrap(true);
        content.setFont(Fonts.CARD_CONTENT);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(content), BorderLayout.CENTER);

        return panel;
    }

}
