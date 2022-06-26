package org.pmesmeur.sketchit.ui;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import org.jetbrains.annotations.NotNull;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.notification.NotificationType.WARNING;

public final class Notifyer {

    private static final String GROUP_DISPLAY_ID = "Settings Error";
    private static final String TITLE = "Sketch.It!";


    private Notifyer() {
    }



    public static void info(String message) {
        displayNotification(newNotification(message, INFORMATION));
    }



    @NotNull
    private static Notification newNotification(String message, NotificationType information) {
        return new Notification(GROUP_DISPLAY_ID, TITLE, message, information);
    }



    public static void warning(String message) {
        displayNotification(newNotification(message, WARNING));
    }



    private static void displayNotification(Notification notification) {
        Notifications.Bus.notify(notification);
    }

}
