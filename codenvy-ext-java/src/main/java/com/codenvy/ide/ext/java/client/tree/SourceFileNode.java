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
package com.codenvy.ide.ext.java.client.tree;

import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.ide.api.projecttree.AbstractTreeNode;
import com.codenvy.ide.api.projecttree.generic.FileNode;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Node that represents a java source file (class, interface, enum).
 *
 * @author Artem Zatsarynnyy
 */
public class SourceFileNode extends FileNode {
    public SourceFileNode(AbstractTreeNode parent, ItemReference data, EventBus eventBus) {
        super(parent, data, eventBus);

        final String name = data.getName();
        // display name without '.java' extension
        getPresentation().setDisplayName(name.substring(0, name.length() - "java".length() - 1));
    }
}