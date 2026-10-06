/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.ResourceModel;

import com.advanced.widgets.collapsible.CollapsiblePanel;
import com.advanced.widgets.theme.Theme;
import com.advanced.widgets.theme.ThemeBehavior;
import com.advanced.widgets.theme.ThemeChoice;

/**
 * The layout of a demo page: a top bar linking to the {@link HomePage}, the demo's title and its
 * explanation in a collapsible panel. Both come from the page's own bundle, under the keys
 * {@code title} and {@code explanation}; the explanation is markup authored with the demo and is
 * written as is.
 * <p>
 * The page has a {@link Theme}, which {@link #newThemeChoice(String, Component...)} lets the user
 * choose and {@link #newThemedContainer(String)} applies to the components of the demo.
 */
public abstract class DemoPage extends WebPage
{
	private static final long serialVersionUID = 1L;

	private Theme theme = Theme.DEFAULT;

	protected DemoPage()
	{
		add(new BookmarkablePageLink<>("home", HomePage.class));
		add(new Label("pageTitle", () -> getString("title")));
		add(new Label("demoTitle", () -> getString("title")));
		add(newExplanation("explanation", () -> getString("explanation", null, ""))
			.setOutputMarkupId(true));
	}

	/**
	 * @return the theme of the page
	 */
	protected IModel<Theme> getThemeModel()
	{
		return LambdaModel.of(() -> theme, chosen -> theme = chosen);
	}

	/**
	 * @return a container in the page's theme, for the components of the demo
	 */
	protected WebMarkupContainer newThemedContainer(String id)
	{
		WebMarkupContainer themed = new WebMarkupContainer(id);
		themed.setOutputMarkupId(true);
		themed.add(new ThemeBehavior(getThemeModel()));
		return themed;
	}

	/**
	 * @return a drop-down choosing the page's theme, repainting the explanation and the given
	 *         components
	 */
	protected ThemeChoice newThemeChoice(String id, Component... themed)
	{
		return new ThemeChoice(id, getThemeModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onThemeChanged(AjaxRequestTarget target)
			{
				target.add(themed);
				target.add(DemoPage.this.get("explanation"));
				DemoPage.this.onThemeChanged(target);
			}
		};
	}

	/**
	 * Called after the user chose another theme with the drop-down of
	 * {@link #newThemeChoice(String, Component...)}, once the themed components are repainted.
	 * Does nothing by default.
	 */
	protected void onThemeChanged(AjaxRequestTarget target)
	{
	}

	/**
	 * @return the component showing the explanation: a collapsible panel in the page's theme
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
		}.setRememberExpanded(true).add(new ThemeBehavior(getThemeModel()));
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forUrl("style.css"));
	}
}
