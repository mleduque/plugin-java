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
import com.codenvy.ide.ext.java.jdt.JavaUIStatus;
import com.codenvy.ide.ext.java.jdt.codeassistant.api.IProblemLocation;
import com.codenvy.ide.ext.java.jdt.core.ToolFactory;
import com.codenvy.ide.ext.java.jdt.core.compiler.CharOperation;
import com.codenvy.ide.ext.java.jdt.core.compiler.IScanner;
import com.codenvy.ide.ext.java.jdt.core.compiler.ITerminalSymbols;
import com.codenvy.ide.ext.java.jdt.core.compiler.InvalidInputException;
import com.codenvy.ide.ext.java.jdt.internal.corext.dom.TokenScanner;
import com.codenvy.ide.ext.java.jdt.internal.text.correction.CorrectionMessages;
import com.codenvy.ide.legacy.client.api.text.edits.ReplaceEdit;
import com.codenvy.ide.legacy.client.api.text.edits.TextEdit;
import com.codenvy.ide.runtime.CoreException;
import com.codenvy.ide.runtime.IStatus;
import com.codenvy.ide.api.text.BadLocationException;
import com.codenvy.ide.api.text.Document;
import com.codenvy.ide.api.text.Position;
import com.codenvy.ide.api.text.Region;


public class TaskMarkerProposal extends CUCorrectionProposal {

    private IProblemLocation fLocation;

    public TaskMarkerProposal(IProblemLocation location, int relevance, Document document) {
        super("", relevance, document, null); //$NON-NLS-1$
        fLocation = location;

        setDisplayName(CorrectionMessages.INSTANCE.TaskMarkerProposal_description());
        setImage(Images.correction_change);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.ui.text.correction.CUCorrectionProposal#addEdits(org.eclipse.jdt.internal.corext.textmanipulation
     * .TextBuffer)
     */
    @Override
    protected void addEdits(Document document, TextEdit rootEdit) throws CoreException {
        super.addEdits(document, rootEdit);

        try {
            Position pos = getUpdatedPosition(document);
            if (pos != null) {
                rootEdit.addChild(new ReplaceEdit(pos.getOffset(), pos.getLength(), "")); //$NON-NLS-1$
            } else {
                rootEdit.addChild(new ReplaceEdit(fLocation.getOffset(), fLocation.getLength(), "")); //$NON-NLS-1$
            }
        } catch (BadLocationException e) {
            throw new CoreException(JavaUIStatus.createError(IStatus.ERROR, e));
        }
    }

    private Position getUpdatedPosition(Document document) throws BadLocationException {
        IScanner scanner = ToolFactory.createScanner(true, false, false, false);
        scanner.setSource(document.get().toCharArray());

        int token = getSurroundingComment(scanner);
        if (token == ITerminalSymbols.TokenNameEOF) {
            return null;
        }
        int commentStart = scanner.getCurrentTokenStartPosition();
        int commentEnd = scanner.getCurrentTokenEndPosition() + 1;

        int contentStart = commentStart + 2;
        int contentEnd = commentEnd;
        if (token == ITerminalSymbols.TokenNameCOMMENT_JAVADOC) {
            contentStart = commentStart + 3;
            contentEnd = commentEnd - 2;
        } else if (token == ITerminalSymbols.TokenNameCOMMENT_BLOCK) {
            contentEnd = commentEnd - 2;
        }
        if (hasContent(document, contentStart, fLocation.getOffset())
            || hasContent(document, contentEnd, fLocation.getOffset() + fLocation.getLength())) {
            return new Position(fLocation.getOffset(), fLocation.getLength());
        }

        Region startRegion = document.getLineInformationOfOffset(commentStart);
        int start = startRegion.getOffset();
        boolean contentAtBegining = hasContent(document, start, commentStart);

        if (contentAtBegining) {
            start = commentStart;
        }

        int end;
        if (token == ITerminalSymbols.TokenNameCOMMENT_LINE) {
            if (contentAtBegining) {
                end = startRegion.getOffset() + startRegion.getLength(); // only to the end of the line
            } else {
                end = commentEnd; // includes new line
            }
        } else {
            int endLine = document.getLineOfOffset(commentEnd - 1);
            if (endLine + 1 == document.getNumberOfLines() || contentAtBegining) {
                Region endRegion = document.getLineInformation(endLine);
                end = endRegion.getOffset() + endRegion.getLength();
            } else {
                Region endRegion = document.getLineInformation(endLine + 1);
                end = endRegion.getOffset();
            }
        }
        if (hasContent(document, commentEnd, end)) {
            end = commentEnd;
            start = commentStart; // only remove comment
        }
        return new Position(start, end - start);
    }

    private int getSurroundingComment(IScanner scanner) {
        try {
            int start = fLocation.getOffset();
            int end = start + fLocation.getLength();

            int token = scanner.getNextToken();
            while (token != ITerminalSymbols.TokenNameEOF) {
                if (TokenScanner.isComment(token)) {
                    int currStart = scanner.getCurrentTokenStartPosition();
                    int currEnd = scanner.getCurrentTokenEndPosition() + 1;
                    if (currStart <= start && end <= currEnd) {
                        return token;
                    }
                }
                token = scanner.getNextToken();
            }

        } catch (InvalidInputException e) {
            // ignore
        }
        return ITerminalSymbols.TokenNameEOF;
    }

    private boolean hasContent(Document document, int start, int end) throws BadLocationException {
        for (int i = start; i < end; i++) {
            char ch = document.getChar(i);
            if (!CharOperation.isWhitespace(ch)) {
                return true;
            }
        }
        return false;
    }

}
