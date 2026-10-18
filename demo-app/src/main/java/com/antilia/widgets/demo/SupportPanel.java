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
 * What runs where: for each demo, whether it runs in Wicket, React and Svelte and in every theme. Every page shows it in a {@link BrowserModal}, which the
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
