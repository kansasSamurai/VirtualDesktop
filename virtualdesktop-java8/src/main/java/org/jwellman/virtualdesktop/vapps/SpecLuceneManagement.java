package org.jwellman.virtualdesktop.vapps;

import org.jwellman.lucene.engine.LuceneConfigLoader;
import org.jwellman.lucene.engine.LuceneService;
import org.jwellman.lucene.model.LuceneGlobalConfig;
import org.jwellman.lucene.ui.LuceneManagementPanel;

import javax.swing.*;
import java.awt.*;

/**
 * VApp that hosts the Lucene Index Management UI.
 *
 * <p>Handles adhoc service initialization on first open. Because
 * {@link VirtualAppSpec} instances are created fresh each time the menu item
 * is clicked, the {@link LuceneService#isInitialized()} guard prevents
 * double-initialization on subsequent opens.</p>
 *
 * <p><strong>Gap:</strong> No standard service-init protocol exists in this
 * project yet. The {@code isInitialized()} pattern is an adhoc workaround.
 * A future {@code ServiceRegistry} could standardize this.</p>
 */
public class SpecLuceneManagement extends VirtualAppSpec {

    public SpecLuceneManagement() {
        super();
        this.setTitle("Lucene Management");
        this.setWidth(880);
        this.setHeight(560);

        if (!LuceneService.get().isInitialized()) {
            LuceneGlobalConfig cfg = LuceneConfigLoader.load();
            LuceneService.get().initialize(cfg);
        }

        JPanel content = new JPanel(new BorderLayout());
        content.add(new LuceneManagementPanel(), BorderLayout.CENTER);
        this.setContent(content);
    }
}
