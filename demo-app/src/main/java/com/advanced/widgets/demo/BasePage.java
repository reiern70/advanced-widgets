/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.time.Year;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.advanced.widgets.demo.register.RegisterPage;
import com.advanced.widgets.dropdown.Dropdown;
import com.advanced.widgets.dropdown.DropdownBehavior;
import com.advanced.widgets.theme.Theme;
import com.advanced.widgets.theme.ThemeBehavior;
import com.advanced.widgets.theme.ThemeChoice;

/**
 * The layout of every page: the top bar with the logo linking to the {@link HomePage}, the
 * drop-downs choosing the theme, the language and the {@link AjaxEngine} of the session, each on
 * its own, and the link to register; then the footer. The whole page is in the session's
 * {@link Theme}, its own colors included.
 */
public abstract class BasePage extends WebPage
{
	private static final long serialVersionUID = 1L;

	/** The languages the demos have texts in. */
	private static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.GERMAN,
		Locale.of("es"));

	protected BasePage()
	{
		this(null);
	}

	protected BasePage(PageParameters parameters)
	{
		super(parameters);

		add(new Label("pageTitle", this::getPageTitle));

		TransparentWebMarkupContainer body = new TransparentWebMarkupContainer("body");
		body.add(new ThemeBehavior(getThemeModel()));
		add(body);

		add(new BookmarkablePageLink<>("home", HomePage.class));

		Form<Void> settings = new Form<>("settings");
		add(settings);
		settings.add(new ThemeChoice("theme", getThemeModel())
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onThemeChanged(AjaxRequestTarget target)
			{
				setResponsePage(getPage());
			}
		}.add(new DropdownBehavior()));
		settings.add(newLanguageChoice("language"));
		settings.add(newAjaxEngineChoice("ajaxEngine"));

		add(new BookmarkablePageLink<>("register", RegisterPage.class));

		add(new Label("year", Year.now().getValue()));
		add(new Label("version", getApplication().getFrameworkSettings().getVersion()));
		add(newBuildInfo("buildInfo"));
	}

	/**
	 * @return the text of the browser tab
	 */
	protected String getPageTitle()
	{
		return "UI Advanced Widgets";
	}

	/**
	 * @return the theme of the session, which every page is in
	 */
	protected final IModel<Theme> getThemeModel()
	{
		return LambdaModel.of(() -> DemoSession.get().getTheme(),
			theme -> DemoSession.get().setTheme(theme));
	}

	private Dropdown<AjaxEngine> newAjaxEngineChoice(String id)
	{
		Dropdown<AjaxEngine> choice = new Dropdown<>(id,
			LambdaModel.of(() -> DemoSession.get().getAjaxEngine(),
				engine -> DemoSession.get().setAjaxEngine(engine)),
			List.of(AjaxEngine.values()), new EnumChoiceRenderer<>(this));
		choice.setRequired(true);
		// the page is rendered anew, so that the browser loads the other script
		choice.add(reloadOnChange());
		return choice;
	}

	private AjaxFormComponentUpdatingBehavior reloadOnChange()
	{
		return new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				setResponsePage(getPage());
			}
		};
	}

	private Dropdown<Locale> newLanguageChoice(String id)
	{
		IModel<Locale> language = LambdaModel.of(
			() -> LANGUAGES.stream()
				.filter(locale -> locale.getLanguage().equals(getSession().getLocale().getLanguage()))
				.findFirst()
				.orElse(Locale.ENGLISH),
			locale -> getSession().setLocale(locale));
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
	 * @return the branch and commit the demo was built from, shown only in demo mode
	 */
	private Component newBuildInfo(String id)
	{
		BuildInfo build = BuildInfo.get();
		String key = build.dirty() ? "buildInfo.dirty" : "buildInfo";
		return new Label(id, new StringResourceModel(key, this).setParameters(build.branch(),
			build.commit()))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onConfigure()
			{
				super.onConfigure();
				setVisible(BuildInfo.isDemoMode() && build.isKnown());
			}
		};
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forUrl("style.css"));
	}
}
