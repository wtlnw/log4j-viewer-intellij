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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jspecify.annotations.NonNull;

/**
 * The {@link ToolWindowFactory} implementation responsible for initializing the viewer contents.
 */
public final class LogEventToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NonNull final Project project, final @NonNull ToolWindow window) {
        final Content content = ContentFactory.getInstance().createContent(null, null, false);
        content.setDisposer(new LogEventToolWindowContent(content));
        window.getContentManager().addContent(content);
    }
}
