package com.developerphil.adbidea.action;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.RenameElementAction;
import org.jetbrains.annotations.NotNull;

public class Myact extends RenameElementAction {

    @Override
    public RefactoringActionHandler getHandler(@NotNull DataContext dataContext) {
        PsiElement[] elements = getPsiElementArray(dataContext);
        Project p = CommonDataKeys.PROJECT.getData(dataContext);

        for (PsiElement e : elements)
            PlugUtil.showMsg(e.toString(), p);

        return super.getHandler(dataContext);
    }
}
