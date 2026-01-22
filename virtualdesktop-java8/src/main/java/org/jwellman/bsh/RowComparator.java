package org.jwellman.bsh;

import java.util.Map;

/**
 * An interface to support scripted comparators.
 * 
 * @author rwellman
 *
 */
public interface RowComparator {
    int compare(Map<String, Object> a, Map<String, Object> b);
}
