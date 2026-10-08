/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import org.apache.wicket.csp.CSPDirective;
import org.apache.wicket.csp.CSPDirectiveSrcValue;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.settings.JavaScriptLibrarySettings;

import com.advanced.widgets.demo.autocomplete.AutocompletePage;
import com.advanced.widgets.demo.datatable.BeanDynamicDataTablePage;
import com.advanced.widgets.demo.datatable.DynamicDataTablePage;
import com.advanced.widgets.demo.datatable.FontAwesomeResourceReference;
import com.advanced.widgets.demo.datatable.UsersRowsResource;
import com.advanced.widgets.demo.dropdown.DropdownPage;
import com.advanced.widgets.demo.list.JsonListPage;
import com.advanced.widgets.demo.frameworks.ChoicesResource;
import com.advanced.widgets.demo.frameworks.FrameworksPage;
import com.advanced.widgets.demo.progress.ProgressBarPage;
import com.advanced.widgets.demo.register.RegisterPage;
import com.advanced.widgets.demo.scrollbar.ScrollbarPage;
import com.advanced.widgets.demo.theme.ThemeEditorPage;

/**
 * Application object for your web application.
 * If you want to run this application without deploying, run the Start class.
 * 
 * @see com.advanced.widgets.demo.Start#main(String[])
 */
public class WicketApplication extends WebApplication
{
	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<? extends WebPage> getHomePage()
	{
		return HomePage.class;
	}

	@Override
	public Session newSession(Request request, Response response)
	{
		return new DemoSession(request);
	}

	/**
	 * @see org.apache.wicket.Application#init()
	 */
	@Override
	public void init()
	{
		super.init();

		// leave out <wicket:child> and the like also in development, they would break the layout
		getMarkupSettings().setStripWicketTags(true);

		// Wicket's Ajax runs on the engine the session chose, plain JavaScript by default
		setJavaScriptLibrarySettings(new JavaScriptLibrarySettings()
		{
			@Override
			public ResourceReference getWicketAjaxReference()
			{
				return (Session.exists() ? DemoSession.get().getAjaxEngine() : AjaxEngine.DEFAULT)
					.getReference();
			}
		});

		// allow the application stylesheet (style.css) served from this origin
		getCspSettings().blocking()
			.add(CSPDirective.STYLE_SRC, CSPDirectiveSrcValue.SELF);

		mountPage("dynamic-data-table", DynamicDataTablePage.class);
		mountPage("bean-dynamic-data-table", BeanDynamicDataTablePage.class);
		mountPage("json-infinite-list", JsonListPage.class);
		mountPage("progress-bar", ProgressBarPage.class);
		mountPage("theme-editor", ThemeEditorPage.class);
		mountPage("dropdown", DropdownPage.class);
		mountPage("autocomplete", AutocompletePage.class);
		mountPage("scrollbar", ScrollbarPage.class);
		mountPage("register", RegisterPage.class);
		mountPage("react-svelte", FrameworksPage.class);
		mountResource(ChoicesResource.PATH, ChoicesResource.REFERENCE);
		mountResource(FontAwesomeResourceReference.PATH, FontAwesomeResourceReference.INSTANCE);
		mountResource(UsersRowsResource.PATH, UsersRowsResource.REFERENCE);
	}
}
