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
package com.codenvy.ide.extension.maven.client.projecttree;

import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.ide.api.icon.IconRegistry;
import com.codenvy.ide.api.projecttree.TreeNode;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.extension.maven.client.event.BeforeModuleOpenEvent;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Node that represents module of multi-module project.
 *
 * @author Artem Zatsarynnyy
 */
public class ModuleNode extends MavenProjectNode {

    @AssistedInject
    public ModuleNode(@Assisted TreeNode<?> parent,
                      @Assisted ProjectDescriptor data,
                      @Assisted MavenProjectTreeStructure treeStructure,
                      EventBus eventBus,
                      ProjectServiceClient projectServiceClient,
                      DtoUnmarshallerFactory dtoUnmarshallerFactory,
                      IconRegistry iconRegistry) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);
        setDisplayIcon(iconRegistry.getIcon("maven.module").getSVGImage());
    }

    @Override
    protected void getChildren(String path, AsyncCallback<Array<ItemReference>> callback) {
        if (!isOpened()) {
            eventBus.fireEvent(new BeforeModuleOpenEvent(this));
        }
        super.getChildren(path, callback);
    }
}
