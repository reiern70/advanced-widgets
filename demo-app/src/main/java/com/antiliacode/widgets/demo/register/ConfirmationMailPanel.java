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
package com.antiliacode.widgets.demo.register;

import java.util.Locale;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * The body of the {@link ConfirmationMail}, a Wicket template rendered to HTML: a greeting, without
 * the name the visitor typed, the link to the {@link EmailValidationPage} and how long it works,
 * in the language of the account rather than the one of the request. The styles are inline, as
 * email programs want them.
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
	 * @param link
	 *            the full address of the link
	 * @param hours
	 *            how many hours the link works
	 */
	ConfirmationMailPanel(String id, Locale locale, String link, long hours)
	{
		super(id);
		this.locale = locale;
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
