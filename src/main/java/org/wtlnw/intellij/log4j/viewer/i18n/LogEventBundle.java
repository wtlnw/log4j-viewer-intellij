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

package org.wtlnw.intellij.log4j.viewer.i18n;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

/**
 * Central class for localization purposes.
 */
public final class LogEventBundle {

    /**
     * The name of the message bundle (see plugin.xml for definition).
     */
    private static final String BUNDLE = "messages.LogEventBundle";

    /**
     * The singleton instance.
     */
    private static final DynamicBundle INSTANCE = new DynamicBundle(LogEventBundle.class, BUNDLE);

    /**
     * Create a {@link LogEventBundle} instance.
     */
    private LogEventBundle() {
        // enforce singleton pattern
    }

    /**
     * @param clazz  the {@link Class} defining localization context
     * @param suffix the class-local key
     * @return the localization key to be used for localization of the given suffix in the context of the given class
     */
    public static @NotNull @Nls String key(@NotNull final Class<?> clazz, @NotNull final String suffix) {
        return clazz.getName() + "." + suffix;
    }

    /**
     * @param key    the key {@link String} to localize
     * @param params a (possibly empty) array of message parameters
     * @return the localized message of the given key
     */
    public static @NotNull @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) final String key, @NotNull final Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    /**
     * @param literal the {@link Enum} literal to resolve the localized name for
     * @return the localized name of the given enumeration literal
     */
    public static @NotNull @Nls String message(@NotNull Enum<?> literal) {
        return INSTANCE.getMessage(literal.getDeclaringClass().getName() + "." + literal.name());
    }
}
