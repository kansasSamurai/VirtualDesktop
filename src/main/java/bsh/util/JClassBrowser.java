package bsh.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.MatteBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import bsh.BshClassManager;
import bsh.ClassPathException;
import bsh.classpath.BshClassPath;
import bsh.classpath.ClassManagerImpl;

/**
 * Small customizations to BSH ClassBrowser.
 * 
 * @author rwellman
 *
 */
public class JClassBrowser extends ClassBrowser {

    private static final long serialVersionUID = 1L;

    public JClassBrowser() {
        super();
    }

    public JClassBrowser(BshClassManager classManager) {
        super(classManager);
    }

    /* This is mostly a copy of the original method.
     * <p>
     * However, the big difference is that an "empty" classpath object
     * is used initially so that the user interface is much more responsive
     * instead of waiting for the entire classpath to be scanned/created.
     * <p>
     * Then, at the end of the method, we use the original code inside
     * an asynchronous thread that updates the jtree model on the EDT when finished.
     * 
     */
    @Override
    public void init() throws ClassPathException {
        // super.init();

        // Currently we have to cast because BshClassPath is not known by the core.
        classPath = new BshClassPath("dummy"); // ((ClassManagerImpl)classManager).getClassPath();
        classPath.addListener( this );

        final Set<?> pset = classPath.getClassesForPackage("org.jwellman.virtualdesktop"); // classPath.getPackagesSet();

        ptree = new JPackageTree(pset);
        ptree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                TreePath tp = e.getPath();
                Object[] oa = tp.getPath();
                StringBuffer selectedPackage = new StringBuffer();
                for (int i = 1; i < oa.length; i++) {
                    selectedPackage.append(oa[i].toString());
                    if (i + 1 < oa.length)
                        selectedPackage.append(".");
                }
                setClist(selectedPackage.toString());
            }
        });

        classlist = new JList<Object>();
        classlist.addListSelectionListener(this);

        conslist = new JList<Object>();
        conslist.addListSelectionListener(this);

        mlist = new JList<Object>();
        mlist.addListSelectionListener(this);

        fieldlist = new JList<Object>();
        fieldlist.addListSelectionListener(this);

        JSplitPane methodConsPane = splitPane(
            JSplitPane.VERTICAL_SPLIT, true, 
            labeledPane(new JScrollPane(conslist), "Constructors"),
            labeledPane(new JScrollPane(mlist), "Methods")
            );

        JSplitPane rightPane = splitPane(JSplitPane.VERTICAL_SPLIT, true,
            methodConsPane,
            labeledPane(new JScrollPane(fieldlist), "Fields")
            );

        JSplitPane sp = splitPane( 
            JSplitPane.HORIZONTAL_SPLIT, true, 
            labeledPane(new JScrollPane(classlist), "Classes"),
            rightPane );
        sp = splitPane( 
            JSplitPane.HORIZONTAL_SPLIT, true, 
                labeledPane(new JScrollPane(ptree), "Packages"), sp);

        JPanel bottompanel = new JPanel( new BorderLayout() );

        methodLine = new JTextArea(1,60);
        methodLine.setEditable(false);
        methodLine.setLineWrap(true);
        methodLine.setWrapStyleWord(true);
        methodLine.setFont(new Font("Monospaced", Font.BOLD, 14));
        methodLine.setMargin(new Insets(5, 5, 5, 5));
        methodLine.setBorder(new MatteBorder(1, 0, 1, 0, Color.black));
        bottompanel.add("North", methodLine);

        tree = new JTree();
        tree.setBorder( BorderFactory.createRaisedBevelBorder() );
        tree.addTreeSelectionListener( new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                driveToClass(e.getPath().getLastPathComponent().toString());
            }
        } );

        setClassTree(null);

        JPanel p = new JPanel(new BorderLayout());
        p.add("Center", tree);
        bottompanel.add("Center", p);

        // give it a preferred height
        bottompanel.setPreferredSize(new java.awt.Dimension(150,150));

        setTopComponent(sp);
        setBottomComponent(bottompanel);

        // ================================================

        // The original class sets custom background on a few components; this un-does that
        Color bg = this.conslist.getBackground();
        this.mlist.setBackground(bg);
        this.classlist.setBackground(bg);
        this.methodLine.setBackground(bg);

        // ================================================

        new Thread( () -> 
        {
            try {
                classPath = ((ClassManagerImpl)classManager).getClassPath();
                classPath.addListener( this );

                @SuppressWarnings("unchecked")
                final Set<String> pkgset = classPath.getPackagesSet();
                final Set<String> test = new TreeSet<>();
                test.addAll(pkgset);

                SwingUtilities.invokeLater(() -> {
                    ptree.setPackages(test); // (pkgset);
                });
            } catch (ClassPathException e1) {
                e1.printStackTrace();
            }

        } ) .start();
    }

    // I won't really use this as a JTree but I do need it 
    class JPackageTree extends ClassBrowser.PackageTree {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings("rawtypes")
        JPackageTree(Collection packages) {
            super(packages);
        }

        // Override to provide sorted packages
        @Override
        @SuppressWarnings("unchecked")
        protected MutableTreeNode makeNode(@SuppressWarnings("rawtypes") Map map, String nodeName) {
            // This is the new part to sort
            final TreeSet<String> sortedset = new TreeSet<>();
            sortedset.addAll(map.keySet());

            final DefaultMutableTreeNode root = new DefaultMutableTreeNode(nodeName);
            for (String name : sortedset) {
                final Map<String, ?> val = (Map<String, ?>) map.get(name);
                if (val.size() == 0) {
                    DefaultMutableTreeNode leaf = new DefaultMutableTreeNode(name);
                    root.add(leaf);
                } else {
                    MutableTreeNode node = makeNode(val, name);
                    root.add(node);
                }
            }

            return root;
        }

    }

    // This is a duplicate of the base class code because it is private
    protected JSplitPane splitPane(int orientation, boolean redraw, JComponent c1, JComponent c2) {
        final JSplitPane sp = new JSplitPane(orientation, redraw, c1, c2);
        sp.setBorder(null);

        javax.swing.plaf.SplitPaneUI ui = sp.getUI();
        if (ui instanceof javax.swing.plaf.basic.BasicSplitPaneUI) {
            ((javax.swing.plaf.basic.BasicSplitPaneUI) ui).getDivider().setBorder(null);
        }

        return sp;
    }

    // Override to provide a sorted array 
    @Override
    protected Method[] getPublicMethods(Method[] methods) {
        Method[] ma = super.getPublicMethods(methods);
        Arrays.sort(ma, Comparator.comparing(Method::getName));

        return ma; 
    }

    // Override to provide a sorted array 
    @Override
    protected String[] parseMethods(Method[] methods) {
        String[] sa = super.parseMethods(methods);
        Arrays.sort(sa);

        return sa;
    }

    // Override to provide a sorted array 
    @Override
    protected Field[] getPublicFields(Field[] fields) {
        Field[] fa = super.getPublicFields(fields);
        Arrays.sort(fa, Comparator.comparing(Field::getName));

        return fa;
    }

    // Override to provide a sorted array 
    @Override
    protected String[] parseFields(Field[] fields) {
        String[] sa = super.parseFields(fields);
        Arrays.sort(sa);

        return sa;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Constructor[] getPublicConstructors(Constructor[] constructors) {
        return super.getPublicConstructors(constructors);
    }

}
