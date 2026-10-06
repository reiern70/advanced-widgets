/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.advanced.widgets.demo;

import java.time.Year;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.TransparentWebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.advanced.widgets.theme.Theme;
import com.advanced.widgets.theme.ThemeBehavior;
import com.advanced.widgets.theme.ThemeChoice;

/**
 * The layout of every page: the top bar with the logo linking to the {@link HomePage}, a floating
 * button choosing the theme and the language of the session, and the footer. The whole page is in
 * the session's {@link Theme}, its own colors included.
 */
public abstract class BasePage extends WebPage
{
	private static final long serialVersionUID = 1L;

	/** The languages the demos have texts in. */
	private static final List<Locale> LANGUAGES = List.of(Locale.ENGLISH, Locale.GERMAN);

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
		});
		settings.add(newLanguageChoice("language"));

		add(new Label("year", Year.now().getValue()));
		add(new Label("version", getApplication().getFrameworkSettings().getVersion()));
	}

	/**
	 * @return the text of the browser tab
	 */
	protected String getPageTitle()
	{
		return "Advanced Widgets";
	}

	/**
	 * @return the theme of the session, which every page is in
	 */
	protected final IModel<Theme> getThemeModel()
	{
		return LambdaModel.of(() -> DemoSession.get().getTheme(),
			theme -> DemoSession.get().setTheme(theme));
	}

	private DropDownChoice<Locale> newLanguageChoice(String id)
	{
		IModel<Locale> language = LambdaModel.of(
			() -> LANGUAGES.stream()
				.filter(locale -> locale.getLanguage().equals(getSession().getLocale().getLanguage()))
				.findFirst()
				.orElse(Locale.ENGLISH),
			locale -> getSession().setLocale(locale));
		DropDownChoice<Locale> choice = new DropDownChoice<>(id, language, LANGUAGES,
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
		choice.add(new AjaxFormComponentUpdatingBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target)
			{
				setResponsePage(getPage());
			}
		});
		return choice;
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forUrl("style.css"));
	}
}
