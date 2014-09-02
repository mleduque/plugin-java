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

import static com.codenvy.ide.api.notification.Notification.Status.FINISHED;

import com.codenvy.ide.api.editor.EditorPartPresenter;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.projecttree.generic.FileNode;
import com.codenvy.ide.api.text.Region;
import com.codenvy.ide.api.texteditor.outline.OutlineModel;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.ext.java.client.JavaLocalizationConstant;
import com.codenvy.ide.ext.java.client.editor.JavaParserWorker;
import com.codenvy.ide.ext.java.client.editor.outline.OutlineUpdater;
import com.codenvy.ide.ext.java.jdt.core.compiler.IProblem;
import com.codenvy.ide.jseditor.client.document.EmbeddedDocument;
import com.codenvy.ide.jseditor.client.reconciler.DirtyRegion;
import com.codenvy.ide.jseditor.client.reconciler.ReconcilingStrategy;
import com.codenvy.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class JavaReconcilerStrategy implements ReconcilingStrategy, JavaParserWorker.WorkerCallback<IProblem> {

    private final EmbeddedTextEditorPresenter editor;
    private final JavaParserWorker         worker;
    private final OutlineModel             outlineModel;
    private final NotificationManager      notificationManager;
    private final JavaCodeAssistProcessor  codeAssistProcessor;
    private final JavaLocalizationConstant localizationConstant;

    private       FileNode                       file;
    private       EmbeddedDocument               document;
    private boolean first = true;
    private Notification notification;

    @AssistedInject
    public JavaReconcilerStrategy(@Assisted final EmbeddedTextEditorPresenter editor,
                                  @Assisted final OutlineModel outlineModel,
                                  @Assisted final JavaCodeAssistProcessor codeAssistProcessor,
                                  final JavaParserWorker worker,
                                  final NotificationManager notificationManager,
                                  final JavaLocalizationConstant localizationConstant) {
        this.editor = editor;
        this.worker = worker;
        this.outlineModel = outlineModel;
        this.notificationManager = notificationManager;
        this.codeAssistProcessor = codeAssistProcessor;
        this.localizationConstant = localizationConstant;

        editor.addCloseHandler(new EditorPartPresenter.EditorPartCloseHandler() {
            @Override
            public void onClose(final EditorPartPresenter editor) {
                if (notification != null && !notification.isFinished()) {
                    notification.setStatus(FINISHED);
                    notification.setType(Notification.Type.WARNING);
                    notification.setMessage("Parsing file canceled");
                    notification = null;
                }
            }
        });
    }

    @Override
    public void setDocument(final EmbeddedDocument document) {
        this.document = document;
        file = editor.getEditorInput().getFile();
        new OutlineUpdater(file.getPath(), outlineModel, worker);
    }

    @Override
    public void reconcile(final DirtyRegion dirtyRegion, final Region subRegion) {
        parse();
    }

    public void parse() {
        if (first) {
            notification = new Notification("Parsing file...", Notification.Status.PROGRESS);
            codeAssistProcessor.disableCodeAssistant();
            notificationManager.showNotification(notification);
            first = false;
        }
        final String[] path = file.getPath().substring(1).split("/");
        final String projectPath = "/" + path[0];
        final String parentName = path[path.length - 2];
        worker.parse(document.getContents(), file.getName(), file.getPath(), parentName, projectPath, this);
    }

    @Override
    public void reconcile(final Region partition) {
        parse();
    }

    public FileNode getFile() {
        return file;
    }

    @Override
    public void onResult(final Array<IProblem> problems) {
        if (!first) {
            if (notification != null) {
                notification.setStatus(FINISHED);
                notification.setMessage(localizationConstant.fileFuccessfullyParsed());
                notification = null;
            }
            codeAssistProcessor.enableCodeAssistant();
        }
    }
}
