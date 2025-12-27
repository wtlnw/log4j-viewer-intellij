/********************************************************************************
 * Copyright (c) 2025 wtlnw and contributors
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ********************************************************************************/

package org.wtlnw.intellij.log4j.viewer.core.filter;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link PersistentStateComponent} implementation responsible for reading/writing
 * {@link LogEventFilter} settings.
 */
@Service
@State(
        name="org.wtlnw.intellij.log4j.viewer.core.filter.LogEventFilterService",
        storages= @Storage("LogEventFilter.xml")
)
public final class LogEventFilterService implements PersistentStateComponent<LogEventFilterService.LogEventFilterState> {

    /**
     * @see #getFilter()
     */
    private LogEventFilter _filter = new LogEventFilter();

    /**
     * @return the currently used {@link LogEventFilter}.
     */
    public synchronized @NotNull  LogEventFilter getFilter() {
        return _filter;
    }

    /**
     * Setter for {@link #getFilter()}.
     *
     * @param filter see {@link #getFilter()}
     */
    public synchronized void setFilter(@NotNull final LogEventFilter filter) {
        _filter = filter;
    }

    @Override
    public LogEventFilterState getState() {
        final LogEventFilterState filterState = new LogEventFilterState();

        for (final LogEventPropertyFilter property : _filter.getFilters()) {
            final LogEventPropertyFilterState propertyState = new LogEventPropertyFilterState();
            propertyState.property = property.getProperty();
            propertyState.enabled = property.isEnabled();
            propertyState.matchCase = property.isMatchCase();
            propertyState.regex = property.isRegularExpression();
            propertyState.wholeWord = property.isWholeWord();
            propertyState.inverse = property.isInverse();
            propertyState.pattern = property.getPattern();
            filterState.filters.add(propertyState);
        }

        return filterState;
    }

    @Override
    public void loadState(@NonNull LogEventFilterState filterState) {
        final LogEventFilter filter = new LogEventFilter();

        for (final LogEventPropertyFilterState propertyState : filterState.filters) {
            final LogEventPropertyFilter property = filter.get(propertyState.property);
            property.setEnabled(propertyState.enabled);
            property.setMatchCase(propertyState.matchCase);
            property.setRegularExpression(propertyState.regex);
            property.setWholeWord(propertyState.wholeWord);
            property.setInverse(propertyState.inverse);
            property.setPattern(propertyState.pattern);
        }

        _filter = filter;
    }

    /**
     * @return the singleton {@link LogEventFilterService} instance
     */
    public static LogEventFilterService getInstance() {
        return ApplicationManager.getApplication().getService(LogEventFilterService.class);
    }

    /**
     * A java bean class to be used as a serializable representation of {@link LogEventFilter} instances.
     */
    public static class LogEventFilterState {

        /**
         * @see LogEventFilter#getFilters()
         */
        @XCollection
        public List<LogEventPropertyFilterState> filters = new ArrayList<>();
    }

    /**
     * A java bean class to be used as a serializable representation of {@link LogEventPropertyFilter} instances.
     */
    public static class LogEventPropertyFilterState {

        /**
         * @see LogEventPropertyFilter#getProperty()
         */
        public LogEventProperty property;

        /**
         * @see LogEventPropertyFilter#isEnabled()
         */
        public boolean enabled;

        /**
         * @see LogEventPropertyFilter#isMatchCase()
         */
        public boolean matchCase;

        /**
         * @see LogEventPropertyFilter#isRegularExpression()
         */
        public boolean regex;

        /**
         * @see LogEventPropertyFilter#isWholeWord()
         */
        public boolean wholeWord;

        /**
         * @see LogEventPropertyFilter#isInverse()
         */
        public boolean inverse;

        /**
         * @see LogEventPropertyFilter#getPattern()
         */
        public String pattern;
    }
}
