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
package com.antilia.widgets.icon;

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
			assertTrue(markup != null && markup.startsWith("<svg class=\"antilia-svg-icon\" " +
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
