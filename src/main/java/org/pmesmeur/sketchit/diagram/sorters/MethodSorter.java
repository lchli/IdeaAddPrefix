package org.pmesmeur.sketchit.diagram.sorters;

import com.intellij.psi.PsiMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MethodSorter {


    static public List<PsiMethod> sort(PsiMethod [] methods) {
        List<PsiMethod> methodList = Arrays.asList(methods);
        Collections.sort(methodList, new PsiMethodComparator());

        return methodList;

    }



    private static class PsiMethodComparator implements Comparator<PsiMethod> {

        @Override
        public int compare(PsiMethod method1, PsiMethod method2) {
            int result = ModifierListComparator.compare(method1.getModifierList(), method2.getModifierList());

            if (result == 0) {
                String name1 = method1.getName();
                String name2 = method2.getName();

                result = name1.compareTo(name2);
            }

            return result;
        }

    }

}
