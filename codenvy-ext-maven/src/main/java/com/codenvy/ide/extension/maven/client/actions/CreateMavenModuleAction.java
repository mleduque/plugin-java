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
package com.codenvy.ide.extension.maven.client.actions;

import com.codenvy.api.analytics.client.logger.AnalyticsEventLogger;
import com.codenvy.ide.api.action.ActionEvent;
import com.codenvy.ide.api.action.ProjectAction;
import com.codenvy.ide.api.app.AppContext;
import com.codenvy.ide.extension.maven.client.MavenLocalizationConstant;
import com.codenvy.ide.extension.maven.client.module.CreateMavenModulePresenter;
import com.codenvy.ide.extension.maven.shared.MavenAttributes;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/** @author Evgen Vidolob */
@Singleton
public class CreateMavenModuleAction extends ProjectAction {

    private CreateMavenModulePresenter presenter;

    private final AnalyticsEventLogger eventLogger;

    @Inject
    public CreateMavenModuleAction(MavenLocalizationConstant constant, CreateMavenModulePresenter presenter, AnalyticsEventLogger eventLogger) {
        super(constant.actionCreateMavenModuleText(), constant.actionCreateMavenModuleDescription());
        this.presenter = presenter;
        this.eventLogger = eventLogger;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);
        if (appContext.getCurrentProject() != null) {
            presenter.showDialog(appContext.getCurrentProject());
        }
    }

    @Override
    public void updateProjectAction(ActionEvent e) {
        e.getPresentation().setVisible(true);
        e.getPresentation().setEnabled("pom".equals(appContext.getCurrentProject().getAttributeValue(MavenAttributes.PACKAGING)));
    }
}
