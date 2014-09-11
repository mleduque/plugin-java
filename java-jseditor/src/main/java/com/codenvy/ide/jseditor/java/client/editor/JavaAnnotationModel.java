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
import java.util.Iterator;
import java.util.List;

import com.codenvy.ide.api.text.Position;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.collections.StringMap;
import com.codenvy.ide.ext.java.client.JavaCss;
import com.codenvy.ide.ext.java.client.editor.CompilationUnitDocumentProvider.ProblemAnnotation;
import com.codenvy.ide.ext.java.jdt.core.IProblemRequestor;
import com.codenvy.ide.ext.java.jdt.core.compiler.IProblem;
import com.codenvy.ide.jseditor.client.annotation.AnnotationModel;
import com.codenvy.ide.jseditor.client.annotation.AnnotationModelImpl;
import com.codenvy.ide.jseditor.client.partition.DocumentPositionMap;
import com.codenvy.ide.jseditor.client.texteditor.EditorHandle;
import com.codenvy.ide.jseditor.client.texteditor.EditorResources.EditorCss;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.web.bindery.event.shared.EventBus;

public class JavaAnnotationModel extends AnnotationModelImpl implements AnnotationModel, IProblemRequestor {

    private List<IProblem> reportedProblems;

    private List<ProblemAnnotation> fGeneratedAnnotations = new ArrayList<ProblemAnnotation>();

    private final JavaCss javaCss;
    private final EditorCss editorCss;

    @AssistedInject
    public JavaAnnotationModel(final EventBus generalEventBus,
                               @Assisted final EditorHandle editorHandle,
                               @Assisted final DocumentPositionMap docPositionMap,
                               final JavaCss javaCss,
                               final EditorCss editorCss) {
        super(generalEventBus, editorHandle, docPositionMap);
        this.javaCss = javaCss;
        this.editorCss = editorCss;
    }

    protected Position createPositionFromProblem(IProblem problem) {
        int start = problem.getSourceStart();
        int end = problem.getSourceEnd();

        if (start == -1 && end == -1)
            return new Position(0);

        if (start == -1)
            return new Position(end);

        if (end == -1)
            return new Position(start);

        int length = end - start + 1;
        if (length < 0)
            return null;

        return new Position(start, length);
    }

    @Override
    public void acceptProblem(IProblem problem) {
        reportedProblems.add(problem);
    }

    @Override
    public void beginReporting() {
        reportedProblems = new ArrayList<IProblem>();
    }

    @Override
    public void endReporting() {
        reportProblems(reportedProblems);
    }

    private void reportProblems(List<IProblem> problems) {
        boolean temporaryProblemsChanged = false;

        // fPreviouslyOverlaid= fCurrentlyOverlaid;
        // fCurrentlyOverlaid= new ArrayList<JavaMarkerAnnotation>();

        if (fGeneratedAnnotations.size() > 0) {
            temporaryProblemsChanged = true;
            removeAnnotations(fGeneratedAnnotations, false, true);
            fGeneratedAnnotations.clear();
        }

        if (reportedProblems != null && reportedProblems.size() > 0) {

            Iterator<IProblem> e = reportedProblems.iterator();
            while (e.hasNext()) {

                IProblem problem = e.next();
                Position position = createPositionFromProblem(problem);
                if (position != null) {

                    ProblemAnnotation annotation = new ProblemAnnotation(problem);
                    addAnnotation(annotation, position, false);
                    fGeneratedAnnotations.add(annotation);

                    temporaryProblemsChanged = true;
                }
            }

        }

        if (temporaryProblemsChanged)
            fireModelChanged();
    }

    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public StringMap<String> getAnnotationDecorations() {
        StringMap<String> decorations = Collections.createStringMap();
        // TODO configure this
        decorations.put("org.eclipse.jdt.ui.error", this.editorCss.lineError());
        decorations.put("org.eclipse.jdt.ui.warning", this.editorCss.lineWarning());

        return decorations;
    }

    @Override
    public StringMap<String> getAnnotationStyle() {
        StringMap<String> decorations = Collections.createStringMap();
        // //TODO configure this
        decorations.put("org.eclipse.jdt.ui.error", javaCss.overviewMarkError());
        decorations.put("org.eclipse.jdt.ui.warning", javaCss.overviewMarkWarning());
        decorations.put("org.eclipse.jdt.ui.info", javaCss.overviewMarkTask());
        decorations.put("org.eclipse.ui.workbench.texteditor.task", javaCss.overviewMarkTask());
        return decorations;
    }
}
