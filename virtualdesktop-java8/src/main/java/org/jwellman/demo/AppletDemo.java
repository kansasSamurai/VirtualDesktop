package org.jwellman.demo;

import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JApplet;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
* An applet demonstrating the JSVGCanvas.
*
* @version $Id: AppletDemo.java 985243 2010-08-13 15:30:25Z helder $
*/
public class AppletDemo extends JApplet {

    protected JSVGCanvas canvas;

    protected Document doc;

    protected Element svg;

    protected Element path1, path2, path3;

    protected boolean parsed = false;

    protected Timer timer;

    private static final long serialVersionUID = 1L;

    public void init() {
        // Create a new JSVGCanvas.
        canvas = new JSVGCanvas();
        getContentPane().add(canvas);

        try {
            URL path = getCodeBase();
            System.out.println("codebase: " + path.toString());
            // file:/C:/dev/workspaces/git/VirtualDesktop/target/classes/
            // so if they project is cleaned you will have to copy barChart.svg to this location again

            // Parse the barChart.svg file into a Document.
            // https://raw.githubusercontent.com/apache/xmlgraphics-batik/refs/heads/main/samples/barChart.svg
            // copied to the project target/classes and "header" is removed
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            URL url = new URL(path, "barChart.svg");
            doc = f.createDocument(url.toString());

            svg = doc.getDocumentElement();

            // Change the document viewBox.
            svg.setAttributeNS(null, "viewBox", "40 95 370 265");

            // Make the text look nice.
            svg.setAttributeNS(null, "text-rendering", "geometricPrecision");

            // Remove the xml-stylesheet PI.
            for (Node n = svg.getPreviousSibling(); n != null; n = n.getPreviousSibling()) {
                if (n.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                    doc.removeChild(n);
                    break;
                }
            }

            // Remove the Batik sample mark 'use' element.
            for (Node n = svg.getLastChild(); n != null; n = n.getPreviousSibling()) {
                if (n.getNodeType() == Node.ELEMENT_NODE && n.getLocalName().equals("use")) {
                    svg.removeChild(n);
                    break;
                }
            }

        } catch (Exception ex) {
        }
    }

    public void start() {
        // Display the document.
        canvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        canvas.setDocument(doc);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            int travel = 10;
            int direction = 1;

            @Override
            public void run() {
                if (travel == 10) direction = 1;
                if (travel == 50) direction = -1;
                travel = travel + direction;
                // System.out.println("" + travel);
                updateBar("TravelBar", travel, direction);
            }
        }, 2000, 40); // 2-second pause, 1 second interval

    }

    public void stop() {
        timer.cancel();

        // Remove the document.
        canvas.setDocument(null);
    }

    public void destroy() {
        canvas.dispose();
    }

    public void updateBar(final String name, final float value, int direction) {
        if (canvas.getUpdateManager() == null) return;

        if (parsed) {} else {
            /* This proves that once a Node is obtained you do not have to search
             * for it every time in an action.  i.e. as long as you don't modify the
             * document, then keeping node references is perfectly valid.
             * 
             * This only works because I am always passing the same bar name.
             * This would not work in the original implementation.
             */
            Element bar = doc.getElementById(name);
            if (bar == null) {
                System.out.println("error");
                return;
            }

            /* Find each path element via getFirstChild(), getNextSibling(), getNextSibling()
             * and store ref to each element
                <g id="ShoeBar">
                    <path style="fill:#8686E0;" d="M  86,240 v  -37 l 15    -15 v  37 l -15,15 z"/>
                    <path style="fill:#5B5B97;" d="M  86,203 h  -39 l 15    -15 h  39 l -15,15 z"/>
                    <path style="fill:#7575C3;" d="M  47,203 v   37 h 39 v  -37 H  47 z"/>
                </g>
             */
            Node n; // reusable obj ref
            for (n = bar.getFirstChild(); n.getNodeType() != Node.ELEMENT_NODE; n = n.getNextSibling()) {
            }
            path1 = (Element) n;
            for (n = n.getNextSibling(); n.getNodeType() != Node.ELEMENT_NODE; n = n.getNextSibling()) {
            }
            path2 = (Element) n;
            for (n = n.getNextSibling(); n.getNodeType() != Node.ELEMENT_NODE; n = n.getNextSibling()) {
            }
            path3 = (Element) n;

            parsed = true;
        }

        canvas.getUpdateManager().getUpdateRunnableQueue().invokeLater(new Runnable() {
            public void run() {

                int offset;
                if (name.equals("ShoeBar")) {
                    offset = 0;
                } else if (name.equals("CarBar")) {
                    offset = 79;
                } else if (name.equals("TravelBar")) {
                    offset = 158;
                } else { // name.equals("ComputerBar")
                    offset = 237;
                }

                double a = value; // 3.7 * value;
                double b = 240 - a;

                // side / "M {1},240 v -{2} 1 15,-15 v {3} 1 -15,15,z"
                String d = "M " + (offset + 86) + ",240 v -" + a + " l 15,-15 v " + a + " l -15,15 z";
                path1.setAttributeNS(null, "d", d);
                path1.setAttributeNS(null, "style", (direction > 0) ? "fill:#00ff00;" : "fill:#ff0000;");

                // top / "M {1} ..."
                d = "M " + (offset + 86) + "," + b + " h -39 l 15,-15 h 39 l -15,15 z";
                path2.setAttributeNS(null, "d", d);

                // front / "M {1} ..."
                d = "M " + (offset + 47) + "," + b + " v " + a + " h 39 v -" + a + " h -39 z";
                path3.setAttributeNS(null, "d", d);

            }
        });
    }

    public void updateBar_works(final String name, final float value) {
        if (canvas.getUpdateManager() == null) return;

        canvas.getUpdateManager().getUpdateRunnableQueue().invokeLater(new Runnable() {
            public void run() {
                Element bar = doc.getElementById(name);
                if (bar == null) {
                    return;
                }

                /* Find each path element via getFirstChild(), getNextSibling(), getNextSibling()
                 * and store ref to each element
                    <g id="ShoeBar">
                        <path style="fill:#8686E0;" d="M  86,240 v  -37 l 15    -15 v  37 l -15,15 z"/>
                        <path style="fill:#5B5B97;" d="M  86,203 h  -39 l 15    -15 h  39 l -15,15 z"/>
                        <path style="fill:#7575C3;" d="M  47,203 v   37 h 39 v  -37 H  47 z"/>
                    </g>
                 */
                Node n; // reusable obj ref
                Element path1, path2, path3;
                for (n = bar.getFirstChild(); n.getNodeType() != Node.ELEMENT_NODE; n = n.getNextSibling()) {
                }
                path1 = (Element) n;
                for (n = n.getNextSibling(); n.getNodeType() != Node.ELEMENT_NODE; n = n.getNextSibling()) {
                }
                path2 = (Element) n;
                for (n = n.getNextSibling(); n.getNodeType() != Node.ELEMENT_NODE; n = n.getNextSibling()) {
                }
                path3 = (Element) n;

                int offset;
                if (name.equals("ShoeBar")) {
                    offset = 0;
                } else if (name.equals("CarBar")) {
                    offset = 79;
                } else if (name.equals("TravelBar")) {
                    offset = 158;
                } else { // name.equals("ComputerBar")
                    offset = 237;
                }

                // "M {1},240 v -{2} 1 15,-15 v {3} 1 -15,15,z"
                String d = "M " + (offset + 86) + ",240 v -" + (3.7 * value) + " l 15,-15 v " + (3.7 * value) + " l -15,15 z";
                path1.setAttributeNS(null, "d", d);

                // "M {1} ..."
                d = "M " + (offset + 86) + "," + (240 - 3.7 * value) + " h -39 l 15,-15 h 39 l -15,15 z";
                path2.setAttributeNS(null, "d", d);

                // "M {1} ..."
                d = "M " + (offset + 47) + "," + (240 - 3.7 * value) + " v " + (3.7 * value) + " h 39 v -" + (3.7 * value) + " h -39 z";
                path3.setAttributeNS(null, "d", d);
            }
        });
    }

}
