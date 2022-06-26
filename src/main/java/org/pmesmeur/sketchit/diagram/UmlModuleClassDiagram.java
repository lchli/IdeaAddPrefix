package org.pmesmeur.sketchit.diagram;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.jps.model.serialization.PathMacroUtil;
import org.pmesmeur.sketchit.diagram.clazz.ClassDiagramGenerator;
import org.pmesmeur.sketchit.diagram.plantuml.PlantUmlWriter;

import java.io.File;
import java.io.IOException;


class UmlModuleClassDiagram extends UmlDiagram {
    private final Module module;


    public UmlModuleClassDiagram(Project project, Module module) {
        super(project);
        this.module = module;
    }



    @Override
    protected VirtualFile getOutputFile() throws IOException {
        File directory = new File(PathMacroUtil.getModuleDir(module.getModuleFilePath()));
       // File directory = new File(PathMacroUtil.getModuleDir(module.getName()));
        VirtualFile moduleDirectory = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(directory);

        return getClassDiagramOutputStream(moduleDirectory);
    }



    private VirtualFile getClassDiagramOutputStream(VirtualFile moduleDirectory) throws IOException {
        String outputFileName = createOutputFileName(moduleDirectory.getName());
        return moduleDirectory.findOrCreateChildData(this, outputFileName);
    }



    @Override
    protected void generateDiagram(PlantUmlWriter plantUmlWriter) {
        ClassDiagramGenerator classDiagramGenerator = getDiagramGeneratorBuilder(plantUmlWriter).build();
        classDiagramGenerator.generate();
    }



    private ClassDiagramGenerator.Builder getDiagramGeneratorBuilder(PlantUmlWriter plantUmlWriter) {
        String title = module.getName().toUpperCase() + "'s Class Diagram";

        return ClassDiagramGenerator.newBuilder(plantUmlWriter, project, module)
                .title(title)
                .hideMethods(true)
                .hideAttributes(true)
                .hideInnerClasses(true);
    }

}
