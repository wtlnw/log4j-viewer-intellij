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

package org.wtlnw.intellij.log4j.viewer.ui;

import org.apache.logging.log4j.core.LogEvent;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventFilter;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventProperty;
import org.wtlnw.intellij.log4j.viewer.core.util.LogEventRingBuffer;
import org.wtlnw.intellij.log4j.viewer.i18n.LogEventBundle;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An {@link AbstractTableModel} implementation displaying {@link LogEvent}s.
 */
public class LogEventTableModel extends AbstractTableModel {

    /**
     * The {@link ReadWriteLock} to be used for synchronized access to
     * the underlying event buffers.
     */
    private final ReadWriteLock _lock = new ReentrantReadWriteLock();

    /**
     * The {@link LogEventRingBuffer} instance containing unfiltered {@link LogEvent}s.
     */
    private final LogEventRingBuffer _rawEvents;

    /**
     * The {@link LogEventRingBuffer} instance containing filtered {@link LogEvent}s actually displayed in the table.
     */
    private final LogEventRingBuffer _tableData;

    /**
     * @see #getFilter()
     */
    private volatile LogEventFilter _filter;

    /**
     * Create a {@link LogEventTableModel}.
     *
     * @param size   the maximum number of captured {@link LogEvent}s to be displayed
     * @param filter see {@link #getFilter()}
     */
    public LogEventTableModel(final int size, @Nonnull final LogEventFilter filter) {
        _rawEvents = new LogEventRingBuffer(size);
        _tableData = new LogEventRingBuffer(size);
        _filter = Objects.requireNonNull(filter);
    }

    /**
     * @return the {@link LogEventFilter} to be used for filtering captured {@link LogEvent}s
     */
    public LogEventFilter getFilter() {
        return _filter;
    }

    /**
     * Setter for {@link #getFilter()}.
     *
     * @param filter see {@link #getFilter()}
     */
    public void setFilter(@Nonnull final LogEventFilter filter) {
        _filter = Objects.requireNonNull(filter);

        locking(_lock.writeLock(), () -> {
            _tableData.clear();
            for (int i = 0; i < _rawEvents.getSize(); i++) {
                final LogEvent event = _rawEvents.get(i);

                if (_filter.test(event)) {
                    _tableData.put(event);
                }
            }
        });

        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return LogEventProperty.values().length;
    }

    @Override
    public String getColumnName(final int column) {
        return LogEventBundle.message(LogEventBundle.key(LogEventProperty.class, LogEventProperty.values()[column].name()));
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        // we're rendering strings only
        return String.class;
    }

    @Override
    public int getRowCount() {
        return locking(_lock.readLock(), _tableData::getSize);
    }

    @Override
    public Object getValueAt(final int row, final int col) {
        final LogEvent event = getEventAt(row);
        final LogEventProperty property = LogEventProperty.values()[col];
        return property.getValueProvider().apply(event);
    }

    /**
     * @param row the index of the table row to resolve the
     *                   appropriate {@link LogEvent} for
     * @return the {@link LogEvent} to be displayed by the table row at the
     *         given index
     */
    public LogEvent getEventAt(final int row) {
        return locking(_lock.readLock(), () -> _tableData.get(invert(row)));
    }

    /**
     * Invert the given index for accessing event buffer and table items.
     *
     * @param index the index to invert
     * @return the inverted index
     */
    public int invert(final int index) {
        return _tableData.getSize() - 1 - index;
    }

    /**
     * Add the given {@link LogEvent} instance to the receiver's buffer synchronously.
     *
     * @param event the {@link LogEvent} to add
     */
    public void put(@Nonnull final LogEvent event) {
        Objects.requireNonNull(event);

        // append the given event to the raw event buffer and update the table data
        final ModelUpdateType type = locking(_lock.writeLock(), () -> {
            _rawEvents.put(event);

            final ModelUpdateType update;
            if (_filter.test(event)) {
                update = _tableData.getSize() < _tableData.getCapacity() ? ModelUpdateType.INSERT : ModelUpdateType.SLIDE;
                _tableData.put(event);
            } else {
                update = ModelUpdateType.NONE;
            }

            return update;
        });

        // fire necessary events when table data was updated
        switch (type) {
            case INSERT:
                SwingUtilities.invokeLater(() -> fireTableRowsInserted(0, 0));
                break;
            case SLIDE:
                SwingUtilities.invokeLater(() -> {
                    fireTableRowsDeleted(_tableData.getCapacity() - 1, _tableData.getCapacity() - 1);
                    fireTableRowsInserted(0, 0);
                });
                break;
            default:
                break;
        }
    }

    /**
     * Clear all captured {@link LogEvent}s synchronously.
     */
    public void clear() {
        locking(_lock.writeLock(), () -> {
            _rawEvents.clear();
            _tableData.clear();
        });

        // make sure to notify listeners of data changes in EDT
        SwingUtilities.invokeLater(this::fireTableDataChanged);
    }

    /**
     * Apply the given {@link Consumer} to all currently displayed {@link LogEvent}s synchronously.
     *
     * @param consumer the {@link Consumer} to apply
     */
    public void forEach(final Consumer<LogEvent> consumer) {
        locking(_lock.readLock(), () -> {
            for (int i = _tableData.getSize() - 1; i >= 0; i--)  {
                consumer.accept(_tableData.get(i));
            }
        });
    }

    /**
     * Execute the given {@link Supplier} instance synchronized by the given {@link Lock}.
     *
     * @param lock     the {@link Lock} to acquire prior to calling the given {@link Supplier}
     * @param supplier the {@link Supplier} to call
     * @return the supplied value
     */
    private <T> T locking(final Lock lock, final Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Execute the given {@link Runnable} instance synchronized by the given {@link Lock}.
     *
     * @param lock     the {@link Lock} to acquire prior to calling the given {@link Runnable}
     * @param runnable the {@link Runnable} to call
     */
    private void locking(final Lock lock, final Runnable runnable) {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Enumeration defining model update types.
     */
    private enum ModelUpdateType {

        /**
         * Enumeration literal indicating that no update of table data occurred.
         */
        NONE,

        /**
         * Enumeration literal indicating that a {@link LogEvent} was inserted.
         */
        INSERT,

        /**
         * Enumeration literal indicating that the last {@link LogEvent} was removed and a new one inserted.
         */
        SLIDE
    }
}
