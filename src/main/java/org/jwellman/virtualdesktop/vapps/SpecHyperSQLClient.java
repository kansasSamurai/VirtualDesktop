package org.jwellman.virtualdesktop.vapps;

import ext.hsqldb.util.DatabaseManagerSwing;
import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

/**
 *
 * @author Rick
 */
public class SpecHyperSQLClient extends VirtualAppSpec {

    public SpecHyperSQLClient() {
        super();

        this.setTitle("HyperSQL Client");
        this.internalFrameProvider = true;

    }

    @Override
    public void populateInternalFrame(JInternalFrame frame, JDesktopPane desktop) {

        final JFrame jframe = new JFrame("Custom DBM");
        final DatabaseManagerSwing dbm = new DatabaseManagerSwing(jframe);
        dbm.main();
        dbm.postmain(dbm);
        this.setContent(this.createDefaultContent(jframe.getContentPane()));

        frame.add(this.getContent());
        frame.setJMenuBar(jframe.getRootPane().getJMenuBar());
        frame.setFrameIcon(new ImageIcon( jframe.getIconImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH) ));

        SwingUtilities.invokeLater(() -> {
            frame.setPreferredSize(new Dimension(200, 300));
            // frame.invalidate(); // this does not work
            frame.pack();
            frame.setVisible(true);

            jframe.setVisible(false);
        } );

    }

}
