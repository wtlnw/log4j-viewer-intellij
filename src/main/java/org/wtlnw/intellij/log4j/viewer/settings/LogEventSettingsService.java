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

package org.wtlnw.intellij.log4j.viewer.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A {@link PersistentStateComponent} implementation responsible for reading/writing
 * log viewer settings.
 */
@State(
    name="org.wtlnw.intellij.log4j.viewer.settings.LogEventSettings",
    storages= @Storage("LogEventSettings.xml")
)
public class LogEventSettingsService implements PersistentStateComponent<LogEventSettings> {

    /**
     * @see #getState()
     */
    private LogEventSettings _settings = new LogEventSettings();

    @Override
    public @NonNull LogEventSettings getState() {
        return _settings;
    }

    @Override
    public void loadState(@NonNull LogEventSettings settings) {
        _settings = settings;
    }

    /**
     * @return the singleton {@link LogEventSettingsService} instance
     */
    public static LogEventSettingsService getInstance() {
        return ApplicationManager.getApplication().getService(LogEventSettingsService.class);
    }
}
