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
 * Created by lichenghang .
 */
public class RefactoringDialog extends DialogWrapper {

    private static final String HELP_MSG = "用于给android的module添加资源前缀，以避免资源冲突。" +
            "\nrenameResFiles:重命名所有资源文件。\nrenameXmlContentRes:重命名每个资源文件里面的name属性。\nrenameJavaKotlin:重命名每个Java和kotlin文件。" +
            "\n执行上面任一操作后需要rebuild一下才应该执行下一个操作，以确保引用的刷新。\n作者：李成航";

    private final EditorTextField editorTextField;
    private final EditorTextField editorTextFieldOldPrefix;
    private final JPanel jbPanel;
    private Action myRefactorAction;
    protected final Project myProject;
    private RefactorContentAction myRefactorContentAction;
    private Action mRefactorJavaKotlinClassAction;
    private Action mRefactorActivityFragmentAction;
    private Action mRefactorBindingAction;

    protected RefactoringDialog(@NotNull Project project, boolean canBeParent) {
        super(project, canBeParent);
        myProject = project;
        editorTextField = new EditorTextField("请输入prefix", myProject, null);
        editorTextField.setMinimumSize(new Dimension(300, 50));

        editorTextFieldOldPrefix = new EditorTextField("请输入old prefix,没有则为空", myProject, null);
        editorTextFieldOldPrefix.setMinimumSize(new Dimension(300, 50));

        jbPanel = new JPanel();
        jbPanel.setLayout(new GridLayout(3, 1));
        jbPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        jbPanel.add(editorTextField);
        jbPanel.add(editorTextFieldOldPrefix);

        setTitle("给选中目录下所有资源添加前缀");

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
        mRefactorActivityFragmentAction = new RefactorActivityFragmentAction();
        mRefactorBindingAction = new RefactorBindingAction();
    }


    public interface DoRenameListener {
        void doRename(String prefix,String oldPrefix);

        void doRenameContent(String prefix,String oldPrefix);

        void doRenameClass(String prefix,String oldPrefix);

        void doRenameActivityFragment(String prefix,String oldPrefix);

        void doRenameBinding(String prefix,String oldPrefix);
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
                mDoRenameListener.doRenameContent(editorTextField.getText(),editorTextFieldOldPrefix.getText());
            } else {
                mDoRenameListener.doRename(editorTextField.getText(),editorTextFieldOldPrefix.getText());
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
        actions.add(mRefactorActivityFragmentAction);
        actions.add(mRefactorBindingAction);

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
            putValue(Action.NAME, "renameXmlContentRes");
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
            mDoRenameListener.doRenameClass(editorTextField.getText(),editorTextFieldOldPrefix.getText());
        }
    }

    private class RefactorActivityFragmentAction extends AbstractAction {

        public RefactorActivityFragmentAction() {
            putValue(Action.NAME, "renameActivityFragment");
            putValue(DEFAULT_ACTION, Boolean.FALSE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (DumbService.isDumb(myProject)) {
                Messages.showMessageDialog(myProject, "Refactoring is not available while indexing is in progress", "Indexing", null);
                return;
            }
            closeOKAction();
            mDoRenameListener.doRenameActivityFragment(editorTextField.getText(),editorTextFieldOldPrefix.getText());
        }
    }

    private class RefactorBindingAction extends AbstractAction {

        public RefactorBindingAction() {
            putValue(Action.NAME, "renameBinding");
            putValue(DEFAULT_ACTION, Boolean.FALSE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (DumbService.isDumb(myProject)) {
                Messages.showMessageDialog(myProject, "Refactoring is not available while indexing is in progress", "Indexing", null);
                return;
            }
            closeOKAction();
            mDoRenameListener.doRenameBinding(editorTextField.getText(),editorTextFieldOldPrefix.getText());
        }
    }

}