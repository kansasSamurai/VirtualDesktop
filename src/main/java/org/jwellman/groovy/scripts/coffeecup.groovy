
// http://jroller.com/aalmiray/tags/graphicsbuilder
antialias on  
background( 'black' )  
  
group( id: 'plategroup', bc: 'none' ) {  
  // gray ellipse under the plate  
  ellipse( cx: 0, cy: 10, rx: 160, ry: 50, f: color('darkGray').brighter() )  
    
  // thin 'lip' of the plate (provides a sense of 3D)  
  ellipse( cx: 0, cy: 3, rx: 170, ry: 50, f: 'lavender' )  
    
  // large plate  
  ellipse( cx: 0, cy: 0, rx: 170, ry: 50 ) {  
    radialGradient( cx: 1, cy: 1, fy: 100, r: 270 ) {  
      stop( c: 'whiteSmoke', s: 0 )  
      stop( c: 'lightGray',  s: 0.5 )  
      stop( c: 'darkGray',   s: 1 )  
    }  
  }  
    
  // recessed plate center  
  ellipse( cx: 0, cy: 5, rx: 90, ry: 22 ) {  
    radialGradient( cx: 1, cy: 1, fy: 20, r: 180 ) {  
      stop( c: 'black', s: 0 )  
      stop( c: 'lightGray', s: 0.4 )  
      stop( c: color('white').darker(), s: 1 )  
    }  
  }  
    
  transformations {  
    translate( x: 250, y: 300 )  
  }  
}  
  
group( id: 'cupgroup', bc: 'none' ) {  
  subtract( id: 'cup', asShape: yes ) {  
    circle( cx: 100, cy: 100, r: 50 )   
    rect( x: 25, y:50, w: 150, h: 50 )   
  }  
  draw( cup, keepTrans: yes ) {  
    radialGradient( cx: 0.4, cy: 0,  r: 100 ) {  
      stop( c: 'whiteSmoke', s: 0 )  
      stop( c: color('whiteSmoke').darker(), s: 1 )  
    }  
  }  
    
  // outer rim  
  ellipse( cx: 100, cy: 100, rx: 50, ry: 8, f: 'white' )  
    
  // inner rim  
  ellipse( cx: 100, cy: 100, rx: 48, ry: 7, id: 'innerRim' ) {  
    linearGradient( y2: 1 ){  
      stop( c: 'whiteSmoke', s: 0 )  
      stop( c: color('whiteSmoke').darker(), s: 1 )  
    }  
  }  
    
  intersect( f: color('darkOrange').darker().darker() ) {  
    shape( innerRim )  
    ellipse( cx: 100, cy: 102, rx: 46, ry: 6 )  
  }  
  
  transformations {  
    scale( x: 3, y: 4 )  
    translate( x: -16, y: -70 )  
  }  
}  
  
transformations {  
  translate( x: -50, y: -50 )  
}  