package org.pmesmeur.sketchit.diagram.sorters;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;

import java.util.*;


public class ClassSorter {

    static public List<PsiClass> sort(Set<PsiClass> classes) {
        List<PsiClass> classList = new ArrayList<PsiClass>(classes);
        Collections.sort(classList, new PsiClassComparator());

        return classList;
    }



    static public List<PsiClass> sort(PsiClass [] classes) {
        List<PsiClass> classList = Arrays.asList(classes);
        Collections.sort(classList, new PsiClassComparator());

        return classList;
    }



    private static class PsiClassComparator implements Comparator<PsiClass> {

        @Override
        public int compare(PsiClass class1, PsiClass class2) {
            String name1 = class1.getQualifiedName();
            String name2 = class2.getQualifiedName();

            return StringUtil.compare(name1, name2, false);
        }

    }

}
