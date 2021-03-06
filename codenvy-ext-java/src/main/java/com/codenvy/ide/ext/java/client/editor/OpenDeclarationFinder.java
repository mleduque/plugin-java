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
package com.codenvy.ide.ext.java.client.editor;

import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.editor.EditorAgent;
import com.codenvy.ide.api.editor.EditorPartPresenter;
import com.codenvy.ide.api.projecttree.TreeNode;
import com.codenvy.ide.api.projecttree.TreeStructure;
import com.codenvy.ide.api.projecttree.VirtualFile;
import com.codenvy.ide.api.projecttree.generic.ProjectNode;
import com.codenvy.ide.collections.StringMap;
import com.codenvy.ide.ext.java.client.navigation.JavaNavigationService;
import com.codenvy.ide.ext.java.client.projecttree.JavaTreeStructure;
import com.codenvy.ide.ext.java.shared.OpenDeclarationDescriptor;
import com.codenvy.ide.jseditor.client.text.LinearRange;
import com.codenvy.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.rest.Unmarshallable;
import com.codenvy.ide.util.loging.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class OpenDeclarationFinder {

    private final JavaParserWorker       worker;
    private final EditorAgent            editorAgent;
    private final JavaNavigationService  service;
    private       DtoUnmarshallerFactory factory;
    private       JavaNavigationService  navigationService;
    private       AppContext             context;

    @Inject
    public OpenDeclarationFinder(JavaParserWorker worker, EditorAgent editorAgent, JavaNavigationService service,
                                 DtoUnmarshallerFactory factory, JavaNavigationService navigationService, AppContext context) {
        this.worker = worker;
        this.editorAgent = editorAgent;
        this.service = service;
        this.factory = factory;
        this.navigationService = navigationService;
        this.context = context;
    }

    public void openDeclaration() {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor == null) {
            return;
        }

        if (!(activeEditor instanceof EmbeddedTextEditorPresenter)) {
            Log.error(getClass(), "Open Declaration support only EmbeddedTextEditorPresenter as editor");
            return;
        }
        EmbeddedTextEditorPresenter editor = ((EmbeddedTextEditorPresenter)activeEditor);
        int offset = editor.getCursorOffset();
        final VirtualFile file = editor.getEditorInput().getFile();
        worker.computeJavadocHandle(offset, file.getPath(), new JavaParserWorker.Callback<String>() {
            @Override
            public void onCallback(String result) {
                if (result != null) {
                    sendRequest(result, file.getProject());
                }
            }
        });
    }

    private void sendRequest(String bindingKey, ProjectNode project) {
        Unmarshallable<OpenDeclarationDescriptor> unmarshaller =
                factory.newUnmarshaller(OpenDeclarationDescriptor.class);
        service.findDeclaration(project.getPath(), bindingKey, new AsyncRequestCallback<OpenDeclarationDescriptor>(unmarshaller) {
            @Override
            protected void onSuccess(OpenDeclarationDescriptor result) {
                if (result != null) {
                    handleDescriptor(result);
                }
            }

            @Override
            protected void onFailure(Throwable exception) {
                Log.error(OpenDeclarationFinder.class, exception);
            }
        });
    }

    private void handleDescriptor(final OpenDeclarationDescriptor descriptor) {
        StringMap<EditorPartPresenter> openedEditors = editorAgent.getOpenedEditors();
        for (String s : openedEditors.getKeys().asIterable()) {
            if (descriptor.getPath().equals(s)) {
                EditorPartPresenter editorPartPresenter = openedEditors.get(s);
                editorAgent.activateEditor(editorPartPresenter);
                fileOpened(editorPartPresenter, descriptor);
                return;
            }
        }


        TreeStructure tree = context.getCurrentProject().getCurrentTree();
        if (descriptor.isBinary()) {
            if(tree instanceof JavaTreeStructure){
                ((JavaTreeStructure)tree).getClassFileByPath(context.getCurrentProject().getProjectDescription().getPath(), descriptor.getLibId(), descriptor.getPath(), new AsyncCallback<TreeNode<?>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Log.error(OpenDeclarationFinder.class, caught);
                    }

                    @Override
                    public void onSuccess(TreeNode<?> result) {
                        if (result instanceof VirtualFile) {
                            openFile((VirtualFile)result, descriptor);
                        }
                    }
                });
            }
        } else {
            tree.getNodeByPath(descriptor.getPath(), new AsyncCallback<TreeNode<?>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.error(OpenDeclarationFinder.class, caught);
                }

                @Override
                public void onSuccess(TreeNode<?> result) {
                    if (result instanceof VirtualFile) {
                        openFile((VirtualFile)result, descriptor);
                    }
                }
            });
        }
    }

    private void openFile(VirtualFile result, final OpenDeclarationDescriptor descriptor) {
        editorAgent.openEditor(result, new EditorAgent.OpenEditorCallback() {
            @Override
            public void onEditorOpened(EditorPartPresenter editor) {
                fileOpened(editor, descriptor);
            }
        });
    }

    private void fileOpened(EditorPartPresenter editor, OpenDeclarationDescriptor descriptor) {
        if (editor instanceof EmbeddedTextEditorPresenter) {
            ((EmbeddedTextEditorPresenter)editor).getDocument().setSelectedRange(
                    LinearRange.createWithStart(descriptor.getOffset()).andLength(descriptor.getLength()), true);
        }
    }
}
