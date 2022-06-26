package org.pmesmeur.sketchit.diagram.sorters;

import com.intellij.psi.PsiModifierList;
import com.intellij.util.VisibilityUtil;


public class ModifierListComparator {

    public static int compare(PsiModifierList modifierList1, PsiModifierList modifierList2) {
        String visibilityModifier1 = VisibilityUtil.getVisibilityModifier(modifierList1);
        String visibilityModifier2 = VisibilityUtil.getVisibilityModifier(modifierList2);

        int visibilityRank1 = computeVisibilityRank(visibilityModifier1);
        int visibilityRank2 = computeVisibilityRank(visibilityModifier2);

        return visibilityRank1 - visibilityRank2;
    }



    private static int computeVisibilityRank(String visibilityModifier1) {
        if ("public".equals(visibilityModifier1)) {
            return 1;
        } else if ("protected".equals(visibilityModifier1)) {
            return 2;
        } else if ("packageLocal".equals(visibilityModifier1)) {
            return 3;
        } else if ("private".equals(visibilityModifier1)) {
            return 4;
        }

        return 5;
    }

}
