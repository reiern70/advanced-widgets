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
package com.antiliacode.widgets.ckeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.Set;

import org.apache.wicket.core.util.lang.WicketObjects;
import org.junit.jupiter.api.Test;


/**
 * The cleaning of the HTML of CKEditor: what CKEditor 5 writes for each feature passes, what an
 * attacker writes does not.
 */
class CKEditorPolicyTest
{
	private static String clean(String html)
	{
		return CKEditorPolicy.ALL.sanitize(html);
	}

	@Test
	void keepsWhatCKEditorWrites()
	{
		// as CKEditor 49 writes it, with the sanitizer's way of writing empty elements
		String html = "<h1>One</h1><h2>Two</h2><h3 class=\"antilia-text-center\">Three</h3>" +
			"<h4>Four</h4><h5>Five</h5><h6>Six</h6>" +
			"<p class=\"antilia-text-right\">r <strong>b</strong> <i>i</i> <u>u</u> <s>s</s> " +
			"<code>c</code> x<sup>2</sup> H<sub>2</sub>O<br />A new line.</p>" +
			"<p class=\"antilia-text-justify\">Justified</p>" +
			"<ul><li>a<ul><li>b</li></ul></li></ul><ol start=\"3\"><li>n</li></ol>" +
			"<ul class=\"todo-list\"><li><label class=\"todo-list__label\"><input type=\"checkbox\" " +
			"checked=\"checked\" disabled=\"disabled\" /><span class=\"todo-list__label__description\">" +
			"t</span></label></li></ul>" +
			"<blockquote><p>q</p></blockquote><pre><code class=\"language-java\">int x;</code></pre>" +
			"<hr /><figure class=\"table\"><table><thead><tr><th>h</th><th>Two</th></tr></thead>" +
			"<tbody><tr><td colspan=\"2\">c</td></tr></tbody></table></figure>" +
			"<figure class=\"image\"><img src=\"/a.png\" alt=\"alt\" /><figcaption>cap</figcaption>" +
			"</figure><p><img src=\"https://example.org/b.png\" alt=\"in\" /> x</p>";
		assertEquals(html, clean(html));
	}

	@Test
	void keepsLinksWithTheirRelsAndTargets()
	{
		assertEquals("<p><a href=\"https://x.org\" target=\"_blank\" " +
			"rel=\"noopener noreferrer nofollow\">l</a> <a href=\"/page\" " +
			"rel=\"noopener noreferrer nofollow\">m</a></p>", clean("<p><a href=\"https://x.org\" " +
				"target=\"_blank\" rel=\"noopener noreferrer\">l</a> <a href=\"/page\">m</a></p>"));
	}

	@Test
	void dropsWhatAnAttackerWrites()
	{
		assertEquals("<p>Hi there</p><p>Styled <img alt=\"A\" /></p>" +
			"<figure><img src=\"/a.png\" alt=\"B\" /></figure>" +
			"<ul><li>One</li></ul><p>Plain</p>",
			clean("<p onclick=\"steal()\">Hi <a href=\"javascript:steal()\">there</a></p>" +
				"<script>steal()</script><iframe src=\"https://evil.org\"></iframe>" +
				"<p style=\"position: fixed\">Styled <img src=\"data:image/png;base64,AA\" alt=\"A\" " +
				"onerror=\"steal()\" /></p><figure class=\"image other\"><img src=\"/a.png\" " +
				"alt=\"B\" width=\"10\" height=\"20\" style=\"aspect-ratio:10/20;\" /></figure>" +
				"<ul><li data-list-item-id=\"e3cec17\">One</li></ul>" +
				"<p class=\"evil antilia-text-center\">Plain</p>"));
		// a class only where CKEditor writes it; a span left without one goes
		assertEquals("<ul><li><label>t</label></li></ul>",
			clean("<ul class=\"todo-list x\"><li><label class=\"y\"><span class=\"z\">t</span>" +
				"</label></li></ul>"));
		assertEquals("<pre><code>x</code></pre>", clean("<pre><code class=\"language-a b\">x" +
			"</code></pre>"));
		assertNull(clean(null));
	}

	@Test
	void leavesOutWhatTheFeaturesLeftOutWrite()
	{
		CKEditorPolicy bold = CKEditorPolicy.of(EnumSet.of(CKEditorFeature.BOLD));
		assertEquals("<p><strong>b</strong> i 2 2</p>cell<p>a</p>caption",
			bold.sanitize("<p><strong>b</strong> <i>i</i> <sup>2</sup> <sub>2</sub></p>" +
				"<figure class=\"table\"><table>" +
				"<tbody><tr><td>cell</td></tr></tbody></table></figure><ul><li><p>a</p></li></ul>" +
				"<figure class=\"image\"><img src=\"/a.png\" /><figcaption>caption</figcaption>" +
				"</figure>"));
		// tables without images: their figure only
		CKEditorPolicy tables = CKEditorPolicy.of(EnumSet.of(CKEditorFeature.TABLE));
		assertEquals("<figure class=\"table\"><table><tbody><tr><td>c</td></tr></tbody></table>" +
			"</figure><figure></figure>", tables.sanitize("<figure class=\"table\"><table><tbody>" +
				"<tr><td>c</td></tr></tbody></table></figure><figure class=\"image\"><img " +
				"src=\"/a.png\" /></figure>"));
		// no alignment: no class
		CKEditorPolicy paragraphs = CKEditorPolicy.of(EnumSet.of(CKEditorFeature.HEADING));
		assertEquals("<h2>T</h2><p>p</p>", paragraphs.sanitize(
			"<h2 class=\"antilia-text-center\">T</h2><p class=\"antilia-text-right\">p</p>"));
	}

	@Test
	void dropsTheColorsOfTiptapsPalette()
	{
		assertEquals(CKEditor.FEATURES, CKEditorPolicy.ALL.getFeatures());
		assertEquals(CKEditorPolicy.of(Set.of(CKEditorFeature.BOLD)), CKEditorPolicy.of(Set.of(
			CKEditorFeature.BOLD)));
		assertEquals("<p>red and yellow</p>", CKEditorPolicy.ALL.sanitize(
			"<p><span class=\"antilia-color-red\">red</span> and " +
				"<mark class=\"antilia-background-yellow\">yellow</mark></p>"));
	}

	@Test
	void isOnePolicyPerSetOfFeatures()
	{
		assertSame(CKEditorPolicy.ALL, CKEditorPolicy.of(null));
		assertSame(CKEditorPolicy.ALL, CKEditorPolicy.of(EnumSet.allOf(CKEditorFeature.class)));
		CKEditorPolicy none = CKEditorPolicy.of(Set.of());
		assertEquals("<p>x</p>", none.sanitize("<p><strong>x</strong></p>"));
		assertEquals(Set.of(), none.getFeatures());
		assertEquals(CKEditorPolicy.of(EnumSet.of(CKEditorFeature.BOLD)),
			CKEditorPolicy.of(Set.of(CKEditorFeature.BOLD)));
		assertEquals(CKEditorPolicy.of(EnumSet.of(CKEditorFeature.BOLD)).hashCode(),
			CKEditorPolicy.of(Set.of(CKEditorFeature.BOLD)).hashCode());
		assertNotEquals(none, CKEditorPolicy.ALL);
		assertNotEquals(CKEditorPolicy.ALL, "ALL");
		assertEquals("CKEditorPolicy[BOLD]", CKEditorPolicy.of(Set.of(CKEditorFeature.BOLD))
			.toString());
		assertThrows(UnsupportedOperationException.class, () -> CKEditorPolicy.ALL.getFeatures()
			.clear());

		// serialized, it makes OWASP's policy anew
		CKEditorPolicy copy = WicketObjects.cloneObject(CKEditorPolicy.of(Set.of(
			CKEditorFeature.ITALIC)));
		assertEquals("<p><i>i</i>b</p>", copy.sanitize("<p><i>i</i><b>b</b></p>"));
	}
}
