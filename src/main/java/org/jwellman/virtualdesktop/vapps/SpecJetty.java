package org.jwellman.virtualdesktop.vapps;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.p;
import static j2html.TagCreator.title;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

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
import org.jwellman.swing.layout.FluidConstraint;
import org.jwellman.swing.layout.FluidLayout;
import org.jwellman.swing.layout.WrapLayout;

/**
 * 
 * @author Rick Wellman
 *
 */
public class SpecJetty extends VirtualAppSpec {

	private Server server;
	private JButton btnStart;

	private JLabel lblName, lblPort;
	private JTextField txtName, txtPort;
	
	public SpecJetty() {
		super();		
		this.setTitle("Jetty Dashboard");
		
		this.setContent(this.createContent());
	}

	private JPanel createContent() {
		final JPanel content = new JPanel();

		lblName = new JLabel("Name: ");
		txtName = new JTextField("ServerName");
		// TODO create a keylistener on the textfield to update a tabname

		lblPort = new JLabel("Port: ");
		txtPort = new JTextField("8080");

		btnStart = new JButton("Start");
		btnStart.addActionListener(new StartServerAction());

		int version = 4;
		switch(version) {
			case 1: layoutPanelsInGrid(content); break;
			case 2: layoutFluidInBox(content); break;
			case 3: layoutFluidInBorder(content); break;
			case 4: layoutFlow(content); break;
		}

		return content;
	}

	private void layoutPanelsInGrid(JPanel content) {
		content.setLayout(new GridLayout(0,1));
		
		JPanel row = new JPanel();
		row.add(new JLabel("Name: "));
		row.add(txtName = new JTextField("Jetty", 15));
		content.add(row);

		row = new JPanel();
		row.add(new JLabel("Port: "));
		row.add(txtPort = new JTextField("8080", 5));
		content.add(row);
		
		row = new JPanel();
		row.add(btnStart = new JButton("Start"));
		content.add(row);
	}

	private void layoutFlow(JPanel content) {
		WrapLayout fl = new WrapLayout();
		content.setLayout(fl);

		// reusable jpanel ref for creating rows
		JPanel row = new JPanel();
		row.add(lblName);
		row.add(txtName);
		content.add(row);

		row = new JPanel();
		row.add(lblPort);
		row.add(txtPort);
		content.add(row);

		row = new JPanel();
		row.add(btnStart);
		content.add(row);
	}

	private void layoutFluidInBox(JPanel content) {
		BoxLayout b = new BoxLayout(content, BoxLayout.PAGE_AXIS);
		content.setLayout(b);

		FluidLayout fluid = new FluidLayout();
		FluidConstraint clabel = new FluidConstraint(12,4,4,3,2);
		FluidConstraint ctext = new FluidConstraint(12,8,8,3,2);
		FluidConstraint cbutton = new FluidConstraint(12,12,12,12,4);
		
		// reusable jpanel ref for creating rows
		JPanel row = panel(); 
			row.setLayout(fluid);
		row.add(lblName, clabel);
		row.add(txtName, ctext);
		row.add(lblPort, clabel);
		row.add(txtPort, ctext);
		row.add(btnStart, cbutton);
		content.add(row);

		// Add some glue and dummy content to see how it looks
//		content.add(Box.createVerticalGlue());
//
//		row = panel();
//		row.add(new JButton("dummy"));
//		content.add(row);		
//
//		row = panel();
//		row.add(new JButton("dummy"));
//		content.add(row);		
//
//		row = panel();
//		row.add(new JButton("dummy"));
//		content.add(row);		
//
//		row = panel();
//		row.add(new JButton("dummy"));
//		content.add(row);		
//
//		row = panel();
//		row.add(new JButton("dummy"));
//		content.add(row);		
//
//		row = panel();
//		row.add(new JButton("dummy"));
//		content.add(row);		
	}

	private void layoutFluidInBorder(JPanel content) {
		content.setLayout(new BorderLayout());

		FluidLayout fluid = new FluidLayout();
		FluidConstraint clabel = new FluidConstraint(12,4,4,3,2);
		FluidConstraint ctext = new FluidConstraint(12,8,8,3,2);
		FluidConstraint cbutton = new FluidConstraint(12,12,12,12,4);
		
		// reusable jpanel ref for creating rows
		JPanel row = panel(); 
			row.setLayout(fluid);
		row.add(lblName, clabel);
		row.add(txtName, ctext);
		row.add(lblPort, clabel);
		row.add(txtPort, ctext);
		row.add(btnStart, cbutton);
		content.add(row, BorderLayout.NORTH);

		// Add some dummy content to south
		row = panel();
		row.add(new JButton("center"));
		content.add(row, BorderLayout.CENTER);	

		row = panel();
		row.add(new JButton("south"));
		content.add(row, BorderLayout.SOUTH);	
	}

	private static final Border outline = BorderFactory.createLineBorder(Color.red);
	private JPanel panel() {
		JPanel p = new JPanel();
		p.setBorder(outline);
		return p;
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
	 * @author rwellman
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
