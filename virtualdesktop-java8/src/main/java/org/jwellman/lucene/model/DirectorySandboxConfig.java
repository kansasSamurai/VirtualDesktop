package org.jwellman.lucene.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Static configuration for a single Lucene index sandbox.
 *
 * <p>Serialized as part of {@link LuceneGlobalConfig} in lucene-config.json.
 * Mutable fields (sourcePath, filter, analyzerType, directoryExclusions) are
 * user-configurable via the management UI. The id and displayName identify
 * the sandbox.</p>
 *
 * <p>When deserializing an existing config that pre-dates the
 * {@code directoryExclusions} field, Jackson leaves the field at its
 * initialized default ({@code [".git", ".claude"]}), so old configs are
 * automatically upgraded on the next save.</p>
 */
public class DirectorySandboxConfig {

    private String id;
    private String displayName;
    private String sourcePath;
    private String fileInclusionFilter = "*.txt,*.md";
    private AnalyzerType analyzerType = AnalyzerType.STANDARD;
    private List<String> directoryExclusions =
            new ArrayList<String>(Arrays.asList(".git", ".claude"));

    public DirectorySandboxConfig() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getFileInclusionFilter() {
        return fileInclusionFilter;
    }

    public void setFileInclusionFilter(String fileInclusionFilter) {
        this.fileInclusionFilter = fileInclusionFilter;
    }

    public AnalyzerType getAnalyzerType() {
        return analyzerType;
    }

    public void setAnalyzerType(AnalyzerType analyzerType) {
        this.analyzerType = analyzerType;
    }

    public List<String> getDirectoryExclusions() {
        return directoryExclusions;
    }

    public void setDirectoryExclusions(List<String> directoryExclusions) {
        this.directoryExclusions = directoryExclusions;
    }
}
