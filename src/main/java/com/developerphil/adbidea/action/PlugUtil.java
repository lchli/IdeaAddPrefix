package com.developerphil.adbidea.action;

import com.developerphil.adbidea.ui.NotificationHelper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.diagnostic.Logger;

public class PlugUtil {
    private static final Logger LOG = Logger.getInstance("#com.intellij.codeInspection.ComparingReferencesInspection");

    public static void showMsg(String msg, Project project){
        NotificationHelper.INSTANCE.info(msg);
       // CommonRefactoringUtil.showErrorMessage("msg",msg, null, project);
    }
}
