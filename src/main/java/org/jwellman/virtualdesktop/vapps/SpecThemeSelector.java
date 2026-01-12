package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import org.jwellman.dsp.DSP;
import org.jwellman.virtualdesktop.theme.IconSize;
import org.jwellman.virtualdesktop.theme.JsonTheme;
import org.jwellman.virtualdesktop.theme.Theme;
import org.jwellman.virtualdesktop.theme.ThemeManager;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Theme selector vapp for VirtualDesktop.
 * <p>
 * Allows users to:
 * <ul>
 *   <li>Browse available themes from config/themes/</li>
 *   <li>View theme details (name, description, icon sizes)</li>
 *   <li>Preview theme with sample icons</li>
 *   <li>Select a theme and save to preferences.json</li>
 * </ul>
 * </p>
 * <p>
 * Phase 1: Theme changes require application restart.
 * After selecting a theme, user is prompted to restart the application.
 * </p>
 *
 * @author Rick Wellman
 */
public class SpecThemeSelector extends VirtualAppSpec {

    private JList<ThemeInfo> themeList;
    private JPanel previewPanel;
    private JButton applyButton;
    private JTextArea detailsArea;
    private DefaultListModel<ThemeInfo> themeListModel;

    private ThemeInfo currentSelection;

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Holds theme metadata for display in the list.
     */
    private static class ThemeInfo {
        String name;
        String displayName;
        String description;
        String version;
        String extendsTheme;

        @Override
        public String toString() {
            return displayName != null ? displayName : name;
        }
    }

    public SpecThemeSelector() {
        super();

        this.setTitle("Theme Selector");
        this.setContent(createUI());
        // this.setPreferredSize(new Dimension(700, 500));
    }

    private JPanel createUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create split pane: theme list on left, details on right
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createThemeListPanel());
        splitPane.setRightComponent(createDetailsPanel());
        splitPane.setDividerLocation(200);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        // Load themes
        loadThemes();

        return mainPanel;
    }

    private JPanel createThemeListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Available Themes"));

        themeListModel = new DefaultListModel<>();
        themeList = new JList<>(themeListModel);
        themeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        themeList.setCellRenderer(new ThemeListCellRenderer());

        themeList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ThemeInfo selected = themeList.getSelectedValue();
                if (selected != null) {
                    currentSelection = selected;
                    updateDetailsPanel(selected);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(themeList);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Details text area
        detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setLineWrap(true);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailsArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Theme Details"));

        // Preview panel
        previewPanel = new JPanel();
        previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));
        previewPanel.setBorder(BorderFactory.createTitledBorder("Icon Size Preview"));
        previewPanel.setPreferredSize(new Dimension(0, 200));

        JScrollPane previewScroll = new JScrollPane(previewPanel);

        // Split vertically: details on top, preview on bottom
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setTopComponent(detailsScroll);
        verticalSplit.setBottomComponent(previewScroll);
        verticalSplit.setDividerLocation(200);

        panel.add(verticalSplit, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        applyButton = new JButton("Apply Theme");
        applyButton.setEnabled(false);
        applyButton.addActionListener(e -> applyTheme());

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadThemes());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> closeWindow());

        panel.add(refreshButton);
        panel.add(applyButton);
        panel.add(closeButton);

        return panel;
    }

    private void loadThemes() {
        themeListModel.clear();
        List<String> themeNames = ThemeManager.getInstance().getAvailableThemes();

        for (String themeName : themeNames) {
            try {
                ThemeInfo info = loadThemeInfo(themeName);
                themeListModel.addElement(info);
            } catch (IOException ex) {
                System.err.println("Failed to load theme info: " + themeName + " - " + ex.getMessage());
            }
        }

        // Select current theme
        String currentThemeName = getCurrentThemeName();
        for (int i = 0; i < themeListModel.getSize(); i++) {
            ThemeInfo info = themeListModel.getElementAt(i);
            if (info.name.equals(currentThemeName)) {
                themeList.setSelectedIndex(i);
                break;
            }
        }
    }

    private ThemeInfo loadThemeInfo(String themeName) throws IOException {
        File themeFile = new File("config/themes", themeName + ".json");
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(themeFile);

        ThemeInfo info = new ThemeInfo();
        info.name = root.has("name") ? root.get("name").asText() : themeName;
        info.displayName = root.has("displayName") ? root.get("displayName").asText() : info.name;
        info.description = root.has("description") ? root.get("description").asText() : "";
        info.version = root.has("version") ? root.get("version").asText() : "1.0.0";
        info.extendsTheme = root.has("extends") ? root.get("extends").asText() : null;

        return info;
    }

    private String getCurrentThemeName() {
        Theme currentTheme = ThemeManager.getInstance().getCurrentTheme();
        if (currentTheme != null) {
            return currentTheme.getName();
        }
        return "default";
    }

    private void updateDetailsPanel(ThemeInfo info) {
        // Update details text
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(info.displayName).append("\n\n");
        details.append("Description:\n").append(info.description).append("\n\n");
        details.append("Version: ").append(info.version).append("\n");

        if (info.extendsTheme != null) {
            details.append("Extends: ").append(info.extendsTheme).append("\n");
        }

        details.append("\n--- Icon Sizes ---\n");
        try {
            // Load theme to get actual sizes
            File themeFile = new File("config/themes", info.name + ".json");
            JsonTheme theme = JsonTheme.loadFromFile(themeFile);

            details.append("SMALL:   ").append(theme.getIconSize(IconSize.SMALL)).append("px\n");
            details.append("MENU:    ").append(theme.getIconSize(IconSize.MENU)).append("px\n");
            details.append("TOOLBAR: ").append(theme.getIconSize(IconSize.TOOLBAR)).append("px\n");
            details.append("LARGE:   ").append(theme.getIconSize(IconSize.LARGE)).append("px\n");
            details.append("XLARGE:  ").append(theme.getIconSize(IconSize.XLARGE)).append("px\n");

            // Update preview panel
            updatePreviewPanel(theme);

        } catch (IOException ex) {
            details.append("(Unable to load size information)\n");
            System.err.println("Failed to load theme details: " + ex.getMessage());
        }

        detailsArea.setText(details.toString());
        detailsArea.setCaretPosition(0);

        // Enable apply button if different from current theme
        applyButton.setEnabled(!info.name.equals(getCurrentThemeName()));
    }

    private void updatePreviewPanel(Theme theme) {
        previewPanel.removeAll();
        previewPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Try to load a sample icon for preview (home156 is always registered)
        final String sampleIconName = "home156";

        IconSize[] sizes = {IconSize.SMALL, IconSize.MENU, IconSize.TOOLBAR, IconSize.LARGE, IconSize.XLARGE};
        String[] sizeLabels = {"SMALL", "MENU", "TOOLBAR", "LARGE", "XLARGE"};

        int row = 0;
        for (int i = 0; i < sizes.length; i++) {
            IconSize size = sizes[i];
            int pixels = theme.getIconSize(size);

            // Label column
            gbc.gridx = 0;
            gbc.gridy = row;
            JLabel sizeLabel = new JLabel(sizeLabels[i] + ":");
            sizeLabel.setFont(new Font("Dialog", Font.BOLD, 12));
            previewPanel.add(sizeLabel, gbc);

            // Pixel size column
            gbc.gridx = 1;
            JLabel pixelLabel = new JLabel(pixels + "px");
            previewPanel.add(pixelLabel, gbc);

            // Icon preview column
            gbc.gridx = 2;
            Icon icon = DSP.Icons.getIcon(sampleIconName + "-" + size.toThemeKey());
            if (icon != null) {
                JLabel iconLabel = new JLabel(icon);
                iconLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
                previewPanel.add(iconLabel, gbc);
            } else {
                JLabel noIconLabel = new JLabel("(no preview)");
                noIconLabel.setForeground(Color.GRAY);
                previewPanel.add(noIconLabel, gbc);
            }

            row++;
        }

        // Add glue to push everything to the top
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weighty = 1.0;
        previewPanel.add(Box.createVerticalGlue(), gbc);

        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private void applyTheme() {
        if (currentSelection == null) {
            return;
        }

        // Confirm with user
        int result = JOptionPane.showConfirmDialog(
            this.getContent(),
            "Apply theme '" + currentSelection.displayName + "'?\n\n" +
            "The application must be restarted for the theme to take effect.",
            "Apply Theme",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            // Save theme preference
            ThemeManager.getInstance().savePreferredTheme(currentSelection.name);

            // Show restart message
            JOptionPane.showMessageDialog(
                this.getContent(),
                "Theme '" + currentSelection.displayName + "' has been saved.\n\n" +
                "Please restart VirtualDesktop for the theme to take effect.",
                "Theme Applied",
                JOptionPane.INFORMATION_MESSAGE
            );

            // Disable apply button
            applyButton.setEnabled(false);
        }
    }

    private void closeWindow() {
        // Find the internal frame and close it
        javax.swing.JInternalFrame frame = (javax.swing.JInternalFrame)
            javax.swing.SwingUtilities.getAncestorOfClass(javax.swing.JInternalFrame.class, this.getContent());
        if (frame != null) {
            frame.setVisible(false);
        }
    }

    /**
     * Custom cell renderer for theme list.
     */
    @SuppressWarnings("serial")
    private class ThemeListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

            if (value instanceof ThemeInfo) {
                ThemeInfo info = (ThemeInfo) value;
                label.setText(info.displayName);

                // Highlight current theme
                if (info.name.equals(getCurrentThemeName())) {
                    label.setText("• " + info.displayName + " (current)");
                    if (!isSelected) {
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    }
                }
            }

            return label;
        }
    }
}
