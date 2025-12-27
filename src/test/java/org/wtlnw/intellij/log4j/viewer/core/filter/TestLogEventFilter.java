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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * JUnit tests for {@link LogEventFilter}.
 */
class TestLogEventFilter {

	@Test
	void test() {
		final MutableLogEvent event = new MutableLogEvent();

		final LogEventFilter filter = new LogEventFilter();
		filter.get(LogEventProperty.CATEGORY).setPattern("logger");
		filter.get(LogEventProperty.LEVEL).setPattern("info");

		// no filters are active by default, the event must be accepted
		Assertions.assertTrue(filter.test(event));

		// activate the CATEGORY and LEVEl filters
		filter.get(LogEventProperty.CATEGORY).setEnabled(true);
		filter.get(LogEventProperty.LEVEL).setEnabled(true);
		Assertions.assertFalse(filter.test(event));
		
		event.setLoggerName("logger");
		Assertions.assertFalse(filter.test(event));
		
		event.setLevel(Level.INFO);
		Assertions.assertTrue(filter.test(event));
	}

	@Test
	void testDefaults() {
		final LogEventFilter filter = new LogEventFilter();

		Assertions.assertEquals(LogEventProperty.values().length, filter.getFilters().size());
		Assertions.assertThrows(UnsupportedOperationException.class, () -> filter.getFilters().add(new LogEventPropertyFilter(LogEventProperty.CATEGORY)));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> filter.getFilters().remove(0));
	}
}
