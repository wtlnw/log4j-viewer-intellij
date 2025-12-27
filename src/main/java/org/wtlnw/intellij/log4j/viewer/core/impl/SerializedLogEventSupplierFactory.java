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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;

import org.apache.logging.log4j.core.LogEvent;
import org.wtlnw.intellij.log4j.viewer.core.api.LogEventSupplierFactory;

/**
 * {@link LogEventSupplierFactory} implementation for serialized {@link LogEvent}s.
 */
public class SerializedLogEventSupplierFactory implements LogEventSupplierFactory {

	@Override
	public LogEventSupplier get(final InputStream stream) throws IOException {
		if (!stream.markSupported()) {
			throw new IllegalArgumentException();
		}

		// read stream's header in the same way ObjectInputStream does (two shorts = four bytes)
		stream.mark(4);
		try {
			// this will check the stream's header and fail if it's not
			// a valid ObjectInputStream or is not supported.
			final ObjectInputStream ois = new ObjectInputStream(stream);
			return () -> {
				try {
					return (LogEvent) ois.readObject();
				} catch (final ClassNotFoundException ex) {
					throw new IOException(ex);
				}
			};
		} catch (final StreamCorruptedException ex) {
			// make sure to reset the stream to the previous position if it's not
			// a valid ObjectInputStream in order to allow other factories to try
			stream.reset();
			return null;
		}
	}
}
