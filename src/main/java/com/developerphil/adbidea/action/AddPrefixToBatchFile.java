package com.developerphil.adbidea.action;

import com.intellij.find.FindBundle;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.actions.FindInPathAction;
import com.intellij.find.actions.ReplaceInPathAction;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.ide.DataManager;
import com.intellij.json.psi.impl.JsonFileImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.impl.file.PsiBinaryFileImpl;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.ui.content.Content;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewContentManager;
import com.intellij.usages.*;
import com.intellij.usages.impl.UsageViewImpl;
import com.intellij.usages.rules.UsageInFile;
import com.intellij.util.AdapterProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;


/**
 * Created by lichenghang .
 */
public class AddPrefixToBatchFile extends AnAction {

    private Project project;
    private DataContext context;
    private AnActionEvent mAnActionEvent;
    private final ConditionPredicate defFilter = new ConditionPredicate() {
        @Override
        public boolean isMatch(PsiElement element) {
            return true;
        }
    };


    @Override
    public void actionPerformed(AnActionEvent e) {
        context = e.getDataContext();
        mAnActionEvent = e;
        project = e.getProject();
        if (project == null) {
            throw new NullPointerException("project is null.");
        }

        Thread.UncaughtExceptionHandler oldErrorHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                PlugUtil.showMsg(getStack(e), project);
                oldErrorHandler.uncaughtException(t, new Exception(e.getMessage()));
            }
        });


        RefactoringDialog dialog = new RefactoringDialog(project, false);
        dialog.setDoRenameListener(new RefactoringDialog.DoRenameListener() {
            @Override
            public void doRenameActivityFragment(String prefix) {
                renameFragmentAndActivity(prefix);
            }

            @Override
            public void doRenameBinding(String prefix) {
                renameXmlBinding(prefix);
            }

            @Override
            public void doRenameClass(String prefix) {
                renameClass(prefix,null);
            }

            @Override
            public void doRename(String prefix) {
                renameResFile(prefix);
            }

            @Override
            public void doRenameContent(String prefix) {
                renameXmlContent(prefix);
            }
        });

        dialog.show();

    }


    @Nullable
    private PsiElement findSelectedPsiElement() {
//        PsiElement[] psiElements = LangDataKeys.PSI_ELEMENT_ARRAY.getData(context);
//        if (psiElements == null || psiElements.length == 0) {
//            PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(context);
//            if (element != null) {
//                psiElements = new PsiElement[]{element};
//            }
//        }
//
        PsiElement[] psiElements = BaseRefactoringAction.getPsiElementArray(context);
        if (psiElements == null || psiElements.length <= 0) {
            return null;
        }

        return psiElements[0];

    }

    private interface ConditionPredicate {
        boolean isMatch(PsiElement element);
    }

    private void findMatchedChildrenInDirOnly(PsiElement startRoot, List<PsiElement> result, ConditionPredicate condition) {
        if (startRoot == null || result == null) {
            return;
        }

        if (condition.isMatch(startRoot)) {
            result.add(startRoot);
        }

        PsiElement[] children = startRoot.getChildren();
        if (children == null || children.length <= 0) {
            PlugUtil.showMsg("children is null:" + startRoot, project);
            return;
        }
        for (PsiElement e : children) {
            if (e instanceof PsiDirectory) {
                findMatchedChildrenInDirOnly(e, result, condition);
            } else {
                if (condition.isMatch(e)) {
                    result.add(e);
                }
            }
        }
    }


    private void findJavaKotlin(PsiElement startRoot, Map<String, PsiElement> result, ConditionPredicate condition) {
        if (startRoot == null || result == null) {
            return;
        }

        if (condition.isMatch(startRoot)) {
            String name = getOldName(startRoot);
            if (name != null) {
                result.put(name, startRoot);
            }
        }

        PsiElement[] children = startRoot.getChildren();
        if (children == null || children.length <= 0) {
            PlugUtil.showMsg("children is null:" + startRoot, project);
            return;
        }
        for (PsiElement e : children) {
            if (e instanceof PsiDirectory || isDir(e) ||
                    isJavaClass(e) || isJavaFile(e) ||
                    isKtClass(e) || isKtFile(e)) {
                findJavaKotlin(e, result, condition);
            } else {
                if (condition.isMatch(e)) {
                    String name = getOldName(e);
                    if (name != null) {
                        result.put(name, e);
                    }
                }
            }
        }
    }


    public final void renameXmlContent(String prefix) {

        @Nullable PsiElement startRoot = findSelectedPsiElement();
        PlugUtil.showMsg("startRoot:" + startRoot, project);

        if (startRoot == null) {
            showStartRootNullMsg();
            return;
        }
        List<PsiElement> result = new ArrayList<>();

        findMatchedChildrenInDirOnly(startRoot, result, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl")) {

                    return true;
                }

                return false;
            }
        });

        for (PsiElement xmlFile : result) {
            renameContent(xmlFile, prefix);
        }

    }

    public final void renameXmlBinding(String xmlPrefix) {

        @Nullable PsiElement startRoot = findSelectedPsiElement();
        PlugUtil.showMsg("startRoot:" + startRoot, project);

        if (startRoot == null) {
            showStartRootNullMsg();
            return;
        }
        List<PsiElement> result = new ArrayList<>();

        findMatchedChildrenInDirOnly(startRoot, result, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl") &&
                        isLayoutBindingFile(element)) {

                    return true;
                }

                return false;
            }
        });

        for (PsiElement xmlFile : result) {
            String oldName = getOldName(xmlFile);
            PlugUtil.showMsg("binding xmlFile:" + oldName, project);
            if (oldName == null) {
                continue;
            }
            if (!oldName.startsWith(xmlPrefix)) {
                continue;
            }
            oldName = oldName.substring(xmlPrefix.length());
            String[] arr = oldName.split("_");
            if (arr == null || arr.length <= 0) {
                continue;
            }
            String bindingName = "";
            for (String part : arr) {
                bindingName += upperFistChar(part);
            }
            bindingName += "Binding";
            PlugUtil.showMsg("binding name:" + bindingName, project);

            String[] prefixarr = xmlPrefix.split("_");
            if (prefixarr == null || prefixarr.length <= 0) {
                continue;
            }
            String prefix=upperFistChar(prefixarr[0]);

            replaceStringUse(bindingName,prefix+bindingName);
        }



    }

    private void replaceStringUse(String beReplace,String newVal){
        FindManager findManager = FindManager.getInstance(this.project);

        FindModel findModel;
        findModel = findManager.getFindInProjectModel().clone();
        findModel.setReplaceState(true);
        findModel.setProjectScope(true);
        findModel.setStringToFind(beReplace);
        findModel.setStringToReplace(newVal);

        replaceInPath(findModel);
    }

    public void replaceInPath(@NotNull FindModel findModel) {
        FindManager findManager = FindManager.getInstance(project);
        if (findModel.isProjectScope() || FindInProjectUtil.getDirectory(findModel) != null || findModel.getModuleName() != null || findModel.getCustomScope() != null) {
            UsageViewManager manager = UsageViewManager.getInstance(this.project);
            if (manager != null) {
                findManager.getFindInProjectModel().copyFrom(findModel);
                FindModel findModelCopy = findModel.clone();
                UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(findModelCopy);
                FindUsagesProcessPresentation processPresentation = FindInProjectUtil.setupProcessPresentation(this.project, true, presentation);
                processPresentation.setShowFindOptionsPrompt(findModel.isPromptOnReplace());
                UsageSearcherFactory factory = new UsageSearcherFactory(findModelCopy, processPresentation);
                this.searchAndShowUsages(manager, factory, findModelCopy, presentation, processPresentation);
            }
        }
    }

    public void searchAndShowUsages(@NotNull final UsageViewManager manager, @NotNull final Factory<UsageSearcher> usageSearcherFactory, @NotNull final FindModel findModelCopy, @NotNull final UsageViewPresentation presentation, @NotNull final FindUsagesProcessPresentation processPresentation) {
        presentation.setMergeDupLinesAvailable(false);
        ReplaceInProjectTarget target = new ReplaceInProjectTarget(this.project, findModelCopy);
        ((FindManagerImpl)FindManager.getInstance(this.project)).getFindUsagesManager().addToHistory(target);
        final ReplaceContext[] context = new ReplaceContext[1];
        manager.searchAndShowUsages(new UsageTarget[]{target}, usageSearcherFactory, processPresentation, presentation, new UsageViewManager.UsageViewStateListener() {
            public void usageViewCreated(@NotNull UsageView usageView) {
                context[0] = new ReplaceContext(usageView, findModelCopy);
                findingUsagesFinished(usageView);
            }

            public void findingUsagesFinished(UsageView usageView) {
                if (context[0] != null) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        replaceUsagesUnderCommand(context[0], usageView.getUsages());
                        context[0].invalidateExcludedSetCache();
                    }, project.getDisposed());
                }

            }
        });
    }

    private void replaceUsagesUnderCommand(@NotNull ReplaceContext replaceContext, @NotNull Set<? extends Usage> usagesSet) {
        if (!usagesSet.isEmpty()) {
            List<Usage> usages = new ArrayList(usagesSet);
            Collections.sort(usages, UsageViewImpl.USAGE_COMPARATOR);
           // if (this.ensureUsagesWritable(replaceContext, usages)) {
                CommandProcessor.getInstance().executeCommand(this.project, () -> {
                    boolean success = this.replaceUsages(replaceContext, usages);
                    UsageView usageView = replaceContext.getUsageView();

                }, FindBundle.message("find.replace.command", new Object[0]), (Object)null);

                replaceContext.invalidateExcludedSetCache();
           // }
        }
    }

    private boolean replaceUsages(@NotNull ReplaceContext replaceContext, @NotNull Collection<? extends Usage> usages) {

        int[] replacedCount = {0};
        boolean[] success = {true};
        boolean result = ((ApplicationImpl)ApplicationManager.getApplication()).runWriteActionWithCancellableProgressInDispatchThread(
                "msg",
                project,
                null,
                indicator -> {
                    indicator.setIndeterminate(false);
                    int processed = 0;
                    VirtualFile lastFile = null;

                    for (final Usage usage : usages) {
                        ++processed;
                        indicator.checkCanceled();
                        indicator.setFraction((float)processed / usages.size());

                        if (usage instanceof UsageInFile) {
                            VirtualFile virtualFile = ((UsageInFile)usage).getFile();
                            if (virtualFile != null && !virtualFile.equals(lastFile)) {
                                indicator.setText2(virtualFile.getPresentableUrl());
                                lastFile = virtualFile;
                            }
                        }

                        ProgressManager.getInstance().executeNonCancelableSection(() -> {
                            try {
                                if ( ReplaceInProjectManager.getInstance(project).replaceUsage(usage, replaceContext.getFindModel(), replaceContext.getExcludedSetCached(), false)) {
                                    replacedCount[0]++;
                                }
                            }
                            catch (FindManager.MalformedReplacementStringException ex) {
                                markAsMalformedReplacement(replaceContext, usage);
                                success[0] = false;
                            }
                        });
                    }
                    FileDocumentManager.getInstance().saveAllDocuments();
                }
        );
        success[0] &= result;
        replaceContext.getUsageView().removeUsagesBulk(usages);
        //reportNumberReplacedOccurrences(myProject, replacedCount[0]);
        return success[0];
    }
    private static void markAsMalformedReplacement(ReplaceContext replaceContext, Usage usage) {
        replaceContext.getUsageView().excludeUsages(new Usage[]{usage});
    }

    private static class ReplaceInProjectTarget extends FindInProjectUtil.StringUsageTarget {
        ReplaceInProjectTarget(@NotNull Project project, @NotNull FindModel findModel) {
            super(project, findModel);
        }

        @NotNull
        public String getLongDescriptiveName() {
            UsageViewPresentation presentation = FindInProjectUtil.setupViewPresentation(this.myFindModel);
            return "Replace " + StringUtil.decapitalize(presentation.getToolwindowTitle()) + " with '" + this.myFindModel.getStringToReplace() + "'";
        }

        public KeyboardShortcut getShortcut() {
            return ActionManager.getInstance().getKeyboardShortcut("ReplaceInPath");
        }

        public void showSettings() {
            Content selectedContent = UsageViewContentManager.getInstance(this.myProject).getSelectedContent(true);
            JComponent component = selectedContent == null ? null : selectedContent.getComponent();
            ReplaceInProjectManager findInProjectManager = ReplaceInProjectManager.getInstance(this.myProject);
            findInProjectManager.replaceInProject(DataManager.getInstance().getDataContext(component), this.myFindModel);
        }
    }

    static class ReplaceContext {
        private final UsageView usageView;
        private final FindModel findModel;
        private Set<Usage> excludedSet;

        ReplaceContext(@NotNull UsageView usageView, @NotNull FindModel findModel) {
            this.usageView = usageView;
            this.findModel = findModel;
        }

        @NotNull
        public FindModel getFindModel() {
            return this.findModel;
        }

        @NotNull
        public UsageView getUsageView() {
            return this.usageView;
        }

        @NotNull
        Set<Usage> getExcludedSetCached() {
            if (this.excludedSet == null) {
                this.excludedSet = this.usageView.getExcludedUsages();
            }

            return this.excludedSet;
        }

        void invalidateExcludedSetCache() {
            this.excludedSet = null;
        }
    }
    private String upperFistChar(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private boolean isLayoutBindingFile(PsiElement file) {
        XmlDocument doc = (XmlDocument) file.getFirstChild();
        if (doc == null) {
            return false;
        }
        XmlTag root = doc.getRootTag();
        String rootName = root != null ? root.getName() : null;
        PlugUtil.showMsg("root tag:" + rootName, project);

        if (root == null || !root.getName().equals("layout")) {//do not handle other layout.
            return false;
        }

        return true;

    }

    private class UsageSearcherFactory implements Factory<UsageSearcher> {
        private final FindModel myFindModelCopy;
        private final FindUsagesProcessPresentation myProcessPresentation;

        private UsageSearcherFactory(@NotNull FindModel findModelCopy, @NotNull FindUsagesProcessPresentation processPresentation) {
            this.myFindModelCopy = findModelCopy;
            this.myProcessPresentation = processPresentation;
        }

        public UsageSearcher create() {
            return (processor) -> {
                try {
                   // ReplaceInProjectManager.this.myIsFindInProgress = true;
                    FindInProjectUtil.findUsages(this.myFindModelCopy, project, new AdapterProcessor(processor, UsageInfo2UsageAdapter.CONVERTER), this.myProcessPresentation);
                } finally {
                   // ReplaceInProjectManager.this.myIsFindInProgress = false;
                }

            };
        }
    }

    private void renameContent(PsiElement xmlFile, String prefix) {
        List<PsiElement> result = getResNameFromLayout(xmlFile);
        if (result == null || result.isEmpty()) {
            PlugUtil.showMsg("skip xml:" + xmlFile, project);
            return;
        }

        for (PsiElement p : result) {
            //PlugUtil.showMsg(p.toString() + "@" + p.getClass().getName(), project);
            dor(p, prefix);
        }
    }

    private void showStartRootNullMsg() {
        PlugUtil.showMsg("请选择要重构的目录", project, true);

    }

    public final void renameResFile(String prefix) {

        @Nullable PsiElement startRoot = findSelectedPsiElement();
        PlugUtil.showMsg("startRoot:" + startRoot, project);
        if (startRoot == null) {
            showStartRootNullMsg();
            return;
        }
        List<PsiElement> result = new ArrayList<>();

        findMatchedChildrenInDirOnly(startRoot, result, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl") ||
                        element.getClass().getName().equals("com.intellij.psi.impl.file.PsiBinaryFileImpl") ||
                        element.getClass().getName().equals("com.intellij.json.psi.impl.JsonFileImpl")) {

                    return true;
                }

                return false;
            }
        });

        for (PsiElement p : result) {
            dor(p, prefix);
        }

    }


    public void renameFragmentAndActivity(String prefix) {
        renameClass(prefix, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                String oldName = getOldName(element);
                PlugUtil.showMsg("aQualifiedName:" + oldName, project);
                if (oldName == null) {
                    return false;
                }

                return oldName.contains("Fragment") || oldName.contains("Activity");
            }
        });
    }

    public void renameClass(String prefix, ConditionPredicate extraFilter) {
        @Nullable PsiElement startRoot = findSelectedPsiElement();
        PlugUtil.showMsg("startRoot:" + startRoot, project);

        if (startRoot == null) {
            showStartRootNullMsg();
            return;
        }

        if (extraFilter == null) {
            extraFilter = defFilter;
        }

        ConditionPredicate filterCopy = extraFilter;

        Map<String, PsiElement> result = new HashMap<>();
        findJavaKotlin(startRoot, result, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                PlugUtil.showMsg("element:" + element + "@" + element.getClass().getName(), project);

                return (isKtClass(element) || isJavaClass(element) || isKtFile(element)) && filterCopy.isMatch(element);
            }
        });

        for (PsiElement p : result.values()) {
            dor(p, prefix);
        }


    }

    private boolean isDir(PsiElement element) {
        return element.getClass().getName().equals("com.intellij.psi.impl.file.PsiJavaDirectoryImpl");
    }

    private boolean isKtFile(PsiElement element) {
        return element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtFile");
    }

    private boolean isKtClass(PsiElement element) {
        return element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtClass");
    }

    private boolean isJavaFile(PsiElement element) {
        return element.getClass().getName().equals("com.intellij.psi.impl.source.PsiJavaFileImpl");
    }

    private boolean isJavaClass(PsiElement element) {
        return element.getClass().getName().equals("com.intellij.psi.impl.source.PsiClassImpl");
    }

    private Class findCls(String classname) {
        try {
            return Class.forName(classname);
        } catch (Throwable e) {
            PlugUtil.showMsg(getStack(e), project);
            return null;
        }
    }


    private String getOldName(PsiElement element) {
        String oldName = null;

        if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiClassImpl")) {
            oldName = getPsiClassImplName(element, element.getClass());
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiJavaFileImpl")) {
            oldName = getPsiClassImplName(element, findCls("com.intellij.psi.impl.source.PsiFileImpl"));
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtFile")) {
            oldName = ((org.jetbrains.kotlin.psi.KtFile) element).getName();
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtClass")) {
            oldName = ((org.jetbrains.kotlin.psi.KtClass) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl")) {
            oldName = ((XmlFileImpl) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.file.PsiBinaryFileImpl")) {
            oldName = ((PsiBinaryFileImpl) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.json.psi.impl.JsonFileImpl")) {
            oldName = ((JsonFileImpl) element).getName();
        } else if (element instanceof XmlAttributeValue) {
            oldName = ((XmlAttributeValue) element).getValue();
        }

        if (oldName == null) {
            return null;
        }

        int index = oldName.indexOf(".");
        if (index >= 0) {
            oldName = oldName.substring(0, index);
        }
        // PlugUtil.showMsg("oldName:" + oldName, project);

        return oldName;
    }

    private void dor(PsiElement element, String prefix) {
        String oldName = null;
        boolean isSearchTextOccurrences = false;

        if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiClassImpl")) {
            oldName = getPsiClassImplName(element, element.getClass());
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.source.PsiJavaFileImpl")) {
            oldName = getPsiClassImplName(element, findCls("com.intellij.psi.impl.source.PsiFileImpl"));
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtFile")) {
            oldName = ((org.jetbrains.kotlin.psi.KtFile) element).getName();
        } else if (element.getClass().getName().equals("org.jetbrains.kotlin.psi.KtClass")) {
            oldName = ((org.jetbrains.kotlin.psi.KtClass) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.source.xml.XmlFileImpl")) {
            oldName = ((XmlFileImpl) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.psi.impl.file.PsiBinaryFileImpl")) {
            oldName = ((PsiBinaryFileImpl) element).getName();
        } else if (element.getClass().getName().equals("com.intellij.json.psi.impl.JsonFileImpl")) {
            oldName = ((JsonFileImpl) element).getName();
        } else if (element instanceof XmlAttributeValue) {
            oldName = ((XmlAttributeValue) element).getValue();
            isSearchTextOccurrences = true;
        }

        if (oldName == null) {
            return;
        }

        if (!oldName.endsWith(".kt")) {
            int index = oldName.indexOf(".");
            if (index >= 0) {
                oldName = oldName.substring(0, index);
            }
        }
        PlugUtil.showMsg("oldName:" + oldName, project);

        if (oldName.startsWith("AndroidManifest")) {
            return;
        }

        if (oldName.startsWith(prefix)) {
            return;
        }

        PlugUtil.showMsg("run processor:" + oldName, project);

        RenameProcessor processor = new RenameProcessor(project, element, prefix + oldName, GlobalSearchScope.projectScope(project), false, isSearchTextOccurrences) {
            @Override
            protected boolean isPreviewUsages() {
                return false;
            }

            @Override
            protected void previewRefactoring(@NotNull UsageInfo[] usages) {
                super.execute(usages);
            }

        };
        processor.run();
    }

    private String getPsiClassImplName(PsiElement element, Class clazz) {
        try {
            Method getNameMethod = clazz.getDeclaredMethod("getName");
            getNameMethod.setAccessible(true);
            return getNameMethod.invoke(element).toString();

        } catch (Throwable e) {
            e.printStackTrace();
            PlugUtil.showMsg(getStack(e), project);
            return null;
        }
    }

    private String getPsiClassImplQualifiedName(PsiElement element, Class clazz) {
        try {
            Method getNameMethod = clazz.getDeclaredMethod("getPackageName");
            getNameMethod.setAccessible(true);
            return getNameMethod.invoke(element).toString();

        } catch (Throwable e) {
            e.printStackTrace();
            PlugUtil.showMsg(getStack(e), project);
            return null;
        }
    }


    private String getStack(Throwable e) {
        String ret = "";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        ret = sw.toString();
        pw.close();
        return ret;

    }


    private static List<PsiElement> getResNameFromLayout(final PsiElement file) {
        XmlDocument doc = (XmlDocument) file.getFirstChild();
        if (doc == null) {
            return null;
        }
        XmlTag root = doc.getRootTag();
        if (root == null || !root.getName().equals("resources")) {//do not handle other layout.
            return null;
        }

        List<PsiElement> elements = new ArrayList<>();

        file.accept(new XmlRecursiveElementVisitor() {

            @Override
            public void visitElement(final PsiElement element) {
                try {

                    if (!(element instanceof XmlTag)) {
                        return;
                    }

                    XmlTag tag = (XmlTag) element;
                    String name = tag.getName();

                    if (name.equals("declare-styleable") || name.equals("attr")) {
                        return;
                    }

                    // get element ID
                    XmlAttribute nameAttr = tag.getAttribute("name", null);
                    if (nameAttr == null) {
                        return; // missing android:id attribute
                    }
                    String value = nameAttr.getValue();
                    if (value == null) {
                        return; // empty value
                    }
                    XmlTag parent = tag.getParentTag();
                    if (parent != null && parent.getName().equals("style")) {
                        return;
                    }

                    elements.add(nameAttr.getValueElement());
                } finally {
                    super.visitElement(element);
                }

            }
        });

        return elements;
    }

}
