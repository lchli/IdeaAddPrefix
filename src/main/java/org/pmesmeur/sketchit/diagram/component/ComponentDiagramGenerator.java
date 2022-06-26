package org.pmesmeur.sketchit.diagram.component;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import org.jetbrains.annotations.NotNull;
import org.pmesmeur.sketchit.diagram.plantuml.PlantUmlWriter;
import org.pmesmeur.sketchit.diagram.sorters.ModuleSorter;

import java.util.*;


public class ComponentDiagramGenerator {

    private static final Logger LOG = Logger.getInstance(ComponentDiagramGenerator.class);

    private final PlantUmlWriter plantUmlWriter;
    private final Project project;
    private final List<String> patternsToExclude;
    private final Set<Module> managedModules;
    private final String title;


    public static Builder newBuilder(PlantUmlWriter plantUmlWriter, Project project) {
        return new Builder(plantUmlWriter, project);
    }



    public static class Builder {
        private final PlantUmlWriter plantUmlWriter;
        private final Project project;
        private final List<String> patternsToExclude;
        private String title;


        public Builder(PlantUmlWriter plantUmlWriter, Project project) {
            this.plantUmlWriter = plantUmlWriter;
            this.project = project;
            this.patternsToExclude = new ArrayList<String>();
        }


        public Builder exclude(String patternToExclude) {
            patternsToExclude.add(patternToExclude);
            return this;
        }


        public ComponentDiagramGenerator build() {
            return new ComponentDiagramGenerator(this);
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }
    }



    protected ComponentDiagramGenerator(Builder builder) {
        this.plantUmlWriter = builder.plantUmlWriter;
        this.project = builder.project;
        this.patternsToExclude = builder.patternsToExclude;
        this.title = builder.title;
        this.managedModules = computeManagedModuleList();
    }



    private Set<Module> computeManagedModuleList() {
        Set<Module> managedModules = new HashSet<Module>();

        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();

        for (Module module : ModuleSorter.sort(modules)) {
            if (!excluded(module))
                managedModules.add(module);
        }

        return managedModules;
    }



    private boolean excluded(Module module) {
        return excluded(module.getName());
    }



    private boolean excluded(String moduleName) {
        for (String patternToExclude : patternsToExclude) {
            if (moduleName.contains(patternToExclude)) {
                return true;
            }
        }

        return false;
    }



    public void generate() {
        LOG.info("Starting to generate component diagram: " + title);
        plantUmlWriter.startDiagram(title);

        ModulesHierarchyGenerator modulesHierarchyGenerator = new ModulesHierarchyGenerator(managedModules);
        modulesHierarchyGenerator.generate(plantUmlWriter);

        List<Module> modules = getListOfManagedModulesOrderedAlphabetically();
        for (Module module : ModuleSorter.sort(modules)) {
            printModuleDependencies(module);
        }

        plantUmlWriter.endDiagram();
        LOG.info("Ending to generate component diagram: " + title);
    }



    private List<Module> getListOfManagedModulesOrderedAlphabetically() {
        List<Module> modules = new ArrayList<Module>(managedModules);
        Collections.sort(modules, new ModuleComparator());

        return modules;
    }



    private void printModuleDependencies(Module module) {
        String moduleName = module.getName();

        LOG.info("Adding module dependencies: " + moduleName);

        String[] namesOfDependentModules = getNamesOfDependentModules(module);
        for (String dependentModulesName : ModuleSorter.sort(namesOfDependentModules)) {
            if (!excluded(dependentModulesName)) {
                plantUmlWriter.addComponentDependency(moduleName, dependentModulesName);
            }
        }
    }



    @NotNull
    private String[] getNamesOfDependentModules(Module module) {
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        return moduleRootManager.getDependencyModuleNames();
    }



    private class ModuleComparator implements Comparator<Module> {

        @Override
        public int compare(Module module1, Module module2) {
            String name1 = module1.getName();
            String name2 = module2.getName();

            return name1.compareTo(name2) ;
        }

    }

}
