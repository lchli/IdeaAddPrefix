package org.pmesmeur.sketchit.diagram.clazz;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.VisibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.pmesmeur.sketchit.PlugUtil;
import org.pmesmeur.sketchit.diagram.plantuml.PlantUmlWriter;
import org.pmesmeur.sketchit.diagram.sorters.ClassSorter;
import org.pmesmeur.sketchit.diagram.sorters.FieldSorter;
import org.pmesmeur.sketchit.diagram.sorters.JavaCodeReferenceElementSorter;
import org.pmesmeur.sketchit.diagram.sorters.MethodSorter;

import java.util.*;


public class ClassDiagramGenerator {
    private static final Logger LOG = Logger.getInstance(ClassDiagramGenerator.class);

    private final PlantUmlWriter plantUmlWriter;
    private final Project project;
    private final Module module;
    private final Set<PsiClass> managedClasses;
    private final List<String> packages;
    private final VirtualFile sourceDirectory;
    private final String title;
    private final boolean hideMethods;
    private final boolean hideAttributes;
    private final boolean hideInnerClasses;


    public static Builder newBuilder(PlantUmlWriter plantUmlWriter,
                                     Project project,
                                     Module module) {
        return new Builder(plantUmlWriter, project, module);
    }


    public static class Builder {
        private final PlantUmlWriter plantUmlWriter;
        private final Project project;
        private final Module module;
        private VirtualFile sourceDirectory;
        private String title;
        private boolean hideMethods = false;
        private boolean hideAttributes = false;
        private boolean hideInnerClasses = false;
        private PsiClass[] targetClasses;


        public Builder(PlantUmlWriter plantUmlWriter, Project project, Module module) {
            this.plantUmlWriter = plantUmlWriter;
            this.project = project;
            this.module = module;
        }


        public Builder sourceDirectory(VirtualFile sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
            return this;
        }


        public Builder title(String title) {
            this.title = title;
            return this;
        }


        public Builder hideMethods(boolean hideMethods) {
            this.hideMethods = hideMethods;
            return this;
        }


        public Builder hideAttributes(boolean hideAttributes) {
            this.hideAttributes = hideAttributes;
            return this;
        }


        public Builder hideInnerClasses(boolean hideInnerClasses) {
            this.hideInnerClasses = hideInnerClasses;
            return this;
        }

        public Builder setTargetClasses(PsiClass[] targetClasses) {
            this.targetClasses = targetClasses;
            return this;
        }

        public ClassDiagramGenerator build() {
            return new ClassDiagramGenerator(this);
        }
    }


    private ClassDiagramGenerator(Builder builder) {
        this.plantUmlWriter = builder.plantUmlWriter;
        this.project = builder.project;
        this.module = builder.module;
        this.sourceDirectory = builder.sourceDirectory;
        this.title = builder.title;
        this.hideMethods = builder.hideMethods;
        this.hideAttributes = builder.hideAttributes;
        this.hideInnerClasses = builder.hideInnerClasses;
        this.packages = new ArrayList<String>();
        this.managedClasses = createListOfClassesToManage(builder.targetClasses);
    }


    private Set<PsiClass> createListOfClassesToManage(PsiClass[] targetClasses) {
        Finder finder = new Finder(project, module,targetClasses);

        Set<PsiClass> classes = finder.getClasses();
        if (classes.isEmpty()) {
            throw new NoSuchElementException("No classes found");
        }

        Set<String> packageSet = finder.getPackages();
        for (String pkg : packageSet) {
            packages.add(pkg);
        }
        Collections.sort(packages, new StringLengthComparator());

        return filterClasses(classes);
    }


    private Set<PsiClass> filterClasses(Set<PsiClass> classes) {
        Set<PsiClass> newSet = new HashSet<PsiClass>();

        for (PsiClass clazz : classes) {
            if (clazz.getQualifiedName() == null) {
                continue;
            }

            PsiElement parentElement = clazz.getParent();
            PsiElement owningDirectory = parentElement.getParent();
            if (sourceDirectory == null ||
                    (owningDirectory instanceof PsiDirectory &&
                            ((PsiDirectory) owningDirectory).getVirtualFile().equals(sourceDirectory))) {
                newSet.add(clazz);
            }
        }

        return newSet;
    }


    public void generate() {
        LOG.info("Starting to generate class diagram: " + title);
        plantUmlWriter.startDiagram(title);

        List<PsiClass> classes = ClassSorter.sort(managedClasses);
        for (PsiClass clazz : classes) {
            LOG.info("* generating class: " + clazz.getQualifiedName());
            new ClassGenerator(clazz).generate();
        }


        for (PsiClass clazz : classes) {
            LOG.info("* generating relationships for class: " + clazz.getQualifiedName());
            new RelationshipsGenerator(clazz).generate();
        }

        plantUmlWriter.endDiagram();
        LOG.info("Ending to generate component diagram: " + title);
    }


    private static class StringLengthComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            return o1.length() - o2.length();
        }

    }


    enum FieldDisplayType {
        NONE,
        ATTRIBUTE,
        AGGREGATION
    }


    private class BaseGenerator {
        final PsiClass clazz;


        private BaseGenerator(PsiClass clazz) {
            this.clazz = clazz;
        }


        FieldDisplayType getFieldDisplayType(PsiClass clazz, PsiField field) {
            if (isInheritedMember(field, clazz)) {
                return FieldDisplayType.NONE;
            }

            if (typeBelongsToCurrentProject(field.getType()) &&
                    !typeContainsGeneric(field) &&
                    !field.hasModifierProperty(PsiModifier.STATIC)) {
                return FieldDisplayType.AGGREGATION;
            }

            return FieldDisplayType.ATTRIBUTE;
        }


        boolean isInheritedMember(PsiMember member, PsiClass clazz) {
            return !member.getContainingClass().equals(clazz);
        }


        private boolean typeBelongsToCurrentProject(PsiType type) {
            PsiClass typeClass = PsiTypesUtil.getPsiClass(type);
            if (typeClass == null) {
                return false;
            }


            return classBelongsToProject(typeClass);
        }


        private boolean classBelongsToProject(PsiClass clazz) {
            PsiFile classFile = clazz.getContainingFile();
            if (classFile == null) {
                return false;
            }

            return isBinaryFile(classFile);
        }


        private boolean isBinaryFile(PsiFile containingFile) {
            return containingFile.getFileType().isBinary() == false;
        }


        private boolean typeContainsGeneric(PsiField field) {
            String presentableText = field.getType().getPresentableText();
            return presentableText.contains("<") || presentableText.contains(">");
        }

    }


    private class ClassGenerator extends BaseGenerator {

        ClassGenerator(PsiClass clazz) {
            super(clazz);
        }


        void generate() {
            try {
                String packageName = getPackageName(clazz);
                List<String> packageStack = computePackageStack(packageName);

                generateClassIntoPackage(packageStack);

                if (!hideInnerClasses) {
                    generateInnerClasses();
                }
            } catch (Throwable e) {
                e.printStackTrace();
                PlugUtil.showMsg(e, project);
            }
        }


        private List<String> computePackageStack(String packageName) {
           // LOG.info("packageName:" + packageName);
           // PlugUtil.showMsg("packageName:" + packageName, project);

            List<String> packageStack = new ArrayList<String>();

            for (String pkg : packages) {
                if (packageName.startsWith(pkg)) {
                    packageStack.add(pkg);
                }
            }

            return packageStack;
        }


        private void generateClassIntoPackage(List<String> packageStack) {
            if (clazz.isEnum()) {
                plantUmlWriter.startEnumDeclaration(packageStack, clazz.getName());
                generateEnumValues();
            } else {
                String qualifiedName = clazz.getQualifiedName();

                if (clazz.isInterface()) {
                    plantUmlWriter.startInterfaceDeclaration(packageStack, qualifiedName);
                } else if (clazz.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    plantUmlWriter.startAbstractClassDeclaration(packageStack, qualifiedName);
                } else {
                    plantUmlWriter.startClassDeclaration(packageStack, qualifiedName);
                }

                generateClassMembers();
            }

            plantUmlWriter.endClassDeclaration(packageStack);
        }


        private void generateEnumValues() {
            PsiField[] allFields = clazz.getAllFields();

            for (PsiField enumValue : FieldSorter.sort(allFields)) {
                if (!isInheritedMember(enumValue, clazz)) {
                    generateEnumValue(enumValue);
                }
            }
        }


        private void generateEnumValue(PsiField enumValue) {
            if (!hideAttributes) {
                plantUmlWriter.declareEnumValue(enumValue.getName());
            }
        }


        private void generateClassMembers() {
            if (!hideAttributes) {
                generateClassAttributes();
            }

            if (!hideMethods) {
                generateClassMethods();
            }
        }


        private void generateClassAttributes() {
            PsiField[] allFields = clazz.getAllFields();

            for (PsiField field : FieldSorter.sort(allFields)) {
                FieldDisplayType type = getFieldDisplayType(clazz, field);
                if (type == FieldDisplayType.ATTRIBUTE || type == FieldDisplayType.AGGREGATION
                ) {
                    generateClassField(field);
                }
            }
        }


        private void generateClassField(PsiField field) {
            String visibility = getVisibility(field.getModifierList());
            String fieldType = field.getType().getPresentableText();

            if (!field.hasModifierProperty(PsiModifier.STATIC)) {
                plantUmlWriter.declareField(visibility, fieldType, field.getName());
            } else {
                plantUmlWriter.declareStaticField(visibility, fieldType, field.getName());
            }
        }


        @NotNull
        private String getVisibility(PsiModifierList methodModifiers) {
            return VisibilityUtil.getVisibilityModifier(methodModifiers);
        }


        private void generateClassMethods() {
            PsiMethod[] allMethods = clazz.getAllMethods();

            for (PsiMethod method : MethodSorter.sort(allMethods)) {
                if (!isInheritedMember(method, clazz)) {
                    generateClassMethod(method);
                }
            }
        }


        private void generateClassMethod(PsiMethod method) {
            String visibility = getVisibility(method.getModifierList());

            if (method.hasModifierProperty(PsiModifier.ABSTRACT)) {
                plantUmlWriter.declareAbstractMethod(visibility, method.getName());
            } else if (method.hasModifierProperty(PsiModifier.STATIC)) {
                plantUmlWriter.declareStaticMethod(visibility, method.getName());
            } else {
                plantUmlWriter.declareMethod(visibility, method.getName());
            }
        }


        private void generateInnerClasses() {
            PsiClass[] allInnerClasses = clazz.getAllInnerClasses();

            for (PsiClass innerClass : ClassSorter.sort(allInnerClasses)) {
                generateInnerClass(innerClass);
            }
        }


        private void generateInnerClass(PsiClass innerClass) {
            if (innerClass != clazz) {
                if (innerClass.getParent() == clazz) {
                    new ClassGenerator(innerClass).generate();
                }
            }
        }

    }


    private String getPackageName(PsiClass clazz) {
        String qualifiedName = clazz.getQualifiedName();
        return ClassUtil.extractPackageName(qualifiedName);
    }


    private class RelationshipsGenerator extends BaseGenerator {


        RelationshipsGenerator(PsiClass clazz) {
            super(clazz);
        }


        void generate() {
            generateInterfaceRealization();
            generateClassInheritence();
            generateClassAssociations();

            if (!hideInnerClasses) {
                generateInnerClassesAssociations();
            }
        }


        private void generateInterfaceRealization() {
            PsiReferenceList implementsList = clazz.getImplementsList();
            PsiJavaCodeReferenceElement[] referenceElements = implementsList.getReferenceElements();

            for (PsiJavaCodeReferenceElement referenceElement : JavaCodeReferenceElementSorter.sort(referenceElements)) {
                LOG.info("  - generating implementation interface " + referenceElement.getQualifiedName() + " of class " + clazz.getQualifiedName());
                plantUmlWriter.addInterfaceRealization(clazz.getQualifiedName(), referenceElement.getQualifiedName());
            }
        }


        private void generateClassInheritence() {
            PsiClass superClass = clazz.getSuperClass();
            if (superClass != null && !classIsFromJavaLangPackage(superClass)) {
                LOG.info("  - generating inheritance of class " + superClass.getQualifiedName() + " for class " + clazz.getQualifiedName());
                plantUmlWriter.addClassesInheritence(clazz.getQualifiedName(), superClass.getQualifiedName());
            }
        }


        private boolean classIsFromJavaLangPackage(PsiClass clazz) {
            String classPackage = getPackageName(clazz);
            return classPackage.equals("java.lang");
        }


        private void generateClassAssociations() {
            PsiField[] allFields = clazz.getAllFields();

            for (PsiField field : FieldSorter.sort(allFields)) {
                if (getFieldDisplayType(clazz, field) == FieldDisplayType.AGGREGATION) {
                    LOG.info("  - generating association from class " + clazz.getQualifiedName() + " to class " + field.getType().getCanonicalText());
                    plantUmlWriter.addClassesAssociation(clazz.getQualifiedName(),
                            field.getType().getCanonicalText(),
                            field.getName());
                }
            }

            //todo
            for (PsiField field : FieldSorter.sort(allFields)) {
                if (getFieldDisplayType(clazz, field) == FieldDisplayType.AGGREGATION) {
                    PsiClass typeClass = PsiTypesUtil.getPsiClass(field.getType());

                    PlugUtil.showMsg("typeClass:" + typeClass, project);
                    if (typeClass != null) {
                        PlugUtil.showMsg("typeClass:" + typeClass.getQualifiedName(), project);
                        new ClassGenerator(typeClass).generate();
                    }
                }
            }

        }


        private void generateInnerClassesAssociations() {
            PsiClass[] innerClasses = clazz.getInnerClasses();
            List<PsiClass> sortedInnerClasses = ClassSorter.sort(innerClasses);

            for (PsiClass innerClass : sortedInnerClasses) {
                if (innerClass.getParent() == clazz) {
                    LOG.info("  - generating association from class " + clazz.getQualifiedName() + " to inner class " + innerClass.getQualifiedName());
                    plantUmlWriter.addInnerClassesAssociation(clazz.getQualifiedName(), innerClass.getQualifiedName());
                }
            }

            for (PsiClass innerClass : sortedInnerClasses) {
                LOG.info("  - generating relationships for inner class " + innerClass.getQualifiedName() + " of class " + clazz.getQualifiedName());
                generateInnerClassesRelationships(innerClass);
            }
        }


        private void generateInnerClassesRelationships(PsiClass innerClass) {
            if (innerClass != clazz) {
                new RelationshipsGenerator(innerClass).generate();
            }
        }

    }
}
