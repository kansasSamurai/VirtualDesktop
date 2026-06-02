package org.jwellman.lucene.model;

/**
 * The logical operator applied when a search query contains multiple words.
 *
 * <ul>
 *   <li>{@link #AND} — all terms must appear somewhere in the document (default)</li>
 *   <li>{@link #OR}  — any term is sufficient; documents with more matches rank higher</li>
 *   <li>{@link #PHRASE} — terms must appear adjacent and in order</li>
 * </ul>
 */
public enum SearchOperator {
    AND, OR, PHRASE
}
