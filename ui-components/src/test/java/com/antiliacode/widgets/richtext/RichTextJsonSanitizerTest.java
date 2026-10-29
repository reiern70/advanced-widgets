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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

class RichTextJsonSanitizerTest
{
	private static String clean(String json)
	{
		return RichTextJsonSanitizer.INSTANCE.sanitize(json);
	}

	private static String paragraph(String content)
	{
		return "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[" + content +
			"]}]}";
	}

	private static String link(String href)
	{
		return "{\"type\":\"text\",\"marks\":[{\"type\":\"bold\"},{\"type\":\"link\",\"attrs\":" +
			"{\"href\":\"" + href + "\",\"target\":\"_blank\"}}],\"text\":\"there\"}";
	}

	@Test
	void takesOutTheLinksAndImagesToAddressesThatAreNotSafe()
	{
		String json = paragraph(link("javascript:steal()") +
			",{\"type\":\"image\",\"attrs\":{\"src\":\"data:image/svg+xml,x\"}}" +
			",{\"type\":\"text\",\"text\":\"!\"}");

		String bold = "{\"type\":\"text\",\"marks\":[{\"type\":\"bold\"}],\"text\":\"there\"}";
		assertEquals(new JSONObject(paragraph(bold + ",{\"type\":\"text\",\"text\":\"!\"}"))
			.toString(), clean(json));
		assertEquals(new JSONObject(paragraph(bold)).toString(),
			clean(paragraph(link(" JaVaScRiPt:steal()"))));
	}

	@Test
	void leavesADocumentWithSafeAddressesAsItIs()
	{
		for (String json : new String[] { paragraph(link("https://example.com/a")),
			paragraph(link("/relative")), paragraph(link("")),
			paragraph("{\"type\":\"image\",\"attrs\":{\"src\":\"images/a.png\",\"alt\":\"<b>\"}}"),
			"{\"type\":\"doc\"}" })
		{
			assertSame(json, clean(json), json);
		}
	}

	@Test
	void givesNothingForATextThatIsNoDocument()
	{
		assertNull(clean(null));
		assertNull(clean("<p>HTML</p>"));
		assertNull(clean("[1, 2]"));
		assertNull(clean("{\"type\":\"doc\"} trailing"));
		assertNull(clean("{\"type\":\"doc\""));
		assertNull(clean("{\"a\":" + "[".repeat(RichTextJsonSanitizer.MAX_DEPTH) + "]".repeat(
			RichTextJsonSanitizer.MAX_DEPTH) + "}"), "too deep");
		String deepest = "{\"a\":" + "[".repeat(RichTextJsonSanitizer.MAX_DEPTH - 1) +
			"]".repeat(RichTextJsonSanitizer.MAX_DEPTH - 1) + "}";
		assertSame(deepest, clean(deepest));
	}
}
