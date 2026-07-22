package org.jwellman.virtualdesktop.tools;

/**
 * Process-wide access to the tool catalog and lifecycle service.
 *
 * `ToolEnvironment` wasn’t on the original six-step list because it’s **wiring**, not a paradigm noun like `ToolDefinition` or `ToolService`.

    ### Purpose

    Step 1 needs Actions to call **`ToolService.open(defId)`** without going back to “the catalog is the list of Actions” or hard-coding `DesktopManager.get()` in every adapter.

    Something has to answer:

    1. Where is the **catalog**?  
    2. Where is the **`ToolService`**?

    In a fuller design that would be DI / an app context / constructor injection. This app doesn’t have that yet, and `ActionFactory` / `DesktopAction` were already static-friendly. So `ToolEnvironment` is a small **process-wide holder**:

    - `init(catalog, service)` once at desktop startup  
    - `catalog()` / `service()` for adapters and anyone else that needs the seams  

    Today: `ActionFactory.initDesktop()` builds the catalog, then `ToolEnvironment.init(catalog, DesktopManager.get())`.  
    `DesktopAction` does `ToolEnvironment.service().open(id)`.

    ### What it is *not*

    - Not a domain type (not “a tool,” not “the desktop”)  
    - Not a replacement for `AppStore`  
    - Not meant to grow into a god object  

    ### If it feels extra

    You could delete it and instead:

    - `DesktopAction.setToolService(ToolService)` + `ActionFactory.getCatalog()`, or  
    - inject the service into each action at construction  

    Same idea; `ToolEnvironment` just avoids threading two statics through every call site until a real app context exists.

    **Short version:** roadmap said “catalog + ToolService + thin Actions”; `ToolEnvironment` is the temporary **service locator** that plugs those together without a DI framework.

 * <p>Wired once during desktop init (see ActionFactory). Launch adapters call
 * {@link #service()}; feature code should prefer these accessors over concrete
 * DesktopManager / ActionFactory lists when possible.</p>
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
