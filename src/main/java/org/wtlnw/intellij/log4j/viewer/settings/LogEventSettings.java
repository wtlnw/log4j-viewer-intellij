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

import org.apache.logging.log4j.core.LogEvent;

/**
 * A java bean class declaring log viewer settings with their default values.
 */
public class LogEventSettings {

    /**
     * The port to listen to incoming {@link LogEvent}s on.
     */
    public int port = 4445;

    /**
     * The timeout in milliseconds to block for when listening
     * for incoming connection requests or data for.
     */
    public int timeout = 500;

    /**
     * The number of entries to be displayed in the log event view.
     */
    public int buffer = 1 << 12;

    /**
     * The flag indicating whether to automatically start listening
     * for incoming events when the log event view is opened.
     */
    public boolean autostart = true;

    /**
     * The color to be used for displaying debug messages in the log view.
     */
    public String colorDebug = "000000";

    /**
     * The color to be used for displaying info messages in the log view.
     */
    public String colorInfo = "008000";

    /**
     * The color to be used for displaying warning messages in the log view.
     */
    public String colorWarn = "ff8000";

    /**
     * The color to be used for displaying error messages in the log view.
     */
    public String colorError = "ff0000";

    /**
     * The color to be used for displaying fatal messages in the log view.
     */
    public String colorFatal = "800000";
}
