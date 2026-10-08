/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo.scrollbar;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.advanced.widgets.demo.DemoPage;
import com.advanced.widgets.demo.frameworks.FrameworkTabs;
import com.advanced.widgets.scrollbar.ScrollbarBehavior;
import com.advanced.widgets.scrollbar.ScrollbarBehavior.Axis;
import com.github.openjson.JSONObject;

/**
 * {@link ScrollbarBehavior} on scrolling divs: vertically, horizontally and both ways, next to a
 * div with the browser's own scroll bar, and a div whose content grows via Ajax. The React and
 * Svelte tabs show the same boxes with their {@code Scrollable}, the last one growing in the app.
 */
public class ScrollbarPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	private int lines = 20;

	/**
	 * Constructor.
	 */
	public ScrollbarPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		themed.add(new WebMarkupContainer("native").add(lines("lines", 40)));
		themed.add(new WebMarkupContainer("vertical").add(lines("lines", 40))
			.add(new ScrollbarBehavior()));
		themed.add(new WebMarkupContainer("horizontal").add(new ScrollbarBehavior(Axis.X)));
		themed.add(new WebMarkupContainer("both").add(lines("lines", 60))
			.add(new ScrollbarBehavior()));

		WebMarkupContainer growing = new WebMarkupContainer("growing");
		growing.add(new ListView<>("lines", () -> range(lines))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Integer> item)
			{
				item.add(new Label("line", new StringResourceModel("line", this)
					.setParameters(item.getModelObject())));
			}
		});
		growing.add(new ScrollbarBehavior());
		themed.add(growing);
		themed.add(new AjaxLink<Void>("addLines")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				lines += 20;
				target.add(growing);
			}
		});
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "scrollbar";
	}

	/**
	 * @return the texts, a line's with {0} for its number, and the lines of the growing box
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		// the lines are patterns, {0} standing for their numbers
		JSONObject texts = FrameworkTabs.texts(this, "native", "vertical", "horizontal", "both",
			"growing", "addLines", "line", "line.long");
		return new JSONObject().put("texts", texts).put("lines", lines);
	}

	private static ListView<Integer> lines(String id, int count)
	{
		return new ListView<>(id, range(count))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Integer> item)
			{
				item.add(new Label("line", new StringResourceModel("line.long", this)
					.setParameters(item.getModelObject())));
			}
		};
	}

	private static List<Integer> range(int count)
	{
		return IntStream.rangeClosed(1, count).boxed().toList();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(ScrollbarPage.class, "ScrollbarPage.css")));
	}
}
