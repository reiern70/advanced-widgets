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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * The cleaning of the HTML of a {@link RichTextEditor}, with the OWASP Java HTML Sanitizer: it
 * keeps the elements and the attributes the {@link RichTextFeature features} of the editor write,
 * and nothing else. No script, no event handler, no style attribute, no element of a feature left
 * out; links and images only to http, https and mailto addresses, or relative ones, and links with
 * {@code rel="noopener noreferrer nofollow"}. Classes only where the editor writes them: the
 * alignment of paragraphs, headings and cells ({@code antilia-text-center}, {@code -right},
 * {@code -justify}), the language of a code block ({@code language-java}) and the
 * {@link RichTextColor colors} of the text, on a {@code span} ({@code antilia-color-red}), and of
 * its background, on a {@code mark} ({@code antilia-background-yellow}).
 * <p>
 * What the editor writes passes unchanged, but for the way the sanitizer writes the HTML: its
 * empty elements, {@code <br />}, and its entities, such as {@code &#39;}. The editor reads it back
 * as it wrote it.
 *
 * @since 11.0.0
 */
public final class RichTextPolicy implements RichTextSanitizer
{
	private static final long serialVersionUID = 1L;

	/** The policy of all the features. */
	public static final RichTextPolicy ALL = new RichTextPolicy(EnumSet.allOf(
		RichTextFeature.class));

	/** The classes aligning text. */
	private static final Pattern ALIGN_CLASS = Pattern.compile(
		"antilia-text-(?:center|right|justify)");

	/** The names of the colors of the palette, as their classes end. */
	private static final String COLORS = Arrays.stream(RichTextColor.values())
		.map(RichTextColor::getName).collect(Collectors.joining("|", "(?:", ")"));

	/** The class of a color of the text. */
	private static final Pattern COLOR_CLASS = Pattern.compile(Pattern.quote(
		RichTextColor.COLOR_CLASS) + COLORS);

	/** The class of a color of the background. */
	private static final Pattern BACKGROUND_CLASS = Pattern.compile(Pattern.quote(
		RichTextColor.BACKGROUND_CLASS) + COLORS);

	/** The class of the language of a code block. */
	private static final Pattern LANGUAGE_CLASS = Pattern.compile("language-[A-Za-z0-9_+#.-]{1,40}");

	private static final Pattern NUMBER = Pattern.compile("[0-9]{1,6}");

	/** The widths of the columns a cell spans. */
	private static final Pattern WIDTHS = Pattern.compile("[0-9]{1,5}(?:,[0-9]{1,5}){0,20}");

	/** What the browser ignores in an address, which does not hide its scheme then. */
	private static final Pattern IGNORED = Pattern.compile(
		"[\\u0000-\\u0020\\u007f-\\u00a0\\u1680\\u180e\\u2000-\\u2029\\u205f\\u3000\\ufeff]");

	/** The scheme of an address. */
	private static final Pattern SCHEME = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*):");

	private static final String[] HEADINGS = { "h1", "h2", "h3", "h4", "h5", "h6" };

	private final Set<RichTextFeature> features;

	/** Made the first time it cleans: OWASP's policies are not serializable. */
	private transient PolicyFactory factory;

	private RichTextPolicy(Set<RichTextFeature> features)
	{
		this.features = features.isEmpty() ? EnumSet.noneOf(RichTextFeature.class) :
			EnumSet.copyOf(features);
	}

	/**
	 * @param features
	 *            the features of the editor; {@code null} for all of them
	 * @return the policy keeping what those features write
	 */
	public static RichTextPolicy of(Set<RichTextFeature> features)
	{
		if (features == null || features.containsAll(ALL.features))
		{
			return ALL;
		}
		return new RichTextPolicy(features);
	}

	/**
	 * Whether a link or an image may go to an address, as the editor in the browser tells
	 * ({@code isSafeUrl}): an http, https or mailto address, or a relative one; not
	 * {@code javascript:}, {@code data:} and so on. Spaces and control characters the browser
	 * ignores do not hide a scheme.
	 *
	 * @param url
	 *            the address
	 * @return whether the cleaning keeps it
	 */
	public static boolean isSafeUrl(String url)
	{
		if (url == null)
		{
			return false;
		}
		String bare = IGNORED.matcher(url).replaceAll("");
		if (bare.isEmpty())
		{
			return false;
		}
		Matcher scheme = SCHEME.matcher(bare);
		if (!scheme.find())
		{
			return true;
		}
		String name = scheme.group(1).toLowerCase(Locale.ROOT);
		return name.equals("http") || name.equals("https") || name.equals("mailto");
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
		allow(builder, features, RichTextFeature.ITALIC, "em", "i");
		allow(builder, features, RichTextFeature.UNDERLINE, "u");
		allow(builder, features, RichTextFeature.STRIKE, "s", "del");
		allow(builder, features, RichTextFeature.CODE, "code");
		if (features.contains(RichTextFeature.TEXT_COLOR))
		{
			builder.allowElements("span")
				.allowAttributes("class").matching(COLOR_CLASS).onElements("span");
		}
		if (features.contains(RichTextFeature.TEXT_BACKGROUND))
		{
			builder.allowElements("mark")
				.allowAttributes("class").matching(BACKGROUND_CLASS).onElements("mark");
		}
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
				.allowAttributes("start").matching(NUMBER).onElements("ol")
				.allowAttributes("type").matching(false, "1", "a", "A", "i", "I").onElements("ol");
		}
		if (bullets || numbers)
		{
			builder.allowElements("li");
		}
		if (features.contains(RichTextFeature.TASK_LIST))
		{
			// <ul data-type="taskList"><li data-type="taskItem" data-checked="true"><label>
			// <input type="checkbox" checked="checked"><span></span></label><div>...</div></li>
			builder.allowElements("label", "input", "span", "div")
				.allowWithoutAttributes("span")
				.allowAttributes("data-type").matching(false, "taskList").onElements("ul")
				.allowAttributes("data-type").matching(false, "taskItem").onElements("li")
				.allowAttributes("data-checked").matching(false, "true", "false").onElements("li")
				.allowAttributes("type").matching(false, "checkbox").onElements("input")
				.allowAttributes("checked").matching(true, "checked", "").onElements("input");
		}
		if (features.contains(RichTextFeature.CODE_BLOCK))
		{
			builder.allowElements("pre", "code")
				.allowAttributes("class").matching(LANGUAGE_CLASS).onElements("code");
		}
		if (features.contains(RichTextFeature.TABLE))
		{
			builder.allowElements("table", "thead", "tbody", "tr", "th", "td")
				.allowAttributes("colspan", "rowspan").matching(NUMBER).onElements("th", "td")
				.allowAttributes("colwidth").matching(WIDTHS).onElements("th", "td")
				// the alignment of the columns comes with the table, as in Markdown
				.allowAttributes("class").matching(ALIGN_CLASS).onElements("th", "td");
		}
		if (features.contains(RichTextFeature.IMAGE))
		{
			builder.allowElements("img")
				.allowAttributes("src").onElements("img")
				.allowAttributes("alt", "title").onElements("img")
				.allowAttributes("width", "height").matching(NUMBER).onElements("img");
		}
		if (features.contains(RichTextFeature.TEXT_ALIGN))
		{
			String[] aligned = new String[HEADINGS.length + 1];
			aligned[0] = "p";
			System.arraycopy(HEADINGS, 0, aligned, 1, HEADINGS.length);
			builder.allowAttributes("class").matching(ALIGN_CLASS).onElements(aligned);
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
		return other instanceof RichTextPolicy policy && policy.features.equals(features);
	}

	@Override
	public int hashCode()
	{
		return features.hashCode();
	}

	@Override
	public String toString()
	{
		return "RichTextPolicy" + features;
	}
}
