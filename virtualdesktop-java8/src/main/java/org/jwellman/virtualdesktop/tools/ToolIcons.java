package org.jwellman.virtualdesktop.tools;

import javax.swing.Icon;

import org.jwellman.dsp.DSP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves catalog / ToolInstance icon keys to Swing Icons for views.
 *
 * <p>Keeps DSP lookup conventions in one place so controllers do not reach
 * into DesktopManager frames for display data.</p>
 */
public final class ToolIcons {

    private static final Logger LOG = LoggerFactory.getLogger(ToolIcons.class);

    /** Fallback when no key is available (matches DesktopManager frame default). */
    public static final String FALLBACK_KEY = "jpad.java";

    private ToolIcons() {
    }

    /**
     * Resolve an icon key to a large/desktop-tile Icon.
     *
     * @param iconKey catalog key (without -small/-large suffix), or null
     * @return a non-null icon when DSP has the fallback; may be null if DSP fails entirely
     */
    public static Icon resolveLarge(String iconKey) {
        if (iconKey != null && !iconKey.isEmpty()) {
            Icon icon = tryKey(iconKey + "-large");
            if (icon != null) {
                return icon;
            }
            icon = tryKey(iconKey + "-small");
            if (icon != null) {
                return icon;
            }
            icon = tryKey(iconKey);
            if (icon != null) {
                return icon;
            }
        }
        return tryKey(FALLBACK_KEY);
    }

    /**
     * Resolve an icon key to a small/taskbar-sized Icon.
     *
     * @param iconKey catalog key (without -small/-large suffix), or null
     * @return a non-null icon when DSP has the fallback; may be null if DSP fails entirely
     */
    public static Icon resolve(String iconKey) {
        if (iconKey != null && !iconKey.isEmpty()) {
            Icon icon = tryKey(iconKey + "-small");
            if (icon != null) {
                return icon;
            }
            icon = tryKey(iconKey + "-large");
            if (icon != null) {
                return icon;
            }
            icon = tryKey(iconKey);
            if (icon != null) {
                return icon;
            }
        }
        return tryKey(FALLBACK_KEY);
    }

    private static Icon tryKey(String key) {
        try {
            return DSP.Icons.getIcon(key);
        } catch (Exception ex) {
            LOG.debug("Icon not found for key {}", key);
            return null;
        }
    }

}
