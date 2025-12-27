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

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.SocketTimeoutException;
import java.util.Vector;

import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.jackson.Log4jXmlObjectMapper;
import org.wtlnw.intellij.log4j.viewer.core.api.LogEventSupplierFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;

/**
 * {@link LogEventSupplierFactory} implementation for XML based {@link LogEvent}s.
 */
public class XmlLogEventSupplierFactory implements LogEventSupplierFactory {

	@Override
	public LogEventSupplier get(final InputStream stream) throws IOException {
		if (!stream.markSupported()) {
			throw new IllegalArgumentException();
		}

		stream.mark(16);
		try {
			final byte[] bytes = stream.readNBytes(16);
			final String string = new String(bytes).trim();
			final boolean complete = string.startsWith("<?xml");
			final boolean fragment = string.startsWith("<Event");
			if (complete || fragment) {
				// make sure to reset the stream prior to supplier initialization
				stream.reset();
				return new XmlLogEventSupplier(stream, complete);
			}
		} catch (final Exception ex) {
			// failed to read from the stream
		}

		// either we do not support the stream or we failed reading from it
		stream.reset();
		return null;
	}

	/**
	 * A {@link LogEventSupplier} implementation using {@link Log4jXmlObjectMapper}.
	 */
	private static class XmlLogEventSupplier implements LogEventSupplier {

		/**
		 * The {@link MappingIterator} for {@link LogEvent} de-serialization.
		 */
		private final MappingIterator<LogEvent> _reader;

		/**
		 * Create an {@link XmlLogEventSupplier} to read events from the given stream.
		 * 
		 * @param stream   the {@link InputStream} to read the {@link LogEvent}s from
		 * @param complete {@code true} if the contents of the given stream is a
		 *                 well-formed XML document or {@code false} for XML fragments
		 *                 only
		 * @throws IOException if an error occurred during initialization
		 */
		public XmlLogEventSupplier(final InputStream stream, final boolean complete) throws IOException {
			final InputStream input;
			if (!complete) {
				final Vector<InputStream> streams = new Vector<>();
				streams.add(new ByteArrayInputStream("<Events xmlns=\"https://logging.apache.org/log4j/2.0/events\">".getBytes()));
				streams.add(stream);
				streams.add(new ByteArrayInputStream("</Events>".getBytes()));
				
				input = new SequenceInputStream(streams.elements());
			} else {
				input = stream;
			}
			
			_reader = new Log4jXmlObjectMapper()
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
					.readerFor(Log4jLogEvent.class)
					.readValues(input);
		}

		@Override
		public LogEvent get() throws IOException {
			try {
				if (_reader.hasNext()) {
					return _reader.next();
				}
			} catch (final RuntimeException ex) {
				// highly sophisticated code to detect the SocketTimeoutException
				// because it is not being propagated as is, we don't want that.
				if (ex.getCause() instanceof JsonParseException jpe) {
					if (jpe.getCause() instanceof XMLStreamException xse) {
						if (xse.getNestedException() instanceof SocketTimeoutException ste) {
							throw ste;
						}
					}
				}

				// make sure to wrap other RuntimeExceptions in an IOException
				// to terminate the handler thread correctly
				throw new IOException(ex);
			}
			
			// when we get here, we're done reading from the stream
			throw new EOFException();
		}
	}
}
