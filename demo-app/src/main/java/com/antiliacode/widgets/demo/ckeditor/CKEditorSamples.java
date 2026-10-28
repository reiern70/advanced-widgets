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
package com.antiliacode.widgets.demo.ckeditor;

/**
 * The texts the CKEditor demo starts with: an article in HTML as CKEditor writes it, notes in
 * Markdown, and HTML from anywhere, unsafe, for the cleaning.
 */
final class CKEditorSamples
{
	/** The article: every feature of the editor, in CKEditor's HTML. */
	static final String ARTICLE = String.join("",
		"<h2>Field notes from the coast</h2>",
		"<p>The tide came in <strong>early</strong> this morning, and with it a crowd of ",
		"<i>oystercatchers</i> working the mud flats. Read more about them on ",
		"<a target=\"_blank\" rel=\"noopener noreferrer nofollow\" ",
		"href=\"https://en.wikipedia.org/wiki/Oystercatcher\">Wikipedia</a>.</p>",
		"<h3>What we counted</h3>",
		"<figure class=\"table\"><table><thead><tr><th>Bird</th><th>Count</th></tr></thead>",
		"<tbody><tr><td>Oystercatcher</td><td>42</td></tr>",
		"<tr><td>Curlew</td><td>7</td></tr>",
		"<tr><td>Little egret</td><td>3</td></tr></tbody></table></figure>",
		"<h3>To do</h3>",
		"<ul class=\"todo-list\"><li><label class=\"todo-list__label\"><input type=\"checkbox\" ",
		"checked=\"checked\" disabled=\"disabled\"><span class=\"todo-list__label__description\">",
		"Check the nets</span></label></li><li><label class=\"todo-list__label\">",
		"<input type=\"checkbox\" disabled=\"disabled\"><span class=\"todo-list__label__description\">",
		"Send the counts to the society</span></label></li></ul>",
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

	/** HTML from somewhere else, with what a page must never show. */
	static final String UNSAFE = String.join("\n",
		"<h2 onclick=\"alert('heading')\">A heading that runs a script when clicked</h2>",
		"<p>Some <b>bold</b> text, then a script:</p>",
		"<script>alert('script')</script>",
		"<figure class=\"image\"><img src=\"favicon.svg\" width=\"24\" height=\"24\" ",
		"onload=\"alert('image')\" alt=\"An image\"><figcaption>An image that runs a script once ",
		"it loaded, with a size</figcaption></figure>",
		"<p><a href=\"javascript:alert('link')\">A link that runs a script</a> and ",
		"<a href=\"https://example.org\" target=\"_blank\">one that does not</a>.</p>",
		"<p style=\"color: red; position: fixed; inset: 0\">A paragraph styled to cover the page.</p>",
		"<iframe src=\"https://example.org\"></iframe>",
		"<ul><li data-list-item-id=\"e1\" class=\"evil\">An item with an id of CKEditor's</li></ul>",
		"<p class=\"antilia-text-center evil\">A class of the page's own, and an alignment.</p>",
		"<p class=\"antilia-text-right\">Aligned to the right.</p>");

	private CKEditorSamples()
	{
	}
}
