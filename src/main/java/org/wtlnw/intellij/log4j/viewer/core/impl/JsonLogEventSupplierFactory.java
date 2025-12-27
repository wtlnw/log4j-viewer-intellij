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

package org.wtlnw.intellij.log4j.viewer.core.impl;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.wtlnw.intellij.log4j.viewer.core.api.LogEventSupplierFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;

/**
 * {@link LogEventSupplierFactory} implementation for JsonLayout based {@link LogEvent}s.
 */
public class JsonLogEventSupplierFactory implements LogEventSupplierFactory {

	@Override
	public LogEventSupplier get(final InputStream stream) throws IOException {
		if (!stream.markSupported()) {
			throw new IllegalArgumentException();
		}

		stream.mark(4);
		try {
			final byte[] bytes = stream.readNBytes(4);
			final String string = new String(bytes).trim();
			final boolean complete = string.startsWith("[");
			final boolean fragment = string.startsWith("{");
			if (complete || fragment) {
				// make sure to reset the stream prior to supplier initialization
				stream.reset();

				// initialize the reader with the reset stream for reading multiple events
				final MappingIterator<LogEvent> reader = new Log4jJsonObjectMapper()
						.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
						.readerFor(Log4jLogEvent.class)
						.readValues(stream);

				// that's the actual LogEventSupplier implementation as lambda
				return () -> {
					try {
						if (reader.hasNext()) {
							return reader.next();
						}
					} catch (final RuntimeException ex) {
						// highly sophisticated code to detect the SocketTimeoutException
						// because it is not being propagated as is, we don't want that.
						if (ex.getCause() instanceof SocketTimeoutException ste) {
							throw ste;
						}

						// make sure to wrap other RuntimeExceptions in an IOException
						// to terminate the handler thread correctly
						throw new IOException(ex);
					}
					
					// when we get here, we're done reading from the stream
					throw new EOFException();
				};
			}
		} catch (final Exception ex) {
			// failed to read from the stream
		}

		// either we do not support the stream or we failed reading from it
		stream.reset();
		return null;
	}
}
