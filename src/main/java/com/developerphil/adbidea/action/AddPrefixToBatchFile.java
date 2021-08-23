package com.developerphil.adbidea.action;

import com.intellij.json.psi.impl.JsonFileImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.impl.file.PsiBinaryFileImpl;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by lichenghang .
 */
public class AddPrefixToBatchFile extends AnAction {

    private Project project;
    private DataContext context;
    private AnActionEvent mAnActionEvent;


    @Override
    public void actionPerformed(AnActionEvent e) {
        context = e.getDataContext();
        mAnActionEvent = e;
        project = e.getProject();
        if (project == null) {
            throw new NullPointerException("project is null.");
        }

        Thread.UncaughtExceptionHandler oldErrorHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                PlugUtil.showMsg(getStack(e), project);
                oldErrorHandler.uncaughtException(t, new Exception(e.getMessage()));
            }
        });


        RefactoringDialog dialog = new RefactoringDialog(project, false);
        dialog.setDoRenameListener(new RefactoringDialog.DoRenameListener() {
            @Override
            public void doRenameClass(String prefix) {
                renameClass(prefix);
            }

            @Override
            public void doRename(String prefix) {
                renameResFile(prefix);
            }

            @Override
            public void doRenameContent(String prefix) {
                renameXmlContent(prefix);
            }
        });

        dialog.show();

    }


    @Nullable
    private PsiElement findSelectedPsiElement() {
//        PsiElement[] psiElements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(context);
//        if (psiElements == null || psiElements.length == 0) {
//            PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(context);
//            if (element != null) {
//                psiElements = new PsiElement[]{element};
//            }
//        }
//
        PsiElement[] psiElements = BaseRefactoringAction.getPsiElementArray(context);
        if (psiElements == null || psiElements.length <= 0) {
            return null;
        }

        return psiElements[0];

    }

    private interface ConditionPredicate {
        boolean isMatch(PsiElement element);
    }

    private void findMatchedChildrenInDirOnly(PsiElement startRoot, List<PsiElement> result, ConditionPredicate condition) {
        if (startRoot == null || result == null) {
            return;
        }

        if (condition.isMatch(startRoot)) {
            result.add(startRoot);
        }

        PsiElement[] children = startRoot.getChildren();
        if (children == null || children.length <= 0) {
            PlugUtil.showMsg("children is null:" + startRoot, project);
            return;
        }
        for (PsiElement e : children) {
            if (e instanceof PsiDirectory) {
                findMatchedChildrenInDirOnly(e, result, condition);
            } else {
                if (condition.isMatch(e)) {
                    result.add(e);
                }
            }
        }
    }


    private void findJavaKotlin(PsiElement startRoot, Map<String,PsiElement> result, ConditionPredicate condition) {
        if (startRoot == null || result == null) {
            return;
        }

        if (condition.isMatch(startRoot)) {
           String name=getOldName(startRoot);
           if(name!=null) {
               result.put(name,startRoot);
           }
        }

        PsiElement[] children = startRoot.getChildren();
        if (children == null || children.length <= 0) {
            PlugUtil.showMsg("children is null:" + startRoot, project);
            return;
        }
        for (PsiElement e : children) {
            if (e instanceof PsiDirectory ||isDir(e)||
                    isJavaClass(e)||isJavaFile(e)||
            isKtClass(e)||isKtFile(e)) {
                findJavaKotlin(e, result, condition);
            } else {
                if (condition.isMatch(e)) {
                    String name=getOldName(e);
                    if(name!=null) {
                        result.put(name,e);
                    }
                }
            }
        }
    }


    public final void renameXmlContent(String prefix) {

        @Nullable PsiElement startRoot = findSelectedPsiElement();
        PlugUtil.showMsg("startRoot:" + startRoot, project);

        if (startRoot == null) {
            showStartRootNullMsg();
            return;
        }
        List<PsiElement> result = new ArrayList<>();

        findMatchedChildrenInDirOnly(startRoot, result, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl")) {

                    return true;
                }

                return false;
            }
        });

        for (PsiElement xmlFile : result) {
            renameContent(xmlFile, prefix);
        }

    }


    private void renameContent(PsiElement xmlFile, String prefix) {
        List<PsiElement> result = getResNameFromLayout(xmlFile);
        if (result == null || result.isEmpty()) {
            PlugUtil.showMsg("skip xml:" + xmlFile, project);
            return;
        }

        for (PsiElement p : result) {
            //PlugUtil.showMsg(p.toString() + "@" + p.getClass().getName(), project);
            dor(p, prefix);
        }
    }

    private void showStartRootNullMsg(){
        PlugUtil.showMsg("请选择要重构的目录", project,true);

    }

    public final void renameResFile(String prefix) {

        @Nullable PsiElement startRoot = findSelectedPsiElement();
        PlugUtil.showMsg("startRoot:" + startRoot, project);
        if (startRoot == null) {
            showStartRootNullMsg();
            return;
        }
        List<PsiElement> result = new ArrayList<>();

        findMatchedChildrenInDirOnly(startRoot, result, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl") ||
                        element.getClass().getName().equals("com.intellij.psi.impl.file.PsiBinaryFileImpl") ||
                        element.getClass().getName().equals("com.intellij.json.psi.impl.JsonFileImpl")) {

                    return true;
                }

                return false;
            }
        });

        for (PsiElement p : result) {
            dor(p, prefix);
        }

    }


    public final void renameClass(String prefix) {
        @Nullable PsiElement startRoot = findSelectedPsiElement();
        PlugUtil.showMsg("startRoot:" + startRoot, project);

        if (startRoot == null) {
            showStartRootNullMsg();
            return;
        }

        Map<String,PsiElement> result = new HashMap<>();
        findJavaKotlin(startRoot, result, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                PlugUtil.showMsg("element:" + element+"@"+element.getClass().getName(), project);

                return isKtClass(element)|| isJavaClass(element)||isKtFile(element);
            }
        });

        for (PsiElement p : result.values()) {
            dor(p, prefix);
        }


    }

    private boolean isDir(PsiElement element){
        return element.getClass().getName().equals("com.intellij.psi.impl.file.PsiJavaDirectoryImpl");
    }

    private boolean isKtFile(PsiElement element){
        return element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtFile");
    }
    private boolean isKtClass(PsiElement element){
        return element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtClass");
    }

    private boolean isJavaFile(PsiElement element){
        return element.getClass().getName().equals("com.intellij.psi.impl.source.PsiJavaFileImpl");
    }

    private boolean isJavaClass(PsiElement element){
        return element.getClass().getName().equals("com.intellij.psi.impl.source.PsiClassImpl");
    }

    private Class findCls(String classname){
        try {
           return Class.forName(classname);
        }catch (Throwable e){
            PlugUtil.showMsg(getStack(e),project);
            return null;
        }
    }

    private String getOldName(PsiElement element){
        String oldName = null;

        if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiClassImpl")) {
            oldName = getPsiClassImplName(element,element.getClass());
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiJavaFileImpl")) {
            oldName = getPsiClassImplName(element,findCls("com.intellij.psi.impl.source.PsiFileImpl"));
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtFile")) {
            oldName = ((org.jetbrains.kotlin.psi.KtFile) element).getName();
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtClass")) {
            oldName = ((org.jetbrains.kotlin.psi.KtClass) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl")) {
            oldName = ((XmlFileImpl) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.file.PsiBinaryFileImpl")) {
            oldName = ((PsiBinaryFileImpl) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.json.psi.impl.JsonFileImpl")) {
            oldName = ((JsonFileImpl) element).getName();
        } else if (element instanceof XmlAttributeValue) {
            oldName = ((XmlAttributeValue) element).getValue();
        }

        if (oldName == null) {
            return null;
        }

        int index=oldName.indexOf(".");
        if (index>=0) {
            oldName = oldName.substring(0,index);
        }
       // PlugUtil.showMsg("oldName:" + oldName, project);

        return oldName;
    }

    private void dor(PsiElement element, String prefix) {
        String oldName = null;
        boolean isSearchTextOccurrences = false;

        if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiClassImpl")) {
            oldName = getPsiClassImplName(element,element.getClass());
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiJavaFileImpl")) {
            oldName = getPsiClassImplName(element,findCls("com.intellij.psi.impl.source.PsiFileImpl"));
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtFile")) {
            oldName = ((org.jetbrains.kotlin.psi.KtFile) element).getName();
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtClass")) {
            oldName = ((org.jetbrains.kotlin.psi.KtClass) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl")) {
            oldName = ((XmlFileImpl) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.file.PsiBinaryFileImpl")) {
            oldName = ((PsiBinaryFileImpl) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.json.psi.impl.JsonFileImpl")) {
            oldName = ((JsonFileImpl) element).getName();
        } else if (element instanceof XmlAttributeValue) {
            oldName = ((XmlAttributeValue) element).getValue();
            isSearchTextOccurrences = true;
        }

        if (oldName == null) {
            return;
        }

        if(!oldName.endsWith(".kt")) {
            int index = oldName.indexOf(".");
            if (index >= 0) {
                oldName = oldName.substring(0, index);
            }
        }
        PlugUtil.showMsg("oldName:" + oldName, project);

        if (oldName.startsWith("AndroidManifest")) {
            return;
        }

        if (oldName.startsWith(prefix)) {
            return;
        }

        PlugUtil.showMsg("run processor:" + oldName, project);

        RenameProcessor processor = new RenameProcessor(project, element, prefix + oldName, GlobalSearchScope.projectScope(project), false, isSearchTextOccurrences) {
            @Override
            protected boolean isPreviewUsages() {
                return false;
            }

            @Override
            protected void previewRefactoring(@NotNull UsageInfo[] usages) {
                super.execute(usages);
            }

        };
        processor.run();
    }

    private String getPsiClassImplName(PsiElement element,Class clazz) {
        try {
            Method getNameMethod = clazz.getDeclaredMethod("getName");
            getNameMethod.setAccessible(true);
            return getNameMethod.invoke(element).toString();

        } catch (Throwable e) {
            e.printStackTrace();
            PlugUtil.showMsg(getStack(e), project);
            return null;
        }
    }

    private String getStack(Throwable e) {
        String ret = "";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        ret = sw.toString();
        pw.close();
        return ret;

    }


    private static List<PsiElement> getResNameFromLayout(final PsiElement file) {
        XmlDocument doc = (XmlDocument) file.getFirstChild();
        if (doc == null) {
            return null;
        }
        XmlTag root = doc.getRootTag();
        if (root == null || !root.getName().equals("resources")) {//do not handle other layout.
            return null;
        }

        List<PsiElement> elements = new ArrayList<>();

        file.accept(new XmlRecursiveElementVisitor() {

            @Override
            public void visitElement(final PsiElement element) {
                try {

                    if (!(element instanceof XmlTag)) {
                        return;
                    }

                    XmlTag tag = (XmlTag) element;
                    String name = tag.getName();

                    if (name.equals("declare-styleable") || name.equals("attr")) {
                        return;
                    }

                    // get element ID
                    XmlAttribute nameAttr = tag.getAttribute("name", null);
                    if (nameAttr == null) {
                        return; // missing android:id attribute
                    }
                    String value = nameAttr.getValue();
                    if (value == null) {
                        return; // empty value
                    }
                    XmlTag parent = tag.getParentTag();
                    if (parent != null && parent.getName().equals("style")) {
                        return;
                    }

                    elements.add(nameAttr.getValueElement());
                } finally {
                    super.visitElement(element);
                }

            }
        });

        return elements;
    }

}
