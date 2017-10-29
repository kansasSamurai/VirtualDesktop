package org.jwellman.virtualdesktop.vapps;

import ext.hsqldb.util.DatabaseManagerSwing;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.hsqldb.Server;

/**
 * This spec provides access to the HyperSQL Database Manager.
 *
 * Note that this spec will provide the roadmap for a new subclass
 * of VirtualAppSpec (whose name is TBD but I will call X for now).
 *
 * Namely, X virtual apps are those that are for some reason only
 * able to express themselves as "external" JFrames.  However, we
 * still want the JVD to "manage" the virtual app so we might,
 * at a minimum, want the JInternalFrame to close the external JFrame
 * when the user closes the JInternalFrame (or at least verify/prompt the user).
 *
 * I do not have other use cases identified now, but it certainly
 * makes sense for us to capture this commonality via a subclass
 * to VirtualAppSpec.
 *
 * A use case for THIS particular app is to not start the app automatically
 * but rather offer some options before starting the app; such as:
 * (1) whether to run HSQL in memory or from persistent store
 *
 * @author Rick Wellman
 */
public class SpecHyperSQL extends VirtualAppSpec {

    private static Server server;

    private static final String CONNECTION_URL = "jdbc:hsqldb:hsql://localhost:1234/sandbox";
    
    private static final String CONNECTION_USER = "SA";
    
    private static final String CONNECTION_DRIVER = "org.hsqldb.jdbc.JDBCDriver";
    
    private static final String CONNECTION_PASSWORD = "";

    /**
     * 
     */
    public SpecHyperSQL() {

        this.setTitle("HyperSQL Manager");
        this.setContent(new JPanel());

        if (server == null) {
            try {
                Class.forName(CONNECTION_DRIVER);
                server = new Server();
                server.setAddress("localhost");
                server.setDatabaseName(0, "sandbox");
                server.setDatabasePath(0, "file:C:/data/hsqldb/sandbox");
                server.setPort(1234);
                server.setTrace(true);
                server.setLogWriter(new PrintWriter(System.out));
                server.start();

            } catch (ClassNotFoundException e) {
                e.printStackTrace(System.out);
            }
        }

        try {
            Connection con = DriverManager.getConnection( CONNECTION_URL, CONNECTION_USER, CONNECTION_PASSWORD);
            con.createStatement().executeUpdate(
                "create table contacts (name varchar(45),email varchar(45),phone varchar(45))"
            );
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DatabaseManagerSwing dbm = null;
                try {
                    int version = 3;
                    switch (version) {
                        case 1:
                            DatabaseManagerSwing.main(new String[]{"--noexit"});
                            break;
                        case 2:
                            // 20171007; this works but trying something new
                            dbm = new DatabaseManagerSwing(new JFrame("Custom DBM"));
                            dbm.main();
                            break;
                        case 3:
                            dbm = new DatabaseManagerSwing(new JFrame("Custom DBM"));
                            dbm.main();
                            dbm.postmain(dbm);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace(); // for now, simply swallow the exception
                }
            }
        });

    }

}
