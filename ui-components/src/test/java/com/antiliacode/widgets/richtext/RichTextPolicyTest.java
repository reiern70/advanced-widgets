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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.wicket.core.util.lang.WicketObjects;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.ckeditor.CKEditorPolicy;

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
			"<p>x<sup>2</sup> and H<sub>2</sub>O</p>" +
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
	void dropsWhatRunsScriptsHoweverHidden()
	{
		String[] attacks = { "<svg onload=\"steal()\"><circle /></svg>",
			"<img src=\"x\" onerror=\"steal()\" />",
			"<img src=\"https://example.org/a.png\" srcset=\"javascript:steal() 1x\" />",
			"<a href=\"&#106;avascript:steal()\">entity</a>",
			"<a href=\"&#x6A;&#x61;&#x76;&#x61;script&#x3A;steal()\">hex</a>",
			"<a href=\"jav&#x09;ascript:steal()\">tab</a>",
			"<a href=\"JaVaScRiPt:steal()\">case</a>",
			"<img src=\"vbscript:steal()\" />",
			"<object data=\"javascript:steal()\"></object><embed src=\"x.swf\" />",
			"<form action=\"https://evil.example\"><button formaction=\"javascript:steal()\">" +
				"Go</button></form>",
			"<base href=\"https://evil.example/\" />",
			"<meta http-equiv=\"refresh\" content=\"0;url=javascript:steal()\" />",
			"<math><mtext><table><mglyph><style><img src=x onerror=steal()></style></mglyph>" +
				"</table></mtext></math>",
			"<a href=\"https://example.org\" onmouseover=\"steal()\">over</a>",
			"<p style=\"background:url(javascript:steal())\">styled</p>",
			"<noscript><p title=\"</noscript><img src=x onerror=steal()>\"></p></noscript>" };
		// both editors' cleanings, Tiptap's and CKEditor's
		List<IRichTextSanitizer> policies = List.of(RichTextPolicy.ALL, CKEditorPolicy.ALL);
		String[] banned = { "javascript", "vbscript", "onerror", "onload", "onmouseover", "<svg",
			"<object", "<embed", "<form", "formaction", "<base", "<meta", "<math", "<style",
			"<noscript", "srcset", "style=" };
		for (IRichTextSanitizer policy : policies)
		{
			for (String attack : attacks)
			{
				String cleaned = policy.sanitize("<p>Before</p>" + attack);
				String lower = cleaned == null ? "" : cleaned.toLowerCase(Locale.ROOT);
				for (String word : banned)
				{
					assertFalse(lower.contains(word), attack + " -> " + cleaned);
				}
			}
		}
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
			String html = "<p><span class=\"" + color.getColorClass() + "\">a</span></p>";
			assertEquals(html, clean(html));
		}
		for (RichTextBackgroundColor color : RichTextBackgroundColor.values())
		{
			String html = "<p><mark class=\"" + color.getBackgroundClass() + "\">b</mark></p>";
			assertEquals(html, clean(html));
		}
		// lime colors backgrounds alone, indigo text alone
		String others = "<p><span class=\"antilia-color-lime\">lime</span>, <span " +
			"style=\"color: red\">styled</span>, <mark class=\"antilia-background-red evil\" " +
			"style=\"background: url(x)\">evil</mark>, <mark>pasted</mark></p>";
		// the spans of task lists stay bare
		assertEquals("<p><span>lime</span>, <span>styled</span>, <mark>evil</mark>, " +
			"<mark>pasted</mark></p>", clean(others));
		assertEquals("<p><mark>indigo</mark></p>", clean(
			"<p><mark class=\"antilia-background-indigo\">indigo</mark></p>"));
		assertEquals("<p>lime, styled, <mark>evil</mark>, <mark>pasted</mark></p>",
			RichTextPolicy.of(EnumSet.of(RichTextFeature.TEXT_COLOR,
				RichTextFeature.TEXT_BACKGROUND)).sanitize(others));
		assertEquals("<p>a <strong>b</strong></p>", RichTextPolicy.of(EnumSet.of(
			RichTextFeature.BOLD)).sanitize("<p><span class=\"antilia-color-red\">a</span> " +
				"<mark class=\"antilia-background-red\"><strong>b</strong></mark></p>"));
		assertEquals("antilia-color-red", RichTextColor.RED.getColorClass());
		assertEquals("antilia-background-yellow",
			RichTextBackgroundColor.YELLOW.getBackgroundClass());
	}

	@Test
	void keepsTheClassesOfThePaletteGiven()
	{
		RichTextPolicy policy = RichTextPolicy.of(null, List.of(new RichTextViewTest.Brand(),
			RichTextColor.GREEN), List.of(new RichTextViewTest.Brand(),
				RichTextBackgroundColor.GREEN));
		assertEquals(Set.of("brand-text", "antilia-color-green"), policy.getColorClasses());
		assertEquals(Set.of("brand-mark", "antilia-background-green"),
			policy.getBackgroundClasses());
		assertEquals("<p><span class=\"brand-text\">a</span><mark class=\"brand-mark\">b</mark>" +
			"<span class=\"antilia-color-green\">c</span><span>d</span><mark>e</mark>" +
			"<span>f</span></p>", policy.sanitize("<p><span class=\"brand-text\">a</span>" +
				"<mark class=\"brand-mark\">b</mark><span class=\"antilia-color-green\">c</span>" +
				"<span class=\"antilia-color-red\">d</span><mark class=\"brand-text\">e</mark>" +
				"<span class=\"brand-text evil\">f</span></p>"));
		assertNotEquals(RichTextPolicy.ALL, policy);
		assertSame(RichTextPolicy.ALL, RichTextPolicy.of(null, List.of(RichTextColor.values()),
			List.of(RichTextBackgroundColor.values())));
		assertEquals("<p>a</p>", RichTextPolicy.of(EnumSet.of(RichTextFeature.TEXT_COLOR),
			List.of(), null).sanitize("<p><span class=\"antilia-color-red\">a</span></p>"));
	}

	@Test
	void keepsTheColorsOfTheTextAndOfTheBackgroundApart()
	{
		RichTextPolicy policy = RichTextPolicy.of(null, List.of(text("ink", "ink")),
			List.of(background("ink", "glow"), RichTextBackgroundColor.YELLOW));
		assertEquals(Set.of("ink"), policy.getColorClasses());
		assertEquals(Set.of("glow", "antilia-background-yellow"), policy.getBackgroundClasses());
		assertEquals("<p><span class=\"ink\"><mark class=\"glow\">a</mark></span><mark>b</mark>" +
			"<span>c</span><mark class=\"antilia-background-yellow\">d</mark><span>e</span></p>",
			policy.sanitize("<p><span class=\"ink\"><mark class=\"glow\">a</mark></span>" +
				"<mark class=\"ink\">b</mark><span class=\"glow\">c</span>" +
				"<mark class=\"antilia-background-yellow\">d</mark>" +
				"<span class=\"antilia-color-yellow\">e</span></p>"));

		RichTextPolicy colorsOnly = RichTextPolicy.of(null, RichTextColor.PALETTE, List.of());
		assertEquals(RichTextPolicy.ALL.getColorClasses(), colorsOnly.getColorClasses());
		assertEquals(Set.of(), colorsOnly.getBackgroundClasses());
		assertEquals("<p><span class=\"antilia-color-red\">a</span><mark>b</mark></p>",
			colorsOnly.sanitize("<p><span class=\"antilia-color-red\">a</span>" +
				"<mark class=\"antilia-background-red\">b</mark></p>"));

		RichTextPolicy none = RichTextPolicy.of(null, null, null);
		assertEquals(Set.of(), none.getColorClasses(), "null for no colors");
		assertEquals(Set.of(), none.getBackgroundClasses());
		assertEquals(none, RichTextPolicy.of(null, List.of(), List.of()));
		// the spans of task lists stay bare
		assertEquals("<p><span>a</span><mark>b</mark></p>", none.sanitize("<p><span class=\"antilia-color-red\">" +
			"a</span><mark class=\"antilia-background-red\">b</mark></p>"));
	}

	@Test
	void takesPalettesOfColorsNamedOnce()
	{
		IRichTextColor teal = text("teal", "teal-text");
		assertEquals(List.of(teal, RichTextColor.RED), List.copyOf(IRichTextColor.palette(
			List.of(teal, RichTextColor.RED))));
		assertThrows(IllegalArgumentException.class, () -> IRichTextColor.palette(null));
		assertThrows(IllegalArgumentException.class, () -> IRichTextColor.palette(
			java.util.Arrays.asList(teal, null)));
		assertThrows(IllegalArgumentException.class, () -> IRichTextColor.palette(List.of(teal,
			text("teal", "other-text"))));
		assertThrows(IllegalArgumentException.class, () -> IRichTextColor.palette(List.of(teal,
			text("other", "teal-text"))));
		for (String name : new String[] { null, "", "1st", "with space", "a\"b", "a".repeat(41) })
		{
			assertThrows(IllegalArgumentException.class, () -> IRichTextColor.palette(List.of(
				text(name, "a-text"))), name);
			assertThrows(IllegalArgumentException.class, () -> IRichTextBackgroundColor.palette(
				List.of(background(name, "a-mark"))), name);
		}
		for (String css : new String[] { null, "", "two classes", "a\"b", "9a", "a.b" })
		{
			assertThrows(IllegalArgumentException.class, () -> IRichTextColor.palette(List.of(
				text("a", css))), css);
			assertThrows(IllegalArgumentException.class, () -> IRichTextBackgroundColor.palette(
				List.of(background("a", css))), css);
		}
		assertThrows(UnsupportedOperationException.class, () -> RichTextColor.PALETTE.add(teal));

		IRichTextBackgroundColor mint = background("mint", "mint-mark");
		assertEquals(List.of(RichTextBackgroundColor.RED, mint), List.copyOf(
			IRichTextBackgroundColor.palette(List.of(RichTextBackgroundColor.RED, mint))));
		assertThrows(IllegalArgumentException.class, () -> IRichTextBackgroundColor.palette(null));
		assertThrows(IllegalArgumentException.class, () -> IRichTextBackgroundColor.palette(
			List.of(mint, background("mint", "other-mark"))));
		assertThrows(IllegalArgumentException.class, () -> IRichTextBackgroundColor.palette(
			List.of(mint, background("other", "mint-mark"))));
		assertThrows(UnsupportedOperationException.class,
			() -> RichTextBackgroundColor.PALETTE.add(mint));
	}

	/** A color of the text alone. */
	static IRichTextColor text(String name, String colorClass)
	{
		return new IRichTextColor()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getName()
			{
				return name;
			}

			@Override
			public String getColorClass()
			{
				return colorClass;
			}
		};
	}

	/** A color of the background alone. */
	static IRichTextBackgroundColor background(String name, String backgroundClass)
	{
		return new IRichTextBackgroundColor()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getName()
			{
				return name;
			}

			@Override
			public String getBackgroundClass()
			{
				return backgroundClass;
			}
		};
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

		// superscript and subscript apart
		RichTextPolicy raised = RichTextPolicy.of(EnumSet.of(RichTextFeature.SUPERSCRIPT));
		assertEquals("<p>x<sup>2</sup> H2O</p>", raised.sanitize(
			"<p>x<sup>2</sup> H<sub>2</sub>O</p>"));
		RichTextPolicy lowered = RichTextPolicy.of(EnumSet.of(RichTextFeature.SUBSCRIPT));
		assertEquals("<p>x2 H<sub>2</sub>O</p>", lowered.sanitize(
			"<p>x<sup>2</sup> H<sub>2</sub>O</p>"));

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
		assertEquals("RichTextPolicy[LINK]" + RichTextPolicy.ALL.getColorClasses() +
			RichTextPolicy.ALL.getBackgroundClasses(), policy.toString());

		RichTextPolicy own = RichTextPolicy.of(null, List.of(new RichTextViewTest.Brand()),
			List.of(new RichTextViewTest.Brand()));
		own.sanitize("<p>made</p>");
		RichTextPolicy ownCopy = WicketObjects.cloneObject(own);
		assertEquals(own, ownCopy);
		assertEquals("<p><span class=\"brand-text\">a</span><mark class=\"brand-mark\">b</mark>" +
			"</p>", ownCopy.sanitize("<p><span class=\"brand-text\">a</span>" +
				"<mark class=\"brand-mark\">b</mark></p>"));
	}
}
