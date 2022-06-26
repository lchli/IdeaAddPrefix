package org.pmesmeur.sketchit.diagram;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class JavaFileFinder {

    private final Project project;
    private final Module module;
    private final List<PsiJavaFile> javaFiles;
    private final List<KtFile> ktFiles;
    private final Set<VirtualFile> directories;


    public JavaFileFinder(Project project, Module module) {
        this.project = project;
        this.module = module;
        this.javaFiles = new ArrayList<PsiJavaFile>();
        this.ktFiles = new ArrayList<KtFile>();
        this.directories = new HashSet<VirtualFile>();

        find();
    }



    public List<PsiJavaFile> getFoundFiles() {
        return javaFiles;
    }

    public List<KtFile> getFoundFilesKt() {
        return ktFiles;
    }



    public Set<VirtualFile> getFoundDirectories() {
        return directories;
    }



    private void find()
    {
        VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        for (VirtualFile file : roots)
        {
            PsiDirectory dir = PsiManager.getInstance(project).findDirectory(file);
            if (dir != null)
            {
                findFiles(dir, true);
            }
        }
    }



    private void findFiles(PsiDirectory directory, boolean subdirs)
    {
        PsiFile[] locals = directory.getFiles();
        for (PsiFile local : locals)
        {
            if (psiFileBelongsToCurrentModule(local) && (local instanceof PsiJavaFile)) {
                javaFiles.add((PsiJavaFile) local);
                directories.add(local.getParent().getVirtualFile());
            }

            if (psiFileBelongsToCurrentModule(local) && (local instanceof KtFile)) {
                ktFiles.add((KtFile) local);
                directories.add(local.getParent().getVirtualFile());
            }
        }

        if (subdirs)
        {
            PsiDirectory[] dirs = directory.getSubdirectories();
            for (PsiDirectory dir : dirs)
            {
                findFiles(dir, subdirs);
            }

        }
    }



    private boolean psiFileBelongsToCurrentModule(PsiFile file) {
        ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
        ProjectFileIndex projectFileIndex = projectRootManager.getFileIndex();
        Module fileModule = projectFileIndex.getModuleForFile(file.getVirtualFile());

        return module.equals(fileModule);
    }

}
