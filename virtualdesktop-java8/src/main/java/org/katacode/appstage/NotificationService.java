package org.katacode.appstage;

/**
 * Transient user feedback overlaid on the application stage (typically POPUP layer).
 */
public interface NotificationService {

    void showToast(String title, String message, ToastType type);

    void showToast(String message, int durationMillis);

    void clearAllToasts();
}
