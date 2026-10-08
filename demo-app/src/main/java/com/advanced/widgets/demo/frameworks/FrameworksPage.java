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
import com.advanced.widgets.dropdown.DropdownBehavior;
import com.github.openjson.JSONObject;

/**
 * The drop-down of ui-components in React and in Svelte, next to each other: the same script and
 * style sheet as the Wicket {@code Dropdown}, from {@code frontend/core}, wrapped by
 * {@code @advanced-widgets/react} and {@code @advanced-widgets/svelte}. Both load their choices
 * from the {@link ChoicesResource} the application mounts, and take the theme of the page.
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
		demo.add(AttributeModifier.replace("data-texts", () -> texts().toString()));
		return demo;
	}

	private JSONObject texts()
	{
		JSONObject texts = new JSONObject();
		for (String key : new String[] { "country", "countryNone", "skills", "skillsNone",
			"chosen", "nothing", "loading" })
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
		response.render(CssHeaderItem.forReference(DropdownBehavior.CSS));
		response.render(CssHeaderItem.forReference(CSS));
		response.render(JavaScriptHeaderItem.forReference(REACT).setDefer(true));
		response.render(JavaScriptHeaderItem.forReference(SVELTE).setDefer(true));
	}
}
