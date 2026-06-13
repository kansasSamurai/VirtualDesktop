package org.jwellman.demo.chess;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Stack;

@SuppressWarnings("serial")
public class MovesScoresheetPanel extends JPanel {

    private final JTable table;
    private final DefaultTableModel tableModel;

    public MovesScoresheetPanel() {
        this.setLayout(new BorderLayout());

        // Setup a strict 3-column data structure: [ Move #, White, Black ]
        String[] columns = {"#", "White", "Black"};
        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Strict read-only presentation
            }
        };

        this.table = new JTable(tableModel);
        this.table.getTableHeader().setReorderingAllowed(false);
        this.table.getColumnModel().getColumn(0).setMaxWidth(40); // Move number column stays tight
        this.table.getColumnModel().getColumn(1).setMaxWidth(120); // Move number column stays tight
        this.table.getColumnModel().getColumn(2).setMaxWidth(120); // Move number column stays tight
        this.table.setBorder(null);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setPreferredSize(new Dimension(400, 400));
        scrollPane.getViewport().setBorder(null);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * The Controller invokes this method during the UI refresh cascade.
     */
    public void synchronizeHistory(Stack<MoveEvent> undoStack) {
        // 1. Wipe the current view layout clean
        tableModel.setRowCount(0);

        if (undoStack.isEmpty()) return;

        // 2. Map the sequential timeline list into two-ply game rounds
        int moveNumber = 1;
        String whiteText = "";
        String blackText = "";

        // Elements are naturally traversed from oldest (index 0) to newest (top of stack)
        for (int i = 0; i < undoStack.size(); i++) {
            MoveEvent event = undoStack.get(i);
            
            if (i % 2 == 0) {
                // Even index = White's turn
                whiteText = event.toAlgebraicNotation();
                blackText = ""; // Clear out black in case this is the active incomplete turn
            } else {
                // Odd index = Black's turn
                blackText = event.toAlgebraicNotation();
                
                // We have a complete pair! Commit the row to the table view matrix
                tableModel.addRow(new Object[]{ moveNumber + ".", whiteText, blackText });
                moveNumber++;
                whiteText = ""; // Reset anchors
            }
        }

        // Catch an trailing trailing hanging move if White has played but Black hasn't responded yet
        if (!whiteText.isEmpty()) {
            tableModel.addRow(new Object[]{ moveNumber + ".", whiteText, "" });
        }

        // 3. Proactive UI Comfort: Auto-scroll to the very bottom row as moves accumulate
        table.scrollRectToVisible(table.getCellRect(tableModel.getRowCount() - 1, 0, true));
    }

}
