import groovy.swing.SwingBuilder
import groovy.swing.j2d.*
import java.awt.BorderLayout as BL
import java.awt.Color
import java.awt.Font
import javax.swing.JFrame

// https://uberconf.com/blog/andres_almiray/2007/12/draw_the_groovy_logo_with_graphicsbuilder
Font afont = new Font("Alfredo", Font.BOLD, 48) // new Font("Baby Kruffy", Font.PLAIN, 36)
Color gblue = new Color(118,167,183) // fill: [118,167,183] as Color

// This script requires GraphicsBuilder (not yet JVD integrated)
// available at: http://docs.codehaus.org/display/GROOVY/GraphicsBuilder
def gb = new GraphicsBuilder()

// load the swingtime font, get it? Swing
def fontFile = new File("SWINM___.TTF")

// turn on antialias: antialias( 'on' )

def graphicsOperation = gb.group {

    antialias on

    rect( id: 'preview', x: 0, y: 0, w: 300, h: 530, f: gblue )

    // create the Groovy star
    star( ir: 40, or: 100, cx: 160, cy: 120,
                borderWidth: 4, borderColor: 'white', fill: gblue ) {
       transformations {
          scale( y: 0.6 )
       }
    }

//    font( Font.createFont(Font.TRUETYPE_FONT,fontFile).deriveFont(50.0f) )
    font( afont )

    // draw the text
    // text( text: 'Groovy', borderWidth: 4, fill: 'white' ) {
    text( text: 'Groovy', borderWidth: 12, borderColor: 'white', fill: gblue ) {
       transformations {
           translate( x:80, y:30 )
           scale( x: 1.1, y: 1.5 )
        }
    }

    // draw the text
    // text( text: 'Groovy', borderWidth: 4, fill: 'white' ) {
    text( text: 'Groovy', borderWidth: 1, borderColor: gblue, fill: gblue ) {
       transformations {
           translate( x:80, y:30 )
           scale( x: 1.1, y: 1.5 )
        }
    }

}

SwingBuilder.build {
   frame( title: 'GraphicsBuilder',
          size: [240,250],
          defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
          visible: true ) {
      panel( new GraphicsPanel(), graphicsOperation: graphicsOperation )
   }
}
