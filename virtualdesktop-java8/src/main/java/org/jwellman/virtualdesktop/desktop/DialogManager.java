package org.jwellman.virtualdesktop.desktop;

import javax.swing.JOptionPane;

import org.jwellman.virtualdesktop.App;

/**
 * Centralizes user-facing dialogs so the application presents a consistent
 * interaction style in one place.
 */
public class DialogManager {

    private DialogManager() {}

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
