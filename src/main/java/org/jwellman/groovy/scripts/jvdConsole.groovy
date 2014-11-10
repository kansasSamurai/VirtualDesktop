
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
        this.run([
        rootContainerDelegate:{
            internalFrame(
                title: 'MyGroovyConsole',
                //location: [100,100], // in groovy 2.0 use platform default location
                //frameIcon: imageIcon("/groovy/ui/ConsoleIcon.png").image                
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
        menuBarDelegate: {arg->
            current.JMenuBar = build(arg)}
        ] );
        swing.consoleFrame.pack();
        swing.consoleFrame.setVisible(true);
        org.jwellman.virtualdesktop.App.getDesktop().add(swing.consoleFrame);
    }
}

console = new CustomConsole(); // groovy.ui.Console();
console.customRun();
// console.swing.consoleFrame.pack();
// console.swing.consoleFrame.show();
