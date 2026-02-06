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
import java.net.BindException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.wtlnw.intellij.log4j.viewer.core.api.LogEventSupplierFactory;

/**
 * Unit tests for {@link LogEventServer}.
 */
@Tag("log4j")
public class TestLogEventServer {

	private static final String CONFIG = """
			<Configuration xmlns="https://logging.apache.org/xml/ns"
			               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
			               xsi:schemaLocation="
			                   https://logging.apache.org/xml/ns
			                   https://logging.apache.org/xml/ns/log4j-config-2.xsd">
			  <Appenders>
			    <Socket name="SOCKET_APPENDER" host="localhost" port="4445">
			      <SerializedLayout/>
			    </Socket>
			  </Appenders>
			  <Loggers>
			    <Root level="INFO">
			      <AppenderRef ref="SOCKET_APPENDER"/>
			    </Root>
			  </Loggers>
			</Configuration>
			""";

	private static final Consumer<LogEventServer> NOOP = server -> {};
	
	private static final Consumer<LogEventServer> WAIT = server -> {
		try {
			Thread.sleep(server.getTimeout());
		} catch (final InterruptedException e) {
			// ignore interruption
		}
	}; 
	
	@Test
	void test() throws IOException, InterruptedException {
		runWithConfiguration(config(CONFIG), NOOP, NOOP);
	}

	@Test
	void testAcceptTimeout() throws IOException, InterruptedException {
		runWithConfiguration(config(CONFIG), WAIT, NOOP);
	}
	
	@Test
	void testReadTimeout() throws IOException, InterruptedException {
		runWithConfiguration(config(CONFIG), NOOP, WAIT);
	}
	
	@Test
	void testNoSuppliers() throws IOException, InterruptedException {
		final Semaphore sema = new Semaphore(0);
		final List<LogEvent> events = new ArrayList<>();
		final List<LogEventSupplierFactory> factories = List.of();
		final LogEventServer server = new LogEventServer(factories, events::add);
		server.addErrorListener((msg, ex) -> {
			if (ex instanceof IllegalStateException) {
				sema.release();
			}
		});

		server.start();
		
		try (final LoggerContext context = Configurator.initialize(config(CONFIG))) {
			final Logger logger = context.getLogger(TestLogEventServer.class.getSimpleName());
			
			// now send the log messages
			logger.info("Information message");
			logger.warn("Warning message");
			logger.error("Error message", new RuntimeException());
		} catch (final Exception ex) {
			// logger initialization failed: make sure to release the
			// semaphore to allow the test to terminate
			sema.release();
		}

		sema.acquire();
		server.stop(true);

		Assertions.assertEquals(0, events.size());
	}

	@Test
	void testMultipleServers() throws IOException, InterruptedException {
		// start two servers (one would start and one would not) and then try to stop the second one
		final Semaphore startTwo = new Semaphore(0);
		final Semaphore stopBoth = new Semaphore(0);
		final List<LogEvent> events = Collections.synchronizedList(new ArrayList<>());
		final List<LogEventSupplierFactory> factories = List.of();
		final LogEventServer serverOne = new LogEventServer(factories, events::add);
		serverOne.addServerListener(started -> {
			if (started) {
				startTwo.release();
			}
		});
		final LogEventServer serverTwo = new LogEventServer(factories, events::add);
		serverTwo.addErrorListener((msg, ex) -> {
			if (ex instanceof BindException) {
				stopBoth.release();
			}
		});

		// this one should succeed
		serverOne.start();
		startTwo.tryAcquire(serverOne.getTimeout() * 3, TimeUnit.MILLISECONDS);

		Assertions.assertTrue(serverOne.isRunning());

		// this one should fail
		serverTwo.start();
		stopBoth.tryAcquire(serverTwo.getTimeout() * 3, TimeUnit.MILLISECONDS);

		Assertions.assertFalse(serverTwo.isRunning());

		// both should terminate in a finite amount of time
		Assertions.assertDoesNotThrow(() -> serverOne.stop(true));
		Assertions.assertFalse(serverOne.isRunning());

		Assertions.assertThrows(IllegalStateException.class, () -> serverTwo.stop(true));
	}

	@SuppressWarnings("deprecation")
	private void runWithConfiguration(final Configuration config, final Consumer<LogEventServer> postStartAction, final Consumer<LogEventServer> preLogAction) throws InterruptedException {
		// use a semaphore to block until the handler thread fails
		// which is a signal for us that either an error occurred or
		// end of stream was reached.
		final Semaphore sema = new Semaphore(0);
		
		final List<LogEvent> events = new ArrayList<>();
		final List<LogEventSupplierFactory> factories = List.of(new SerializedLogEventSupplierFactory());
		final List<Throwable> errors = new ArrayList<>();
		final LogEventServer server = new LogEventServer(factories, events::add);
		server.addErrorListener((msg, ex) -> {
			if (ex instanceof EOFException) {
				// ignore EOF
			} else {
				errors.add(ex);
			}
			sema.release();
		});

		server.start();
		postStartAction.accept(server);

		try (final LoggerContext context = Configurator.initialize(config)) {
			final Logger logger = context.getLogger(TestLogEventServer.class.getSimpleName());
			
			preLogAction.accept(server);
			
			logger.info("Information message");
			logger.warn("Warning message");
			logger.error("Error message", new RuntimeException());
		} catch (final Exception ex) {
			// logger initialization failed: make sure to release the
			// semaphore to allow the test to terminate
			sema.release();
		}

		sema.acquire();
		server.stop(true);

		if (!errors.isEmpty()) {
			Assertions.fail(errors.getFirst());
		}
		Assertions.assertEquals(3, events.size());
		Assertions.assertEquals("Information message", events.get(0).getMessage().getFormattedMessage());
		Assertions.assertEquals("Warning message", events.get(1).getMessage().getFormattedMessage());
		Assertions.assertEquals("Error message", events.get(2).getMessage().getFormattedMessage());
		
		Assertions.assertNotNull(events.get(2).getThrownProxy());
	}
	
	private Configuration config(final String config) throws IOException {
		try (final InputStream input = new ByteArrayInputStream(config.getBytes())) {
			return ConfigurationFactory.getInstance().getConfiguration(null, new ConfigurationSource(input));
		}
	}
}
