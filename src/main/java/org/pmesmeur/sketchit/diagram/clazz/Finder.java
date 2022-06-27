package org.pmesmeur.sketchit.diagram.clazz;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import org.jetbrains.kotlin.psi.KtFile;
import org.pmesmeur.sketchit.diagram.JavaFileFinder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Finder {

    private final Project project;
    private final Module module;
    private Set<PsiClass> classes;
    private Set<String> packages;


    public Finder(Project project, Module module, PsiClass[] targetClasses) {
        this.project = project;
        this.module = module;
        this.packages = new HashSet<String>();
        this.classes = findClasses(targetClasses);
    }


    public Set<PsiClass> getClasses() {
        return classes;
    }


    public Set<String> getPackages() {
        return packages;
    }


    private Set<PsiClass> findClasses(PsiClass[] targetClasses) {
        JavaFileFinder javaFileFinder = new JavaFileFinder(project, module);

        return computeManagedPsiClassesFromFiles(javaFileFinder.getFoundFiles(), javaFileFinder.getFoundFilesKt(), targetClasses);
    }


    private Set<PsiClass> computeManagedPsiClassesFromFiles(List<PsiJavaFile> pfiles, List<KtFile> ktFiles, PsiClass[] targetClasses) {
        Set<PsiClass> managedPsiClasses = new HashSet<PsiClass>();
        if (targetClasses != null) {//todo??
            for (PsiClass clazz : targetClasses) {
                PsiFile theContainingFile = clazz.getContainingFile();
                if (theContainingFile instanceof PsiJavaFile) {
                    recordFilePackageAsKnownPackage((PsiJavaFile) theContainingFile);
                } else if (theContainingFile instanceof KtFile) {
                    recordFilePackageAsKnownPackage((KtFile) theContainingFile);
                }

                managedPsiClasses.add(clazz);
            }

            return managedPsiClasses;
        }

        for (PsiJavaFile file : pfiles) {
            recordFilePackageAsKnownPackage(file);

            if (!isTestFile(file)) {
                PsiClass[] classes = file.getClasses();
                for (PsiClass clazz : classes) {
                    if (isIn(targetClasses, clazz)) {
                        managedPsiClasses.add(clazz);
                    }
                }
            }
        }

        for (KtFile file : ktFiles) {
            recordFilePackageAsKnownPackage(file);

            if (!isTestFile(file)) {
                PsiClass[] classes = file.getClasses();
                for (PsiClass clazz : classes) {
                    if (isIn(targetClasses, clazz)) {
                        managedPsiClasses.add(clazz);
                    }
                }
            }
        }

        return managedPsiClasses;
    }

    private boolean isIn(PsiClass[] targetClasses, PsiClass clazz) {
        if (targetClasses == null) {
            return true;
        }
        for (PsiClass psiClass : targetClasses) {
            if (psiClass.getQualifiedName().equals(clazz.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }


    private void recordFilePackageAsKnownPackage(PsiJavaFile local) {
        packages.add(local.getPackageName());
    }

    private void recordFilePackageAsKnownPackage(KtFile local) {
        packages.add(local.getPackageName());
    }


    private boolean isTestFile(PsiFile file) {
        return ModuleRootManager.getInstance(module)
                .getFileIndex()
                .isUnderSourceRootOfType(file.getVirtualFile(),
                        JavaModuleSourceRootTypes.TESTS);
    }

}
