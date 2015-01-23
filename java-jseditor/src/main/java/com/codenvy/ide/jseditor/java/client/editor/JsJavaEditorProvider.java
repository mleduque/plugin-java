/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.jseditor.java.client.editor;

import static com.codenvy.ide.jseditor.client.partition.DocumentPartitioner.DEFAULT_CONTENT_TYPE;

import java.util.logging.Logger;

import javax.inject.Inject;

import com.codenvy.ide.api.editor.EditorPartPresenter;
import com.codenvy.ide.api.editor.EditorProvider;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.ext.java.client.editor.FileWatcher;
import com.codenvy.ide.jseditor.client.defaulteditor.DefaultEditorProvider;
import com.codenvy.ide.jseditor.client.editorconfig.EditorUpdateAction;
import com.codenvy.ide.jseditor.client.editorconfig.TextEditorConfiguration;
import com.codenvy.ide.jseditor.client.reconciler.Reconciler;
import com.codenvy.ide.jseditor.client.reconciler.ReconcilingStrategy;
import com.codenvy.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;

/** EditorProvider that provides a text editor configured for java source files. */
public class JsJavaEditorProvider implements EditorProvider {

    private static final Logger LOG = Logger.getLogger(JsJavaEditorProvider.class.getName());

    private final DefaultEditorProvider editorProvider;
    private final FileWatcher watcher;
    private final JsJavaEditorConfigurationFactory jsJavaEditorConfigurationFactory;
    private final NotificationManager notificationManager;


    @Inject
    public JsJavaEditorProvider(final DefaultEditorProvider editorProvider,
                                final FileWatcher watcher,
                                final JsJavaEditorConfigurationFactory jsJavaEditorConfigurationFactory,
                                final NotificationManager notificationManager) {
        this.editorProvider = editorProvider;
        this.watcher = watcher;
        this.jsJavaEditorConfigurationFactory = jsJavaEditorConfigurationFactory;
        this.notificationManager = notificationManager;
    }

    @Override
    public String getId() {
        return "JavaEditor";
    }

    @Override
    public String getDescription() {
        return "Java Editor";
    }

    @Override
    public EditorPartPresenter getEditor() {
        LOG.fine("JsJavaEditor instance creation.");

        final EditorPartPresenter textEditor = editorProvider.getEditor();

        if (textEditor instanceof EmbeddedTextEditorPresenter) {
            final EmbeddedTextEditorPresenter< ? > editor = (EmbeddedTextEditorPresenter< ? >)textEditor;
            final TextEditorConfiguration configuration =
                                                          this.jsJavaEditorConfigurationFactory.create(editor);
            editor.initialize(configuration, this.notificationManager);
            editor.addEditorUpdateAction(new EditorUpdateAction() {
                @Override
                public void doRefresh() {
                    final Reconciler reconciler = configuration.getReconciler();
                    if (reconciler != null) {
                        final ReconcilingStrategy strategy = reconciler.getReconcilingStrategy(DEFAULT_CONTENT_TYPE);
                        if (strategy instanceof JavaReconcilerStrategy) {
                            ((JavaReconcilerStrategy)strategy).parse();
                        }
                    }
                }
            });
        }
        watcher.editorOpened(textEditor);
        return textEditor;
    }

}
