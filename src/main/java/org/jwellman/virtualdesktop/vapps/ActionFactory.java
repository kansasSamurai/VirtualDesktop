package org.jwellman.virtualdesktop.vapps;

import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.Icon;
import org.apache.batik.transcoder.TranscoderException;
import org.jwellman.virtualdesktop.desktop.VException;
import org.jwellman.virtualdesktop.desktop.VIcon;

/**
 * This class is responsible for creating all desktop actions
 * and their corresponding action components.
 *
 * @author rwellman
 */
public class ActionFactory {

    public static Class<?>[] registeredApps = {
        SpecBeanShell.class
        ,SpecJCXConsole.class
//      ,SpecVfsFileChooser2.class // this app sucks
        ,SpecHyperSQL.class
        ,SpecJFreeChart.class
        ,SpecXChartDemo.class
//      ,SpecXionFM.class // this app is targeted for Linux
        ,SpecUberDragAndDrop.class
//      ,SpecJzy3D.class // this app sucks
    };

    private static final List<DesktopAction> listOfActions = new ArrayList<>();

    public static void initDesktop() {

        DesktopAction a = null; // reusable
        
        for (Class<?> clazz : registeredApps) {
            a = new DesktopAction(clazz.getSimpleName());
            getListOfActions().add(a);

            try {
                a.putValue(Action.SMALL_ICON, VIcon.createSVGIcon("org/jwellman/virtualdesktop/images/global_ui/home156", 16, 16));
                a.putValue(Action.ACTION_COMMAND_KEY, clazz.getCanonicalName()); // i.e. org.jwellman.virtualdesktop.vapps.SpecJCXConsole
//            a.putValue(Action.SHORT_DESCRIPTION, "");
//            a.putValue(Action.MNEMONIC_KEY, "");
            } catch (TranscoderException ex) {
                throw new VException("", ex);
            }

        }

        String[] labels = {"Home",    "Calendar",    "Office Writer", "Trash"};
        String[] icons =  {"home156", "calendar168", "document176",   "rubbish1"};
        String[] clazzs = {"SpecJCXConsole", "SpecJCXConsole", "SpecJCXConsole", "SpecJCXConsole"};
        String iconpath = "org/jwellman/virtualdesktop/images/global_ui/";

        for (int i=0; i < labels.length; i++) {
            try {
                final Icon icon = VIcon.createSVGIcon(iconpath + icons[i]);

                a = new DesktopAction(labels[i]);
                    a.setDesktopOnly(true);
                    a.setClazzName("org.jwellman.virtualdesktop.vapps.SpecJCXConsole"); // TODO (clazzs[i]);
                    a.putValue(Action.LARGE_ICON_KEY, icon);
                    // a.putValue(Action.ACTION_COMMAND_KEY, clazz.getCanonicalName());
                    // a.putValue(Action.SHORT_DESCRIPTION, "");
                    // a.putValue(Action.MNEMONIC_KEY, "");
                getListOfActions().add(a);

            } catch (TranscoderException ex) {
                throw new VException("", ex);
            }
        }

    }

    /**
     * @return the listOfActions
     */
    public static List<DesktopAction> getListOfActions() {
        return listOfActions;
    }

}
