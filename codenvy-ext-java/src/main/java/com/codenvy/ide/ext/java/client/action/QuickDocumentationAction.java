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
package com.codenvy.ide.ext.java.client.action;

import com.codenvy.api.analytics.client.logger.AnalyticsEventLogger;
import com.codenvy.ide.MimeType;
import com.codenvy.ide.api.action.ActionEvent;
import com.codenvy.ide.api.action.ProjectAction;
import com.codenvy.ide.api.editor.EditorAgent;
import com.codenvy.ide.api.editor.EditorInput;
import com.codenvy.ide.api.projecttree.VirtualFile;
import com.codenvy.ide.ext.java.client.JavaLocalizationConstant;
import com.codenvy.ide.ext.java.client.documentation.QuickDocumentation;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class QuickDocumentationAction extends ProjectAction {

    private QuickDocumentation quickDocumentation;
    private EditorAgent        editorAgent;

    private final AnalyticsEventLogger eventLogger;

    @Inject
    public QuickDocumentationAction(JavaLocalizationConstant constant,
                                    QuickDocumentation quickDocumentation,
                                    EditorAgent editorAgent,
                                    AnalyticsEventLogger eventLogger) {
        super(constant.actionQuickdocTitle(), constant.actionQuickdocDescription());
        this.quickDocumentation = quickDocumentation;
        this.editorAgent = editorAgent;
        this.eventLogger = eventLogger;
    }

    @Override
    protected void updateProjectAction(ActionEvent e) {
        if (editorAgent.getActiveEditor() != null) {
            EditorInput input = editorAgent.getActiveEditor().getEditorInput();
            VirtualFile file = input.getFile();
            String mediaType = file.getMediaType();
            if (mediaType != null && (mediaType.equals(MimeType.TEXT_X_JAVA) ||
                                      mediaType.equals(MimeType.TEXT_X_JAVA_SOURCE) ||
                                      mediaType.equals(MimeType.APPLICATION_JAVA_CLASS))) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }
        }
        e.getPresentation().setEnabledAndVisible(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        quickDocumentation.showDocumentation();
    }
}
