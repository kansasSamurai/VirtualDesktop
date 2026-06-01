package org.jwellman.lucene.engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.jwellman.lucene.model.DirectorySandboxConfig;
import org.jwellman.lucene.model.SandboxRuntimeState;
import org.jwellman.lucene.model.SandboxRuntimeState.IndexStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the full lifecycle of a single Lucene index sandbox directory.
 *
 * <p>One instance per configured sandbox. Responsible for creating the index
 * directory, acquiring the write lock (fail-fast on contention), and exposing
 * the live {@link SandboxRuntimeState} for the management UI.</p>
 *
 * <p>All public methods must be called from the same background thread or with
 * external synchronization, except {@link #getRuntimeState()} which is safe to
 * call from any thread.</p>
 */
public class IndexSandboxManager {

    private final DirectorySandboxConfig config;
    private final Path indexPath;
    private final SandboxRuntimeState runtimeState;

    private FSDirectory directory;
    private IndexWriter writer;

    public IndexSandboxManager(DirectorySandboxConfig config, String baseIndexDir) {
        this.config = config;
        this.indexPath = Paths.get(baseIndexDir, config.getId());
        this.runtimeState = new SandboxRuntimeState(config.getId());
    }

    /**
     * Opens the index directory and acquires a write lock.
     *
     * <p>If another JVM instance already holds the lock, the sandbox is marked
     * {@link IndexStatus#ERROR} rather than throwing — the UI will show an amber
     * indicator so the user can investigate without a crash.</p>
     */
    public void open() {
        try {
            Files.createDirectories(indexPath);
            directory = FSDirectory.open(indexPath);
            Analyzer analyzer = AnalyzerFactory.create(config.getAnalyzerType());
            IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
            writer = new IndexWriter(directory, writerConfig);
            runtimeState.updateProgress(IndexStatus.IDLE, 0, 0);
        } catch (LockObtainFailedException e) {
            runtimeState.setError("Index locked by another process: " + e.getMessage());
        } catch (IOException e) {
            runtimeState.setError("Failed to open index: " + e.getMessage());
        }
    }

    /**
     * Commits pending changes and closes the index writer and directory.
     */
    public void close() {
        if (writer != null) {
            try {
                writer.commit();
                writer.close();
            } catch (IOException e) {
                // best-effort on shutdown
            } finally {
                writer = null;
            }
        }
        if (directory != null) {
            try {
                directory.close();
            } catch (IOException e) {
                // best-effort
            } finally {
                directory = null;
            }
        }
    }

    /**
     * Drops all index data for this sandbox and reopens a fresh empty index.
     * Used by the "Reindex Directory" action in the management UI.
     */
    public void purge() {
        close();
        try {
            // Delete all files in the index directory
            if (Files.exists(indexPath)) {
                Files.walk(indexPath)
                    .sorted(java.util.Comparator.reverseOrder())
                    .filter(p -> !p.equals(indexPath))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException ignored) {
                        }
                    });
            }
        } catch (IOException e) {
            runtimeState.setError("Purge failed: " + e.getMessage());
            return;
        }
        open();
    }

    /**
     * Manually commits the writer's buffer to disk.
     * Called from the "Commit Active Transactions" button.
     */
    public void commit() throws IOException {
        if (writer != null) {
            writer.commit();
            runtimeState.setLastCommittedTimestamp(System.currentTimeMillis());
        }
    }

    /**
     * Returns the live telemetry state object. Safe to call from any thread.
     */
    public SandboxRuntimeState getRuntimeState() {
        return runtimeState;
    }

    public DirectorySandboxConfig getConfig() {
        return config;
    }

    public Path getIndexPath() {
        return indexPath;
    }

    public IndexWriter getWriter() {
        return writer;
    }
}
