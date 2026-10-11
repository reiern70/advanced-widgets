/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
import org.apache.wicket.util.string.Strings;

import com.antilia.widgets.collapsible.CollapsiblePanel;
import com.antilia.widgets.demo.Demos.Demo;
import com.antilia.widgets.demo.frameworks.FrameworkTabs;
import com.github.openjson.JSONObject;

/**
 * The layout of a demo page: links back to the {@link HomePage} and to the previous and next demo,
 * the demo's title and its explanation in a collapsible panel. Both come from the page's own
 * bundle, under the keys {@code title} and {@code explanation}; the explanation is markup authored
 * with the demo and is written as is.
 * <p>
 * A demo that has React and Svelte apps too names them with {@link #getFrameworkDemo()} and gives
 * them their configuration with {@link #getFrameworkConfig()}: the page then shows the
 * {@link FrameworkTabs} below the explanation, its own components in the Wicket tab.
 */
public abstract class DemoPage extends BasePage
{
	private static final long serialVersionUID = 1L;

	/** A closed lock: the component is private for now. */
	private static final String LOCK = "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 " +
		"2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 " +
		".9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z";

	/** An open lock: the component is public. */
	private static final String LOCK_OPEN = "M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 " +
		"2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 " +
		"3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 " +
		"12H6V10h12v10z";

	protected DemoPage()
	{
		add(new BookmarkablePageLink<>("back", HomePage.class));
		add(newDemoLink("previous", Demos.previous(getClass())));
		add(newDemoLink("next", Demos.next(getClass())));
		add(new Label("demoTitle", () -> getString("title")));
		add(Demos.of(getClass()).<Component> map(demo -> newVisibilityBadge("visibility", demo))
			.orElseGet(() -> new WebMarkupContainer("visibility").setVisible(false)));
		add(newExplanation("explanation", () -> getString("explanation", null, ""))
			.setOutputMarkupId(true));
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

	/**
	 * @return the icon telling whether the component of a demo is public or private for now
	 */
	static Component newVisibilityBadge(String id, Demo demo)
	{
		return newVisibilityBadge(id, demo.publicComponent());
	}

	/**
	 * @return the icon telling whether a component is public or private for now: an open or a
	 *         closed lock, with the words as tooltip and for screen readers
	 */
	public static Component newVisibilityBadge(String id, boolean publicComponent)
	{
		String key = publicComponent ? "component.public" : "component.private";
		Label badge = new Label(id);
		badge.setDefaultModel(() -> visibilityIcon(publicComponent, badge));
		return badge.setEscapeModelStrings(false)
			.add(AttributeModifier.replace("class", publicComponent
				? "visibility visibility--public" : "visibility visibility--private"))
			.add(AttributeModifier.replace("title", new ResourceModel(key + ".help")));
	}

	/**
	 * @param publicComponent
	 *            whether the component is public, else private for now
	 * @param component
	 *            the component whose bundles have the words
	 * @return the markup of the icon telling whether a component is public or private: an open or
	 *         a closed lock and the words, hidden but for screen readers
	 */
	public static String visibilityIcon(boolean publicComponent, Component component)
	{
		String path = publicComponent ? LOCK_OPEN : LOCK;
		String text = Strings.escapeMarkup(component.getString(
			publicComponent ? "component.public" : "component.private")).toString();
		return "<svg viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" aria-hidden=\"true\" " +
			"focusable=\"false\"><path fill=\"currentColor\" d=\"" + path + "\"/></svg>" +
			"<span class=\"visually-hidden\">" + text + "</span>";
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
