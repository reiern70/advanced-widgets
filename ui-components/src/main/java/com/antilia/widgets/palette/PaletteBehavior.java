/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.palette;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.scrollbar.ScrollbarBehavior;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

/**
 * Turns a {@code <select multiple>} into a palette: the choices still available on the left, the
 * chosen ones on the right and arrow buttons between them moving the highlighted choices, or all
 * of them, from one side to the other. A double click, Enter or dragging a choice to the other
 * list moves it too. When the {@link #setOrdered(boolean) order matters}, the chosen ones keep the
 * order they were chosen in and are moved up and down with the buttons next to their list, Alt
 * with the arrow keys, or by dragging them; otherwise they show in the order of the choices. The
 * rows can show their numbers, and each palette can have its own titles over the lists. A filter
 * over the available ones underlines what it matched, how it matches being
 * {@link #setMatcher(IPaletteMatcher) pluggable}.
 * <p>
 * Everything happens in the browser: {@code Wicket.Palette} from {@link #JS} hides the select and
 * draws the lists next to it. The select stays in the page and holds the choice, its selected
 * options in the chosen order, so a form posts them in that order and a
 * {@code ListMultipleChoice} keeps it in its model. A change fires the select's {@code change}
 * event, so an {@code AjaxFormComponentUpdatingBehavior} on it is notified as with a native
 * select. Without JavaScript the native select is shown.
 * <p>
 * The script needs no inline style, so the palette works under a strict Content Security Policy.
 * {@link Palette} is the form component with this behavior.
 *
 * @since 11.0.0
 */
public class PaletteBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The script of the palette, {@code Wicket.Palette}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		PaletteBehavior.class, "antilia-palette.js");

	/** The style sheet of the palette. */
	public static final ResourceReference CSS = new CssResourceReference(PaletteBehavior.class,
		"antilia-palette.css");

	/** The number of choices from which on the available ones have a filter, unless set otherwise. */
	public static final int DEFAULT_FILTER_THRESHOLD = 8;

	/** How many rows a list draws at first and adds at a time, unless set otherwise. */
	public static final int DEFAULT_PAGE_SIZE = 30;

	/** The keys of the texts, under {@code Palette.} in the bundles. */
	private static final String[] TEXTS = { "availableTitle", "selectedTitle", "add", "addAll",
			"remove", "removeAll", "up", "down", "filter", "empty" };

	private boolean ordered;

	private boolean filterable = true;

	private int filterThreshold = DEFAULT_FILTER_THRESHOLD;

	private IPaletteMatcher matcher = PaletteMatcher.ANYWHERE;

	private boolean numbered;

	private int pageSize = DEFAULT_PAGE_SIZE;

	private IModel<String> availableTitle;

	private IModel<String> selectedTitle;

	/**
	 * @return whether the order of the chosen ones matters
	 */
	public boolean isOrdered()
	{
		return ordered;
	}

	/**
	 * @param ordered
	 *            whether the order of the chosen ones matters: they keep the order they were chosen
	 *            in and can be moved up and down
	 * @return {@code this}
	 */
	public PaletteBehavior setOrdered(boolean ordered)
	{
		this.ordered = ordered;
		return this;
	}

	/**
	 * @return whether the available ones can have a filter
	 */
	public boolean isFilterable()
	{
		return filterable;
	}

	/**
	 * @param filterable
	 *            whether the available ones can have a filter, shown from the
	 *            {@link #setFilterThreshold(int) threshold} on; {@code true} unless set otherwise
	 * @return {@code this}
	 */
	public PaletteBehavior setFilterable(boolean filterable)
	{
		this.filterable = filterable;
		return this;
	}

	/**
	 * @return the number of choices from which on the available ones have a filter
	 */
	public int getFilterThreshold()
	{
		return filterThreshold;
	}

	/**
	 * @param filterThreshold
	 *            the number of choices from which on the available ones have a filter: 0 for
	 *            always, {@link Integer#MAX_VALUE} for never
	 * @return {@code this}
	 */
	public PaletteBehavior setFilterThreshold(int filterThreshold)
	{
		this.filterThreshold = Math.max(0, filterThreshold);
		return this;
	}

	/**
	 * @return how the filter matches the choices
	 */
	public IPaletteMatcher getMatcher()
	{
		return matcher;
	}

	/**
	 * @param matcher
	 *            how the filter matches the choices, underlining what it matched:
	 *            {@link PaletteMatcher#ANYWHERE} unless set otherwise, or {@code null}
	 * @return {@code this}
	 */
	public PaletteBehavior setMatcher(IPaletteMatcher matcher)
	{
		this.matcher = matcher == null ? PaletteMatcher.ANYWHERE : matcher;
		return this;
	}

	/**
	 * @return whether the rows of both lists show their number
	 */
	public boolean isNumbered()
	{
		return numbered;
	}

	/**
	 * @param numbered
	 *            whether the rows of both lists show their number, from 1, before their text
	 * @return {@code this}
	 */
	public PaletteBehavior setNumbered(boolean numbered)
	{
		this.numbered = numbered;
		return this;
	}

	/**
	 * @return how many rows a list draws at first, and adds each time it is scrolled to its end
	 */
	public int getPageSize()
	{
		return pageSize;
	}

	/**
	 * Long lists are drawn a page at a time: the first rows, more as the list is scrolled to its
	 * end. The filter searches all choices, drawn or not, and "add all" adds them all.
	 *
	 * @param pageSize
	 *            how many rows a list draws at first, and adds each time it is scrolled to its
	 *            end, {@link #DEFAULT_PAGE_SIZE} unless set otherwise; 0 draws them all
	 * @return {@code this}
	 */
	public PaletteBehavior setPageSize(int pageSize)
	{
		this.pageSize = Math.max(0, pageSize);
		return this;
	}

	/**
	 * @return the title of the list of the available choices, {@code null} for that of the
	 *         bundles, {@code Palette.availableTitle}
	 */
	public IModel<String> getAvailableTitle()
	{
		return availableTitle;
	}

	/**
	 * @param availableTitle
	 *            the title of the list of the available choices, {@code null} for that of the
	 *            bundles, {@code Palette.availableTitle}
	 * @return {@code this}
	 */
	public PaletteBehavior setAvailableTitle(IModel<String> availableTitle)
	{
		this.availableTitle = availableTitle;
		return this;
	}

	/**
	 * @return the title of the list of the chosen ones, {@code null} for that of the bundles,
	 *         {@code Palette.selectedTitle}
	 */
	public IModel<String> getSelectedTitle()
	{
		return selectedTitle;
	}

	/**
	 * @param selectedTitle
	 *            the title of the list of the chosen ones, {@code null} for that of the bundles,
	 *            {@code Palette.selectedTitle}
	 * @return {@code this}
	 */
	public PaletteBehavior setSelectedTitle(IModel<String> selectedTitle)
	{
		this.selectedTitle = selectedTitle;
		return this;
	}

	@Override
	public void bind(Component component)
	{
		super.bind(component);
		component.setOutputMarkupId(true);
	}

	@Override
	public void onComponentTag(Component component, ComponentTag tag)
	{
		super.onComponentTag(component, tag);
		if (!"select".equalsIgnoreCase(tag.getName()))
		{
			throw new WicketRuntimeException(
				"A PaletteBehavior can only be attached to a <select>, not to <" + tag.getName() +
					"> of " + component);
		}
		tag.put("multiple", "multiple");
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		renderHeadItems(response);
		matcher.renderHead(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Palette.attach(" +
			JSONObject.quote(component.getMarkupId()) + ", " + newOptions(component) + ");"));
	}

	/**
	 * @return the options of the script: the texts, whether the order matters, whether the rows
	 *         show their numbers, when to show the filter and how it matches, how many rows to
	 *         draw at a time and, while the order matters, the ids of the chosen ones in their
	 *         order, which the options of the select, in the order of the choices, do not have
	 */
	protected JSONObject newOptions(Component component)
	{
		JSONObject options = new JSONObject();
		options.put("ordered", ordered);
		options.put("numbered", numbered);
		options.put("filterable", filterable);
		options.put("filterThreshold", filterThreshold);
		options.put("match", matcher.getName());
		options.put("pageSize", pageSize);
		for (String text : TEXTS)
		{
			options.put(text, component.getString("Palette." + text));
		}
		putTitle(options, "availableTitle", availableTitle);
		putTitle(options, "selectedTitle", selectedTitle);
		if (ordered && component instanceof FormComponent<?> formComponent)
		{
			JSONArray selected = new JSONArray();
			String value = formComponent.getValue();
			if (!Strings.isEmpty(value))
			{
				for (String id : Strings.split(value, FormComponent.VALUE_SEPARATOR.charAt(0)))
				{
					selected.put(id);
				}
			}
			options.put("selected", selected);
		}
		return options;
	}

	private static void putTitle(JSONObject options, String key, IModel<String> title)
	{
		String text = title == null ? null : title.getObject();
		if (!Strings.isEmpty(text))
		{
			options.put(key, text);
		}
	}

	@Override
	public void detach(Component component)
	{
		super.detach(component);
		if (availableTitle != null)
		{
			availableTitle.detach();
		}
		if (selectedTitle != null)
		{
			selectedTitle.detach();
		}
	}

	/**
	 * Renders what the palette needs in the browser: {@link #JS} and {@link #CSS}, and the scroll
	 * bars of its lists, those of {@link ScrollbarBehavior}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(ScrollbarBehavior.JS));
		response.render(CssHeaderItem.forReference(ScrollbarBehavior.CSS));
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
