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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import com.antiliacode.widgets.richtext.RichTextFeature;
import com.antiliacode.widgets.richtext.RichTextPolicy;
import com.antiliacode.widgets.richtext.RichTextSanitizer;

/**
 * The cleaning of the HTML of a {@link CKEditor}, with the OWASP Java HTML Sanitizer: it keeps the
 * elements and the attributes CKEditor 5 writes for the {@link RichTextFeature features} of the
 * editor, and nothing else, as {@link RichTextPolicy} does for Tiptap's. No script, no event
 * handler, no style attribute, no element of a feature left out; links and images only to http,
 * https and mailto addresses, or relative ones ({@link RichTextPolicy#isSafeUrl(String)}), links
 * with {@code rel="noopener noreferrer nofollow"}, and images without a width or a height. Classes
 * only where CKEditor writes them:
 * <ul>
 * <li>the alignment of paragraphs, headings and the items of lists, {@code antilia-text-center},
 * {@code -right} and {@code -justify}, those of Tiptap's editor;</li>
 * <li>the language of a code block, {@code <pre><code class="language-java">};</li>
 * <li>the figures holding tables and images, {@code <figure class="table">} and
 * {@code <figure class="image">}, with the caption of an image;</li>
 * <li>the lists of tasks, {@code <ul class="todo-list">}, each item a {@code label} of the class
 * {@code todo-list__label} holding a check box and a {@code span} of the class
 * {@code todo-list__label__description}.</li>
 * </ul>
 * CKEditor's ids of the items of lists, {@code data-list-item-id}, which it makes anew as it reads
 * the HTML, do not stay.
 *
 * @since 11.0.0
 */
public final class CKEditorPolicy implements RichTextSanitizer
{
	private static final long serialVersionUID = 1L;

	/** The policy of all the features of CKEditor, {@link CKEditor#FEATURES}. */
	public static final CKEditorPolicy ALL = new CKEditorPolicy(CKEditor.FEATURES);

	/** The classes aligning text. */
	private static final Pattern ALIGN_CLASS = Pattern.compile(
		"antilia-text-(?:center|right|justify)");

	/** The class of the language of a code block. */
	private static final Pattern LANGUAGE_CLASS = Pattern.compile("language-[A-Za-z0-9_+#.-]{1,40}");

	/** The classes of the label of an item of a list of tasks. */
	private static final Pattern TODO_LABEL_CLASS = Pattern.compile(
		"todo-list__label(?: todo-list__label_without-description)?");

	private static final Pattern NUMBER = Pattern.compile("[0-9]{1,6}");

	private static final String[] HEADINGS = { "h1", "h2", "h3", "h4", "h5", "h6" };

	private final Set<RichTextFeature> features;

	/** Made the first time it cleans: OWASP's policies are not serializable. */
	private transient PolicyFactory factory;

	private CKEditorPolicy(Set<RichTextFeature> features)
	{
		this.features = EnumSet.noneOf(RichTextFeature.class);
		this.features.addAll(features);
		this.features.retainAll(CKEditor.FEATURES);
	}

	/**
	 * @param features
	 *            the features of the editor, of which those CKEditor has count; {@code null} for
	 *            all of them
	 * @return the policy keeping what CKEditor writes for those features
	 */
	public static CKEditorPolicy of(Set<RichTextFeature> features)
	{
		if (features == null || features.containsAll(ALL.features))
		{
			return ALL;
		}
		return new CKEditorPolicy(features);
	}

	/**
	 * @return the features whose HTML the policy keeps
	 */
	public Set<RichTextFeature> getFeatures()
	{
		return Collections.unmodifiableSet(features);
	}

	@Override
	public String sanitize(String html)
	{
		if (html == null)
		{
			return null;
		}
		if (factory == null)
		{
			factory = build(features);
		}
		return factory.sanitize(html);
	}

	/**
	 * @return OWASP's policy of the features
	 */
	private static PolicyFactory build(Set<RichTextFeature> features)
	{
		HtmlPolicyBuilder builder = new HtmlPolicyBuilder()
			.allowElements("p", "br")
			.allowStandardUrlProtocols();
		if (features.contains(RichTextFeature.HEADING))
		{
			builder.allowElements(HEADINGS);
		}
		allow(builder, features, RichTextFeature.BOLD, "strong", "b");
		allow(builder, features, RichTextFeature.ITALIC, "i", "em");
		allow(builder, features, RichTextFeature.UNDERLINE, "u");
		allow(builder, features, RichTextFeature.STRIKE, "s", "del");
		allow(builder, features, RichTextFeature.CODE, "code");
		allow(builder, features, RichTextFeature.BLOCKQUOTE, "blockquote");
		allow(builder, features, RichTextFeature.HORIZONTAL_RULE, "hr");
		if (features.contains(RichTextFeature.LINK))
		{
			builder.allowElements("a")
				.allowAttributes("href").onElements("a")
				.allowAttributes("target").matching(true, "_blank").onElements("a")
				.requireRelsOnLinks("noopener", "noreferrer", "nofollow");
		}
		boolean bullets = features.contains(RichTextFeature.BULLET_LIST) ||
			features.contains(RichTextFeature.TASK_LIST);
		boolean numbers = features.contains(RichTextFeature.ORDERED_LIST);
		if (bullets)
		{
			builder.allowElements("ul");
		}
		if (numbers)
		{
			builder.allowElements("ol")
				.allowAttributes("start").matching(NUMBER).onElements("ol");
		}
		if (bullets || numbers)
		{
			builder.allowElements("li");
		}
		if (features.contains(RichTextFeature.TASK_LIST))
		{
			// <ul class="todo-list"><li><label class="todo-list__label"><input type="checkbox"
			// checked="checked" disabled="disabled"><span class="todo-list__label__description">
			builder.allowElements("label", "input", "span")
				.allowAttributes("class").matching(false, "todo-list").onElements("ul")
				.allowAttributes("class").matching(TODO_LABEL_CLASS).onElements("label")
				.allowAttributes("class").matching(false, "todo-list__label__description")
				.onElements("span")
				.allowAttributes("type").matching(false, "checkbox").onElements("input")
				.allowAttributes("checked").matching(true, "checked", "").onElements("input")
				.allowAttributes("disabled").matching(true, "disabled", "").onElements("input");
		}
		if (features.contains(RichTextFeature.CODE_BLOCK))
		{
			builder.allowElements("pre", "code")
				.allowAttributes("class").matching(LANGUAGE_CLASS).onElements("code");
		}
		List<String> figures = new ArrayList<>();
		if (features.contains(RichTextFeature.TABLE))
		{
			figures.add("table");
			builder.allowElements("table", "thead", "tbody", "tr", "th", "td")
				.allowAttributes("colspan", "rowspan").matching(NUMBER).onElements("th", "td");
		}
		if (features.contains(RichTextFeature.IMAGE))
		{
			figures.add("image");
			builder.allowElements("img", "figcaption")
				.allowAttributes("src").onElements("img")
				.allowAttributes("alt").onElements("img");
		}
		if (!figures.isEmpty())
		{
			builder.allowElements("figure")
				.allowAttributes("class").matching(false, figures.toArray(String[]::new))
				.onElements("figure");
		}
		if (features.contains(RichTextFeature.TEXT_ALIGN))
		{
			List<String> aligned = new ArrayList<>(List.of(HEADINGS));
			aligned.add("p");
			if (bullets || numbers)
			{
				aligned.add("li");
			}
			builder.allowAttributes("class").matching(ALIGN_CLASS).onElements(
				aligned.toArray(String[]::new));
		}
		return builder.toFactory();
	}

	private static void allow(HtmlPolicyBuilder builder, Set<RichTextFeature> features,
		RichTextFeature feature, String... elements)
	{
		if (features.contains(feature))
		{
			builder.allowElements(elements);
		}
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof CKEditorPolicy policy && policy.features.equals(features);
	}

	@Override
	public int hashCode()
	{
		return features.hashCode();
	}

	@Override
	public String toString()
	{
		return "CKEditorPolicy" + features;
	}
}
