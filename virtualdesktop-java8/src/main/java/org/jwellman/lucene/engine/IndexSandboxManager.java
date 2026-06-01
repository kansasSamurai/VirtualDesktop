package org.jwellman.lucene.engine;

import org.apache.lucene.store.FSDirectory;
import org.jwellman.lucene.model.DirectorySandboxConfig;
import org.jwellman.lucene.model.SandboxRuntimeState;
import org.jwellman.lucene.model.SandboxRuntimeState.IndexStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the lifecycle of a single Lucene index sandbox directory.
 *
 * <p>One instance per configured sandbox. Responsibilities are intentionally narrow:
 * open and hold the {@link FSDirectory}, expose it to the indexing and search layers,
 * and track live telemetry via {@link SandboxRuntimeState}.</p>
 *
 * <p>This class does <em>not</em> own an {@link org.apache.lucene.index.IndexWriter}.
 * Writers are opened, used, committed, and closed entirely within
 * {@link BulkIndexer#run()} so that the write lock ({@code write.lock}) only exists
 * on disk during an active indexing run. Between runs the index directory is
 * lock-free and open for reading.</p>
 *
 * <p>All public methods may be called from any thread.
 * {@link #getRuntimeState()} is always safe to read from the Swing EDT.</p>
 *
 * <h3>Write Lock Lifecycle (summary)</h3>
 * <ul>
 *   <li>Lock acquired: when {@link BulkIndexer} opens an {@code IndexWriter}.</li>
 *   <li>Lock released: when {@link BulkIndexer} closes the writer at the end of its run.</li>
 *   <li>Between indexing runs: no lock held; the directory is readable at any time.</li>
 *   <li>Stale lock (killed JVM during indexing): handled by {@link BulkIndexer}, which
 *       deletes the stale {@code write.lock} and retries once.</li>
 * </ul>
 */
public class IndexSandboxManager {

    private final DirectorySandboxConfig config;
    private final Path indexPath;
    private final SandboxRuntimeState runtimeState;

    private FSDirectory directory;

    public IndexSandboxManager(DirectorySandboxConfig config, String baseIndexDir) {
        this.config = config;
        this.indexPath = Paths.get(baseIndexDir, config.getId());
        this.runtimeState = new SandboxRuntimeState(config.getId());
    }

    /**
     * Creates the index directory on disk and opens the {@link FSDirectory} handle.
     * Does not acquire the write lock — that happens only when a
     * {@link BulkIndexer} opens an {@code IndexWriter}.
     */
    public void open() {
        try {
            Files.createDirectories(indexPath);
            directory = FSDirectory.open(indexPath);
            runtimeState.updateProgress(IndexStatus.IDLE, 0, 0);
        } catch (IOException e) {
            runtimeState.setError("Failed to open index directory: " + e.getMessage());
        }
    }

    /**
     * Closes the {@link FSDirectory} handle. Safe to call even if {@link #open()}
     * was never called or failed.
     */
    public void close() {
        if (directory != null) {
            try {
                directory.close();
            } catch (IOException e) {
                // best-effort on shutdown
            } finally {
                directory = null;
            }
        }
    }

    /**
     * Drops all index data and reopens a fresh empty directory.
     * Used by the "Reindex Directory" action; a {@link BulkIndexer} is expected to
     * be submitted immediately after this call.
     */
    public void purge() {
        close();
        try {
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

    /**
     * Returns the open {@link FSDirectory} for this sandbox, or {@code null} if
     * {@link #open()} was never called or failed. Used by {@link BulkIndexer} to
     * open an {@code IndexWriter} and by {@link LuceneService} to open a
     * {@code DirectoryReader} for search.
     */
    public FSDirectory getDirectory() {
        return directory;
    }
}
