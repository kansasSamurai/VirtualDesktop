

/* ==============================================================
   Title: Create a vapp from a jpanel
   This is often useful, not only for your own prototypes,
   but for converting example from the internet into embedded examples.
   ==============================================================
*/

import javax.swing.JPanel
import org.jwellman.virtualdesktop.DesktopManager
JPanel yourpanel
// [see example below]
DesktopManager.get().createVApp(yourpanel, "yourIFrameTitle");

/* Example... Note the assignment of yourpanel

new SwingBuilder().edt {
    yourpanel = panel(
        visible: true,
        ) {
      borderLayout()
      ...
    }
...    
}

*/

