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
package com.codenvy.ide.maven.tools;

/**
 * @author andrew00x
 */
public class MavenArtifact {
    private String groupId;
    private  String artifactId;
    private  String type;
    private  String classifier;
    private  String version;
    private  String scope;

    public MavenArtifact(String groupId, String artifactId, String type, String classifier, String version, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.classifier = classifier;
        this.version = version;
        this.scope = scope;
    }

    public MavenArtifact() {
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "MavenArtifact{" +
               "groupId='" + groupId + '\'' +
               ", artifactId='" + artifactId + '\'' +
               ", type='" + type + '\'' +
               ", classifier='" + classifier + '\'' +
               ", version='" + version + '\'' +
               ", scope='" + scope + '\'' +
               '}';
    }
}
