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
package com.codenvy.ide.ext.java.client.projecttree;

import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.api.app.CurrentProject;
import com.codenvy.ide.api.icon.IconRegistry;
import com.codenvy.ide.api.projecttree.AbstractTreeNode;
import com.codenvy.ide.api.projecttree.TreeNode;
import com.codenvy.ide.api.projecttree.generic.GenericTreeStructure;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.collections.Collections;
import com.codenvy.ide.ext.java.client.navigation.JavaNavigationService;
import com.codenvy.ide.ext.java.client.projecttree.nodes.ExternalLibrariesNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.JarClassNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.JarContainerNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.JarEntryNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.JarFileNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.JarNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.JavaFolderNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.JavaProjectNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.PackageNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.SourceFileNode;
import com.codenvy.ide.ext.java.client.projecttree.nodes.SourceFolderNode;
import com.codenvy.ide.ext.java.shared.Jar;
import com.codenvy.ide.ext.java.shared.JarEntry;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.rest.Unmarshallable;
import com.codenvy.ide.util.loging.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Tree structure for Java project.
 *
 * @author Artem Zatsarynnyy
 */
public class JavaTreeStructure extends GenericTreeStructure {

    protected final IconRegistry          iconRegistry;
    protected final JavaNavigationService navigationService;
    protected final Map<String, ExternalLibrariesNode> librariesNodeMap = new HashMap<>();
    private final JavaTreeSettings settings;
    protected       JavaProjectNode  projectNode;

    protected JavaTreeStructure(JavaNodeFactory nodeFactory, EventBus eventBus, AppContext appContext,
                                ProjectServiceClient projectServiceClient, IconRegistry iconRegistry,
                                DtoUnmarshallerFactory dtoUnmarshallerFactory, JavaNavigationService javaNavigationService) {
        super(nodeFactory, eventBus, appContext, projectServiceClient, dtoUnmarshallerFactory);
        this.iconRegistry = iconRegistry;
        this.navigationService = javaNavigationService;
        this.settings = new JavaTreeSettings();
    }

    /** {@inheritDoc} */
    @Override
    public void getRootNodes(@Nonnull AsyncCallback<Array<TreeNode<?>>> callback) {
        if (projectNode == null) {
            final CurrentProject currentProject = appContext.getCurrentProject();
            if (currentProject != null) {
                projectNode = newJavaProjectNode(currentProject.getRootProject());
            } else {
                callback.onFailure(new IllegalStateException("No project is opened."));
                return;
            }
        }
        callback.onSuccess(Collections.<TreeNode<?>>createArray(projectNode));
    }

    @Nonnull
    @Override
    public JavaTreeSettings getSettings() {
        return settings;
    }

    @Override
    public JavaNodeFactory getNodeFactory() {
        return (JavaNodeFactory)nodeFactory;
    }

    public void getClassFileByPath(String projectPath, final int libId, String path, final AsyncCallback<TreeNode<?>> callback) {
        Unmarshallable<JarEntry> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(JarEntry.class);
        navigationService.getEntry(projectPath, libId, path, new AsyncRequestCallback<JarEntry>(unmarshaller) {
            @Override
            protected void onSuccess(JarEntry result) {
                callback.onSuccess(createNodeForJarEntry(newJavaProjectNode(appContext.getCurrentProject().getProjectDescription()), result, libId));
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }


    public JarEntryNode createNodeForJarEntry(AbstractTreeNode<?> parent, JarEntry entry, int libId) {
        switch (entry.getType()) {
            case FOLDER:
            case PACKAGE:
                return newJarContainerNode(parent, entry, libId);
            case FILE:
                return newJarFileNode(parent, entry, libId);
            case CLASS_FILE:
                return newJarClassNode(parent, entry, libId);
        }
        return null;
    }

    private JavaProjectNode newJavaProjectNode(@Nonnull ProjectDescriptor data) {
        return getNodeFactory().newJavaProjectNode(null, data, this);
    }

    /**
     * Creates a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JavaFolderNode} owned by this tree
     * with the specified {@code parent} and associated {@code data}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link ItemReference}
     * @return a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JavaFolderNode}
     * @throws IllegalStateException
     *         when the specified {@link ItemReference} hasn't type folder
     */
    public JavaFolderNode newJavaFolderNode(@Nonnull AbstractTreeNode parent, @Nonnull ItemReference data) {
        if (!"folder".equals(data.getType())) {
            throw new IllegalArgumentException("The associated ItemReference type must be - folder.");
        }
        return getNodeFactory().newJavaFolderNode(parent, data, this);
    }

    /**
     * Creates a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.SourceFolderNode} owned by this tree
     * with the specified {@code parent} and associated {@code data}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link ItemReference}
     * @return a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.SourceFolderNode}
     * @throws IllegalStateException
     *         when the specified {@link ItemReference} hasn't type folder
     */
    public SourceFolderNode newSourceFolderNode(@Nonnull AbstractTreeNode parent, @Nonnull ItemReference data) {
        if (!"folder".equals(data.getType())) {
            throw new IllegalArgumentException("The associated ItemReference type must be - folder.");
        }
        return getNodeFactory().newSourceFolderNode(parent, data, this);
    }

    /**
     * Creates a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.PackageNode} owned by this tree
     * with the specified {@code parent} and associated {@code data}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link ItemReference}
     * @return a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.PackageNode}
     * @throws IllegalStateException
     *         when the specified {@link ItemReference} hasn't type folder
     */
    public PackageNode newPackageNode(@Nonnull AbstractTreeNode parent, @Nonnull ItemReference data) {
        if (!"folder".equals(data.getType())) {
            throw new IllegalArgumentException("The associated ItemReference type must be - folder.");
        }
        return getNodeFactory().newPackageNode(parent, data, this);
    }

    /**
     * Creates a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.SourceFileNode} owned by this tree
     * with the specified {@code parent} and associated {@code data}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link ItemReference}
     * @return a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.SourceFileNode}
     * @throws IllegalStateException
     *         when the specified {@link ItemReference} hasn't type file
     */
    public SourceFileNode newSourceFileNode(@Nonnull AbstractTreeNode parent, @Nonnull ItemReference data) {
        if (!"file".equals(data.getType())) {
            throw new IllegalArgumentException("The associated ItemReference type must be - file.");
        }
        return getNodeFactory().newSourceFileNode(parent, data, this);
    }

    /**
     * Creates a new {@link ExternalLibrariesNode} owned by this tree with the specified {@code parent}.
     *
     * @param parent
     *         the parent node
     * @return a new {@link ExternalLibrariesNode}
     */
    public ExternalLibrariesNode newExternalLibrariesNode(@Nonnull JavaProjectNode parent) {
        ExternalLibrariesNode librariesNode = getNodeFactory().newExternalLibrariesNode(parent, new Object(), this);
        librariesNodeMap.put(parent.getData().getPath(), librariesNode);
        return librariesNode;
    }

    /**
     * Creates a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JarNode} owned by this tree with
     * the specified {@code parent} and associated {@code data}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link Jar}
     * @return a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JarNode}
     */
    public JarNode newJarNode(@Nonnull ExternalLibrariesNode parent, @Nonnull Jar data) {
        return getNodeFactory().newJarNode(parent, data, this);
    }

    /**
     * Creates a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JarContainerNode} owned by this tree with the
     * specified {@code parent}, associated {@code data} and {@code libId}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link JarEntry}
     * @param libId
     *         lib ID
     * @return a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JarContainerNode}
     */
    public JarContainerNode newJarContainerNode(@Nonnull AbstractTreeNode<?> parent, @Nonnull JarEntry data, int libId) {
        return getNodeFactory().newJarContainerNode(parent, data, this, libId);
    }

    /**
     * Creates a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JarFileNode} owned by this tree with the
     * specified {@code parent}, associated {@code data} and {@code libId}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link JarEntry}
     * @param libId
     *         lib ID
     * @return a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JarFileNode}
     */
    public JarFileNode newJarFileNode(@Nonnull AbstractTreeNode<?> parent, @Nonnull JarEntry data, int libId) {
        return getNodeFactory().newJarFileNode(parent, data, this, libId);
    }

    /**
     * Creates a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JarClassNode} owned by this tree with the
     * specified {@code parent}, associated {@code data} and {@code libId}.
     *
     * @param parent
     *         the parent node
     * @param data
     *         the associated {@link JarEntry}
     * @param libId
     *         lib ID
     * @return a new {@link com.codenvy.ide.ext.java.client.projecttree.nodes.JarClassNode}
     */
    public JarClassNode newJarClassNode(@Nonnull AbstractTreeNode<?> parent, @Nonnull JarEntry data, int libId) {
        return getNodeFactory().newJarClassNode(parent, data, this, libId);
    }

    /**
     * Returns {@link ExternalLibrariesNode} of the project with the specified {@code projectPath}.
     *
     * @param projectPath
     *         the path of the project for which need to get {@link ExternalLibrariesNode}
     * @return an {@link ExternalLibrariesNode} of the project with the specified {@code projectPath}
     */
    public ExternalLibrariesNode getExternalLibrariesNode(@Nonnull String projectPath) {
        return librariesNodeMap.get(projectPath);
    }
}
