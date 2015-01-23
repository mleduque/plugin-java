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

package com.codenvy.ide.ext.java.jdt.internal.text.correction.proposals;

import com.codenvy.ide.ext.java.jdt.Images;
import com.codenvy.ide.ext.java.jdt.core.NamingConventions;
import com.codenvy.ide.ext.java.jdt.core.compiler.ITerminalSymbols;
import com.codenvy.ide.ext.java.jdt.core.dom.AST;
import com.codenvy.ide.ext.java.jdt.core.dom.ASTNode;
import com.codenvy.ide.ext.java.jdt.core.dom.AbstractTypeDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.Assignment;
import com.codenvy.ide.ext.java.jdt.core.dom.Block;
import com.codenvy.ide.ext.java.jdt.core.dom.BodyDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.ChildListPropertyDescriptor;
import com.codenvy.ide.ext.java.jdt.core.dom.CompilationUnit;
import com.codenvy.ide.ext.java.jdt.core.dom.Expression;
import com.codenvy.ide.ext.java.jdt.core.dom.ExpressionStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.FieldAccess;
import com.codenvy.ide.ext.java.jdt.core.dom.FieldDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.ITypeBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.IVariableBinding;
import com.codenvy.ide.ext.java.jdt.core.dom.Initializer;
import com.codenvy.ide.ext.java.jdt.core.dom.MethodDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.Modifier;
import com.codenvy.ide.ext.java.jdt.core.dom.SimpleName;
import com.codenvy.ide.ext.java.jdt.core.dom.SingleVariableDeclaration;
import com.codenvy.ide.ext.java.jdt.core.dom.Statement;
import com.codenvy.ide.ext.java.jdt.core.dom.Type;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclarationExpression;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclarationFragment;
import com.codenvy.ide.ext.java.jdt.core.dom.VariableDeclarationStatement;
import com.codenvy.ide.ext.java.jdt.core.dom.rewrite.ASTRewrite;
import com.codenvy.ide.ext.java.jdt.core.dom.rewrite.ImportRewrite;
import com.codenvy.ide.ext.java.jdt.core.dom.rewrite.ImportRewrite.ImportRewriteContext;
import com.codenvy.ide.ext.java.jdt.internal.corext.codemanipulation.ASTResolving;
import com.codenvy.ide.ext.java.jdt.internal.corext.codemanipulation.ContextSensitiveImportRewriteContext;
import com.codenvy.ide.ext.java.jdt.internal.corext.codemanipulation.StubUtility;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.ASTNodeFactory;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.ASTNodes;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.Bindings;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.TokenScanner;
import com.codenvy.ide.ext.java.jdt.internal.text.correction.CorrectionMessages;
import com.codenvy.ide.legacy.client.api.text.Document;
import com.codenvy.ide.runtime.CoreException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Proposals for 'Assign to variable' quick assist
 * - Assign an expression from an ExpressionStatement to a local or field
 * - Assign a parameter to a field
 */
public class AssignToVariableAssistProposal extends LinkedCorrectionProposal {

    public static final int LOCAL = 1;

    public static final int FIELD = 2;

    private final String KEY_NAME = "name"; //$NON-NLS-1$

    private final String KEY_TYPE = "type"; //$NON-NLS-1$

    private final int fVariableKind;

    private final ASTNode fNodeToAssign; // ExpressionStatement or SingleVariableDeclaration

    private final ITypeBinding fTypeBinding;

    private VariableDeclarationFragment fExistingFragment;

    public AssignToVariableAssistProposal(int variableKind, ExpressionStatement node, ITypeBinding typeBinding,
                                          int relevance, Document document) {
        super("", null, relevance, document, null); //$NON-NLS-1$

        fVariableKind = variableKind;
        fNodeToAssign = node;
        if (typeBinding.isWildcardType()) {
            typeBinding = ASTResolving.normalizeWildcardType(typeBinding, true, node.getAST());
        }

        fTypeBinding = typeBinding;
        if (variableKind == LOCAL) {
            setDisplayName(CorrectionMessages.INSTANCE.AssignToVariableAssistProposal_assigntolocal_description());
            setImage(Images.local_var);
        } else {
            setDisplayName(CorrectionMessages.INSTANCE.AssignToVariableAssistProposal_assigntofield_description());
            setImage(Images.privateField);
        }
        createImportRewrite((CompilationUnit)node.getRoot());
    }

    public AssignToVariableAssistProposal(SingleVariableDeclaration parameter,
                                          VariableDeclarationFragment existingFragment, ITypeBinding typeBinding, int relevance,
                                          Document document) {
        super("", null, relevance, document, null); //$NON-NLS-1$

        fVariableKind = FIELD;
        fNodeToAssign = parameter;
        fTypeBinding = typeBinding;
        fExistingFragment = existingFragment;

        if (existingFragment == null) {
            setDisplayName(CorrectionMessages.INSTANCE.AssignToVariableAssistProposal_assignparamtofield_description());
        } else {
            setDisplayName(CorrectionMessages.INSTANCE
                                             .AssignToVariableAssistProposal_assigntoexistingfield_description(existingFragment.getName()
                                                                                                                               .getIdentifier()));
        }
        setImage(Images.privateField);
    }

    @Override
    protected ASTRewrite getRewrite() throws CoreException {
        if (fVariableKind == FIELD) {
            return doAddField();
        } else { // LOCAL
            return doAddLocal();
        }
    }

    private ASTRewrite doAddLocal() {
        Expression expression = ((ExpressionStatement)fNodeToAssign).getExpression();
        AST ast = fNodeToAssign.getAST();

        ASTRewrite rewrite = ASTRewrite.create(ast);

        createImportRewrite((CompilationUnit)fNodeToAssign.getRoot());

        String[] varNames = suggestLocalVariableNames(fTypeBinding, expression);
        //      for (int i = 0; i < varNames.length; i++)
        //      {
        //         addLinkedPositionProposal(KEY_NAME, varNames[i], null);
        //      }

        VariableDeclarationFragment newDeclFrag = ast.newVariableDeclarationFragment();
        newDeclFrag.setName(ast.newSimpleName(varNames[0]));
        newDeclFrag.setInitializer((Expression)rewrite.createCopyTarget(expression));

        Type type = evaluateType(ast);

        if (ASTNodes.isControlStatementBody(fNodeToAssign.getLocationInParent())) {
            Block block = ast.newBlock();
            block.statements().add(rewrite.createMoveTarget(fNodeToAssign));
            rewrite.replace(fNodeToAssign, block, null);
        }

        if (needsSemicolon(expression)) {
            VariableDeclarationStatement varStatement = ast.newVariableDeclarationStatement(newDeclFrag);
            varStatement.setType(type);
            rewrite.replace(expression, varStatement, null);
        } else {
            // trick for bug 43248: use an VariableDeclarationExpression and keep the ExpressionStatement
            VariableDeclarationExpression varExpression = ast.newVariableDeclarationExpression(newDeclFrag);
            varExpression.setType(type);
            rewrite.replace(expression, varExpression, null);
        }

        //      addLinkedPosition(rewrite.track(newDeclFrag.getName()), true, KEY_NAME);
        //      addLinkedPosition(rewrite.track(type), false, KEY_TYPE);
        //      setEndPosition(rewrite.track(fNodeToAssign)); // set cursor after expression statement

        return rewrite;
    }

    private boolean needsSemicolon(Expression expression) {
        if ((expression.getParent().getFlags() & ASTNode.RECOVERED) != 0) {
            try {
                TokenScanner scanner = new TokenScanner(document);
                return scanner.readNext(expression.getStartPosition() + expression.getLength(), true) !=
                       ITerminalSymbols.TokenNameSEMICOLON;
            } catch (CoreException e) {
                // ignore
            }
        }
        return false;
    }

    private ASTRewrite doAddField() {
        boolean isParamToField = fNodeToAssign.getNodeType() == ASTNode.SINGLE_VARIABLE_DECLARATION;

        ASTNode newTypeDecl = ASTResolving.findParentType(fNodeToAssign);
        if (newTypeDecl == null) {
            return null;
        }

        Expression expression =
                isParamToField ? ((SingleVariableDeclaration)fNodeToAssign).getName() : ((ExpressionStatement)fNodeToAssign)
                        .getExpression();

        AST ast = newTypeDecl.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        createImportRewrite((CompilationUnit)fNodeToAssign.getRoot());

        BodyDeclaration bodyDecl = ASTResolving.findParentBodyDeclaration(fNodeToAssign);
        Block body;
        if (bodyDecl instanceof MethodDeclaration) {
            body = ((MethodDeclaration)bodyDecl).getBody();
        } else if (bodyDecl instanceof Initializer) {
            body = ((Initializer)bodyDecl).getBody();
        } else {
            return null;
        }

        boolean isAnonymous = newTypeDecl.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION;
        boolean isStatic = Modifier.isStatic(bodyDecl.getModifiers()) && !isAnonymous;
        boolean isConstructorParam =
                isParamToField && fNodeToAssign.getParent() instanceof MethodDeclaration
                && ((MethodDeclaration)fNodeToAssign.getParent()).isConstructor();
        int modifiers = Modifier.PRIVATE;
        if (isStatic) {
            modifiers |= Modifier.STATIC;
        } else if (isConstructorParam) {
            modifiers |= Modifier.FINAL;
        }

        VariableDeclarationFragment newDeclFrag = addFieldDeclaration(rewrite, newTypeDecl, modifiers, expression);
        String varName = newDeclFrag.getName().getIdentifier();

        Assignment assignment = ast.newAssignment();
        assignment.setRightHandSide((Expression)rewrite.createCopyTarget(expression));

        //TODO
        boolean needsThis = StubUtility.useThisForFieldAccess();
        if (isParamToField) {
            needsThis |= varName.equals(((SimpleName)expression).getIdentifier());
        }

        SimpleName accessName = ast.newSimpleName(varName);
        if (needsThis) {
            FieldAccess fieldAccess = ast.newFieldAccess();
            fieldAccess.setName(accessName);
            if (isStatic) {
                String typeName = ((AbstractTypeDeclaration)newTypeDecl).getName().getIdentifier();
                fieldAccess.setExpression(ast.newSimpleName(typeName));
            } else {
                fieldAccess.setExpression(ast.newThisExpression());
            }
            assignment.setLeftHandSide(fieldAccess);
        } else {
            assignment.setLeftHandSide(accessName);
        }

        ASTNode selectionNode;
        if (isParamToField) {
            // assign parameter to field
            ExpressionStatement statement = ast.newExpressionStatement(assignment);
            int insertIdx = findAssignmentInsertIndex(body.statements());
            rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY).insertAt(statement, insertIdx, null);
            selectionNode = statement;
        } else {
            if (needsSemicolon(expression)) {
                rewrite.replace(expression, ast.newExpressionStatement(assignment), null);
            } else {
                rewrite.replace(expression, assignment, null);
            }
            selectionNode = fNodeToAssign;
        }

        //      addLinkedPosition(rewrite.track(newDeclFrag.getName()), false, KEY_NAME);
        //      if (!isParamToField)
        //      {
        //         FieldDeclaration fieldDeclaration = (FieldDeclaration)newDeclFrag.getParent();
        //         addLinkedPosition(rewrite.track(fieldDeclaration.getType()), false, KEY_TYPE);
        //      }
        //      addLinkedPosition(rewrite.track(accessName), true, KEY_NAME);
        //      IVariableBinding variableBinding = newDeclFrag.resolveBinding();
        //      if (variableBinding != null)
        //      {
        //         SimpleName[] linkedNodes = LinkedNodeFinder.findByBinding(fNodeToAssign.getRoot(), variableBinding);
        //         for (int i = 0; i < linkedNodes.length; i++)
        //         {
        //            addLinkedPosition(rewrite.track(linkedNodes[i]), false, KEY_NAME);
        //         }
        //      }
        //      setEndPosition(rewrite.track(selectionNode));

        return rewrite;
    }

    private VariableDeclarationFragment addFieldDeclaration(ASTRewrite rewrite, ASTNode newTypeDecl, int modifiers,
                                                            Expression expression) {
        if (fExistingFragment != null) {
            return fExistingFragment;
        }

        ChildListPropertyDescriptor property = ASTNodes.getBodyDeclarationsProperty(newTypeDecl);
        List<BodyDeclaration> decls = (List<BodyDeclaration>)newTypeDecl.getStructuralProperty(property);
        AST ast = newTypeDecl.getAST();
        String[] varNames = suggestFieldNames(fTypeBinding, expression, modifiers);
        //      for (int i = 0; i < varNames.length; i++)
        //      {
        //         addLinkedPositionProposal(KEY_NAME, varNames[i], null);
        //      }
        String varName = varNames[0];

        VariableDeclarationFragment newDeclFrag = ast.newVariableDeclarationFragment();
        newDeclFrag.setName(ast.newSimpleName(varName));

        FieldDeclaration newDecl = ast.newFieldDeclaration(newDeclFrag);

        Type type = evaluateType(ast);
        newDecl.setType(type);
        newDecl.modifiers().addAll(ASTNodeFactory.newModifiers(ast, modifiers));

        //      ModifierCorrectionSubProcessor.installLinkedVisibilityProposals(getLinkedProposalModel(), rewrite,
        //         newDecl.modifiers(), false);

        int insertIndex = findFieldInsertIndex(decls, fNodeToAssign.getStartPosition());
        rewrite.getListRewrite(newTypeDecl, property).insertAt(newDecl, insertIndex, null);

        return newDeclFrag;
    }

    private Type evaluateType(AST ast) {
        ITypeBinding[] proposals = ASTResolving.getRelaxingTypes(ast, fTypeBinding);
        //      for (int i = 0; i < proposals.length; i++)
        //      {
        //         addLinkedPositionProposal(KEY_TYPE, proposals[i]);
        //      }
        ImportRewrite importRewrite = getImportRewrite();
        CompilationUnit cuNode = (CompilationUnit)fNodeToAssign.getRoot();
        ImportRewriteContext context =
                new ContextSensitiveImportRewriteContext(cuNode, fNodeToAssign.getStartPosition(), importRewrite);
        return importRewrite.addImport(fTypeBinding, ast, context);
    }

    private String[] suggestLocalVariableNames(ITypeBinding binding, Expression expression) {
        return StubUtility.getVariableNameSuggestions(NamingConventions.VK_LOCAL, binding, expression,
                                                      getUsedVariableNames());
    }

    private String[] suggestFieldNames(ITypeBinding binding, Expression expression, int modifiers) {
        int varKind =
                Modifier.isStatic(modifiers) ? NamingConventions.VK_STATIC_FIELD : NamingConventions.VK_INSTANCE_FIELD;
        return StubUtility.getVariableNameSuggestions(varKind, binding, expression, getUsedVariableNames());
    }

    private Collection<String> getUsedVariableNames() {
        return Arrays.asList(ASTResolving.getUsedVariableNames(fNodeToAssign));
    }

    private int findAssignmentInsertIndex(List<Statement> statements) {

        HashSet<String> paramsBefore = new HashSet<String>();
        List<SingleVariableDeclaration> params = ((MethodDeclaration)fNodeToAssign.getParent()).parameters();
        for (int i = 0; i < params.size() && (params.get(i) != fNodeToAssign); i++) {
            SingleVariableDeclaration decl = params.get(i);
            paramsBefore.add(decl.getName().getIdentifier());
        }

        int i = 0;
        for (i = 0; i < statements.size(); i++) {
            Statement curr = statements.get(i);
            switch (curr.getNodeType()) {
                case ASTNode.CONSTRUCTOR_INVOCATION:
                case ASTNode.SUPER_CONSTRUCTOR_INVOCATION:
                    break;
                case ASTNode.EXPRESSION_STATEMENT:
                    Expression expr = ((ExpressionStatement)curr).getExpression();
                    if (expr instanceof Assignment) {
                        Assignment assignment = (Assignment)expr;
                        Expression rightHand = assignment.getRightHandSide();
                        if (rightHand instanceof SimpleName && paramsBefore.contains(((SimpleName)rightHand).getIdentifier())) {
                            IVariableBinding binding = Bindings.getAssignedVariable(assignment);
                            if (binding == null || binding.isField()) {
                                break;
                            }
                        }
                    }
                    return i;
                default:
                    return i;

            }
        }
        return i;

    }

    private int findFieldInsertIndex(List<BodyDeclaration> decls, int currPos) {
        for (int i = decls.size() - 1; i >= 0; i--) {
            ASTNode curr = decls.get(i);
            if (curr instanceof FieldDeclaration && currPos > curr.getStartPosition() + curr.getLength()) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * Returns the variable kind.
     *
     * @return int
     */
    public int getVariableKind() {
        return fVariableKind;
    }

}
