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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * JUnit tests for {@link LogEventRingBuffer}.
 */
class TestLogEventRingBuffer {

	@Test
	void test() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new LogEventRingBuffer(0));
		
		final LogEvent event1 = new MutableLogEvent();
		final LogEvent event2 = new MutableLogEvent();
		final LogEvent event3 = new MutableLogEvent();
		final LogEvent event4 = new MutableLogEvent();
		final LogEvent event5 = new MutableLogEvent();
		final LogEvent event6 = new MutableLogEvent();

		final LogEventRingBuffer buffer = new LogEventRingBuffer(3);
		Assertions.assertEquals(0, buffer.getSize());
		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(0));

		buffer.put(event1);
		Assertions.assertEquals(1, buffer.getSize());
		Assertions.assertEquals(event1, buffer.get(0));

		buffer.put(event2);
		buffer.put(event3);

		// buffer is full now, no entry has been overwritten yet
		Assertions.assertEquals(3, buffer.getSize());
		Assertions.assertEquals(event1, buffer.get(0));
		Assertions.assertEquals(event2, buffer.get(1));
		Assertions.assertEquals(event3, buffer.get(2));
		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(3));

		// wrap around of buffer's head, no wrap around of buffer's tail
		buffer.put(event4);
		Assertions.assertEquals(event2, buffer.get(0));
		Assertions.assertEquals(event3, buffer.get(1));
		Assertions.assertEquals(event4, buffer.get(2));
		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(3));

		// wrap around of buffer's tail
		buffer.put(event5);
		buffer.put(event6);
		Assertions.assertEquals(event4, buffer.get(0));
		Assertions.assertEquals(event5, buffer.get(1));
		Assertions.assertEquals(event6, buffer.get(2));
		Assertions.assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(3));
	}
}
