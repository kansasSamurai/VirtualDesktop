package org.jwellman.swing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.VolatileImage;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

/**
 * High-Performance Abstract Base Class managing VRAM VolatileImage caching, 
 * HiDPI scaling, and multi-monitor DPI transition handling.
 */
@SuppressWarnings("serial")
public abstract  class AbstractVolatileImageComponent extends JComponent {

    private VolatileImage offscreenBuffer;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private double cachedScaleX = -1.0;
    private double cachedScaleY = -1.0;

    private final PropertyChangeListener configListener = evt -> {
        if ("graphicsConfiguration".equals(evt.getPropertyName())) {
            disposeBuffer();
            invalidateFontMetricsCache();
            repaint();
        }
    };

    protected AbstractVolatileImageComponent() {
        setOpaque(true);
        addPropertyChangeListener("graphicsConfiguration", configListener);
    }

    @Override
    public void removeNotify() {
        removePropertyChangeListener("graphicsConfiguration", configListener);
        disposeBuffer();
        super.removeNotify();
    }

    protected synchronized void disposeBuffer() {
        if (offscreenBuffer != null) {
            offscreenBuffer.flush();
            offscreenBuffer = null;
        }
        cachedWidth = -1;
        cachedHeight = -1;
        cachedScaleX = -1.0;
        cachedScaleY = -1.0;
    }

    @Override
    protected final void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        Graphics2D g2d = (Graphics2D) g;
        GraphicsConfiguration gc = g2d.getDeviceConfiguration();
        AffineTransform tx = gc.getDefaultTransform();
        double scaleX = tx.getScaleX();
        double scaleY = tx.getScaleY();

        validateBuffer(width, height, scaleX, scaleY, gc);

        if (offscreenBuffer != null) {
            do {
                int valCode = offscreenBuffer.validate(gc);
                if (valCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                    disposeBuffer();
                    validateBuffer(width, height, scaleX, scaleY, gc);
                    if (offscreenBuffer == null) return;
                    valCode = offscreenBuffer.validate(gc);
                }

                if (valCode == VolatileImage.IMAGE_RESTORED || valCode == VolatileImage.IMAGE_OK || shouldRedrawBuffer()) {
                    Graphics2D bufG2d = offscreenBuffer.createGraphics();
                    try {
                        bufG2d.scale(scaleX, scaleY);
                        bufG2d.setRenderingHints(getRenderingHints());
                        renderToBuffer(bufG2d, width, height);
                        resetBufferRedrawFlag();
                    } finally {
                        bufG2d.dispose();
                    }
                }

                // Fast GPU blit to screen
                Graphics2D screenG2d = (Graphics2D) g2d.create();
                try {
                    screenG2d.drawImage(offscreenBuffer, 0, 0, width, height, null);
                } finally {
                    screenG2d.dispose();
                }

            } while (offscreenBuffer.contentsLost());
        }
    }

    private void validateBuffer(int w, int h, double sx, double sy, GraphicsConfiguration gc) {
        if (offscreenBuffer == null || cachedWidth != w || cachedHeight != h || cachedScaleX != sx || cachedScaleY != sy) {
            disposeBuffer();
            if (gc != null) {
                int physW = (int) Math.ceil(w * sx);
                int physH = (int) Math.ceil(h * sy);
                offscreenBuffer = gc.createCompatibleVolatileImage(physW, physH, Transparency.OPAQUE);
                cachedWidth = w;
                cachedHeight = h;
                cachedScaleX = sx;
                cachedScaleY = sy;
                markBufferDirty();
            }
        }
    }

    protected RenderingHints getRenderingHints() {
        RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return hints;
    }

    protected void invalidateFontMetricsCache() {}
    protected boolean shouldRedrawBuffer() { return false; }
    protected void resetBufferRedrawFlag() {}
    protected void markBufferDirty() {}

    protected abstract void renderToBuffer(Graphics2D g2d, int width, int height);

}
