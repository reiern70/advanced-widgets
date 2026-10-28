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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.owasp.html.PolicyFactory;

/**
 * The cleaning of the HTML of a {@link RichTextEditor}, with the OWASP Java HTML Sanitizer: it
 * keeps the elements and the attributes the {@link IRichTextFeature features} of the editor write,
 * each {@link IRichTextFeature#allowHtml(RichTextHtmlRules) letting through its own}, and nothing
 * else. No script, no event handler, no style attribute, no element of a feature left
 * out; links and images only to http, https and mailto addresses, or relative ones, and links with
 * {@code rel="noopener noreferrer nofollow"}. Classes only where the editor writes them: the
 * alignment of paragraphs, headings and cells ({@code antilia-text-center}, {@code -right},
 * {@code -justify}), the language of a code block ({@code language-java}) and the colors of the
 * palettes of the editor: {@link IRichTextColor of the text}, on a {@code span}
 * ({@code antilia-color-red}), and {@link IRichTextBackgroundColor of its background}, on a
 * {@code mark} ({@code antilia-background-yellow}).
 * <p>
 * Classes are not harmless: one of the page's own style sheets, a box fixed over the page or text
 * hidden for example, would let the HTML of a user dress itself as the page. So the policy keeps
 * the classes of the colors of its palettes alone, as they are, on those elements.
 * <p>
 * What the editor writes passes unchanged, but for the way the sanitizer writes the HTML: its
 * empty elements, {@code <br />}, and its entities, such as {@code &#39;}. The editor reads it back
 * as it wrote it.
 *
 * @since 11.0.0
 */
public final class RichTextPolicy implements IRichTextSanitizer
{
	private static final long serialVersionUID = 1L;

	/**
	 * The policy of all the features of {@link RichTextFeature}, with the colors of
	 * {@link RichTextColor#PALETTE} and {@link RichTextBackgroundColor#PALETTE}.
	 */
	public static final RichTextPolicy ALL = new RichTextPolicy(List.of(RichTextFeature.values()),
		RichTextColor.PALETTE, RichTextBackgroundColor.PALETTE);

	/** What the browser ignores in an address, which does not hide its scheme then. */
	private static final Pattern IGNORED = Pattern.compile(
		"[\\u0000-\\u0020\\u007f-\\u00a0\\u1680\\u180e\\u2000-\\u2029\\u205f\\u3000\\ufeff]");

	/** The scheme of an address. */
	private static final Pattern SCHEME = Pattern.compile("^([a-zA-Z][a-zA-Z0-9+.-]*):");

	private final Set<IRichTextFeature> features;

	/** The classes of the colors of the text. */
	private final Set<String> colorClasses;

	/** The classes of the colors of the background. */
	private final Set<String> backgroundClasses;

	/** Made the first time it cleans: OWASP's policies are not serializable. */
	private transient PolicyFactory factory;

	private RichTextPolicy(Collection<? extends IRichTextFeature> features,
		Set<IRichTextColor> colors, Set<IRichTextBackgroundColor> backgroundColors)
	{
		this.features = IRichTextFeature.checked(features);
		this.colorClasses = classes(colors, IRichTextColor::getColorClass);
		this.backgroundClasses = classes(backgroundColors,
			IRichTextBackgroundColor::getBackgroundClass);
	}

	private static <C> Set<String> classes(Set<C> colors, Function<C, String> css)
	{
		Set<String> classes = new LinkedHashSet<>();
		colors.forEach(color -> classes.add(css.apply(color)));
		return Collections.unmodifiableSet(classes);
	}

	/**
	 * @param features
	 *            the features of the editor; {@code null} for all those of {@link RichTextFeature}
	 * @return the policy keeping what those features write, with the colors of
	 *         {@link RichTextColor#PALETTE} and {@link RichTextBackgroundColor#PALETTE}
	 */
	public static RichTextPolicy of(Collection<? extends IRichTextFeature> features)
	{
		return of(features, RichTextColor.PALETTE, RichTextBackgroundColor.PALETTE);
	}

	/**
	 * @param features
	 *            the features of the editor, checked as
	 *            {@link IRichTextFeature#checked(Collection)} does; {@code null} for all those of
	 *            {@link RichTextFeature}
	 * @param colors
	 *            the palette of the colors of the text of the editor, checked as
	 *            {@link IRichTextColor#palette(Collection)} does; {@code null} for none
	 * @param backgroundColors
	 *            the palette of the colors of the background, checked as
	 *            {@link IRichTextBackgroundColor#palette(Collection)} does; {@code null} for none
	 * @return the policy keeping what those features write, with the classes of those colors
	 */
	public static RichTextPolicy of(Collection<? extends IRichTextFeature> features,
		Collection<? extends IRichTextColor> colors,
		Collection<? extends IRichTextBackgroundColor> backgroundColors)
	{
		RichTextPolicy policy = new RichTextPolicy(features == null ? ALL.features : features,
			colors == null ? Set.of() : IRichTextColor.palette(colors),
			backgroundColors == null ? Set.of() : IRichTextBackgroundColor.palette(backgroundColors));
		return policy.equals(ALL) ? ALL : policy;
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
	public Set<IRichTextFeature> getFeatures()
	{
		return features;
	}

	/**
	 * @return the classes of the colors of the text the policy keeps, on a {@code span}
	 */
	public Set<String> getColorClasses()
	{
		return colorClasses;
	}

	/**
	 * @return the classes of the colors of the background the policy keeps, on a {@code mark}
	 */
	public Set<String> getBackgroundClasses()
	{
		return backgroundClasses;
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
			factory = RichTextHtmlRules.toPolicy(features, colorClasses, backgroundClasses);
		}
		return factory.sanitize(html);
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof RichTextPolicy policy && policy.features.equals(features) &&
			policy.colorClasses.equals(colorClasses) &&
			policy.backgroundClasses.equals(backgroundClasses);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(features, colorClasses, backgroundClasses);
	}

	@Override
	public String toString()
	{
		return "RichTextPolicy" + features + colorClasses + backgroundClasses;
	}
}
