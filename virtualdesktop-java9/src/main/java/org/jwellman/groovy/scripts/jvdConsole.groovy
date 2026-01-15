
import javax.swing.JInternalFrame

class CustomIFrameFactory extends groovy.swing.factory.RootPaneContainerFactory {

    public Object newInstance(FactoryBuilderSupport builder, Object name, Object value, Map attributes) throws InstantiationException, IllegalAccessException {
        if (FactoryBuilderSupport.checkValueIsType(value, name, JInternalFrame)) {
            return value;
        }
        JInternalFrame frame = new JInternalFrame("[title-goes-here]", true, true, true, true);

        return frame;
    }
}


class CustomConsole extends groovy.ui.Console {

def iframeDelegates = [
        rootContainerDelegate:{
            internalFrame(
                title: 'GroovyConsole',
                //location: [100,100], // in groovy 2.0 use platform default location
                //frameIcon: imageIcon("/groovy/ui/ConsoleIcon.png").image
            ) {
//                try {
////                    current.locationByPlatform = true
//                    current.location = [100, 100] // for 1.4 compatibility
//                } catch (Exception e) {
//                    current.location = [100, 100] // for 1.4 compatibility
//                }
                containingWindows += current
            }
        },
        menuBarDelegate: {arg->
            current.JMenuBar = build(arg)}
    ];

    void customRun() {

        // This currently won't work because the 'swing' object is null :(
        // swing.registerBeanFactory("customFrame", CustomIFrameFactory )

        this.run([
        rootContainerDelegate:{
            internalFrame(
                title: 'MyGroovyConsole',
                // defaultCloseOperation: JInternalFrame.DO_NOTHING_ON_CLOSE,
                // location: [100,100], // in groovy 2.0 use platform default location
                // frameIcon: imageIcon("/groovy/ui/ConsoleIcon.png").image
            ) {
//                try {
////                    current.locationByPlatform = true
//                    current.location = [100, 100] // for 1.4 compatibility
//                } catch (Exception e) {
//                    current.location = [100, 100] // for 1.4 compatibility
//                }
//                containingWindows += current
            }
        },
        menuBarDelegate: {arg-> current.JMenuBar = build(arg)}
        ] );
//        swing.consoleFrame.setClosable(true);
        swing.consoleFrame.setResizable(true);
        swing.consoleFrame.setIconifiable(true);
//        swing.consoleFrame.setMaximizable(true);

        org.jwellman.virtualdesktop.App.getVSystem().getDesktop().add(swing.consoleFrame);
        // org.jwellman.virtualdesktop.App.getVSystem().createVApp(swing.consoleFrame);
        swing.consoleFrame.pack();
        swing.consoleFrame.setVisible(true);
    }
}

gconsole = new CustomConsole(); // groovy.ui.Console();
gconsole.customRun();
// console.swing.consoleFrame.pack();
// console.swing.consoleFrame.show();


// =======================
/* This is not quite working yet but I want to make sure I capture the current state:
import groovy.swing.SwingBuilder;
import java.awt.BorderLayout as BL

class DesktopFactory extends AbstractFactory {
    public Object newInstance( FactoryBuilderSupport builder, Object name, Object value, Map properties )
    throws InstantiationException, IllegalAccessException {
       return org.jwellman.virtualdesktop.App.getVSystem().getDesktop()
    }
}

def swing = new SwingBuilder()
swing.registerFactory( "desktop", new DesktopFactory() )

def iframe = swing.edt {
    desktop {
        internalFrame(visible: true, title: 'MyGroovyConsole', bounds: [25, 25, 200, 100]) {
            borderLayout()
            textlabel = label(text: 'Click the button!', constraints: BL.NORTH)
            button(text:'Click Me', constraints: BL.SOUTH)
        }
    }
}
*/
// org.jwellman.virtualdesktop.App.getVSystem().getDesktop().add(iframe);

