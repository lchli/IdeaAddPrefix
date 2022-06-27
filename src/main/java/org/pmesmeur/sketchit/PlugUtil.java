package org.pmesmeur.sketchit;

import com.android.ddmlib.AdbDevice;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.lch.puml.MLogUtils;
import org.pmesmeur.sketchit.ui.Notifyer;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PlugUtil {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.ComparingReferencesInspection");

    public static void showMsg(String msg, Project project) {
        showMsg(msg,project,false);
    }

    public static void showMsg(String msg) {
        showMsg(msg,null,false);
    }


    public static void showMsg(String msg, Project project, boolean showDialog) {
        Notifyer.info(msg);

        MLogUtils.INSTANCE.log();
    }

    public static void showMsg(Throwable msg, Project project) {
        showMsg(getStack(msg),project,false);
    }

    private static String getStack(Throwable e) {
        String ret = "";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        ret = sw.toString();
        pw.close();
        return ret;

    }
}