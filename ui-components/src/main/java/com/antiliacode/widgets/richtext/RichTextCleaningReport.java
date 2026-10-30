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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.wicket.util.io.IClusterable;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.PolicyFactory;

import com.github.openjson.JSONObject;

/**
 * What the cleaning of a rich text removed: the elements and the attributes, by name, each with the
 * number of times, never their content. The server's cleaning reports it
 * ({@link IRichTextSanitizer#sanitizeAndReport(String)}), and so does the editor in the browser
 * for what a paste lost ({@link RichTextEditorPasteCleanedEvent}).
 * <p>
 * The names are those of HTML, in small letters: {@code script}, {@code onclick}, {@code a} for a
 * link and {@code img} for an image, also when the text is Markdown or Tiptap's JSON.
 *
 * @since 11.0.0
 */
public final class RichTextCleaningReport implements IClusterable
{
	private static final long serialVersionUID = 1L;

	/** Nothing removed. */
	public static final RichTextCleaningReport EMPTY = new RichTextCleaningReport(Map.of(),
		Map.of());

	/** The most names a report read from the browser takes, of elements and of attributes each. */
	public static final int MAX_NAMES = 64;

	/** A name of an element or an attribute as a report keeps it. */
	private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9:_-]{0,63}");

	private final Map<String, Integer> elements;

	private final Map<String, Integer> attributes;

	private RichTextCleaningReport(Map<String, Integer> elements, Map<String, Integer> attributes)
	{
		this.elements = Collections.unmodifiableMap(new LinkedHashMap<>(elements));
		this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
	}

	/**
	 * @param elements
	 *            the elements removed, by name, with the number of times
	 * @param attributes
	 *            the attributes removed, by name, with the number of times
	 * @return the report, those whose name is no name of HTML or whose number is not positive left
	 *         out; {@link #EMPTY} for none
	 */
	public static RichTextCleaningReport of(Map<String, Integer> elements,
		Map<String, Integer> attributes)
	{
		Builder builder = builder();
		if (elements != null)
		{
			elements.forEach((name, count) -> builder.add(builder.elements, name, count));
		}
		if (attributes != null)
		{
			attributes.forEach((name, count) -> builder.add(builder.attributes, name, count));
		}
		return builder.build();
	}

	/**
	 * Reads a report the browser sent, as the editor in the browser tells what a paste lost:
	 * {@code {"elements": {"script": 1}, "attributes": {"onclick": 2}}}. It comes from the browser
	 * and is not trusted: names that are no names of HTML, numbers that are not positive whole
	 * numbers, and the names after {@value #MAX_NAMES} of each kind are left out.
	 *
	 * @param values
	 *            what the browser sent, maybe {@code null}
	 * @return the report; {@link #EMPTY} for none
	 */
	public static RichTextCleaningReport fromJson(JSONObject values)
	{
		if (values == null)
		{
			return EMPTY;
		}
		Builder builder = builder();
		read(values.optJSONObject("elements"), builder, builder.elements);
		read(values.optJSONObject("attributes"), builder, builder.attributes);
		return builder.build();
	}

	private static void read(JSONObject names, Builder builder, Map<String, Integer> into)
	{
		if (names == null)
		{
			return;
		}
		for (String name : names.keySet())
		{
			if (into.size() >= MAX_NAMES)
			{
				return;
			}
			Object count = names.opt(name);
			if (count instanceof Number number && number.doubleValue() == number.longValue() &&
				number.longValue() > 0)
			{
				builder.add(into, name, (int)Math.min(number.longValue(), Integer.MAX_VALUE));
			}
		}
	}

	/**
	 * Cleans HTML with a policy of OWASP's, telling what it removed. A {@code rel} the policy
	 * writes anew on a link ({@link RichTextHtmlRules#requireRelsOnLinks(String...)}) is not
	 * counted: the link keeps one, the one the policy requires.
	 *
	 * @param policy
	 *            the policy
	 * @param html
	 *            the HTML, maybe {@code null}
	 * @return the HTML cleaned, {@code null} for {@code null}, with what was removed
	 */
	public static RichTextSanitized sanitize(PolicyFactory policy, String html)
	{
		if (html == null)
		{
			return new RichTextSanitized(null, EMPTY);
		}
		Builder builder = builder();
		String clean = policy.sanitize(html, new HtmlChangeListener<Builder>()
		{
			@Override
			public void discardedTag(Builder report, String elementName)
			{
				report.addElement(elementName);
			}

			@Override
			public void discardedAttributes(Builder report, String tagName,
				String... attributeNames)
			{
				for (String attribute : attributeNames)
				{
					if (!("a".equals(tagName) && "rel".equals(attribute)))
					{
						report.addAttribute(attribute);
					}
				}
			}
		}, builder);
		return new RichTextSanitized(clean, builder.build());
	}

	/**
	 * @return a builder of a report, counting the elements and the attributes it is told of
	 */
	public static Builder builder()
	{
		return new Builder();
	}

	/**
	 * @return whether nothing was removed
	 */
	public boolean isEmpty()
	{
		return elements.isEmpty() && attributes.isEmpty();
	}

	/**
	 * @return the elements removed, by name, with the number of times, in the order they were first
	 *         removed
	 */
	public Map<String, Integer> getElements()
	{
		return elements;
	}

	/**
	 * @return the attributes removed, by name, with the number of times, in the order they were
	 *         first removed
	 */
	public Map<String, Integer> getAttributes()
	{
		return attributes;
	}

	/**
	 * @return the names of what was removed, the elements, then the attributes
	 */
	public List<String> getNames()
	{
		List<String> names = new ArrayList<>(elements.keySet());
		attributes.keySet().stream().filter(name -> !names.contains(name)).forEach(names::add);
		return names;
	}

	/**
	 * @return the report as the browser tells it, {@code {"elements": {...}, "attributes": {...}}}
	 */
	public JSONObject toJson()
	{
		return new JSONObject().put("elements", new JSONObject(elements))
			.put("attributes", new JSONObject(attributes));
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof RichTextCleaningReport report && report.elements.equals(elements) &&
			report.attributes.equals(attributes);
	}

	@Override
	public int hashCode()
	{
		return elements.hashCode() * 31 + attributes.hashCode();
	}

	/**
	 * @return the names of what was removed, separated by commas: {@code script, onclick}
	 */
	@Override
	public String toString()
	{
		return String.join(", ", getNames());
	}

	/**
	 * Counts what a cleaning removes, as it removes it.
	 */
	public static final class Builder
	{
		private final Map<String, Integer> elements = new LinkedHashMap<>();

		private final Map<String, Integer> attributes = new LinkedHashMap<>();

		private Builder()
		{
		}

		/**
		 * @param name
		 *            the name of an element removed, once more
		 * @return {@code this}
		 */
		public Builder addElement(String name)
		{
			add(elements, name, 1);
			return this;
		}

		/**
		 * @param name
		 *            the name of an attribute removed, once more
		 * @return {@code this}
		 */
		public Builder addAttribute(String name)
		{
			add(attributes, name, 1);
			return this;
		}

		private void add(Map<String, Integer> into, String name, Integer count)
		{
			String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
			if (count != null && count > 0 && NAME.matcher(lower).matches())
			{
				into.merge(lower, count, (a, b) -> (int)Math.min((long)a + b, Integer.MAX_VALUE));
			}
		}

		/**
		 * @return the report of what was removed; {@link RichTextCleaningReport#EMPTY} for
		 *         nothing
		 */
		public RichTextCleaningReport build()
		{
			return elements.isEmpty() && attributes.isEmpty() ? EMPTY :
				new RichTextCleaningReport(elements, attributes);
		}
	}
}
