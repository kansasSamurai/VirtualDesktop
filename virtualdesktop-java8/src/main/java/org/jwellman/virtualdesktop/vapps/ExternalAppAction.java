package org.jwellman.virtualdesktop.vapps;

import org.jwellman.virtualdesktop.tools.ToolDefinition;

/**
 * Desktop shortcut adapter for externally launched tools.
 *
 * <p>Launch logic lives in {@code ToolService}; this subclass exists so
 * desktop tiles can detect external tools (e.g. indicator glyph).</p>
 *
 * @author Rick Wellman
 */
public class ExternalAppAction extends DesktopAction {

    private static final long serialVersionUID = 1L;

    /**
     * @param definition EXTERNAL catalog entry
     */
    public ExternalAppAction(ToolDefinition definition) {
        super(definition, true);
    }

}
