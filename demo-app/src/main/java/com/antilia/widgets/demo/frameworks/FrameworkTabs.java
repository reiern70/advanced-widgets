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
package com.antilia.widgets.demo.frameworks;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptPackageResource;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import com.antilia.widgets.button.ButtonBehavior;
import com.antilia.widgets.demo.DemoLicense;
import com.antilia.widgets.modal.Modal;
import com.antilia.widgets.scrollbar.ScrollbarBehavior;
import com.github.openjson.JSONObject;

/**
 * The tabs of a demo showing it in Wicket, React and Svelte, below its explanation. The Wicket tab
 * is the demo as the page renders it: the page keeps its components where they are, in the element
 * {@code DemoPage} wraps them in, and the tabs only show and hide it. The React and Svelte tabs hold
 * an empty element the app of their framework renders into, carrying the name of the demo and its
 * configuration as JSON: texts in the language of the session, options, initial values and the
 * URLs of mounted resources.
 * <p>
 * The script of the tabs, {@code framework-tabs.js}, mounts an app the first time its tab is shown
 * and remembers the tab chosen across the demos, so a visitor can go through all of them in React.
 * The apps are those of {@code react-demo.js} and {@code svelte-demo.js}, built from
 * {@code frontend/demo}, keyed by the name of the demo.
 */
public class FrameworkTabs extends Panel
{
	private static final long serialVersionUID = 1L;

	/** The frameworks besides Wicket, by the names their apps and tabs go by. */
	public static final String[] FRAMEWORKS = { "react", "svelte" };

	/** The bundle of the React apps. */
	public static final ResourceReference REACT = new BundleReference("react-demo.js");

	/** The bundle of the Svelte apps. */
	public static final ResourceReference SVELTE = new BundleReference("svelte-demo.js");

	private static final ResourceReference SCRIPT = new JavaScriptResourceReference(
		FrameworkTabs.class, "framework-tabs.js");

	private static final ResourceReference CSS = new CssResourceReference(FrameworkTabs.class,
		"framework-tabs.css");

	/**
	 * @param id
	 *            the id of the component
	 * @param demo
	 *            the name of the demo, the key of its apps in the bundles
	 * @param config
	 *            the configuration the apps get, read when the page renders
	 */
	public FrameworkTabs(String id, String demo, IModel<JSONObject> config)
	{
		super(id);
		for (String framework : FRAMEWORKS)
		{
			add(newApp(framework, demo, config));
		}
	}

	/**
	 * @return the element the app of a framework renders the demo into, once its tab is shown
	 */
	static WebMarkupContainer newApp(String framework, String demo, IModel<JSONObject> config)
	{
		return newApp(framework, framework, demo, config);
	}

	/**
	 * An element an app renders into, anywhere in a page that renders the bundles of the apps with
	 * {@link #renderApps(IHeaderResponse)}. The app mounts the first time the element gets the
	 * event framework-app:mount, or as soon as its bundle runs when the element is marked
	 * data-mount.
	 *
	 * @param id
	 *            the id of the component
	 * @param framework
	 *            the framework of the app, one of {@link #FRAMEWORKS}
	 * @param demo
	 *            the name of the demo, the key of its apps in the bundles
	 * @param config
	 *            the configuration the app gets, read when the page renders
	 * @return the element the app renders into
	 */
	public static WebMarkupContainer newApp(String id, String framework, String demo,
		IModel<JSONObject> config)
	{
		WebMarkupContainer app = new WebMarkupContainer(id);
		app.add(AttributeModifier.replace("data-framework-app", framework));
		app.add(AttributeModifier.replace("data-demo", demo));
		// the key of the demo, which the apps activate: the demo shows every private component
		app.add(AttributeModifier.replace("data-config", () -> config.getObject()
			.put("licenseKey", DemoLicense.key()).toString()));
		return app;
	}

	/**
	 * @param component
	 *            the component whose bundles have the texts, a page mostly
	 * @param keys
	 *            the keys of the texts
	 * @return the texts in the language of the session, by their keys
	 */
	public static JSONObject texts(Component component, String... keys)
	{
		JSONObject texts = new JSONObject();
		for (String key : keys)
		{
			texts.put(key, component.getString(key));
		}
		return texts;
	}

	/**
	 * @param component
	 *            the component whose bundles have the texts
	 * @param texts
	 *            where to put them
	 * @return the texts, with those of the drop-down added: the filter, the absence of matches, the
	 *         chips left out and the x of a chip, as the Wicket drop-down has them
	 */
	public static JSONObject withDropdownTexts(Component component, JSONObject texts)
	{
		texts.put("filter", component.getString("Dropdown.filter", null, "Filter…"));
		texts.put("noMatch", component.getString("Dropdown.noMatch", null, "No matches"));
		texts.put("more", component.getString("Dropdown.more", null, "+{0}"));
		texts.put("remove", component.getString("Dropdown.remove", null, "Remove {0}"));
		return texts;
	}

	/**
	 * @return the URL of a mounted resource, for the apps to fetch from
	 */
	public static String urlOf(Component component, ResourceReference reference)
	{
		return component.urlFor(reference, new PageParameters()).toString();
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderApps(response);
		// the buttons of the apps, such as those of their forms
		response.render(CssHeaderItem.forReference(ButtonBehavior.CSS));
		// the dialogs of the apps, and the scroll bars of their bodies, on pages without a Modal
		response.render(CssHeaderItem.forReference(ScrollbarBehavior.CSS));
		response.render(CssHeaderItem.forReference(Modal.CSS));
		response.render(CssHeaderItem.forReference(CSS));
		response.render(JavaScriptHeaderItem.forReference(SCRIPT).setDefer(true));
	}

	/**
	 * Renders the bundles of the apps, deferred.
	 */
	public static void renderApps(IHeaderResponse response)
	{
		response.render(JavaScriptHeaderItem.forReference(REACT).setDefer(true));
		response.render(JavaScriptHeaderItem.forReference(SVELTE).setDefer(true));
	}

	/**
	 * A bundle of apps, which the build minifies already: Wicket's own compressor, which in
	 * deployment mode strips the scripts again, takes the // in a template literal for a comment,
	 * breaking the bundle.
	 */
	private static final class BundleReference extends JavaScriptResourceReference
	{
		private static final long serialVersionUID = 1L;

		BundleReference(String name)
		{
			super(FrameworkTabs.class, name);
		}

		@Override
		public JavaScriptPackageResource getResource()
		{
			JavaScriptPackageResource resource = super.getResource();
			resource.setCompress(false);
			return resource;
		}
	}
}
