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

import com.intellij.util.messages.Topic;

/**
 * Implementing classes can connect to the topic publishing changes to
 * {@link LogEventSettings} through {@link LogEventConfigurable}.
 *
 * <p>
 * Note that programmatic changes to settings are not exposed through this interface.
 * </p>
 */
@FunctionalInterface
public interface LogEventSettingsListener {

    /**
     * The {@link Topic} to subscribe to in order to be notified upon changes to {@link LogEventSettings}.
     */
    @Topic.AppLevel
    Topic<LogEventSettingsListener> CHANGE_SETTINGS_TOPIC = Topic.create("LogEventSettings", LogEventSettingsListener.class);

    /**
     * This method is called after {@link LogEventSettings} have been changed.
     *
     * <p>
     * Changed settings should be retrieved via {@link LogEventSettingsService#getState()}.
     * </p>
     */
    void settingsChanged();
}
