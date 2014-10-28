/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.jseditor.java.client.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import com.codenvy.ide.api.icon.Icon;
import com.codenvy.ide.api.text.Position;
import com.codenvy.ide.api.text.Region;
import com.codenvy.ide.api.text.annotation.Annotation;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.js.JsoArray;
import com.codenvy.ide.ext.java.client.JavaResources;
import com.codenvy.ide.ext.java.client.editor.CompilationUnitDocumentProvider;
import com.codenvy.ide.ext.java.client.editor.JavaAnnotation;
import com.codenvy.ide.ext.java.client.editor.JavaParserWorker;
import com.codenvy.ide.ext.java.client.editor.QuickFixResolver;
import com.codenvy.ide.ext.java.jdt.core.IJavaModelMarker;
import com.codenvy.ide.ext.java.messages.ProblemLocationMessage;
import com.codenvy.ide.ext.java.messages.WorkerProposal;
import com.codenvy.ide.ext.java.messages.impl.MessagesImpls;
import com.codenvy.ide.jseditor.client.annotation.QueryAnnotationsEvent;
import com.codenvy.ide.jseditor.client.annotation.QueryAnnotationsEvent.AnnotationFilter;
import com.codenvy.ide.jseditor.client.annotation.QueryAnnotationsEvent.QueryCallback;
import com.codenvy.ide.jseditor.client.codeassist.CodeAssistCallback;
import com.codenvy.ide.jseditor.client.codeassist.CompletionProposal;
import com.codenvy.ide.jseditor.client.document.DocumentHandle;
import com.codenvy.ide.jseditor.client.quickfix.QuickAssistInvocationContext;
import com.codenvy.ide.jseditor.client.quickfix.QuickAssistProcessor;
import com.codenvy.ide.jseditor.client.text.LinearRange;
import com.codenvy.ide.jseditor.client.texteditor.EditorHandle;

/**
 * {@link QuickAssistProcessor} for java files.
 */
public class JavaQuickAssistProcessor implements QuickAssistProcessor {

    /** The java parser worker. */
    private final JavaParserWorker worker;
    /** The resources used for java assistants. */
    private final JavaResources javaResources;

    @Inject
    public JavaQuickAssistProcessor(final JavaParserWorker worker,
                                    final JavaResources javaResources) {
        this.worker = worker;
        this.javaResources = javaResources;
    }

    @Override
    public void computeQuickAssistProposals(final QuickAssistInvocationContext quickAssistContext, final CodeAssistCallback callback) {
        final EditorHandle editorHandle = quickAssistContext.getEditorHandle();
        final DocumentHandle documentHandle = quickAssistContext.getDocumentHandle();

        LinearRange tempRange;
        if (quickAssistContext.getLine() != null) {
            tempRange = documentHandle.getDocument().getLinearRangeForLine(quickAssistContext.getLine());
        } else {
            final Region selection = editorHandle.getEditor().getSelectedRegion();
            tempRange = LinearRange.createWithStart(selection.getOffset()).andLength(selection.getLength());
        }
        final LinearRange range = tempRange;

        final boolean goToClosest = (range.getLength() == 0);

        final AnnotationFilter filter = new AnnotationFilter() {
            @Override
            public boolean accept(final Annotation annotation) {
                if (!(annotation instanceof JavaAnnotation)) {
                    return false;
                } else {
                    JavaAnnotation javaAnnotation = (JavaAnnotation)annotation;
                    return !javaAnnotation.isMarkedDeleted() && hasCorrections(annotation);
                }
            }
        };
        final QueryCallback queryCallback = new QueryCallback() {
            @Override
            public void respond(final Map<Annotation, Position> annotations) {
                final Map<Annotation, Position> problems = collectQuickFixableAnnotations(range, annotations, goToClosest);
                setupProposals(callback, editorHandle, range, problems);
            }
        };
        final QueryAnnotationsEvent event = new QueryAnnotationsEvent.Builder().withFilter(filter).withCallback(queryCallback).build();
        documentHandle.getDocEventBus().fireEvent(event);
    }

    private void setupProposals(final CodeAssistCallback callback,
                                final EditorHandle editorHandle,
                                final LinearRange range,
                                final Map<Annotation, Position> annotations) {
        final JsoArray<ProblemLocationMessage> problems = JsoArray.create();
        // collect problem locations and corrections from marker annotations
        if (annotations != null) {
            for (final Entry<Annotation, Position> entry : annotations.entrySet()) {
                final Annotation annotation = entry.getKey();
                if (annotation instanceof JavaAnnotation) {
                    final ProblemLocationMessage problemLocation = getProblemLocation((JavaAnnotation)annotation, entry.getValue());
                    if (problemLocation != null) {
                        problems.add(problemLocation);
                    }
                }
            }
        }
        worker.computeQAProposals(editorHandle.getEditor().getContents(), range.getStartOffset(), range.getLength(),
                                  false, problems,
                                  new JavaParserWorker.WorkerCallback<WorkerProposal>() {
                                      @Override
                                      public void onResult(final Array<WorkerProposal> problems) {
                                          final List<CompletionProposal> proposals = buildProposals(problems);
                                          callback.proposalComputed(proposals);
                                      }
                                  });
    }

    private static boolean hasCorrections(final Annotation annotation) {
        if (annotation instanceof JavaAnnotation) {
            final JavaAnnotation javaAnnotation = (JavaAnnotation)annotation;
            final int problemId = javaAnnotation.getId();
            if (problemId != -1) {
                return QuickFixResolver.hasCorrections(problemId);
            }
        }
        return false;
    }

    private static ProblemLocationMessage getProblemLocation(final JavaAnnotation javaAnnotation, final Position position) {
        final int problemId = javaAnnotation.getId();
        if (problemId != -1 && position != null) {
            final MessagesImpls.ProblemLocationMessageImpl problemLocations = MessagesImpls.ProblemLocationMessageImpl.make();

            problemLocations.setOffset(position.getOffset()).setLength(position.getLength());
            problemLocations.setIsError(CompilationUnitDocumentProvider.ProblemAnnotation.ERROR_ANNOTATION_TYPE.equals(javaAnnotation.getType()));

            final String markerType = javaAnnotation.getMarkerType();
            if (markerType != null) {
                problemLocations.setMarkerType(markerType);
            } else {
                problemLocations.setMarkerType(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
            }

            problemLocations.setProblemId(javaAnnotation.getId());

            if (javaAnnotation.getArguments() != null) {
                problemLocations.setProblemArguments(JsoArray.from(javaAnnotation.getArguments()));
            } else {
                problemLocations.setProblemArguments(null);
            }

            return problemLocations;
        } else {
            return null;
        }
    }

    private List<CompletionProposal> buildProposals(final Array<WorkerProposal> problems) {
        final List<CompletionProposal> proposals = new ArrayList<>();
        for (final WorkerProposal problem : problems.asIterable()) {
            final String style = JavaCodeAssistProcessor.insertStyle(javaResources, problem.displayText());
            final Icon icon = new Icon("", JavaCodeAssistProcessor.getImage(javaResources, problem.image()));
            final CompletionProposal proposal = new CompletionProposalImpl(problem.id(), style, icon,
                                                                           problem.autoInsertable(),
                                                                           worker);
            proposals.add(proposal);
        }
        return proposals;
    }


    private static Map<Annotation, Position> collectQuickFixableAnnotations(final LinearRange lineRange,
                                                                            final Map<Annotation, Position> annotations,
                                                                            final boolean goToClosest) {

        if (goToClosest) {
            final int rangeStart = lineRange.getStartOffset();
            final int rangeEnd = rangeStart + lineRange.getLength();

            final ArrayList<Annotation> allAnnotations = new ArrayList<Annotation>();
            int bestOffset = Integer.MAX_VALUE;
            for (Annotation annotation : annotations.keySet()) {
                if (isQuickFixableType(annotation)) {
                    final Position pos = annotations.get(annotation);
                    if (pos != null && isInside(pos.offset, rangeStart, rangeEnd)) { // inside our range?
                        allAnnotations.add(annotation);
                        bestOffset = processAnnotation(annotation, pos, lineRange.getStartOffset(), bestOffset);
                    }
                }
            }
            if (bestOffset == Integer.MAX_VALUE) {
                return null;
            }
            final Map<Annotation, Position> result = new HashMap<>();
            for (final Annotation annotation : allAnnotations) {
                final Position pos = annotations.get(annotation);
                if (isInside(bestOffset, pos.offset, pos.offset + pos.length)) {
                    result.put(annotation, pos);
                }
            }
            return result;
        } else {
            final Map<Annotation, Position> result = new HashMap<>();
            for (final Annotation annotation : annotations.keySet()) {
                if (isQuickFixableType(annotation)) {
                    final Position pos = annotations.get(annotation);
                    if (pos != null && isInside(lineRange.getStartOffset(), pos.offset, pos.offset + pos.length)) {
                        result.put(annotation, pos);
                    }
                }
            }
            if (result.isEmpty()) {
                return null;
            } else {
                return result;
            }
        }
    }

    private static int processAnnotation(Annotation annot, Position pos, int invocationLocation, int bestOffset) {
        final int posBegin = pos.offset;
        final int posEnd = posBegin + pos.length;
        if (isInside(invocationLocation, posBegin, posEnd)) { // covers invocation location?
            return invocationLocation;
        } else if (bestOffset != invocationLocation) {
            final int newClosestPosition = computeBestOffset(posBegin, invocationLocation, bestOffset);
            if (newClosestPosition != -1) {
                if (newClosestPosition != bestOffset) { // new best
                    if (hasCorrections(annot)) { // only jump to it if there are proposals
                        return newClosestPosition;
                    }
                }
            }
        }
        return bestOffset;
    }

    /**
     * Computes and returns the invocation offset given a new position, the initial offset and the best invocation offset found so far.
     * <p>
     * The closest offset to the left of the initial offset is the best. If there is no offset on the left, the closest on the right is the
     * best.
     * </p>
     * 
     * @param newOffset the offset to llok at
     * @param invocationLocation the invocation location
     * @param bestOffset the current best offset
     * @return -1 is returned if the given offset is not closer or the new best offset
     */
    private static int computeBestOffset(int newOffset, int invocationLocation, int bestOffset) {
        if (newOffset <= invocationLocation) {
            if (bestOffset > invocationLocation) {
                return newOffset; // closest was on the right, prefer on the left
            } else if (bestOffset <= newOffset) {
                return newOffset; // we are closer or equal
            }
            return -1; // further away
        }

        if (newOffset <= bestOffset) {
            return newOffset; // we are closer or equal
        }

        return -1; // further away
    }

    /**
     * Tells is the offset is inside the (inclusive) range defined by start-end.
     * 
     * @param offset the offset
     * @param start the start of the range
     * @param end the end of the range
     * @return true iff offset is inside
     */
    private static boolean isInside(int offset, int start, int end) {
        return offset == start || offset == end || (offset > start && offset < end); // make sure to handle 0-length ranges
    }

    private static boolean isQuickFixableType(final Annotation annotation) {
        return (annotation instanceof JavaAnnotation) && !annotation.isMarkedDeleted();
    }
}
