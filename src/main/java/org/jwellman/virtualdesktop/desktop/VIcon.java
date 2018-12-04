/*
 * This class is heavily based on:
 * -------------------------------------------
 * SVGIcon.java (package au.id.mcc.adapted.swing;)
 *
 * A Swing Icon that draws an SVG image.
 *
 * Cameron McCormack <cam (at) mcc.id.au>
 *
 * Permission is hereby granted to use, copy, modify and distribute this
 * code for any purpose, without fee.
 *
 * Initial version: April 21, 2005
 */
package org.jwellman.virtualdesktop.desktop;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.Dimension2D;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.Icon;

import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

import org.w3c.dom.Document;

/**
 * A Swing Icon that draws an SVG image.
 *
 * @author Rick Wellman
 * @author <a href="mailto:cam%40mcc%2eid%2eau">Cameron McCormack</a>
 */
public class VIcon extends UserAgentAdapter implements Icon {

    /**
     * The width of the rendered image.
     */
    protected int width;

    /**
     * The height of the rendered image.
     */
    protected int height;

    /**
     * The BufferedImage generated from the SVG document.
     */
    protected BufferedImage bufferedImage;

    /**
     * Create a new SVGIcon object.
     * @param uri The URI to read the SVG document from.
     * @throws org.apache.batik.transcoder.TranscoderException
     */
    public VIcon(String uri) throws TranscoderException {
        this(uri, 0, 0);
    }

    /**
     * Create a new VIcon object.
     * @param uri The URI to read the SVG document from.
     * @param w The width of the icon.
     * @param h The height of the icon.
     * @throws org.apache.batik.transcoder.TranscoderException
     */
    public VIcon(String uri, int w, int h) throws TranscoderException {
        generateBufferedImage(new TranscoderInput(uri), w, h);
    }

    /**
     * Create a new VIcon object.
     * @param doc The SVG document.
     * @throws org.apache.batik.transcoder.TranscoderException
     */
    public VIcon(Document doc) throws TranscoderException {
        this(doc, 0, 0);
    }

    /**
     * Create a new VIcon object.
     * @param doc The SVG document.
     * @param w The width of the icon.
     * @param h The height of the icon.
     * @throws org.apache.batik.transcoder.TranscoderException
     */
    public VIcon(Document doc, int w, int h) throws TranscoderException {
        generateBufferedImage(new TranscoderInput(doc), w, h);
    }

    /**
     * A convenience method for creating Icons from their path.
     *
     * @param path
     * @return
     * @throws TranscoderException
     */
    public static VIcon createSVGIcon(String path) throws TranscoderException {
        final URL url = VIcon.class.getClassLoader().getResource(path + ".svg");
        final VIcon v = new VIcon(url.toString());
        return v;
    }

    /**
     * Generate the BufferedImage.
     * @param in
     * @param w
     * @param h
     * @throws org.apache.batik.transcoder.TranscoderException
     */
    protected void generateBufferedImage(TranscoderInput in, int w, int h) throws TranscoderException {

        final BufferedImageTranscoder t = new BufferedImageTranscoder();
        if (w != 0 && h != 0) {
            t.setDimensions(w, h);
        }
        t.transcode(in, null);
        bufferedImage = t.getBufferedImage();
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
    }

    /**
     * A transcoder that generates a BufferedImage.
     */
    protected class BufferedImageTranscoder extends ImageTranscoder {

        /**
         * The BufferedImage generated from the SVG document.
         */
        protected BufferedImage bufferedImage;

        /**
         * Creates a new ARGB image with the specified dimension.
         * @param width the image width in pixels
         * @param height the image height in pixels
         * @return
         */
        @Override
        public BufferedImage createImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        /**
         * Writes the specified image to the specified output.
         * @param img the image to write
         * @param output the output where to store the image
         * @throws org.apache.batik.transcoder.TranscoderException if an error occured while storing the image
         */
        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) throws TranscoderException {
            bufferedImage = img;
        }

        /**
         * Returns the BufferedImage generated from the SVG document.
         * @return
         */
        public BufferedImage getBufferedImage() {
            return bufferedImage;
        }

        /**
         * Set the dimensions to be used for the image.
         * @param w
         * @param h
         */
        public void setDimensions(int w, int h) {
            hints.put(KEY_WIDTH, new Float(w));
            hints.put(KEY_HEIGHT, new Float(h));
        }
    }

    // Icon //////////////////////////////////////////////////////////////////

    /**
     * Returns the icon's width.
     */
    @Override
    public int getIconWidth() {
        return width;
    }

    /**
     * Returns the icon's height.
     */
    @Override
    public int getIconHeight() {
        return height;
    }

    /**
     * Draw the icon at the specified location.
     * @param c
     * @param g
     * @param x
     * @param y
     */
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.drawImage(bufferedImage, x, y, null);
    }

    // UserAgent /////////////////////////////////////////////////////////////

    /**
     * Returns the default size of this user agent.
     * @return
     */
    @Override
    public Dimension2D getViewportSize() {
        return new Dimension(width, height);
    }

}