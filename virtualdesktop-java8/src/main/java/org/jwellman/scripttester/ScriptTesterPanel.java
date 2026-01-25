package org.jwellman.scripttester;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jwellman.virtualdesktop.bsh.BeanShellService;

/**
 * Script Tester Tool - Step-by-step BeanShell script execution with visual feedback.
 *
 * <p>A tool for debugging and stepping through BeanShell scripts one line at a time.
 * Provides visual feedback for completed, current, pending, and error states.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Step button: Execute one line at a time (retries on error)</li>
 *   <li>Back button: Go back one line without undoing</li>
 *   <li>Skip button: Skip current line without executing</li>
 *   <li>Edit button: Edit current line in a dialog</li>
 *   <li>Reset button: Restart execution from beginning</li>
 *   <li>Load button: Load a script from file</li>
 *   <li>Paste button: Load script from system clipboard</li>
 *   <li>Swap View button: Toggle between table and two-panel views</li>
 * </ul>
 *
 * @author Rick Wellman
 */
public class ScriptTesterPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private final List<ScriptLine> lines = new ArrayList<ScriptLine>();
    private int currentIndex = 0;

    private ScriptTableModel tableModel;
    private JTable table;
    private JPanel viewPanel;
    private boolean isTableView = true;

    // Two-panel view components
    private JTextArea remainingArea;
    private JTextArea executedArea;

    // Output listener for messages
    private OutputListener outputListener;

    /**
     * Interface for receiving output messages (print statements).
     */
    public interface OutputListener {
        void print(String message);
    }

    /**
     * Create a new ScriptTesterPanel.
     */
    public ScriptTesterPanel() {
        super(new BorderLayout());
        buildUI();
    }

    /**
     * Set the output listener for print messages.
     *
     * @param listener the listener to receive messages
     */
    public void setOutputListener(OutputListener listener) {
        this.outputListener = listener;
    }

    /**
     * Print a message to the output listener (or System.out if none set).
     */
    private void print(String message) {
        if (outputListener != null) {
            outputListener.print(message);
        } else {
            System.out.println(message);
        }
    }

    /**
     * Build the UI components.
     */
    private void buildUI() {
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        buttonPanel.add(createButton("Step"));
        buttonPanel.add(createButton("Back"));
        buttonPanel.add(createButton("Skip"));
        buttonPanel.add(createButton("Edit"));
        buttonPanel.add(createButton("Reset"));
        buttonPanel.add(createButton("Load"));
        buttonPanel.add(createButton("Paste"));
        buttonPanel.add(createButton("Swap View"));

        add(buttonPanel, BorderLayout.NORTH);

        // Create initial view (table view)
        viewPanel = createTableView();
        add(viewPanel, BorderLayout.CENTER);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.addActionListener(this);
        return button;
    }

    /**
     * Parse script text into ScriptLine objects.
     * Skips empty lines and comment-only lines.
     */
    private void parseScript(String scriptText) {
        lines.clear();
        currentIndex = 0;

        String[] rawLines = scriptText.split("\n");
        int lineNum = 0;

        for (int i = 0; i < rawLines.length; i++) {
            String trimmed = rawLines[i].trim();

            // Skip empty lines and comment-only lines
            if (trimmed.length() == 0) continue;
            if (trimmed.startsWith("//")) continue;

            lineNum++;
            lines.add(new ScriptLine(lineNum, rawLines[i]));
        }

        print("Loaded " + lines.size() + " lines.");
    }

    /**
     * Reset all lines to pending state.
     */
    private void resetExecution() {
        currentIndex = 0;
        for (ScriptLine line : lines) {
            line.setStatus(Status.PENDING);
            line.setErrorMessage(null);
        }
        refreshView();
        print("Execution reset.");
    }

    /**
     * Execute the current line and advance.
     */
    private void stepExecution() {
        if (currentIndex >= lines.size()) {
            print("Script complete.");
            return;
        }

        ScriptLine line = lines.get(currentIndex);
        line.setStatus(Status.CURRENT);
        refreshView();

        try {
            // Use the shared BeanShell interpreter
            BeanShellService.get().eval(line.getText());
            line.setStatus(Status.COMPLETED);
            // Only advance on success
            currentIndex++;
        } catch (Exception e) {
            line.setStatus(Status.ERROR);
            line.setErrorMessage(e.getMessage());
            print("Error at line " + line.getLineNumber() + ": " + e.getMessage());
            // Don't advance on error - allows retry with Step or skip with Skip
        }

        refreshView();

        // Scroll to show current line in table
        if (isTableView && table != null && currentIndex < lines.size()) {
            table.scrollRectToVisible(table.getCellRect(currentIndex, 0, true));
        }
    }

    /**
     * Go back one line (not an undo - just repositions to previous line).
     */
    private void backStep() {
        if (currentIndex <= 0) {
            print("Already at the beginning.");
            return;
        }

        // Move back one line
        currentIndex--;

        // Reset the current line to current status
        ScriptLine line = lines.get(currentIndex);
        line.setStatus(Status.CURRENT);
        line.setErrorMessage(null);
        refreshView();
        print("Moved back to line " + line.getLineNumber());

        // Scroll to show current line in table
        if (isTableView && table != null) {
            table.scrollRectToVisible(table.getCellRect(currentIndex, 0, true));
        }
    }

    /**
     * Skip the current line without executing.
     */
    private void skipLine() {
        if (currentIndex >= lines.size()) {
            print("No line to skip.");
            return;
        }

        ScriptLine line = lines.get(currentIndex);
        line.setStatus(Status.SKIPPED);
        currentIndex++;
        refreshView();
        print("Skipped line " + line.getLineNumber());
    }

    /**
     * Edit the current line in a dialog.
     */
    private void editCurrentLine() {
        if (currentIndex >= lines.size()) {
            print("No line to edit.");
            return;
        }

        ScriptLine line = lines.get(currentIndex);

        // Create edit dialog
        JTextArea editArea = new JTextArea(3, 50);
        editArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        editArea.setText(line.getText());
        editArea.setLineWrap(true);
        editArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(editArea);
        scrollPane.setPreferredSize(new Dimension(500, 80));

        String title = "Edit Line " + line.getLineNumber();
        int result = JOptionPane.showConfirmDialog(
            this,
            scrollPane,
            title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String newText = editArea.getText().trim();
            if (newText.length() > 0) {
                line.setText(newText);
                line.setStatus(Status.PENDING);
                line.setErrorMessage(null);
                refreshView();
                print("Line " + line.getLineNumber() + " updated.");
            }
        }
    }

    /**
     * Refresh the current view.
     */
    private void refreshView() {
        if (isTableView) {
            if (tableModel != null) {
                tableModel.fireDataChanged();
            }
        } else {
            refreshTwoPanelView();
        }
    }

    /**
     * Refresh the two-panel view by rebuilding text areas.
     */
    private void refreshTwoPanelView() {
        if (remainingArea == null || executedArea == null) return;

        StringBuilder executed = new StringBuilder();
        StringBuilder remaining = new StringBuilder();

        for (ScriptLine line : lines) {
            String prefix = "";
            if (Status.ERROR.equals(line.getStatus())) {
                prefix = "[ERROR] ";
            }
            if (Status.COMPLETED.equals(line.getStatus()) || Status.ERROR.equals(line.getStatus())) {
                executed.append(prefix).append(line.getText()).append("\n");
            } else if (Status.CURRENT.equals(line.getStatus())) {
                remaining.append(">>> ").append(line.getText()).append("\n");
            } else {
                remaining.append(line.getText()).append("\n");
            }
        }

        executedArea.setText(executed.toString());
        remainingArea.setText(remaining.toString());
    }

    /**
     * Create the table view panel.
     */
    private JPanel createTableView() {
        tableModel = new ScriptTableModel(lines);
        table = new JTable(tableModel);

        // Set up column renderers and widths
        TableColumnModel columnModel = table.getColumnModel();

        // Status column
        TableColumn statusCol = columnModel.getColumn(0);
        statusCol.setPreferredWidth(30);
        statusCol.setMaxWidth(40);
        statusCol.setCellRenderer(new StatusCellRenderer());

        // Line number column
        TableColumn lineNumCol = columnModel.getColumn(1);
        lineNumCol.setPreferredWidth(40);
        lineNumCol.setMaxWidth(60);
        lineNumCol.setCellRenderer(new LineNumberCellRenderer());

        // Code column
        TableColumn codeCol = columnModel.getColumn(2);
        codeCol.setCellRenderer(new CodeCellRenderer());

        // Table settings
        table.setRowHeight(20);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.setFocusable(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    /**
     * Create the two-panel view.
     */
    private JPanel createTwoPanelView() {
        // Remaining (pending) area - top
        remainingArea = new JTextArea(10, 60);
        remainingArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        remainingArea.setEditable(true);
        JScrollPane remainingScroll = new JScrollPane(remainingArea);
        remainingScroll.setBorder(BorderFactory.createTitledBorder("Remaining"));

        // Executed area - bottom
        executedArea = new JTextArea(10, 60);
        executedArea.setFont(new Font(Font.MONOSPACED, Font.ITALIC, 12));
        executedArea.setEditable(false);
        executedArea.setBackground(new Color(230, 230, 230));
        JScrollPane executedScroll = new JScrollPane(executedArea);
        executedScroll.setBorder(BorderFactory.createTitledBorder("Executed"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            remainingScroll, executedScroll);
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0.5);

        JPanel twoPanelPanel = new JPanel(new BorderLayout());
        twoPanelPanel.add(splitPane, BorderLayout.CENTER);

        refreshTwoPanelView();

        return twoPanelPanel;
    }

    /**
     * Swap between table view and two-panel view.
     */
    private void swapView() {
        remove(viewPanel);

        if (isTableView) {
            viewPanel = createTwoPanelView();
            isTableView = false;
        } else {
            viewPanel = createTableView();
            isTableView = true;
        }

        add(viewPanel, BorderLayout.CENTER);
        revalidate();
        repaint();

        refreshView();
    }

    /**
     * Load a script from file.
     */
    private void loadScript() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select BeanShell Script");
        String[] extensions = new String[] {"bsh", "java", "txt"};
        chooser.setFileFilter(new FileNameExtensionFilter("BeanShell Scripts", extensions));

        // Start in current directory
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                parseScript(sb.toString());
                refreshView();
                print("Loaded: " + file.getName());
            } catch (IOException e) {
                print("Error loading file: " + e.getMessage());
            }
        }
    }

    /**
     * Load script from text (for programmatic use).
     *
     * @param scriptText the script text to load
     */
    public void loadScriptText(String scriptText) {
        parseScript(scriptText);
        refreshView();
    }

    /**
     * Paste script from system clipboard.
     */
    private void pasteFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String text = (String) contents.getTransferData(DataFlavor.stringFlavor);
                parseScript(text);
                refreshView();
                print("Pasted " + lines.size() + " lines from clipboard.");
            } else {
                print("Clipboard does not contain text.");
            }
        } catch (Exception e) {
            print("Error reading clipboard: " + e.getMessage());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        if ("Step".equals(cmd)) {
            stepExecution();
        } else if ("Back".equals(cmd)) {
            backStep();
        } else if ("Reset".equals(cmd)) {
            resetExecution();
        } else if ("Load".equals(cmd)) {
            loadScript();
        } else if ("Paste".equals(cmd)) {
            pasteFromClipboard();
        } else if ("Skip".equals(cmd)) {
            skipLine();
        } else if ("Edit".equals(cmd)) {
            editCurrentLine();
        } else if ("Swap View".equals(cmd)) {
            swapView();
        }
    }
}
