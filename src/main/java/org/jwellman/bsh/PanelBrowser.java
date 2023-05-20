package org.jwellman.bsh;
// for use in bsh scripts: import org.jwellman.bsh.PanelBrowser

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;

/**
 * This isn't something I will probably use but it is an interesting
 * proof of concept.  Creates a Mac-like file browser.
 * 
 * @see http://stackoverflow.com/a/15104660/230513
 */
@SuppressWarnings("serial")
public class PanelBrowser extends Box {

	private String rootpath;
	
	private List<FilePanel> listOfPanels = new ArrayList<FilePanel>();

    private static final Dimension SIZE = new Dimension(200, 300);
    
    public PanelBrowser( ) {
    	this( new File(System.getProperty("user.dir")) );
    }

    public PanelBrowser(File root) {
        super(BoxLayout.LINE_AXIS);        
        setBackground(Color.red);
        
        this.rootpath = root.getAbsolutePath();

        FilePanel panel = new FilePanel(this, root);
        listOfPanels.add(panel);
        this.add(panel);
    }

    private void update(FilePanel fp, File file) {
        int index = listOfPanels.indexOf(fp);
        int i = listOfPanels.size() - 1;
        while (i > index) {
            listOfPanels.remove(i);
            this.remove(i);
            i--;
        }
        final FilePanel panel = new FilePanel(this, file);
        listOfPanels.add(panel);
        this.add(panel);
        revalidate();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollRectToVisible(panel.getBounds());
            }
        });
    }

    public String getRootpath() {
		return rootpath;
	}

    private static class FilePanel extends Box {

        private static FileSystemView FSV = FileSystemView.getFileSystemView();

        private static DateFormat df = DateFormat.getDateTimeInstance( DateFormat.SHORT, DateFormat.DEFAULT);

        private JList<File> jlistOfFiles;

        private PanelBrowser parent;

        public FilePanel(PanelBrowser parent, File file) {
            super(BoxLayout.PAGE_AXIS);
            this.parent = parent;

            final DefaultListModel<File> model = new DefaultListModel<>();
            if (file.isFile()) {
                JLabel name = new JLabel(file.getName());
                name.setIcon(FSV.getSystemIcon(file));
                this.add(name);
                Date d = new Date(file.lastModified());
                JLabel mod = new JLabel("Date: " + df.format(d));
                this.add(mod);
                final String v = String.valueOf(file.length());
                JLabel length = new JLabel("Size: " + v);
                this.add(length);
            }
            if (file.isDirectory()) {
            	File[] rawfiles = file.listFiles();
            	List<File> dirlist = new ArrayList<>();
            	List<File> filelist = new ArrayList<>();
            	
            	for (File f : rawfiles) {
                    if (f.isDirectory()) 
                    	dirlist.add(f);
                    else
                    	filelist.add(f);
                }

            	dirlist.sort(null);
            	for (File f : dirlist) {
                    model.addElement(f);
                }

            	filelist.sort(null);
            	for (File f : filelist) {
                    model.addElement(f);
                }

                jlistOfFiles = new JList<File>(model);
                jlistOfFiles.setCellRenderer(new FileRenderer());
                jlistOfFiles.addListSelectionListener(new SelectionHandler());
                jlistOfFiles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                this.add(new JScrollPane(jlistOfFiles) {
                    @Override
                    public int getVerticalScrollBarPolicy() {
                        return JScrollPane.VERTICAL_SCROLLBAR_ALWAYS;
                    }
                });
            }
        }

        private static class FileRenderer extends DefaultListCellRenderer {

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            	final File f = (File) value;
            	final Path realfile = Paths.get(f.getName());

                final JLabel label = (JLabel) super.getListCellRendererComponent( list, value, index, isSelected, cellHasFocus);
                label.setText(realfile.getFileName().toString()); //(f.getName()); 
                label.setIcon(FSV.getSystemIcon(f));

                return label;
            }
        }

        private class SelectionHandler implements ListSelectionListener {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    File f = (File) jlistOfFiles.getSelectedValue();
                    parent.update(FilePanel.this, f);
                }
            }
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(SIZE);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIZE);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(SIZE.width, Short.MAX_VALUE);
        }
    }

    public static void display(PanelBrowser pb) {

        final JFrame f = new JFrame(pb.getRootpath());
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new JScrollPane(pb) {
            @Override
            public int getVerticalScrollBarPolicy() {
                return JScrollPane.VERTICAL_SCROLLBAR_NEVER;
            }
        });
        f.pack();
        f.setSize(4 * SIZE.width, SIZE.height);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    // wrap a panel browser in a jpanel for jpad
    public JPanel display2(PanelBrowser pb) {

        JPanel f = new JPanel();
        f.add(new JScrollPane(pb) {
            @Override
            public int getVerticalScrollBarPolicy() {
                return JScrollPane.VERTICAL_SCROLLBAR_NEVER;
            }
        });
        f.setSize(4 * SIZE.width, SIZE.height);
        
        return f;
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                display(new PanelBrowser());
            }
        });
    }

}