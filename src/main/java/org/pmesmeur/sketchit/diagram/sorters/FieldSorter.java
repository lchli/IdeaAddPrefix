package org.pmesmeur.sketchit.diagram.sorters;

import com.intellij.psi.PsiField;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class FieldSorter {

    static public List<PsiField> sort(PsiField [] fields) {
        List<PsiField> fieldList = Arrays.asList(fields);
        Collections.sort(fieldList, new PsiFieldComparator());

        return fieldList;

    }



    private static class PsiFieldComparator implements Comparator<PsiField> {

        @Override
        public int compare(PsiField field1, PsiField field2) {
            int result = ModifierListComparator.compare(field1.getModifierList(), field2.getModifierList());

            if (result == 0) {
                String name1 = field1.getName();
                String name2 = field2.getName();

                result = name1.compareTo(name2);
            }

            return result;
        }

    }

}
