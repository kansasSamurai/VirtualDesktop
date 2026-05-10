package org.jwellman.swing.grid;

import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * Navigation bar for SmartGrid page-based pagination.
 *
 * Layout: ◀ ‹ [1] [2] [3] [4] [5] › ▶  |  Showing rows X–Y of Z
 *
 * Shows up to 5 page-number buttons centered on the current page.
 * Current page button is bold and disabled (not clickable).
 */
public class PaginationBar extends JPanel {

    private final SmartGrid grid;

    public PaginationBar(SmartGrid grid) {
        super(new FlowLayout(FlowLayout.CENTER, 3, 2));
        this.grid = grid;
        update(grid.getCurrentPage(), grid.totalPages());
    }

    public void update(int currentPage, int totalPages) {
        removeAll();

        if (totalPages <= 0) { revalidate(); repaint(); return; }

        // First / Prev
        JButton first = navButton("◀", currentPage > 0, () -> grid.goToPage(0));
        JButton prev  = navButton("‹", currentPage > 0, () -> grid.goToPage(currentPage - 1));
        add(first);
        add(prev);

        // Numbered page buttons (window of up to 5, centered on currentPage)
        int window    = Math.min(5, totalPages);
        int startPage = Math.max(0, currentPage - window / 2);
        int endPage   = Math.min(totalPages - 1, startPage + window - 1);
        startPage     = Math.max(0, endPage - window + 1);

        for (int p = startPage; p <= endPage; p++) {
            final int target = p;
            JButton btn = new JButton(String.valueOf(p + 1));
            if (p == currentPage) {
                btn.setFont(btn.getFont().deriveFont(Font.BOLD));
                btn.setEnabled(false);
            } else {
                btn.addActionListener(e -> grid.goToPage(target));
            }
            add(btn);
        }

        // Next / Last
        JButton next = navButton("›", currentPage < totalPages - 1, () -> grid.goToPage(currentPage + 1));
        JButton last = navButton("▶", currentPage < totalPages - 1, () -> grid.goToPage(totalPages - 1));
        add(next);
        add(last);

        // Separator + "Showing rows X–Y of Z"
        add(new JSeparator(JSeparator.VERTICAL));
        int pageSize  = grid.getPageSize();
        int totalRows = grid.getModel().getRowCount();
        int startRow  = currentPage * pageSize + 1;
        int endRow    = Math.min((currentPage + 1) * pageSize, totalRows);
        add(new JLabel(String.format("  Showing %,d–%,d of %,d rows  ", startRow, endRow, totalRows)));

        revalidate();
        repaint();
    }

    private JButton navButton(String text, boolean enabled, Runnable action) {
        JButton btn = new JButton(text);
        btn.setEnabled(enabled);
        if (enabled) btn.addActionListener(e -> action.run());
        return btn;
    }
}
