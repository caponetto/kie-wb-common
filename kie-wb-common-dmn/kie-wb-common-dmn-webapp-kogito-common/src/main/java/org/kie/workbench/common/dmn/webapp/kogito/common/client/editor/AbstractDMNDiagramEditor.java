/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.common.dmn.webapp.kogito.common.client.editor;

import java.util.Optional;
import java.util.function.Supplier;

import javax.enterprise.event.Event;

import com.google.gwt.user.client.ui.IsWidget;
import elemental2.dom.HTMLElement;
import elemental2.promise.Promise;
import org.jboss.errai.common.client.ui.ElementWrapperWidget;
import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.kie.workbench.common.dmn.client.commands.general.NavigateToExpressionEditorCommand;
import org.kie.workbench.common.dmn.client.docks.navigator.DecisionNavigatorDock;
import org.kie.workbench.common.dmn.client.editors.drd.DRDNameChanger;
import org.kie.workbench.common.dmn.client.editors.expressions.ExpressionEditorView;
import org.kie.workbench.common.dmn.client.editors.included.IncludedModelsPage;
import org.kie.workbench.common.dmn.client.editors.included.common.IncludedModelsContext;
import org.kie.workbench.common.dmn.client.editors.search.DMNEditorSearchIndex;
import org.kie.workbench.common.dmn.client.editors.search.DMNSearchableElement;
import org.kie.workbench.common.dmn.client.editors.types.DataTypePageTabActiveEvent;
import org.kie.workbench.common.dmn.client.editors.types.DataTypesPage;
import org.kie.workbench.common.dmn.client.editors.types.listview.common.DataTypeEditModeToggleEvent;
import org.kie.workbench.common.dmn.client.events.EditExpressionEvent;
import org.kie.workbench.common.dmn.client.session.DMNSession;
import org.kie.workbench.common.dmn.client.widgets.codecompletion.MonacoFEELInitializer;
import org.kie.workbench.common.dmn.webapp.common.client.docks.preview.PreviewDiagramDock;
import org.kie.workbench.common.dmn.webapp.kogito.common.client.tour.GuidedTourBridgeInitializer;
import org.kie.workbench.common.kogito.client.editor.MultiPageEditorContainerView;
import org.kie.workbench.common.stunner.client.widgets.presenters.Viewer;
import org.kie.workbench.common.stunner.client.widgets.presenters.session.impl.SessionEditorPresenter;
import org.kie.workbench.common.stunner.client.widgets.presenters.session.impl.SessionViewerPresenter;
import org.kie.workbench.common.stunner.core.client.api.SessionManager;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.CanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.util.CanvasFileExport;
import org.kie.workbench.common.stunner.core.client.command.SessionCommandManager;
import org.kie.workbench.common.stunner.core.client.components.layout.LayoutHelper;
import org.kie.workbench.common.stunner.core.client.components.layout.OpenDiagramLayoutExecutor;
import org.kie.workbench.common.stunner.core.client.error.DiagramClientErrorHandler;
import org.kie.workbench.common.stunner.core.client.i18n.ClientTranslationService;
import org.kie.workbench.common.stunner.core.client.service.ClientRuntimeError;
import org.kie.workbench.common.stunner.core.client.service.ServiceCallback;
import org.kie.workbench.common.stunner.core.client.session.impl.EditorSession;
import org.kie.workbench.common.stunner.core.client.session.impl.ViewerSession;
import org.kie.workbench.common.stunner.core.diagram.Diagram;
import org.kie.workbench.common.stunner.core.documentation.DocumentationView;
import org.kie.workbench.common.stunner.forms.client.event.RefreshFormPropertiesEvent;
import org.kie.workbench.common.stunner.kogito.client.docks.DiagramEditorPropertiesDock;
import org.kie.workbench.common.stunner.kogito.client.editor.AbstractDiagramEditor;
import org.kie.workbench.common.stunner.kogito.client.editor.event.OnDiagramFocusEvent;
import org.kie.workbench.common.stunner.kogito.client.service.KogitoClientDiagramService;
import org.kie.workbench.common.stunner.kogito.client.session.EditorSessionCommands;
import org.kie.workbench.common.widgets.client.search.component.SearchBarComponent;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.client.promise.Promises;
import org.uberfire.client.views.pfly.multipage.MultiPageEditorSelectedPageEvent;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.workbench.events.NotificationEvent;

public abstract class AbstractDMNDiagramEditor extends AbstractDiagramEditor {

    public static final String EDITOR_ID = "DMNDiagramEditor";

    //Editor tabs: [0] Main editor, [1] Documentation, [2] Data-Types
    public static final int DATA_TYPES_PAGE_INDEX = 2;

    protected final SessionManager sessionManager;
    protected final SessionCommandManager<AbstractCanvasHandler> sessionCommandManager;
    protected final Event<RefreshFormPropertiesEvent> refreshFormPropertiesEvent;
    protected final DecisionNavigatorDock decisionNavigatorDock;
    protected final DiagramEditorPropertiesDock diagramPropertiesDock;
    protected final PreviewDiagramDock diagramPreviewAndExplorerDock;
    protected final LayoutHelper layoutHelper;
    protected final OpenDiagramLayoutExecutor openDiagramLayoutExecutor;
    protected final DataTypesPage dataTypesPage;
    protected final DMNEditorSearchIndex editorSearchIndex;
    protected final SearchBarComponent<DMNSearchableElement> searchBarComponent;
    protected final KogitoClientDiagramService diagramServices;
    protected final MonacoFEELInitializer feelInitializer;
    protected final CanvasFileExport canvasFileExport;
    protected final Promises promises;
    protected final IncludedModelsPage includedModelsPage;
    protected final IncludedModelsContext includedModelContext;
    protected final GuidedTourBridgeInitializer guidedTourBridgeInitializer;
    protected final DRDNameChanger drdNameChanger;

    public AbstractDMNDiagramEditor(final View view,
                                    final PlaceManager placeManager,
                                    final MultiPageEditorContainerView multiPageEditorContainerView,
                                    final Event<NotificationEvent> notificationEvent,
                                    final Event<OnDiagramFocusEvent> onDiagramFocusEvent,
                                    final ManagedInstance<SessionEditorPresenter<EditorSession>> editorSessionPresenterInstances,
                                    final ManagedInstance<SessionViewerPresenter<ViewerSession>> viewerSessionPresenterInstances,
                                    final DiagramClientErrorHandler diagramClientErrorHandler,
                                    final ClientTranslationService translationService,
                                    final DocumentationView<Diagram> documentationView,
                                    final DMNEditorSearchIndex editorSearchIndex,
                                    final SearchBarComponent<DMNSearchableElement> searchBarComponent,
                                    final SessionManager sessionManager,
                                    final SessionCommandManager<AbstractCanvasHandler> sessionCommandManager,
                                    final Event<RefreshFormPropertiesEvent> refreshFormPropertiesEvent,
                                    final DecisionNavigatorDock decisionNavigatorDock,
                                    final DiagramEditorPropertiesDock diagramPropertiesDock,
                                    final PreviewDiagramDock diagramPreviewAndExplorerDock,
                                    final LayoutHelper layoutHelper,
                                    final OpenDiagramLayoutExecutor openDiagramLayoutExecutor,
                                    final DataTypesPage dataTypesPage,
                                    final KogitoClientDiagramService diagramServices,
                                    final MonacoFEELInitializer feelInitializer,
                                    final CanvasFileExport canvasFileExport,
                                    final Promises promises,
                                    final IncludedModelsPage includedModelsPage,
                                    final IncludedModelsContext includedModelContext,
                                    final GuidedTourBridgeInitializer guidedTourBridgeInitializer,
                                    final DRDNameChanger drdNameChanger,
                                    final EditorSessionCommands editorSessionCommands) {
        super(view,
              placeManager,
              multiPageEditorContainerView,
              notificationEvent,
              onDiagramFocusEvent,
              editorSessionPresenterInstances,
              viewerSessionPresenterInstances,
              diagramClientErrorHandler,
              translationService,
              documentationView,
              editorSessionCommands);
        this.sessionManager = sessionManager;
        this.sessionCommandManager = sessionCommandManager;
        this.refreshFormPropertiesEvent = refreshFormPropertiesEvent;
        this.decisionNavigatorDock = decisionNavigatorDock;
        this.diagramPropertiesDock = diagramPropertiesDock;
        this.diagramPreviewAndExplorerDock = diagramPreviewAndExplorerDock;
        this.layoutHelper = layoutHelper;
        this.openDiagramLayoutExecutor = openDiagramLayoutExecutor;
        this.dataTypesPage = dataTypesPage;
        this.editorSearchIndex = editorSearchIndex;
        this.searchBarComponent = searchBarComponent;
        this.diagramServices = diagramServices;
        this.feelInitializer = feelInitializer;
        this.canvasFileExport = canvasFileExport;
        this.promises = promises;
        this.includedModelsPage = includedModelsPage;
        this.includedModelContext = includedModelContext;
        this.guidedTourBridgeInitializer = guidedTourBridgeInitializer;
        this.drdNameChanger = drdNameChanger;
    }

    public void onStartup(final PlaceRequest place) {
        superDoStartUp(place);

        decisionNavigatorDock.init();
        diagramPropertiesDock.init();
        diagramPreviewAndExplorerDock.init();
        guidedTourBridgeInitializer.init();
    }

    void superDoStartUp(final PlaceRequest place) {
        super.doStartUp(place);
    }

    @Override
    public void initialiseKieEditorForSession(final Diagram diagram) {
        superInitialiseKieEditorForSession(diagram);

        getWidget().getMultiPage().addPage(dataTypesPage);
        if (includedModelContext.isIncludedModelChannel()) {
            getWidget().getMultiPage().addPage(includedModelsPage);
        }
        setupEditorSearchIndex();
        setupSearchComponent();
    }

    private void setupSessionHeaderContainer() {
        Optional.ofNullable(getSessionPresenter()).ifPresent(s -> {
            drdNameChanger.setSessionPresenterView(s.getView());
            s.getView().setSessionHeaderContainer(getWidget(drdNameChanger.getElement()));
        });
    }

    private void setupEditorSearchIndex() {
        editorSearchIndex.setCurrentAssetHashcodeSupplier(getHashcodeSupplier());
        editorSearchIndex.setIsDataTypesTabActiveSupplier(getIsDataTypesTabActiveSupplier());
    }

    Supplier<Integer> getHashcodeSupplier() {
        return this::getCurrentDiagramHash;
    }

    Supplier<Boolean> getIsDataTypesTabActiveSupplier() {
        return () -> {
            final int selectedPageIndex = getWidget().getMultiPage().selectedPage();
            return selectedPageIndex == DATA_TYPES_PAGE_INDEX;
        };
    }

    void setupSearchComponent() {
        final HTMLElement element = searchBarComponent.getView().getElement();

        searchBarComponent.init(editorSearchIndex);
        getWidget().getMultiPage().addTabBarWidget(getWidget(element));
    }

    protected ElementWrapperWidget<?> getWidget(final HTMLElement element) {
        return ElementWrapperWidget.getWidget(element);
    }

    @SuppressWarnings("unused")
    public void onDataTypePageNavTabActiveEvent(final DataTypePageTabActiveEvent event) {
        getWidget().getMultiPage().selectPage(DATA_TYPES_PAGE_INDEX);
    }

    void superInitialiseKieEditorForSession(final Diagram diagram) {
        super.initialiseKieEditorForSession(diagram);
    }

    @Override
    public void open(final Diagram diagram,
                     final Viewer.Callback callback) {
        this.layoutHelper.applyLayout(diagram, openDiagramLayoutExecutor);
        feelInitializer.initializeFEELEditor();
        super.open(diagram, new Viewer.Callback() {
            @Override
            public void onSuccess() {
                setupSessionHeaderContainer();
                callback.onSuccess();
            }

            @Override
            public void onError(ClientRuntimeError error) {
                callback.onError(error);
            }
        });
    }

    public void onOpen() {
        super.doOpen();
    }

    public void onClose() {
        superOnClose();

        decisionNavigatorDock.destroy();
        decisionNavigatorDock.resetContent();

        diagramPropertiesDock.destroy();
        diagramPreviewAndExplorerDock.destroy();

        dataTypesPage.disableShortcuts();
    }

    void superOnClose() {
        super.doClose();
    }

    @Override
    public IsWidget asWidget() {
        return super.asWidget();
    }

    @Override
    public String getEditorIdentifier() {
        return EDITOR_ID;
    }

    public void onDataTypeEditModeToggle(final DataTypeEditModeToggleEvent event) {
    }

    protected void onMultiPageEditorSelectedPageEvent(final MultiPageEditorSelectedPageEvent event) {
        searchBarComponent.disableSearch();
    }

    protected void onRefreshFormPropertiesEvent(final RefreshFormPropertiesEvent event) {
        searchBarComponent.disableSearch();
    }

    protected void onEditExpressionEvent(final EditExpressionEvent event) {
        searchBarComponent.disableSearch();
        if (isSameSession(event.getSession())) {
            final DMNSession session = sessionManager.getCurrentSession();
            final ExpressionEditorView.Presenter expressionEditor = session.getExpressionEditor();
            sessionCommandManager.execute(session.getCanvasHandler(),
                                          new NavigateToExpressionEditorCommand(expressionEditor,
                                                                                getSessionPresenter(),
                                                                                sessionManager,
                                                                                sessionCommandManager,
                                                                                refreshFormPropertiesEvent,
                                                                                event.getNodeUUID(),
                                                                                event.getHasExpression(),
                                                                                event.getHasName(),
                                                                                event.isOnlyVisualChangeAllowed()));
        }
    }

    @Override
    public Promise getContent() {
        return diagramServices.transform(getEditor().getEditorProxy().getContentSupplier().get());
    }

    @Override
    public Promise setContent(final String path,
                              final String value) {
        Promise promise =
                promises.create((success, failure) -> {
                    superOnClose();
                    diagramServices.transform(path,
                                              value,
                                              new ServiceCallback<Diagram>() {

                                                  @Override
                                                  public void onSuccess(final Diagram diagram) {
                                                      AbstractDMNDiagramEditor.this.open(diagram,
                                                                                         new Viewer.Callback() {
                                                                                             @Override
                                                                                             public void onSuccess() {
                                                                                                 success.onInvoke((Object) null);
                                                                                             }

                                                                                             @Override
                                                                                             public void onError(ClientRuntimeError error) {
                                                                                                 AbstractDMNDiagramEditor.this.getEditor().onLoadError(error);
                                                                                                 failure.onInvoke(error);
                                                                                             }
                                                                                         });
                                                  }

                                                  @Override
                                                  public void onError(final ClientRuntimeError error) {
                                                      AbstractDMNDiagramEditor.this.getEditor().onLoadError(error);
                                                      failure.onInvoke(error);
                                                  }
                                              });
                });

        return promise;
    }

    public Promise getPreview() {
        final CanvasHandler canvasHandler = getCanvasHandler();
        if (canvasHandler != null) {
            return Promise.resolve(canvasFileExport.exportToSvg((AbstractCanvasHandler) canvasHandler));
        } else {
            return Promise.resolve("");
        }
    }
}
