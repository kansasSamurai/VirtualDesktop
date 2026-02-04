package org.jwellman.demo.smarttree;

/**
 * 
 * @author rwellman
 *
 */
public interface NodeFormatProvider {

    /**
     * @return An HTML string or plain text to be used as the node's display text.
     * Returns null if this provider doesn't want to handle this specific pair.
     */
    String format(SmartTreePanel.PropertyPair pair, SummaryRegistry summaries);

}