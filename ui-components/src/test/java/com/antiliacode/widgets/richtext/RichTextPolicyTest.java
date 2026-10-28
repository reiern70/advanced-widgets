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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.apache.wicket.core.util.lang.WicketObjects;
import org.junit.jupiter.api.Test;

/**
 * The cleaning of the HTML: what each feature writes passes, what an attacker writes does not.
 */
class RichTextPolicyTest
{
	private static String clean(String html)
	{
		return RichTextPolicy.ALL.sanitize(html);
	}

	@Test
	void keepsWhatTheEditorWrites()
	{
		String html = "<h1>One</h1><h2>Two</h2><h3>Three</h3><h4>Four</h4><h5>Five</h5>" +
			"<h6>Six</h6><p>Some <strong>bold</strong>, <em>italic</em>, <u>underlined</u>, " +
			"<s>struck</s> and <code>code</code> text.<br />A new line.</p>" +
			"<p><span class=\"antilia-color-red\">Red</span>, <mark " +
			"class=\"antilia-background-yellow\">marked</mark> and <span " +
			"class=\"antilia-color-pink\"><mark class=\"antilia-background-blue\">both</mark>" +
			"</span></p>" +
			"<blockquote><p>A quote</p></blockquote><hr />" +
			"<ul><li><p>One</p><ul><li><p>Nested</p></li></ul></li></ul>" +
			"<ol start=\"3\" type=\"a\"><li><p>Three</p></li></ol>" +
			"<pre><code class=\"language-java\">record A() {}</code></pre>" +
			"<p class=\"antilia-text-center\">Centered</p>" +
			"<h2 class=\"antilia-text-right\">Right</h2>" +
			"<table><tbody><tr><th colspan=\"2\" class=\"antilia-text-right\"><p>A</p></th></tr>" +
			"<tr><td rowspan=\"1\" colwidth=\"120,80\"><p>B</p></td><td><p>C</p></td></tr></tbody>" +
			"</table><img src=\"https://example.org/a.png\" alt=\"A bird\" title=\"Bird\" " +
			"width=\"120\" height=\"80\" />";
		assertEquals(html, clean(html));
	}

	@Test
	void keepsLinksWithTheirRelsAndTargets()
	{
		assertEquals("<p><a target=\"_blank\" href=\"https://example.org\" " +
			"rel=\"noopener noreferrer nofollow\">A</a> <a href=\"/page\" " +
			"rel=\"noopener noreferrer nofollow\">B</a> <a href=\"mailto:a&#64;example.org\" " +
			"rel=\"noopener noreferrer nofollow\">C</a></p>", clean("<p><a target=\"_blank\" " +
				"rel=\"noopener noreferrer nofollow\" href=\"https://example.org\">A</a> " +
				"<a href=\"/page\">B</a> <a href=\"mailto:a@example.org\" rel=\"author\">C</a></p>"));
	}

	@Test
	void keepsTaskLists()
	{
		assertEquals("<ul data-type=\"taskList\"><li data-type=\"taskItem\" data-checked=\"true\">" +
			"<label><input type=\"checkbox\" checked=\"checked\" /><span></span></label><div>" +
			"<p>Done</p></div></li><li data-type=\"taskItem\" data-checked=\"false\"><label>" +
			"<input type=\"checkbox\" /><span></span></label><div><p>To do</p></div></li></ul>",
			clean("<ul data-type=\"taskList\"><li data-type=\"taskItem\" data-checked=\"true\">" +
				"<label><input type=\"checkbox\" checked=\"checked\"><span></span></label><div>" +
				"<p>Done</p></div></li><li data-type=\"taskItem\" data-checked=\"false\"><label>" +
				"<input type=\"checkbox\"><span></span></label><div><p>To do</p></div></li></ul>"));
	}

	@Test
	void dropsScriptsHandlersStylesAndOtherElements()
	{
		assertEquals("<p>Text</p>", clean("<p onclick=\"steal()\" style=\"color: red\">Text</p>" +
			"<script>steal()</script><style>p { color: red }</style>"));
		assertEquals("<p>A link</p>", clean("<p><a href=\"javascript:steal()\">A link</a></p>"));
		assertEquals("<p>An image</p>", clean("<p><img src=\"javascript:steal()\" />An image</p>"));
		assertEquals("<p>An image</p>", clean("<p><img src=\"data:image/png;base64,AAAA\" />" +
			"An image</p>"));
		assertEquals("", clean("<iframe src=\"https://example.org\"></iframe>"));
		assertEquals("<p>Classes</p>", clean("<p class=\"antilia-text-center evil\">Classes</p>"));
		assertEquals("<p>Text</p>", clean("<p>Text<object data=\"x\"></object><embed src=\"x\" />" +
			"</p>"));
		assertEquals("Form", clean("<form action=\"/steal\"><input name=\"password\" />Form</form>"));
		assertEquals("<ul><li>Item</li></ul>", clean("<ul data-type=\"other\"><li " +
			"data-checked=\"maybe\" data-type=\"other\">Item</li></ul>"));
		assertEquals("<pre><code>a</code></pre>", clean("<pre><code onmouseover=\"steal()\" " +
			"class=\"x\">a</code></pre>"));
		assertEquals("<ol><li>One</li></ol>", clean("<ol start=\"-1\" type=\"disc\"><li>One</li>" +
			"</ol>"));
		assertEquals("<p><a href=\"https://example.org\" rel=\"noopener noreferrer nofollow\">" +
			"A</a></p>", clean("<p><a href=\"https://example.org\" target=\"_top\">A</a></p>"));
	}

	@Test
	void keepsTheColorsOfThePaletteOnly()
	{
		for (RichTextColor color : RichTextColor.values())
		{
			String html = "<p><span class=\"" + color.getColorClass() + "\">a</span><mark " +
				"class=\"" + color.getBackgroundClass() + "\">b</mark></p>";
			assertEquals(html, clean(html));
		}
		String others = "<p><span class=\"antilia-color-teal\">teal</span>, <span " +
			"style=\"color: red\">styled</span>, <mark class=\"antilia-background-red evil\" " +
			"style=\"background: url(x)\">evil</mark>, <mark>pasted</mark></p>";
		// the spans of task lists stay bare
		assertEquals("<p><span>teal</span>, <span>styled</span>, <mark>evil</mark>, " +
			"<mark>pasted</mark></p>", clean(others));
		assertEquals("<p>teal, styled, <mark>evil</mark>, <mark>pasted</mark></p>",
			RichTextPolicy.of(EnumSet.of(RichTextFeature.TEXT_COLOR,
				RichTextFeature.TEXT_BACKGROUND)).sanitize(others));
		assertEquals("<p>a <strong>b</strong></p>", RichTextPolicy.of(EnumSet.of(
			RichTextFeature.BOLD)).sanitize("<p><span class=\"antilia-color-red\">a</span> " +
				"<mark class=\"antilia-background-red\"><strong>b</strong></mark></p>"));
		assertEquals("antilia-color-red", RichTextColor.RED.getColorClass());
		assertEquals("antilia-background-yellow", RichTextColor.YELLOW.getBackgroundClass());
	}

	@Test
	void keepsOnlyWhatTheFeaturesOfTheEditorWrite()
	{
		RichTextPolicy policy = RichTextPolicy.of(EnumSet.of(RichTextFeature.BOLD,
			RichTextFeature.BULLET_LIST));
		assertEquals(EnumSet.of(RichTextFeature.BOLD, RichTextFeature.BULLET_LIST),
			policy.getFeatures());
		// the text of what is left out stays, the items of a list left out too
		assertEquals("Title<p><strong>Bold</strong> and italic, a link</p><ul><li>One</li>" +
			"</ul><li></li>An image", policy.sanitize("<h1>Title</h1><p><strong>Bold</strong> and " +
				"<em>italic</em>, <a href=\"/x\">a link</a></p><ul><li>One</li></ul>" +
				"<ol><li></li></ol><img src=\"/a.png\" />An image"));

		RichTextPolicy tasks = RichTextPolicy.of(EnumSet.of(RichTextFeature.TASK_LIST));
		assertEquals("<ul data-type=\"taskList\"><li data-type=\"taskItem\">x</li></ul>",
			tasks.sanitize("<ul data-type=\"taskList\"><li data-type=\"taskItem\">x</li></ul>"));

		RichTextPolicy none = RichTextPolicy.of(Set.of());
		assertEquals("<p>Only text<br />and lines</p>", none.sanitize("<p class=\"antilia-text-center\">" +
			"Only <b>text</b><br>and lines</p>"));
	}

	@Test
	void takesTheAddressesTheEditorTakes()
	{
		assertTrue(RichTextPolicy.isSafeUrl("https://example.org/a.png"));
		assertTrue(RichTextPolicy.isSafeUrl("HTTP://example.org"));
		assertTrue(RichTextPolicy.isSafeUrl("mailto:someone@example.org"));
		assertTrue(RichTextPolicy.isSafeUrl("/images/a.png"));
		assertTrue(RichTextPolicy.isSafeUrl("images/a.png?x=1:2"));
		assertFalse(RichTextPolicy.isSafeUrl("javascript:alert(1)"));
		assertFalse(RichTextPolicy.isSafeUrl(" Java\tScript :alert(1)"));
		assertFalse(RichTextPolicy.isSafeUrl("\u0000javascript:alert(1)"));
		assertFalse(RichTextPolicy.isSafeUrl("data:image/png;base64,AAAA"));
		assertFalse(RichTextPolicy.isSafeUrl("vbscript:x"));
		assertFalse(RichTextPolicy.isSafeUrl(" "));
		assertFalse(RichTextPolicy.isSafeUrl(""));
		assertFalse(RichTextPolicy.isSafeUrl(null));
	}

	@Test
	void isAllForAllTheFeaturesAndSerializable()
	{
		assertSame(RichTextPolicy.ALL, RichTextPolicy.of(null));
		assertSame(RichTextPolicy.ALL, RichTextPolicy.of(EnumSet.allOf(RichTextFeature.class)));
		assertNull(RichTextPolicy.ALL.sanitize(null));

		RichTextPolicy policy = RichTextPolicy.of(EnumSet.of(RichTextFeature.LINK));
		policy.sanitize("<p>made</p>");
		RichTextPolicy copy = WicketObjects.cloneObject(policy);
		assertEquals(policy, copy);
		assertEquals(policy.hashCode(), copy.hashCode());
		assertEquals("<p><a href=\"/x\" rel=\"noopener noreferrer nofollow\">x</a></p>",
			copy.sanitize("<p><a href=\"/x\">x</a></p>"));
		assertNotEquals(policy, RichTextPolicy.ALL);
		assertNotEquals(policy, "policy");
		assertEquals("RichTextPolicy[LINK]", policy.toString());
	}
}
