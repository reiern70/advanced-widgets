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

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * The body of the {@link AccountExistsMail}, a Wicket template rendered to HTML: that somebody
 * tried to register with the email of an account, and a link to sign in, in the language of the
 * account rather than the one of the request. The styles are inline, as email programs want them.
 */
class AccountExistsMailPanel extends Panel
{
	private static final long serialVersionUID = 1L;

	private final Locale locale;

	/**
	 * @param id
	 *            the id of the panel
	 * @param locale
	 *            the language of the email
	 * @param link
	 *            the full address of the sign-in page
	 */
	AccountExistsMailPanel(String id, Locale locale, String link)
	{
		super(id);
		this.locale = locale;
		add(new ExternalLink("link", link, link));
		add(new ExternalLink("button", link));
	}

	@Override
	public Locale getLocale()
	{
		return locale;
	}
}
