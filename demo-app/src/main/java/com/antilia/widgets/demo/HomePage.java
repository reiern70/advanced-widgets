/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.antilia.widgets.demo.Demos.Demo;
import com.antilia.widgets.demo.account.DemosAuthorization;
import com.antilia.widgets.demo.account.SignInPage;
import com.antilia.widgets.demo.register.RegisterPage;
import com.antilia.widgets.theme.Theme;

/**
 * The public part of the application: the name of the components and what sets them apart, then
 * an index of all the demos by category, which previews the screenshot of the demo and what it
 * shows as the pointer or the focus goes over it. A button of each demo in the index opens what it
 * is about in {@link DemoDetails}, and another one what runs where. The demos themselves are for
 * the visitors who signed in, see {@link DemosAuthorization}.
 */
public class HomePage extends BasePage {
	private static final long serialVersionUID = 1L;

	private static final ResourceReference SCRIPT = new JavaScriptResourceReference(
		HomePage.class, "home-index.js");

	private static final ResourceReference CSS = new CssResourceReference(HomePage.class,
		"home-index.css");

	public HomePage(final PageParameters parameters) {
		super(parameters);

		add(new Label("webSocketStatus", new ResourceModel("connecting"))
			.add(AttributeModifier.replace("data-live", new ResourceModel("live")))
			.add(new LiveStatusBehavior()));

		add(newSignInHint("signInHint"));

		add(new Label("demoCount", Demos.all().size()));
		add(new Label("themeCount", Theme.values().length));

		add(new ListView<>("demoDetails", Demos.all())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<Demo> item)
			{
				item.add(new DemoDetails("details", item.getModelObject()));
			}
		});

		Map<String, List<Demo>> byCategory = new LinkedHashMap<>();
		Demos.all().forEach(demo -> byCategory.computeIfAbsent(demo.category(),
			category -> new ArrayList<>()).add(demo));
		add(new ListView<>("categories", new ArrayList<>(byCategory.keySet()))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<String> item)
			{
				item.add(new Label("category", new ResourceModel("category." + item.getModelObject())));
				item.add(new ListView<>("demos", byCategory.get(item.getModelObject()))
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(ListItem<Demo> item)
					{
						item.add(newIndexLink("link", item.getModelObject()));
						item.add(newDetailsButton("details", item.getModelObject()));
					}
				});
			}
		});
	}

	/**
	 * @return the button opening the dialog about a demo, {@link DemoDetails}
	 */
	static Component newDetailsButton(String id, Demo demo)
	{
		return new WebMarkupContainer(id).add(AttributeModifier.replace(
			BrowserModal.OPENER_ATTRIBUTE, DemoDetails.markupIdOf(demo)));
	}

	/**
	 * @return the link to a demo in the index: its name, whether it runs in React and Svelte too
	 *         and whether it is public, with its screenshot and what it shows for the preview
	 */
	private static Component newIndexLink(String id, Demo demo)
	{
		BookmarkablePageLink<Void> link = new BookmarkablePageLink<>(id, demo.page());
		link.add(AttributeModifier.replace("data-image", () -> RequestCycle.get()
			.getUrlRenderer().renderContextRelativeUrl(demo.imagePath())));
		link.add(AttributeModifier.replace("data-description",
			new ResourceModel(demo.descriptionKey())));
		link.add(new Label("name", new ResourceModel(demo.nameKey())));
		link.add(new WebMarkupContainer("frameworks").setVisible(demo.frameworkApps()));
		link.add(DemoPage.newVisibilityBadge("visibility", demo));
		return link;
	}

	/**
	 * @return the invitation to sign in or register to try the demos, shown to visitors who have
	 *         not signed in while the demos are private
	 */
	private static Component newSignInHint(String id) {
		WebMarkupContainer hint = new WebMarkupContainer(id) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure() {
				super.onConfigure();
				setVisible(!DemoSession.get().isSignedIn() && !DemosAuthorization.isOpen());
			}
		};
		hint.add(new BookmarkablePageLink<>("signIn", SignInPage.class));
		hint.add(new BookmarkablePageLink<>("register", RegisterPage.class));
		return hint;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(CSS));
		response.render(JavaScriptHeaderItem.forReference(SCRIPT).setDefer(true));
	}
}
