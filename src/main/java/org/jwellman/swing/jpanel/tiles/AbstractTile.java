package org.jwellman.swing.jpanel.tiles;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Point2D;

/**
 * A base class for all Tiles.
 * <p>
 * All Tiles are defined by a rectangular region.
 * I am going to try to define Tiles in cartesian coordinates
 * instead of screen coordinates since that is a more natural
 * way to construct shapes.  Java2D can be used to transform
 * when it comes time to draw it.
 * 
 * @author rwellman
 *
 */
public abstract class AbstractTile implements Tile {

    /** The top left x coordinate */
    protected int x;

    /** The top left y coordinate */
    protected int y;

    /** The width */
    protected int width;

    /** The height */
    protected int height;

    /** The fill paint. If null, the Tile shape will not be filled. */
    protected Paint paint;

    /** The paint for the border. If null, a border will not be painted. */
    protected Paint borderPaint;

    /** The stroke for the border. */
    protected Stroke borderStroke;

    /** The next Tile to draw */
    protected Tile next;

    /** The previous Tile drawn */
    protected Tile prev;

    /** This tile's anchor point; default to bottom left */
    protected Tile.Anchor anchorMe = Tile.Anchor.BOTTOM_LEFT;

    /** The anchor point of the anchorTo Tile (does not have to be the previous Tile) */
    protected Tile.Anchor anchorPoint;

    /** The Tile that defines where to anchor this Tile */
    protected Tile anchorTo;

    /** An absolute point to anchor this Tile */
    protected Point2D fixedAnchor;

    /** Allow a tile to be disabled */
    protected boolean enabled = true;

    // used to optionally draw debug borders during development
    public static boolean DEBUG = false;

    /** Adjust y calculations by positive(1) when cartesian coordinates, or negative(-1) when screen coordinates */
    protected int yadjust = -1;

    /**
     * Calculate the x and y coordinates of the top left corner for drawing.
     * First, adjust based on the Tile anchored to.
     * Then, adjust based on this Tile's anchor.
     */
    protected final void calcPosition() {

        if (fixedAnchor != null) {
            x = (int) fixedAnchor.getX();
            y = (int) fixedAnchor.getY();
        } else {
            switch (anchorPoint) {
            case BOTTOM_LEFT:
                x = anchorTo.getX();
                y = anchorTo.getY() - anchorTo.getHeight() * yadjust;
                break;
            case BOTTOM_MIDDLE:
                x = anchorTo.getX() + (anchorTo.getWidth()/2);
                y = anchorTo.getY() - anchorTo.getHeight() * yadjust;
                break;
            case BOTTOM_RIGHT:
                x = anchorTo.getX() + anchorTo.getWidth();
                y = anchorTo.getY() - anchorTo.getHeight() * yadjust;
                break;
            case CENTER:
                x = anchorTo.getX() + (anchorTo.getWidth()/2);
                y = anchorTo.getY() - (anchorTo.getHeight()/2) * yadjust;
                break;
            case MIDDLE_LEFT:
                x = anchorTo.getX();
                y = anchorTo.getY() - (anchorTo.getHeight()/2) * yadjust;
                break;
            case MIDDLE_RIGHT:
                x = anchorTo.getX() + anchorTo.getWidth();
                y = anchorTo.getY() - (anchorTo.getHeight()/2) * yadjust;
                break;
            case TOP_LEFT:
                x = anchorTo.getX();
                y = anchorTo.getY();
                break;
            case TOP_MIDDLE:
                x = anchorTo.getX() + (anchorTo.getWidth()/2);
                y = anchorTo.getY();
                break;
            case TOP_RIGHT:
                x = anchorTo.getX() + anchorTo.getWidth();
                y = anchorTo.getY();
                break;
            }
        }

        // This Tile must always have an anchor.
        switch (anchorMe) {
        case TOP_LEFT:
            // x stays the same when left
            // y stays the same when top
            break;
        case MIDDLE_LEFT:
            // x stays the same when left
            y += (getHeight()/2) * yadjust;
            break;
        case BOTTOM_LEFT:
            // x stays the same when left
            y += getHeight() * yadjust;
            break;
        case TOP_RIGHT:
            x -= getWidth();
            // y stays the same when top
            break;
        case MIDDLE_RIGHT:
            x -= getWidth();
            y += (getHeight()/2) * yadjust;
            break;
        case BOTTOM_RIGHT:
            x -= getWidth();
            y += getHeight() * yadjust;
            break;
        case TOP_MIDDLE:
            x -= getWidth()/2;
            // y stays the same when top
            break;
        case CENTER:
            x -= getWidth()/2;
            y += (getHeight()/2) * yadjust;
            break;
        case BOTTOM_MIDDLE:
            x -= getWidth()/2;
            y += getHeight() * yadjust;
            break;
        }

    }

    @Override
    public final void drawTile(Graphics2D g2) {
        this.draw(g2);

        if (DEBUG) {
            g2.setPaint(Color.red);
            g2.drawRect(x, y, width, height);
        }

        if (next != null) {
            next.drawTile(g2);
        }

    }

    @Override
    public void draw(Graphics2D g2) {
        // Since this will be overridden by implementing classes,
        // just draw a cyan square with a black border.
        this.calcPosition();

        g2.setPaint(Color.cyan);
        g2.fillRect(x, y, width, height);

        g2.setPaint(Color.black);
        g2.drawRect(x, y, width, height);
    }

    public AbstractTile(int w, int h) {
        this.width = w;
        this.height = h;
    }

    public Tile anchorTo(int x, int y) {
        this.fixedAnchor = new Point2D.Double(x, y);
        return this;
    }

    public Tile anchorTo(Tile tile, Tile.Anchor anchor) {
        this.anchorTo = tile;
        this.anchorPoint = anchor;
        return this;
    }

    public Tile anchorTopLeft() {
        this.anchorMe = Tile.Anchor.TOP_LEFT;
        return this;
    }

    public Tile anchorTopMiddle() {
        this.anchorMe = Tile.Anchor.TOP_MIDDLE;
        return this;
    }

    public Tile anchorTopRight() {
        this.anchorMe = Tile.Anchor.TOP_RIGHT;
        return this;
    }

    public Tile anchorMiddleLeft() {
        this.anchorMe = Tile.Anchor.MIDDLE_LEFT;
        return this;
    }

    public Tile anchorCenter() {
        this.anchorMe = Tile.Anchor.CENTER;
        return this;
    }

    public Tile anchorMiddleRight() {
        this.anchorMe = Tile.Anchor.MIDDLE_RIGHT;
        return this;
    }

    public Tile anchorBottomLeft() {
        this.anchorMe = Tile.Anchor.BOTTOM_LEFT;
        return this;
    }

    public Tile anchorBottomMiddle() {
        this.anchorMe = Tile.Anchor.BOTTOM_MIDDLE;
        return this;
    }

    public Tile anchorBottomRight() {
        this.anchorMe = Tile.Anchor.BOTTOM_RIGHT;
        return this;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    public Paint getPaint() {
        return paint;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public Tile getNext() {
        return next;
    }

    public void setNext(Tile next) {
        this.next = next;
    }

    public Tile getPrev() {
        return prev;
    }

    public void setPrev(Tile prev) {
        this.prev = prev;
        prev.setNext(this);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Paint getBorderPaint() {
        return borderPaint;
    }

    public void setBorderPaint(Paint borderPaint) {
        this.borderPaint = borderPaint;
    }

}
