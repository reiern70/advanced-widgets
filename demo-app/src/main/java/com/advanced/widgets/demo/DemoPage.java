/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import com.advanced.widgets.collapsible.CollapsiblePanel;
import com.advanced.widgets.demo.Demos.Demo;

/**
 * The layout of a demo page: links back to the {@link HomePage} and to the previous and next demo,
 * the demo's title and its explanation in a collapsible panel. Both come from the page's own
 * bundle, under the keys {@code title} and {@code explanation}; the explanation is markup authored
 * with the demo and is written as is.
 */
public abstract class DemoPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	protected DemoPage()
	{
		add(new BookmarkablePageLink<>("back", HomePage.class));
		add(newDemoLink("previous", Demos.previous(getClass())));
		add(newDemoLink("next", Demos.next(getClass())));
		add(new Label("demoTitle", () -> getString("title")));
		add(newExplanation("explanation", () -> getString("explanation", null, ""))
			.setOutputMarkupId(true));
	}

	@Override
	protected String getPageTitle()
	{
		return getString("title") + " · " + super.getPageTitle();
	}

	/**
	 * @return a container for the components of the demo, which a re-render via Ajax can target;
	 *         it is in the session's theme like the whole page
	 */
	protected WebMarkupContainer newThemedContainer(String id)
	{
		WebMarkupContainer themed = new WebMarkupContainer(id);
		themed.setOutputMarkupId(true);
		return themed;
	}

	private static Component newDemoLink(String id, Optional<Demo> demo)
	{
		if (demo.isEmpty())
		{
			return new WebMarkupContainer(id).setVisible(false);
		}
		BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(id, demo.get().page());
		link.add(new Label("name", demo.get().name()));
		return link;
	}

	/**
	 * @return the component showing the explanation: a collapsible panel
	 */
	private Component newExplanation(String id, IModel<String> explanation)
	{
		return new CollapsiblePanel(id, new ResourceModel("aboutThisDemo"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return new Label(id, explanation).setEscapeModelStrings(false);
			}
		}.setRememberExpanded(true);
	}
}
