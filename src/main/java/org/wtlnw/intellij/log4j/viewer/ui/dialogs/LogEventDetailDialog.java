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

package org.wtlnw.intellij.log4j.viewer.ui.dialogs;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Splitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventProperty;
import org.wtlnw.intellij.log4j.viewer.i18n.LogEventBundle;
import org.wtlnw.intellij.log4j.viewer.settings.LogEventConfigurable;

import javax.swing.*;
import java.awt.*;

/**
 * A {@link DialogWrapper} displaying additional information for a {@link LogEvent} instance.
 */
public class LogEventDetailDialog extends DialogWrapper {

    private static final String TITLE_KEY = LogEventBundle.key(LogEventDetailDialog.class, "TITLE");

    /**
     * @see #getEvent()
     */
    private final LogEvent _event;

    /**
     * Create a {@link LogEventDetailDialog} displaying the given event's details.
     *
     * @param event see {@link #getEvent()}
     */
    public LogEventDetailDialog(final LogEvent event) {
        super(false);

        _event = event;

        // initialize the dialog
        setModal(false);
        setTitle(LogEventBundle.message(TITLE_KEY, LogEventProperty.LEVEL.getValueProvider().apply(_event), LogEventProperty.TIMESTAMP.getValueProvider().apply(_event)));
        init();
    }

    public LogEvent getEvent() {
        return _event;
    }

    @Override
    protected Action @NotNull [] createActions() {
        final Action action = getOKAction();
        action.putValue(DialogWrapper.DEFAULT_ACTION, true);
        action.putValue(DialogWrapper.FOCUSED_ACTION, true);

        return new Action[]{action};
    }

    @Override
    public @Nullable Dimension getInitialSize() {
        return JBUI.DialogSizes.large();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        final Splitter root = new Splitter(true);
        root.setFirstComponent(createGeneralFields());

        final String throwable = LogEventProperty.THROWABLE.getValueProvider().apply(_event);
        if (!throwable.isEmpty()) {
            root.setSecondComponent(createStackTrace());
        }

        return root;
    }

    private JComponent createStackTrace() {
        @SuppressWarnings("deprecation")
        final ThrowableProxy proxy = _event.getThrownProxy();

        final JPanel panel = new JPanel(new BorderLayout(0, UIUtil.DEFAULT_VGAP));
        panel.setBorder(BorderFactory.createEmptyBorder(UIUtil.LARGE_VGAP, 0, 0, 0));
        panel.add(LogEventConfigurable.separator(LogEventBundle.message(LogEventProperty.THROWABLE)), BorderLayout.NORTH);

        final JBTextArea textArea = new JBTextArea();
        textArea.setText(proxy.getExtendedStackTraceAsString());
        textArea.setEditable(false);
        panel.add(withBorder(ScrollPaneFactory.createScrollPane(textArea)), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGeneralFields() {
        final JPanel panel = new JPanel(new BorderLayout(0, UIUtil.LARGE_VGAP));

        final JPanel fields = new JPanel(new GridBagLayout());
        final GridBag constraints = new GridBag()
                .setDefaultWeightX(1.0)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultAnchor(GridBagConstraints.WEST);

        createGeneralField(fields, LogEventProperty.CATEGORY, constraints);
        createGeneralField(fields, LogEventProperty.LEVEL, constraints);
        createGeneralField(fields, LogEventProperty.TIMESTAMP, constraints);
        panel.add(fields, BorderLayout.NORTH);

        final JPanel msgPanel = new JPanel(new BorderLayout(0, UIUtil.DEFAULT_VGAP));
        msgPanel.add(LogEventConfigurable.separator(LogEventBundle.message(LogEventProperty.MESSAGE)), BorderLayout.NORTH);
        final JTextArea msgField = new JTextArea();
        msgField.setText(LogEventProperty.MESSAGE.getValueProvider().apply(_event));
        msgField.setFont(JBUI.Fonts.label());
        msgField.setForeground(JBUI.CurrentTheme.Label.foreground());
        msgField.setEditable(false);
        msgPanel.add(withBorder(ScrollPaneFactory.createScrollPane(msgField)), BorderLayout.CENTER);
        panel.add(msgPanel, BorderLayout.CENTER);

        return panel;
    }

    private void createGeneralField(final JPanel parent, final LogEventProperty property, final GridBag constraints) {
        final JLabel label = new JLabel(LogEventBundle.message(property) + ":");
        parent.add(label, constraints.nextLine().next().weightx(0.0));

        final JTextField text = new JTextField();
        text.setText(property.getValueProvider().apply(_event));
        text.setEditable(false);
        parent.add(text, constraints.next());
    }

    private <T extends JComponent> T withBorder(final T component) {
        component.setBorder(JBUI.Borders.compound(JBUI.Borders.customLine(JBUI.CurrentTheme.Editor.BORDER_COLOR), JBUI.Borders.empty(4)));
        return component;
    }
}
