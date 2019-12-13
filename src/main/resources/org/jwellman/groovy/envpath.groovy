// http://codetojoy.blogspot.com/2007/08/groovy-swingbuilder-and-secret-methods.html

import java.awt.*
import javax.swing.*
import javax.swing.table.*
import groovy.swing.SwingBuilder

class MyTableCellRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(
                        JTable table, Object value,
                        boolean isSelected, boolean hasFocus,
                        int rowIndex, int vColIndex) {
        
        // println(value)
        setText(value.toString());

        File file = new File(value); // println("file: " + file)
        if( file.exists() ) { 
            setForeground(Color.black)
        } else {
            setForeground(Color.red) 
        }

        return this
    }
}

///////////////////////////////////////////////////////////////
// static public void main(String[] args)
//
// args[0] = env var to view

final String DELIMITER = ';'   // change this for Unix

String[] args = [ 'PATH' ] // normally this comes from main(); see above

// Build data
String[] columns = [ args[0] ]

String envVar = System.getenv( args[0] ); println(envVar);
String[] values = envVar.split(DELIMITER)
Object[][] data = new String[values.length]; println(values.length);
for( i in 0..values.length-1 ) {
    String[] row = new String[1]; row[0] = values[i]
    data[i] = row
}

// Build GUI. Note that this is quick-n-dirty stuff,
// that illustrates a SwingBuilder more than proper
// Swing techniques.

JTable table = new JTable(data, columns)
def renderer = new MyTableCellRenderer()
table.columnModel.getColumn(0).setCellRenderer(renderer)
JScrollPane scrollPane = new JScrollPane(table)

import org.jwellman.virtualdesktop.DesktopManager
JPanel yourpanel

// def JFrame gui
def builder = new SwingBuilder()
builder.edt {
    yourpanel = //frame( title:'Code to Joy', size:[520,500] ) {
    panel( layout: new BorderLayout() ) {
       widget(scrollPane, constraints: BorderLayout.CENTER)   // THIS is a revelation
   }
 // }
}

// gui.pack(); gui.show()
DesktopManager.get().createVApp(yourpanel, "Environment PATH");
