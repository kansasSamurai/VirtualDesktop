package org.jwellman.virtualdesktop.vapps;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.jwellman.dsp.DirectoryIconProvider;
import org.jwellman.dsp.icons.IconSpecifier;
import org.jwellman.vfsjfilechooser2.SpecVfsFileChooser2;

import com.fasterxml.jackson.databind.ObjectMapper;

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
         ,SpecScar.class
        ,SpecJetty.class
        ,SpecHyperSQL.class
        ,SpecHyperSQLClient.class
        ,SpecJDatePicker.class
        ,SpecJFreeChart.class
        ,SpecXChartDemo.class
        ,SpecXChartBarChart.class
        ,SpecSVGViewer.class
        ,SpecVfsFileChooser2.class // this isn't as bad as I originally thought... I was confusing it with XionFM and/or SpecJzy3D below.
        ,SpecUberDragAndDrop.class
//      ,SpecXionFM.class // this app is targeted for Linux
        ,SpecDocking.class
        ,SpecJediTerm.class
        ,SpecGroovyConsole.class // newly readded
        ,SpecGroovyGraphics.class
//      ,SpecJzy3D.class // this app sucks
    };

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final List<DesktopAction> listOfActions = new ArrayList<>();

    private static final DirectoryIconProvider iconProvider = new DirectoryIconProvider();

    public static void initDesktop() {

        DesktopAction a = null; // reusable

        for (Class<?> clazz : registeredApps) {
            a = new DesktopAction(clazz.getSimpleName());
            getListOfActions().add(a);

            Icon icon = iconProvider.getIcon(new IconSpecifier("Directory", "org/jwellman/virtualdesktop/images/global_ui/home156", 16, null, null, null));
            a.putValue(Action.SMALL_ICON, icon);
            a.putValue(Action.ACTION_COMMAND_KEY, clazz.getCanonicalName()); // i.e. org.jwellman.virtualdesktop.vapps.SpecJCXConsole
//            a.putValue(Action.SHORT_DESCRIPTION, "");
//            a.putValue(Action.MNEMONIC_KEY, "");

        }

        String[] labels = {"Home",    "Calendar",    "Office Writer", "Trash"};
        String[] icons =  {"home156", "calendar168", "document176",   "rubbish1"};
        String[] clazzs = {"SpecJCXConsole", "SpecJCXConsole", "SpecJCXConsole", "SpecJCXConsole"};
        String iconpath = "org/jwellman/virtualdesktop/images/global_ui/";

        for (int i=0; i < labels.length; i++) {
            final Icon icon = iconProvider.getIcon(new IconSpecifier("Directory", iconpath + icons[i], 48, null, null, null));

            a = new DesktopAction(labels[i]);
                a.setDesktopOnly(true);
                a.setClazzName("org.jwellman.virtualdesktop.vapps." + clazzs[i]); // TODO (clazzs[i]);
                a.putValue(Action.LARGE_ICON_KEY, icon);
                // a.putValue(Action.ACTION_COMMAND_KEY, clazz.getCanonicalName());
                // a.putValue(Action.SHORT_DESCRIPTION, "");
                // a.putValue(Action.MNEMONIC_KEY, "");
            getListOfActions().add(a);
        }

        // Load external applications from configuration file
        loadExternalApps();

    }

    /**
     * Loads external application definitions from a JSON configuration file.
     * If the file doesn't exist or cannot be read, this method fails silently.
     */
    private static void loadExternalApps() {
        File configFile = new File("config/external-apps.json");

        if (!configFile.exists()) {
            System.out.println("External apps config file not found: " + configFile.getAbsolutePath());
            System.out.println("Skipping external app loading. Create this file to define external applications.");
            return;
        }

        try {
            ExternalAppsConfig config = mapper.readValue(configFile, ExternalAppsConfig.class);

            for (ExternalAppConfig appConfig : config.getExternalApps()) {
                registerExternalApp(appConfig);
            }

            System.out.println("Loaded " + config.getExternalApps().size() + " external app(s) from configuration");

        } catch (IOException ex) {
            System.err.println("Failed to load external apps configuration: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Registers an external application as a desktop action
     * @param appConfig the external app configuration
     */
    private static void registerExternalApp(ExternalAppConfig appConfig) {
        try {
            // Create the action
            ExternalAppAction action = new ExternalAppAction(
                appConfig.getName(),
                appConfig.getCommand(),
                appConfig.getWorkingDir(),
                appConfig.isWaitForCompletion()
            );
            action.setClazzName("org.jwellman.virtualdesktop.vapps.ExternalAppSpec");

            // Set desktop-only flag
            action.setDesktopOnly(appConfig.isDesktopOnly());

            // Load icon if specified
            if (appConfig.getIcon() != null && !appConfig.getIcon().isEmpty()) {
                Icon largeIcon = iconProvider.getIcon(new IconSpecifier("Directory", appConfig.getIcon(), 48, null, null, null));
                Icon smallIcon = iconProvider.getIcon(new IconSpecifier("Directory", appConfig.getIcon(), 16, null, null, null));

                if (largeIcon != null) {
                    action.putValue(Action.LARGE_ICON_KEY, largeIcon);
                    action.putValue(Action.SMALL_ICON, smallIcon);
                } else {
                    System.err.println("Failed to load icon for " + appConfig.getName() + ": " + appConfig.getIcon());
                    // Use default icon
                    Icon defaultLargeIcon = iconProvider.getIcon(new IconSpecifier("Directory", "org/jwellman/virtualdesktop/images/global_ui/winking18", 48, null, null, null));
                    Icon defaultSmallIcon = iconProvider.getIcon(new IconSpecifier("Directory", "org/jwellman/virtualdesktop/images/global_ui/winking18", 16, null, null, null));
                    action.putValue(Action.LARGE_ICON_KEY, defaultLargeIcon);
                    action.putValue(Action.SMALL_ICON, defaultSmallIcon);
                }
            }

            action.putValue(Action.NAME, appConfig.getName());

            // Add to list of actions
            getListOfActions().add(action);

        } catch (Exception ex) {
            System.err.println("Failed to register external app: " + appConfig.getName());
            ex.printStackTrace();
        }
    }

    /**
     * @return the listOfActions
     */
    public static List<DesktopAction> getListOfActions() {
        return listOfActions;
    }

}
