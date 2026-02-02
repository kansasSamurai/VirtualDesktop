package org.jwellman.demo.smarttree;

/**
 * 
 * @author rwellman
 *
 */
public interface TransformationProvider {
    /**
     * @return The transformed object to be used for the tree, 
     * or the original object if no change is needed.
     */
    Object transform(Object original);
}