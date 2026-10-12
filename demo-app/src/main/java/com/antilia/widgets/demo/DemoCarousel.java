/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.ContextImage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.antilia.widgets.demo.Demos.Demo;

/**
 * A carousel going through demos: the screenshot of each with its name, what it shows and the
 * frameworks it runs in, a link to open it and a button opening what it is about in the
 * {@link DemoDetails} of the page. It moves on by itself every few seconds, unless the visitor
 * prefers reduced motion, and stops while the pointer or the focus is in it; the arrows, the arrow
 * keys and a swipe move it by hand. Without script the slides scroll sideways.
 */
public class DemoCarousel extends Panel
{
	private static final long serialVersionUID = 1L;

	private static final ResourceReference SCRIPT = new JavaScriptResourceReference(
		DemoCarousel.class, "demo-carousel.js");

	private static final ResourceReference CSS = new CssResourceReference(DemoCarousel.class,
		"demo-carousel.css");

	/**
	 * @param id
	 *            the id of the panel
	 * @param demos
	 *            the demos to go through, in order
	 */
	public DemoCarousel(String id, List<Demo> demos)
	{
		super(id);
		setOutputMarkupId(true);
		add(new ListView<>("slides", demos)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Demo> item)
			{
				Demo demo = item.getModelObject();
				item.add(AttributeModifier.replace("aria-label",
					new StringResourceModel("slide", DemoCarousel.this).setParameters(
						item.getIndex() + 1, demos.size())));
				BookmarkablePageLink<Void> shot = new BookmarkablePageLink<>("shot", demo.page());
				shot.add(new ContextImage("image", demo.imagePath())
					.add(AttributeModifier.replace("alt", new ResourceModel(demo.nameKey()))));
				item.add(shot);
				item.add(new Label("category", new ResourceModel(demo.categoryKey())));
				item.add(DemoPage.newVisibilityBadge("visibility", demo));
				item.add(new Label("name", new ResourceModel(demo.nameKey())));
				item.add(new Label("description", new ResourceModel(demo.descriptionKey())));
				item.add(new WebMarkupContainer("react").setVisible(demo.frameworkApps()));
				item.add(new WebMarkupContainer("svelte").setVisible(demo.frameworkApps()));
				item.add(new BookmarkablePageLink<>("open", demo.page()));
				item.add(HomePage.newDetailsButton("details", demo));
			}
		});
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(CSS));
		response.render(JavaScriptHeaderItem.forReference(SCRIPT));
		response.render(OnDomReadyHeaderItem.forScript(
			"AntiliaDemoCarousel.init(document.getElementById('" + getMarkupId() + "'));"));
	}
}
