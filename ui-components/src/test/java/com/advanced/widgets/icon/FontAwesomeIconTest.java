/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.icon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FontAwesomeIconTest
{
	@Test
	void theNameIsTheConstantInLowerCaseWithDashes()
	{
		assertEquals("pen-to-square", FontAwesomeIcon.PEN_TO_SQUARE.getName());
		assertEquals("digital-tachograph", FontAwesomeIcon.DIGITAL_TACHOGRAPH.getName());
	}

	@Test
	void theDigitsHaveNoPrefix()
	{
		assertEquals("0", FontAwesomeIcon.DIGIT_0.getName());
		assertEquals("9", FontAwesomeIcon.DIGIT_9.getName());
	}

	@Test
	void theMarkupIsTheSolidIconOfTheFont()
	{
		assertEquals("<i class=\"fas fa-trash-can\" aria-hidden=\"true\"></i>",
			FontAwesomeIcon.TRASH_CAN.getMarkup());
	}
}
