package org.jwellman.bsh;

import java.awt.*;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

/**
 * https://stackoverflow.com/questions/18866466/slow-and-unresponsive-ui-while-listing-files-with-unc-path-for-the-source-server 
 * 
 * @author mkorbel
 *
 */
public class FilesInTheJList {

    private Dimension size;

    private static final int COLUMNS = 5;

    public FilesInTheJList() {

    	final JList<File> list = new JList<File>(new File("C:\\").listFiles()) {

            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                if (size != null) {
                    return new Dimension(size);
                }
                return super.getPreferredScrollableViewportSize();
            }
        };
        list.setVisibleRowCount(0);
        list.setFixedCellHeight(50);
        list.setFixedCellWidth(150);
        list.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new MyCellRenderer());

        size = list.getPreferredScrollableViewportSize();
        size.width *= COLUMNS;

        JFrame f = new JFrame("Files In the JList");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(new JScrollPane(list));
        f.pack();
        f.setVisible(true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new FilesInTheJList());
    }

    private static final FileSystemView FSV = FileSystemView.getFileSystemView();
    
    private static class MyCellRenderer extends JLabel implements ListCellRenderer<File> {

        private static final long serialVersionUID = 1L;

        @Override
		public Component getListCellRendererComponent(JList<? extends File> list, File value, int index, boolean isSelected, boolean cellHasFocus) {

        	if (value instanceof File) {
                final File file = (File) value;
                final Icon icon = FSV.getSystemIcon(file);
                System.out.println("H: " + icon.getIconHeight() + " W: " + icon.getIconWidth() + " < " + file.getName());
                
                setText(file.getName());
                setIcon(icon);
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                setPreferredSize(new Dimension(250, 25));
                setEnabled(list.isEnabled());
                setFont(list.getFont());
                setOpaque(true);
            }
            return this;
        }

    }

}