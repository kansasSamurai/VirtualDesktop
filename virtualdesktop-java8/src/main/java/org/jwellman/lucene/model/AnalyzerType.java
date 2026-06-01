package org.jwellman.lucene.model;

/**
 * Selects the tokenization strategy for a Lucene index sandbox.
 */
public enum AnalyzerType {

    /** General-purpose text: punctuation-stripped, case-folded. */
    STANDARD,

    /** Splits only on whitespace; preserves punctuation. */
    WHITESPACE,

    /** Preserves underscores and dots for code identifiers (USER_ID, sys.log). */
    CODE_SYNTAX
}
