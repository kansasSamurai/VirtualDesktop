package org.jwellman.virtualdesktop.taskbar;

import java.awt.Color;

/**
 * Immutable color palette for a WindowListView implementation.
 *
 * Concrete views receive a WindowListTheme via applyTheme() and use it for all
 * rendering decisions. The static factory methods provide ready-made palettes;
 * callers can also construct a custom instance via the builder.
 */
public final class WindowListTheme {

    /** 2px left-edge indicator bar color for the active/focused row. */
    public final Color activeIndicator;

    /** Translucent background tint applied to the active/focused row. */
    public final Color activeBackground;

    /** Background fill tint for the execution-progress meter (left-to-right fill). */
    public final Color progressTint;

    /** Text color for normal (non-minimized) tools. */
    public final Color normalForeground;

    /** Text color for minimized tools (dimmed). */
    public final Color minimizedForeground;

    /** Background color for inline group header rows. */
    public final Color groupHeaderBackground;

    /** Text color for inline group header rows. */
    public final Color groupHeaderForeground;

    /** Overall sidebar/panel background. */
    public final Color panelBackground;

    /** Close-button (×) foreground at rest — semi-transparent. */
    public final Color closeButtonDefault;

    /** Close-button (×) foreground on hover. */
    public final Color closeButtonHover;

    private WindowListTheme(
            Color activeIndicator, Color activeBackground, Color progressTint,
            Color normalForeground, Color minimizedForeground,
            Color groupHeaderBackground, Color groupHeaderForeground,
            Color panelBackground,
            Color closeButtonDefault, Color closeButtonHover) {
        this.activeIndicator       = activeIndicator;
        this.activeBackground      = activeBackground;
        this.progressTint          = progressTint;
        this.normalForeground      = normalForeground;
        this.minimizedForeground   = minimizedForeground;
        this.groupHeaderBackground = groupHeaderBackground;
        this.groupHeaderForeground = groupHeaderForeground;
        this.panelBackground       = panelBackground;
        this.closeButtonDefault    = closeButtonDefault;
        this.closeButtonHover      = closeButtonHover;
    }

    /**
     * Dark sidebar with Muted Gold active states — the primary theme for
     * VirtualDesktop's dark Look and Feel.
     *
     * Active indicator: Muted Gold (220, 168, 66)
     * Active background: soft translucent gold tint (alpha 35)
     * Progress tint: slightly opaque gold (alpha 40)
     */
    public static WindowListTheme darkGold() {
        return new WindowListTheme(
            new Color(220, 168,  66),       // activeIndicator  — Muted Gold
            new Color(220, 168,  66,  35),  // activeBackground — translucent gold
            new Color(220, 168,  66,  40),  // progressTint     — soft gold fill
            Color.WHITE,                     // normalForeground
            new Color(140, 145, 150),       // minimizedForeground
            new Color( 45,  52,  60),       // groupHeaderBackground — dark slate
            new Color(180, 190, 200),       // groupHeaderForeground — muted light
            new Color( 30,  35,  40),       // panelBackground
            new Color(255, 255, 255, 100),  // closeButtonDefault — semi-transparent
            Color.RED                        // closeButtonHover
        );
    }

    /**
     * Light sidebar with blue active states — suitable for light Look and Feels.
     */
    public static WindowListTheme light() {
        return new WindowListTheme(
            new Color( 50, 100, 200),       // activeIndicator  — blue
            new Color( 50, 100, 200,  30),  // activeBackground
            new Color( 50, 100, 200,  40),  // progressTint
            Color.DARK_GRAY,                 // normalForeground
            Color.GRAY,                      // minimizedForeground
            new Color(215, 220, 228),       // groupHeaderBackground
            new Color( 60,  70,  80),       // groupHeaderForeground
            new Color(240, 242, 245),       // panelBackground
            new Color(100, 100, 100, 120),  // closeButtonDefault
            Color.RED                        // closeButtonHover
        );
    }

}
