package org.jwellman.lucene.ui;

import org.jwellman.lucene.model.IndexRowItem;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Top-level panel for the Lucene Management vapp.
 *
 * <p>Hosts a {@link JTabbedPane} with two tabs:
 * <ul>
 *   <li><b>Management</b> — the original split-pane UI:
 *       {@link LuceneSidebarPanel} (left) + {@link LuceneDetailPanel} (right)</li>
 *   <li><b>Search</b> — {@link LuceneSearchPanel} with debounced omni-search
 *       and SmartGrid results</li>
 * </ul>
 * </p>
 */
@SuppressWarnings("serial")
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

        // Initial load — show global view
        sidebarPanel.reload();
        detailPanel.showGlobal();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Management", split);
        tabs.addTab("Search", new LuceneSearchPanel());
        add(tabs, BorderLayout.CENTER);
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
