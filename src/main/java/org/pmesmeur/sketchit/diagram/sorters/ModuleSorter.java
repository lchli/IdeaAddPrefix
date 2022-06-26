package org.pmesmeur.sketchit.diagram.sorters;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class ModuleSorter {

    static public List<Module> sort(Set<Module> modules) {
        return sortModuleCollection(modules);
    }



    @NotNull
    private static List<Module> sortModuleCollection(Collection<Module> modules) {
        List<Module> moduleList = new ArrayList<Module>(modules);
        Collections.sort(moduleList, new ModuleComparator());

        return moduleList;
    }



    public static List<Module> sort(List<Module> modules) {
        return sortModuleCollection(modules);
    }



    static public List<Module> sort(Module[] modules) {
        List<Module> moduleList = Arrays.asList(modules);
        Collections.sort(moduleList, new ModuleComparator());

        return moduleList;
    }



    private static class ModuleComparator implements Comparator<Module> {

        @Override
        public int compare(Module module1, Module module2) {
            String name1 = module1.getName();
            String name2 = module2.getName();

            return name1.compareTo(name2) ;
        }

    }



    static public List<String> sort(String [] modules) {
        List<String> moduleList = Arrays.asList(modules);
        Collections.sort(moduleList);

        return moduleList;
    }

}
