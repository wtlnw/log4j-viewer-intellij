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

import com.intellij.ui.ColorUtil;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.spi.StandardLevel;
import org.wtlnw.intellij.log4j.viewer.settings.LogEventSettings;
import org.wtlnw.intellij.log4j.viewer.settings.LogEventSettingsService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link DefaultTableCellRenderer} specialization using custom colors for rendering
 * {@link LogEvent}s based on their level.
 */
public class LogEventTableCellRenderer extends DefaultTableCellRenderer {

    /**
     * A {@link Map} of {@link StandardLevel} to the configured {@link Color}
     * to be used for {@link LogEvent} text rendering.
     */
    private final Map<StandardLevel, Color> _colors = new ConcurrentHashMap<>();

    /**
     * Create a {@link LogEventTableCellRenderer}.
     */
    public LogEventTableCellRenderer() {
        loadColors();
    }

    /**
     * Load the configured colors from {@link LogEventSettingsService}.
     */
    public void loadColors() {
        final LogEventSettings settings = LogEventSettingsService.getInstance().getState();

        _colors.put(StandardLevel.DEBUG, ColorUtil.fromHex(settings.colorDebug));
        _colors.put(StandardLevel.INFO, ColorUtil.fromHex(settings.colorInfo));
        _colors.put(StandardLevel.WARN, ColorUtil.fromHex(settings.colorWarn));
        _colors.put(StandardLevel.ERROR, ColorUtil.fromHex(settings.colorError));
        _colors.put(StandardLevel.FATAL, ColorUtil.fromHex(settings.colorFatal));
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
        final Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        final LogEventTableModel model = (LogEventTableModel) table.getModel();
        final LogEvent event = model.getEventAt(row);
        final Color color = _colors.get(event.getLevel().getStandardLevel());

        // override default color behavior for text if applicable
        if (color != null) {
            result.setForeground(color);
        }

        return result;
    }
}
