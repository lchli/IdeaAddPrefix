package org.pmesmeur.sketchit.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;


public final class ExceptionDialog {

    private ExceptionDialog() {}


    public static void show(Project project, Throwable e) {
        Messages.showErrorDialog(project, text(e), "Exception Received");

    }



    private static String text(Throwable e) {
        String text = introduction(e);
        return text + "\n\n" + stackTraceToText(e);
    }



    private static String introduction(Throwable e) {
        String text = "Unexpected exception received while running Sketch It!: " + e.getClass().getName() + "\n";
        text += "Please, do not hesitate to report it:\n";
        text += " * by mail at philippe.mesmeur@gmail.com\n";
        text += " * by opening an issue at https://bitbucket.org/pmesmeur/sketch.it/issues\n";
        text += "\n";
        text += "For an efficient investigation, please do not forget to provide:\n";
        text += " * the call stack that follows the current message\n";
        text += " * the IDEA log file (to find it, select entry \"Show Log in Explorer\", from \"Help\" menu)\n";
        text += "\n";
        text += "Sorry for inconvenience and thank you for your contribution.";
        return text;
    }



    private static String stackTraceToText(Throwable e) {
        String text = "Call stack:\n";
        return text + stackTraceToText(e.getStackTrace());
    }



    private static String stackTraceToText(StackTraceElement[] stackTrace) {
        String strStackTrace = stackTrace[0].toString();

        for (int i = 1 ; i < stackTrace.length ; i++) {
            strStackTrace += "\n" + stackTrace[i].toString();
        }

        return strStackTrace;
    }


}
