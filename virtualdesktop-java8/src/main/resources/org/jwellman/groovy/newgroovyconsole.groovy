import groovy.ui.Console;
import java.awt.Dimension;
import javax.swing.*;
import org.jwellman.virtualdesktop.DesktopManager;
import org.jwellman.virtualdesktop.vapps.VirtualAppSpec;

/**
 *
 * TODO:  the current implementation of createvapp with internalfameprovider = true
 * will create a default internal iframe that is useless and therefore excess;
 * need to rethink this.
 * 
 * TODO:  using createvapp with the populateinternalframe() callback does not
 * have a mechanism to associate the iframe created in this class to the 
 * desktop's JList.
 * 
 */
class NewGroovyConsole extends VirtualAppSpec {
        
    private Console console;

    def frameConsoleDelegates = [
        rootContainerDelegate:{
            internalFrame(
                title: 'JPAD Groovy Console',
                closable: true,
                resizable: true,
                iconifiable: true,
                maximizable: true,
                //location: [100,100], // in groovy 2.0 use platform default location
                //iconImage: imageIcon('/groovy/ui/ConsoleIcon.png').image,
                frameIcon: imageIcon('/groovy/ui/ConsoleIcon.png'),
                // this does not work (yet?): .getBufferedImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH),
                defaultCloseOperation: JInternalFrame.HIDE_ON_CLOSE,
            ) {
                try {
                    current.locationByPlatform = true
                } catch (Exception e) {
                    current.location = [100, 100] // for 1.4 compatibility
                }
                // containingWindows += current
            }
        },
        menuBarDelegate: {arg->
            current.JMenuBar = build(arg)}
    ];
    
    public NewGroovyConsole() {
        super();
        
        // the framework handles the title in both scenarios regarding 'internalFrameProvider'
        this.setTitle("default");
        
        this.internalFrameProvider = true;
        if (this.internalFrameProvider) {
            // do nothing here... it will be done in populateInternalFrame()
        } else {
        }
    }
    
    public void populateInternalFrame(JInternalFrame frame, JDesktopPane desktop) {
        System.out.println("populateInternalFrame()");

            // the frame has already been added to the desktop...
            frame.setPreferredSize(new Dimension(200, 300));
            frame.pack();
            frame.setVisible(true);
            
            // Override the updateTitle() method to prevent... well, updating the frame title
            // ... should the groovy class be updated to do more, this may have to be redesigned.
            Console.metaClass.updateTitle = {}

            Console c = new Console(); c.run(frameConsoleDelegates);
            def iframe = c.frame;
            iframe.setPreferredSize(new Dimension(200, 300));
            iframe.setFrameIcon(new ImageIcon( iframe.getFrameIcon().getImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH) ));
            desktop.add(iframe);
            
            iframe.pack();
            iframe.setVisible(true);
        
    }
}

DesktopManager.get().createVApp(new NewGroovyConsole());
