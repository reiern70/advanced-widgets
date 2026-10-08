/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.frameworks;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.resource.CssResourceReference;
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
 * {@code frontend/demo} into this package; each renders its app of the demo {@code overview} into
 * the element marked with its framework's name, right away. The other demos show their apps in
 * {@link FrameworkTabs}.
 */
public class FrameworksPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

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
	 * @return the element a framework renders its demo into right away, telling it where the
	 *         choices and the users are and the texts in the language of the session
	 */
	private WebMarkupContainer newDemo(String framework)
	{
		WebMarkupContainer demo = FrameworkTabs.newApp(framework, "overview", this::config);
		demo.add(AttributeModifier.replace("data-mount", "true"));
		return demo;
	}

	private JSONObject config()
	{
		JSONObject texts = FrameworkTabs.texts(this, "country", "countryNone", "skills",
			"skillsNone", "chosen", "nothing", "loading", "colleagues", "colleaguesNone",
			"searching", "people", "listLoading", "listEnd", "listEmpty", "picked");
		return new JSONObject()
			.put("choicesUrl", FrameworkTabs.urlOf(this, ChoicesResource.REFERENCE))
			.put("usersUrl", FrameworkTabs.urlOf(this, UsersRowsResource.REFERENCE))
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts));
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
		FrameworkTabs.renderApps(response);
	}
}
