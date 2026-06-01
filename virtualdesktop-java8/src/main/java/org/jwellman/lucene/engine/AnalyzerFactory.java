package org.jwellman.lucene.engine;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
//import org.apache.lucene.analysis.pattern.PatternAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.jwellman.lucene.model.AnalyzerType;

//import java.util.regex.Pattern;

/**
 * Factory that maps {@link AnalyzerType} selections to configured Lucene analyzers.
 *
 * <p>Callers are responsible for closing the returned {@link Analyzer} when done.</p>
 */
public final class AnalyzerFactory {

    private AnalyzerFactory() {
    }

    /**
     * Create an analyzer appropriate for the given type.
     *
     * @param type the tokenization strategy; must not be null
     * @return a new {@link Analyzer} instance (caller must close)
     */
    public static Analyzer create(AnalyzerType type) {
        switch (type) {
            case WHITESPACE:
                return new WhitespaceAnalyzer();
            case CODE_SYNTAX:
                // TODO One thing to be aware of when compiling: 
                /* PatternAnalyzer in AnalyzerFactory — if it's not available in
                 * lucene-analyzers-common 8.11.2 you may get a compile error on 
                 * that class specifically. If so, the CODE_SYNTAX case can fall
                 * back to WhitespaceAnalyzer as a quick fix until a custom analyzer is built.
                 */
                // Split on anything that is not a word char, underscore, or dot.
                // This preserves identifiers like USER_ID and sys.log intact.
                //return new PatternAnalyzer(Pattern.compile("[^\\w._]+"), true, null);
                return new WhitespaceAnalyzer();
            case STANDARD:
            default:
                return new StandardAnalyzer();
        }
    }
}
