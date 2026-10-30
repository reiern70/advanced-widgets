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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.wicket.util.lang.Args;
import org.owasp.html.AttributePolicy;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

/**
 * What the {@link IRichTextFeature features} of an editor let through the cleaning of its HTML,
 * {@link RichTextPolicy}: each feature {@link IRichTextFeature#allowHtml(RichTextHtmlRules) adds}
 * the elements and the attributes it writes, and nothing else passes. Paragraphs and line breaks
 * always do; links and images go only to http, https and mailto addresses, or relative ones. An
 * image whose address does not pass goes whole, rather than staying without its picture.
 * <p>
 * With {@link #isImagesFromSiteOnly() the images of the site alone}, every address the browser
 * loads by itself must be {@link RichTextPolicy#isSiteUrl(String) the site's own}, whichever rule
 * let the attribute through: {@code src}, {@code srcset}, {@code poster}, {@code background} and
 * {@code data} on any element, and {@code href} on any but a link, {@code a} or {@code area}.
 * <p>
 * An attribute passes on the elements the rules allow, not on others: allowing a class on
 * {@code h1} lets no heading through. Rules of several features for the same attribute of the same
 * element add up: a value passes when one of them allows it. A feature writing {@code <span class="mention">} keeps its class
 * beside those of the {@link IRichTextColor colors of the text}, each whole.
 *
 * @since 11.0.0
 */
public final class RichTextHtmlRules
{
	private static final AttributePolicy ANY = (element, attribute, value) -> value;

	/** The attributes whose addresses the browser loads by itself, on any element. */
	private static final Set<String> LOADED = Set.of("src", "srcset", "poster", "background",
		"data");

	/** The attributes holding the address of a link on {@link #LINKS}, loaded elsewhere. */
	private static final Set<String> LINKED = Set.of("href", "xlink:href");

	/** The elements whose {@code href} is a link, followed on a click. */
	private static final Set<String> LINKS = Set.of("a", "area");

	/** A candidate of a {@code srcset} ends at a comma. */
	private static final Pattern CANDIDATES = Pattern.compile(",");

	/** The address of a candidate of a {@code srcset} ends at a space. */
	private static final Pattern SPACES = Pattern.compile("\\s+");

	private final Set<IRichTextFeature> features;

	private final Set<String> colorClasses;

	private final Set<String> backgroundClasses;

	private final boolean imagesFromSiteOnly;

	private final Set<String> elements = new LinkedHashSet<>();

	private final Set<String> bare = new LinkedHashSet<>();

	/** The policies of the attributes, by element, then by attribute. */
	private final Map<String, Map<String, List<AttributePolicy>>> attributes =
		new LinkedHashMap<>();

	private final Set<String> rels = new LinkedHashSet<>();

	private RichTextHtmlRules(Set<IRichTextFeature> features, Set<String> colorClasses,
		Set<String> backgroundClasses, boolean imagesFromSiteOnly)
	{
		this.features = features;
		this.colorClasses = colorClasses;
		this.backgroundClasses = backgroundClasses;
		this.imagesFromSiteOnly = imagesFromSiteOnly;
		allowElements("p", "br");
	}

	/**
	 * @param features
	 *            the features of an editor
	 * @param colorClasses
	 *            the classes of its colors of the text
	 * @param backgroundClasses
	 *            the classes of its colors of the background
	 * @return OWASP's policy letting through what the features write, and nothing else
	 */
	public static PolicyFactory toPolicy(Collection<? extends IRichTextFeature> features,
		Set<String> colorClasses, Set<String> backgroundClasses)
	{
		return toPolicy(features, colorClasses, backgroundClasses, false);
	}

	/**
	 * @param features
	 *            the features of an editor
	 * @param colorClasses
	 *            the classes of its colors of the text
	 * @param backgroundClasses
	 *            the classes of its colors of the background
	 * @param imagesFromSiteOnly
	 *            whether images go only to the site's own addresses
	 * @return OWASP's policy letting through what the features write, and nothing else
	 * @since 11.0.0
	 */
	public static PolicyFactory toPolicy(Collection<? extends IRichTextFeature> features,
		Set<String> colorClasses, Set<String> backgroundClasses, boolean imagesFromSiteOnly)
	{
		Set<IRichTextFeature> all = Collections.unmodifiableSet(new LinkedHashSet<>(features));
		RichTextHtmlRules rules = new RichTextHtmlRules(all, Set.copyOf(colorClasses),
			Set.copyOf(backgroundClasses), imagesFromSiteOnly);
		all.forEach(feature -> feature.allowHtml(rules));
		return rules.toFactory();
	}

	/**
	 * @return the features of the editor, whose rules make the cleaning
	 */
	public Set<IRichTextFeature> getFeatures()
	{
		return features;
	}

	/**
	 * @param feature
	 *            a feature
	 * @return whether the editor has it
	 */
	public boolean has(IRichTextFeature feature)
	{
		return features.contains(feature);
	}

	/**
	 * @return the classes of the {@link IRichTextColor colors of the text} of the editor
	 */
	public Set<String> getColorClasses()
	{
		return colorClasses;
	}

	/**
	 * @return the classes of the {@link IRichTextBackgroundColor colors of the background} of the
	 *         editor
	 */
	public Set<String> getBackgroundClasses()
	{
		return backgroundClasses;
	}

	/**
	 * @return whether images go only to the site's own addresses
	 *         ({@link RichTextPolicy#withImagesFromSiteOnly(boolean)}): then every address the
	 *         browser loads by itself passes only if {@link RichTextPolicy#isSiteUrl(String) the
	 *         site's own}, whichever rule let it through
	 * @since 11.0.0
	 */
	public boolean isImagesFromSiteOnly()
	{
		return imagesFromSiteOnly;
	}

	/**
	 * @param names
	 *            elements that pass, without attributes unless allowed
	 * @return {@code this}
	 */
	public RichTextHtmlRules allowElements(String... names)
	{
		for (String name : names)
		{
			elements.add(element(name));
		}
		return this;
	}

	/**
	 * @param names
	 *            elements that pass without any attribute too, such as {@code span}, which the
	 *            sanitizer otherwise drops bare
	 * @return {@code this}
	 */
	public RichTextHtmlRules allowWithoutAttributes(String... names)
	{
		for (String name : names)
		{
			allowElements(name);
			bare.add(element(name));
		}
		return this;
	}

	/**
	 * @param attribute
	 *            an attribute passing with any value, {@code alt} for example; not an address, for
	 *            which see {@link #allowUrl(String, String...)}
	 * @param on
	 *            the elements it passes on
	 * @return {@code this}
	 */
	public RichTextHtmlRules allowAttribute(String attribute, String... on)
	{
		return add(attribute, ANY, on);
	}

	/**
	 * @param attribute
	 *            an attribute
	 * @param values
	 *            what its whole value matches to pass
	 * @param on
	 *            the elements it passes on
	 * @return {@code this}
	 */
	public RichTextHtmlRules allowAttribute(String attribute, Pattern values, String... on)
	{
		Args.notNull(values, "values");
		return add(attribute, (element, name, value) -> values.matcher(value).matches() ? value :
			null, on);
	}

	/**
	 * @param attribute
	 *            an attribute
	 * @param values
	 *            the values it passes with, whole and of the same case; none for no value
	 * @param on
	 *            the elements it passes on
	 * @return {@code this}
	 */
	public RichTextHtmlRules allowAttribute(String attribute, Collection<String> values,
		String... on)
	{
		Set<String> allowed = Set.copyOf(Args.notNull(values, "values"));
		if (allowed.isEmpty())
		{
			return this;
		}
		return add(attribute, (element, name, value) -> allowed.contains(value) ? value : null, on);
	}

	/**
	 * @param attribute
	 *            an attribute holding an address, {@code href} or {@code src} for example
	 * @param on
	 *            the elements it passes on
	 * @return {@code this}, the attribute passing with the addresses
	 *         {@link RichTextPolicy#isSafeUrl(String) the editor takes} alone, and with the site's
	 *         own alone where the browser loads them by itself, the {@code src} of an image for
	 *         example, if {@link #isImagesFromSiteOnly() the images are the site's alone}
	 */
	public RichTextHtmlRules allowUrl(String attribute, String... on)
	{
		return add(attribute, (element, name, value) -> RichTextPolicy.isSafeUrl(value) ? value :
			null, on);
	}

	/**
	 * @param values
	 *            what the {@code rel} of every link gets, {@code noopener} for example
	 * @return {@code this}
	 */
	public RichTextHtmlRules requireRelsOnLinks(String... values)
	{
		rels.addAll(List.of(values));
		return this;
	}

	private RichTextHtmlRules add(String attribute, AttributePolicy policy, String... on)
	{
		String name = Args.notEmpty(attribute, "attribute").trim().toLowerCase(Locale.ROOT);
		if (on.length == 0)
		{
			throw new IllegalArgumentException("The attribute " + name + " is on no element");
		}
		for (String each : on)
		{
			String element = element(each);
			attributes.computeIfAbsent(element, key -> new LinkedHashMap<>())
				.computeIfAbsent(name, key -> new ArrayList<>()).add(policy);
		}
		return this;
	}

	private static String element(String name)
	{
		return Args.notEmpty(name, "element").trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * @return whether the browser loads the address of an attribute of an element by itself,
	 *         without a click
	 */
	private static boolean isLoaded(String element, String attribute)
	{
		return LOADED.contains(attribute) ||
			(LINKED.contains(attribute) && !LINKS.contains(element));
	}

	/**
	 * @return whether the address of an attribute is the site's own; for a {@code srcset}, the
	 *         address of each of its candidates
	 */
	private static boolean isSite(String attribute, String value)
	{
		if (!"srcset".equals(attribute))
		{
			return RichTextPolicy.isSiteUrl(value);
		}
		// split at every comma, even one a browser reads as part of an address: a piece more to
		// check, never one less
		for (String candidate : CANDIDATES.split(value))
		{
			String trimmed = candidate.strip();
			if (!trimmed.isEmpty() && !RichTextPolicy.isSiteUrl(SPACES.split(trimmed, 2)[0]))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @return whether the attributes of an element, names and values in turn, hold one
	 */
	private static boolean hasAttribute(List<String> attributes, String name)
	{
		for (int i = 0; i < attributes.size(); i += 2)
		{
			if (name.equals(attributes.get(i)))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * @return OWASP's policy of the rules
	 */
	private PolicyFactory toFactory()
	{
		HtmlPolicyBuilder builder = new HtmlPolicyBuilder().allowStandardUrlProtocols()
			.allowElements(elements.toArray(String[]::new));
		if (!bare.isEmpty())
		{
			builder.allowWithoutAttributes(bare.toArray(String[]::new));
		}
		if (elements.contains("img"))
		{
			// an image whose address did not pass goes whole, its alt and title too
			builder.allowElements((name, attributes) -> hasAttribute(attributes, "src") ? name :
				null, "img");
		}
		attributes.forEach((element, byName) -> byName.forEach((name, policies) -> {
			AttributePolicy any = policies.size() == 1 ? policies.get(0) :
				(on, attribute, value) -> {
					for (AttributePolicy policy : policies)
					{
						String passed = policy.apply(on, attribute, value);
						if (passed != null)
						{
							return passed;
						}
					}
					return null;
				};
			if (imagesFromSiteOnly && isLoaded(element, name))
			{
				AttributePolicy allowed = any;
				any = (on, attribute, value) -> {
					String passed = allowed.apply(on, attribute, value);
					return passed != null && isSite(attribute, passed) ? passed : null;
				};
			}
			builder.allowAttributes(name).matching(any).onElements(element);
		}));
		if (!rels.isEmpty())
		{
			builder.requireRelsOnLinks(rels.toArray(String[]::new));
		}
		return builder.toFactory();
	}
}
