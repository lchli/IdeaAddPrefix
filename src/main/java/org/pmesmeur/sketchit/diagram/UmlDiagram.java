package org.pmesmeur.sketchit.diagram;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.pmesmeur.sketchit.diagram.plantuml.PlantUmlWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.NoSuchElementException;


abstract class UmlDiagram {
    private static final Logger LOG = Logger.getInstance(UmlDiagram.class);

    private static final String FILE_EXTENSION = "plantuml";

    protected final Project project;


    protected UmlDiagram(Project project) {
        this.project = project;
    }



    public void generate() {
        VirtualFile outputFile = null;

        try {
            outputFile = getOutputFile();
            OutputStream outputStream = outputFile.getOutputStream(this);

            generateDiagram(outputStream);

            outputStream.close();
        } catch (NoSuchElementException e) {
            LOG.info("Output file empty: deleting it");
            deleteEmptyFile(outputFile);
        } catch (IOException e) {
            LOG.info("Error while generating diagram");
            e.printStackTrace();
        } finally {
            DumbServiceImpl.getInstance(project).completeJustSubmittedTasks(); /// Issue-27
        }
    }



    protected abstract VirtualFile getOutputFile() throws IOException;



    private void generateDiagram(OutputStream outputStream) {
        generateDiagram(new PlantUmlWriter(outputStream));
    }



    protected abstract void generateDiagram(PlantUmlWriter plantUmlWriter);


    private void deleteEmptyFile(VirtualFile outputFile) {
        if (outputFile != null) {
            try {
                outputFile.delete(this);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }



    protected String createOutputFileName(String name) {
        return name + "." + FILE_EXTENSION;
    }

}
