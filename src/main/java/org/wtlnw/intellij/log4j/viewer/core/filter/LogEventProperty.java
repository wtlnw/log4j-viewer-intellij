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

import java.time.Instant;
import java.util.function.Function;

import org.apache.logging.log4j.core.LogEvent;
import org.wtlnw.intellij.log4j.viewer.core.util.Util;

/**
 * An enumeration of {@link LogEvent} properties supported by this
 * implementation.
 */
public enum LogEventProperty {

	/**
	 * Literal representing the moment in time a {@link LogEvent} occurred.
	 */
	TIMESTAMP("Timestamp", e -> Util.FORMAT.format(Instant.ofEpochMilli(e.getInstant().getEpochMillisecond()))),

	/**
	 * Literal representing a {@link LogEvent}'s severity level.
	 */
	LEVEL("Level", e -> e.getLevel().toString()),

	/**
	 * Literal representing a {@link LogEvent}'s category (aka logger name).
	 */
	CATEGORY("Category", e -> e.getLoggerName()),

	/**
	 * Literal representing a {@link LogEvent}'s formatted message.
	 */
	MESSAGE("Message", e -> e.getMessage().getFormattedMessage()),

	/**
	 * A literal representing whether an error was logged or not.
	 */
	@SuppressWarnings("deprecation")
	THROWABLE("Throwable", e -> e.getThrownProxy() == null ? "" : "x");

	/**
	 * @see #getName()
	 */
	private final String _name;

	/**
	 * @see #getValueProvider()
	 */
	private final Function<LogEvent, String> _accessor;

	/**
	 * Create a {@link LogEventProperty}.
	 * 
	 * @param name     see {@link #getName()}
	 * @param accessor see {@link #getValueProvider()}
	 */
	private LogEventProperty(final String name, final Function<LogEvent, String> accessor) {
		_name = name;
		_accessor = accessor;
	}

	/**
	 * @return the name of the property represented by this literal
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @return the {@link Function} to be used to retrieve the property value from a
	 *         {@link LogEvent} instance
	 */
	public Function<LogEvent, String> getValueProvider() {
		return _accessor;
	}
}
