package org.pmesmeur.sketchit.diagram;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.pmesmeur.sketchit.diagram.component.ComponentDiagramGenerator;
import org.pmesmeur.sketchit.diagram.plantuml.PlantUmlWriter;

import java.io.IOException;


class UmlComponentDiagram extends UmlDiagram {

    public UmlComponentDiagram(Project project) {
        super(project);
    }



    @Override
    protected VirtualFile getOutputFile() throws IOException {
        String outputFileName = createOutputFileName(project.getName());
        return project.getBaseDir().findOrCreateChildData(this, outputFileName);
    }



    @Override
    protected void generateDiagram(PlantUmlWriter plantUmlWriter) {
        ComponentDiagramGenerator componentDiagramGenerator = getDiagramGeneratorBuilder(plantUmlWriter).build();
        componentDiagramGenerator.generate();
    }



    private ComponentDiagramGenerator.Builder getDiagramGeneratorBuilder(PlantUmlWriter plantUmlWriter) {
        String title = project.getName().toUpperCase() + "'s Component Diagram";

        return ComponentDiagramGenerator.newBuilder(plantUmlWriter, project)
                                        .title(title)
                                        .exclude("test")
                                        .exclude("feature");
    }

}
