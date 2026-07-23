package org.jwellman.virtualdesktop.tools;

/**
 * Process-wide access to the tool catalog and lifecycle service.
 *
 * <p>Wiring / service-locator for the current static-friendly bootstrap. Call
 * {@link #init} once during desktop startup (see ActionFactory). Launch adapters
 * and controllers should use {@link #service()} / {@link #catalog()} instead of
 * depending on concrete DesktopManager.</p>
 *
 * <p>Not a domain type and not a replacement for AppStore. Prefer constructor
 * injection later if an application context appears.</p>
 */
public final class ToolEnvironment {

    private static ToolCatalog catalog;
    private static ToolService service;

    private ToolEnvironment() {
    }

    /**
     * Binds catalog and service for this JVM process.
     *
     * @param toolCatalog the populated catalog
     * @param toolService the lifecycle service (typically DesktopManager)
     */
    public static synchronized void init(ToolCatalog toolCatalog, ToolService toolService) {
        if (toolCatalog == null) {
            throw new IllegalArgumentException("toolCatalog is required");
        }
        if (toolService == null) {
            throw new IllegalArgumentException("toolService is required");
        }
        catalog = toolCatalog;
        service = toolService;
    }

    public static ToolCatalog catalog() {
        if (catalog == null) {
            throw new IllegalStateException("ToolEnvironment not initialized — call ActionFactory.initDesktop() first");
        }
        return catalog;
    }

    public static ToolService service() {
        if (service == null) {
            throw new IllegalStateException("ToolEnvironment not initialized — call ActionFactory.initDesktop() first");
        }
        return service;
    }

    /**
     * @return true if {@link #init} has been called
     */
    public static boolean isInitialized() {
        return catalog != null && service != null;
    }

}
