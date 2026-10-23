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
package com.antiliacode.widgets.demo;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.resource.loader.IStringResourceLoader;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.demo.Demos.Demo;

/**
 * What a demo is about, in a {@link BrowserModal} the home page opens without leaving it: what it
 * shows in short, the frameworks it runs in, the explanation the demo page itself shows, and a
 * button opening the demo. Each demo's dialog has a markup id of its own,
 * {@link #markupIdOf(Demo)}, which the buttons opening it name.
 */
public class DemoDetails extends BrowserModal
{
	private static final long serialVersionUID = 1L;

	private final Demo demo;

	/**
	 * @param id
	 *            the id of the component
	 * @param demo
	 *            the demo the dialog is about
	 */
	public DemoDetails(String id, Demo demo)
	{
		super(id, new ResourceModel(demo.nameKey()), "52em");
		this.demo = demo;
		setMarkupId(markupIdOf(demo));
	}

	/**
	 * @return the markup id of the dialog about a demo
	 */
	public static String markupIdOf(Demo demo)
	{
		return "demo-details-" + demo.id();
	}

	@Override
	protected Component newBody(String id)
	{
		Fragment body = new Fragment(id, "bodyFragment", this);
		body.add(new Label("description", new ResourceModel(demo.descriptionKey())));
		body.add(new Label("category", new ResourceModel(demo.categoryKey())));
		body.add(new WebMarkupContainer("react").setVisible(demo.frameworkApps()));
		body.add(new WebMarkupContainer("svelte").setVisible(demo.frameworkApps()));
		body.add(new Label("explanation", explanationOf(demo.page()))
			.setEscapeModelStrings(false));
		return body;
	}

	@Override
	protected Component newFooter(String id)
	{
		Fragment footer = new Fragment(id, "footerFragment", this);
		footer.add(new BookmarkablePageLink<>("open", demo.page())
			.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.MEDIUM)));
		footer.add(new WebMarkupContainer("close").add(new ButtonBehavior()));
		return footer;
	}

	/**
	 * @param page
	 *            the page of a demo
	 * @return the explanation the page shows, markup from its own bundle, in the language of the
	 *         session; read when the dialog renders
	 */
	static IModel<String> explanationOf(Class<?> page)
	{
		return () -> {
			for (IStringResourceLoader loader : Application.get().getResourceSettings()
				.getStringResourceLoaders())
			{
				String explanation = loader.loadStringResource(page, "explanation",
					Session.get().getLocale(), Session.get().getStyle(), null);
				if (explanation != null)
				{
					return explanation;
				}
			}
			return "";
		};
	}
}
