package org.jwellman.vfsjfilechooser2;

import com.googlecode.vfsjfilechooser2.VFSJFileChooser;
import com.googlecode.vfsjfilechooser2.VFSJFileChooser.SELECTION_MODE;
import com.googlecode.vfsjfilechooser2.accessories.DefaultAccessoriesPanel;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import org.jwellman.virtualdesktop.vapps.VirtualAppSpec;

/**
 *
 * @author Rick Wellman
 */
public class SpecVfsFileChooser2 extends VirtualAppSpec {

    public SpecVfsFileChooser2() {
        this.setTitle("Apache VFS File Chooser");

        // create a file chooser
        final VFSJFileChooser fileChooser = new VFSJFileChooser();
        fileChooser.setAccessory(new DefaultAccessoriesPanel(fileChooser));
        fileChooser.setFileHidingEnabled(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(SELECTION_MODE.FILES_ONLY);

        final JPanel content = new JPanel(new BorderLayout());
        content.add(fileChooser);

        this.setContent(content);
    }

}
