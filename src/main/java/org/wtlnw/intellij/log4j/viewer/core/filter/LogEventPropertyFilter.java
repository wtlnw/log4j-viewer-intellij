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

package org.wtlnw.intellij.log4j.viewer.core.filter;

import org.apache.logging.log4j.core.LogEvent;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Instances of this class provide a description for filtering {@link LogEvent}s according to their properties.
 * 
 * <p>
 * Note: this implementation treats {@code null} values as empty strings.
 * </p>
 */
public class LogEventPropertyFilter implements Predicate<LogEvent> {

	/**
	 * @see #getProperty()
	 */
	private final LogEventProperty _property;

	/**
	 * @see #isEnabled()
	 */
	private boolean _enabled = false;
	
	/**
	 * @see #isMatchCase()
	 */
	private boolean _matchCase = false;

	/**
	 * @see #isRegularExpression()
	 */
	private boolean _regex = false;

	/**
	 * @see #isWholeWord()
	 */
	private boolean _wholeWord = false;

	/**
	 * @see #isInverse()
	 */
	private boolean _inverse = false;

	/**
	 * The pattern to be used for filtering.
	 */
	private Pattern _pattern;
	
	/**
	 * Create a {@link LogEventPropertyFilter} which is disabled by default.
	 * 
	 * @param property see {@link #getProperty()}
	 */
	public LogEventPropertyFilter(final LogEventProperty property) {
		_property = Objects.requireNonNull(property);
		_pattern = build("");
	}
	
	/**
	 * @return the {@link LogEventProperty} to filter
	 */
	public LogEventProperty getProperty() {
		return _property;
	}

	/**
	 * @return {@code true} if this filter is enabled, {@code false} otherwise
	 * @see #test(LogEvent)
	 */
	public boolean isEnabled() {
		return _enabled;
	}

	/**
	 * Setter for {@link #isEnabled()}.
	 * 
	 * @param enabled see {@link #isEnabled()}
	 * @return this {@link LogEventPropertyFilter} for convenient call chaining
	 * @see #test(LogEvent)
	 */
	public LogEventPropertyFilter setEnabled(final boolean enabled) {
		_enabled = enabled;

		return this;
	}

	/**
	 * @return {@code true} to explicitly match character's case, {@code false} for
	 *         case-insensitive filtering
	 */
	public boolean isMatchCase() {
		return _matchCase;
	}

	/**
	 * Setter for {@link #isMatchCase()}.
	 * 
	 * @param matchCase see {@link #isMatchCase()}
	 * @return this {@link LogEventPropertyFilter} for convenient call chaining
	 */
	public LogEventPropertyFilter setMatchCase(final boolean matchCase) {
		_matchCase = matchCase;
		
		// re-build the pattern if it had already been built
		if (_pattern != null) {
			_pattern = build(_pattern.pattern());
		}
		
		return this;
	}

	/**
	 * @return {@code true} to interpret {@link #getPattern()} as a regular expression,
	 *         {@code false} otherwise
	 */
	public boolean isRegularExpression() {
		return _regex;
	}

	/**
	 * Setter for {@link #isRegularExpression()}.
	 * 
	 * @param regex see {@link #isRegularExpression()}
	 * @return this {@link LogEventPropertyFilter} for convenient call chaining
	 */
	public LogEventPropertyFilter setRegularExpression(final boolean regex) {
		_regex = regex;

		// re-build the pattern if it had already been built
		if (_pattern != null) {
			_pattern = build(_pattern.pattern());
		}

		return this;
	}

	/**
	 * @return {@code true} to require input to match the entire {@link #getPattern()},
	 *         {@code false} to use 'contain' semantics
	 */
	public boolean isWholeWord() {
		return _wholeWord;
	}

	/**
	 * Setter for {@link #isWholeWord()}.
	 * 
	 * @param wholeWord see {@link #isWholeWord()}
	 * @return this {@link LogEventPropertyFilter} for convenient call chaining
	 */
	public LogEventPropertyFilter setWholeWord(final boolean wholeWord) {
		_wholeWord = wholeWord;

		return this;
	}

	/**
	 * @return {@code true} to invert filter result, {@code false} to use the filter
	 *         result as is
	 */
	public boolean isInverse() {
		return _inverse;
	}

	/**
	 * Setter for {@link #isInverse()}.
	 * 
	 * @param inverse see {@link #isInverse()}
	 * @return this {@link LogEventPropertyFilter} for convenient call chaining
	 */
	public LogEventPropertyFilter setInverse(final boolean inverse) {
		_inverse = inverse;

		return this;
	}

	/**
	 * @return the filter {@link String}
	 */
	public String getPattern() {
		return _pattern.pattern();
	}

	/**
	 * Setter for {@link #getPattern()}.
	 * 
	 * @param pattern see {@link #getPattern()}
	 * @return this {@link LogEventPropertyFilter} for convenient call chaining
	 */
	public LogEventPropertyFilter setPattern(final String pattern) {
		_pattern = build(Objects.requireNonNull(pattern));

		return this;
	}

	private Pattern build(final String pattern) {
		int flags = 0;
		
		// disable case sensitivity
		if (!_matchCase) {
			flags |= Pattern.CASE_INSENSITIVE;
		}

		// disable meta character parsing
		if (!_regex) {
			flags |= Pattern.LITERAL;
		}
		
		return Pattern.compile(pattern, flags);
	}

	/**
	 * @return {@code true} if the given {@link LogEvent} satisfies the receiver's
	 *         criteria, {@code false} otherwise. Disabled receiver's always return
	 *         {@code true}.
	 */
	@Override
	public boolean test(final LogEvent event) {
		// fast-path return for disabled filters
		if (!_enabled) {
			return true;
		}
		
		final String value = _property.getValueProvider().apply(event);
		final Matcher matcher = _pattern.matcher(value == null ? "" : value);
		final boolean result = _wholeWord ? matcher.matches() : matcher.find();
		
		return _inverse ? !result : result;
	}

	/**
	 * Copy all settings from source to target.
	 * 
	 * @param src the {@link LogEventPropertyFilter} to copy the settings from
	 * @param tgt the {@link LogEventPropertyFilter} to copy the settings to
	 * @throws IllegalArgumentException if the given filters have different
	 *                                  {@link LogEventProperty}s
	 */
	public static void copy(final LogEventPropertyFilter src, final LogEventPropertyFilter tgt) {
		if (src.getProperty() != tgt.getProperty()) {
			throw new IllegalArgumentException(String.format("Cannot copy settings from %s to %s", 
					src.getProperty().getName(), tgt.getProperty().getName()));
		}

		tgt.setEnabled(src.isEnabled());
		tgt.setInverse(src.isInverse());
		tgt.setMatchCase(src.isMatchCase());
		tgt.setPattern(src.getPattern());
		tgt.setRegularExpression(src.isRegularExpression());
		tgt.setWholeWord(src.isWholeWord());
	}
}
