/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.frameworks;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.advanced.widgets.demo.DemoPage;
import com.advanced.widgets.demo.datatable.UsersRowsResource;
import com.advanced.widgets.autocomplete.AbstractAutocomplete;
import com.advanced.widgets.chips.Chips;
import com.advanced.widgets.dropdown.DropdownBehavior;
import com.advanced.widgets.jsonlist.JsonBasedInfiniteList;
import com.advanced.widgets.scrollbar.ScrollbarBehavior;
import com.github.openjson.JSONObject;

/**
 * Components of ui-components in React and in Svelte, next to each other: the drop-downs, an
 * autocomplete and an infinite list, on the same scripts and style sheets as the Wicket ones, from
 * {@code frontend/core}, wrapped by {@code @advanced-widgets/react} and
 * {@code @advanced-widgets/svelte}. Both load their data from resources the application mounts,
 * the {@link ChoicesResource} and the {@link UsersRowsResource}, and take the theme of the page.
 * <p>
 * The scripts, {@code react-demo.js} and {@code svelte-demo.js}, are built from
 * {@code frontend/demo} into this package; each renders into the elements marked
 * {@code data-framework-demo} with its framework's name.
 */
public class FrameworksPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private static final ResourceReference REACT = new JavaScriptResourceReference(
		FrameworksPage.class, "react-demo.js");

	private static final ResourceReference SVELTE = new JavaScriptResourceReference(
		FrameworksPage.class, "svelte-demo.js");

	private static final ResourceReference CSS = new CssResourceReference(FrameworksPage.class,
		"FrameworksPage.css");

	private static final ResourceReference LIST_CSS = new CssResourceReference(
		JsonBasedInfiniteList.class, "json-infinite-list.css");

	/**
	 * Constructor.
	 */
	public FrameworksPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		themed.add(newDemo("react"));
		themed.add(newDemo("svelte"));
	}

	/**
	 * @return the element a framework renders its demo into, telling it where the choices are and
	 *         the texts in the language of the session
	 */
	private WebMarkupContainer newDemo(String framework)
	{
		WebMarkupContainer demo = new WebMarkupContainer(framework);
		demo.add(AttributeModifier.replace("data-framework-demo", framework));
		demo.add(AttributeModifier.replace("data-choices",
			() -> urlFor(ChoicesResource.REFERENCE, new PageParameters()).toString()));
		demo.add(AttributeModifier.replace("data-users",
			() -> urlFor(UsersRowsResource.REFERENCE, new PageParameters()).toString()));
		demo.add(AttributeModifier.replace("data-texts", () -> texts().toString()));
		return demo;
	}

	private JSONObject texts()
	{
		JSONObject texts = new JSONObject();
		for (String key : new String[] { "country", "countryNone", "skills", "skillsNone",
			"chosen", "nothing", "loading", "colleagues", "colleaguesNone", "searching", "people",
			"listLoading", "listEnd", "listEmpty", "picked" })
		{
			texts.put(key, getString(key));
		}
		texts.put("filter", getString("Dropdown.filter", null, "Filter…"));
		texts.put("noMatch", getString("Dropdown.noMatch", null, "No matches"));
		texts.put("more", getString("Dropdown.more", null, "+{0}"));
		texts.put("remove", getString("Dropdown.remove", null, "Remove {0}"));
		return texts;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		// the style sheets of the components only: the scripts come with the frameworks' bundles
		response.render(CssHeaderItem.forReference(Chips.CSS));
		response.render(CssHeaderItem.forReference(DropdownBehavior.CSS));
		response.render(CssHeaderItem.forReference(AbstractAutocomplete.CSS));
		response.render(CssHeaderItem.forReference(ScrollbarBehavior.CSS));
		response.render(CssHeaderItem.forReference(LIST_CSS));
		response.render(CssHeaderItem.forReference(CSS));
		response.render(JavaScriptHeaderItem.forReference(REACT).setDefer(true));
		response.render(JavaScriptHeaderItem.forReference(SVELTE).setDefer(true));
	}
}
