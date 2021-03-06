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
package com.codenvy.ide.ext.java;

import com.codenvy.ide.ext.java.server.JavaNavigation;
import com.codenvy.ide.ext.java.server.SourcesFromBytecodeGenerator;
import com.codenvy.ide.ext.java.server.internal.core.JavaProject;
import com.codenvy.ide.ext.java.shared.OpenDeclarationDescriptor;

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Evgen Vidolob
 */
public class FindDeclarationTest extends BaseTest {

    private JavaNavigation navigation = new JavaNavigation(new SourcesFromBytecodeGenerator());

    @Test
    public void testFindClassIsNotNullOrEmpty() throws Exception {
        OpenDeclarationDescriptor declaration = navigation.findDeclaration(project, "Ljava/lang/String;");
        assertThat(declaration).isNotNull();
    }

    @Test
    public void testFindClassShouldReturnBinaryPath() throws Exception {
        OpenDeclarationDescriptor declaration = navigation.findDeclaration(project, "Ljava/lang/String;");
        assertThat(declaration).isNotNull();
        assertThat(declaration.getOffset()).isNotNull();
        assertThat(declaration.getLength()).isNotNull();
        assertThat(declaration.isBinary()).isTrue();
        assertThat(declaration.getPath()).isEqualTo("java.lang.String");
    }

    @Test
    public void testFindClassShouldReturnSourcePath() throws Exception {
        OpenDeclarationDescriptor declaration = navigation.findDeclaration(project, "Lcom/codenvy/test/MyClass;");
        assertThat(declaration).isNotNull();
        assertThat(declaration.isBinary()).isFalse();
        assertThat(declaration.getPath()).isEqualTo("/test/src/main/java/com/codenvy/test/MyClass.java");
    }

    @Test
    public void testFindClassShouldReturnSourcePathInMavenModule() throws Exception {
        JavaProject project = new JavaProject(new File(getClass().getResource("/projects").getFile()), "/multimoduleProject/test", getClass().getResource("/temp").getPath(), "ws", options);
        OpenDeclarationDescriptor declaration = navigation.findDeclaration(project, "Lcom/codenvy/test/MyClass;");
        assertThat(declaration).isNotNull();
        assertThat(declaration.isBinary()).isFalse();
        assertThat(declaration.getPath()).isEqualTo("/multimoduleProject/test/src/main/java/com/codenvy/test/MyClass.java");
    }

    @Test
    public void findClassShouldReturnSourcePathInMavenModule() throws Exception {
        JavaProject project = new JavaProject(new File(getClass().getResource("/projects").getFile()), "/multimoduleProject/test", getClass().getResource("/temp").getPath(), "ws", options);
        OpenDeclarationDescriptor declaration = navigation.findDeclaration(project, "Lcom/codenvy/sub/ClassInSubModule;");
        assertThat(declaration).isNotNull();
        assertThat(declaration.isBinary()).isFalse();
        assertThat(declaration.getPath()).isEqualTo("/multimoduleProject/subModule/src/main/java/com/codenvy/sub/ClassInSubModule.java");
    }
}
