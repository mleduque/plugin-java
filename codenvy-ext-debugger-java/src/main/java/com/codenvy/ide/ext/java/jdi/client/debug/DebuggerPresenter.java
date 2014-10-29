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
package com.codenvy.ide.ext.java.jdi.client.debug;

import com.codenvy.api.project.gwt.client.ProjectServiceClient;
import com.codenvy.api.project.gwt.client.QueryExpression;
import com.codenvy.api.project.shared.dto.ItemReference;
import com.codenvy.api.project.shared.dto.ProjectDescriptor;
import com.codenvy.api.runner.dto.ApplicationProcessDescriptor;
import com.codenvy.api.runner.dto.RunOptions;
import com.codenvy.ide.api.editor.EditorAgent;
import com.codenvy.ide.api.editor.EditorPartPresenter;
import com.codenvy.ide.api.event.ProjectActionEvent;
import com.codenvy.ide.api.event.ProjectActionHandler;
import com.codenvy.ide.api.notification.Notification;
import com.codenvy.ide.api.notification.NotificationManager;
import com.codenvy.ide.api.parts.PartPresenter;
import com.codenvy.ide.api.parts.PartStackType;
import com.codenvy.ide.api.parts.WorkspaceAgent;
import com.codenvy.ide.api.parts.base.BasePresenter;
import com.codenvy.ide.api.projecttree.generic.FileNode;
import com.codenvy.ide.collections.Array;
import com.codenvy.ide.debug.Breakpoint;
import com.codenvy.ide.debug.BreakpointManager;
import com.codenvy.ide.debug.Debugger;
import com.codenvy.ide.dto.DtoFactory;
import com.codenvy.ide.ext.java.jdi.client.JavaRuntimeExtension;
import com.codenvy.ide.ext.java.jdi.client.JavaRuntimeLocalizationConstant;
import com.codenvy.ide.ext.java.jdi.client.debug.changevalue.ChangeValuePresenter;
import com.codenvy.ide.ext.java.jdi.client.debug.expression.EvaluateExpressionPresenter;
import com.codenvy.ide.ext.java.jdi.client.fqn.FqnResolver;
import com.codenvy.ide.ext.java.jdi.client.fqn.FqnResolverFactory;
import com.codenvy.ide.ext.java.jdi.client.marshaller.DebuggerEventListUnmarshallerWS;
import com.codenvy.ide.ext.java.jdi.shared.BreakPoint;
import com.codenvy.ide.ext.java.jdi.shared.BreakPointEvent;
import com.codenvy.ide.ext.java.jdi.shared.DebuggerEvent;
import com.codenvy.ide.ext.java.jdi.shared.DebuggerEventList;
import com.codenvy.ide.ext.java.jdi.shared.DebuggerInfo;
import com.codenvy.ide.ext.java.jdi.shared.Location;
import com.codenvy.ide.ext.java.jdi.shared.StackFrameDump;
import com.codenvy.ide.ext.java.jdi.shared.StepEvent;
import com.codenvy.ide.ext.java.jdi.shared.Value;
import com.codenvy.ide.ext.java.jdi.shared.Variable;
import com.codenvy.ide.extension.runner.client.ProjectRunCallback;
import com.codenvy.ide.extension.runner.client.run.RunController;
import com.codenvy.ide.rest.AsyncRequestCallback;
import com.codenvy.ide.rest.DtoUnmarshallerFactory;
import com.codenvy.ide.rest.HTTPStatus;
import com.codenvy.ide.util.loging.Log;
import com.codenvy.ide.websocket.MessageBus;
import com.codenvy.ide.websocket.WebSocketException;
import com.codenvy.ide.websocket.rest.SubscriptionHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.codenvy.ide.api.notification.Notification.Type.ERROR;
import static com.codenvy.ide.api.notification.Notification.Type.INFO;
import static com.codenvy.ide.api.notification.Notification.Type.WARNING;
import static com.codenvy.ide.ext.java.jdi.shared.DebuggerEvent.BREAKPOINT;
import static com.codenvy.ide.ext.java.jdi.shared.DebuggerEvent.STEP;

/**
 * The presenter provides debug java application.
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyy
 */
@Singleton
public class DebuggerPresenter extends BasePresenter implements DebuggerView.ActionDelegate, Debugger {
    private static final String TITLE = "Debug";
    private final DtoFactory                             dtoFactory;
    private final DtoUnmarshallerFactory                 dtoUnmarshallerFactory;
    private       ProjectServiceClient                   projectServiceClient;
    /** Channel identifier to receive events from debugger over WebSocket. */
    private       String                                 debuggerEventsChannel;
    /** Channel identifier to receive event when debugger will disconnected. */
    private       String                                 debuggerDisconnectedChannel;
    private       DebuggerView                           view;
    private       EventBus                               eventBus;
    private       RunController                          runController;
    private       DebuggerClientService                  service;
    private       JavaRuntimeLocalizationConstant        constant;
    private       DebuggerInfo                           debuggerInfo;
    private       MessageBus                             messageBus;
    private       BreakpointManager                      breakpointManager;
    private       WorkspaceAgent                         workspaceAgent;
    private       FqnResolverFactory                     resolverFactory;
    private       EditorAgent                            editorAgent;
    private       Variable                               selectedVariable;
    private       EvaluateExpressionPresenter            evaluateExpressionPresenter;
    private       ChangeValuePresenter                   changeValuePresenter;
    private       NotificationManager                    notificationManager;
    /** Handler for processing events which is received from debugger over WebSocket connection. */
    private       SubscriptionHandler<DebuggerEventList> debuggerEventsHandler;
    private       SubscriptionHandler<Void>              debuggerDisconnectedHandler;
    private       List<Variable>                         variables;
    private       ApplicationProcessDescriptor           appDescriptor;
    private       ProjectDescriptor                      project;
    private       Location                               location;

    /** Create presenter. */
    @Inject
    public DebuggerPresenter(DebuggerView view,
                             final DebuggerClientService service,
                             final EventBus eventBus,
                             final MessageBus messageBus,
                             final JavaRuntimeLocalizationConstant constant,
                             WorkspaceAgent workspaceAgent,
                             final BreakpointManager breakpointManager,
                             FqnResolverFactory resolverFactory,
                             EditorAgent editorAgent,
                             final EvaluateExpressionPresenter evaluateExpressionPresenter,
                             ChangeValuePresenter changeValuePresenter,
                             final NotificationManager notificationManager,
                             final RunController runController,
                             final DtoFactory dtoFactory,
                             DtoUnmarshallerFactory dtoUnmarshallerFactory,
                             ProjectServiceClient projectServiceClient) {
        this.view = view;
        this.eventBus = eventBus;
        this.runController = runController;
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.projectServiceClient = projectServiceClient;
        this.view.setDelegate(this);
        this.view.setTitle(TITLE);
        this.service = service;
        this.messageBus = messageBus;
        this.constant = constant;
        this.workspaceAgent = workspaceAgent;
        this.breakpointManager = breakpointManager;
        this.resolverFactory = resolverFactory;
        this.variables = new ArrayList<>();
        this.editorAgent = editorAgent;
        this.evaluateExpressionPresenter = evaluateExpressionPresenter;
        this.changeValuePresenter = changeValuePresenter;
        this.notificationManager = notificationManager;

        this.debuggerEventsHandler = new SubscriptionHandler<DebuggerEventList>(new DebuggerEventListUnmarshallerWS(dtoFactory)) {
            @Override
            public void onMessageReceived(DebuggerEventList result) {
                onEventListReceived(result);
            }

            @Override
            public void onErrorReceived(Throwable exception) {
                try {
                    messageBus.unsubscribe(debuggerEventsChannel, this);
                } catch (WebSocketException e) {
                    Log.error(DebuggerPresenter.class, e);
                }
                closeView();

                if (exception instanceof com.codenvy.ide.websocket.rest.exceptions.ServerException) {
                    com.codenvy.ide.websocket.rest.exceptions.ServerException serverException =
                            (com.codenvy.ide.websocket.rest.exceptions.ServerException)exception;
                    if (HTTPStatus.INTERNAL_ERROR == serverException.getHTTPStatus() && serverException.getMessage() != null
                        && serverException.getMessage().contains("not found")) {
                        runController.stopActiveProject(false);
                        onDebuggerDisconnected();
                        return;
                    }
                }
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
            }
        };

        this.debuggerDisconnectedHandler = new SubscriptionHandler<Void>() {
            @Override
            protected void onMessageReceived(Void result) {
                try {
                    messageBus.unsubscribe(debuggerDisconnectedChannel, this);
                } catch (WebSocketException e) {
                    Log.error(DebuggerPresenter.class, e);
                }

                evaluateExpressionPresenter.closeDialog();
                closeView();
                onDebuggerDisconnected();
            }

            @Override
            protected void onErrorReceived(Throwable exception) {
                try {
                    messageBus.unsubscribe(debuggerDisconnectedChannel, this);
                } catch (WebSocketException e) {
                    Log.error(DebuggerPresenter.class, e);
                }
            }
        };

        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                // do nothing
            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                // application will be stopped after closing a project
                if (debuggerInfo != null) {
                    changeButtonsEnableState(false);
                    onDebuggerDisconnected();
                    closeView();
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return TITLE;
    }

    /** {@inheritDoc} */
    @Override
    public ImageResource getTitleImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitleToolTip() {
        return "Debug";
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        view.setBreakpoints(breakpointManager.getBreakpointList());
        view.setVariables(variables);
        container.setWidget(view);
    }

    private void onEventListReceived(@Nonnull DebuggerEventList eventList) {
        if (eventList.getEvents().size() == 0) {
            return;
        }

        FileNode activeFile = null;
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor != null) {
            activeFile = activeEditor.getEditorInput().getFile();
        }
        Location location;
        List<DebuggerEvent> events = eventList.getEvents();
        for (DebuggerEvent event : events) {
            switch (event.getType()) {
                case STEP:
                    location = ((StepEvent)event).getLocation();
                    break;
                case BREAKPOINT:
                    location = ((BreakPointEvent)event).getBreakPoint().getLocation();
                    break;
                default:
                    Log.error(DebuggerPresenter.class, "Unknown type of debugger event: " + event.getType());
                    return;
            }
            this.location = location;

            final String filePath = resolveFilePathByLocation(location);
            if (activeFile == null || !filePath.equalsIgnoreCase(activeFile.getPath())) {
                final Location finalLocation = location;
                openFile(location, new AsyncCallback<ItemReference>() {
                    @Override
                    public void onSuccess(ItemReference result) {
                        if (result != null && filePath != null && filePath.equalsIgnoreCase(result.getPath())) {
                            breakpointManager.markCurrentBreakpoint(finalLocation.getLineNumber() - 1);
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Notification notification =
                                new Notification(constant.errorSourceNotFoundForClass(finalLocation.getClassName()), WARNING);
                        notificationManager.showNotification(notification);
                    }
                });
            } else {
                breakpointManager.markCurrentBreakpoint(location.getLineNumber() - 1);
            }
            getStackFrameDump();
            changeButtonsEnableState(true);
        }
    }

    /**
     * Create file path from {@link Location}.
     *
     * @param location
     *         location of class
     * @return file path
     */
    @Nonnull
    private String resolveFilePathByLocation(@Nonnull Location location) {
        final String sourcePath = "src/main/java";
        return project.getPath() + "/" + sourcePath + "/" + location.getClassName().replace(".", "/") + ".java";
    }

    private void openFile(@Nonnull Location location, final AsyncCallback<ItemReference> callback) {
        projectServiceClient.search(new QueryExpression().setPath(resolveFilePathByLocation(location)),
                                    new AsyncRequestCallback<Array<ItemReference>>(
                                            dtoUnmarshallerFactory.newArrayUnmarshaller(ItemReference.class)) {
                                        @Override
                                        protected void onSuccess(Array<ItemReference> result) {
                                            FileNode file = new FileNode(null, result.get(0), eventBus, projectServiceClient, null);
                                            editorAgent.openEditor(file);
                                            callback.onSuccess(result.get(0));
                                        }

                                        @Override
                                        protected void onFailure(Throwable exception) {
                                            callback.onFailure(exception);
                                        }
                                    });
    }

    private void getStackFrameDump() {
        service.getStackFrameDump(debuggerInfo.getId(),
                                  new AsyncRequestCallback<StackFrameDump>(dtoUnmarshallerFactory.newUnmarshaller(StackFrameDump.class)) {
                                      @Override
                                      protected void onSuccess(StackFrameDump result) {
                                          List<Variable> variables = new ArrayList<>();
                                          variables.addAll(result.getFields());
                                          variables.addAll(result.getLocalVariables());

                                          DebuggerPresenter.this.variables = variables;
                                          view.setVariables(variables);
                                          view.setVariablesInfo(variables.get(0).isExistInformation(), location);
                                      }

                                      @Override
                                      protected void onFailure(Throwable exception) {
                                          Notification notification = new Notification(exception.getMessage(), ERROR);
                                          notificationManager.showNotification(notification);
                                      }
                                  }
                                 );
    }

    /** Change enable state of all buttons (except Disconnect button) on Debugger panel. */
    private void changeButtonsEnableState(boolean isEnable) {
        view.setEnableResumeButton(isEnable);
        view.setEnableStepIntoButton(isEnable);
        view.setEnableStepOverButton(isEnable);
        view.setEnableStepReturnButton(isEnable);
        view.setEnableEvaluateExpressionButtonEnable(isEnable);
    }

    /** {@inheritDoc} */
    @Override
    public void onResumeButtonClicked() {
        changeButtonsEnableState(false);
        service.resume(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                resetStates();
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onRemoveAllBreakpointsButtonClicked() {
        service.deleteAllBreakpoints(debuggerInfo.getId(), new AsyncRequestCallback<String>() {
            @Override
            protected void onSuccess(String result) {
                breakpointManager.removeAllBreakpoints();
                view.setBreakpoints(new ArrayList<Breakpoint>());
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
            }

        });
    }

    /** {@inheritDoc} */
    @Override
    public void onDisconnectButtonClicked() {
        disconnectDebugger();
    }

    /** {@inheritDoc} */
    @Override
    public void onStepIntoButtonClicked() {
        if (!view.resetStepIntoButton(false)) return;
        service.stepInto(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                resetStates();
                view.resetStepIntoButton(true);
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
                view.resetStepIntoButton(true);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onStepOverButtonClicked() {
        if (!view.resetStepOverButton(false)) return;
        service.stepOver(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                resetStates();
                view.resetStepOverButton(true);
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
                view.resetStepOverButton(true);
            }

        });
    }

    /** {@inheritDoc} */
    @Override
    public void onStepReturnButtonClicked() {
        if (!view.resetStepReturnButton(false)) return;
        service.stepReturn(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                resetStates();
                view.resetStepReturnButton(true);
            }

            @Override
            protected void onFailure(Throwable exception) {
                Notification notification = new Notification(exception.getMessage(), ERROR);
                notificationManager.showNotification(notification);
                view.resetStepReturnButton(true);
            }

        });
    }

    /** {@inheritDoc} */
    @Override
    public void onChangeValueButtonClicked() {
        if (selectedVariable == null) {
            return;
        }

        changeValuePresenter.showDialog(debuggerInfo, selectedVariable, new AsyncCallback<String>() {
            @Override
            public void onSuccess(String s) {
                getStackFrameDump();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.error(DebuggerPresenter.class, throwable);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onEvaluateExpressionButtonClicked() {
        evaluateExpressionPresenter.showDialog(debuggerInfo);
    }

    /** {@inheritDoc} */
    @Override
    public void onExpandVariablesTree() {
        List<Variable> rootVariables = selectedVariable.getVariables();
        if (rootVariables.size() == 0) {
            service.getValue(debuggerInfo.getId(), selectedVariable,
                             new AsyncRequestCallback<Value>(dtoUnmarshallerFactory.newUnmarshaller(Value.class)) {
                                 @Override
                                 protected void onSuccess(Value result) {
                                     List<Variable> variables = result.getVariables();
                                     view.setVariablesIntoSelectedVariable(variables);
                                     view.updateSelectedVariable();
                                 }

                                 @Override
                                 protected void onFailure(Throwable exception) {
                                     Notification notification = new Notification(exception.getMessage(), ERROR);
                                     notificationManager.showNotification(notification);
                                 }
                             }
                            );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onSelectedVariableElement(@Nonnull Variable variable) {
        this.selectedVariable = variable;
        updateChangeValueButtonEnableState();
    }

    /** Update enable state for 'Change value' button. */
    private void updateChangeValueButtonEnableState() {
        view.setEnableChangeValueButtonEnable(selectedVariable != null);
    }

    private void resetStates() {
        variables.clear();
        view.setVariables(variables);
        selectedVariable = null;
        updateChangeValueButtonEnableState();
        breakpointManager.unmarkCurrentBreakpoint();
    }

    private void showDialog(@Nonnull DebuggerInfo debuggerInfo) {
        view.setVMName(debuggerInfo.getVmName() + " " + debuggerInfo.getVmVersion());
        selectedVariable = null;
        updateChangeValueButtonEnableState();
        changeButtonsEnableState(false);
        view.setEnableRemoveAllBreakpointsButton(true);
        view.setEnableDisconnectButton(true);

        workspaceAgent.openPart(this, PartStackType.INFORMATION);
        PartPresenter activePart = partStack.getActivePart();
        if (activePart != null && !activePart.equals(this)) {
            partStack.setActivePart(this);
        }
    }

    private void closeView() {
        variables.clear();
        view.setEnableRemoveAllBreakpointsButton(false);
        view.setEnableDisconnectButton(false);
        workspaceAgent.removePart(this);
    }

    /**
     * Debug active project.
     *
     * @param isUserAction
     *         points whether the build is started directly by user interaction
     * @param callback
     *         callback that will be notified when project will be run
     */
    public void debug(final boolean isUserAction, final ProjectRunCallback callback) {
        RunOptions runOptions = dtoFactory.createDto(RunOptions.class);
        runOptions.setInDebugMode(true);

        runController.runActiveProject(runOptions, callback, isUserAction);
    }

    /**
     * Attach debugger to the specified app.
     *
     * @param appDescriptor
     *         descriptor of application to debug
     * @param project
     *         project to debug
     */
    public void attachDebugger(@Nonnull final ApplicationProcessDescriptor appDescriptor, ProjectDescriptor project) {
        this.project = project;
        this.appDescriptor = appDescriptor;
        service.connect(appDescriptor.getDebugHost(), appDescriptor.getDebugPort(),
                        new AsyncRequestCallback<DebuggerInfo>(dtoUnmarshallerFactory.newUnmarshaller(DebuggerInfo.class)) {
                            @Override
                            public void onSuccess(DebuggerInfo result) {
                                debuggerInfo = result;
                                Notification notification =
                                        new Notification(appDescriptor.getDebugHost() + ':' + appDescriptor.getDebugPort(), INFO);
                                notificationManager.showNotification(notification);
                                showDialog(debuggerInfo);
                                startCheckingEvents();
                            }

                            @Override
                            protected void onFailure(Throwable exception) {
                                Notification notification = new Notification(exception.getMessage(), ERROR);
                                notificationManager.showNotification(notification);
                            }
                        }
                       );
    }

    private void disconnectDebugger() {
        if (debuggerInfo != null) {
            stopCheckingDebugEvents();
            service.disconnect(debuggerInfo.getId(), new AsyncRequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    changeButtonsEnableState(false);
                    runController.stopActiveProject(false);
                    onDebuggerDisconnected();
                    closeView();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    Notification notification = new Notification(exception.getMessage(), ERROR);
                    notificationManager.showNotification(notification);
                }
            });
        } else {
            changeButtonsEnableState(false);
            breakpointManager.unmarkCurrentBreakpoint();
        }
    }

    private void startCheckingEvents() {
        debuggerEventsChannel = JavaRuntimeExtension.EVENTS_CHANNEL + debuggerInfo.getId();
        try {
            messageBus.subscribe(debuggerEventsChannel, debuggerEventsHandler);
        } catch (WebSocketException e) {
            Log.error(DebuggerPresenter.class, e);
        }

        try {
            debuggerDisconnectedChannel = JavaRuntimeExtension.DISCONNECT_CHANNEL + debuggerInfo.getId();
            messageBus.subscribe(debuggerDisconnectedChannel, debuggerDisconnectedHandler);
        } catch (WebSocketException e) {
            Log.error(DebuggerPresenter.class, e);
        }
    }

    private void stopCheckingDebugEvents() {
        try {
            if (messageBus.isHandlerSubscribed(debuggerEventsHandler, debuggerEventsChannel)) {
                messageBus.unsubscribe(debuggerEventsChannel, debuggerEventsHandler);
            }

            if (messageBus.isHandlerSubscribed(debuggerDisconnectedHandler, debuggerDisconnectedChannel)) {
                messageBus.unsubscribe(debuggerDisconnectedChannel, debuggerDisconnectedHandler);
            }
        } catch (WebSocketException e) {
            Log.error(DebuggerPresenter.class, e);
        }
    }

    /** Perform some action after disconnecting a debugger. */
    private void onDebuggerDisconnected() {
        debuggerInfo = null;
        breakpointManager.unmarkCurrentBreakpoint();
        breakpointManager.removeAllBreakpoints();
        Notification notification =
                new Notification(constant.debuggerDisconnected(appDescriptor.getDebugHost()) + ':' + appDescriptor.getDebugPort(), INFO);
        notificationManager.showNotification(notification);
        appDescriptor = null;
    }

    private void updateBreakPoints() {
        view.setBreakpoints(breakpointManager.getBreakpointList());
    }

    /** {@inheritDoc} */
    @Override
    public void addBreakpoint(@Nonnull final FileNode file, final int lineNumber, final AsyncCallback<Breakpoint> callback) {
        if (debuggerInfo != null) {
            Location location = dtoFactory.createDto(Location.class);
            location.setLineNumber(lineNumber + 1);
            final FqnResolver resolver = resolverFactory.getResolver(file.getData().getMediaType());
            if (resolver != null) {
                location.setClassName(resolver.resolveFqn(file));
            } else {
                Log.warn(DebuggerPresenter.class, "FqnResolver is not found");
            }

            BreakPoint breakPoint = dtoFactory.createDto(BreakPoint.class);
            breakPoint.setLocation(location);
            breakPoint.setEnabled(true);
            service.addBreakpoint(debuggerInfo.getId(), breakPoint, new AsyncRequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    if (resolver != null) {
                        final String fqn = resolver.resolveFqn(file);
                        Breakpoint breakpoint = new Breakpoint(Breakpoint.Type.BREAKPOINT, lineNumber, fqn, file);
                        callback.onSuccess(breakpoint);
                    }
                    updateBreakPoints();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    callback.onFailure(exception);
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteBreakpoint(@Nonnull FileNode file, int lineNumber, final AsyncCallback<Void> callback) {
        if (debuggerInfo != null) {
            Location location = dtoFactory.createDto(Location.class);
            location.setLineNumber(lineNumber + 1);
            FqnResolver resolver = resolverFactory.getResolver(file.getData().getMediaType());
            if (resolver != null) {
                location.setClassName(resolver.resolveFqn(file));
            } else {
                Log.warn(DebuggerPresenter.class, "FqnResolver is not found");
            }

            BreakPoint point = dtoFactory.createDto(BreakPoint.class);
            point.setLocation(location);
            point.setEnabled(true);

            service.deleteBreakpoint(debuggerInfo.getId(), point, new AsyncRequestCallback<Void>() {
                @Override
                protected void onSuccess(Void result) {
                    callback.onSuccess(null);
                    updateBreakPoints();
                }

                @Override
                protected void onFailure(Throwable exception) {
                    callback.onFailure(exception);
                }
            });
        }
    }
}