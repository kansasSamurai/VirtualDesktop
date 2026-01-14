package org.jwellman.swing.jpanel.tiles;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Draws text.
 * <p>
 * 
 * 
 * TODO research how to implement text rotation:
 * https://stackoverflow.com/questions/10388118/how-to-make-rotated-text-look-good-with-java2d 
 * Also:
 * https://stackoverflow.com/questions/61823828/java-2d-vertically-centering-text 
 * 
 * @author rwellman
 *
 */
public class TextTile extends AbstractTile {

    private Font font = new Font("Segoe UI", 12, Font.PLAIN);

    private String text;

    public TextTile(String t) {
        /* This just sets a small rectangle for the sake of the constructor.  
         * The actual bounds of this object will be set during the draw() method 
         * based on the font metrics of the text.
         */
        super(3, 3);
        
        this.text = t;
    }

    @Override
    public void draw(Graphics2D g2d) {

        g2d.setFont(this.getFont());
        FontRenderContext frc = g2d.getFontRenderContext();
        Rectangle2D bounds = g2d.getFont().getStringBounds(this.getText(), frc);
        this.setWidth((int) bounds.getWidth()); // subtracting one is still an option
        this.setHeight((int) bounds.getHeight()); // subtracting one is still an option

//        LineMetrics lm = this.getFont().getLineMetrics("test", frc);
//        int bli = lm.getBaselineIndex();
//        float[] offsets = lm.getBaselineOffsets();
//        System.out.println("BaselineIndex: " + bli);
//        for (int i=0; i<offsets.length; i++) {
//            System.out.println("BaselineOffsets " + i + ": " + offsets[i]);
//        }
//        lm.getStrikethroughThickness();
        
        this.calcPosition();

        if (this.isEnabled()) {
            g2d.setPaint(this.getPaint());

            // remember, strings are drawn lower left unlike rectangles and ellipses
            g2d.drawString(this.getText(), x-1, (int)(y - bounds.getY() - 1));
            // 9/29/2025 : The 1px adjustment "just looks better" but core geometry seems to be correct.
            /* I think there is a few pixel difference between this and
             * drawCenteredStringPrecise() in panelbuilder.bsh.  I am not sure
             * exactly why they are different.  I think I am going to have to
             * do image comparison using paint.net.  Surely, the difference
             * (if any) is due to integer/truncation in my implementation.
             */
        }

    }

    protected void drawString(Graphics2D g2d, String s, int tx, int ty, double theta, double rotx, double roty) {
        int w = g2d.getFontMetrics().stringWidth(s) + 5;
        int h = g2d.getFontMetrics().getHeight();
        int d = g2d.getFontMetrics().getDescent();

        AffineTransform aff = AffineTransform.getRotateInstance(theta, rotx, roty);
        aff.translate(tx, ty);

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.drawImage(createStringImage(s, w, h, d), aff, null);
        g2d.drawString(s, tx, ty);
    }

    /**
     * 
     * @param s the String
     * @param w the width
     * @param h the height
     * @param d the descent
     * @return a BufferedImage
     */
    protected BufferedImage createStringImage(String s, int w, int h, int d) {

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imageGraphics = image.createGraphics();
        imageGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        imageGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        imageGraphics.setPaint(this.getPaint());
        imageGraphics.setFont(this.getFont());
        imageGraphics.drawString(s, 0, h - d);
        imageGraphics.dispose();

        return image;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

}
