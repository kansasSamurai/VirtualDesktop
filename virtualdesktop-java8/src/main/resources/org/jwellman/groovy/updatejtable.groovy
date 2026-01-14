// -------------------------
// https://varomorf.wordpress.com/2014/09/22/update-jtable-using-groovy/
// -------------------------

// -------------------------
// ... this is the common code between original and modified for jpad versions
// -------------------------

import groovy.swing.SwingBuilder

import java.awt.BorderLayout

import javax.swing.JFrame
import javax.swing.JTable

JTable theTable

def reset = {
  model = (1..10).collect{
    ['name':it, 'number':it]
  }
  theTable.model.rowsModel.value = model
  theTable.model.fireTableDataChanged()
}

def add = {
  def newEntry = ['name':'Foo', 'number':123456]
  theTable.model.rowsModel.value.add(newEntry)
  theTable.model.fireTableDataChanged()
}

// -------------------------
// this version is compatible with jpad
// -------------------------
// https://varomorf.wordpress.com/2014/09/22/update-jtable-using-groovy/

import org.jwellman.virtualdesktop.DesktopManager

new SwingBuilder().edt {
    // giframe := groovy internal frame (JInternalFrame)
    giframe = panel(
        // title: 'Table refresh',
        visible: true,
        // id: "giframe" // (g)roovy (i)nternal (frame)
        // The next two properties only apply when using JFrame:
        // pack :true,
        // defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE
        ){
      borderLayout()

      panel(constraints:BorderLayout.SOUTH){
        button(text:'Add', actionPerformed:add)
        button(text:'Reset', actionPerformed:reset)
      }

      panel(constraints:BorderLayout.CENTER){
        scrollPane(preferredSize:[300, 300]){
          theTable = table(){
            tableModel(){
              closureColumn(header:'Name', read:{it.name})
              closureColumn(header:'Number', read:{it.number})
            }
          }
        }
      }
    }

}

DesktopManager.get().createVApp(giframe, "Table Refresh Demo");





// -------------------------
// this is the original version; in a JFrame
// -------------------------

new SwingBuilder().frame(title:'Table refresh',visible:true,pack:true,
    defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE){
  borderLayout()
  panel(constraints:BorderLayout.SOUTH){
    button(text:'Add', actionPerformed:add)
    button(text:'Reset', actionPerformed:reset)
  }
  panel(constraints:BorderLayout.CENTER){
    scrollPane(preferredSize:[600, 600]){
      theTable = table(){
        tableModel(){
          closureColumn(header:'Name', read:{it.name})
          closureColumn(header:'Number', read:{it.number})
        }
      }
    }
  }
}

