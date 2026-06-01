package org.jwellman.lucene.engine;

/**
 * Canonical field name constants for all Lucene documents in this project.
 *
 * <p>Using these constants everywhere (indexing, searching, rendering) ensures
 * that a field rename is a single-file change with compile-time safety.</p>
 *
 * <p>Field semantics:</p>
 * <ul>
 *   <li>{@link #FIELD_ID} — StringField (stored + indexed, not tokenized): absolute file path.</li>
 *   <li>{@link #FIELD_TITLE} — TextField (stored + indexed): file name or document title.</li>
 *   <li>{@link #FIELD_CONTENTS} — TextField (indexed only, not stored): full text body.
 *       Not stored because the file on disk is the source of truth.</li>
 *   <li>{@link #FIELD_LAST_MODIFIED} — LongPoint / StoredField: epoch millis for sorting.</li>
 * </ul>
 */
public final class LuceneDocumentSchema {

    private LuceneDocumentSchema() {
    }

    public static final String FIELD_ID                    = "id";
    public static final String FIELD_TITLE                 = "title";
    public static final String FIELD_CONTENTS              = "contents";
    public static final String FIELD_LAST_MODIFIED         = "last_modified";
    public static final String FIELD_LAST_MODIFIED_STORED  = "last_modified_stored";
}
