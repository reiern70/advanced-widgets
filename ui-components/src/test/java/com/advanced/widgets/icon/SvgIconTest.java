/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class SvgIconTest
{
	@Test
	void everyIconHasItsPath()
	{
		for (SvgIcon icon : SvgIcon.values())
		{
			String markup = icon.getMarkup();
			assertTrue(markup != null && markup.startsWith("<svg class=\"wicket-svg-icon\" " +
				"aria-hidden=\"true\" focusable=\"false\" viewBox=\"0 0 ") &&
				markup.contains(" fill=\"currentColor\">") && markup.contains("<path ") &&
				markup.endsWith("/></svg>"), icon.name());
		}
	}

	@Test
	void theIconsAreTheOnesOfFontAwesome()
	{
		assertEquals(Arrays.stream(FontAwesomeIcon.values()).map(Enum::name).toList(),
			Arrays.stream(SvgIcon.values()).map(Enum::name).toList());
	}

	@Test
	void anIconIsOneEmHighAndAsWideAsItsViewBox()
	{
		String markup = SvgIcon.USER_PLUS.getMarkup();

		assertTrue(markup.contains(" viewBox=\"0 0 640 512\" width=\"1.25em\" height=\"1em\" "),
			markup);
	}

	@Test
	void anIconKeepsTheAttributionOfFontAwesome()
	{
		String markup = SvgIcon.HOUSE.getMarkup();

		assertTrue(markup.contains("<!--! Font Awesome Free ") &&
			markup.contains("Icons: CC BY 4.0") && markup.contains("Fonticons, Inc."), markup);
	}
}
