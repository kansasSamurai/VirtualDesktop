package org.jwellman.bsh;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/*
import org.jwellman.bsh.ClipboardHistory;

jvd.cdemo = object();
jvd.cdemo.panel = new ClipboardHistory();
jvd.cdemo.panel.createAndShowGUI();

*/


/**
 * A Swing user interface to store, display, 
 * and manipulate Clipboard history received from a ClipboardListener.
 * 
 * Inspired by: https://medium.com/swlh/creating-a-clipboard-history-application-in-java-with-swing-16b006f7b322
 * 
 * @author rwellman
 *
 */
public class ClipboardHistory extends JPanel implements ClipboardListener.EntryListener {

    // for the list of entries copied in clipboard
    private final JList<String> list = new JList<String>();
    private final DefaultListModel<String> listModel = new DefaultListModel<String>();
    private JPanel controlPane = new JPanel();
    private JButton btnCopy = new JButton("Copy");
    private JButton btnClear = new JButton("Clear");
    private JButton btnDelete = new JButton("Delete");
    private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    private static final long serialVersionUID = 1L;

    public ClipboardHistory() {
        super(new BorderLayout());

        list.setModel(listModel);
        ListSelectionModel listSelectionModel = list.getSelectionModel();
        listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // we create a JScrollPane to embed our control pane
        JScrollPane listPane = new JScrollPane(list);

        // we add a button to let users copy old entries to the clipboard
        btnCopy.addActionListener(e -> copyCurrentSelection() );

        btnClear.addActionListener(e -> clear());

        btnDelete.addActionListener(e -> deleteCurrentSelection());

        // we add the button
        controlPane.add(btnCopy);
        controlPane.add(btnDelete);
        controlPane.add(btnClear);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        add(splitPane, BorderLayout.CENTER);

        JPanel topHalf = new JPanel();
        topHalf.setLayout(new BoxLayout(topHalf, BoxLayout.LINE_AXIS));
        JPanel listContainer = new JPanel(new GridLayout(1, 1));
        listContainer.setBorder(BorderFactory.createTitledBorder("Entries"));
        listContainer.add(listPane);

        topHalf.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        topHalf.add(listContainer);
        topHalf.setMinimumSize(new Dimension(100, 50));
        topHalf.setPreferredSize(new Dimension(100, 250));
        splitPane.add(topHalf);

        JPanel bottomHalf = new JPanel(new BorderLayout());
        bottomHalf.add(controlPane, BorderLayout.CENTER);
        bottomHalf.setPreferredSize(new Dimension(450, 30));
        splitPane.add(bottomHalf);
    }

    public void clear() {
        DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
        model.removeAllElements();
    }

    public void copyCurrentSelection() {
        // get the selected value before we clobber it
        String value = (String) list.getSelectedValue();

        // remove selected index to avoid duplicate when we copy it back to the clipboard ...
        deleteCurrentSelection();

        // copy to clipboard (which puts this back in the list on top)
        copyToClipboard(value);
    }

    public void deleteCurrentSelection() {
        if ( ! list.isSelectionEmpty() ) {
            int index = list.getSelectedIndex();
            listModel.remove(index);
        }
    }

    public void copyToClipboard(String value) {
        StringSelection data = new StringSelection(value);
        clipboard.setContents(data, data);
    }

    public void createAndShowGUI() {

        setOpaque(true); // This probably isn't necessary but is from the original code

        // TODO put this in an internalframe / use jpad 
        // We create a top JFrame
        JFrame frame = new JFrame("Clipboard History");
        try { frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); }
        catch (Exception e) {/* jpad will throw a security exception */}
        frame.setContentPane(this);
        frame.pack();
        frame.setVisible(true);

        // we connect the Clipboard Listener to our UI
        ClipboardListener listener = new ClipboardListener();
        listener.setEntryListener(this);
        listener.start();
    }

    @Override
    public void onCopy(String data) {
        // we add new entry on the top of our list
        listModel.add(0, data);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClipboardHistory().createAndShowGUI();
            }
        });
    }

}
