package com.developerphil.adbidea.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.RenameElementAction;
import com.intellij.refactoring.rename.*;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.com.intellij.psi.PsiClass;
import org.jetbrains.kotlin.com.intellij.psi.impl.source.PsiClassImpl;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtFile;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by lichenghang .
 */
public class AddPrefixToBatchFile extends AnAction {

    private Project project;
    private DataContext context;
    private AnActionEvent e;


    @Override
    public void actionPerformed(AnActionEvent e) {
        context = e.getDataContext();
        this.e = e;

        Thread.UncaughtExceptionHandler oldErrorHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                System.err.println("[****error**********************]\n" + e.getMessage());
                VirtualFile baseDir = project.getBaseDir();
                try {
                    VirtualFile log = baseDir.findOrCreateChildData(this, "./rename.log");
                    OutputStream outs = log.getOutputStream(this);
                    e.printStackTrace(new PrintWriter(outs));
                    outs.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                oldErrorHandler.uncaughtException(t, new Exception(e.getMessage()));
            }
        });

        project = e.getProject();
        if (project == null) {
            throw new NullPointerException("project is null.");
        }

        RefactoringDialog dialog = new RefactoringDialog(project, false);
        dialog.setTitle("给res目录下所有资源文件添加前缀");
        dialog.setDoRenameListener(new RefactoringDialog.DoRenameListener() {
            @Override
            public void doRenameClass(String prefix) {
                renameClass(prefix);
            }

            @Override
            public void doRename(String prefix, String resPath) {

                VirtualFile baseDir = project.getBaseDir();
                if (baseDir == null) {
                    throw new NullPointerException("baseDir is null.");
                }
                System.err.println("baseDir path:" + baseDir.getPath());

                VirtualFile resDir = baseDir.findFileByRelativePath(resPath);
                if (resDir == null) {
                    throw new NullPointerException("resDir is null.");
                }
                System.err.println("res path:" + resDir.getPath());

                recusiveRenameResFile(resDir, prefix);
            }

            @Override
            public void doRenameContent(String prefix, String resPath) {
                VirtualFile baseDir = project.getBaseDir();
                if (baseDir == null) {
                    throw new NullPointerException("baseDir is null.");
                }
                System.err.println("baseDir path:" + baseDir.getPath());

                VirtualFile resDir = baseDir.findFileByRelativePath(resPath);
                if (resDir == null) {
                    throw new NullPointerException("resDir is null.");
                }
                System.err.println("res path:" + resDir.getPath());

                recusiveRenameContent(resDir, prefix);
            }
        });

        dialog.show();

    }

    private void recusiveRenameContent(VirtualFile dir, String prefix) {

        VfsUtilCore.visitChildrenRecursively(dir, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()) {
                    renameFileContent(file, prefix);
                }
                return super.visitFile(file);
            }
        });

    }

    private void recusiveRenameResFile(VirtualFile dir, String prefix) {

        VfsUtilCore.visitChildrenRecursively(dir, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (!file.isDirectory()) {
                    renameResFile(file, prefix);
                }
                return super.visitFile(file);
            }
        });


    }

    private void renameFileContent(VirtualFile f, String prefix) {
        System.err.println("oldName:" + f.getName());

        PsiFile psiFile = PsiManager.getInstance(project).findFile(f);
        if (psiFile == null) {
            System.err.println("psiFile is null:" + f.getName());
            return;
        }
        String oldName = psiFile.getName();
        if (!oldName.endsWith(".xml")) {//just handle xml file content.
            return;
        }

        ArrayList<XmlAttributeValue> resNames = getResNameFromLayout(psiFile, new ArrayList<XmlAttributeValue>());

        for (XmlAttributeValue xmlAttribute : resNames) {

            if (!xmlAttribute.getValue().startsWith(prefix)) {
                new RenameProcessor(project, xmlAttribute, prefix + xmlAttribute.getValue(), false, true) {
                    @Override
                    protected boolean isPreviewUsages() {
                        return false;
                    }

                    @Override
                    protected void previewRefactoring(@NotNull UsageInfo[] usages) {
                        super.execute(usages);
                    }
                }.run();
            }
        }

    }

    private void renameResFile(VirtualFile f, String prefix) {
        System.err.println("oldName:" + f.getName());

        PsiFile psiFile = PsiManager.getInstance(project).findFile(f);
        if (psiFile == null) {
            System.err.println("psiFile is null:" + f.getName());
            return;
        }
        String oldName = psiFile.getName();
        if (oldName.contains(".xml")) {
            oldName = oldName.split("\\.")[0];
        }

        if (!oldName.startsWith(prefix)) {
            RenameProcessor processor = new RenameProcessor(project, psiFile, prefix + oldName, GlobalSearchScope.projectScope(project), false, false) {
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


    }


    public final void renameClass(String prefix) {

        PsiElement[] psiElements =  LangDataKeys.PSI_ELEMENT_ARRAY.getData(context);
        if (psiElements == null || psiElements.length == 0) {
            PsiElement element =  CommonDataKeys.PSI_ELEMENT.getData(context);
            if (element != null) {
                psiElements = new PsiElement[]{element};
            }
        }

        List<PsiElement> psiElements2 = new ArrayList<>();

        psiElements[0].accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {

                if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtFile") ||
                        element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtClass") ||
                        element.getClass().getName().equals("com.intellij.psi.impl.source.PsiClassImpl")) {

                    PlugUtil.showMsg(element.getClass().getName(), project);

                    psiElements2.add(element);
                }

                super.visitElement(element);
            }
        });


        PsiElement[] psiElements8 = new PsiElement[psiElements2.size()];
        psiElements2.toArray(psiElements8);

        for (PsiElement p : psiElements8) {
            dor(p, prefix);
        }


    }

    private void dor(PsiElement element, String prefix) {
        String oldName = null;

        if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiClassImpl")) {
            oldName = getPsiClassImplName(element);
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtFile")) {
            oldName = ((org.jetbrains.kotlin.psi.KtFile) element).getName();
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtClass")) {
            oldName = ((org.jetbrains.kotlin.psi.KtClass) element).getName();
        }

        if (oldName == null) {
            return;
        }

        RenameProcessor processor = new RenameProcessor(project, element, prefix + oldName, GlobalSearchScope.projectScope(project), false, true) {
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

    private String getPsiClassImplName(PsiElement element) {
        try {
            Method getNameMethod = element.getClass().getDeclaredMethod("getName");
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


    private static ArrayList<XmlAttributeValue> getResNameFromLayout(final PsiFile file, final ArrayList<XmlAttributeValue> elements) {

        XmlDocument doc = (XmlDocument) file.getFirstChild();
        if (doc == null) {
            return elements;
        }
        XmlTag root = doc.getRootTag();
        if (root == null || !root.getName().equals("resources")) {//do not handle other layout.
            return elements;
        }

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

                    System.err.println(name);

                    elements.add(nameAttr.getValueElement());


                } finally {
                    super.visitElement(element);
                }

            }
        });

        return elements;
    }

}
