package org.jwellman.swing.jpanel.tiles;

import java.awt.Graphics2D;

public interface Tile {

    /**
     * This is the customizable draw() method.
     * 
     * @param g2
     */
    void draw(Graphics2D g2);

    /**
     * This is the framework method implemented by the abstract base class.
     * 
     * @param g2
     */
    void drawTile(Graphics2D g2);

    /** The anchor points of a rectangle. */
    enum Anchor {
        TOP_LEFT,
        TOP_MIDDLE,
        TOP_RIGHT,
        MIDDLE_LEFT,
        CENTER,
        MIDDLE_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_MIDDLE,
        BOTTOM_RIGHT
    }

    int getX();

    void setX(int x);

    int getY();

    void setY(int y);

    int getWidth();

    void setWidth(int width);

    int getHeight();

    void setHeight(int height);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    public Tile getPrev();

    public void setPrev(Tile prev);

    public Tile getNext();

    public void setNext(Tile next);

}
