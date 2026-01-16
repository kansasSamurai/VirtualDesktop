package org.jwellman.virtualdesktop.vapps;

import java.util.Map;

/**
 * Interface for tools that accept configuration attributes.
 *
 * Tools implementing this interface will have their {@link #configure(Map)}
 * method called after instantiation, allowing them to read custom
 * configuration from vapps-config.json.
 *
 * Example JSON configuration:
 * <pre>
 * {
 *   "class": "org.jwellman.virtualdesktop.vapps.SpecHtmlViewer",
 *   "title": "HTML Viewer",
 *   "attrs": {
 *     "htmlFile": "docs/index.html"
 *   }
 * }
 * </pre>
 *
 * @author rwellman
 */
public interface Configurable {

    /**
     * Configure this tool with the provided attributes.
     *
     * @param attrs key-value configuration from vapps-config.json
     */
    void configure(Map<String, String> attrs);

}
