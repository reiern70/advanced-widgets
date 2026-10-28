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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The features of the text of a {@link RichTextEditor} built in its script: those of an editor not
 * told otherwise ({@link #defaults(RichTextFormat)}). A feature left out is not in the editor's
 * schema, nor let through the {@link RichTextPolicy cleaning} of its HTML: pasting it brings its
 * text alone, and its tools do not show. Each lets through the HTML Tiptap writes for it; a page
 * adds features of its own, {@link IRichTextFeature}.
 *
 * @since 11.0.0
 */
public enum RichTextFeature implements IRichTextFeature
{
	/** Headings, of the {@link RichTextEditor#setHeadingLevels(int...) levels} of the editor. */
	HEADING("heading")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements(HEADINGS);
		}
	},

	/** Bold text. */
	BOLD("bold")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("strong", "b");
		}
	},

	/** Italic text. */
	ITALIC("italic")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("em", "i");
		}
	},

	/** Underlined text; not in Markdown, which has no syntax for it. */
	UNDERLINE("underline", false)
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("u");
		}
	},

	/** Struck through text. */
	STRIKE("strike")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("s", "del");
		}
	},

	/** Code in the text. */
	CODE("code")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("code");
		}
	},

	/**
	 * The color of text, of the {@link RichTextEditor#setColors(java.util.Collection) palette},
	 * by a class; not in Markdown.
	 */
	TEXT_COLOR("textColor", false)
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			if (!rules.getColorClasses().isEmpty())
			{
				rules.allowElements("span").allowAttribute("class", rules.getColorClasses(),
					"span");
			}
		}
	},

	/**
	 * The color of the background of text, of the
	 * {@link RichTextEditor#setBackgroundColors(java.util.Collection) palette} of the backgrounds,
	 * by a class; not in Markdown.
	 */
	TEXT_BACKGROUND("textBackground", false)
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("mark").allowAttribute("class", rules.getBackgroundClasses(),
				"mark");
		}
	},

	/** Links, to http, https and mailto addresses, or relative ones. */
	LINK("link")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("a").allowUrl("href", "a")
				.allowAttribute("target", BLANK, "a")
				.requireRelsOnLinks("noopener", "noreferrer", "nofollow");
		}
	},

	/** Bulleted lists. */
	BULLET_LIST("bulletList")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("ul", "li");
		}
	},

	/** Numbered lists. */
	ORDERED_LIST("orderedList")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("ol", "li").allowAttribute("start", NUMBER, "ol")
				.allowAttribute("type", Set.of("1", "a", "A", "i", "I"), "ol");
		}
	},

	/** Lists of tasks, each with a check box. */
	TASK_LIST("taskList")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			// <ul data-type="taskList"><li data-type="taskItem" data-checked="true"><label>
			// <input type="checkbox" checked="checked"><span></span></label><div>...</div></li>
			rules.allowElements("ul", "li", "label", "input", "div")
				.allowWithoutAttributes("span")
				.allowAttribute("data-type", Set.of("taskList"), "ul")
				.allowAttribute("data-type", Set.of("taskItem"), "li")
				.allowAttribute("data-checked", Set.of("true", "false"), "li")
				.allowAttribute("type", Set.of("checkbox"), "input")
				.allowAttribute("checked", Pattern.compile("(?i)(?:checked)?"), "input");
		}
	},

	/** Quotes. */
	BLOCKQUOTE("blockquote")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("blockquote");
		}
	},

	/** Blocks of code, in a language. */
	CODE_BLOCK("codeBlock")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("pre", "code").allowAttribute("class", LANGUAGE_CLASS, "code");
		}
	},

	/** Horizontal lines. */
	HORIZONTAL_RULE("horizontalRule")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("hr");
		}
	},

	/** Tables, with a header row or not. */
	TABLE("table")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("table", "thead", "tbody", "tr", "th", "td")
				.allowAttribute("colspan", NUMBER, "th", "td")
				.allowAttribute("rowspan", NUMBER, "th", "td")
				.allowAttribute("colwidth", WIDTHS, "th", "td")
				// the alignment of the columns comes with the table, as in Markdown
				.allowAttribute("class", ALIGN_CLASS, "th", "td");
		}
	},

	/** Images, of http and https addresses, or relative ones. */
	IMAGE("image")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("img").allowUrl("src", "img")
				.allowAttribute("alt", "img").allowAttribute("title", "img")
				.allowAttribute("width", NUMBER, "img").allowAttribute("height", NUMBER, "img");
		}
	},

	/** The alignment of paragraphs, headings and cells, by classes; not in Markdown. */
	TEXT_ALIGN("textAlign", false)
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowAttribute("class", ALIGN_CLASS, "p").allowAttribute("class", ALIGN_CLASS,
				HEADINGS);
		}
	},

	/**
	 * Mentions of users, or of what a {@link IRichTextSuggester suggester} mentions,
	 * {@code <span class="antilia-mention" data-mention="users" data-id="42">@Ann</span>}; not in
	 * Markdown, where the editor inserts the text of a mention. The ids come back from the
	 * browser: check each again before acting on it (see {@link RichTextMention#of(String)}).
	 */
	MENTION("mention", false)
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("span").allowAttribute("class", MENTION_CLASS, "span")
				.allowAttribute("data-mention", RichTextSuggestion.NAME, "span")
				.allowAttribute("data-id", RichTextSuggestion.ID, "span");
		}
	};

	/** The class of a mention. */
	private static final Pattern MENTION_CLASS = Pattern.compile(RichTextMention.CLASS);

	/** The classes aligning text. */
	private static final Pattern ALIGN_CLASS = Pattern.compile(
		"antilia-text-(?:center|right|justify)");

	/** The class of the language of a code block. */
	private static final Pattern LANGUAGE_CLASS = Pattern.compile("language-[A-Za-z0-9_+#.-]{1,40}");

	private static final Pattern NUMBER = Pattern.compile("[0-9]{1,6}");

	/** The widths of the columns a cell spans. */
	private static final Pattern WIDTHS = Pattern.compile("[0-9]{1,5}(?:,[0-9]{1,5}){0,20}");

	private static final Pattern BLANK = Pattern.compile("(?i)_blank");

	private static final String[] HEADINGS = { "h1", "h2", "h3", "h4", "h5", "h6" };

	private final String name;

	private final boolean inMarkdown;

	RichTextFeature(String name)
	{
		this(name, true);
	}

	RichTextFeature(String name, boolean inMarkdown)
	{
		this.name = name;
		this.inMarkdown = inMarkdown;
	}

	/**
	 * @return the name of the feature in the browser
	 */
	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean isInMarkdown()
	{
		return inMarkdown;
	}

	/**
	 * @param format
	 *            how the editor keeps the text
	 * @return the features of an editor not told otherwise: all of them, but underlined text,
	 *         colors and alignment for Markdown
	 */
	public static Set<IRichTextFeature> defaults(RichTextFormat format)
	{
		Set<IRichTextFeature> features = new LinkedHashSet<>();
		for (RichTextFeature feature : values())
		{
			if (format != RichTextFormat.MARKDOWN || feature.isInMarkdown())
			{
				features.add(feature);
			}
		}
		return Collections.unmodifiableSet(features);
	}
}
