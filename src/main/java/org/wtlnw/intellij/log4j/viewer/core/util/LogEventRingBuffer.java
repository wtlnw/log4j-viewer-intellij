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

/**
 * Instances of this class provide a cyclic buffer for {@link LogEvent} reading and writing.
 * 
 * <p>
 * Typically, using instances of this class would look like this:
 * 
 * {@snippet lang=java :
 *    final LogEventRingBuffer b = new LogEventRingBuffer(42);
 *    
 *    // put entries into the buffer
 *    b.put(new MutableLogEvent());
 *    b.put(new MutableLogEvent());
 *    b.put(new MutableLogEvent());
 *    
 *    // read from the buffer
 *    for (int i = 0; i < b.getSize(); i++) {
 *    	final LogEvent e = b.get(i);
 *    }
 * }
 * </p>
 * 
 * <p>
 * Note: this class is not thread-safe. Make sure to synchronize access for reading/writing
 * where necessary. 
 * </p>
 */
public class LogEventRingBuffer {

	/**
	 * Actual buffer of {@link LogEvent} instances.
	 */
	private final LogEvent[] _events;

	/**
	 * The first entry in the buffer (inclusive).
	 */
	private int _tail = 0;

	/**
	 * The last entry in the buffer (exclusive).
	 */
	private int _head = 0;

	/**
	 * @see #getSize()
	 */
	private int _size = 0;

	/**
	 * Create a {@link LogEventRingBuffer}.
	 *
	 * @param capacity see {@link #getCapacity()}
	 * @throws IllegalArgumentException if the given capacity is less than one
	 */
	public LogEventRingBuffer(final int capacity) throws IllegalArgumentException {
		if (capacity < 1) {
			throw new IllegalArgumentException();
		}
		_events = new LogEvent[capacity];
	}

	/**
	 * @return the receiver's capacity
	 */
	public int getCapacity() {
		return _events.length;
	}

	/**
	 * @return the number of {@link LogEvent}s retrievable via {@link #get(int)}
	 */
	public int getSize() {
		return _size;
	}

	/**
	 * Insert the given {@link LogEvent} at receiver's head.
	 * 
	 * @param event the {@link LogEvent} to insert
	 */
	public void put(final LogEvent event) {
		_events[_head] = event;

		// advance head cursor and wrap around if necessary
		if (++_head == _events.length) {
			_head = 0;
		}

		// buffer is not full yet, so we can simply increment the size
		if (_size < _events.length) {
			_size++;
		}
		// buffer is full, so the size does not change;
		// instead, advance tail cursor and wrap around if necessary
		else if (++_tail == _events.length) {
			_tail = 0;
		}
	}

	/**
	 * @param index the zero based index (zero is the buffer's tail) to retrieve the
	 *              {@link LogEvent} at
	 * @return the {@link LogEvent} at the given index in the buffer
	 * @throws IndexOutOfBoundsException if the given index is less than zero or the
	 *                                   receiver does not have that many entries
	 */
	public LogEvent get(final int index) throws IndexOutOfBoundsException {
		if (index < 0 || index >= _size) {
			throw new IndexOutOfBoundsException(index);
		}

		int i = _tail + index;
		if (i >= _events.length) {
			i -= _events.length;
		}
		
		return _events[i];
	}

	/**
	 * Clears the receiver and {@code null}s its contents.
	 */
	public void clear() {
		for (int i = 0; i < _size; i++) {
			_events[i] = null;
		}

		_head = _tail = _size = 0;
	}
}
