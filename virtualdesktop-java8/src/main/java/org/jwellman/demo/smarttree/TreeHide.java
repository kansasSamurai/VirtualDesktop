package org.jwellman.demo.smarttree;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to exclude fields from tree views
 * when using SmartTreePanel.
 * 
 * @author rwellman
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TreeHide {
    // Just a marker to exclude fields from tree views
}
