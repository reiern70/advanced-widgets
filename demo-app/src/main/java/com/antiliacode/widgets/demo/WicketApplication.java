/*
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

import java.io.File;
import java.time.Duration;

import org.apache.wicket.DefaultPageManagerProvider;
import org.apache.wicket.csp.CSPDirective;
import org.apache.wicket.csp.CSPDirectiveSrcValue;
import org.apache.wicket.csp.FixedCSPValue;
import org.apache.wicket.Session;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.pageStore.IPageStore;
import org.apache.wicket.protocol.http.ResourceIsolationRequestCycleListener.CsrfAction;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.ws.WebSocketAwareResourceIsolationRequestCycleListener;
import org.apache.wicket.protocol.ws.WebSocketSettings;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.settings.StoreSettings;
import org.apache.wicket.util.string.Strings;
import org.springframework.context.ApplicationContext;

import com.antiliacode.widgets.ckeditor.CKEditorLibrary;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.DemosAuthorization;
import com.antiliacode.widgets.demo.account.SignInAttempts;
import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.account.SignedInUsers;
import com.antiliacode.widgets.demo.admin.AccessTimeResource;
import com.antiliacode.widgets.demo.admin.AccessesApiResource;
import com.antiliacode.widgets.demo.admin.UsersApiResource;
import com.antiliacode.widgets.demo.admin.AdminPage;
import com.antiliacode.widgets.demo.autocomplete.AutocompletePage;
import com.antiliacode.widgets.demo.button.ButtonsPage;
import com.antiliacode.widgets.demo.ckeditor.CKEditorAssets;
import com.antiliacode.widgets.demo.ckeditor.CKEditorPage;
import com.antiliacode.widgets.demo.contacts.ContactsPage;
import com.antiliacode.widgets.demo.datatable.BeanDynamicDataTablePage;
import com.antiliacode.widgets.demo.datatable.DynamicDataTablePage;
import com.antiliacode.widgets.demo.datatable.MasterDetailPage;
import com.antiliacode.widgets.demo.datatable.FontAwesomeResourceReference;
import com.antiliacode.widgets.demo.theme.BootstrapResourceReference;
import com.antiliacode.widgets.demo.datatable.UsersRowsResource;
import com.antiliacode.widgets.demo.data.Accesses;
import com.antiliacode.widgets.demo.data.Database;
import com.antiliacode.widgets.demo.datepicker.DatePickerPage;
import com.antiliacode.widgets.demo.desktop.DesktopPage;
import com.antiliacode.widgets.demo.download.DownloadsPage;
import com.antiliacode.widgets.demo.download.DownloadsResource;
import com.antiliacode.widgets.demo.dropdown.DropdownPage;
import com.antiliacode.widgets.demo.error.AccessDeniedPage;
import com.antiliacode.widgets.demo.error.InternalErrorPage;
import com.antiliacode.widgets.demo.error.NotFoundPage;
import com.antiliacode.widgets.demo.error.PageExpiredPage;
import com.antiliacode.widgets.demo.list.JsonListPage;
import com.antiliacode.widgets.demo.looks.LooksPage;
import com.antiliacode.widgets.demo.mail.IMailer;
import com.antiliacode.widgets.demo.breadcrumb.BreadcrumbsPage;
import com.antiliacode.widgets.demo.menu.MenusPage;
import com.antiliacode.widgets.demo.modal.ModalDialogPage;
import com.antiliacode.widgets.demo.notification.NotificationsPage;
import com.antiliacode.widgets.demo.modal.TeamResource;
import com.antiliacode.widgets.demo.palette.PalettePage;
import com.antiliacode.widgets.demo.split.SplitPanelPage;
import com.antiliacode.widgets.demo.codeeditor.CodeEditorPage;
import com.antiliacode.widgets.demo.resizable.ResizablePage;
import com.antiliacode.widgets.demo.richtext.RichTextEditorPage;
import com.antiliacode.widgets.demo.richtext.RichTextImageResource;
import com.antiliacode.widgets.demo.popover.PopoversPage;
import com.antiliacode.widgets.demo.tabs.TabbedPanelPage;
import com.antiliacode.widgets.demo.frameworks.ChoicesResource;
import com.antiliacode.widgets.demo.progress.ProgressBarPage;
import com.antiliacode.widgets.demo.register.EmailValidationPage;
import com.antiliacode.widgets.demo.register.RegisterPage;
import com.antiliacode.widgets.demo.register.RegistrationLimits;
import com.antiliacode.widgets.demo.scrollbar.ScrollbarPage;
import com.antiliacode.widgets.demo.theme.ThemeEditorPage;
import com.antiliacode.widgets.demo.tree.TreePage;
import com.antiliacode.widgets.demo.tree.TreeGridPage;
import com.antiliacode.widgets.demo.tree.TreeOfLifeResource;
import com.antiliacode.widgets.demo.upload.UploadPage;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.throttle.Throttle;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Application object for your web application.
 * If you want to run this application without deploying, run the Start class.
 * 
 * @see com.antiliacode.widgets.demo.Start#main(String[])
 */
public class WicketApplication extends WebApplication
{
	/** The system property holding the folder of the page store. */
	public static final String PAGE_STORE_PROPERTY = "antilia-widgets.page-store";

	/** The environment variable holding the folder of the page store. */
	public static final String PAGE_STORE_ENVIRONMENT_VARIABLE = "ANTILIA_WIDGETS_PAGE_STORE";

	/**
	 * How long a session lasts without a request, as {@code web.xml} sets it: then the visitor is
	 * signed out.
	 */
	public static final Duration SESSION_TIMEOUT = Duration.ofMinutes(5);

	/**
	 * The system property naming the account every new session is signed in with, so that nobody
	 * signs in on a development machine; {@code jetty:run} sets it to {@code admin}. It counts in
	 * development only, see {@link DevelopmentSwitches}.
	 */
	public static final String SIGN_IN_AS_PROPERTY = "antilia-widgets.sign-in-as";

	private Accounts accounts;

	private Accesses accesses;

	private final SignInAttempts signInAttempts = new SignInAttempts();

	private final SignedInUsers signedInUsers = new SignedInUsers();

	private final RegistrationLimits registrationLimits = new RegistrationLimits();

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
		String account = DevelopmentSwitches.get(SIGN_IN_AS_PROPERTY);
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
		DevelopmentSwitches.warnOfIgnored(this);

		// the users and the accesses are in the database; the accounts the demo always has and the
		// other accounts the server is started with get the passwords it is started with, and those
		// no longer listed are locked
		ApplicationContext database = newDatabase();
		accounts = database.getBean(Accounts.class);
		accesses = database.getBean(Accesses.class);
		accounts.addDefaults();
		accounts.addFromSettings();

		// a client keeping no cookies gets its pages instead of redirects for good, and the error
		// pages keep their status
		setPageRendererProvider(DirectPageRenderer::new);

		// what went wrong, in the look of the site; Jetty's 404 goes to not-found (see web.xml)
		getApplicationSettings().setPageExpiredErrorPage(PageExpiredPage.class)
			.setInternalErrorPage(InternalErrorPage.class)
			.setAccessDeniedPage(AccessDeniedPage.class);

		// the fields that failed their validation say so, to assistive technologies and the eye
		getComponentInitializationListeners().add(InvalidFieldMarker::install);

		// a session does not outlive its account: removed, locked or given another role, it is
		// signed out
		getRequestCycleListeners().add(new IRequestCycleListener()
		{
			@Override
			public void onBeginRequest(RequestCycle cycle)
			{
				if (Session.exists() && DemoSession.get().isSignedIn())
				{
					DemoSession.get().checkAccount();
				}
			}
		});

		// a visitor who does not sign in, such as a script checking the site, tells who it is with
		// ?visitor=, which the accesses show: it names the session there is, a request alone
		// makes none
		getRequestCycleListeners().add(new IRequestCycleListener()
		{
			@Override
			public void onBeginRequest(RequestCycle cycle)
			{
				String visitor = cycle.getRequest()
					.getQueryParameters()
					.getParameterValue(DemoSession.VISITOR_PARAMETER)
					.toOptionalString();
				if (visitor != null && !DemoSession.get().isTemporary())
				{
					DemoSession.get().identify(visitor);
				}
			}

			@Override
			public void onEndRequest(RequestCycle cycle)
			{
				// a request keeps the user counted among those signed in
				if (Session.exists() && DemoSession.get().isSignedIn() &&
					DemoSession.get().getId() != null)
				{
					signedInUsers.seen(DemoSession.get().getId());
				}
			}
		});
		// the components of a page answer the site's own pages only: a page of another site
		// neither signs out nor sends a form; a request telling nothing of where it comes from is
		// no browser's, and goes through
		getRequestCycleListeners().add(new WebSocketAwareResourceIsolationRequestCycleListener()
		{
			@Override
			protected void allowHandler(HttpServletRequest request, IRequestablePage page)
			{
				// not logged: every client but a browser sends such requests
			}
		}.setUnknownOutcomeAction(CsrfAction.ALLOW));
		// at most SignedInUsers.MAXIMUM users are signed in at once: the ended sessions are not
		getSessionStore().registerUnboundListener(signedInUsers::signedOut);

		// the status of the home page's websocket, which keeps the home page stateless
		getSharedResources().add(LiveResource.NAME, new LiveResource());
		// the websockets connect from the site's own pages only
		WebSocketSettings.Holder.get(this).setConnectionFilter(new WebSocketOriginFilter());

		// the demos are the private part: only visitors who signed in see them, and get the
		// resources of the demos, such as the JSON of /api/...
		DemosAuthorization authorization = new DemosAuthorization();
		getSecuritySettings().setAuthorizationStrategy(authorization);
		getSecuritySettings().setUnauthorizedComponentInstantiationListener(authorization);
		getSecuritySettings().setUnauthorizedResourceRequestListener(authorization);

		// the pages of the sessions go to this folder on disk, the servlet container's temporary
		// folder by default; they last as long as the server runs, as the sessions do
		File pageStore = pageStoreFolder();
		if (pageStore != null)
		{
			getStoreSettings().setFileStoreFolder(pageStore);
		}
		setPageManagerProvider(new DefaultPageManagerProvider(this)
		{
			@Override
			protected IPageStore newPersistentStore()
			{
				StoreSettings settings = application.getStoreSettings();
				return new TransientDiskPageStore(application.getName(),
					settings.getFileStoreFolder(), settings.getMaxSizePerSession());
			}
		});

		// the upload demo: the progress of uploads the browser cannot follow is asked of the server
		getApplicationSettings().setUploadProgressUpdatesEnabled(true);
		// the session that asks for it waits before its Ajax requests and sends files slowly, to
		// see the demos as on a slow network: the button of the top bar of a demo opens the
		// settings, which the upload demo shows too
		Throttle.install(this);

		// leave out <wicket:child> and the like also in development, they would break the layout
		getMarkupSettings().setStripWicketTags(true);

		// allow the application stylesheet (BasePage.css) served from this origin, and the images
		// Bootstrap's style sheet writes as data: URLs, such as the tick of its check boxes, for
		// the pages in its look
		getCspSettings().blocking()
			.add(CSPDirective.STYLE_SRC, CSPDirectiveSrcValue.SELF)
			.add(CSPDirective.IMG_SRC, new FixedCSPValue("data:"))
			// and no page in a frame of another site, where it could be clicked unseen
			.add(CSPDirective.FRAME_ANCESTORS, CSPDirectiveSrcValue.SELF);

		// CKEditor as the demo brings it, the browser build of ckeditor5 under the GPL, with the
		// license key of the application: the components hold no code of CKEditor
		CKEditorLibrary.set(this, CKEditorAssets.library());

		mountPage("address-book", ContactsPage.class);
		mountPage("dynamic-data-table", DynamicDataTablePage.class);
		mountPage("bean-dynamic-data-table", BeanDynamicDataTablePage.class);
		mountPage("json-infinite-list", JsonListPage.class);
		mountPage("tree", TreePage.class);
		mountPage("tree-grid", TreeGridPage.class);
		mountPage("master-detail", MasterDetailPage.class);
		mountPage("progress-bar", ProgressBarPage.class);
		mountPage("theme-editor", ThemeEditorPage.class);
		mountPage("looks", LooksPage.class);
		mountPage("dropdown", DropdownPage.class);
		mountPage("autocomplete", AutocompletePage.class);
		mountPage("palette", PalettePage.class);
		mountPage("date-picker", DatePickerPage.class);
		mountPage("buttons", ButtonsPage.class);
		mountPage("downloads", DownloadsPage.class);
		mountPage("upload", UploadPage.class);
		mountPage("code-editor", CodeEditorPage.class);
		mountPage("rich-text-editor", RichTextEditorPage.class);
		mountPage("ckeditor", CKEditorPage.class);
		mountPage("scrollbar", ScrollbarPage.class);
		mountPage("split-panel", SplitPanelPage.class);
		mountPage("resizable", ResizablePage.class);
		mountPage("tabs", TabbedPanelPage.class);
		mountPage("menus", MenusPage.class);
		mountPage("breadcrumbs", BreadcrumbsPage.class);
		mountPage("popovers", PopoversPage.class);
		mountPage("desktop", DesktopPage.class);
		mountPage("modal-dialog", ModalDialogPage.class);
		mountPage("notifications", NotificationsPage.class);
		mountPage("register", RegisterPage.class);
		mountPage("sign-in", SignInPage.class);
		mountPage("validate-email", EmailValidationPage.class);
		mountPage("admin", AdminPage.class);
		mountPage("not-found", NotFoundPage.class);
		mountResource(AccessTimeResource.PATH, AccessTimeResource.REFERENCE);
		// the API of the administration: the users and the accesses, recent and old, for
		// administrators only
		mountResource(UsersApiResource.PATH, UsersApiResource.REFERENCE);
		mountResource(AccessesApiResource.PATH, AccessesApiResource.REFERENCE);
		mountResource(AccessesApiResource.OLD_PATH, AccessesApiResource.OLD_REFERENCE);
		mountResource(ChoicesResource.PATH, ChoicesResource.REFERENCE);
		mountResource(RichTextImageResource.PATH, RichTextImageResource.REFERENCE);
		mountResource(FontAwesomeResourceReference.PATH, FontAwesomeResourceReference.INSTANCE);
		mountResource(BootstrapResourceReference.PATH, BootstrapResourceReference.INSTANCE);
		mountResource(UsersRowsResource.PATH, UsersRowsResource.REFERENCE);
		mountResource(TreeOfLifeResource.PATH, TreeOfLifeResource.REFERENCE);
		mountResource(TeamResource.PATH, TeamResource.REFERENCE);
		mountResource(DownloadsResource.PATH, DownloadsResource.REFERENCE);
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
	 * @return the users signed in now
	 */
	public SignedInUsers getSignedInUsers()
	{
		return signedInUsers;
	}

	/**
	 * @return the failed attempts to sign in, by account
	 */
	public SignInAttempts getSignInAttempts()
	{
		return signInAttempts;
	}

	/**
	 * @return the limits of registering and of the emails sent
	 */
	public RegistrationLimits getRegistrationLimits()
	{
		return registrationLimits;
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
