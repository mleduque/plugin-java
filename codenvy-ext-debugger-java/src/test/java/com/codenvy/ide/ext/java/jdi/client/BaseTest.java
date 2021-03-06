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
package com.codenvy.ide.ext.java.jdi.client;

import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.parts.PartStack;
import com.codenvy.ide.api.parts.WorkspaceAgent;
import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.ext.java.jdi.client.debug.DebuggerServiceClient;
import com.codenvy.ide.ext.java.jdi.shared.DebuggerInfo;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.websocket.MessageBus;
import com.google.web.bindery.event.shared.EventBus;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

/**
 * Base test for java debugger extension.
 *
 * @author Artem Zatsarynnyy
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BaseTest {
    public static final String  DEBUGGER_ID    = "debugger_id";
    public static final boolean DISABLE_BUTTON = false;
    @Mock
    protected DebuggerServiceClient           service;
    @Mock
    protected DebuggerInfo                    debuggerInfo;
    @Mock
    protected JavaRuntimeLocalizationConstant constants;
    @Mock
    protected NotificationManager             notificationManager;
    @Mock
    protected EventBus                        eventBus;
    @Mock
    protected MessageBus                      messageBus;
    @Mock
    protected DtoFactory                      dtoFactory;
    @Mock
    protected DtoUnmarshallerFactory          dtoUnmarshallerFactory;
    @Mock
    protected WorkspaceAgent                  workspaceAgent;
    @Mock
    protected PartStack                       partStack;

    @Before
    public void setUp() {
        when(debuggerInfo.getId()).thenReturn(DEBUGGER_ID);
    }
}
