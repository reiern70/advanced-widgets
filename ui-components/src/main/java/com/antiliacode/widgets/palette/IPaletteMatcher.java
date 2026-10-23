/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antiliacode.widgets.palette;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.util.io.IClusterable;

/**
 * How the filter of a {@link PaletteBehavior palette} matches the text typed into it against the
 * labels of the choices. The filter runs in the browser, so a matcher is a function of
 * {@code Wicket.Palette.matchers} known by its {@link #getName() name}: {@link PaletteMatcher}
 * has those built in. A matcher of one's own registers its function in
 * {@link #renderHead(IHeaderResponse)}, after {@link PaletteBehavior#JS}:
 *
 * <pre>
 * Wicket.Palette.matchers.words = function (label, text) {
 *     // the [start, end) parts of the label matched, which are underlined, or null
 * };
 * </pre>
 *
 * @since 11.0.0
 */
public interface IPaletteMatcher extends IClusterable
{
	/**
	 * @return the name of the matcher among {@code Wicket.Palette.matchers}
	 */
	String getName();

	/**
	 * Renders what the matcher needs in the browser, such as the script registering it; nothing
	 * for those built in.
	 *
	 * @param response
	 *            the header response, which has {@link PaletteBehavior#JS} already
	 */
	default void renderHead(IHeaderResponse response)
	{
	}
}
