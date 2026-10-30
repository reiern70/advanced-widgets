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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class RichTextMarkdownSanitizerTest
{
	private static String clean(String markdown)
	{
		return RichTextMarkdownSanitizer.INSTANCE.sanitize(markdown);
	}

	@Test
	void takesOutTheAddressesOfLinksAndImagesThatAreNotSafe()
	{
		assertEquals("[a](#) and ![b](#)",
			clean("[a](javascript:steal()) and ![b](data:text/html,x)"));
		assertEquals("[a](<#>)", clean("[a](<javascript:steal()>)"));
		assertEquals("[a](# \"title\")", clean("[a](JavaScript:steal() \"title\")"));
		assertEquals("[a](\n  #)", clean("[a](\n  vbscript:steal)"));
		assertEquals("[r]\n\n[r]: # \"title\"", clean("[r]\n\n[r]: javascript:steal() \"title\""));
		assertEquals("> [r]:\n> #", clean("> [r]:\n> javascript:steal()"), "in a quote");
		// a renderer ending the first address at the no-break space sees the second link
		char space = 0xa0;
		assertEquals("[a](https://example.com" + space + "[b](#))",
			clean("[a](https://example.com" + space + "[b](javascript:steal()))"));
	}

	@Test
	void readsTheEntitiesAndBackslashEscapesOfAnAddress()
	{
		for (String hidden : List.of("&#106;avascript:steal()", "&#x6A;avascript:steal()",
			"javascript&colon;steal()", "javascript&#58;steal()", "javascript\\:steal()",
			"java&Tab;script:steal()", "java&#10;script:steal()"))
		{
			assertEquals("[a](#)", clean("[a](" + hidden + ")"), hidden);
		}
	}

	@Test
	void turnsAutolinksToAddressesThatAreNotSafeIntoText()
	{
		assertEquals("&lt;javascript:steal()>", clean("<javascript:steal()>"));
		assertEquals("<https://example.com/a> <mailto:a@example.com>",
			clean("<https://example.com/a> <mailto:a@example.com>"));
	}

	@Test
	void leavesSafeAddressesAndTheRestAsTheyAre()
	{
		for (String markdown : List.of("[a](https://example.com/a_(b)) [b](/path) [c](#top)",
			"[a](mailto:a@example.com) ![b](images/b.png \"B\") [c]()",
			"# Title\n\nSome <b>HTML</b> as text, and `code`", "a ](b c ]: d", "](", "]:",
			"[r]: https://example.com", ""))
		{
			assertSame(markdown, clean(markdown), markdown);
		}
		assertNull(clean(null));
	}

	@Test
	void keepsTheImagesOfTheSiteAloneWhenAsked()
	{
		RichTextMarkdownSanitizer site = RichTextMarkdownSanitizer.IMAGES_FROM_SITE_ONLY;
		assertTrue(site.isImagesFromSiteOnly());
		assertFalse(RichTextMarkdownSanitizer.INSTANCE.isImagesFromSiteOnly());
		assertFalse(new RichTextMarkdownSanitizer().isImagesFromSiteOnly());

		// written as Markdown: a backslash is written twice, once in Java
		for (String elsewhere : List.of("https://example.org/a.png", "//example.org/a.png",
			"\\\\\\\\example.org/a.png", "/\\example.org/a.png", "&#47;&#47;example.org/a.png",
			"<//example.org/a.png>", "data:image/png;base64,AAAA"))
		{
			String markdown = "An ![image](" + elsewhere + " \"Title\") here";
			String clean = site.sanitize(markdown);
			assertEquals(elsewhere.startsWith("<") ? "An ![image](<#> \"Title\") here" :
				"An ![image](# \"Title\") here", clean, elsewhere);
			if (!elsewhere.startsWith("data:"))
			{
				// not safe anyway
				assertSame(markdown, RichTextMarkdownSanitizer.INSTANCE.sanitize(markdown),
					elsewhere);
			}
		}

		// an image in a link: the image goes, the link stays
		assertEquals("[![a](#)](https://example.org)",
			site.sanitize("[![a](//example.org/a.png)](https://example.org)"));
		// a reference definition may serve an image
		assertEquals("![a][r]\n\n[r]: #", site.sanitize("![a][r]\n\n[r]: https://example.org/a.png"));

		String kept = "![a](/a.png) ![b](images/b.png \"B\") ![c]() [d](https://example.org) " +
			"<https://example.org> [e]: /e.png";
		assertSame(kept, site.sanitize(kept));

		RichTextSanitized sanitized = site.sanitizeAndReport(
			"![a](https://example.org/a.png) [b](javascript:x) [c](https://example.org)");
		assertEquals("![a](#) [b](#) [c](https://example.org)", sanitized.text());
		assertEquals(Map.of("img", 1, "a", 1), sanitized.report().getElements());
	}
}
