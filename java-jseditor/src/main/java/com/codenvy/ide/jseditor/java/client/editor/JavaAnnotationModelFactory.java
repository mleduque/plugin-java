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

import com.codenvy.ide.jseditor.client.partition.DocumentPositionMap;
import com.codenvy.ide.jseditor.client.texteditor.EmbeddedTextEditorPresenter;

public interface JavaAnnotationModelFactory {
    JavaAnnotationModel create(EmbeddedTextEditorPresenter editor, DocumentPositionMap docPositionMap);
}
