package org.jwellman.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Renders drag-ghost images for use with {@link SmartTransferHandler}.
 *
 * Images are created on demand each time a drag starts so they always
 * reflect the current state (color, text) of the item being dragged.
 */
public final class DragImageFactory {

    private static final int  HEIGHT = 18;
    private static final int  PAD_X  = 3;
    private static final Font FONT   = new Font("SansSerif", Font.PLAIN, 11);

    private DragImageFactory() {}

    /**
     * Renders a pill-shaped label image: solid {@code background} fill with
     * {@code text} in white, matching the visual style of a DayCellPanel event
     * banner. Suitable as a drag ghost for any horizontally-oriented colored label.
     *
     * @param text       label text (clipped naturally by image width)
     * @param background fill color, typically the event category color
     * @param width      image width in pixels; 140 is a reasonable fixed default
     */
    public static BufferedImage forColoredLabel(String text, Color background, int width) {
        BufferedImage img = new BufferedImage(width, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(background);
        g.fillRect(0, 0, width, HEIGHT);

        g.setFont(FONT);
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int textY = (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, PAD_X, textY);

        g.dispose();
        return img;
    }

}
