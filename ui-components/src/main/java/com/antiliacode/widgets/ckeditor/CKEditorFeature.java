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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.antiliacode.widgets.richtext.IRichTextFeature;
import com.antiliacode.widgets.richtext.RichTextFeature;
import com.antiliacode.widgets.richtext.RichTextFormat;
import com.antiliacode.widgets.richtext.RichTextHtmlRules;

/**
 * What the text of a {@link CKEditor} may hold, as CKEditor 5's plugins write it: the features of
 * {@link com.antiliacode.widgets.richtext.RichTextEditor} of the same names, but the colors of the
 * text and of its background, which CKEditor's {@code FontColor} and {@code FontBackgroundColor}
 * write as styles. Each lets through the {@link CKEditorPolicy cleaning} the HTML CKEditor writes
 * for it, which is not always Tiptap's: a table in a {@code <figure class="table">}, a list of
 * tasks of the classes {@code todo-list}...
 *
 * @since 11.0.0
 */
public enum CKEditorFeature implements IRichTextFeature
{
	/** Headings. */
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
			rules.allowElements("i", "em");
		}
	},

	/** Underlined text; not in Markdown. */
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
	 * Text raised above the line, x²: {@code <sup>}; not in Markdown, which has no syntax for it.
	 * Superscript takes subscript away from the text it raises.
	 */
	SUPERSCRIPT("superscript", false)
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("sup");
		}
	},

	/**
	 * Text lowered under the line, H₂O: {@code <sub>}; not in Markdown. Subscript takes
	 * superscript away from the text it lowers.
	 */
	SUBSCRIPT("subscript", false)
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("sub");
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
			rules.allowElements("ol", "li").allowAttribute("start", NUMBER, "ol");
		}
	},

	/** Lists of tasks, each with a check box. */
	TASK_LIST("taskList")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			// <ul class="todo-list"><li><label class="todo-list__label"><input type="checkbox"
			// checked="checked" disabled="disabled"><span class="todo-list__label__description">
			rules.allowElements("ul", "li", "label", "input", "span")
				.allowAttribute("class", Set.of("todo-list"), "ul")
				.allowAttribute("class", TODO_LABEL_CLASS, "label")
				.allowAttribute("class", Set.of("todo-list__label__description"), "span")
				.allowAttribute("type", Set.of("checkbox"), "input")
				.allowAttribute("checked", Pattern.compile("(?i)(?:checked)?"), "input")
				.allowAttribute("disabled", Pattern.compile("(?i)(?:disabled)?"), "input");
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

	/** Tables, in a {@code <figure class="table">}. */
	TABLE("table")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("figure", "table", "thead", "tbody", "tr", "th", "td")
				.allowAttribute("class", Set.of("table"), "figure")
				.allowAttribute("colspan", NUMBER, "th", "td")
				.allowAttribute("rowspan", NUMBER, "th", "td");
		}
	},

	/** Images, in a {@code <figure class="image">} with their caption, without sizes. */
	IMAGE("image")
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowElements("figure", "img", "figcaption")
				.allowAttribute("class", Set.of("image"), "figure")
				.allowUrl("src", "img").allowAttribute("alt", "img");
		}
	},

	/** The alignment of paragraphs, headings and the items of lists, by classes; not in Markdown. */
	TEXT_ALIGN("textAlign", false)
	{
		@Override
		public void allowHtml(RichTextHtmlRules rules)
		{
			rules.allowAttribute("class", ALIGN_CLASS, "p").allowAttribute("class", ALIGN_CLASS,
				HEADINGS);
			if (rules.has(BULLET_LIST) || rules.has(ORDERED_LIST) || rules.has(TASK_LIST))
			{
				rules.allowAttribute("class", ALIGN_CLASS, "li");
			}
		}
	};

	/** The classes aligning text. */
	private static final Pattern ALIGN_CLASS = Pattern.compile(
		"antilia-text-(?:center|right|justify)");

	/** The class of the language of a code block. */
	private static final Pattern LANGUAGE_CLASS = Pattern.compile("language-[A-Za-z0-9_+#.-]{1,40}");

	/** The classes of the label of an item of a list of tasks. */
	private static final Pattern TODO_LABEL_CLASS = Pattern.compile(
		"todo-list__label(?: todo-list__label_without-description)?");

	private static final Pattern NUMBER = Pattern.compile("[0-9]{1,6}");

	private static final Pattern BLANK = Pattern.compile("(?i)_blank");

	private static final String[] HEADINGS = { "h1", "h2", "h3", "h4", "h5", "h6" };

	private final String name;

	private final boolean inMarkdown;

	CKEditorFeature(String name)
	{
		this(name, true);
	}

	CKEditorFeature(String name, boolean inMarkdown)
	{
		this.name = name;
		this.inMarkdown = inMarkdown;
	}

	/**
	 * @return the name of the feature in the browser, that of the feature of
	 *         {@link RichTextFeature} it is
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
	 * @param name
	 *            the name of a feature, of {@link RichTextFeature} for example
	 * @return CKEditor's feature of that name, {@code null} for none: the colors, or a feature of
	 *         a page's own
	 */
	public static CKEditorFeature of(String name)
	{
		for (CKEditorFeature feature : values())
		{
			if (feature.name.equals(name))
			{
				return feature;
			}
		}
		return null;
	}

	/**
	 * @param format
	 *            how the editor keeps the text
	 * @return the features of an editor not told otherwise: all of them, but underlined text,
	 *         superscript, subscript and alignment for Markdown
	 */
	public static Set<CKEditorFeature> defaults(RichTextFormat format)
	{
		Set<CKEditorFeature> features = EnumSet.allOf(CKEditorFeature.class);
		if (format == RichTextFormat.MARKDOWN)
		{
			features.removeIf(feature -> !feature.isInMarkdown());
		}
		return Collections.unmodifiableSet(features);
	}
}
