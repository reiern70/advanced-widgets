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

import java.util.Optional;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antilia.widgets.demo.Demos.Demo;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.github.openjson.JSONObject;

/**
 * The layout of a demo page: links back to the {@link HomePage} and to the previous and next demo,
 * the demo's title and a button opening its explanation in a {@link BrowserModal}. Both come from
 * the page's own bundle, under the keys {@code title} and {@code explanation}; the explanation is
 * markup authored with the demo and is written as is.
 * <p>
 * A demo that has React and Svelte apps too names them with {@link #getFrameworkDemo()} and gives
 * them their configuration with {@link #getFrameworkConfig()}: the page then shows the
 * {@link FrameworkTabs} below the title, its own components in the Wicket tab.
 */
public abstract class DemoPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	protected DemoPage()
	{
		this(null);
	}

	/**
	 * @param parameters
	 *            the parameters of a page that can be bookmarked, {@code null} for none
	 */
	protected DemoPage(PageParameters parameters)
	{
		super(parameters);
		add(new BookmarkablePageLink<>("back", HomePage.class));
		add(newDemoLink("previous", Demos.previous(getClass())));
		add(newDemoLink("next", Demos.next(getClass())));
		add(new Label("demoTitle", () -> getString("title")));
		BrowserModal explanation = newExplanation("explanation",
			() -> getString("explanation", null, ""));
		add(explanation);
		add(new WebMarkupContainer("about").add(AttributeModifier.replace(
			BrowserModal.OPENER_ATTRIBUTE, explanation::getMarkupId)));
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		// the session opened the private part
		DemoSession.get().recordPrivatePart();
		String demo = getFrameworkDemo();
		add(demo != null ? new FrameworkTabs("frameworkTabs", demo, this::getFrameworkConfig)
			: new WebMarkupContainer("frameworkTabs").setVisible(false));
	}

	/**
	 * @return the name of the demo's React and Svelte apps, the key of the apps in the bundles
	 *         built from {@code frontend/demo}; {@code null}, the default, when there are none
	 */
	protected String getFrameworkDemo()
	{
		return null;
	}

	/**
	 * @return the configuration of the demo's React and Svelte apps, read when the page renders:
	 *         texts in the language of the session, options, initial values and the URLs of
	 *         mounted resources
	 */
	protected JSONObject getFrameworkConfig()
	{
		return new JSONObject();
	}

	/**
	 * @param page
	 *            the page of another demo, made only to read its configuration
	 * @return the configuration of the React and Svelte apps of the demo of the page
	 */
	protected static JSONObject getFrameworkConfig(DemoPage page)
	{
		return page.getFrameworkConfig();
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
		link.add(new Label("name", new ResourceModel(demo.get().nameKey())));
		return link;
	}

	/**
	 * @return the dialog showing the explanation, which the button next to the title opens
	 */
	private BrowserModal newExplanation(String id, IModel<String> explanation)
	{
		return new BrowserModal(id, () -> getString("title"), "52em")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return new Label(id, explanation).setEscapeModelStrings(false);
			}
		};
	}
}
