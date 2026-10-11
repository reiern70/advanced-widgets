/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.palette;

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
