package org.jwellman.lucene.model;

/**
 * Static configuration for a single Lucene index sandbox.
 *
 * <p>Serialized as part of {@link LuceneGlobalConfig} in lucene-config.json.
 * Mutable fields (sourcePath, filter, analyzerType) are user-configurable
 * via the management UI. The id and displayName identify the sandbox.</p>
 */
public class DirectorySandboxConfig {

    private String id;
    private String displayName;
    private String sourcePath;
    private String fileInclusionFilter = "*.txt,*.md";
    private AnalyzerType analyzerType = AnalyzerType.STANDARD;

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
}
