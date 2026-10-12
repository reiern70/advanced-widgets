/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

import com.antilia.widgets.demo.Demos.Demo;

/**
 * What runs where: for each demo, whether its component is public and whether it runs in Wicket,
 * React and Svelte and in every theme. Every page shows it in a {@link BrowserModal}, which the
 * link in the footer opens.
 */
public class SupportPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	public SupportPanel(String id)
	{
		super(id);

		add(new ListView<>("support", Demos.all())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Demo> item)
			{
				Demo demo = item.getModelObject();
				BookmarkablePageLink<Void> link = new BookmarkablePageLink<>("link", demo.page());
				link.add(new Label("name", new ResourceModel(demo.nameKey())));
				item.add(link);
				item.add(new Label("category", new ResourceModel(demo.categoryKey())));
				item.add(DemoPage.newVisibilityBadge("visibility", demo));
				item.add(newSupported("wicket", true));
				item.add(newSupported("react", demo.frameworkApps()));
				item.add(newSupported("svelte", demo.frameworkApps()));
				item.add(newSupported("themes", true));
			}
		});
	}

	/**
	 * @return a check mark or a dash telling whether a demo supports something, with the words for
	 *         screen readers
	 */
	private static Component newSupported(String id, boolean supported)
	{
		return new Label(id, new ResourceModel(supported ? "supported" : "notSupported"))
			.add(AttributeModifier.replace("class", supported ? "support support--yes"
				: "support support--no"));
	}
}
