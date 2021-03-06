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
package com.codenvy.ide.ext.java.jdt.internal.corext.codemanipulation;

import com.codenvy.ide.ext.java.jdt.compiler.batch.CompilationUnit;
import com.codenvy.ide.ext.java.jdt.core.CompletionProposal;
import com.codenvy.ide.ext.java.jdt.core.CompletionRequestor;
import com.codenvy.ide.ext.java.jdt.core.Flags;
import com.codenvy.ide.ext.java.jdt.core.JavaCore;
import com.codenvy.ide.ext.java.jdt.core.Signature;
import com.codenvy.ide.ext.java.jdt.core.dom.ASTNode;
import com.codenvy.ide.ext.java.jdt.core.dom.Javadoc;
import com.codenvy.ide.ext.java.jdt.core.dom.Name;
import com.codenvy.ide.ext.java.jdt.core.dom.QualifiedName;
import com.codenvy.ide.ext.java.jdt.internal.codeassist.CompletionEngine;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.ASTNodes;
import com.codenvy.ide.ext.java.jdt.internal.text.correction.NameMatcher;
import com.codenvy.ide.ext.java.worker.WorkerMessageHandler;
import com.codenvy.ide.ext.java.jdt.text.Document;

import java.util.HashSet;

public class SimilarElementsRequestor extends CompletionRequestor {

    public static final int CLASSES = 1 << 1;

    public static final int INTERFACES = 1 << 2;

    public static final int ANNOTATIONS = 1 << 3;

    public static final int ENUMS = 1 << 4;

    public static final int VARIABLES = 1 << 5;

    public static final int PRIMITIVETYPES = 1 << 6;

    public static final int VOIDTYPE = 1 << 7;

    public static final int REF_TYPES = CLASSES | INTERFACES | ENUMS | ANNOTATIONS;

    public static final int REF_TYPES_AND_VAR = REF_TYPES | VARIABLES;

    public static final int ALL_TYPES = PRIMITIVETYPES | REF_TYPES_AND_VAR;

    private static final String[] PRIM_TYPES = {"boolean", "byte", "char", "short", "int", "long", "float", "double"};
            //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$

    private int fKind;

    private String fName;

    private HashSet<SimilarElement> fResult;

    public static SimilarElement[] findSimilarElement(Document document,
                                                      com.codenvy.ide.ext.java.jdt.core.dom.CompilationUnit compilationUnit, Name name,
                                                      int kind) {
        int pos = name.getStartPosition();
        int nArguments = -1;

        String identifier = ASTNodes.getSimpleNameIdentifier(name);
        String returnType = null;

        try {
            if (name.isQualifiedName()) {
                pos = ((QualifiedName)name).getName().getStartPosition();
            } else {
                pos = name.getStartPosition() + 1; // first letter must be included, other
                if (name.getLength() >= 2)
                    pos++;
            }
            Javadoc javadoc = (Javadoc)ASTNodes.getParent(name, ASTNode.JAVADOC);
            if (javadoc != null) {
                //               preparedCU = createPreparedCU(cu, javadoc, name.getStartPosition());
                //               cu = preparedCU;
            }

            SimilarElementsRequestor requestor = new SimilarElementsRequestor(identifier, kind, nArguments, returnType);
            requestor.setIgnored(CompletionProposal.ANONYMOUS_CLASS_DECLARATION, true);
            requestor.setIgnored(CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION, true);
            requestor.setIgnored(CompletionProposal.KEYWORD, true);
            requestor.setIgnored(CompletionProposal.LABEL_REF, true);
            requestor.setIgnored(CompletionProposal.METHOD_DECLARATION, true);
            requestor.setIgnored(CompletionProposal.PACKAGE_REF, true);
            requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
            requestor.setIgnored(CompletionProposal.METHOD_REF, true);
            requestor.setIgnored(CompletionProposal.CONSTRUCTOR_INVOCATION, true);
            requestor.setIgnored(CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER, true);
            requestor.setIgnored(CompletionProposal.FIELD_REF, true);
            requestor.setIgnored(CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER, true);
            requestor.setIgnored(CompletionProposal.LOCAL_VARIABLE_REF, true);
            requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
            requestor.setIgnored(CompletionProposal.VARIABLE_DECLARATION, true);
            requestor.setIgnored(CompletionProposal.POTENTIAL_METHOD_DECLARATION, true);
            requestor.setIgnored(CompletionProposal.METHOD_NAME_REFERENCE, true);
            return requestor.process(pos, document, compilationUnit);
        } finally {
            //            if (preparedCU != null)
            //            {
            //               preparedCU.discardWorkingCopy();
            //            }
        }
    }

    //   private static ICompilationUnit createPreparedCU(ICompilationUnit cu, Javadoc comment, int wordStart)
    //      throws JavaModelException
    //   {
    //      int startpos = comment.getStartPosition();
    //      boolean isTopLevel = comment.getParent().getParent() instanceof CompilationUnit;
    //      char[] content = cu.getBuffer().getCharacters().clone();
    //      if (isTopLevel && (wordStart + 6 < content.length))
    //      {
    //         content[startpos++] = 'i';
    //         content[startpos++] = 'm';
    //         content[startpos++] = 'p';
    //         content[startpos++] = 'o';
    //         content[startpos++] = 'r';
    //         content[startpos++] = 't';
    //      }
    //      if (wordStart < content.length)
    //      {
    //         for (int i = startpos; i < wordStart; i++)
    //         {
    //            content[i] = ' ';
    //         }
    //      }
    //
    //      /*
    //       * Explicitly create a new non-shared working copy.
    //       */
    //      ICompilationUnit newCU = cu.getWorkingCopy(null);
    //      newCU.getBuffer().setContents(content);
    //      return newCU;
    //   }
    //

    /**
     * Constructor for SimilarElementsRequestor.
     *
     * @param name
     *         the name
     * @param kind
     *         the type kind
     * @param nArguments
     *         the number of arguments
     * @param preferredType
     *         the preferred type
     */
    private SimilarElementsRequestor(String name, int kind, int nArguments, String preferredType) {
        super();
        fName = name;
        fKind = kind;

        fResult = new HashSet<SimilarElement>();
        // nArguments and preferredType not yet used
    }

    private void addResult(SimilarElement elem) {
        fResult.add(elem);
    }

    private SimilarElement[] process(int pos, Document document,
                                     com.codenvy.ide.ext.java.jdt.core.dom.CompilationUnit compilationUnit) {
        try {

            CompletionEngine e =
                    new CompletionEngine(WorkerMessageHandler.get().getNameEnvironment(), this, JavaCore.getOptions());
            e.complete(new CompilationUnit(document.get().toCharArray(), "", "UTF-8"), pos, 0);
            processKeywords();
            return fResult.toArray(new SimilarElement[fResult.size()]);
        } finally {
            fResult.clear();
        }
    }

    private boolean isKind(int kind) {
        return (fKind & kind) != 0;
    }

    /** Method addPrimitiveTypes. */
    private void processKeywords() {
        if (isKind(PRIMITIVETYPES)) {
            for (int i = 0; i < PRIM_TYPES.length; i++) {
                if (NameMatcher.isSimilarName(fName, PRIM_TYPES[i])) {
                    addResult(new SimilarElement(PRIMITIVETYPES, PRIM_TYPES[i], 50));
                }
            }
        }
        if (isKind(VOIDTYPE)) {
            String voidType = "void"; //$NON-NLS-1$
            if (NameMatcher.isSimilarName(fName, voidType)) {
                addResult(new SimilarElement(PRIMITIVETYPES, voidType, 50));
            }
        }
    }

    private static final int getKind(int flags, char[] typeNameSig) {
        if (Signature.getTypeSignatureKind(typeNameSig) == Signature.TYPE_VARIABLE_SIGNATURE) {
            return VARIABLES;
        }
        if (Flags.isAnnotation(flags)) {
            return ANNOTATIONS;
        }
        if (Flags.isInterface(flags)) {
            return INTERFACES;
        }
        if (Flags.isEnum(flags)) {
            return ENUMS;
        }
        return CLASSES;
    }

    private void addType(char[] typeNameSig, int flags, int relevance) {
        int kind = getKind(flags, typeNameSig);
        if (!isKind(kind)) {
            return;
        }
        String fullName = new String(Signature.toCharArray(Signature.getTypeErasure(typeNameSig)));
        //TODO
        //      if (TypeFilter.isFiltered(fullName))
        //      {
        //         return;
        //      }
        if (NameMatcher.isSimilarName(fName, Signature.getSimpleName(fullName))) {
            addResult(new SimilarElement(kind, fullName, relevance));
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.core.CompletionRequestor#accept(org.eclipse.jdt.core.CompletionProposal)
     */
    @Override
    public void accept(CompletionProposal proposal) {
        if (proposal.getKind() == CompletionProposal.TYPE_REF) {
            addType(proposal.getSignature(), proposal.getFlags(), proposal.getRelevance());
        }
    }
}
