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
package org.kie.workbench.common.kogito.client.editor;

import java.util.Objects;
import java.util.function.Supplier;

import elemental2.promise.Promise;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.ext.editor.commons.client.BaseEditorView;
import org.uberfire.mvp.PlaceRequest;

/**
 * This is a trimmed down {@code org.uberfire.ext.editor.commons.client.BaseEditor} for Kogito.
 * @param <CONTENT> The domain model of the editor
 */
public abstract class BaseKogitoEditor<CONTENT> {

    private boolean isReadOnly;

    private BaseEditorView baseEditorView;
    private PlaceManager placeManager;
    private PlaceRequest place;
    private Integer originalHash;

    protected BaseKogitoEditor() {
        //CDI proxy
    }

    protected BaseKogitoEditor(final BaseEditorView baseView,
                               final PlaceManager placeManager) {
        this.baseEditorView = baseView;
        this.placeManager = placeManager;
    }

    protected void init(final PlaceRequest place) {
        this.place = place;
        this.isReadOnly = this.place.getParameter("readOnly", null) != null;
    }

    protected PlaceRequest getPlaceRequest() {
        return place;
    }

    protected PlaceManager getPlaceManager() {
        return placeManager;
    }

    protected BaseEditorView getBaseEditorView() {
        return baseEditorView;
    }

    protected Supplier<CONTENT> getContentSupplier() {
        return () -> null;
    }

    public void setOriginalContentHash(final Integer originalHash) {
        this.originalHash = originalHash;
    }

    protected Integer getOriginalContentHash() {
        return originalHash;
    }

    protected Integer getCurrentContentHash() {
        try {
            return getContentSupplier().get().hashCode();
        } catch (final Exception e) {
            return null;
        }
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean mayClose() {
        return !isDirty() || baseEditorView.confirmClose();
    }

    /**
     * Used by Kogito to determine whether the content has unsaved changes.
     * @return true if there are unsaved changes.
     */
    public boolean isDirty() {
        return !Objects.equals(getCurrentContentHash(), getOriginalContentHash());
    }

    /**
     * Used by Kogito to set the XML content of the editor.
     * @param path
     * Relative path to the content resource
     * @param value
     * Editor's content
     */
    public abstract Promise setContent(final String path, final String value);

    /**
     * Used by Kogito to get the XML content of the editor. This should return a {@link String}
     * representation of the editors content to persist to an underlying persistent store.
     */
    public abstract Promise getContent();

    /**
     * Used by Kogito to reset the editors "dirty" state following a successful save.
     */
    public abstract void resetContentHash();
}
