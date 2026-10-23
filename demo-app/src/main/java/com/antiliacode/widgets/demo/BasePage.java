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

import java.time.Year;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.StatelessForm;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.StatelessLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.admin.AccessTimeResource;
import com.antiliacode.widgets.demo.admin.AdminPage;
import com.antiliacode.widgets.demo.register.RegisterPage;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.dropdown.DropdownBehavior;
import com.antiliacode.widgets.demo.theme.BootstrapResourceReference;
import com.antiliacode.widgets.notification.Notifications;
import com.antiliacode.widgets.theme.ILook;
import com.antiliacode.widgets.theme.Look;
import com.antiliacode.widgets.theme.LookAspect;
import com.antiliacode.widgets.theme.LookChoice;
import com.antiliacode.widgets.theme.Looks;
import com.antiliacode.widgets.theme.Theme;
import com.antiliacode.widgets.theme.ThemeBehavior;
import com.antiliacode.widgets.theme.ThemeChoice;
import com.antiliacode.widgets.throttle.ThrottleButton;

/**
 * The layout of every page: the top bar with the logo linking to the {@link HomePage}, the
 * drop-downs choosing the theme, the {@link Look}, the language and the {@link AjaxEngine} of the
 * session, each on its own, and the links to sign in and to register, or the account signed in with and the link to
 * sign out, and for an administrator the link to the administration; then the footer, and the
 * {@link Notifications} showing the feedback messages of the page, and the dialog telling what runs
 * where, which the link in the footer opens in the browser. Nothing of it keeps a page, so the
 * public pages are stateless. The whole page is in the session's {@link Theme}, its own colors
 * included, and in its look; in the Bootstrap look or a Bootstrap theme the page has the style
 * sheet of Bootstrap, never its script.
 */
public abstract class BasePage extends WebPage
{
	private static final long serialVersionUID = 1L;

	/**
	 * The style sheet of the application. As a resource of the package its URL carries a hash of
	 * its contents in deployment, so browsers fetch it anew after a deployment changes it, instead
	 * of the copy they cached.
	 */
	private static final CssResourceReference STYLE = new CssResourceReference(BasePage.class,
		"BasePage.css");

	/** The languages the demos have texts in. */
	/**
	 * The looks to choose from: those of ui-components, and Bootstrap's made of Antilia's colors and
	 * sizes.
	 */
	private static final List<ILook> LOOKS = List.of(Look.ANTILIA, Look.BOOTSTRAP,
		Look.BOOTSTRAP.with(LookAspect.COLOR, Look.ANTILIA).with(LookAspect.SIZE, Look.ANTILIA));

	private static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.GERMAN,
		Locale.of("es"));

	/** The markup id of the dialog telling what runs where, which a link opens from any page. */
	public static final String WHAT_RUNS_WHERE = "what-runs-where";

	private final Notifications notifications;

	protected BasePage()
	{
		this(null);
	}

	protected BasePage(PageParameters parameters)
	{
		super(parameters);

		add(new Label("pageTitle", this::getPageTitle));

		TransparentWebMarkupContainer body = new TransparentWebMarkupContainer("body");
		body.add(new ThemeBehavior(getThemeModel(), Looks.sessionLook()));
		add(body);

		add(new BookmarkablePageLink<>("home", HomePage.class));

		// stateless, like the Ajax of the drop-downs, so that the public pages are
		StatelessForm<Void> settings = new StatelessForm<>("settings");
		add(settings);
		settings.add(newThemeChoice("theme"));
		settings.add(newLookChoice("look"));
		settings.add(newLanguageChoice("language"));
		// the script of Wicket's Ajax is for the signed in only, not for the public pages
		WebMarkupContainer ajaxEngine = new WebMarkupContainer("ajaxEngineSetting")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(DemoSession.get().isSignedIn());
			}
		};
		settings.add(ajaxEngine.add(newAjaxEngineChoice("ajaxEngine")));
		// a demo can be seen as on a slow network: the server slows down for the session
		add(this instanceof DemoPage ? new ThrottleButton("throttle")
			: new WebMarkupContainer("throttle").setVisible(false));

		add(new BookmarkablePageLink<>("register", RegisterPage.class)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!DemoSession.get().isSignedIn());
			}
		});
		add(new BookmarkablePageLink<>("signIn", SignInPage.class)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(!DemoSession.get().isSignedIn());
			}
		});
		add(newSignOut("signOut"));
		add(new BookmarkablePageLink<>("admin", AdminPage.class)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(DemoSession.get().isAdmin());
			}
		});

		notifications = new Notifications("notifications");
		add(notifications);

		add(new BrowserModal("whatRunsWhere", new ResourceModel("whatRunsWhere"), "1400px")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected Component newBody(String id)
			{
				return new SupportPanel(id);
			}
		}.setMarkupId(WHAT_RUNS_WHERE));
		add(new Label("year", Year.now().getValue()));
		add(new Label("version", getApplication().getFrameworkSettings().getVersion()));
		add(newBuildInfo("buildInfo"));
	}

	/**
	 * @return the notifications of the page, which show its feedback messages too, those a
	 *         {@link org.apache.wicket.markup.html.panel.FeedbackPanel FeedbackPanel} of a form in
	 *         a dialog shows apart
	 */
	public Notifications getNotifications()
	{
		return notifications;
	}

	/**
	 * @return the text of the browser tab
	 */
	protected String getPageTitle()
	{
		return "Antilia's Advanced Widgets";
	}

	/**
	 * @return the theme of the session, which every page is in
	 */
	protected final IModel<Theme> getThemeModel()
	{
		return LambdaModel.of(() -> DemoSession.get().getTheme(),
			theme -> DemoSession.get().setTheme(theme));
	}

	private LookChoice newLookChoice(String id)
	{
		// the look of the session, which every page is in once chosen on one
		LookChoice choice = new LookChoice(id, Looks.sessionLook(), LOOKS)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onLookChanged(AjaxRequestTarget target)
			{
				// anew, as the Bootstrap look brings a style sheet the page may lack
				reload();
			}
		};
		choice.add(new DropdownBehavior());
		return choice;
	}

	private ThemeChoice newThemeChoice(String id)
	{
		ThemeChoice choice = new ThemeChoice(id, getThemeModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onThemeChanged(AjaxRequestTarget target)
			{
				reload();
			}
		};
		choice.add(new DropdownBehavior());
		return choice;
	}

	private Dropdown<AjaxEngine> newAjaxEngineChoice(String id)
	{
		Dropdown<AjaxEngine> choice = new Dropdown<>(id,
			LambdaModel.of(() -> DemoSession.get().getAjaxEngine(),
				engine -> DemoSession.get().setAjaxEngine(engine)),
			List.of(AjaxEngine.values()), new EnumChoiceRenderer<>(this));
		choice.setRequired(true);
		choice.add(reloadOnChange());
		return choice;
	}

	/**
	 * @return the behavior rendering the page anew after a choice, in the new theme, language or
	 *         with the other script; stateless, the page is made anew from its URL to run it
	 */
	private AjaxFormComponentUpdatingBehavior reloadOnChange()
	{
		return new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				reload();
			}

			@Override
			public boolean getStatelessHint(Component component)
			{
				return true;
			}
		};
	}

	/**
	 * Renders the page anew: a stateless one from its URL, as a stateful one may be gone.
	 */
	private void reload()
	{
		Page page = getPage();
		if (page.isPageStateless() && page.isBookmarkable())
		{
			setResponsePage(page.getPageClass(), page.getPageParameters());
		}
		else
		{
			setResponsePage(page);
		}
	}

	private Dropdown<Locale> newLanguageChoice(String id)
	{
		IModel<Locale> language = LambdaModel.of(
			() -> LANGUAGES.stream()
				.filter(locale -> locale.getLanguage().equals(getSession().getLocale().getLanguage()))
				.findFirst()
				.orElse(Locale.ENGLISH),
			locale -> {
				getSession().setLocale(locale);
				// kept beyond the stateless page
				getSession().bind();
			});
		Dropdown<Locale> choice = new Dropdown<>(id, language, LANGUAGES,
			new ChoiceRenderer<>()
			{
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(Locale locale)
				{
					String name = locale.getDisplayLanguage(locale);
					return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
				}

				@Override
				public String getIdValue(Locale locale, int index)
				{
					return locale.getLanguage();
				}
			});
		choice.setRequired(true);
		choice.add(reloadOnChange());
		return choice;
	}

	/**
	 * @return the name of the account the visitor signed in with and the link signing out, back to
	 *         the {@link HomePage}; only shown to visitors who signed in
	 */
	private Component newSignOut(String id)
	{
		StatelessLink<Void> signOut = new StatelessLink<>(id)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick()
			{
				DemoSession.get().signOut();
				setResponsePage(HomePage.class);
			}

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(DemoSession.get().isSignedIn());
			}
		};
		signOut.add(new Label("account", () -> DemoSession.get().getAccount()));
		return signOut;
	}

	/**
	 * @return the commit the application was built from, and in demo mode its branch too, so
	 *         that what a server runs shows at a glance
	 */
	private Component newBuildInfo(String id)
	{
		BuildInfo build = BuildInfo.get();
		String key = (BuildInfo.isDemoMode() ? "buildInfo" : "buildInfo.commit") +
			(build.dirty() ? ".dirty" : "");
		return new Label(id, new StringResourceModel(key, this).setParameters(build.branch(),
			build.commit()))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(build.isKnown());
			}
		};
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		if (Looks.current().getLooks().contains(Look.BOOTSTRAP) ||
			DemoSession.get().getTheme().isBootstrap())
		{
			response.render(BootstrapResourceReference.styleSheet());
		}
		response.render(CssHeaderItem.forReference(STYLE));
		AccessTimeResource.renderHead(response);
	}
}
