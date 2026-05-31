package org.jwellman.virtualdesktop.desktop;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jwellman.virtualdesktop.App;

/**
 * Centralizes user-facing dialogs so the application presents a consistent
 * interaction style in one place.
 */
public class DialogManager {

    private DialogManager() {}

    /**
     * Shows a modal skin-picker dialog populated from the same action list
     * used by the Skin menu. Fires the selected action to apply the LAF.
     */
    public static void showSkinDialog(VActionLNF[] skins) {
        String[] names = new String[skins.length];
        for (int i = 0; i < skins.length; i++) {
            names[i] = (String) skins[i].getValue(Action.NAME);
        }
        Object selected = JOptionPane.showInputDialog(
            App.getVSystem(),
            "Select a skin:",
            "Skin",
            JOptionPane.PLAIN_MESSAGE,
            null,
            names,
            names[0]
        );
        if (selected != null) {
            for (VActionLNF action : skins) {
                if (selected.equals(action.getValue(Action.NAME))) {
                    action.actionPerformed(null);
                    break;
                }
            }
        }
    }

    /**
     * Shows a modal "Are you sure?" YES/NO confirmation dialog parented to
     * the main application frame.
     *
     * @return true if the user selected YES
     */
    public static boolean confirmAreYouSure() {
        int result = JOptionPane.showConfirmDialog(
            App.getVSystem(),
            "Are you sure?",
            "Confirm",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        return result == JOptionPane.YES_OPTION;
    }

}
