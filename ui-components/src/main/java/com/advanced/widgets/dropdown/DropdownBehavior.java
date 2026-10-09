/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.dropdown;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;

import com.advanced.widgets.chips.Chips;
import com.advanced.widgets.scrollbar.ScrollbarBehavior;
import com.github.openjson.JSONObject;

/**
 * Turns a {@code <select>}, single or multiple, into a drop-down that looks the same in every
 * browser and takes the colors of the {@link com.advanced.widgets.theme.Theme theme} it is in.
 * <p>
 * Everything happens in the browser: {@code Wicket.Dropdown} from {@link #JS} hides the select and
 * puts a button and a list of its options next to it. Opening, filtering and choosing need no
 * request to the server. The select stays in the page and holds the choice: a form posts it as
 * always, and a choice fires the select's {@code change} event, so an
 * {@code AjaxFormComponentUpdatingBehavior} on it is notified as with a native select. Without
 * JavaScript the native select is shown.
 * <p>
 * The list is used with the mouse, touch or the keyboard: the arrow keys move through the options,
 * Enter or Space chooses, Escape closes and typing into the filter shows only the options whose
 * text contains it. A multiple select keeps the list open while options are toggled and shows the
 * chosen ones in its button.
 * <p>
 * The script needs no inline style, so the drop-down works under a strict Content Security
 * Policy. {@link Dropdown} and {@link MultiDropdown} are the form components with this behavior.
 *
 * @since 11.0.0
 */
public class DropdownBehavior extends Behavior
{
	private static final long serialVersionUID = 1L;

	/** The script of the drop-down, {@code Wicket.Dropdown}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(
		DropdownBehavior.class, "wicket-dropdown.js");

	/** The style sheet of the drop-down. */
	public static final ResourceReference CSS = new CssResourceReference(DropdownBehavior.class,
		"wicket-dropdown.css");

	/** The number of options from which on the list has a filter, unless set otherwise. */
	public static final int DEFAULT_FILTER_THRESHOLD = 8;

	private IModel<String> placeholder;

	private int filterThreshold = DEFAULT_FILTER_THRESHOLD;

	/**
	 * @return the text the button shows while nothing is chosen, {@code null} for the default
	 */
	public IModel<String> getPlaceholder()
	{
		return placeholder;
	}

	/**
	 * @param placeholder
	 *            the text the button shows while nothing is chosen, {@code null} for the default
	 * @return {@code this}
	 */
	public DropdownBehavior setPlaceholder(IModel<String> placeholder)
	{
		this.placeholder = placeholder;
		return this;
	}

	/**
	 * @return the number of options from which on the list has a filter
	 */
	public int getFilterThreshold()
	{
		return filterThreshold;
	}

	/**
	 * @param filterThreshold
	 *            the number of options from which on the list has a filter: 0 for always,
	 *            {@link Integer#MAX_VALUE} for never
	 * @return {@code this}
	 */
	public DropdownBehavior setFilterThreshold(int filterThreshold)
	{
		this.filterThreshold = Math.max(0, filterThreshold);
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
				"A DropdownBehavior can only be attached to a <select>, not to <" + tag.getName() +
					"> of " + component);
		}
	}

	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		super.renderHead(component, response);
		renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Dropdown.attach(" +
			JSONObject.quote(component.getMarkupId()) + ", " + newOptions(component) + ");"));
	}

	@Override
	public void detach(Component component)
	{
		super.detach(component);
		if (placeholder != null)
		{
			placeholder.detach();
		}
	}

	/**
	 * @return the options of the script: the texts and when to show the filter
	 */
	protected JSONObject newOptions(Component component)
	{
		JSONObject options = new JSONObject();
		options.put("filterThreshold", filterThreshold);
		String chosen = null;
		if (placeholder != null)
		{
			IModel<String> text = placeholder instanceof IComponentAssignedModel<String> assigned
				? assigned.wrapOnAssignment(component) : placeholder;
			chosen = text.getObject();
			text.detach();
		}
		options.put("placeholder", chosen != null ? chosen
			: component.getString("Dropdown.placeholder", null, "Choose…"));
		options.put("filter", component.getString("Dropdown.filter", null, "Filter…"));
		options.put("noMatch", component.getString("Dropdown.noMatch", null, "No matches"));
		options.put("more", component.getString("Dropdown.more", null, "+{0}"));
		options.put("remove", component.getString("Dropdown.remove", null, "Remove {0}"));
		return options;
	}

	/**
	 * Renders what the drop-down needs in the browser: {@link #JS} and {@link #CSS}, and the
	 * scroll bars of its list, those of {@link ScrollbarBehavior}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		Chips.renderHeadItems(response);
		response.render(JavaScriptHeaderItem.forReference(ScrollbarBehavior.JS));
		response.render(CssHeaderItem.forReference(ScrollbarBehavior.CSS));
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
