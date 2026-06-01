package org.jwellman.lucene.ui;

import org.jwellman.lucene.model.IndexRowItem;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Top-level panel for the Lucene Management vapp.
 *
 * <p>Hosts a horizontal {@link JSplitPane}:
 * <ul>
 *   <li>Left (~220px): {@link LuceneSidebarPanel} — sandbox list with live indicators</li>
 *   <li>Right: {@link LuceneDetailPanel} — config form + activity log</li>
 * </ul>
 * </p>
 */
public class LuceneManagementPanel extends JPanel {

    private final LuceneSidebarPanel sidebarPanel = new LuceneSidebarPanel();
    private final LuceneDetailPanel  detailPanel  = new LuceneDetailPanel();

    public LuceneManagementPanel() {
        super(new BorderLayout());

        sidebarPanel.setPreferredSize(new Dimension(220, 0));
        sidebarPanel.setSelectionCallback(new Consumer<IndexRowItem>() {
            @Override
            public void accept(IndexRowItem item) {
                onSelectionChanged(item);
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebarPanel, detailPanel);
        split.setDividerLocation(220);
        split.setResizeWeight(0.0);
        add(split, BorderLayout.CENTER);

        // Initial load — show global view
        sidebarPanel.reload();
        detailPanel.showGlobal();
    }

    private void onSelectionChanged(IndexRowItem item) {
        // Null config signals the synthetic Global row
        if (item.getConfig() == null) {
            detailPanel.showGlobal();
        } else {
            detailPanel.showSandbox(item);
        }
    }
}
