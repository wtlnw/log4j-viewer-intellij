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

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import org.apache.logging.log4j.core.LogEvent;

/**
 * A {@link Buffer} inspired implementation for {@link LogEvent}s.
 * 
 * <p>
 * Typically, using instances of this class would look like this:
 * 
 * {@snippet lang=java :
 *    final LogEventBuffer b = new LogEventBuffer(42);
 *    
 *    // fill the buffer completely
 *    while (!b.depleted()) {
 *       b.put(new MutableLogEvent());
 *    }
 *    
 *    // flip the buffer for reading
 *    b.flip();
 *    while(!b.depleted()) {
 *       final LogEvent e = b.get();
 *       // do magic
 *    }
 *    
 *    // clear the buffer for writing again
 *    buffer.clear();
 * }
 * </p>
 * 
 * <p>
 * Note: this class is not thread-safe. Make sure to synchronize access for reading/writing
 * where necessary. 
 * </p>
 */
public class LogEventBuffer {

	/**
	 * The array of {@link LogEvent}s representing the actual buffer.
	 */
	private final LogEvent[] _events;

	/**
	 * The index in the buffer to {@link #put(LogEvent)} or {@link #get()} at.
	 */
	private int _position;

	/**
	 * The index in the buffer at which no events can be put or read.
	 */
	private int _limit;

	/**
	 * Create a {@link LogEventBuffer} which is ready for writing.
	 * 
	 * @param capacity the buffer's capacity
	 * @throws IllegalArgumentException if the given capacity is less than one
	 */
	public LogEventBuffer(final int capacity) throws IllegalArgumentException {
		if (capacity < 1) {
			throw new IllegalArgumentException();
		}

		_events = new LogEvent[capacity];
		_position = 0;
		_limit = _events.length;
	}

	/**
	 * @return {@code true} if no elements can be inserted into or read from the
	 *         receiver, {@code false} otherwise
	 */
	public boolean depleted() {
		return _position >= _limit;
	}

	/**
	 * Insert the given {@link LogEvent} at the receiver's current position.
	 * 
	 * @param event the {@link LogEvent} to insert
	 * @return this {@link LogEventBuffer} instance for convenient call chaining
	 * @throws BufferOverflowException if the receiver is {@link #depleted()}
	 */
	public LogEventBuffer put(final LogEvent event) throws BufferOverflowException {
		if (depleted()) {
			throw new BufferOverflowException();
		}
		_events[_position++] = event;

		return this;
	}

	/**
	 * @return the {@link LogEvent} at the receiver's current position
	 * @throws BufferUnderflowException if the receiver is {@link #depleted()}
	 */
	public LogEvent get() throws BufferUnderflowException {
		if (depleted()) {
			throw new BufferUnderflowException();
		}

		return _events[_position++];
	}

	/**
	 * Limits the receiver to its current position and resets the position to start
	 * thus preparing the receiver for reading.
	 * 
	 * @return this {@link LogEventBuffer} instance for convenient call chaining
	 */
	public LogEventBuffer flip() {
		_limit = _position;
		_position = 0;
		return this;
	}

	/**
	 * Resets the receiver's limit to its initial capacity and the position to start,
	 * thus preparing the receiver for writing.
	 * 
	 * @return this {@link LogEventBuffer} instance for convenient call chaining
	 */
	public LogEventBuffer clear() {
		_limit = _events.length;
		_position = 0;
		return this;
	}
}
