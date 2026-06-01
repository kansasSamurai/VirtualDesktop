package org.jwellman.lucene.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Root configuration for the Lucene subsystem.
 *
 * <p>Persisted to {@code config/lucene-config.json} via Jackson.
 * Loaded once at initialization; re-written whenever sandboxes are added
 * or modified via the management UI.</p>
 */
public class LuceneGlobalConfig {

    private String version = "1.0.0";
    private String baseIndexDirectory = "data/lucene";
    private int maxBackgroundThreads = 2;
    private boolean autoStartFileWatcher = false;
    private List<DirectorySandboxConfig> sandboxes = new ArrayList<DirectorySandboxConfig>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBaseIndexDirectory() {
        return baseIndexDirectory;
    }

    public void setBaseIndexDirectory(String baseIndexDirectory) {
        this.baseIndexDirectory = baseIndexDirectory;
    }

    public int getMaxBackgroundThreads() {
        return maxBackgroundThreads;
    }

    public void setMaxBackgroundThreads(int maxBackgroundThreads) {
        this.maxBackgroundThreads = maxBackgroundThreads;
    }

    public boolean isAutoStartFileWatcher() {
        return autoStartFileWatcher;
    }

    public void setAutoStartFileWatcher(boolean autoStartFileWatcher) {
        this.autoStartFileWatcher = autoStartFileWatcher;
    }

    public List<DirectorySandboxConfig> getSandboxes() {
        return sandboxes;
    }

    public void setSandboxes(List<DirectorySandboxConfig> sandboxes) {
        this.sandboxes = sandboxes;
    }
}
