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

    static Class[] registeredApps = {
        SpecBeanShell.class
        ,SpecJCXConsole.class
        ,SpecHyperSQL.class
        ,SpecJFreeChart.class
        ,SpecXChartDemo.class
        ,SpecXionFM.class
        ,SpecUberDragAndDrop.class
//        ,SpecXionFM.class // this app is targeted for Linux
//        ,SpecJzy3D.class // this app sucks
    };

    private static List<DesktopAction> listOfActions = new ArrayList<>();
    
    public static void initDesktop() {
        
        DesktopAction a = null;
        for (Class clazz : registeredApps) {
            a = new DesktopAction(clazz.getSimpleName());
            getListOfActions().add(a);
            
            try {
                a.putValue(Action.SMALL_ICON, VIcon.createSVGIcon("org/jwellman/virtualdesktop/images/global_ui/home156"));
                a.putValue(Action.ACTION_COMMAND_KEY, clazz.getCanonicalName());
//            a.putValue(Action.SHORT_DESCRIPTION, "");            
//            a.putValue(Action.MNEMONIC_KEY, "");
            } catch (TranscoderException ex) {
                throw new VException("", ex);                        
            }

        }
        
        String[] labels = {"Home", "Calendar", "Office Writer", "Trash"};
        String[] icons = {"home156", "calendar168", "document176", "rubbish1"};
        String iconpath = "org/jwellman/virtualdesktop/images/global_ui/";
        
        for (int i=0; i < labels.length; i++) { 
            try {
                Icon icon = VIcon.createSVGIcon(iconpath + icons[i]);

                a = new DesktopAction(labels[i]);
                getListOfActions().add(a);

                a.setDesktopOnly(true);
                a.putValue(Action.LARGE_ICON_KEY, icon);
                // a.putValue(Action.ACTION_COMMAND_KEY, clazz.getCanonicalName());
                // a.putValue(Action.SHORT_DESCRIPTION, "");            
                // a.putValue(Action.MNEMONIC_KEY, "");
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
