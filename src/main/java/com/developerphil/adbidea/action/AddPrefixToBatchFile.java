package com.developerphil.adbidea.action;

import com.android.tools.pixelprobe.TextInfo;
import com.intellij.find.FindBundle;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.find.actions.FindInPathAction;
import com.intellij.find.actions.ReplaceInPathAction;
import com.intellij.find.impl.FindInProjectUtil;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.find.replaceInProject.ReplaceInProjectManager;
import com.intellij.ide.DataManager;
import com.intellij.json.psi.impl.JsonFileImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.impl.ApplicationImpl;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.*;
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
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.com.intellij.psi.PsiClass;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiUtil;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtPsiUtil;

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
            public void doRenameActivityFragment(String prefix, String oldPrefix) {
                renameFragmentAndActivity(prefix, oldPrefix);
            }

            @Override
            public void doRenameBinding(String prefix, String oldPrefix) {
                renameXmlBinding(prefix, oldPrefix);
            }

            @Override
            public void doRenameClass(String prefix, String oldPrefix) {
                renameClass(prefix, null, oldPrefix);
            }

            @Override
            public void doRename(String prefix, String oldPrefix) {
                renameResFile(prefix, oldPrefix);
            }

            @Override
            public void doRenameContent(String prefix, String oldPrefix) {
                renameXmlContent(prefix, oldPrefix);
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


    public final void renameXmlContent(String prefix, String oldPrefix) {

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
            renameContent(xmlFile, prefix, oldPrefix);
        }

    }

    public final void renameXmlBinding(String xmlPrefix, String oldPrefix) {

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

        if (result.isEmpty()) {
            return;
        }

//        for (PsiElement xmlFile :result){
//            doBinding(xmlFile,xmlPrefix);
//        }

        doBinding(result.remove(0), xmlPrefix, new Runnable() {
            @Override
            public void run() {
                final Runnable outer = this;
                ApplicationManager.getApplication().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (!result.isEmpty()) {
                            PlugUtil.showMsg("run===", project);
                            doBinding(result.remove(0), xmlPrefix, outer, oldPrefix);
                        }
                    }
                });

            }
        }, oldPrefix);


    }

    private void doBinding(PsiElement xmlFile, String xmlPrefix, Runnable runnable, String oldPrefix) {
        String oldName = getOldName(xmlFile);
        // PlugUtil.showMsg("binding xmlFile:" + oldName, project);
        if (oldName == null) {
            runnable.run();
            return;
        }
        if (!oldName.startsWith(xmlPrefix)) {
            runnable.run();
            return;
        }
        oldName = oldName.substring(xmlPrefix.length());
        if (oldPrefix != null && oldPrefix.length() > 0) {
            oldName = oldPrefix + oldName;
        }
        String[] arr = oldName.split("_");
        if (arr == null || arr.length <= 0) {
            runnable.run();
            return;
        }
        String bindingName = "";
        for (String part : arr) {
            bindingName += upperFistChar(part);
        }
        bindingName += "Binding";
        PlugUtil.showMsg("binding name:" + bindingName, project);

        String[] prefixarr = xmlPrefix.split("_");
        if (prefixarr == null || prefixarr.length <= 0) {
            runnable.run();
            return;
        }
        String prefix = upperFistChar(prefixarr[0]);
        String newBindingName = prefix + bindingName;
        PlugUtil.showMsg("newBindingName name:" + newBindingName, project);

        replaceStringUse(bindingName, newBindingName, runnable);
    }

    private void replaceStringUse(String beReplace, String newVal, Runnable runnable) {
        FindManager findManager = FindManager.getInstance(this.project);

        FindModel findModel;
        findModel = findManager.getFindInProjectModel().clone();
        findModel.setReplaceState(true);
        findModel.setProjectScope(true);
        findModel.setStringToFind(beReplace);
        findModel.setStringToReplace(newVal);

        replaceInPath(findModel, runnable);
    }

    public void replaceInPath(@NotNull FindModel findModel, Runnable runnable) {
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
                this.searchAndShowUsages(manager, factory, findModelCopy, presentation, processPresentation, runnable);
            }
        } else {
            runnable.run();
        }
    }

    public void searchAndShowUsages(@NotNull final UsageViewManager manager, @NotNull final Factory<UsageSearcher> usageSearcherFactory, @NotNull final FindModel findModelCopy, @NotNull final UsageViewPresentation presentation, @NotNull final FindUsagesProcessPresentation processPresentation,
                                    Runnable runnable) {
        presentation.setMergeDupLinesAvailable(false);
        ReplaceInProjectTarget target = new ReplaceInProjectTarget(this.project, findModelCopy);
        ((FindManagerImpl) FindManager.getInstance(this.project)).getFindUsagesManager().addToHistory(target);
        final ReplaceContext[] context = new ReplaceContext[1];
        manager.searchAndShowUsages(new UsageTarget[]{target}, usageSearcherFactory, processPresentation, presentation, new UsageViewManager.UsageViewStateListener() {
            public void usageViewCreated(@NotNull UsageView usageView) {
                context[0] = new ReplaceContext(usageView, findModelCopy);
                //findingUsagesFinished(usageView);
            }

            public void findingUsagesFinished(UsageView usageView) {
                if (context[0] != null) {
                    PlugUtil.showMsg("findingUsagesFinished", project);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            replaceUsagesUnderCommand(context[0], usageView.getUsages(), runnable);
                            context[0].invalidateExcludedSetCache();
                        } catch (Throwable e) {
                            PlugUtil.showMsg(getStack(e), project);
                            runnable.run();
                        }
                    }, project.getDisposed());

//                    replaceUsagesUnderCommand(context[0], usageView.getUsages(), runnable);
//                    context[0].invalidateExcludedSetCache();
                } else {
                    runnable.run();
                }

            }
        });
    }

    private void replaceUsagesUnderCommand(@NotNull ReplaceContext replaceContext, @NotNull Set<? extends Usage> usagesSet,
                                           Runnable runnable) throws Throwable {

        PlugUtil.showMsg("replaceUsagesUnderCommand", project);

        if (!usagesSet.isEmpty()) {
            PlugUtil.showMsg("replaceUsagesUnderCommand1", project);
            List<Usage> usages = new ArrayList(usagesSet);
            Collections.sort(usages, UsageViewImpl.USAGE_COMPARATOR);
            if (this.ensureUsagesWritable(replaceContext, usages)) {
//                CommandProcessor.getInstance().executeCommand(this.project, () -> {
//                    boolean success = this.replaceUsages(replaceContext, usages);
//                    UsageView usageView = replaceContext.getUsageView();
//
//                }, FindBundle.message("find.replace.command", new Object[0]), (Object)null);

                PlugUtil.showMsg("replaceUsagesUnderCommand2", project);
                boolean success = this.replaceUsages(replaceContext, usages, runnable);
                UsageView usageView = replaceContext.getUsageView();
                replaceContext.invalidateExcludedSetCache();
            } else {
                runnable.run();
            }
        } else {
            PlugUtil.showMsg("replaceUsagesUnderCommand usagesSet.isEmpty", project);
            runnable.run();
        }
    }

    private boolean ensureUsagesWritable(ReplaceContext replaceContext, Collection<? extends Usage> selectedUsages) {
        Set<VirtualFile> readOnlyFiles = null;
        Iterator var4 = selectedUsages.iterator();

        while (var4.hasNext()) {
            Usage usage = (Usage) var4.next();
            VirtualFile file = ((UsageInFile) usage).getFile();
            if (file != null && !file.isWritable()) {
                if (readOnlyFiles == null) {
                    readOnlyFiles = new HashSet();
                }

                readOnlyFiles.add(file);
            }
        }

        if (readOnlyFiles != null) {
            ReadonlyStatusHandler.getInstance(this.project).ensureFilesWritable(readOnlyFiles);
        }

        if (hasReadOnlyUsages(selectedUsages)) {
            int result = Messages.showOkCancelDialog(replaceContext.getUsageView().getComponent(), FindBundle.message("find.replace.occurrences.in.read.only.files.prompt", new Object[0]), FindBundle.message("find.replace.occurrences.in.read.only.files.title", new Object[0]), Messages.getWarningIcon());
            if (result != 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasReadOnlyUsages(Collection<? extends Usage> usages) {
        Iterator var1 = usages.iterator();

        Usage usage;
        do {
            if (!var1.hasNext()) {
                return false;
            }

            usage = (Usage) var1.next();
        } while (!usage.isReadOnly());

        return true;
    }

    private boolean replaceUsages(@NotNull ReplaceContext replaceContext, @NotNull Collection<? extends Usage> usages, Runnable runnable) throws Throwable {
        if (!ensureUsagesWritable(replaceContext, usages)) {
            PlugUtil.showMsg("replaceUsages", project);
            runnable.run();
            return true;
        }
        PlugUtil.showMsg("replaceUsages1", project);
        int[] replacedCount = {0};
        boolean[] success = {true};
        WriteCommandAction.writeCommandAction(project).run(new ThrowableRunnable<Throwable>() {
            @Override
            public void run() throws Throwable {

//        ((ApplicationImpl)ApplicationManager.getApplication()).runWriteAction(
//             new Runnable() {
//                 @Override
//                 public void run() {

                PlugUtil.showMsg("replaceUsages2", project);
                // indicator.setIndeterminate(false);
                int processed = 0;
                VirtualFile lastFile = null;

                for (final Usage usage : usages) {
                    ++processed;
                    // indicator.checkCanceled();
                    //indicator.setFraction((float) processed / usages.size());

                    if (usage instanceof UsageInFile) {
                        VirtualFile virtualFile = ((UsageInFile) usage).getFile();
                        if (virtualFile != null && !virtualFile.equals(lastFile)) {
                            // indicator.setText2(virtualFile.getPresentableUrl());
                            lastFile = virtualFile;
                        }
                    }

                    ProgressManager.getInstance().executeNonCancelableSection(() -> {
                        try {
                            PlugUtil.showMsg("replaceUsages3", project);
                            if (replaceUsage(usage, replaceContext.getFindModel(), new HashSet<>())) {
                                replacedCount[0]++;
                                PlugUtil.showMsg("replaceUsages4", project);
                            }
                        } catch (Throwable ex) {
                            PlugUtil.showMsg(getStack(ex), project);
                            markAsMalformedReplacement(replaceContext, usage);
                            success[0] = false;
                        }
                    });
                }

                FileDocumentManager.getInstance().saveAllDocuments();
//                 }
//             }
                // );
                // success[0] &= result;
                replaceContext.getUsageView().removeUsagesBulk(usages);

                runnable.run();

            }
        });
        // reportNumberReplacedOccurrences(myProject, replacedCount[0]);
        return success[0];
    }


    public boolean replaceUsage(@NotNull Usage usage, @NotNull FindModel findModel, @NotNull Set<Usage> excludedSet) throws FindManager.MalformedReplacementStringException {

        PlugUtil.showMsg("replaceUsage=====1", project);

        if (excludedSet.contains(usage)) {
            PlugUtil.showMsg("replaceUsages=====9", project);
            return false;
        } else {
            Document document = ((UsageInfo2UsageAdapter) usage).getDocument();
            PlugUtil.showMsg("document.isWritable():" + document.isWritable(), project);
            return !document.isWritable() ? false : ((UsageInfo2UsageAdapter) usage).processRangeMarkers((segment) -> {
                int textOffset = segment.getStartOffset();
                int textEndOffset = segment.getEndOffset();
                Ref stringToReplace = Ref.create();

                try {
                    PlugUtil.showMsg("replaceUsages=====99", project);
                    if (!getStringToReplace(textOffset, textEndOffset, document, findModel, stringToReplace)) {
                        PlugUtil.showMsg("replaceUsages=====99a", project);
                        return true;
                    } else {
                        PlugUtil.showMsg("replaceUsages=====99b", project);
                        if (!stringToReplace.isNull()) {
                            PlugUtil.showMsg("replaceUsages=====99c:" + (CharSequence) stringToReplace.get(), project);
                            document.replaceString(textOffset, textEndOffset, (CharSequence) stringToReplace.get());
                        }

                        return true;
                    }
                } catch (Throwable var10) {
                    PlugUtil.showMsg(getStack(var10), project);
                    PlugUtil.showMsg("replaceUsages=====999", project);
                    return false;
                } finally {

                }
            });
        }

//        if (!exceptionResult.isNull()) {
//
//            PlugUtil.showMsg(getStack(exceptionResult.get()),project);
//            throw (FindManager.MalformedReplacementStringException)exceptionResult.get();
//        } else {
//            return true;
//        }

    }

    private boolean getStringToReplace(int textOffset, int textEndOffset, Document document, FindModel findModel, Ref<? super String> stringToReplace) throws FindManager.MalformedReplacementStringException {
        if (textOffset >= 0 && textOffset < document.getTextLength()) {
            if (textEndOffset >= 0 && textEndOffset <= document.getTextLength()) {
                FindManager findManager = FindManager.getInstance(this.project);
                CharSequence foundString = document.getCharsSequence().subSequence(textOffset, textEndOffset);
                PsiFile file = PsiDocumentManager.getInstance(this.project).getPsiFile(document);
                FindResult findResult = findManager.findString(document.getCharsSequence(), textOffset, findModel, file != null ? file.getVirtualFile() : null);
                if (findResult.isStringFound() && findResult.getStartOffset() >= textOffset && findResult.getEndOffset() <= textEndOffset) {
                    stringToReplace.set(FindManager.getInstance(this.project).getStringToReplace(foundString.toString(), findModel, textOffset, document.getText()));
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
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

    private void renameContent(PsiElement xmlFile, String prefix, String oldPrefix) {
        List<PsiElement> result = getResNameFromLayout(xmlFile);
        if (result == null || result.isEmpty()) {
            PlugUtil.showMsg("skip xml:" + xmlFile, project);
            return;
        }

        for (PsiElement p : result) {
            //PlugUtil.showMsg(p.toString() + "@" + p.getClass().getName(), project);
            dor(p, prefix, oldPrefix);
        }
    }

    private void showStartRootNullMsg() {
        PlugUtil.showMsg("请选择要重构的目录", project, true);

    }

    public final void renameResFile(String prefix, String oldPrefix) {

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
            dor(p, prefix, oldPrefix);
        }

    }

//    private String getJavaPkgName(PsiElement element){
////        if(element instanceof KtElement){
////            String r= KtPsiUtil.getPackageName((KtElement)element);
////            PlugUtil.showMsg("r:"+r,project);
////        }else if(element instanceof PsiClass) {//not found ,must use reflect.todo
////            String r=PsiUtil.getPackageName((PsiClass) element);
////            PlugUtil.showMsg("r java:"+r,project);
////        }
//        if(isJavaClass(element)){
//          return getPsiElementPackageName(element,element.getClass());
//        }
//        return null;
//    }


    private String getFullClassName(PsiElement element) {
        if (element instanceof KtElement) {
            return KtPsiUtil.getPackageName((KtElement) element)+"."+getOldName(element);
        }
        if (isJavaClass(element)) {
            return getPsiElementPackageName(element);
        }

        return null;
    }

    private Method findMethodInParentRec(Class elementClass, String methodName) {
        if (elementClass == null) {
            return null;
        }

        try {
            Method method = elementClass.getDeclaredMethod(methodName);
            return method;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            PlugUtil.showMsg(getStack(e),project);
            return findMethodInParentRec(elementClass.getSuperclass(), methodName);
        }

    }


    private boolean isClassFor(PsiElement element, String className) {
        return element.getClass().getName().equals(className);
    }


    public void renameFragmentAndActivity(String prefix, String oldPrefix) {
        renameClass(prefix, new ConditionPredicate() {
            @Override
            public boolean isMatch(PsiElement element) {
                String oldName = getOldName(element);
                PlugUtil.showMsg("oldName:" + oldName, project);
                if (oldName == null) {
                    return false;
                }
                element.accept(new PsiElementVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        PlugUtil.showMsg("element0:" + element, project);
                        super.visitElement(element);
                    }
                });
                //String name=getFullClassName(element);
               // PlugUtil.showMsg("pkgname:" + name, project);
                //PlugUtil.showMsg("isact:" +isActivity(name), project);
               // PlugUtil.showMsg("isfrag:" +isFragment(name), project);

                 return oldName.contains("Fragment") || oldName.contains("Activity");

            }
        }, oldPrefix);
    }

    public void renameClass(String prefix, ConditionPredicate extraFilter, String oldPrefix) {
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
            dor(p, prefix, oldPrefix);
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

    private boolean isActivity(String clsname) {
      Class cls=findCls(clsname);
      return cls.isAssignableFrom(findCls("android.app.Activity"));
    }

    private boolean isFragment(String clsname) {
        Class cls=findCls(clsname);
        return cls.isAssignableFrom(findCls("androidx.fragment.app.Fragment"))||
                cls.isAssignableFrom(findCls("android.app.Fragment"));
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

    private void dor(PsiElement element, String prefix, String oldPrefix) {
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
            //isSearchTextOccurrences = true;
        }

        if (oldName == null) {
            return;
        }

        if (oldPrefix != null && oldPrefix.length() > 0 && oldName.startsWith(oldPrefix)) {
            oldName = oldName.substring(oldPrefix.length());//del old prefix.
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

    private String getPsiElementPackageName(PsiElement element) {
        try {
            Method getNameMethod =findMethodInParentRec(element.getClass(),"getQualifiedName");
            if(getNameMethod==null){
                return null;
            }
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

                    if (name.equals("declare-styleable") || name.equals("attr") || name.equals("style")) {
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
