/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.antiliacode.widgets.breadcrumb;

import java.util.Locale;

import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.LookPart;

/**
 * The separators a {@link Breadcrumb} draws between its crumbs. The style sheet draws them, by a
 * class of the breadcrumb and never by a {@code style} attribute, so that a Content Security Policy
 * without {@code 'unsafe-inline'} lets them through, and gives them an empty alternative text, so
 * that screen readers skip them. In Bootstrap's look they are Bootstrap's divider.
 *
 * @since 11.0.0
 */
public enum BreadcrumbSeparator
{
	/** A chevron, ›. */
	CHEVRON,

	/** A slash, /. */
	SLASH,

	/** An arrow, →. */
	ARROW;

	/**
	 * @return the name of the separator in the script
	 */
	public String cssName()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return the CSS class drawing the separator, that of its part in {@link Look#ANTILIA}
	 */
	public String getCssClass()
	{
		return Look.ANTILIA.getCssClass(getPart());
	}

	/**
	 * @return the part of a look the breadcrumb is with this separator
	 */
	public LookPart getPart()
	{
		return LookPart.valueOf("BREADCRUMB_" + name());
	}
}
