package org.jwellman.lucene.engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.jwellman.lucene.model.DirectorySandboxConfig;
import org.jwellman.lucene.model.IndexRowItem;
import org.jwellman.lucene.model.LuceneGlobalConfig;
import org.jwellman.lucene.model.SandboxRuntimeState;
import org.jwellman.lucene.model.SearchOperator;

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
    private final Map<String, DirectoryReader> readerCache = new LinkedHashMap<String, DirectoryReader>();
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
     * Closes all cached readers and sandbox managers. Called by the JVM shutdown hook.
     */
    public void shutdown() {
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        synchronized (readerCache) {
            for (DirectoryReader reader : readerCache.values()) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
            readerCache.clear();
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
     * Executes a full-text search across one or all sandboxes.
     *
     * <p>Searches {@link LuceneDocumentSchema#FIELD_TITLE} and
     * {@link LuceneDocumentSchema#FIELD_CONTENTS} using a {@link MultiFieldQueryParser}.
     * Uses a per-sandbox cached {@link DirectoryReader} that is only reopened when
     * a new commit is detected (via {@link DirectoryReader#openIfChanged}), so
     * repeated searches against an unchanged index pay no I/O cost.</p>
     *
     * @param sandboxId   the sandbox to search, or {@code null} / empty to search all
     * @param queryString the raw query string (Lucene query syntax supported)
     * @param maxResults  maximum results per sandbox
     * @param operator    how multiple words are combined ({@link SearchOperator#AND} by default)
     * @return combined results sorted by relevance score descending
     */
    public List<SearchResult> search(String sandboxId, String queryString, int maxResults,
                                     SearchOperator operator) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        boolean searchAll = (sandboxId == null || sandboxId.trim().isEmpty());

        for (Map.Entry<String, IndexSandboxManager> entry : managers.entrySet()) {
            if (!searchAll && !entry.getKey().equals(sandboxId)) {
                continue;
            }
            IndexSandboxManager mgr = entry.getValue();
            FSDirectory dir = mgr.getDirectory();
            if (dir == null) {
                continue;
            }
            DirectoryReader reader = acquireReader(entry.getKey(), dir);
            if (reader == null) {
                continue;
            }
            try {
                IndexSearcher searcher = new IndexSearcher(reader);
                Analyzer analyzer = AnalyzerFactory.create(mgr.getConfig().getAnalyzerType());
                String[] fields = {
                    LuceneDocumentSchema.FIELD_TITLE,
                    LuceneDocumentSchema.FIELD_CONTENTS
                };
                MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
                parser.setAllowLeadingWildcard(true);
                Query query;
                if (operator == SearchOperator.PHRASE) {
                    String escaped = queryString.replace("\"", "\\\"");
                    query = parser.parse("\"" + escaped + "\"");
                } else {
                    parser.setDefaultOperator(operator == SearchOperator.OR
                        ? QueryParser.Operator.OR
                        : QueryParser.Operator.AND);
                    query = parser.parse(queryString);
                }
                TopDocs hits = searcher.search(query, maxResults);
                for (ScoreDoc sd : hits.scoreDocs) {
                    Document doc = searcher.doc(sd.doc);
                    String title = doc.get(LuceneDocumentSchema.FIELD_TITLE);
                    String path  = doc.get(LuceneDocumentSchema.FIELD_ID);
                    String storedModified = doc.get(LuceneDocumentSchema.FIELD_LAST_MODIFIED_STORED);
                    long millis = 0L;
                    if (storedModified != null) {
                        try {
                            millis = Long.parseLong(storedModified);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    results.add(new SearchResult(entry.getKey(), title, path, millis, sd.score));
                }
            } catch (ParseException e) {
                System.err.println("LuceneService: query parse error: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("LuceneService: search error for " + entry.getKey() + ": " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Returns a live {@link DirectoryReader} for the given sandbox, using a
     * cached instance where possible.
     *
     * <p>On the first call for a sandbox this opens a fresh reader. On subsequent
     * calls {@link DirectoryReader#openIfChanged} checks whether a new commit has
     * appeared; if not, the existing reader is returned immediately with no I/O.
     * If a new commit is detected the old reader is closed and a new one opened.</p>
     *
     * <p>Returns {@code null} if the index has not been built yet
     * ({@link IndexNotFoundException}).</p>
     *
     * <p>Synchronized to guard the shared {@link #readerCache} map against
     * concurrent search calls from background threads.</p>
     */
    private synchronized DirectoryReader acquireReader(String sandboxId, FSDirectory dir) {
        DirectoryReader cached = readerCache.get(sandboxId);
        try {
            if (cached == null) {
                DirectoryReader fresh = DirectoryReader.open(dir);
                readerCache.put(sandboxId, fresh);
                return fresh;
            }
            DirectoryReader updated = DirectoryReader.openIfChanged(cached);
            if (updated != null) {
                try {
                    cached.close();
                } catch (IOException ignored) {
                }
                readerCache.put(sandboxId, updated);
                return updated;
            }
            return cached;
        } catch (IndexNotFoundException e) {
            // Index not yet built for this sandbox
            return null;
        } catch (IOException e) {
            System.err.println("LuceneService: failed to acquire reader for " + sandboxId + ": " + e.getMessage());
            return null;
        }
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
