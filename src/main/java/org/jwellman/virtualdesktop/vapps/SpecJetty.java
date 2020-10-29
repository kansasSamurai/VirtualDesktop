package org.jwellman.virtualdesktop.vapps;

import static j2html.TagCreator.*;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.Resource;

/**
 * 
 * @author Rick Wellman
 *
 */
public class SpecJetty extends VirtualAppSpec {

	private Server server;
	private JButton btnStart;
	private JTextField txtName, txtPort;
	
	public SpecJetty() {
		super();		
		this.setTitle("Jetty Dashboard");
		
		this.setContent(this.createContent());
	}

	private JPanel createContent() {
		final JPanel content = new JPanel(new GridLayout(0, 1));

		// reusable jpanel ref for creating rows
		JPanel row = new JPanel();
		row.add(new JLabel("Name: "));
		row.add(txtName = new JTextField("Jetty", 15));
		content.add(row);
		// TODO create a keylistener on the textfield to update a tabname

		row = new JPanel();
		row.add(new JLabel("Port: "));
		row.add(txtPort = new JTextField("8080", 5));
		content.add(row);

		row = new JPanel();
		row.add(btnStart = new JButton("Start"));
		content.add(row);
			btnStart.addActionListener(new StartServerAction());
		
		return content;
	}
	
	private class StartServerAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
			if (server == null) {
				
				int port = Integer.parseInt(txtPort.getText());
		        Path userDir = Paths.get(System.getProperty("user.dir"));
		        PathResource pathResource = new PathResource(userDir);

		        try {
			        server = createServer(port, pathResource);
			        // server.setHandler(new HelloWorld());
			        server.setDumpAfterStart(true);
					server.start();
				} catch (Exception e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(
							getContent(),
						    ExceptionUtils.getStackTrace(e1),
						    "Error",
						    JOptionPane.ERROR_MESSAGE);
				}							
			} else {
				JOptionPane.showMessageDialog(
						getContent(),
					    "The Jetty server has already been started.",
					    "Action Ignored",
					    JOptionPane.WARNING_MESSAGE);
			}
		}
		
	}

	// https://www.eclipse.org/jetty/documentation/current/embedded-examples.html
	public Server createServer(int port, Resource baseResource) throws Exception {
		
        // Create a basic Jetty server object that will listen on port 8080.  Note that if you set this to port 0
        // then a randomly available port will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
        Server server = new Server(port);

        // Create the ResourceHandler. It is the object that will actually handle the request for a given file. It is
        // a Jetty Handler object so it is suitable for chaining with other handlers as you will see in other examples.
        ResourceHandler resourceHandler = new ResourceHandler();

        // Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
        // In this example it is the current directory but it can be configured to anything that the jvm has access to.
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setBaseResource(baseResource);

        // Add the ResourceHandler to the server.
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resourceHandler, new HelloWorld(), new DefaultHandler()});
        server.setHandler(handlers);

        return server;
    }

	/**
	 * http://localhost:8080/helloworld
	 * 
	 * @author rcwel
	 *
	 */
	private class HelloWorld extends AbstractHandler {
		
	    @Override
	    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) 
	    		throws IOException, ServletException {
	    	
	        // Declare response encoding and types
	        response.setContentType("text/html; charset=utf-8");

	        // Write back response
	        // This was originally just the following...
	        // response.getWriter().println("<h1>Hello Jetty User</h1>");
	        
	        // ... but I incorporated the j2html API to produce this:
	        response.getWriter().println(
		        html(
		        	    head(
		        	        title("Hello World")
		        	        //, link().withRel("stylesheet").withHref("/css/main.css")
		        	    ),
		        	    body(
		        	    	h1("Hello Jetty User")
		        	    	,p("Your request was served at: " + java.time.LocalDateTime.now())
		        	    )
		        	).render()
	        );

	        // Declare response status code
	        response.setStatus(HttpServletResponse.SC_OK);
	        
	        // Inform jetty that this request has now been handled
	        baseRequest.setHandled(true);
	    }
	    
	} // end class HelloWorld
	
}
