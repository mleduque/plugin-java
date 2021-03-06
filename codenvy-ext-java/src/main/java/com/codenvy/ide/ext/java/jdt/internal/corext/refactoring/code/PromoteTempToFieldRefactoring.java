/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.java.jdt.internal.corext.refactoring.code;

import com.codenvy.ide.ext.java.jdt.core.dom.AST;
import com.codenvy.ide.ext.java.jdt.core.dom.ASTNode;
import com.codenvy.ide.ext.java.jdt.core.dom.AbstractTypeDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.AnonymousClassDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.ArrayCreation;
import com.codenvy.ide.ext.java.jdt.core.dom.ArrayInitializer;
import com.codenvy.ide.ext.java.jdt.core.dom.ArrayType;
import com.codenvy.ide.ext.java.jdt.core.dom.Assignment;
import com.codenvy.ide.ext.java.jdt.core.dom.Block;
import com.codenvy.ide.ext.java.jdt.core.dom.BodyDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.CatchClause;
import com.codenvy.ide.ext.java.jdt.core.dom.ChildListPropertyDescriptor;
import com.codenvy.ide.ext.java.jdt.core.dom.CompilationUnit;
import com.codenvy.ide.ext.java.jdt.core.dom.ConstructorInvocation;
import com.codenvy.ide.ext.java.jdt.core.dom.Expression;
import com.codenvy.ide.ext.java.jdt.core.dom.FieldDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.IBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.IExtendedModifier;
import com.codenvy.ide.ext.java.jdt.core.dom.IMethodBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.ITypeBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.IVariableBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.Javadoc;
import com.codenvy.ide.ext.java.jdt.core.dom.MethodDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.Modifier;
import com.codenvy.ide.ext.java.jdt.core.dom.SimpleName;
import com.codenvy.ide.ext.java.jdt.core.dom.Statement;
import com.codenvy.ide.ext.java.jdt.core.dom.SwitchStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.Type;
import com.codenvy.ide.ext.java.jdt.core.dom.TypeDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclarationFragment;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclarationStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.rewrite.ASTRewrite;
import com.codenvy.ide.ext.java.jdt.core.dom.rewrite.ListRewrite;
import com.codenvy.ide.ext.java.jdt.internal.corext.codemanipulation.StubUtility;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.ASTNodeFactory;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.ASTNodes;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.HierarchicalASTVisitor;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.ModifierRewrite;
import com.codenvy.ide.ext.java.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import com.codenvy.ide.ext.java.jdt.internal.corext.refactoring.rename.TempOccurrenceAnalyzer;
import com.codenvy.ide.ext.java.jdt.internal.corext.refactoring.util.TextChangeCompatibility;
import com.codenvy.ide.ext.java.jdt.internal.ui.BindingLabelProvider;
import com.codenvy.ide.ext.java.jdt.internal.ui.JavaElementLabels;
import com.codenvy.ide.ext.java.jdt.refactoring.Change;
import com.codenvy.ide.ext.java.jdt.refactoring.Refactoring;
import com.codenvy.ide.ext.java.jdt.refactoring.RefactoringStatus;
import com.codenvy.ide.ext.java.worker.WorkerMessageHandler;
import com.codenvy.ide.ext.java.jdt.text.Document;
import com.codenvy.ide.ext.java.jdt.text.edits.TextEdit;
import com.codenvy.ide.runtime.Assert;
import com.codenvy.ide.runtime.CoreException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PromoteTempToFieldRefactoring extends Refactoring {

    private static final String ATTRIBUTE_STATIC = "static"; //$NON-NLS-1$

    private static final String ATTRIBUTE_FINAL = "final"; //$NON-NLS-1$

    private static final String ATTRIBUTE_VISIBILITY = "visibility"; //$NON-NLS-1$

    private static final String ATTRIBUTE_INITIALIZE = "initialize"; //$NON-NLS-1$

    private int fSelectionStart;

    private int fSelectionLength;

    //    private ICompilationUnit fCu;

    public static final int INITIALIZE_IN_FIELD = 0;

    public static final int INITIALIZE_IN_METHOD = 1;

    public static final int INITIALIZE_IN_CONSTRUCTOR = 2;

    private static final String LINKED_NAME = "name"; //$NON-NLS-1$

    //------ settings ---------//
    private String fFieldName;

    private int fVisibility; /*see Modifier*/

    private boolean fDeclareStatic;

    private boolean fDeclareFinal;

    private int fInitializeIn; /*see INITIALIZE_IN_* constraints */

    //------ fields used for computations ---------//
    private CompilationUnit fCompilationUnitNode;

    private VariableDeclaration fTempDeclarationNode;

    //------ analysis ---------//
    private boolean fInitializerUsesLocalTypes;

    private boolean fTempTypeUsesClassTypeVariables;

    //------ scripting --------//
    private boolean fSelfInitializing = false;

    private final Document document;

    //	private LinkedProposalModel fLinkedProposalModel;

    /**
     * Creates a new promote temp to field refactoring.
     *
     * @param unit
     *         the compilation unit, or <code>null</code> if invoked by scripting
     * @param selectionStart
     *         start
     * @param selectionLength
     *         length
     */
    public PromoteTempToFieldRefactoring(Document document, CompilationUnit cu, int selectionStart, int selectionLength) {
        this.document = document;
        fCompilationUnitNode = cu;
        Assert.isTrue(selectionStart >= 0);
        Assert.isTrue(selectionLength >= 0);
        fSelectionStart = selectionStart;
        fSelectionLength = selectionLength;
        //      fCu = unit;

        fFieldName = ""; //$NON-NLS-1$
        fVisibility = Modifier.PRIVATE;
        fDeclareStatic = false;
        fDeclareFinal = false;
        fInitializeIn = INITIALIZE_IN_METHOD;
        //        fLinkedProposalModel= null;
    }

    /**
     * Creates a new promote temp to field refactoring.
     *
     * @param declaration
     *         the variable declaration node to convert to a field
     */
    public PromoteTempToFieldRefactoring(VariableDeclaration declaration, Document document) {
        Assert.isTrue(declaration != null);
        this.document = document;
        fTempDeclarationNode = declaration;
        IVariableBinding resolveBinding = declaration.resolveBinding();
        Assert.isTrue(resolveBinding != null && !resolveBinding.isParameter() && !resolveBinding.isField());

        ASTNode root = declaration.getRoot();
        Assert.isTrue(root instanceof CompilationUnit);
        fCompilationUnitNode = (CompilationUnit)root;

        //         IJavaElement input = fCompilationUnitNode.getJavaElement();
        //         Assert.isTrue(input instanceof ICompilationUnit);
        //         fCu = (ICompilationUnit)input;
        //
        fSelectionStart = declaration.getStartPosition();
        fSelectionLength = declaration.getLength();

        fFieldName = ""; //$NON-NLS-1$
        fVisibility = Modifier.PRIVATE;
        fDeclareStatic = false;
        fDeclareFinal = false;
        fInitializeIn = INITIALIZE_IN_METHOD;
        //        fLinkedProposalModel= null;
    }

    //   public PromoteTempToFieldRefactoring(JavaRefactoringArguments arguments, RefactoringStatus status)
    //   {
    //      this(null);
    //      RefactoringStatus initializeStatus = initialize(arguments);
    //      status.merge(initializeStatus);
    //   }

    @Override
    public String getName() {
        return RefactoringCoreMessages.INSTANCE.PromoteTempToFieldRefactoring_name();
    }

    public int[] getAvailableVisibilities() {
        return new int[]{Modifier.PUBLIC, Modifier.PROTECTED, Modifier.NONE, Modifier.PRIVATE};
    }

    public int getVisibility() {
        return fVisibility;
    }

    public boolean getDeclareFinal() {
        return fDeclareFinal;
    }

    public boolean getDeclareStatic() {
        return fDeclareStatic;
    }

    public int getInitializeIn() {
        return fInitializeIn;
    }

    public void setVisibility(int accessModifier) {
        Assert.isTrue(accessModifier == Modifier.PRIVATE || accessModifier == Modifier.NONE
                      || accessModifier == Modifier.PROTECTED || accessModifier == Modifier.PUBLIC);
        fVisibility = accessModifier;
    }

    public void setDeclareFinal(boolean declareFinal) {
        fDeclareFinal = declareFinal;
    }

    public void setDeclareStatic(boolean declareStatic) {
        fDeclareStatic = declareStatic;
    }

    public void setFieldName(String fieldName) {
        Assert.isNotNull(fieldName);
        fFieldName = fieldName;
    }

    public void setInitializeIn(int initializeIn) {
        Assert.isTrue(initializeIn == INITIALIZE_IN_CONSTRUCTOR || initializeIn == INITIALIZE_IN_FIELD
                      || initializeIn == INITIALIZE_IN_METHOD);
        fInitializeIn = initializeIn;
    }

    public boolean canEnableSettingStatic() {
        return fInitializeIn != INITIALIZE_IN_CONSTRUCTOR && !isTempDeclaredInStaticMethod()
               && !fTempTypeUsesClassTypeVariables;
    }

    public boolean canEnableSettingFinal() {
        if (fInitializeIn == INITIALIZE_IN_CONSTRUCTOR)
            return canEnableSettingDeclareInConstructors() && !tempHasAssignmentsOtherThanInitialization();
        else if (fInitializeIn == INITIALIZE_IN_FIELD)
            return canEnableSettingDeclareInFieldDeclaration() && !tempHasAssignmentsOtherThanInitialization();
        else if (getMethodDeclaration().isConstructor())
            return !tempHasAssignmentsOtherThanInitialization();
        else
            return false;
    }

    private boolean tempHasAssignmentsOtherThanInitialization() {
        TempAssignmentFinder assignmentFinder = new TempAssignmentFinder(fTempDeclarationNode);
        fCompilationUnitNode.accept(assignmentFinder);
        return assignmentFinder.hasAssignments();
    }

    public boolean canEnableSettingDeclareInConstructors() {
        return !fDeclareStatic && !fInitializerUsesLocalTypes && !getMethodDeclaration().isConstructor()
               && !isDeclaredInAnonymousClass() && !isTempDeclaredInStaticMethod() && tempHasInitializer();
    }

    public boolean canEnableSettingDeclareInMethod() {
        return !fDeclareFinal && tempHasInitializer();
    }

    private boolean tempHasInitializer() {
        return getTempInitializer() != null;
    }

    public boolean canEnableSettingDeclareInFieldDeclaration() {
        return !fInitializerUsesLocalTypes && tempHasInitializer();
    }

    private Expression getTempInitializer() {
        return fTempDeclarationNode.getInitializer();
    }

    private boolean isTempDeclaredInStaticMethod() {
        return Modifier.isStatic(getMethodDeclaration().getModifiers());
    }

    private MethodDeclaration getMethodDeclaration() {
        return (MethodDeclaration)ASTNodes.getParent(fTempDeclarationNode, MethodDeclaration.METHOD_DECLARATION);
    }

    private boolean isDeclaredInAnonymousClass() {
        return null != ASTNodes.getParent(fTempDeclarationNode, AnonymousClassDeclaration.ANONYMOUS_CLASS_DECLARATION);
    }

    @Override
    public RefactoringStatus checkInitialConditions() throws CoreException {
        RefactoringStatus result = new RefactoringStatus();
        //         Checks.validateModifiesFiles(ResourceUtil.getFiles(new ICompilationUnit[]{fCu}), getValidationContext());
        //      if (result.hasFatalError())
        //         return result;

        //      initAST(pm);

        if (fTempDeclarationNode == null)
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.INSTANCE
                                                                                   .PromoteTempToFieldRefactoring_select_declaration());

        //TODO
        //      if (!Checks.isDeclaredIn(fTempDeclarationNode, MethodDeclaration.class))
        //         return RefactoringStatus
        //            .createFatalErrorStatus(RefactoringCoreMessages.PromoteTempToFieldRefactoring_only_declared_in_methods);

        if (isMethodParameter())
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.INSTANCE
                                                                                   .PromoteTempToFieldRefactoring_method_parameters());

        if (isTempAnExceptionInCatchBlock())
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.INSTANCE
                                                                                   .PromoteTempToFieldRefactoring_exceptions());

        result.merge(checkTempTypeForLocalTypeUsage());
        if (result.hasFatalError())
            return result;

        checkTempInitializerForLocalTypeUsage();

        if (!fSelfInitializing)
            initializeDefaults();
        return result;
    }

    private void initializeDefaults() {
        fVisibility = Modifier.PRIVATE;
        fDeclareStatic = Modifier.isStatic(getMethodDeclaration().getModifiers());
        fDeclareFinal = false;
        if (canEnableSettingDeclareInMethod())
            fInitializeIn = INITIALIZE_IN_METHOD;
        else if (canEnableSettingDeclareInFieldDeclaration())
            fInitializeIn = INITIALIZE_IN_FIELD;
        else if (canEnableSettingDeclareInConstructors())
            fInitializeIn = INITIALIZE_IN_CONSTRUCTOR;
    }

    public String[] guessFieldNames() {
        String rawTempName = StubUtility.getBaseName(fTempDeclarationNode.resolveBinding());
        String[] excludedNames = getNamesOfFieldsInDeclaringType();
        int dim = ASTNodes.getDimensions(fTempDeclarationNode);
        return StubUtility.getFieldNameSuggestions(rawTempName, dim, getModifiers(), excludedNames);
    }

    private String getInitialFieldName() {
        String[] suggestedNames = guessFieldNames();
        if (suggestedNames.length > 0) {
            //			if (fLinkedProposalModel != null) {
            //				LinkedProposalPositionGroup nameGroup= fLinkedProposalModel.getPositionGroup(LINKED_NAME, true);
            //				for (int i= 0; i < suggestedNames.length; i++) {
            //					nameGroup.addProposal(suggestedNames[i], null, suggestedNames.length - i);
            //				}
            //			}
            return suggestedNames[0];
        } else {
            return fTempDeclarationNode.getName().getIdentifier();
        }
    }

    private String[] getNamesOfFieldsInDeclaringType() {
        final AbstractTypeDeclaration type = getEnclosingType();
        if (type instanceof TypeDeclaration) {
            FieldDeclaration[] fields = ((TypeDeclaration)type).getFields();
            List<String> result = new ArrayList<String>(fields.length);
            for (int i = 0; i < fields.length; i++) {
                for (Iterator<VariableDeclarationFragment> iter = fields[i].fragments().iterator(); iter.hasNext(); ) {
                    VariableDeclarationFragment field = iter.next();
                    result.add(field.getName().getIdentifier());
                }
            }
            return result.toArray(new String[result.size()]);
        }
        return new String[]{};
    }

    private void checkTempInitializerForLocalTypeUsage() {
        Expression initializer = fTempDeclarationNode.getInitializer();
        if (initializer == null)
            return;

        IMethodBinding declaringMethodBinding = getMethodDeclaration().resolveBinding();
        ITypeBinding[] methodTypeParameters =
                declaringMethodBinding == null ? new ITypeBinding[0] : declaringMethodBinding.getTypeParameters();
        LocalTypeAndVariableUsageAnalyzer localTypeAnalyer = new LocalTypeAndVariableUsageAnalyzer(methodTypeParameters);
        initializer.accept(localTypeAnalyer);
        fInitializerUsesLocalTypes = !localTypeAnalyer.getUsageOfEnclosingNodes().isEmpty();
    }

    private RefactoringStatus checkTempTypeForLocalTypeUsage() {
        VariableDeclarationStatement vds = getTempDeclarationStatement();
        if (vds == null)
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.INSTANCE
                                                                                   .PromoteTempToFieldRefactoring_cannot_promote());
        Type type = vds.getType();
        ITypeBinding binding = type.resolveBinding();
        if (binding == null)
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.INSTANCE
                                                                                   .PromoteTempToFieldRefactoring_cannot_promote());

        IMethodBinding declaringMethodBinding = getMethodDeclaration().resolveBinding();
        ITypeBinding[] methodTypeParameters =
                declaringMethodBinding == null ? new ITypeBinding[0] : declaringMethodBinding.getTypeParameters();
        LocalTypeAndVariableUsageAnalyzer analyzer = new LocalTypeAndVariableUsageAnalyzer(methodTypeParameters);
        type.accept(analyzer);
        boolean usesLocalTypes = !analyzer.getUsageOfEnclosingNodes().isEmpty();
        fTempTypeUsesClassTypeVariables = analyzer.getClassTypeVariablesUsed();
        if (usesLocalTypes)
            return RefactoringStatus.createFatalErrorStatus(RefactoringCoreMessages.INSTANCE







































































































































































































































































































































































                                                                                   .PromoteTempToFieldRefactoring_uses_type_declared_locally());
        return null;
    }

    private VariableDeclarationStatement getTempDeclarationStatement() {
        return (VariableDeclarationStatement)ASTNodes.getParent(fTempDeclarationNode,
                                                                VariableDeclarationStatement.VARIABLE_DECLARATION_STATEMENT);
    }

    private boolean isTempAnExceptionInCatchBlock() {
        return (fTempDeclarationNode.getParent() instanceof CatchClause);
    }

    private boolean isMethodParameter() {
        return (fTempDeclarationNode.getParent() instanceof MethodDeclaration);
    }

    //   private void initAST(IProgressMonitor pm)
    //   {
    //      if (fCompilationUnitNode == null)
    //      {
    //         fCompilationUnitNode = RefactoringASTParser.parseWithASTProvider(fCu, true, pm);
    //         fTempDeclarationNode =
    //            TempDeclarationFinder.findTempDeclaration(fCompilationUnitNode, fSelectionStart, fSelectionLength);
    //      }
    //   }

    public RefactoringStatus validateInput() {
        //TODO
        return new RefactoringStatus();//Checks.checkFieldName(fFieldName, fCu);
    }

    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.Refactoring#checkInput(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public RefactoringStatus checkFinalConditions() throws CoreException {
        RefactoringStatus result = new RefactoringStatus();
        result.merge(checkClashesWithExistingFields());
        if (fInitializeIn == INITIALIZE_IN_CONSTRUCTOR)
            result.merge(checkClashesInConstructors());
        return result;
    }

    private RefactoringStatus checkClashesInConstructors() {
        Assert.isTrue(fInitializeIn == INITIALIZE_IN_CONSTRUCTOR);
        Assert.isTrue(!isDeclaredInAnonymousClass());
        final AbstractTypeDeclaration declaration = (AbstractTypeDeclaration)getMethodDeclaration().getParent();
        if (declaration instanceof TypeDeclaration) {
            MethodDeclaration[] methods = ((TypeDeclaration)declaration).getMethods();
            for (int i = 0; i < methods.length; i++) {
                MethodDeclaration method = methods[i];
                if (!method.isConstructor())
                    continue;
                NameCollector nameCollector = new NameCollector(method) {
                    @Override
                    protected boolean visitNode(ASTNode node) {
                        return true;
                    }
                };
                method.accept(nameCollector);
                List<String> names = nameCollector.getNames();
                if (names.contains(fFieldName)) {
                    String msg =
                            RefactoringCoreMessages.INSTANCE.PromoteTempToFieldRefactoring_Name_conflict(fFieldName,
                                                                                                         BindingLabelProvider
                                                                                                                 .getBindingLabel(
                                                                                                                         method.resolveBinding(),
                                                                                                                         JavaElementLabels.ALL_FULLY_QUALIFIED));
                    return RefactoringStatus.createFatalErrorStatus(msg);
                }
            }
        }
        return null;
    }

    private RefactoringStatus checkClashesWithExistingFields() {
        FieldDeclaration[] existingFields = getFieldDeclarations(getBodyDeclarationListOfDeclaringType());
        for (int i = 0; i < existingFields.length; i++) {
            FieldDeclaration declaration = existingFields[i];
            VariableDeclarationFragment[] fragments =
                    (VariableDeclarationFragment[])declaration.fragments().toArray(
                            new VariableDeclarationFragment[declaration.fragments().size()]);
            for (int j = 0; j < fragments.length; j++) {
                VariableDeclarationFragment fragment = fragments[j];
                if (fFieldName.equals(fragment.getName().getIdentifier())) {
                    //cannot conflict with more than 1 name
                    //               RefactoringStatusContext context = JavaStatusContext.create(fCu, fragment);
                    return RefactoringStatus.createFatalErrorStatus(
                            RefactoringCoreMessages.INSTANCE.PromoteTempToFieldRefactoring_Name_conflict_with_field(), null);
                }
            }
        }
        return null;
    }

    private ChildListPropertyDescriptor getBodyDeclarationListOfDeclaringType() {
        ASTNode methodParent = getMethodDeclaration().getParent();
        if (methodParent instanceof AbstractTypeDeclaration)
            return ((AbstractTypeDeclaration)methodParent).getBodyDeclarationsProperty();
        if (methodParent instanceof AnonymousClassDeclaration)
            return AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY;
        Assert.isTrue(false);
        return null;
    }

    private FieldDeclaration[] getFieldDeclarations(ChildListPropertyDescriptor descriptor) {
        final List<BodyDeclaration> bodyDeclarations =
                (List<BodyDeclaration>)getMethodDeclaration().getParent().getStructuralProperty(descriptor);
        List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>(1);
        for (Iterator<BodyDeclaration> iter = bodyDeclarations.iterator(); iter.hasNext(); ) {
            Object each = iter.next();
            if (each instanceof FieldDeclaration)
                fields.add((FieldDeclaration)each);
        }
        return fields.toArray(new FieldDeclaration[fields.size()]);
    }

    /*
     * @see org.eclipse.jdt.internal.corext.refactoring.base.IRefactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public Change createChange() throws CoreException {
        if (fFieldName.length() == 0) {
            fFieldName = getInitialFieldName();
        }

        ASTRewrite rewrite = ASTRewrite.create(fCompilationUnitNode.getAST());
        if (fInitializeIn == INITIALIZE_IN_METHOD && tempHasInitializer())
            addLocalDeclarationSplit(rewrite);
        else
            addLocalDeclarationRemoval(rewrite);
        if (fInitializeIn == INITIALIZE_IN_CONSTRUCTOR)
            addInitializersToConstructors(rewrite);
        addTempRenames(rewrite);
        addFieldDeclaration(rewrite);

        CompilationUnitChange result =
                new CompilationUnitChange(RefactoringCoreMessages.INSTANCE.PromoteTempToFieldRefactoring_name(), document);
        //         result.setDescriptor(new RefactoringChangeDescriptor(getRefactoringDescriptor()));
        TextEdit resultingEdits = rewrite.rewriteAST(document, WorkerMessageHandler.get().getOptions());
        TextChangeCompatibility.addTextEdit(result,
                                            RefactoringCoreMessages.INSTANCE.PromoteTempToFieldRefactoring_editName(), resultingEdits);
        return result;

    }

    private void addTempRenames(ASTRewrite rewrite) {
        boolean noNameChange = fFieldName.equals(fTempDeclarationNode.getName().getIdentifier());
        if (/*fLinkedProposalModel == null && */noNameChange) {
            return; // no changes needed
        }
        TempOccurrenceAnalyzer analyzer = new TempOccurrenceAnalyzer(fTempDeclarationNode, false);
        analyzer.perform();
        SimpleName[] tempRefs = analyzer.getReferenceNodes(); // no javadocs (refactoring not for parameters)

        for (int j = 0; j < tempRefs.length; j++) {
            SimpleName occurence = tempRefs[j];
            if (noNameChange) {
                //            addLinkedName(rewrite, occurence, false);
            } else {
                SimpleName newName = getAST().newSimpleName(fFieldName);
                //            addLinkedName(rewrite, newName, false);
                rewrite.replace(occurence, newName, null);
            }
        }
    }

    private void addInitializersToConstructors(ASTRewrite rewrite) throws CoreException {
        Assert.isTrue(!isDeclaredInAnonymousClass());
        final AbstractTypeDeclaration declaration = (AbstractTypeDeclaration)getMethodDeclaration().getParent();
        final MethodDeclaration[] constructors = getAllConstructors(declaration);
        if (constructors.length == 0) {
            AST ast = rewrite.getAST();
            MethodDeclaration newConstructor = ast.newMethodDeclaration();
            newConstructor.setConstructor(true);
            newConstructor.modifiers().addAll(
                    ast.newModifiers(declaration.getModifiers() & ModifierRewrite.VISIBILITY_MODIFIERS));
            newConstructor.setName(ast.newSimpleName(declaration.getName().getIdentifier()));
            newConstructor.setJavadoc(getNewConstructorComment(rewrite));
            newConstructor.setBody(ast.newBlock());

            addFieldInitializationToConstructor(rewrite, newConstructor);

            int insertionIndex = computeInsertIndexForNewConstructor(declaration);
            rewrite.getListRewrite(declaration, declaration.getBodyDeclarationsProperty()).insertAt(newConstructor,
                                                                                                    insertionIndex, null);
        } else {
            for (int index = 0; index < constructors.length; index++) {
                if (shouldInsertTempInitialization(constructors[index]))
                    addFieldInitializationToConstructor(rewrite, constructors[index]);
            }
        }
    }

    private String getEnclosingTypeName() {
        return getEnclosingType().getName().getIdentifier();
    }

    private AbstractTypeDeclaration getEnclosingType() {

        return (AbstractTypeDeclaration)ASTNodes.getParent(getTempDeclarationStatement(),
                                                           AbstractTypeDeclaration.TYPE_DECLARATION);
    }

    private Javadoc getNewConstructorComment(ASTRewrite rewrite) throws CoreException {
        if (StubUtility.doAddComments()) {
            String comment =
                    StubUtility.getMethodComment(getEnclosingTypeName(), getEnclosingTypeName(), new String[0], new String[0],
                                                 null, null, false, StubUtility.getLineDelimiterUsed());
            //            CodeGeneration.getMethodComment(fCu, getEnclosingTypeName(), getEnclosingTypeName(), new String[0],
            //               new String[0], null, null, StubUtility.getLineDelimiterUsed());
            //         if (comment != null && comment.length() > 0)
            {
                return (Javadoc)rewrite.createStringPlaceholder(comment, ASTNode.JAVADOC);
            }
        }
        return null;
    }

    private int computeInsertIndexForNewConstructor(AbstractTypeDeclaration declaration) {
        List<BodyDeclaration> declarations = declaration.bodyDeclarations();
        if (declarations.isEmpty())
            return 0;
        int index = findFirstMethodIndex(declaration);
        if (index == -1)
            return declarations.size();
        else
            return index;
    }

    private int findFirstMethodIndex(AbstractTypeDeclaration typeDeclaration) {
        for (int i = 0, n = typeDeclaration.bodyDeclarations().size(); i < n; i++) {
            if (typeDeclaration.bodyDeclarations().get(i) instanceof MethodDeclaration)
                return i;
        }
        return -1;
    }

    private void addFieldInitializationToConstructor(ASTRewrite rewrite, MethodDeclaration constructor) {
        if (constructor.getBody() == null)
            constructor.setBody(getAST().newBlock());
        Statement newStatement = createNewAssignmentStatement(rewrite);
        rewrite.getListRewrite(constructor.getBody(), Block.STATEMENTS_PROPERTY).insertLast(newStatement, null);
    }

    private static boolean shouldInsertTempInitialization(MethodDeclaration constructor) {
        Assert.isTrue(constructor.isConstructor());
        if (constructor.getBody() == null)
            return false;
        List<Statement> statements = constructor.getBody().statements();
        if (statements == null)
            return false;
        if (statements.size() > 0 && statements.get(0) instanceof ConstructorInvocation)
            return false;
        return true;
    }

    private static MethodDeclaration[] getAllConstructors(AbstractTypeDeclaration typeDeclaration) {
        if (typeDeclaration instanceof TypeDeclaration) {
            MethodDeclaration[] allMethods = ((TypeDeclaration)typeDeclaration).getMethods();
            List<MethodDeclaration> result = new ArrayList<MethodDeclaration>(Math.min(allMethods.length, 1));
            for (int i = 0; i < allMethods.length; i++) {
                MethodDeclaration declaration = allMethods[i];
                if (declaration.isConstructor())
                    result.add(declaration);
            }
            return result.toArray(new MethodDeclaration[result.size()]);
        }
        return new MethodDeclaration[]{};
    }

    //   private ConvertLocalVariableDescriptor getRefactoringDescriptor()
    //   {
    //      final Map<String, String> arguments = new HashMap<String, String>();
    //      String project = null;
    //      IJavaProject javaProject = fCu.getJavaProject();
    //      if (javaProject != null)
    //         project = javaProject.getElementName();
    //      final IVariableBinding binding = fTempDeclarationNode.resolveBinding();
    //      final String description =
    //         Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_descriptor_description_short,
    //            BasicElementLabels.getJavaElementName(binding.getName()));
    //      final String header =
    //         Messages
    //            .format(
    //               RefactoringCoreMessages.PromoteTempToFieldRefactoring_descriptor_description,
    //               new String[]{
    //                  BindingLabelProvider.getBindingLabel(binding, JavaElementLabels.ALL_FULLY_QUALIFIED),
    //                  BindingLabelProvider.getBindingLabel(binding.getDeclaringMethod(),
    //                     JavaElementLabels.ALL_FULLY_QUALIFIED)});
    //      final JDTRefactoringDescriptorComment comment = new JDTRefactoringDescriptorComment(project, this, header);
    //      comment.addSetting(Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_original_pattern,
    //         BindingLabelProvider.getBindingLabel(binding, JavaElementLabels.ALL_FULLY_QUALIFIED)));
    //      comment.addSetting(Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_field_pattern,
    //         BasicElementLabels.getJavaElementName(fFieldName)));
    //      switch (fInitializeIn)
    //      {
    //         case INITIALIZE_IN_CONSTRUCTOR :
    //            comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_initialize_constructor);
    //            break;
    //         case INITIALIZE_IN_FIELD :
    //            comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_initialize_declaration);
    //            break;
    //         case INITIALIZE_IN_METHOD :
    //            comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_initialize_method);
    //            break;
    //      }
    //      String visibility = JdtFlags.getVisibilityString(fVisibility);
    //      if ("".equals(visibility)) //$NON-NLS-1$
    //         visibility = RefactoringCoreMessages.PromoteTempToFieldRefactoring_default_visibility;
    //      comment.addSetting(Messages.format(RefactoringCoreMessages.PromoteTempToFieldRefactoring_visibility_pattern,
    //         visibility));
    //      if (fDeclareFinal && fDeclareStatic)
    //         comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_declare_final_static);
    //      else if (fDeclareFinal)
    //         comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_declare_final);
    //      else if (fDeclareStatic)
    //         comment.addSetting(RefactoringCoreMessages.PromoteTempToFieldRefactoring_declare_static);
    //      final ConvertLocalVariableDescriptor descriptor =
    //         RefactoringSignatureDescriptorFactory.createConvertLocalVariableDescriptor(project, description,
    //            comment.asString(), arguments, RefactoringDescriptor.STRUCTURAL_CHANGE);
    //      arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT,
    //         JavaRefactoringDescriptorUtil.elementToHandle(project, fCu));
    //      arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME, fFieldName);
    //      arguments.put(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION, new Integer(fSelectionStart).toString()
    //         + " " + new Integer(fSelectionLength).toString()); //$NON-NLS-1$
    //      arguments.put(ATTRIBUTE_STATIC, Boolean.valueOf(fDeclareStatic).toString());
    //      arguments.put(ATTRIBUTE_FINAL, Boolean.valueOf(fDeclareFinal).toString());
    //      arguments.put(ATTRIBUTE_VISIBILITY, new Integer(fVisibility).toString());
    //      arguments.put(ATTRIBUTE_INITIALIZE, new Integer(fInitializeIn).toString());
    //      return descriptor;
    //   }

    private void addLocalDeclarationSplit(ASTRewrite rewrite) {
        VariableDeclarationStatement tempDeclarationStatement = getTempDeclarationStatement();
        ASTNode parentStatement = tempDeclarationStatement.getParent();

        ListRewrite listRewrite;
        if (parentStatement instanceof SwitchStatement) {
            listRewrite = rewrite.getListRewrite(parentStatement, SwitchStatement.STATEMENTS_PROPERTY);
        } else if (parentStatement instanceof Block) {
            listRewrite = rewrite.getListRewrite(parentStatement, Block.STATEMENTS_PROPERTY);
        } else {
            // should not happen. VariableDeclaration's can not be in a control statement body
            listRewrite = null;
            Assert.isTrue(false);
        }
        int statementIndex = listRewrite.getOriginalList().indexOf(tempDeclarationStatement);
        Assert.isTrue(statementIndex != -1);

        Statement newStatement = createNewAssignmentStatement(rewrite);

        List<VariableDeclarationFragment> fragments = tempDeclarationStatement.fragments();

        int fragmentIndex = fragments.indexOf(fTempDeclarationNode);
        Assert.isTrue(fragmentIndex != -1);

        if (fragments.size() == 1) {
            rewrite.replace(tempDeclarationStatement, newStatement, null);
            return;
        }

        for (int i1 = fragmentIndex, n = fragments.size(); i1 < n; i1++) {
            VariableDeclarationFragment fragment = fragments.get(i1);
            rewrite.remove(fragment, null);
        }
        if (fragmentIndex == 0)
            rewrite.remove(tempDeclarationStatement, null);

        Assert.isTrue(tempHasInitializer());

        listRewrite.insertAt(newStatement, statementIndex + 1, null);

        if (fragmentIndex + 1 < fragments.size()) {
            VariableDeclarationFragment firstFragmentAfter = fragments.get(fragmentIndex + 1);
            VariableDeclarationFragment copyfirstFragmentAfter =
                    (VariableDeclarationFragment)rewrite.createCopyTarget(firstFragmentAfter);
            VariableDeclarationStatement statement = getAST().newVariableDeclarationStatement(copyfirstFragmentAfter);
            Type type = (Type)rewrite.createCopyTarget(tempDeclarationStatement.getType());
            statement.setType(type);
            List<IExtendedModifier> modifiers = tempDeclarationStatement.modifiers();
            if (modifiers.size() > 0) {
                ListRewrite modifiersRewrite =
                        rewrite.getListRewrite(tempDeclarationStatement, VariableDeclarationStatement.MODIFIERS2_PROPERTY);
                ASTNode firstModifier = (ASTNode)modifiers.get(0);
                ASTNode lastModifier = (ASTNode)modifiers.get(modifiers.size() - 1);
                ASTNode modifiersCopy = modifiersRewrite.createCopyTarget(firstModifier, lastModifier);
                statement.modifiers().add(modifiersCopy);
            }
            for (int i = fragmentIndex + 2; i < fragments.size(); i++) {
                VariableDeclarationFragment fragment = fragments.get(i);
                VariableDeclarationFragment fragmentCopy = (VariableDeclarationFragment)rewrite.createCopyTarget(fragment);
                statement.fragments().add(fragmentCopy);
            }
            listRewrite.insertAt(statement, statementIndex + 2, null);
        }
    }

    private Statement createNewAssignmentStatement(ASTRewrite rewrite) {
        AST ast = getAST();
        Assignment assignment = ast.newAssignment();
        SimpleName fieldName = ast.newSimpleName(fFieldName);
        //		addLinkedName(rewrite, fieldName, true);
        assignment.setLeftHandSide(fieldName);
        assignment.setRightHandSide(getTempInitializerCopy(rewrite));
        return ast.newExpressionStatement(assignment);
    }

    //
    //	private void addLinkedName(ASTRewrite rewrite, SimpleName fieldName, boolean isFirst) {
    //		if (fLinkedProposalModel != null) {
    //			fLinkedProposalModel.getPositionGroup(LINKED_NAME, true).addPosition(rewrite.track(fieldName), isFirst);
    //		}
    //	}

    private Expression getTempInitializerCopy(ASTRewrite rewrite) {
        final Expression initializer = (Expression)rewrite.createCopyTarget(getTempInitializer());
        if (initializer instanceof ArrayInitializer && ASTNodes.getDimensions(fTempDeclarationNode) > 0) {
            ArrayCreation arrayCreation = rewrite.getAST().newArrayCreation();
            arrayCreation.setType((ArrayType)ASTNodeFactory.newType(rewrite.getAST(), fTempDeclarationNode));
            arrayCreation.setInitializer((ArrayInitializer)initializer);
            return arrayCreation;
        }
        return initializer;
    }

    private void addLocalDeclarationRemoval(ASTRewrite rewrite) {
        VariableDeclarationStatement tempDeclarationStatement = getTempDeclarationStatement();
        List<VariableDeclarationFragment> fragments = tempDeclarationStatement.fragments();

        int fragmentIndex = fragments.indexOf(fTempDeclarationNode);
        Assert.isTrue(fragmentIndex != -1);
        VariableDeclarationFragment fragment = fragments.get(fragmentIndex);
        rewrite.remove(fragment, null);
        if (fragments.size() == 1)
            rewrite.remove(tempDeclarationStatement, null);
    }

    private void addFieldDeclaration(ASTRewrite rewrite) {
        final ChildListPropertyDescriptor descriptor = getBodyDeclarationListOfDeclaringType();
        FieldDeclaration[] fields = getFieldDeclarations(getBodyDeclarationListOfDeclaringType());
        final ASTNode parent = getMethodDeclaration().getParent();
        int insertIndex;
        if (fields.length == 0)
            insertIndex = 0;
        else
            insertIndex =
                    ((List<? extends ASTNode>)parent.getStructuralProperty(descriptor)).indexOf(fields[fields.length - 1]) + 1;

        final FieldDeclaration declaration = createNewFieldDeclaration(rewrite);
        rewrite.getListRewrite(parent, descriptor).insertAt(declaration, insertIndex, null);
    }

    private FieldDeclaration createNewFieldDeclaration(ASTRewrite rewrite) {
        AST ast = getAST();
        VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
        SimpleName variableName = ast.newSimpleName(fFieldName);
        fragment.setName(variableName);
        //		addLinkedName(rewrite, variableName, false);
        fragment.setExtraDimensions(fTempDeclarationNode.getExtraDimensions());
        if (fInitializeIn == INITIALIZE_IN_FIELD && tempHasInitializer()) {
            Expression initializer = (Expression)rewrite.createCopyTarget(getTempInitializer());
            fragment.setInitializer(initializer);
        }
        FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);

        VariableDeclarationStatement vds = getTempDeclarationStatement();
        Type type = (Type)rewrite.createCopyTarget(vds.getType());
        fieldDeclaration.setType(type);
        fieldDeclaration.modifiers().addAll(ASTNodeFactory.newModifiers(ast, getModifiers()));
        return fieldDeclaration;
    }

    private int getModifiers() {
        int flags = fVisibility;
        if (fDeclareFinal)
            flags |= Modifier.FINAL;
        if (fDeclareStatic)
            flags |= Modifier.STATIC;
        return flags;
    }

    private AST getAST() {
        return fTempDeclarationNode.getAST();
    }

    private static class LocalTypeAndVariableUsageAnalyzer extends HierarchicalASTVisitor {
        private final List<IBinding> fLocalDefinitions = new ArrayList<IBinding>(0); // List of IBinding (Variable and Type)

        private final List<SimpleName> fLocalReferencesToEnclosing = new ArrayList<SimpleName>(0); // List of ASTNodes

        private final List<ITypeBinding> fMethodTypeVariables;

        private boolean fClassTypeVariablesUsed = false;

        public LocalTypeAndVariableUsageAnalyzer(ITypeBinding[] methodTypeVariables) {
            fMethodTypeVariables = Arrays.asList(methodTypeVariables);
        }

        public List<SimpleName> getUsageOfEnclosingNodes() {
            return fLocalReferencesToEnclosing;
        }

        public boolean getClassTypeVariablesUsed() {
            return fClassTypeVariablesUsed;
        }

        @Override
        public boolean visit(SimpleName node) {
            ITypeBinding typeBinding = node.resolveTypeBinding();
            if (typeBinding != null && typeBinding.isLocal()) {
                if (node.isDeclaration()) {
                    fLocalDefinitions.add(typeBinding);
                } else if (!fLocalDefinitions.contains(typeBinding)) {
                    fLocalReferencesToEnclosing.add(node);
                }
            }
            if (typeBinding != null && typeBinding.isTypeVariable()) {
                if (node.isDeclaration()) {
                    fLocalDefinitions.add(typeBinding);
                } else if (!fLocalDefinitions.contains(typeBinding)) {
                    if (fMethodTypeVariables.contains(typeBinding)) {
                        fLocalReferencesToEnclosing.add(node);
                    } else {
                        fClassTypeVariablesUsed = true;
                    }
                }
            }
            IBinding binding = node.resolveBinding();
            if (binding != null && binding.getKind() == IBinding.VARIABLE && !((IVariableBinding)binding).isField()) {
                if (node.isDeclaration()) {
                    fLocalDefinitions.add(binding);
                } else if (!fLocalDefinitions.contains(binding)) {
                    fLocalReferencesToEnclosing.add(node);
                }
            }
            return super.visit(node);
        }
    }

    //   private RefactoringStatus initialize(JavaRefactoringArguments arguments)
    //   {
    //      fSelfInitializing = true;
    //      final String selection = arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION);
    //      if (selection != null)
    //      {
    //         int offset = -1;
    //         int length = -1;
    //         final StringTokenizer tokenizer = new StringTokenizer(selection);
    //         if (tokenizer.hasMoreTokens())
    //            offset = Integer.valueOf(tokenizer.nextToken()).intValue();
    //         if (tokenizer.hasMoreTokens())
    //            length = Integer.valueOf(tokenizer.nextToken()).intValue();
    //         if (offset >= 0 && length >= 0)
    //         {
    //            fSelectionStart = offset;
    //            fSelectionLength = length;
    //         }
    //         else
    //            return RefactoringStatus.createFatalErrorStatus(Messages.format(
    //               RefactoringCoreMessages.InitializableRefactoring_illegal_argument, new Object[]{selection,
    //                  JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION}));
    //      }
    //      else
    //         return RefactoringStatus.createFatalErrorStatus(Messages.format(
    //            RefactoringCoreMessages.InitializableRefactoring_argument_not_exist,
    //            JavaRefactoringDescriptorUtil.ATTRIBUTE_SELECTION));
    //      final String handle = arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT);
    //      if (handle != null)
    //      {
    //         final IJavaElement element =
    //            JavaRefactoringDescriptorUtil.handleToElement(arguments.getProject(), handle, false);
    //         if (element == null || !element.exists() || element.getElementType() != IJavaElement.COMPILATION_UNIT)
    //            return JavaRefactoringDescriptorUtil.createInputFatalStatus(element, getName(),
    //               IJavaRefactorings.CONVERT_LOCAL_VARIABLE);
    //         else
    //            fCu = (ICompilationUnit)element;
    //      }
    //      else
    //         return RefactoringStatus.createFatalErrorStatus(Messages.format(
    //            RefactoringCoreMessages.InitializableRefactoring_argument_not_exist,
    //            JavaRefactoringDescriptorUtil.ATTRIBUTE_INPUT));
    //      final String visibility = arguments.getAttribute(ATTRIBUTE_VISIBILITY);
    //      if (visibility != null && !"".equals(visibility)) {//$NON-NLS-1$
    //         int flag = 0;
    //         try
    //         {
    //            flag = Integer.parseInt(visibility);
    //         }
    //         catch (NumberFormatException exception)
    //         {
    //            return RefactoringStatus.createFatalErrorStatus(Messages.format(
    //               RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_VISIBILITY));
    //         }
    //         fVisibility = flag;
    //      }
    //      final String initialize = arguments.getAttribute(ATTRIBUTE_INITIALIZE);
    //      if (initialize != null && !"".equals(initialize)) {//$NON-NLS-1$
    //         int value = 0;
    //         try
    //         {
    //            value = Integer.parseInt(initialize);
    //         }
    //         catch (NumberFormatException exception)
    //         {
    //            return RefactoringStatus.createFatalErrorStatus(Messages.format(
    //               RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_INITIALIZE));
    //         }
    //         fInitializeIn = value;
    //      }
    //      final String name = arguments.getAttribute(JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME);
    //      if (name != null && !"".equals(name)) //$NON-NLS-1$
    //         fFieldName = name;
    //      else
    //         return RefactoringStatus.createFatalErrorStatus(Messages.format(
    //            RefactoringCoreMessages.InitializableRefactoring_argument_not_exist,
    //            JavaRefactoringDescriptorUtil.ATTRIBUTE_NAME));
    //      final String declareStatic = arguments.getAttribute(ATTRIBUTE_STATIC);
    //      if (declareStatic != null)
    //      {
    //         fDeclareStatic = Boolean.valueOf(declareStatic).booleanValue();
    //      }
    //      else
    //         return RefactoringStatus.createFatalErrorStatus(Messages.format(
    //            RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_STATIC));
    //      final String declareFinal = arguments.getAttribute(ATTRIBUTE_FINAL);
    //      if (declareFinal != null)
    //      {
    //         fDeclareFinal = Boolean.valueOf(declareFinal).booleanValue();
    //      }
    //      else
    //         return RefactoringStatus.createFatalErrorStatus(Messages.format(
    //            RefactoringCoreMessages.InitializableRefactoring_argument_not_exist, ATTRIBUTE_FINAL));
    //      return new RefactoringStatus();
    //   }

    //	public void setLinkedProposalModel(LinkedProposalModel model) {
    //		fLinkedProposalModel= model;
    //	}
}
