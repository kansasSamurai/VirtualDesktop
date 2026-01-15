// https://www.freeplane.org/wiki/index.php/Scripting:_Example_scripts_using_external_libraries

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.data.general.DefaultPieDataset
import groovy.swing.SwingBuilder
import java.awt.*
import javax.swing.WindowConstants as WC

def piedataset = new DefaultPieDataset();
piedataset.with {
     setValue "Apr", 10
     setValue "May", 30
     setValue "June", 40
}

import javax.swing.JPanel
import org.jwellman.virtualdesktop.DesktopManager
JPanel yourpanel

def options = [true, true, true]
def chart = ChartFactory.createPieChart("Pie Chart Sample", piedataset, *options)
chart.backgroundPaint = Color.white

def swing = new SwingBuilder()
swing.edt {
//    frame(title:'Groovy PieChart', defaultCloseOperation:WC.DISPOSE_ON_CLOSE) {
        yourpanel = panel(id:'canvas') { widget(new ChartPanel(chart)) }
//    }
} // end swing.edt
//frame.pack()
//frame.show()

DesktopManager.get().createVApp(yourpanel, "Pie Chart Demo");
