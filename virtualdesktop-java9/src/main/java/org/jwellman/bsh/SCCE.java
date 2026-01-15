package org.jwellman.bsh;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileSystemView;

/**
 * 
 * @author rwellman
 *
 */
public class SCCE {

    private static final FileSystemView FSV = FileSystemView.getFileSystemView();

    public SCCE() {

    	final JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    	panel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    	
    	final File[] rawfiles = new File("C:\\").listFiles();
    	List<File> dirlist = new ArrayList<>();
    	List<File> filelist = new ArrayList<>();
    	List<File> listoffiles = new ArrayList<>();
    	
    	for (File f : rawfiles) {
            if (f.isDirectory()) 
            	dirlist.add(f);
            else
            	filelist.add(f);
        }

    	dirlist.sort(null);
    	listoffiles.addAll(dirlist);

    	filelist.sort(null);
    	listoffiles.addAll(filelist);

    	for (File file : listoffiles) {
    		final JLabel label = new JLabel(file.getName());
    		label.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));

    		Icon icon = FSV.getSystemIcon(file);
// Java9+ might fix 16x16 icon; see similar code from
// https://github.com/openjdk/jdk/pull/7805/files
//    		if (icon instanceof ImageIcon) {
//                Image image = ((ImageIcon) icon).getImage();
//                if (image instanceof MultiResolutionImage) {
//                    Image variant = ((MultiResolutionImage) image).getResolutionVariant(16, 16);
//                    if (variant.getWidth(null) != 16) {
//                        throw new RuntimeException("Default file icon has size of " +
//                                variant.getWidth(null) + " instead of 16");
//                    }
//                }
//            }
    		label.setIcon(icon);
    		
    		panel.add(label);
    	}

        JFrame f = new JFrame("Bug SCCE");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.add(panel);
        f.pack();
        f.setVisible(true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new SCCE());
    }
}