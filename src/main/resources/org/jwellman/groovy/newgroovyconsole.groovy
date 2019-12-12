import groovy.ui.Console;
import java.awt.Dimension;
import javax.swing.*;
import org.jwellman.virtualdesktop.DesktopManager;
import org.jwellman.virtualdesktop.vapps.VirtualAppSpec;

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

		boolean trySomethingElse = true;
		if (trySomethingElse) {

			// the frame has already been added to the desktop...
			frame.setPreferredSize(new Dimension(200, 300));
			frame.pack();
			frame.setVisible(true);
			
			Console c = new Console(); c.run(frameConsoleDelegates);
			def iframe = c.frame;
			desktop.add(iframe);
			iframe.setPreferredSize(new Dimension(200, 300));
			iframe.pack();
			iframe.setVisible(true);

		} else {

			// This works ...
			Console c = new Console(); c.run();
			final JFrame jframe = (JFrame) c.getFrame();
			this.setContent(this.createDefaultContent(jframe.getContentPane()));
			
			frame.add(this.getContent());
			  frame.setJMenuBar(jframe.getRootPane().getJMenuBar());
			// DONE apparently the groovy .png is "big"... resize to look better in the DesktopManager
			frame.setFrameIcon(new ImageIcon( jframe.getIconImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH) ));
			

		}

		
	}
}

DesktopManager.get().createVApp(new NewGroovyConsole());
