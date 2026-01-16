package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A tool that opens local HTML files in the system's default browser.
 *
 * This tool displays a placeholder panel while the actual HTML content
 * is opened externally in the user's browser. Useful for:
 * - HTML documentation
 * - Web-based tools and utilities
 * - Local web applications
 *
 * @author rwellman
 */
public class SpecHtmlViewer extends VirtualAppSpec implements LaunchAware, Configurable {

    private String htmlFilePath = "docs/index.html";

    public SpecHtmlViewer() {
        super();

        this.setTitle("HTML Viewer");
        this.setContent(createPlaceholderPanel());
    }

    public SpecHtmlViewer(String title, String htmlFilePath) {
        super();

        this.htmlFilePath = htmlFilePath;
        this.setTitle(title);
        this.setContent(createPlaceholderPanel());
    }

    /**
     * Sets the path to the HTML file to open.
     * @param path relative or absolute path to HTML file
     */
    public void setHtmlFile(String path) {
        this.htmlFilePath = path;
    }

    /**
     * Gets the configured HTML file path.
     * @return the HTML file path
     */
    public String getHtmlFile() {
        return this.htmlFilePath;
    }

    @Override
    public void configure(Map<String, String> attrs) {
        if (attrs.containsKey("htmlFile")) {
            this.htmlFilePath = attrs.get("htmlFile");
            // Update the panel to show the configured file path
            this.setContent(createPlaceholderPanel());
        }
    }

    @Override
    public void launch() throws Exception {
        File htmlFile = new File(htmlFilePath);

        if (!Desktop.isDesktopSupported()) {
            System.err.println("Desktop API not supported on this platform");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            System.err.println("Browse action not supported on this platform");
            return;
        }

        if (htmlFile.exists()) {
            desktop.browse(htmlFile.toURI());
            System.out.println("Opened in browser: " + htmlFile.getAbsolutePath());
        } else {
            System.err.println("HTML file not found: " + htmlFile.getAbsolutePath());
        }
    }

    private JPanel createPlaceholderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Main message
        String message = String.format(
            "<html><center>" +
            "<b>HTML Viewer</b><br><br>" +
            "File: <i>%s</i><br><br>" +
            "Content opened in external browser." +
            "</center></html>",
            htmlFilePath
        );

        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));

        // Re-open link
        JLabel reopenLink = new JLabel(
            "<html><center><br>If you closed the browser window,<br>" +
            "<u>click here</u> to re-open.</center></html>",
            SwingConstants.CENTER
        );
        reopenLink.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        reopenLink.setForeground(new Color(0, 102, 204)); // Link blue
        reopenLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reopenLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    launch();
                } catch (Exception ex) {
                    System.err.println("Failed to re-open browser: " + ex.getMessage());
                }
            }
        });

        // Center panel with vertical layout
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(Box.createVerticalGlue());
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        reopenLink.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        centerPanel.add(label);
        centerPanel.add(reopenLink);
        centerPanel.add(Box.createVerticalGlue());

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

}
