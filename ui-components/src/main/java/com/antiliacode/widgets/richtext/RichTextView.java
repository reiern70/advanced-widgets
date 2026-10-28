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
import java.util.List;
import java.util.Set;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;

import com.antiliacode.widgets.theme.LookBehavior;
import com.antiliacode.widgets.theme.LookPart;

/**
 * The HTML of a {@link RichTextEditor} as a page shows it, read only: its headings, lists, quotes,
 * code, tables, colors and alignment drawn as in the editor, by {@link RichTextEditor#CSS}. Attach
 * it to a {@code <div>}, whose body it replaces.
 * <p>
 * The HTML is {@link #setSanitizer(RichTextSanitizer) cleaned} as it is written into the page,
 * whatever the model holds: HTML saved before the editor cleaned it, or from somewhere else, shows
 * no script. By default with the {@link RichTextPolicy policy} of all the features and the
 * palettes of the view, those of the editor the HTML comes from: of the colors of the text,
 * {@link RichTextColor#PALETTE} unless the page has its own ({@link #setColors(Collection)}), and
 * of the colors of the background, {@link RichTextBackgroundColor#PALETTE} unless the page has its
 * own ({@link #setBackgroundColors(Collection)}).
 *
 * @since 11.0.0
 */
public class RichTextView extends WebComponent
{
	private static final long serialVersionUID = 1L;

	private RichTextSanitizer sanitizer;

	/** The features set, {@code null} for all those of {@link RichTextFeature}. */
	private Set<IRichTextFeature> features;

	private Set<IRichTextColor> colors = RichTextColor.PALETTE;

	private Set<IRichTextBackgroundColor> backgroundColors = RichTextBackgroundColor.PALETTE;

	/** The policy of the palettes, made the first time it cleans. */
	private transient RichTextPolicy policy;

	/**
	 * @param id
	 *            component id
	 * @param model
	 *            the HTML
	 */
	public RichTextView(String id, IModel<String> model)
	{
		super(id, model);
		add(new LookBehavior(LookPart.RICH_TEXT_VIEW));
	}

	/**
	 * @return what cleans the HTML as the page shows it: the one set, or the policy of all the
	 *         features with the palettes of the view
	 */
	public RichTextSanitizer getSanitizer()
	{
		if (sanitizer != null)
		{
			return sanitizer;
		}
		if (policy == null)
		{
			policy = RichTextPolicy.of(features, colors, backgroundColors);
		}
		return policy;
	}

	/**
	 * @param sanitizer
	 *            what cleans the HTML as the page shows it: that of the editor the HTML comes
	 *            from, for one keeping what its features write; {@code null} for the policy of all
	 *            the features with the palettes of the view, as by default
	 * @return {@code this}
	 */
	public RichTextView setSanitizer(RichTextSanitizer sanitizer)
	{
		this.sanitizer = sanitizer;
		return this;
	}

	/**
	 * @return the features whose HTML the view keeps, unless it has a sanitizer of its own: those
	 *         set, or all those of {@link RichTextFeature}
	 */
	public Set<IRichTextFeature> getFeatures()
	{
		return features == null ? RichTextPolicy.ALL.getFeatures() : features;
	}

	/**
	 * @param features
	 *            the features whose HTML the view keeps, those of the editor the HTML comes from,
	 *            its features of the page's own too; {@code null} for all those of
	 *            {@link RichTextFeature}, as by default
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for features {@link IRichTextFeature#checked(Collection)} does not take
	 */
	public RichTextView setFeatures(Collection<? extends IRichTextFeature> features)
	{
		this.features = features == null ? null : IRichTextFeature.checked(features);
		this.policy = null;
		return this;
	}

	/**
	 * @param feature
	 *            a feature whose HTML the view keeps besides the others, one of the page's own
	 * @return {@code this}
	 */
	public RichTextView addFeature(IRichTextFeature feature)
	{
		List<IRichTextFeature> all = new ArrayList<>(getFeatures());
		all.add(feature);
		return setFeatures(all);
	}

	/**
	 * @return the palette of the colors of the text whose classes the HTML keeps, unless the view
	 *         has a sanitizer of its own; unmodifiable: {@link #setColors(Collection)} and
	 *         {@link #addColor(IRichTextColor)} change it
	 */
	public Set<IRichTextColor> getColors()
	{
		return colors;
	}

	/**
	 * Adds a color at the end of the palette of the colors of the text, {@link RichTextColor#PALETTE}
	 * until the page sets one: the color the editor the HTML comes from
	 * {@link RichTextEditor#addColor(IRichTextColor) has too}.
	 *
	 * @param color
	 *            the color, by a name no other color of the palette has
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a color {@link IRichTextColor#palette(Collection)} does not take
	 */
	public RichTextView addColor(IRichTextColor color)
	{
		List<IRichTextColor> palette = new ArrayList<>(colors);
		palette.add(color);
		return setColors(palette);
	}

	/**
	 * @param colors
	 *            the palette of the colors of the text whose classes the HTML keeps, that of the
	 *            editor the HTML comes from, {@link RichTextColor#PALETTE} by default; {@code null}
	 *            or empty for none
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a palette {@link IRichTextColor#palette(Collection)} does not take
	 */
	public RichTextView setColors(Collection<? extends IRichTextColor> colors)
	{
		this.colors = colors == null ? Collections.emptySet() : IRichTextColor.palette(colors);
		this.policy = null;
		return this;
	}

	/**
	 * @return the palette of the colors of the background whose classes the HTML keeps, unless the
	 *         view has a sanitizer of its own; unmodifiable:
	 *         {@link #setBackgroundColors(Collection)} and
	 *         {@link #addBackgroundColor(IRichTextBackgroundColor)} change it
	 */
	public Set<IRichTextBackgroundColor> getBackgroundColors()
	{
		return backgroundColors;
	}

	/**
	 * Adds a color at the end of the palette of the colors of the background,
	 * {@link RichTextBackgroundColor#PALETTE} until the page sets one: the color the editor the HTML
	 * comes from {@link RichTextEditor#addBackgroundColor(IRichTextBackgroundColor) has too}.
	 *
	 * @param color
	 *            the color, by a name no other color of the palette has
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a color {@link IRichTextBackgroundColor#palette(Collection)} does not take
	 */
	public RichTextView addBackgroundColor(IRichTextBackgroundColor color)
	{
		List<IRichTextBackgroundColor> palette = new ArrayList<>(backgroundColors);
		palette.add(color);
		return setBackgroundColors(palette);
	}

	/**
	 * @param backgroundColors
	 *            the palette of the colors of the background whose classes the HTML keeps, that of
	 *            the editor the HTML comes from, {@link RichTextBackgroundColor#PALETTE} by
	 *            default; {@code null} or empty for none
	 * @return {@code this}
	 * @throws IllegalArgumentException
	 *             for a palette {@link IRichTextBackgroundColor#palette(Collection)} does not take
	 */
	public RichTextView setBackgroundColors(
		Collection<? extends IRichTextBackgroundColor> backgroundColors)
	{
		this.backgroundColors = backgroundColors == null ? Collections.emptySet() :
			IRichTextBackgroundColor.palette(backgroundColors);
		this.policy = null;
		return this;
	}

	/**
	 * @return the HTML of the model, cleaned; empty for none
	 */
	public String getHtml()
	{
		Object html = getDefaultModelObject();
		String clean = html == null ? null : getSanitizer().sanitize(html.toString());
		return clean == null ? "" : clean;
	}

	@Override
	public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
	{
		replaceComponentTagBody(markupStream, openTag, getHtml());
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		super.onComponentTag(tag);
		if (tag.isOpenClose())
		{
			tag.setType(org.apache.wicket.markup.parser.XmlTag.TagType.OPEN);
		}
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(RichTextEditor.CSS));
	}
}
