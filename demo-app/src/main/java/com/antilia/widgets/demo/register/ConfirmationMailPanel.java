/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.register;

import java.util.Locale;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * The body of the {@link ConfirmationMail}, a Wicket template rendered to HTML: a greeting, the
 * link to the {@link EmailValidationPage} and how long it works, in the language of the account
 * rather than the one of the request. The styles are inline, as email programs want them.
 */
class ConfirmationMailPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final Locale locale;

	/**
	 * @param id
	 *            the id of the panel
	 * @param locale
	 *            the language of the email
	 * @param name
	 *            how the email greets the visitor
	 * @param link
	 *            the full address of the link
	 * @param hours
	 *            how many hours the link works
	 */
	ConfirmationMailPanel(String id, Locale locale, String name, String link, long hours)
	{
		super(id);
		this.locale = locale;
		add(new Label("greeting", new StringResourceModel("greeting", this)
			.setParameters(Model.of(name))));
		add(new ExternalLink("link", link, link));
		add(new ExternalLink("button", link));
		add(new Label("validity", new StringResourceModel("validity", this)
			.setParameters(Model.of(hours))));
	}

	@Override
	public Locale getLocale()
	{
		return locale;
	}
}
