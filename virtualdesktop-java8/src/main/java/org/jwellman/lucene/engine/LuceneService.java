package org.jwellman.lucene.engine;

import org.jwellman.lucene.model.DirectorySandboxConfig;
import org.jwellman.lucene.model.IndexRowItem;
import org.jwellman.lucene.model.LuceneGlobalConfig;
import org.jwellman.lucene.model.SandboxRuntimeState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Singleton service that owns all {@link IndexSandboxManager} instances.
 *
 * <p>Initialization is deferred to the first time the Lucene Management vapp
 * is opened (not at App startup). The {@link #isInitialized()} guard in the
 * vapp constructor prevents double-initialization on subsequent vapp opens.</p>
 *
 * <p><strong>Gap:</strong> The project has no standard service-initialization
 * protocol. This uses an adhoc {@code volatile boolean initialized} flag.
 * A future ServiceRegistry could standardize this pattern.</p>
 *
 * <p>Shutdown is handled by a JVM shutdown hook registered in {@link #initialize}.
 * The vapp does not need to call {@link #shutdown()} explicitly.</p>
 */
public class LuceneService {

    private static volatile LuceneService instance;

    private volatile boolean initialized = false;
    private LuceneGlobalConfig globalConfig;
    private final Map<String, IndexSandboxManager> managers = new LinkedHashMap<String, IndexSandboxManager>();
    private ExecutorService threadPool;

    private LuceneService() {
    }

    public static LuceneService get() {
        if (instance == null) {
            synchronized (LuceneService.class) {
                if (instance == null) {
                    instance = new LuceneService();
                }
            }
        }
        return instance;
    }

    /**
     * Returns true if {@link #initialize} has already been called.
     * Used by the vapp constructor's adhoc guard.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Bootstraps the service: creates the base index directory, opens a manager
     * for each configured sandbox, and registers a JVM shutdown hook.
     *
     * @param config the loaded global configuration
     */
    public void initialize(LuceneGlobalConfig config) {
        this.globalConfig = config;

        try {
            Files.createDirectories(Paths.get(config.getBaseIndexDirectory()));
        } catch (IOException e) {
            System.err.println("LuceneService: cannot create base index directory: " + e.getMessage());
        }

        threadPool = Executors.newFixedThreadPool(config.getMaxBackgroundThreads());

        for (DirectorySandboxConfig sandboxConfig : config.getSandboxes()) {
            IndexSandboxManager manager = new IndexSandboxManager(sandboxConfig, config.getBaseIndexDirectory());
            manager.open();
            managers.put(sandboxConfig.getId(), manager);
            startIndexing(sandboxConfig.getId());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        }));

        initialized = true;
    }

    /**
     * Closes all sandbox managers. Called by the JVM shutdown hook.
     */
    public void shutdown() {
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        for (IndexSandboxManager manager : managers.values()) {
            manager.close();
        }
        managers.clear();
    }

    /**
     * Submits a background indexing job for the given sandbox.
     * Safe to call from any thread; no-op if the pool is shut down or sandbox not found.
     */
    public void startIndexing(String sandboxId) {
        IndexSandboxManager mgr = managers.get(sandboxId);
        if (mgr != null && threadPool != null && !threadPool.isShutdown()) {
            threadPool.submit(new BulkIndexer(mgr));
        }
    }

    /**
     * Adds a new sandbox at runtime: opens its manager, appends to config,
     * and saves the updated config to disk.
     *
     * @param sandboxConfig the new sandbox to add
     */
    public void addSandbox(DirectorySandboxConfig sandboxConfig) {
        IndexSandboxManager manager = new IndexSandboxManager(sandboxConfig, globalConfig.getBaseIndexDirectory());
        manager.open();
        managers.put(sandboxConfig.getId(), manager);
        globalConfig.getSandboxes().add(sandboxConfig);
        LuceneConfigLoader.save(globalConfig);
        startIndexing(sandboxConfig.getId());
    }

    /**
     * Returns the manager for the given sandbox id, or null if not found.
     */
    public IndexSandboxManager getManager(String id) {
        return managers.get(id);
    }

    public LuceneGlobalConfig getGlobalConfig() {
        return globalConfig;
    }

    /**
     * Builds the sidebar row list: one {@link IndexRowItem} per sandbox.
     */
    public List<IndexRowItem> buildRowItems() {
        List<IndexRowItem> items = new ArrayList<IndexRowItem>();
        for (IndexSandboxManager manager : managers.values()) {
            items.add(new IndexRowItem(manager.getConfig(), manager.getRuntimeState()));
        }
        return items;
    }

    /**
     * Returns the aggregate document count across all sandboxes.
     */
    public int getTotalDocumentCount() {
        int total = 0;
        for (IndexSandboxManager manager : managers.values()) {
            total += manager.getRuntimeState().getDocumentCount();
        }
        return total;
    }

    /**
     * Returns the count of sandboxes currently in SCANNING state.
     */
    public int getActiveSandboxCount() {
        int active = 0;
        for (IndexSandboxManager manager : managers.values()) {
            if (manager.getRuntimeState().getStatus() == SandboxRuntimeState.IndexStatus.SCANNING) {
                active++;
            }
        }
        return active;
    }
}
