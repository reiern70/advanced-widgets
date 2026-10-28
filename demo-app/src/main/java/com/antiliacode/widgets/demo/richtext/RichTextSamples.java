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
package com.antiliacode.widgets.demo.richtext;

/**
 * The texts the rich text editor demo starts with: an article in HTML, notes in Markdown, and
 * HTML from anywhere, unsafe, for the cleaning.
 */
final class RichTextSamples
{
	/** The article: every feature of the editor, in HTML. */
	static final String ARTICLE = String.join("",
		"<h2>Field notes from the coast</h2>",
		"<p>The tide came in <strong>early</strong> this morning, and with it a crowd of ",
		"<em>oystercatchers</em> working the mud flats. Read more about them on ",
		"<a target=\"_blank\" href=\"https://en.wikipedia.org/wiki/Oystercatcher\" ",
		"rel=\"noopener noreferrer nofollow\">Wikipedia</a>.</p>",
		"<p>Their bills are <span class=\"antilia-color-orange\">bright orange</span>, the sea was ",
		"<span class=\"antilia-color-blue\">calm</span>, and the ",
		"<mark class=\"antilia-background-yellow\">best spot</mark> is past the ",
		"<mark class=\"antilia-background-green\"><span class=\"antilia-color-gray\">old pier",
		"</span></mark>.</p>",
		"<h3>What we counted</h3>",
		"<table><tbody><tr><th><p>Bird</p></th><th class=\"antilia-text-right\"><p>Count</p></th></tr>",
		"<tr><td><p>Oystercatcher</p></td><td class=\"antilia-text-right\"><p>42</p></td></tr>",
		"<tr><td><p>Curlew</p></td><td class=\"antilia-text-right\"><p>7</p></td></tr>",
		"<tr><td><p>Little egret</p></td><td class=\"antilia-text-right\"><p>3</p></td></tr>",
		"</tbody></table>",
		"<h3>To do</h3>",
		"<ul data-type=\"taskList\"><li data-checked=\"true\" data-type=\"taskItem\"><label>",
		"<input type=\"checkbox\" checked=\"checked\" /><span></span></label><div><p>Check the nets</p>",
		"</div></li><li data-checked=\"false\" data-type=\"taskItem\"><label><input type=\"checkbox\" />",
		"<span></span></label><div><p>Send the counts to the society</p></div></li></ul>",
		"<blockquote><p>The sea, once it casts its spell, holds one in its net of wonder forever.",
		"</p></blockquote>",
		"<p class=\"antilia-text-center\"><s>Rain</s> Sun expected tomorrow, so we go out at ",
		"<code>06:30</code>.</p>",
		"<pre><code class=\"language-java\">/** A bird we saw, and how many. */\n",
		"record Sighting(String bird, int count) {\n",
		"    static int total(List&lt;Sighting&gt; all) {\n",
		"        return all.stream().mapToInt(Sighting::count).sum(); // 52\n",
		"    }\n",
		"}</code></pre>");

	/** The notes: Markdown, with what it has a syntax for. */
	static final String NOTES = String.join("\n",
		"# Packing list",
		"",
		"For the **weekend** on the coast:",
		"",
		"- Binoculars",
		"- A *warm* jacket",
		"- The [tide tables](https://en.wikipedia.org/wiki/Tide_table)",
		"",
		"1. Leave at six",
		"2. Coffee at the harbour",
		"",
		"> Check the forecast the night before.",
		"");

	/**
	 * The text of the writing case: words the demo's proofreader knows spelled wrong, a word said
	 * twice, two blanks, and a mention; written as the cleaning writes it, @ as &amp;#64;.
	 */
	static final String WRITING = "<h2>Writing, checked as you type</h2>" +
		"<p>Teh proofreader of this demo knows a few words spelled wrong: recieve, seperate, " +
		"definately. It sees a word said said twice, and  two blanks between words.</p>" +
		"<p>Ask <span class=\"antilia-mention\" data-mention=\"people\" data-id=\"ada\">" +
		"&#64;Ada Lovelace</span> what she thinks.</p>";

	/** HTML from somewhere else, with what a page must never show. */
	static final String UNSAFE = String.join("\n",
		"<h2 onclick=\"alert('heading')\">A heading that runs a script when clicked</h2>",
		"<p>Some <b>bold</b> text, then a script:</p>",
		"<script>alert('script')</script>",
		"<p><img src=\"favicon.svg\" width=\"24\" height=\"24\" onload=\"alert('image')\"> ",
		"An image that runs a script once it loaded.</p>",
		"<p><a href=\"javascript:alert('link')\">A link that runs a script</a> and ",
		"<a href=\"https://example.org\" target=\"_blank\">one that does not</a>.</p>",
		"<p style=\"color: red; position: fixed; inset: 0\">A paragraph styled to cover the page.</p>",
		"<iframe src=\"https://example.org\"></iframe>",
		"<p class=\"antilia-text-center evil\">A class of the page's own, and an alignment.</p>",
		"<p><span class=\"antilia-color-green\">A color of the palette</span>, ",
		"<span style=\"color: #ff00ff\">a color of its own</span> and ",
		"<mark class=\"antilia-background-yellow\">a background</mark>.</p>",
		"<p class=\"antilia-text-right\">Aligned to the right.</p>");

	private RichTextSamples()
	{
	}
}
