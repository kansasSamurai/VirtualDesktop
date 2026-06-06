package org.jwellman.virtualdesktop.vapps;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.svg.SVGDocument;

public class SpecSVGViewer extends VirtualAppSpec {

    public SpecSVGViewer() {
        this.setTitle("SVG Viewer");
        this.setContent(new Container());
    }

}

class Container extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JTextArea editor = new JTextArea();
    private SVGViewerPanel viewer = new SVGViewerPanel();
    private JButton update = new JButton("Update");

    public Container() {
        this.setLayout(new BorderLayout());

        JScrollPane sp = new JScrollPane(editor);
        this.add(sp, BorderLayout.NORTH);
        this.add(viewer, BorderLayout.CENTER);
        this.add(update, BorderLayout.SOUTH);

        this.editor.setRows(10);
        this.update.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        viewer.update(editor.getText());
    }

}

class SVGViewerPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private SVGDocument svgDocument;
    private GraphicsNode svgGraphicsNode;

    /*
        <svg width="200" height="200" xmlns="http://www.w3.org/2000/svg">
          <circle cx="100" cy="100" r="80" fill="red" />
        </svg>
     */
    public static final String sampleSVG = "<svg width=\"200\" height=\"200\" xmlns=\"http://www.w3.org/2000/svg\">" 
            + "<rect x=\"50\" y=\"50\" width=\"100\" height=\"100\" fill=\"blue\"/>" 
            + "</svg>";

    public SVGViewerPanel() {
        this(sampleSVG);
    }

    public void update(String text) {
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            System.out.println(parser);
            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
//            SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(UserAgentAdapter.XML_PARSER_CLASS_NAME);
//            InputStream reader = new StringReader(svgContent);
            InputStream inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
            svgDocument = factory.createSVGDocument("file:///dummy.svg", inputStream);

            UserAgent userAgent = new UserAgentAdapter();
            DocumentLoader loader = new DocumentLoader(userAgent);
            BridgeContext ctx = new BridgeContext(userAgent, loader);
            ctx.setDynamicState(BridgeContext.DYNAMIC);

            GVTBuilder builder = new GVTBuilder();
            svgGraphicsNode = builder.build(ctx, svgDocument);

            this.invalidate();
            this.repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SVGViewerPanel(String svgContent) {
        this.setBackground(Color.white);
        this.update(svgContent);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (svgGraphicsNode != null) {
            Graphics2D g2d = (Graphics2D) g;
            // Apply rendering hints for better quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Render the SVG
            svgGraphicsNode.paint(g2d);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("SVG Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        SVGViewerPanel panel = new SVGViewerPanel(sampleSVG);
        frame.add(panel);
        frame.setVisible(true);
    }
}
