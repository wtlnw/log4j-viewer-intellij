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

package org.wtlnw.intellij.log4j.viewer.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.ColorChooserService;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.wtlnw.intellij.log4j.viewer.i18n.LogEventBundle;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

/**
 * A {@link Configurable} implementation providing a form allowing users to view/modify log viewer settings.
 */
public class LogEventConfigurable implements Configurable {

    private static final String TITLE = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TITLE"));
    private static final String TEXT_MESSAGE = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_MESSAGE"));
    private static final String TEXT_AUTOSTART = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_AUTOSTART"));
    private static final String TEXT_SERVER_SETTINGS = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_SERVER_SETTINGS"));
    private static final String TEXT_TABLE_SETTINGS = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_TABLE_SETTINGS"));
    private static final String TEXT_COLOR_SETTINGS = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_COLOR_SETTINGS"));
    private static final String TEXT_PORT = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_PORT"));
    private static final String TEXT_TIMEOUT = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_TIMEOUT"));
    private static final String TEXT_BUFFER = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_BUFFER"));
    private static final String TEXT_DEBUG_COLOR = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_DEBUG_COLOR"));
    private static final String TEXT_INFO_COLOR = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_INFO_COLOR"));
    private static final String TEXT_WARNING_COLOR = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_WARNING_COLOR"));
    private static final String TEXT_ERROR_COLOR = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_ERROR_COLOR"));
    private static final String TEXT_FATAL_COLOR = LogEventBundle.message(LogEventBundle.key(LogEventConfigurable.class, "TEXT_FATAL_COLOR"));

    private final IntegerField _port = withDefaultColumns(new IntegerField(null, 0, 65535));
    private final IntegerField _timeout = withDefaultColumns(new IntegerField(null, 100, 1_000_000));
    private final JBCheckBox _autostart = new JBCheckBox(TEXT_AUTOSTART);

    private final IntegerField _buffer = withDefaultColumns(new IntegerField(null, 128, Integer.MAX_VALUE));
    private final JTextField _colorDebug = withColorChooser(new JTextField());
    private final JTextField _colorInfo = withColorChooser(new JTextField());
    private final JTextField _colorWarn = withColorChooser(new JTextField());
    private final JTextField _colorError = withColorChooser(new JTextField());
    private final JTextField _colorFatal = withColorChooser(new JTextField());

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return TITLE;
    }

    @Override
    public @Nullable JComponent createComponent() {
        final JPanel form = new JPanel(new GridBagLayout());
        final GridBag constraints = new GridBag()
                .setDefaultWeightX(1.0)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultAnchor(GridBagConstraints.NORTHWEST);

        form.add(new JLabel(TEXT_MESSAGE), constraints.nextLine());
        form.add(separator(TEXT_SERVER_SETTINGS), constraints.nextLine().insetTop(UIUtil.LARGE_VGAP * 2));
        form.add(createServerFields(), constraints.nextLine().insetLeft(UIUtil.DEFAULT_HGAP * 2));
        form.add(separator(TEXT_TABLE_SETTINGS), constraints.nextLine().insetTop(UIUtil.LARGE_VGAP * 2));
        form.add(createTableFields(), constraints.nextLine().insetLeft(UIUtil.DEFAULT_HGAP * 2));
        form.add(separator(TEXT_COLOR_SETTINGS), constraints.nextLine().insetTop(UIUtil.LARGE_VGAP * 2));
        form.add(createColorFields(), constraints.nextLine().insetLeft(UIUtil.DEFAULT_HGAP * 2).weighty(1.0));

        return form;
    }

    private JPanel createServerFields() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBag constraints = newGridBagConstraints();

        panel.add(new JLabel(TEXT_PORT), constraints.nextLine().next());
        panel.add(_port, constraints.next());

        panel.add(new JLabel(TEXT_TIMEOUT), constraints.nextLine().next());
        panel.add(_timeout, constraints.next());

        panel.add(_autostart, constraints.nextLine().coverLine());

        return panel;
    }

    private JPanel createTableFields() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBag constraints = newGridBagConstraints();

        panel.add(new JLabel(TEXT_BUFFER), constraints.nextLine().next());
        panel.add(_buffer, constraints.next());

        return panel;
    }

    private JPanel createColorFields() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBag constraints = newGridBagConstraints();

        panel.add(new JLabel(TEXT_DEBUG_COLOR), constraints.nextLine().next());
        panel.add(_colorDebug, constraints.next());

        panel.add(new JLabel(TEXT_INFO_COLOR), constraints.nextLine().next());
        panel.add(_colorInfo, constraints.next());

        panel.add(new JLabel(TEXT_WARNING_COLOR), constraints.nextLine().next());
        panel.add(_colorWarn, constraints.next());

        panel.add(new JLabel(TEXT_ERROR_COLOR), constraints.nextLine().next());
        panel.add(_colorError, constraints.next());

        panel.add(new JLabel(TEXT_FATAL_COLOR), constraints.nextLine().next());
        panel.add(_colorFatal, constraints.next());

        return panel;
    }

    private GridBag newGridBagConstraints() {
        return new GridBag()
                .setDefaultWeightX(0, 0.0)
                .setDefaultWeightX(1, 1.0)
                .setDefaultInsets(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP, 0, 0)
                .setDefaultAnchor(GridBagConstraints.WEST)
                .setDefaultFill(GridBagConstraints.NONE);
    }

    private <T extends JTextField> T withDefaultColumns(final T field) {
        field.setColumns(10);
        field.setHorizontalAlignment(JTextField.RIGHT);
        return field;
    }

    private JTextField withColorChooser(final JTextField field) {
        final EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        final String fontName = scheme.getEditorFontName();
        final int fontSize = scheme.getEditorFontSize();
        final Font font = new Font(fontName, Font.PLAIN, fontSize);

        field.setFont(font);
        field.setColumns(8);
        field.setEditable(false);
        field.setFocusable(false);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent event) {
                if (MouseEvent.BUTTON1 == event.getButton()) {
                    ColorChooserService.getInstance().showPopup(null, getColor(field), (color, object) -> field.setText(UIUtil.colorToHex(color)));
                }
            }
        });
        field.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(final @NonNull DocumentEvent event) {
                if (!field.getText().isBlank()) {
                    field.setForeground(getColor(field));
                }
            }
        });
        return field;
    }

    private Color getColor(final JTextField field) {
        return new Color(Integer.parseInt(field.getText(), 16));
    }

    @Override
    public boolean isModified() {
        final LogEventSettings settings = LogEventSettingsService.getInstance().getState();

        boolean changed = false;
        changed |= settings.port != _port.getValue();
        changed |= settings.timeout != _timeout.getValue();
        changed |= settings.autostart != _autostart.isSelected();
        changed |= settings.buffer != _buffer.getValue();
        changed |= !Objects.equals(settings.colorDebug, _colorDebug.getText());
        changed |= !Objects.equals(settings.colorInfo, _colorInfo.getText());
        changed |= !Objects.equals(settings.colorWarn, _colorWarn.getText());
        changed |= !Objects.equals(settings.colorError, _colorError.getText());
        changed |= !Objects.equals(settings.colorFatal, _colorFatal.getText());

        return changed;
    }

    @Override
    public void apply() {
        final LogEventSettings settings = LogEventSettingsService.getInstance().getState();

        settings.port = _port.getValue();
        settings.timeout = _timeout.getValue();
        settings.autostart = _autostart.isSelected();

        settings.buffer = _buffer.getValue();
        settings.colorDebug = _colorDebug.getText();
        settings.colorInfo = _colorInfo.getText();
        settings.colorWarn = _colorWarn.getText();
        settings.colorError = _colorError.getText();
        settings.colorFatal = _colorFatal.getText();

        ApplicationManager.getApplication().getMessageBus().syncPublisher(LogEventSettingsListener.CHANGE_SETTINGS_TOPIC).settingsChanged();
    }

    @Override
    public void reset() {
        final LogEventSettings settings = LogEventSettingsService.getInstance().getState();
        _port.setValue(settings.port);
        _timeout.setValue(settings.timeout);
        _autostart.setSelected(settings.autostart);

        _buffer.setValue(settings.buffer);
        _colorDebug.setText(settings.colorDebug);
        _colorInfo.setText(settings.colorInfo);
        _colorWarn.setText(settings.colorWarn);
        _colorError.setText(settings.colorError);
        _colorFatal.setText(settings.colorFatal);

        ApplicationManager.getApplication().getMessageBus().syncPublisher(LogEventSettingsListener.CHANGE_SETTINGS_TOPIC).settingsChanged();
    }

    /**
     * @param text separator text
     * @return a {@link JPanel} consisting of a {@link JLabel} displaying the given text and {@link JSeparator}
     */
    public static JPanel separator(@NotNull final String text) {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBag constraints = new GridBag()
                .setDefaultWeightX(0.0)
                .setDefaultPaddingX(UIUtil.DEFAULT_HGAP)
                .setDefaultAnchor(GridBagConstraints.WEST)
                .setDefaultFill(GridBagConstraints.NONE);

        panel.add(new JLabel(text), constraints.nextLine().next());
        panel.add(new JSeparator(), constraints.next().weightx(1.0).fillCellHorizontally().coverLine());

        return panel;
    }
}
