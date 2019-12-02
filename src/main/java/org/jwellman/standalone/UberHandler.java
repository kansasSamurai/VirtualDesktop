package org.jwellman.standalone;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;

/**
 * Based on (but modified):
 * Java Swing, Second Edition
 * Chapter 24, 24.1 What is Drag and Drop?
 *
 * @author Rick Wellman
 */
@SuppressWarnings("serial")
public class UberHandler extends TransferHandler {

    private JTextArea output;

    public void TransferHandler() {
    }

    @Override
    public boolean canImport(JComponent dest, DataFlavor[] flavors) {
        return true; // You bet we can!
    }

    @Override
    public boolean importData(JComponent src, Transferable transferable) {
        // Here's the tricky part.
        println("Receiving data from " + src);
        println("Transferable object is: " + transferable);
        println("Valid data flavors: ");
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        DataFlavor listFlavor = null;
        DataFlavor objectFlavor = null;
        DataFlavor readerFlavor = null;
        DataFlavor jvmFlavor = null;
        int lastFlavor = flavors.length - 1;

        // Check the flavors and see if we find one we like. If we do, save it.
        for (int f = 0; f <= lastFlavor; f++) {
            println("  " + flavors[f]);
            if (flavors[f].isFlavorJavaFileListType()) {
                listFlavor = flavors[f];
            }
            if (flavors[f].isFlavorSerializedObjectType()) {
                objectFlavor = flavors[f];
            }
            if (flavors[f].isRepresentationClassReader()) {
                readerFlavor = flavors[f];
            }
            if (flavors[f].isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType)) {
                jvmFlavor = flavors[f];            	
            }
        }

        // Now try to display the content of the drop.
        try {
            BufferedReader br = null;
            String line = null;

            final DataFlavor bestTextFlavor = DataFlavor.selectBestTextFlavor(flavors);
            if (bestTextFlavor != null) {
                println("BEST TEXT flavor: " + bestTextFlavor.getMimeType());
                println("Content:");
                Reader r = bestTextFlavor.getReaderForText(transferable);
                br = new BufferedReader(r);
                line = br.readLine();
                while (line != null) {
                    println(line);
                    line = br.readLine();
                }
                br.close();
            } else if (listFlavor != null) {
            	println("No BEST TEXT flavor; showing flavor LIST:");
                List<?> list = (List<?>) transferable.getTransferData(listFlavor);
                println(list);
            } else if (objectFlavor != null) {
            	println("No BEST TEXT flavor; showing flavor OBJECT:");
                println("Data is a java object:\n" + transferable.getTransferData(objectFlavor));
            } else if (readerFlavor != null) {
            	println("No BEST TEXT flavor; Data is an InputStream:");
                br = new BufferedReader((Reader) transferable.getTransferData(readerFlavor));
                line = br.readLine();
                while (line != null) {
                    println(line);
                }
                br.close();
            } else {
                // Don't know this flavor type yet... 
            	// assume it is DataFlavor[mimetype=application/x-java-jvm-local-objectref;...]
            	// and if not, let it fall through to the exception handler.
                println("No text based MIME Content-type to show; calling toString():");
                println(transferable.getTransferData(jvmFlavor));
            }
            println("\n\n");
        } catch (Exception e) {
            println("Caught exception decoding transfer:");
            println(e);
            return false;
        }

        return true;
    }

    @Override
    public void exportDone(JComponent src, Transferable data, int action) {
        System.out.println("Export Done."); // Just let us know when it occurs.
    }

    public void setOutput(JTextArea jta) {
        output = jta;
        // the next line saves the developer one line of code on the client side ;)
        // since the design of this class presumes that you are always going to have
        // to do this.
        jta.setTransferHandler(this);
    }

    protected void print(Object o) {
        print(o.toString());
    }

    protected void print(String s) {
        if (output != null) {
            output.append(s);
        } else {
            System.out.println(s);
        }
    }

    protected void println(Object o) {
        println(o.toString());
    }

    protected void println(String s) {
        if (output != null) {
            output.append(s);
            output.append("\n");
        } else {
            System.out.println(s);
        }
    }

    protected void println() {
        println("");
    }

    public static void main(String args[]) {
        JFrame frame = new JFrame("Debugging Drop Zone");
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTextArea jta = new JTextArea();
        frame.getContentPane().add(new JScrollPane(jta));
        UberHandler uh = new UberHandler();
        uh.setOutput(jta);
        // setOutput() now does this automatically:  jta.setTransferHandler(uh);

        frame.setVisible(true);

    }

}
