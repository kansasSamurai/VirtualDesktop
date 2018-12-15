// Adapted from - http://groovy.329449.n5.nabble.com/GraphicsPad-part-of-GraphicsBuilder-how-does-it-differ-from-GroovyConsole-td369964.html
import groovy.swing.SwingBuilder
import groovy.swing.j2d.GraphicsBuilder
import groovy.swing.j2d.GraphicsPanel
import java.awt.BorderLayout as BL

def gb = new GraphicsBuilder()

def shapes = [
  'Arrow': gb.group {
     antialias on
     arrow( x: 20, y:  20, width: 100, height:  60, fill: 'red', borderColor: 'black' ) /*rise: 0.5, depth: 0.5*/
  },
  'Arrows': gb.group {
     antialias on
     arrow( x: 20, y: 120, width: 100, height:  60, fill: 'red', borderColor: 'black' ) /*rise: 0.5, depth: 0.5*/
     arrow( x: 20, y: 220, width: 200, height: 120, fill: 'red', borderColor: 'black' ) /*rise: 0.5, depth: 0.5*/
  },
//  'Surprise': gb.group {
//   antialias on
//   rect(x:20, y:40, width:180, height:130, arcWidth:60, arcHeight:60, borderColor:false) {
//     linearGradient {
//       stop(offset:0,   color:'red')
//       stop(offset:0.5, color:'orange')
//       stop(offset:1,   color:'red')
//     }
//     filters(offset: 50) {
//       weave()
//       kaleidoscope(sides:10, angle:0, angle2:0)
//       lights() {
//          ambientLight( color: color('white').rgb(),
//                        centreX: 0, centreY: 0 )
//       }
//       dropShadow()
//     }
//   }
//  }
]

SwingBuilder.build {
  frame( title: 'jSilhouette Shapes (Groovy)',
    size: [500,400],
    show: true ) {
    borderLayout()
    widget( new GraphicsPanel(), id: 'canvas', constraints: BL.CENTER )
    panel( constraints: BL.WEST) {
      gridLayout( columns: 1, rows: 0 )
      shapes.each { shape, graphicsOperation ->
        button( shape, actionPerformed: { evt ->
          canvas.graphicsOperation = graphicsOperation
        })
      }
    }
  }
}