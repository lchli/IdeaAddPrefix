package org.pmesmeur.sketchit.diagram.sorters;

import com.intellij.psi.PsiJavaCodeReferenceElement;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class JavaCodeReferenceElementSorter {

    static public List<PsiJavaCodeReferenceElement> sort(PsiJavaCodeReferenceElement[] elements) {
        List<PsiJavaCodeReferenceElement> elementList = Arrays.asList(elements);
        Collections.sort(elementList, new PsiJavaCodeReferenceElementComparator());

        return elementList;

    }



    private static class PsiJavaCodeReferenceElementComparator implements Comparator<PsiJavaCodeReferenceElement> {

        @Override
        public int compare(PsiJavaCodeReferenceElement field1, PsiJavaCodeReferenceElement field2) {
            String name1 = field1.getQualifiedName();
            String name2 = field2.getQualifiedName();

            return name1.compareTo(name2) ;
        }

    }

}
