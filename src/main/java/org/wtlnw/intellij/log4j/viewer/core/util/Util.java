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

package org.wtlnw.intellij.log4j.viewer.core.util;

import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.logging.log4j.core.LogEvent;

/**
 * Static utility functions and constants.
 */
public class Util {

	/**
	 * The {@link DateTimeFormatter} to use for {@link LogEvent}'s time stamp formatting.
	 */
	public static final DateTimeFormatter FORMAT = DateTimeFormatter.RFC_1123_DATE_TIME
			.withChronology(IsoChronology.INSTANCE)
			.withLocale(Locale.getDefault())
			.withZone(ZoneId.systemDefault());
}
