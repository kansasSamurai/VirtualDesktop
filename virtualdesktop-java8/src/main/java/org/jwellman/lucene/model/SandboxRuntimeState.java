package org.jwellman.lucene.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe runtime telemetry for one index sandbox.
 *
 * <p>Updated exclusively by background indexing threads via
 * {@link #updateProgress}. Read by Swing cell renderers without
 * additional locking — {@code volatile} and {@code AtomicInteger}
 * provide the necessary cross-thread visibility.</p>
 */
public class SandboxRuntimeState {

    public enum IndexStatus {
        IDLE,
        SCANNING,
        WATCHING,
        ERROR
    }

    private final String sandboxId;
    private volatile IndexStatus status = IndexStatus.IDLE;
    private volatile String errorMessage = null;
    private final AtomicInteger documentCount = new AtomicInteger(0);
    private final AtomicInteger currentProgress = new AtomicInteger(0);
    private volatile long lastCommittedTimestamp = 0;

    public SandboxRuntimeState(String sandboxId) {
        this.sandboxId = sandboxId;
    }

    public void updateProgress(IndexStatus status, int docCount, int progress) {
        this.status = status;
        documentCount.set(docCount);
        currentProgress.set(progress);
    }

    public void setError(String message) {
        this.status = IndexStatus.ERROR;
        this.errorMessage = message;
    }

    public String getSandboxId() {
        return sandboxId;
    }

    public IndexStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getDocumentCount() {
        return documentCount.get();
    }

    public int getProgress() {
        return currentProgress.get();
    }

    public long getLastCommittedTimestamp() {
        return lastCommittedTimestamp;
    }

    public void setLastCommittedTimestamp(long lastCommittedTimestamp) {
        this.lastCommittedTimestamp = lastCommittedTimestamp;
    }
}
