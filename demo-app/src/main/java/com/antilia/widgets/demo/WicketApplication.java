/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo;

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
import org.springframework.context.ApplicationContext;

import com.antilia.widgets.demo.account.Accounts;
import com.antilia.widgets.demo.account.DemosAuthorization;
import com.antilia.widgets.demo.account.SignInAttempts;
import com.antilia.widgets.demo.account.SignInPage;
import com.antilia.widgets.demo.admin.AccessTimeResource;
import com.antilia.widgets.demo.admin.AdminPage;
import com.antilia.widgets.demo.autocomplete.AutocompletePage;
import com.antilia.widgets.demo.button.ButtonsPage;
import com.antilia.widgets.demo.contacts.ContactsPage;
import com.antilia.widgets.demo.datatable.BeanDynamicDataTablePage;
import com.antilia.widgets.demo.datatable.DynamicDataTablePage;
import com.antilia.widgets.demo.datatable.MasterDetailPage;
import com.antilia.widgets.demo.datatable.FontAwesomeResourceReference;
import com.antilia.widgets.demo.datatable.UsersRowsResource;
import com.antilia.widgets.demo.data.Accesses;
import com.antilia.widgets.demo.data.Database;
import com.antilia.widgets.demo.datepicker.DatePickerPage;
import com.antilia.widgets.demo.dropdown.DropdownPage;
import com.antilia.widgets.demo.list.JsonListPage;
import com.antilia.widgets.demo.mail.IMailer;
import com.antilia.widgets.demo.modal.ModalDialogPage;
import com.antilia.widgets.demo.modal.TeamResource;
import com.antilia.widgets.demo.palette.PalettePage;
import com.antilia.widgets.demo.split.SplitPanelPage;
import com.antilia.widgets.demo.frameworks.ChoicesResource;
import com.antilia.widgets.demo.progress.ProgressBarPage;
import com.antilia.widgets.demo.register.EmailValidationPage;
import com.antilia.widgets.demo.register.RegisterPage;
import com.antilia.widgets.demo.scrollbar.ScrollbarPage;
import com.antilia.widgets.demo.theme.ThemeEditorPage;
import com.antilia.widgets.demo.tree.TreePage;
import com.antilia.widgets.license.Licensing;
import com.antilia.widgets.demo.tree.TreeGridPage;
import com.antilia.widgets.demo.tree.TreeOfLifeResource;

/**
 * Application object for your web application.
 * If you want to run this application without deploying, run the Start class.
 * 
 * @see com.antilia.widgets.demo.Start#main(String[])
 */
public class WicketApplication extends WebApplication
{
	/** The system property holding the folder of the page store. */
	public static final String PAGE_STORE_PROPERTY = "antilia-widgets.page-store";

	/** The environment variable holding the folder of the page store. */
	public static final String PAGE_STORE_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_PAGE_STORE";

	/**
	 * The system property naming the account every new session is signed in with, so that nobody
	 * signs in on a development machine; {@code jetty:run} sets it to {@code admin}. Never set in
	 * production.
	 */
	public static final String SIGN_IN_AS_PROPERTY = "antilia-widgets.sign-in-as";

	private Accounts accounts;

	private Accesses accesses;

	private final SignInAttempts signInAttempts = new SignInAttempts();

	private IMailer mailer = IMailer.fromSettings();

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
		DemoSession session = new DemoSession(request);
		String account = System.getProperty(SIGN_IN_AS_PROPERTY);
		if (!Strings.isEmpty(account))
		{
			accounts.find(account)
				.ifPresent(user -> session.signedInLocally(user.getName(), user.getRole()));
		}
		return session;
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

		// the users and the accesses are in the database; the accounts the demo always has and
		// those the server is started with are added when missing
		ApplicationContext database = newDatabase();
		accounts = database.getBean(Accounts.class);
		accesses = database.getBean(Accesses.class);
		accounts.addDefaults();
		accounts.addFromSettings();

		// the status of the home page's websocket, which keeps the home page stateless
		getSharedResources().add(LiveResource.NAME, new LiveResource());

		// the demos are the private part: only visitors who signed in see them
		DemosAuthorization authorization = new DemosAuthorization();
		getSecuritySettings().setAuthorizationStrategy(authorization);
		getSecuritySettings().setUnauthorizedComponentInstantiationListener(authorization);

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

		// allow the application stylesheet (BasePage.css) served from this origin
		getCspSettings().blocking()
			.add(CSPDirective.STYLE_SRC, CSPDirectiveSrcValue.SELF);

		mountPage("address-book", ContactsPage.class);
		mountPage("dynamic-data-table", DynamicDataTablePage.class);
		mountPage("bean-dynamic-data-table", BeanDynamicDataTablePage.class);
		mountPage("json-infinite-list", JsonListPage.class);
		mountPage("tree", TreePage.class);
		mountPage("tree-grid", TreeGridPage.class);
		mountPage("master-detail", MasterDetailPage.class);
		mountPage("progress-bar", ProgressBarPage.class);
		mountPage("theme-editor", ThemeEditorPage.class);
		mountPage("dropdown", DropdownPage.class);
		mountPage("autocomplete", AutocompletePage.class);
		mountPage("palette", PalettePage.class);
		mountPage("date-picker", DatePickerPage.class);
		mountPage("buttons", ButtonsPage.class);
		mountPage("scrollbar", ScrollbarPage.class);
		mountPage("split-panel", SplitPanelPage.class);
		mountPage("modal-dialog", ModalDialogPage.class);
		mountPage("register", RegisterPage.class);
		mountPage("sign-in", SignInPage.class);
		mountPage("validate-email", EmailValidationPage.class);
		mountPage("admin", AdminPage.class);
		mountResource(AccessTimeResource.PATH, AccessTimeResource.REFERENCE);
		mountResource(ChoicesResource.PATH, ChoicesResource.REFERENCE);
		mountResource(FontAwesomeResourceReference.PATH, FontAwesomeResourceReference.INSTANCE);
		mountResource(UsersRowsResource.PATH, UsersRowsResource.REFERENCE);
		mountResource(TreeOfLifeResource.PATH, TreeOfLifeResource.REFERENCE);
		mountResource(TeamResource.PATH, TeamResource.REFERENCE);
	}

	/**
	 * @return the accounts that may open the demos
	 */
	public Accounts getAccounts()
	{
		return accounts;
	}

	/**
	 * @return who accessed the site
	 */
	public Accesses getAccesses()
	{
		return accesses;
	}

	/**
	 * @return what sends the emails confirming a registration
	 */
	public IMailer getMailer()
	{
		return mailer;
	}

	/**
	 * @param mailer
	 *            what sends the emails confirming a registration, for tests
	 */
	public void setMailer(IMailer mailer)
	{
		this.mailer = mailer;
	}

	/**
	 * @return the failed attempts to sign in, by account
	 */
	public SignInAttempts getSignInAttempts()
	{
		return signInAttempts;
	}

	/**
	 * @return the Spring context of the database set by the system properties or the environment
	 * @see Database#fromSettings()
	 */
	protected ApplicationContext newDatabase()
	{
		return Database.context(Database.fromSettings());
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
