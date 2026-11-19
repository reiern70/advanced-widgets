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
package com.antiliacode.widgets.richtext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

/**
 * The editors and their views need the OWASP Java HTML Sanitizer, an optional dependency, and
 * say so as they are added to a page without it.
 */
class OwaspHtmlSanitizerTest extends WicketTestCase
{
	@Test
	void isOnTheClassPathOfTheTests()
	{
		assertTrue(OwaspHtmlSanitizer.isPresent());
		OwaspHtmlSanitizer.require(new Label("text"));
	}

	@Test
	void withoutItTellsTheDependencyToAdd()
	{
		RichTextView view = new RichTextView("text", Model.of("<p>Hi</p>"));

		WicketRuntimeException e = assertThrows(WicketRuntimeException.class,
			() -> OwaspHtmlSanitizer.require(view, false));

		assertTrue(e.getMessage().contains(
			"com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer"), e.getMessage());
		assertTrue(e.getMessage().contains("id = text"), e.getMessage());
	}

	@Test
	void findsAClassByItsName()
	{
		assertTrue(OwaspHtmlSanitizer.loads("org.owasp.html.PolicyFactory"));
		assertFalse(OwaspHtmlSanitizer.loads("org.owasp.html.NoSuchClass"));
	}
}
