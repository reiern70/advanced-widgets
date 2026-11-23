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
package com.antiliacode.widgets.demo.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * The letters of a captcha travel in a token that only the server reads, which expires and
 * answers once; see {@link CaptchaToken}.
 */
class CaptchaTokenTest
{
	private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

	@Test
	void aTokenHoldsItsLettersHidden()
	{
		String token = CaptchaToken.issue("HK4WP", NOW);

		assertEquals(Optional.of("HK4WP"), CaptchaToken.letters(token, NOW));
		assertFalse(token.contains("HK4WP"));
		assertNotEquals(token, CaptchaToken.issue("HK4WP", NOW), "each token is new");
	}

	@Test
	void newLettersAreOfTheLettersThatLookLikeNoOther()
	{
		String letters = CaptchaToken.letters(CaptchaToken.issue(NOW), NOW).orElseThrow();

		assertEquals(CaptchaToken.LENGTH, letters.length());
		assertTrue(letters.chars().allMatch(c -> CaptchaToken.LETTERS.indexOf(c) >= 0), letters);
	}

	@Test
	void aTokenAnswersOnceRightOrWrong()
	{
		String token = CaptchaToken.issue("HK4WP", NOW);
		assertTrue(CaptchaToken.answer(token, " hk4wp ", NOW));
		assertFalse(CaptchaToken.answer(token, "HK4WP", NOW), "answered already");

		String other = CaptchaToken.issue("HK4WP", NOW);
		assertFalse(CaptchaToken.answer(other, "WRONG", NOW));
		assertFalse(CaptchaToken.answer(other, "HK4WP", NOW), "a wrong answer uses it too");
	}

	@Test
	void aTokenExpires()
	{
		String token = CaptchaToken.issue("HK4WP", NOW);
		Instant later = NOW.plus(CaptchaToken.VALID);

		assertTrue(CaptchaToken.letters(token, later.minus(Duration.ofSeconds(1))).isPresent());
		assertTrue(CaptchaToken.letters(token, later).isEmpty());
		assertFalse(CaptchaToken.answer(token, "HK4WP", later));
	}

	@Test
	void aTokenNotMadeByTheServerOrChangedIsNone()
	{
		String token = CaptchaToken.issue("HK4WP", NOW);
		// a character in the middle: the last one may carry only bits that do not count
		char middle = token.charAt(20);
		String changed = token.substring(0, 20) + (middle == 'A' ? 'B' : 'A') + token.substring(21);

		for (String none : new String[] { null, "", "made-up", "!!!", changed, "A".repeat(500) })
		{
			assertTrue(CaptchaToken.letters(none, NOW).isEmpty(), none);
			assertFalse(CaptchaToken.answer(none, "HK4WP", NOW), none);
		}
	}
}
