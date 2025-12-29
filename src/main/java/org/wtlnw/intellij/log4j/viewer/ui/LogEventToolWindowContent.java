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

import com.intellij.icons.AllIcons;
import com.intellij.ide.ActivityTracker;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColorizeProxyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.TextTransferable;
import groovyjarjarantlr4.v4.gui.JFileChooserConfirmOverwrite;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventFilter;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventFilterService;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventProperty;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventPropertyFilter;
import org.wtlnw.intellij.log4j.viewer.core.impl.JsonLogEventSupplierFactory;
import org.wtlnw.intellij.log4j.viewer.core.impl.LogEventServer;
import org.wtlnw.intellij.log4j.viewer.core.impl.SerializedLogEventSupplierFactory;
import org.wtlnw.intellij.log4j.viewer.core.impl.XmlLogEventSupplierFactory;
import org.wtlnw.intellij.log4j.viewer.i18n.LogEventBundle;
import org.wtlnw.intellij.log4j.viewer.settings.LogEventSettings;
import org.wtlnw.intellij.log4j.viewer.settings.LogEventSettingsService;
import org.wtlnw.intellij.log4j.viewer.ui.dialogs.LogEventDetailDialog;
import org.wtlnw.intellij.log4j.viewer.ui.dialogs.LogEventFilterDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link Disposable} implementation which builds the actual contents of the log viewer.
 */
public class LogEventToolWindowContent implements Disposable {

    @NonNls private static final String PLACE_TOOLBAR = "LogEventToolWindowToolbar";
    @NonNls private static final String PLACE_POPUP = "LogEventToolWindowPopupMenu";

    @NonNls public static final String TEXT_STOP = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TEXT_STOP"));
    @NonNls public static final String TEXT_START = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TEXT_START"));
    @NonNls public static final String TEXT_PAUSE = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TEXT_PAUSE"));
    @NonNls public static final String TEXT_CLEAR = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TEXT_CLEAR"));
    @NonNls public static final String TEXT_FILTER = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TEXT_FILTER"));
    @NonNls public static final String TEXT_EXPORT = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TEXT_EXPORT"));
    @NonNls public static final String KEY_EXPORT_FAILED = "KEY_EXPORT_FAILED";
    @NonNls public static final String TITLE_EXPORT_FAILED = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TITLE_EXPORT_FAILED"));
    @NonNls public static final String TEXT_COPY = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TEXT_COPY"));
    @NonNls public static final String TEXT_DETAILS = LogEventBundle.message(LogEventBundle.key(LogEventToolWindowContent.class, "TEXT_DETAILS"));

    /**
     * The {@link LogEventServer} responsible for capturing {@link LogEvent}s.
     */
    @Nullable private LogEventServer _server;

    /**
     * The {@link JTable} displaying captured {@link LogEvent}s.
     */
    @Nullable private JTable _table;

    /**
     * A (possibly empty) {@link List} of currently opened {@link LogEventDetailDialog}s.
     */
    private final List<LogEventDetailDialog> _dialogs = new ArrayList<>();

    /**
     * The flag indicating whether {@link #_table} should be updated to display
     * newly captured {@link LogEvent}s or not.
     */
    private volatile boolean _paused;

    /**
     * Create a {@link LogEventToolWindowContent}.
     *
     * @param content the {@link Content} to create the view for
     */
    public LogEventToolWindowContent(final Content content) {
        final LogEventSettings settings = LogEventSettingsService.getInstance().getState();
        final LogEventTableModel model = new LogEventTableModel(settings.buffer, createFilter());

        _table = createTable(model);
        _server = createServer(settings, event -> {
            if (!_paused) {
                model.put(event);
            }
        });

        // wrap the table in a scrollable pane
        final JComponent root = new JPanel();
        root.setLayout(new BorderLayout());

        // create a scroll pane without borders and draw one only on the left
        // to separate it from the view's toolbar.
        final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(_table, true);
        scrollPane.setBorder(JBUI.Borders.customLineLeft(JBColor.border()));
        root.add(scrollPane);

        // create a vertical toolbar to the left of the table
        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(PLACE_TOOLBAR, createToolbarActions(), false);
        toolbar.setTargetComponent(_table);
        root.add(toolbar.getComponent(), BorderLayout.WEST);

        // add popup menu to the table
        final ActionPopupMenu popup = ActionManager.getInstance().createActionPopupMenu(PLACE_POPUP, createPopupActions());
        popup.setTargetComponent(_table);
        _table.setComponentPopupMenu(popup.getComponent());

        // associate the content with our table and actions
        content.setComponent(root);
    }

    private LogEventFilter createFilter() {
        return LogEventFilterService.getInstance().getFilter();
    }

    private JBTable createTable(final LogEventTableModel model) {
        final JBTable table = new JBTable(model);
        table.getTableHeader().setReorderingAllowed(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowColumns(true);
        table.setDefaultRenderer(String.class, new LogEventTableCellRenderer());

        // open the details dialog upon double-click on a table item
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openDetailsDialog();
                }
            }
        });

        // open the details dialog upon hitting ENTER on a table item
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(final KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    openDetailsDialog();
                }
            }
        });

        return table;
    }

    private void openDetailsDialog() {
        final JTable table = Objects.requireNonNull(_table);
        final int selection = table.getSelectedRow();
        if (selection > -1) {
            final LogEventTableModel model = (LogEventTableModel) table.getModel();
            final LogEvent event = model.getEventAt(selection);

            // check if the dialog is already opened and if so, bring it to front
            for (final LogEventDetailDialog dialog : _dialogs) {
                if (dialog.getEvent() == event) {
                    dialog.getWindow().toFront();
                    return;
                }
            }

            // the dialog is not being displayed yet, create a new one
            final LogEventDetailDialog dialog = new LogEventDetailDialog(event);

            // listen to dialog closure to unregister
            dialog.getWindow().addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(final WindowEvent e) {
                    _dialogs.remove(dialog);
                }
            });

            // compute the dialog's location
            if (!_dialogs.isEmpty()) {
                final LogEventDetailDialog lastDialog = _dialogs.getLast();
                final Point lastLoc = lastDialog.getWindow().getLocation();
                final Insets lastInsets = lastDialog.getWindow().getInsets();
                dialog.setInitialLocationCallback(() -> new Point(lastLoc.x + lastInsets.top, lastLoc.y + lastInsets.top));
            }

            // register the dialog and display it
            _dialogs.add(dialog);
            dialog.show();
        }
    }

    private LogEventServer createServer(final LogEventSettings settings, final Consumer<LogEvent> consumer) {
        final LogEventServer server = new LogEventServer(settings.port, settings.timeout,
                List.of(
                        new SerializedLogEventSupplierFactory(),
                        new XmlLogEventSupplierFactory(),
                        new JsonLogEventSupplierFactory()),
                consumer);

        // start the server automatically if configured
        if (settings.autostart) {
            server.start();
        }

        return server;
    }

    private ActionGroup createToolbarActions() {
        return new DefaultActionGroup(
                createStartAction(),
                createPauseAction(),
                createClearAction(),
                createFilterAction(),
                createExportAction()
        );
    }

    private ActionGroup createPopupActions() {
        return new DefaultActionGroup(
                createCopyAction(),
                createDetailsAction()
        );
    }

    private AnAction createStartAction() {
        final ToggleAction action = new DumbAwareToggleAction() {

            final Supplier<Icon> iconSupplier = () -> {
                if (_server != null && _server.isRunning()) {
                    return AllIcons.Run.Stop;
                } else {
                    return AllIcons.Toolwindows.ToolWindowRun;
                }
            };

            final Supplier<String> textSupplier = () -> {
                if (_server != null && _server.isRunning()) {
                    return TEXT_STOP;
                } else {
                    return TEXT_START;
                }
            };

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public boolean isSelected(@NotNull final AnActionEvent e) {
                return _server != null && _server.isRunning();
            }

            @Override
            public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
                // no-op in disposed state
                if (_server == null) {
                    return;
                }

                if (state) {
                    _server.start();
                } else {
                    _server.stop();
                }
            }

            @Override
            public void update(@NotNull final AnActionEvent e) {
                super.update(e);

                final Presentation p = e.getPresentation();
                p.setIcon(iconSupplier.get());
                p.setText(textSupplier.get());
            }
        };

        // listen to server state changes and trigger action update
        Objects.requireNonNull(_server).addServerListener(state -> ActivityTracker.getInstance().inc());

        return action;
    }

    private AnAction createPauseAction() {
        return new DumbAwareToggleAction(TEXT_PAUSE, null, AllIcons.Actions.Pause) {

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public boolean isSelected(@NotNull final AnActionEvent e) {
                return _paused;
            }

            @Override
            public void setSelected(@NotNull final AnActionEvent e, final boolean state) {
                _paused = state;
            }
        };
    }

    private AnAction createClearAction() {
        return new DumbAwareAction(TEXT_CLEAR, null, AllIcons.Actions.ClearCash) {

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void actionPerformed(@NotNull final AnActionEvent e) {
                if (_table != null) {
                    final LogEventTableModel model = (LogEventTableModel) _table.getModel();
                    model.clear();
                }
            }
        };
    }

    private AnAction createFilterAction() {
        return new DumbAwareAction(TEXT_FILTER) {

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void actionPerformed(@NotNull final AnActionEvent e) {
                final LogEventTableModel model = (LogEventTableModel) (Objects.requireNonNull(_table).getModel());
                final LogEventFilterDialog dialog = new LogEventFilterDialog(model.getFilter());

                if (dialog.showAndGet()) {
                    model.setFilter(dialog.getFilter());
                    LogEventFilterService.getInstance().setFilter(model.getFilter());
                }
            }

            @Override
            public void update(@NotNull final AnActionEvent e) {
                super.update(e);

                final LogEventTableModel model = (LogEventTableModel) Objects.requireNonNull(_table).getModel();
                final LogEventFilter filter = model.getFilter();

                if (filter.getFilters().stream().anyMatch(LogEventPropertyFilter::isEnabled)) {
                    e.getPresentation().setIcon(new ColorizeProxyIcon.Simple(AllIcons.General.Filter, JBColor.red));
                } else {
                    e.getPresentation().setIcon(AllIcons.General.Filter);
                }
            }
        };
    }

    private AnAction createExportAction() {
        return new DumbAwareAction(TEXT_EXPORT, null, AllIcons.General.Export) {

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void actionPerformed(@NotNull final AnActionEvent e) {
                // do NOT use FileSaverDialog.save() because it always
                // returns null for non-existent files.
                final JFileChooser dialog = new JFileChooserConfirmOverwrite();
                dialog.setDialogTitle(getTemplateText());
                dialog.setMultiSelectionEnabled(false);
                dialog.setDialogType(JFileChooser.SAVE_DIALOG);
                dialog.setFileSelectionMode(JFileChooser.FILES_ONLY);

                if (JFileChooser.APPROVE_OPTION == dialog.showOpenDialog(null)) {
                    final File file = dialog.getSelectedFile();

                    final LogEventTableModel model = (LogEventTableModel) (Objects.requireNonNull(_table).getModel());
                    try (final PrintWriter out = new PrintWriter(file)) {
                        final Function<LogEvent, String> serializer = getEventSerializer();
                        model.forEach(event -> out.println(serializer.apply(event)));
                    } catch(final IOException ex) {
                        Messages.showErrorDialog(KEY_EXPORT_FAILED + ex.getMessage(), TITLE_EXPORT_FAILED);
                    }
                }
            }
        };
    }

    private AnAction createCopyAction() {
        return new DumbAwareAction(TEXT_COPY, null, AllIcons.Actions.Copy) {

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                final JTable table = Objects.requireNonNull(_table);
                final int selection = table.getSelectedRow();
                if (selection > -1) {
                    final LogEventTableModel model = (LogEventTableModel) table.getModel();
                    final LogEvent event = model.getEventAt(selection);
                    CopyPasteManager.getInstance().setContents(new TextTransferable(getEventSerializer().apply(event)));
                }
            }

            @Override
            public void update(final @NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(Objects.requireNonNull(_table).getSelectedRow() > -1);
            }
        };
    }

    private AnAction createDetailsAction() {
        return new DumbAwareAction(TEXT_DETAILS, null, AllIcons.Actions.Preview) {

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public void actionPerformed(final @NotNull AnActionEvent e) {
                openDetailsDialog();
            }

            @Override
            public void update(final @NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(Objects.requireNonNull(_table).getSelectedRow() > -1);
            }
        };
    }

    /**
     * @return the {@link Function} for serializing {@link LogEvent}s
     */
    private Function<LogEvent, String> getEventSerializer() {
        return e -> {
            final StringBuilder out = new StringBuilder();
            out.append(LogEventProperty.TIMESTAMP.getValueProvider().apply(e)).append("\t");
            out.append(LogEventProperty.LEVEL.getValueProvider().apply(e)).append("\t");
            out.append(LogEventProperty.CATEGORY.getValueProvider().apply(e)).append(" ");
            out.append(LogEventProperty.MESSAGE.getValueProvider().apply(e));

            @SuppressWarnings("deprecation")
            final ThrowableProxy proxy = e.getThrownProxy();
            if (proxy != null) {
                out.append(System.lineSeparator());

                final String stacktrace = proxy.getCauseStackTraceAsString("");
                out.append(stacktrace);
            }

            return out.toString();
        };
    }

    @Override
    public void dispose() {
        // stop the server if it's running and release server reference
        if (Objects.requireNonNull(_server).isRunning()) {
            _server.stop();
            _server = null;
        }

        // close all open dialogs
        _dialogs.forEach(dialog -> dialog.close(LogEventDetailDialog.OK_EXIT_CODE));

        // release the table reference
        _table = null;
    }
}
