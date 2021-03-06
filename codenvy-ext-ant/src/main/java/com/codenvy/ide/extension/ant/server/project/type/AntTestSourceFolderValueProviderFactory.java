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
package com.codenvy.ide.extension.ant.server.project.type;

import com.codenvy.api.project.server.InvalidValueException;
import com.codenvy.api.project.server.Project;
import com.codenvy.api.project.server.ValueProvider;
import com.codenvy.api.project.server.ValueStorageException;
import com.codenvy.ide.extension.ant.shared.AntAttributes;
import com.google.inject.Singleton;

import java.util.Arrays;
import java.util.List;

/** @author Vladyslav Zhukovskii */
@Singleton
public class AntTestSourceFolderValueProviderFactory extends AbstractAntValueProviderFactory {
    /** {@inheritDoc} */
    @Override
    public String getName() {
        return AntAttributes.TEST_SOURCE_FOLDER;
    }

    /** {@inheritDoc} */
    @Override
    public ValueProvider newInstance(Project project) {
        return new AntValueProvider(project) {
            @Override
            public List<String> getValues(org.apache.tools.ant.Project antProject) {
                String testDir = antProject.getProperty("test.dir");
                if (testDir == null) {
                    testDir = AntAttributes.DEF_TEST_SRC_PATH;
                }
                return Arrays.asList(testDir);
            }

            @Override
            public void setValues(List<String> value) throws ValueStorageException, InvalidValueException {
            }
        };
    }
}
