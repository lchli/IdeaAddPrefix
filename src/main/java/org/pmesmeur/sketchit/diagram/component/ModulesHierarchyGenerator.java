package org.pmesmeur.sketchit.diagram.component;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import org.pmesmeur.sketchit.diagram.plantuml.PlantUmlWriter;
import org.pmesmeur.sketchit.diagram.sorters.ModuleSorter;

import java.io.File;
import java.util.*;


public class ModulesHierarchyGenerator {
    private static final Logger LOG = Logger.getInstance(ModulesHierarchyGenerator.class);


    private final List<ModulePath> modulePaths = new ArrayList<ModulePath>();
    private Set<ModulePath> modulePathsDone = new HashSet<ModulePath>();


    public ModulesHierarchyGenerator(Set<Module> managedModules) {
        buildModulePaths(managedModules);
    }



    private void buildModulePaths(Set<Module> managedModules) {
        for (Module module : ModuleSorter.sort(managedModules)) {
            modulePaths.add(new ModulePath(module));
        }
        Collections.sort(modulePaths, new ModulePathComparator());

        if (modulePaths.size() == 0) {
            LOG.warn("no modules found!");
        } else {
            modulePaths.remove(modulePaths.size() - 1); /// remove last element, i.e. project-root
            buildModulePathsDependencies();
        }
    }



    private void buildModulePathsDependencies() {
        for (int i = 0 ; i < modulePaths.size() ; i++) {
            for (int j = i + 1 ; j < modulePaths.size() ; j++) {
                ModulePath containedModulePath = modulePaths.get(i);
                ModulePath containingModulePath = modulePaths.get(j);

                if (containedModulePath.path.contains(containingModulePath.path)) {
                    containingModulePath.subModules.add(containedModulePath);
                    break;
                }
            }
        }
    }



    public void generate(PlantUmlWriter plantUmlWriter) {
        for (int i = modulePaths.size() - 1 ; i >= 0 ; --i) {
            ModulePath modulePath = modulePaths.get(i);

            generate(plantUmlWriter, modulePath);
        }
    }



    private void generate(PlantUmlWriter plantUmlWriter, ModulePath modulePath) {
        if (!modulePathsDone.contains(modulePath)) {
            modulePathsDone.add(modulePath);
            if (modulePath.subModules.size() > 0) {
                plantUmlWriter.startComponentDeclaration(modulePath.module.getName());

                for (ModulePath subModulePath : modulePath.subModules) {
                    plantUmlWriter.addSubComponent(subModulePath.module.getName());
                }

                for (ModulePath subModulePath : modulePath.subModules) {
                    generate(plantUmlWriter, subModulePath);
                }

                plantUmlWriter.endComponentDeclaration();
            }
        }
    }



    private static class ModulePath {
        private final Module module;
        private final String path;
        ArrayList<ModulePath> subModules = new ArrayList<ModulePath>();


        private ModulePath(Module module) {
            this.module = module;
            this.path = getModuleFilePath();
        }


        private String getModuleFilePath() {
            //File file = new File(module.getName());
            File file = new File(module.getModuleFilePath());
            return file.getParent();
        }

    }



    private static class ModulePathComparator implements Comparator<ModulePath> {

        @Override
        public int compare(ModulePath o1, ModulePath o2) {
            return o2.path.length() - o1.path.length();
        }

    }

}
