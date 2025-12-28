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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAwareToggleAction;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventFilter;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventProperty;
import org.wtlnw.intellij.log4j.viewer.core.filter.LogEventPropertyFilter;
import org.wtlnw.intellij.log4j.viewer.i18n.LogEventBundle;
import org.wtlnw.intellij.log4j.viewer.settings.LogEventConfigurable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A {@link DialogWrapper} allowing users to customize the filters
 * to be applied for each column of the log event table.
 */
public class LogEventFilterDialog extends DialogWrapper {

    @NonNls private static final String ENABLED = "enabled";
    @NonNls private static final String MATCH_CASE = "matchCase";
    @NonNls private static final String REGEX = "regex";
    @NonNls private static final String WHOLE_WORD = "wholeWord";
    @NonNls private static final String INVERSE = "inverse";
    @NonNls private static final String PATTERN = "pattern";

    @NonNls private static final String TITLE = LogEventBundle.message(LogEventBundle.key(LogEventFilterDialog.class,"TITLE"));
    @NonNls private static final String TEXT_ENABLED_ACTIVE = LogEventBundle.message(LogEventBundle.key(LogEventFilterDialog.class,"TEXT_ENABLED_ACTIVE"));
    @NonNls private static final String TEXT_ENABLED_INACTIVE = LogEventBundle.message(LogEventBundle.key(LogEventFilterDialog.class,"TEXT_ENABLED_INACTIVE"));
    @NonNls private static final String TEXT_MATCH_CASE = LogEventBundle.message(LogEventBundle.key(LogEventFilterDialog.class,"TEXT_MATCH_CASE"));
    @NonNls private static final String TEXT_REGEX = LogEventBundle.message(LogEventBundle.key(LogEventFilterDialog.class,"TEXT_REGEX"));
    @NonNls private static final String TEXT_WHOLE_WORD = LogEventBundle.message(LogEventBundle.key(LogEventFilterDialog.class,"TEXT_WHOLE_WORD"));
    @NonNls private static final String TEXT_INVERSE = LogEventBundle.message(LogEventBundle.key(LogEventFilterDialog.class,"TEXT_INVERSE"));

    // We can't use a copy of the filter because Actions are not allowed
    // to have fields as of IntelliJ's SDK documentation.
    // Hence, we have to keep the contents of the original filter in a map.
    private final Map<LogEventProperty, Map<String, Object>> _filters = new HashMap<>();

    /**
     * The {@link List} of {@link Supplier}s which are called in {@link #doValidateAll()}
     * in order to check user input.
     */
    private final List<Supplier<ValidationInfo>> _validators = new ArrayList<>();

    /**
     * Create a {@link LogEventFilterDialog} modifying the given {@link LogEventFilter}.
     *
     * @param filter the {@link LogEventFilter} to modify
     */
    public LogEventFilterDialog(final LogEventFilter filter) {
        super(false);

        // copy the filter's values to an internal transient map
        for (final LogEventPropertyFilter src : filter.getFilters()) {
            final Map<String, Object> tgt = _filters.computeIfAbsent(src.getProperty(), p -> new HashMap<>());
            tgt.put(ENABLED, src.isEnabled());
            tgt.put(MATCH_CASE, src.isMatchCase());
            tgt.put(REGEX, src.isRegularExpression());
            tgt.put(WHOLE_WORD, src.isWholeWord());
            tgt.put(INVERSE, src.isInverse());
            tgt.put(PATTERN, src.getPattern());
        }

        // initialize the dialog
        setTitle(TITLE);
        init();
        initValidation();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        final LogEventProperty[] properties = LogEventProperty.values();

        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(UIUtil.LARGE_VGAP, 0, 0, 0));
        panel.setLayout(new GridLayout(properties.length, 1));

        for (final LogEventProperty property : properties) {
            panel.add(createPropertyGroup(property));
        }

        return panel;
    }

    private Component createPropertyGroup(final LogEventProperty property) {
        final Map<String, Object> filter = _filters.get(property);

        final JPanel group = new JPanel();
        group.setLayout(new BorderLayout(0, UIUtil.DEFAULT_VGAP));

        // add separator instead of title border to preserve consistent UI look and feel
        group.add(LogEventConfigurable.separator(LogEventBundle.message(property)), BorderLayout.NORTH);

        // add a spacer to the left and bottom of the panel
        final Border spacerBorder = BorderFactory.createEmptyBorder(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP * 2, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP);
        final JPanel spacerLeft = new JPanel();
        spacerLeft.setBorder(spacerBorder);
        group.add(spacerLeft, BorderLayout.WEST);
        final JPanel spacerBottom = new JPanel();
        spacerBottom.setBorder(spacerBorder);
        group.add(spacerBottom, BorderLayout.SOUTH);

        final JBTextField input = new JBTextField();
        input.setColumns(48);
        input.setText((String) filter.get(PATTERN));
        input.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent e) {
                filter.put(PATTERN, input.getText());
            }
        });
        group.add(input);

        final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(LogEventBundle.message(property), new DefaultActionGroup(
                createEnableAction(filter),
                Separator.create(),
                createMatchCaseAction(filter),
                createRegexAction(filter),
                createWholeWordAction(filter),
                Separator.create(),
                createInverseAction(filter)
        ), true);
        toolbar.setTargetComponent(group);
        group.add(toolbar.getComponent(), BorderLayout.EAST);

        // add validator for this property
        _validators.add(() -> {
            if ((Boolean) filter.get(REGEX)) {
                try {
                    Pattern.compile(input.getText());
                } catch(PatternSyntaxException ex) {
                    return new ValidationInfo(ex.getMessage(), input);
                }
            }

            return null;
        });

        return group;
    }

    @Override
    protected @NotNull List<ValidationInfo> doValidateAll() {
        final List<ValidationInfo> result = super.doValidateAll();

        for (final Supplier<ValidationInfo> validator : _validators) {
            final ValidationInfo info = validator.get();
            if (info != null) {
                result.add(info);
            }
        }

        return result;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    private AnAction createEnableAction(final @UnknownNullability Map<String, Object> filter) {
        final Function<Boolean, String> textFunction = enabled -> enabled ? TEXT_ENABLED_ACTIVE : TEXT_ENABLED_INACTIVE;
        final Function<Boolean, Icon> iconFunction = enabled -> enabled ? AllIcons.Actions.Pause : AllIcons.Toolwindows.ToolWindowRun;

        return createToggleAction(() -> (Boolean) filter.get(ENABLED), enabled -> filter.put(ENABLED, enabled), (a, e) -> {
            final boolean selected = a.isSelected(e);
            e.getPresentation().setText(textFunction.apply(selected));
            e.getPresentation().setIcon(iconFunction.apply(selected));
        });
    }

    private AnAction createMatchCaseAction(final @UnknownNullability Map<String, Object> filter) {
        final ToggleAction action = createToggleAction(() -> (Boolean) filter.get(MATCH_CASE), matchCase -> filter.put(MATCH_CASE, matchCase), null);
        action.getTemplatePresentation().setText(TEXT_MATCH_CASE);
        action.getTemplatePresentation().setIcon(AllIcons.Actions.MatchCase);
        return action;
    }

    private AnAction createRegexAction(final @UnknownNullability Map<String, Object> filter) {
        final ToggleAction action = createToggleAction(() -> (Boolean) filter.get(REGEX), regex -> filter.put(REGEX, regex), null);
        action.getTemplatePresentation().setText(TEXT_REGEX);
        action.getTemplatePresentation().setIcon(AllIcons.Actions.Regex);
        return action;
    }

    private AnAction createWholeWordAction(final @UnknownNullability Map<String, Object> filter) {
        final ToggleAction action = createToggleAction(() -> (Boolean) filter.get(WHOLE_WORD), wholeWord -> filter.put(WHOLE_WORD, wholeWord), null);
        action.getTemplatePresentation().setText(TEXT_WHOLE_WORD);
        action.getTemplatePresentation().setIcon(AllIcons.Actions.Words);
        return action;
    }

    private AnAction createInverseAction(final @UnknownNullability Map<String, Object> filter) {
        final ToggleAction action = createToggleAction(() -> (Boolean) filter.get(INVERSE), inverse -> filter.put(INVERSE, inverse), null);
        action.getTemplatePresentation().setText(TEXT_INVERSE);
        action.getTemplatePresentation().setIcon(AllIcons.Actions.Refresh);
        return action;
    }

    private ToggleAction createToggleAction(final Supplier<Boolean> getter, final Consumer<Boolean> setter, @Nullable final BiConsumer<ToggleAction, AnActionEvent> updater) {
        return new DumbAwareToggleAction() {

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }

            @Override
            public boolean isSelected(@NotNull final AnActionEvent e) {
                return getter.get();
            }

            @Override
            public void setSelected(@NotNull AnActionEvent e, final boolean state) {
                setter.accept(state);
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);

                if (updater != null) {
                    updater.accept(this, e);
                }
            }
        };
    }

    /**
     * @return a new {@link LogEventFilter} instance reflecting user input
     */
    public LogEventFilter getFilter() {
        final LogEventFilter filter = new LogEventFilter();

        for (final LogEventPropertyFilter tgt : filter.getFilters()) {
            final Map<String, Object> src = _filters.get(tgt.getProperty());
            tgt.setEnabled((Boolean) src.get(ENABLED));
            tgt.setMatchCase((Boolean) src.get(MATCH_CASE));
            tgt.setRegularExpression((Boolean) src.get(REGEX));
            tgt.setWholeWord((Boolean) src.get(WHOLE_WORD));
            tgt.setInverse((Boolean) src.get(INVERSE));
            tgt.setPattern((String) src.get(PATTERN));
        }

        return filter;
    }
}
