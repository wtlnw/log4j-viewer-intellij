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

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.wtlnw.intellij.log4j.viewer.core.api.LogEventSupplierFactory;
import org.wtlnw.intellij.log4j.viewer.core.api.LogEventSupplierFactory.LogEventSupplier;

/**
 * Instances of this class provide server facilities for receiving log4j messages
 * sent by clients using {@link SocketAppender}s.
 */
public class LogEventServer {

	/**
	 * @see #getPort()
	 */
	private final int _port;

	/**
	 * @see #getTimeout()
	 */
	private final int _timeout;

	/**
	 * @see #getConsumer()
	 */
	private final Consumer<LogEvent> _consumer;

	/**
	 * @see #getSupplierFactories()
	 */
	private final List<LogEventSupplierFactory> _factories;

	/**
	 * A {@link List} of {@link Consumer}s to be called when the server's state changes.
	 * 
	 * @see #addServerListener(Consumer)
	 * @see #removeServerListener(Consumer)
	 */
	private final CopyOnWriteArrayList<Consumer<Boolean>> _serverListeners = new CopyOnWriteArrayList<>();

	/**
	 * A {@link List} of {@link BiConsumer}s to be called when an error occurs.
	 * 
	 * @see #addErrorListener(BiConsumer)
	 * @see #removeErrorListener(BiConsumer)
	 */
	private final CopyOnWriteArrayList<BiConsumer<String, Throwable>> _errorListeners = new CopyOnWriteArrayList<>();
	
	/**
	 * The {@link ExecutorService} to be used for accepting incoming connections and
	 * handling data in parallel.
	 */
	private volatile ExecutorService _executor;

	/**
	 * Create a {@link LogEventServer} with the default port (4445) and timeout
	 * (500ms).
	 * 
	 * @param factories see {@link #getSupplierFactories()}
	 * @param consumer  see {@link #getConsumer()}
	 * @throws NullPointerException if the given {@link List} of factories or the
	 *                              consumer are invalid
	 */
	public LogEventServer(final List<LogEventSupplierFactory> factories, final Consumer<LogEvent> consumer) throws NullPointerException {
		this(4445, 500, factories, consumer);
	}
	
	/**
	 * Create a {@link LogEventServer}.
	 * 
	 * @param port      see {@link #getPort()}
	 * @param timeout   see {@link #getTimeout()}
	 * @param factories see {@link #getSupplierFactories()}
	 * @param consumer  see {@link #getConsumer()}
	 * @throws IllegalArgumentException if the given port or timeout are invalid
	 * @throws NullPointerException     if the given {@link List} of factories or
	 *                                  the consumer are invalid
	 */
	public LogEventServer(final int port, final int timeout, final List<LogEventSupplierFactory> factories, final Consumer<LogEvent> consumer) throws IllegalArgumentException, NullPointerException {
		if (port < 0 || port > 65535) {
			throw new IllegalArgumentException("Invalid port number: " + port);
		}
		if (timeout < 0) {
			throw new IllegalArgumentException("Invalid timeout [ms]: " + timeout);
		}

		_port = port;
		_timeout = timeout;
		_consumer = Objects.requireNonNull(consumer);
		_factories = Objects.requireNonNull(factories);
	}

	/**
	 * @return the port to listen to incoming connections on
	 */
	public int getPort() {
		return _port;
	}

	/**
	 * @return the timeout in milliseconds to block when waiting for incoming
	 *         connections or data
	 */
	public int getTimeout() {
		return _timeout;
	}

	/**
	 * @return the {@link Consumer} to be called when {@link LogEvent} are read from
	 *         incoming connections
	 */
	public Consumer<LogEvent> getConsumer() {
		return _consumer;
	}

	/**
	 * @return a (possibly empty) {@link List} of {@link LogEventSupplierFactory}s
	 *         to be used for reading {@link LogEvent} from accepted connections.
	 */
	public List<LogEventSupplierFactory> getSupplierFactories() {
		return _factories;
	}

	/**
	 * Register the given {@link Consumer} to be called when the receiver's state
	 * changes.
	 * 
	 * <p>
	 * Note: has no effect if an identical {@link Consumer} had already been
	 * registered.
	 * </p>
	 * 
	 * @param listener the {@link Consumer} to register
	 */
	public void addServerListener(final Consumer<Boolean> listener) {
		_serverListeners.addIfAbsent(listener);
	}

	/**
	 * Unregister the given {@link Consumer}.
	 * 
	 * <p>
	 * Note: has no effect if the given consumer was not registered.
	 * </p>
	 * 
	 * @param listener the {@link Consumer} to unregister
	 */
	public void removeServerListener(final Consumer<Boolean> listener) {
		_serverListeners.remove(listener);
	}

	/**
	 * Register the given {@link BiConsumer} to be called when a communication error occurs.
	 * 
	 * <p>
	 * Note: has no effect if an identical {@link BiConsumer} had already been
	 * registered.
	 * </p>
	 * 
	 * @param listener the {@link BiConsumer} to register
	 */
	public void addErrorListener(final BiConsumer<String, Throwable> listener) {
		_errorListeners.addIfAbsent(listener);
	}
	
	/**
	 * Unregister the given {@link BiConsumer}.
	 * 
	 * <p>
	 * Note: has no effect if the given consumer was not registered.
	 * </p>
	 * 
	 * @param listener the {@link BiConsumer} to unregister
	 */
	public void removeErrorListener(final BiConsumer<String, Throwable> listener) {
		_errorListeners.remove(listener);
	}
	
	/**
	 * Start listening for incoming connections and data.
	 * 
	 * @throws IllegalStateException if the receiver had already been started
	 */
	public synchronized void start() throws IllegalStateException {
		if (_executor != null) {
			throw new IllegalStateException();
		}

		_executor = Executors.newThreadPerTaskExecutor(Thread::new);

		// start the acceptor thread
		_executor.execute(() -> {
			try (final ServerSocket server = new ServerSocket(getPort())) {
				// make sure to set a timeout prior to entering the accept-loop
				// because we cannot guarantee that blocking can be interrupted
				// in order to shutdown the server itself
				server.setSoTimeout(getTimeout());

				// notify all registered listeners of successful server start
				_serverListeners.forEach(l -> l.accept(true));

				// wait for incoming connection requests
				accept(server, _executor);
			} catch (final IOException e) {
				// notify all registered listener of acceptor error
				_errorListeners.forEach(l -> l.accept("Acceptor thread encountered an error, server is going down.", e));

				// Server socket is broken, terminate the server.
				// WARNING: do NOT await termination here because we're running
				// in the context of the executor and would thus prevent it from
				// terminating!
				stop(false);
			}
		});
	}

	/**
	 * Accept incoming connections and handle these in a separate thread.
	 * 
	 * @param server   the {@link ServerSocket} to listen for incoming connection on
	 * @param executor the {@link ExecutorService} to be used for creating new
	 *                 threads for data reading
	 * @throws IOException if an error occurred while using the given
	 *                     {@link ServerSocket}
	 */
	private void accept(final ServerSocket server, final ExecutorService executor) throws IOException {
		// make sure to exit the accept-loop when server stop is requested
		while (!executor.isShutdown()) {
			try {
				final Socket client = server.accept();

				// run event reading in a separate thread
				executor.execute(() -> handle(client, executor));
			} catch (final SocketTimeoutException ex) {
				// no connection attempts, continue listening
			} catch (final RejectedExecutionException ex) {
				// this may happen when a connection request arrives
				// while the server is being shut down -> ignore it
			}
		}
	}

	/**
	 * Read {@link LogEvent}s from the given {@link Socket} and forward them to
	 * {@link #getConsumer()}.
	 * 
	 * @param client   the {@link Socket} to read events from
	 * @param executor the {@link ExecutorService} to check for shutdown requests
	 */
	private void handle(final Socket client, final ExecutorService executor) {
		try (final InputStream stream = new BufferedInputStream(client.getInputStream())) {
			// make sure to set a timeout prior to entering the read-loop
			// because we cannot guarantee that blocking can be interrupted
			// in order to shutdown the server itself
			client.setSoTimeout(getTimeout());

			// a supplier can be cached per stream because log4j
			// allows only one layout per SocketAppender:
			// so one connection -> one layout
			final LogEventSupplier supplier = getSupplier(stream, executor);
			read(supplier, executor);
		} catch (final EOFException ex) {
			// stream closed, terminate thread
			_errorListeners.forEach(l -> l.accept(client.getInetAddress() + ": client connection terminated, handler thread is going down.", ex));
		} catch (final IOException ex) {
			// client socket is broken, notify error listeners and terminate thread
			_errorListeners.forEach(l -> l.accept(client.getInetAddress() + ": handler thread encountered an error, connection is going down.", ex));
		} catch (final IllegalStateException ex) {
			// unsupported event format, notify error listeners and terminate thread
			_errorListeners.forEach(l -> l.accept(client.getInetAddress() + ": unsupported event format, connection is going down.", ex));
		} catch (final IllegalArgumentException ex) {
			// stream without mark/reset support, notify error listeners and terminate thread
			_errorListeners.forEach(l -> l.accept(client.getInetAddress() + ": unsupported stream implementation, connection is going down.", ex));
		}
	}

	/**
	 * @param stream   the {@link InputStream} to return the appropriate
	 *                 {@link LogEventSupplier} for
	 * @param executor the {@link ExecutorService} to check for shutdown requests
	 * @return the {@link LogEventSupplier} which supports reading {@link LogEvent}s
	 *         from the given {@link InputStream}
	 * @throws IOException              if an error occurred while reading from the
	 *                                  given {@link InputStream}
	 * @throws IllegalArgumentException if the given {@link InputStream} does not
	 *                                  support mark/reset API
	 * @throws IllegalStateException    if an appropriate {@link LogEventSupplier}
	 *                                  could not be determined
	 */
	private LogEventSupplier getSupplier(final InputStream stream, final ExecutorService executor)
			throws IOException, IllegalArgumentException, IllegalStateException {
		if (!stream.markSupported()) {
			throw new IllegalArgumentException("Cannot determine event supplier for connections not supporting mark/reset.");
		}

		while (!executor.isShutdown()) {
			try {
				// wait until data arrives in order to determine which supplier to use
				stream.mark(1);
				if (stream.read() < 0) {
					throw new IllegalStateException("Cannot determine event supplier for empty streams.");
				}
				stream.reset();

				// now that we know that there is data in the stream, we can determine
				// the supplier to be used for the given stream or fail if none was
				// determined because.
				for (final LogEventSupplierFactory factory : getSupplierFactories()) {
					final LogEventSupplier supplier = factory.get(stream);
					if (supplier != null) {
						return supplier;
					}
				}

				throw new IllegalStateException("Cannot determine event supplier for unknown event type.");
			} catch (final SocketTimeoutException ex) {
				// no incoming data, continue
			}
		}

		// this may happen when no data was received at all and the server is going
		// down.
		throw new IllegalStateException("Cannot determine event supplier for missing data.");
	}

	/**
	 * Read {@link LogEvent}s using the given {@link LogEventSupplier}.
	 * 
	 * @param supplier the {@link LogEventSupplier} to be used for reading
	 * @param executor the {@link ExecutorService} to check for shutdown requests
	 * @throws IOException  if an error occurred while reading events
	 * @throws EOFException if end of stream was reached
	 */
	private void read(final LogEventSupplier supplier, final ExecutorService executor) throws IOException, EOFException {
		// make sure the exit the event-loop when server stop is requested
		while (!executor.isShutdown()) {
			try {
				getConsumer().accept(supplier.get());
			} catch (final SocketTimeoutException ex) {
				// no incoming data, continue
			}
		}
	}

	/**
	 * Stop listening for incoming connection requests and reading data from
	 * accepted connections.
	 * 
	 * <p>
	 * Note: this method must not be called with {@code true} from within the
	 * acceptor or workers threads as doing so would block them indefinitely.
	 * </p>
	 *
	 * @param await {@code true} to block the calling thread until all connections
	 *              are terminated, {@code false} to terminate without waiting
	 * @throws IllegalStateException if the receiver was not started
	 */
	public synchronized void stop(final boolean await) throws IllegalStateException {
		if (_executor == null) {
			throw new IllegalStateException();
		}

		_executor.shutdown();
		if (await) {
			awaitTermination();
		}
		_executor = null;

		// notify all registered listeners of server termination
		_serverListeners.forEach(l -> l.accept(false));
	}

	/**
	 * Block current thread until the {@link ExecutorService} has terminated all
	 * currently running tasks.
	 */
	private void awaitTermination() {
		while (true) {
			try {
				if (_executor.awaitTermination(_timeout, TimeUnit.MILLISECONDS)) {
					break;
				}
			} catch (final InterruptedException ex) {
				// ignore and continue waiting
			}
		}
	}

	/**
	 * @return {@code true} if the receiver is running, {@code false} otherwise
	 */
	public boolean isRunning() {
		// WARNING: do NOT inline this! We are assigning the reference
		// to a local variable in order to perform two subsequent evaluations
		// here and we don't want it to change midways.
		final ExecutorService executor = _executor;

		return executor != null && !executor.isShutdown();
	}
}
