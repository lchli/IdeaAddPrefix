package com.developerphil.adbidea.action;

import com.developerphil.adbidea.ui.NotificationHelper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.refactoring.util.CommonRefactoringUtil;

public class PlugUtil {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.ComparingReferencesInspection");

    public static void showMsg(String msg, Project project) {
        showMsg(msg,project,false);
    }

    public static void showMsg(String msg, Project project, boolean showDialog) {
        NotificationHelper.INSTANCE.info(msg);

        if (showDialog) {
            CommonRefactoringUtil.showErrorMessage("提示", msg, null, project);
        }
    }
}
