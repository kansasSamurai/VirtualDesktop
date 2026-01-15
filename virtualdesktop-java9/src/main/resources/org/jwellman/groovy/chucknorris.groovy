import groovy.json.JsonSlurper
import groovy.swing.SwingBuilder

import org.jwellman.virtualdesktop.DesktopManager

import javax.swing.JPanel
import javax.swing.WindowConstants as WC
import java.awt.BorderLayout as BL
import java.awt.Color
import java.awt.Font

// https://nofluffjuststuff.com/blog/kenneth_kousen/2013/07/a_groovy_chuck_norris_script

String base1 = 'http://api.icndb.com/jokes/random?limitTo=[nerdy]'
def json1 = new JsonSlurper().parseText(base1.toURL().text)
println json1?.value?.joke

// ------------------------------------------------

String startHTML = "<html><body style='width: 100%'>"
String endHTML = '</body></html>'

String base = 'http://api.icndb.com/jokes/random?limitTo=[nerdy]'
def slurper = new JsonSlurper()

JPanel mypanel

new SwingBuilder().edt {
//    frame(title:'ICNDB', visible: true, pack: true,
//        defaultCloseOperation:WC.DISPOSE_ON_CLOSE) {
        mypanel = panel(layout:new BL(), preferredSize:[300, 250], background: Color.WHITE) {
                
            // see [Note 1]
            scrollPane {
                textArea('Welcome to ICNDB', 
                    constraints:BL.NORTH,
                    font: new Font('Serif', Font.PLAIN, 24),
                    lineWrap: true, wrapStyleWord: true, editable: false,
                    id: 'textArea'
                    )
            }

            button('Get Joke', constraints:BL.SOUTH,
                actionPerformed: {
                    doOutside {
                        def json = slurper.parseText(base.toURL().text)
                        doLater { 
                            // [Note 1] By giving the label/textarea an id attribute, I was able 
                            // to access it inside the doLater closure using the value of the id. 
                            // label.text = "${startHTML}${json?.value?.joke}${endHTML}"
                            textArea.text = json?.value?.joke
                        }
                    }
                }
            )
        }
//    }
}

DesktopManager.get().createVApp(mypanel, "Chuck Norris ...");
