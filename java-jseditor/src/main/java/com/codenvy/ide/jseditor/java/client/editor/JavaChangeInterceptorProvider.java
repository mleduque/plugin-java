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

import com.codenvy.ide.ext.java.jdt.JavaPartitions;
import com.codenvy.ide.jseditor.client.changeintercept.ChangeInterceptorProvider;
import com.codenvy.ide.jseditor.client.changeintercept.CloseCStyleCommentChangeInterceptor;
import com.codenvy.ide.jseditor.client.changeintercept.TextChangeInterceptor;

/** Provider for {@link TextChangeInterceptor}s for java. */
public class JavaChangeInterceptorProvider implements ChangeInterceptorProvider {
    private final Map<String, List<TextChangeInterceptor>> interceptors = new HashMap<>();

    public JavaChangeInterceptorProvider() {
        // doesn't really need a map but more interceptors should appear
        List<TextChangeInterceptor> defaultTypeInterceptors = new ArrayList<>();

        defaultTypeInterceptors.add(new CloseCStyleCommentChangeInterceptor());
        interceptors.put(JavaPartitions.JAVA_DOC, defaultTypeInterceptors);
    }

    @Override
    public List<TextChangeInterceptor> getInterceptors(final String contentType) {
        return this.interceptors.get(contentType);
    }

}
