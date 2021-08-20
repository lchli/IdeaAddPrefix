package com.developerphil.adbidea.action;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * lich。
 */
public class RefactoringDialog extends DialogWrapper {

    private static final String HELP_MSG = "用于给android的module添加资源前缀，以避免资源冲突。" +
            "\nrenameResFiles:重命名所有资源文件。\nrenameContentRes:重命名每个资源文件里面的name属性。" +
            "\n执行上面任一操作后需要rebuild一下才应该执行下一个操作，以确保引用的刷新。\n作者：老李";

    private final EditorTextField editorTextField;
    private final EditorTextField editorTextFieldResPath;
    private final JPanel jbPanel;
    private Action myRefactorAction;
    protected final Project myProject;
    private RefactorContentAction myRefactorContentAction;
    private Action mRefactorJavaKotlinClassAction;

    protected RefactoringDialog(@NotNull Project project, boolean canBeParent) {
        super(project, canBeParent);
        myProject = project;
        editorTextField = new EditorTextField("请输入prefix", myProject, null);
        editorTextField.setMinimumSize(new Dimension(300, 100));

        editorTextFieldResPath = new EditorTextField("./app/src/main/res", myProject, null);
        editorTextFieldResPath.setMinimumSize(new Dimension(300, 100));

        jbPanel = new JPanel();
        jbPanel.setLayout(new GridLayout(2, 1));
        jbPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        jbPanel.add(editorTextField);
        jbPanel.add(editorTextFieldResPath);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {

        return jbPanel;
    }


    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        myRefactorAction = new RefactorAction();
        myRefactorContentAction = new RefactorContentAction();
        mRefactorJavaKotlinClassAction = new RefactorJavaKotlinClassAction();
    }


    public interface DoRenameListener {
        void doRename(String prefix, String resPath);

        void doRenameContent(String prefix, String resPath);
        void doRenameClass(String prefix);
    }

    private DoRenameListener mDoRenameListener;

    public void setDoRenameListener(DoRenameListener listener) {
        mDoRenameListener = listener;

    }


    protected void doRefactorAction(boolean isRenameContent) {
        if (DumbService.isDumb(myProject)) {
            Messages.showMessageDialog(myProject, "Refactoring is not available while indexing is in progress", "Indexing", null);
            return;
        }

        if (mDoRenameListener != null) {
            closeOKAction();

            if (isRenameContent) {
                mDoRenameListener.doRenameContent(editorTextField.getText(), editorTextFieldResPath.getText());
            } else {
                mDoRenameListener.doRename(editorTextField.getText(), editorTextFieldResPath.getText());
            }
        }
    }

    private void closeOKAction() {
        super.doOKAction();
    }


    @Override
    @NotNull
    protected Action[] createActions() {
        List<Action> actions = new ArrayList<>();
        actions.add(myRefactorAction);

        actions.add(myRefactorContentAction);
        actions.add(mRefactorJavaKotlinClassAction);

        actions.add(getHelpAction());

        if (SystemInfo.isMac) {
            Collections.reverse(actions);
        }
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    protected void doHelpAction() {
        Messages.showMessageDialog(myProject, HELP_MSG, "简介", null);
    }


    private class RefactorAction extends AbstractAction {

        public RefactorAction() {
            putValue(Action.NAME, "renameResFiles");
            putValue(DEFAULT_ACTION, Boolean.TRUE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doRefactorAction(false);
        }
    }


    private class RefactorContentAction extends AbstractAction {

        public RefactorContentAction() {
            putValue(Action.NAME, "renameContentRes");
            putValue(DEFAULT_ACTION, Boolean.FALSE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doRefactorAction(true);
        }
    }

    private class RefactorJavaKotlinClassAction extends AbstractAction {

        public RefactorJavaKotlinClassAction() {
            putValue(Action.NAME, "renameJavaKotlin");
            putValue(DEFAULT_ACTION, Boolean.FALSE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (DumbService.isDumb(myProject)) {
                Messages.showMessageDialog(myProject, "Refactoring is not available while indexing is in progress", "Indexing", null);
                return;
            }
            closeOKAction();
            mDoRenameListener.doRenameClass(editorTextField.getText());
        }
    }

}