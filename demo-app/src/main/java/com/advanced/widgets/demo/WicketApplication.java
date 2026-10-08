/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.io.File;

import org.apache.wicket.csp.CSPDirective;
import org.apache.wicket.csp.CSPDirectiveSrcValue;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.settings.JavaScriptLibrarySettings;
import org.apache.wicket.util.string.Strings;

import com.advanced.widgets.demo.autocomplete.AutocompletePage;
import com.advanced.widgets.demo.datatable.BeanDynamicDataTablePage;
import com.advanced.widgets.demo.datatable.DynamicDataTablePage;
import com.advanced.widgets.demo.datatable.FontAwesomeResourceReference;
import com.advanced.widgets.demo.datatable.UsersRowsResource;
import com.advanced.widgets.demo.dropdown.DropdownPage;
import com.advanced.widgets.demo.list.JsonListPage;
import com.advanced.widgets.demo.modal.ModalDialogPage;
import com.advanced.widgets.demo.modal.TeamResource;
import com.advanced.widgets.demo.frameworks.ChoicesResource;
import com.advanced.widgets.demo.frameworks.FrameworksPage;
import com.advanced.widgets.demo.progress.ProgressBarPage;
import com.advanced.widgets.demo.register.RegisterPage;
import com.advanced.widgets.demo.scrollbar.ScrollbarPage;
import com.advanced.widgets.demo.theme.ThemeEditorPage;
import com.advanced.widgets.demo.tree.FilesResource;
import com.advanced.widgets.demo.tree.TreePage;
import com.advanced.widgets.license.Licensing;
import com.advanced.widgets.demo.tree.TreeGridPage;

/**
 * Application object for your web application.
 * If you want to run this application without deploying, run the Start class.
 * 
 * @see com.advanced.widgets.demo.Start#main(String[])
 */
public class WicketApplication extends WebApplication
{
	/** The system property holding the folder of the page store. */
	public static final String PAGE_STORE_PROPERTY = "advanced-widgets.page-store";

	/** The environment variable holding the folder of the page store. */
	public static final String PAGE_STORE_ENVIRONMENT_VARIABLE = "ADVANCED_WIDGETS_PAGE_STORE";

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

		// the demo shows every private component, whatever key the system property or the
		// environment holds: the license limits only the other applications using the library
		Licensing.install(this, DemoLicense.key());

		// the pages of the sessions go to this folder on disk, the servlet container's temporary
		// folder by default; production puts it on a volume of its own
		File pageStore = pageStoreFolder();
		if (pageStore != null)
		{
			getStoreSettings().setFileStoreFolder(pageStore);
		}

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
		mountPage("tree", TreePage.class);
		mountPage("tree-grid", TreeGridPage.class);
		mountPage("progress-bar", ProgressBarPage.class);
		mountPage("theme-editor", ThemeEditorPage.class);
		mountPage("dropdown", DropdownPage.class);
		mountPage("autocomplete", AutocompletePage.class);
		mountPage("scrollbar", ScrollbarPage.class);
		mountPage("modal-dialog", ModalDialogPage.class);
		mountPage("register", RegisterPage.class);
		mountPage("react-svelte", FrameworksPage.class);
		mountResource(ChoicesResource.PATH, ChoicesResource.REFERENCE);
		mountResource(FontAwesomeResourceReference.PATH, FontAwesomeResourceReference.INSTANCE);
		mountResource(UsersRowsResource.PATH, UsersRowsResource.REFERENCE);
		mountResource(FilesResource.PATH, FilesResource.REFERENCE);
		mountResource(TeamResource.PATH, TeamResource.REFERENCE);
	}

	/**
	 * @return the folder of the page store from the system property {@value #PAGE_STORE_PROPERTY}
	 *         or the environment variable {@value #PAGE_STORE_ENVIRONMENT_VARIABLE}, or
	 *         {@code null} to keep Wicket's default
	 */
	static File pageStoreFolder()
	{
		String folder = System.getProperty(PAGE_STORE_PROPERTY);
		if (Strings.isEmpty(folder))
		{
			folder = System.getenv(PAGE_STORE_ENVIRONMENT_VARIABLE);
		}
		return Strings.isEmpty(folder) ? null : new File(folder);
	}
}
