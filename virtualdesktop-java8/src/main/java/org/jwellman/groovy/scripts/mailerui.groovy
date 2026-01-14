// This script modified but originally found at:
// http://groovy.codehaus.org/Swing+Builder

/**
 * This was tested using Groovy Console (via SpecGroovy) on TUE 12/31/2019;
 * the stack was (in maven coordinates):
 * - JDK8
 * - org.codehaus.groovy, groovy-all , 2.3.0
 *
 */

import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode as TreeNode
import groovy.swing.SwingBuilder

mboxes = [
    [name: "root@example.com", folders: [[name: "Inbox"], [name: "Trash"]]],
    [name: "test@foo.com", folders: [[name: "Inbox"], [name: "Trash"]]]
]
def swing = new SwingBuilder()
JTree mboxTree
swing.frame(
    title: 'Mailer',
    defaultCloseOperation: JFrame.DISPOSE_ON_CLOSE,
    size: [800, 600],
    show: true,
    locationRelativeTo: null) {

    // Do not set/adjust look and feel when running within JVD
    // lookAndFeel("system")

    menuBar() {
        menu(text: "File", mnemonic: 'F') {
            menuItem(text: "Exit", mnemonic: 'X', actionPerformed: {dispose() })
        }
    }

    splitPane {
        scrollPane(constraints: "left", preferredSize: [160, -1]) {
            mboxTree = tree(rootVisible: false)
        }
        splitPane(orientation:JSplitPane.VERTICAL_SPLIT, dividerLocation:280) {
            scrollPane(constraints: "top") { mailTable = table() }
            scrollPane(constraints: "bottom") { textArea() }
        }
    }
    ["From", "Date", "Subject"].each { mailTable.model.addColumn(it) }
}

mboxTree.model.root.removeAllChildren()
mboxes.each {mbox ->
    def node = new TreeNode(mbox.name)
    mbox.folders.each { folder -> node.add(new TreeNode(folder.name)) }
    mboxTree.model.root.add(node)
}
mboxTree.model.reload(mboxTree.model.root)
