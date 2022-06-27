package org.pmesmeur.sketchit.diagram;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.pmesmeur.sketchit.PlugUtil;
import org.pmesmeur.sketchit.ui.Notifyer;

import java.util.Set;


public class UmlDiagramsGenerator {
    private static final Logger LOG = Logger.getInstance(UmlDiagramsGenerator.class);

    private final Project project;
    private AnActionEvent mAnActionEvent;

    public UmlDiagramsGenerator(Project project, AnActionEvent event) {
        this.project = project;
        mAnActionEvent = event;
    }


    public void generateComponentDiagram() {
        LOG.info("Generating project component diagram");
        UmlComponentDiagram umlComponentDiagram = new UmlComponentDiagram(project);
        umlComponentDiagram.generate();
    }


    public void generateClassDiagrams() {
        PsiElement[] psiElements = BaseRefactoringAction.getPsiElementArray(mAnActionEvent.getDataContext());
        if (psiElements == null || psiElements.length <= 0) {
            Notifyer.info("未选择元素=================!!!");
            return;
        }
        PsiElement selectedPsiElement = psiElements[0];

        Notifyer.info("selectedPsiElement:" + selectedPsiElement);

        Module module = null;

        if (selectedPsiElement instanceof PsiFile) {
            module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(((PsiFile) selectedPsiElement).getVirtualFile());

        } else if (selectedPsiElement instanceof PsiDirectory) {
            module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(((PsiDirectory) selectedPsiElement).getVirtualFile());
        } else if (selectedPsiElement instanceof PsiClass) {
            module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(((PsiClass) selectedPsiElement).getContainingFile().getVirtualFile());
        } else if (selectedPsiElement instanceof KtClass) {
            module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(((KtClass) selectedPsiElement).getContainingFile().getVirtualFile());
        } else if (selectedPsiElement instanceof KtObjectDeclaration) {
            module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(((KtObjectDeclaration) selectedPsiElement).getContainingFile().getVirtualFile());
        }


        if (module == null) {
            Notifyer.info("未选择module=================!!!");
            return;
        }
        Notifyer.info("selected module:" + module.getName());

        generateModuleClassDiagram(module);
        generateModuleClassDiagramForEachSourceDirectory(module, selectedPsiElement);

//        ModuleManager moduleManager = ModuleManager.getInstance(project);
//        for (Module module : moduleManager.getModules()) {
//            Notifyer.info("module:" + module.getName());
//            Notifyer.info("module:" + module.getModuleFile().getCanonicalPath());
//
//                generateModuleClassDiagram(module);
//                generateModuleClassDiagramForEachSourceDirectory(module);
//
//        }
    }


    private void generateModuleClassDiagram(Module module) {
        UmlModuleClassDiagram umlModuleClassDiagram = new UmlModuleClassDiagram(project, module);
        umlModuleClassDiagram.generate();
    }


    private void generateModuleClassDiagramForEachSourceDirectory(Module module, PsiElement selectedPsiElement) {

        JavaFileFinder javaFileFinder = new JavaFileFinder(project, module);

        Set<VirtualFile> directories = javaFileFinder.getFoundDirectories();

        Notifyer.info("selectedPsiElement:" + (selectedPsiElement instanceof PsiJavaFile));
        Notifyer.info("selectedPsiElement:" + (selectedPsiElement instanceof PsiClass));
        Notifyer.info("selectedPsiElement:" + (selectedPsiElement instanceof KtFile));
        Notifyer.info("selectedPsiElement:" + (selectedPsiElement instanceof KtClass));
        Notifyer.info("selectedPsiElement:" + (selectedPsiElement instanceof KtObjectDeclaration));


        if (selectedPsiElement instanceof PsiDirectory) {
            VirtualFile selectedVirtualFile = ((PsiDirectory) selectedPsiElement).getVirtualFile();

            for (VirtualFile directory : directories) {
                PlugUtil.showMsg("directory:" + directory.getCanonicalPath());
                if (directory.getCanonicalPath().equals(selectedVirtualFile.getCanonicalPath())) {
                    generateModuleClassDiagramForSourceDirectory(module, directory, null);
                    break;
                }
            }

        } else if (selectedPsiElement instanceof PsiClass) {
            VirtualFile dir = selectedPsiElement.getContainingFile().getContainingDirectory().getVirtualFile();
            generateModuleClassDiagramForSourceDirectory(module, dir, new PsiClass[]{(PsiClass) selectedPsiElement});

//            for (VirtualFile directory : directories) {
//                generateModuleClassDiagramForSourceDirectory(module, directory, new PsiClass[]{(PsiClass) selectedPsiElement});
//            }

        } else if (selectedPsiElement instanceof KtFile) {
            VirtualFile dir = selectedPsiElement.getContainingFile().getContainingDirectory().getVirtualFile();
            generateModuleClassDiagramForSourceDirectory(module, dir, ((KtFile) selectedPsiElement).getClasses());

//            for (VirtualFile directory : directories) {
//                generateModuleClassDiagramForSourceDirectory(module, directory, ((KtFile) selectedPsiElement).getClasses());
//            }
        } else if (selectedPsiElement instanceof KtClass) {
            VirtualFile dir = selectedPsiElement.getContainingFile().getContainingDirectory().getVirtualFile();
            generateModuleClassDiagramForSourceDirectory(module, dir, ((KtClass) selectedPsiElement).getContainingKtFile().getClasses());
//
//            for (VirtualFile directory : directories) {
//                generateModuleClassDiagramForSourceDirectory(module, directory, ((KtClass) selectedPsiElement).getContainingKtFile().getClasses());
//            }
        } else if (selectedPsiElement instanceof KtObjectDeclaration) {
            VirtualFile dir = selectedPsiElement.getContainingFile().getContainingDirectory().getVirtualFile();
            generateModuleClassDiagramForSourceDirectory(module, dir, ((KtObjectDeclaration) selectedPsiElement).getContainingKtFile().getClasses());

//            for (VirtualFile directory : directories) {
//                generateModuleClassDiagramForSourceDirectory(module, directory, ((KtObjectDeclaration) selectedPsiElement).getContainingKtFile().getClasses());
//            }
        }


    }


    private void generateModuleClassDiagramForSourceDirectory(Module module, VirtualFile directory, PsiClass[] targetClasses) {
        UmlSourceDirectoryClassDiagram umlSourceDirectoryClassDiagram = new UmlSourceDirectoryClassDiagram(project, module, directory, targetClasses);
        umlSourceDirectoryClassDiagram.generate();
    }

}
