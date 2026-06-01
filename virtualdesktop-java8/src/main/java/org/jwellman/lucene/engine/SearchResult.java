package org.jwellman.lucene.engine;

/**
 * Immutable result record returned by {@link LuceneService#search}.
 *
 * <p>Field semantics mirror {@link LuceneDocumentSchema}:
 * {@link #path} corresponds to {@code FIELD_ID} (absolute file path),
 * {@link #title} to {@code FIELD_TITLE}, and
 * {@link #lastModifiedMillis} to {@code FIELD_LAST_MODIFIED_STORED}.</p>
 */
public final class SearchResult {

    private final String sandboxId;
    private final String title;
    private final String path;
    private final long lastModifiedMillis;
    private final float score;

    public SearchResult(String sandboxId, String title, String path,
                        long lastModifiedMillis, float score) {
        this.sandboxId = sandboxId;
        this.title = title;
        this.path = path;
        this.lastModifiedMillis = lastModifiedMillis;
        this.score = score;
    }

    public String getSandboxId() {
        return sandboxId;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public long getLastModifiedMillis() {
        return lastModifiedMillis;
    }

    public float getScore() {
        return score;
    }
}
