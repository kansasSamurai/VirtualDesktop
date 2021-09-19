package org.jwellman.swing.shapes;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

/**
 * A Java2D triangle.
 * 
 * @author rwellman
 *
 */
public class TriangleShape extends Path2D.Double {
    
    public TriangleShape ( Point2D p1 , Point2D p2 , Point2D p3 ) {
        moveTo(p1.getX(), p1.getY()) ;
        lineTo(p2.getX(), p2.getY()) ;
        lineTo(p3.getX(), p3.getY()) ;
        closePath() ;
    }

    private static final long serialVersionUID = 1L ;

}
