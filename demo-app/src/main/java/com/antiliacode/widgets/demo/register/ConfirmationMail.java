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
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.wicket.Application;
import org.apache.wicket.Page;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiliacode.widgets.demo.WicketApplication;
import com.antiliacode.widgets.demo.account.Accounts;
import com.antiliacode.widgets.demo.account.SignInAttempts;
import com.antiliacode.widgets.demo.data.Language;
import com.antiliacode.widgets.demo.data.User;
import com.antiliacode.widgets.demo.mail.Mail;
import com.antiliacode.widgets.demo.mail.MailException;
import com.antiliacode.widgets.demo.mail.MailTemplate;

/**
 * The email asking a visitor who registered to confirm the address, with the link to the
 * {@link EmailValidationPage}, in the language the visitor chose. Wicket renders it from the
 * template of the {@link ConfirmationMailPanel}, see {@link MailTemplate}. The link points at the
 * site named by {@value #SITE_URL_PROPERTY} or {@value #SITE_URL_ENVIRONMENT_VARIABLE}, behind a
 * proxy as in production, or else at the one the request came to. It holds nothing the visitor
 * typed, not even the name, so that nobody can make the site send a text of theirs; and it is not
 * sent beyond the {@link RegistrationLimits}.
 */
public final class ConfirmationMail
{
	/** The system property holding the address of the site, as {@code https://example.com}. */
	public static final String SITE_URL_PROPERTY = "antilia-widgets.site-url";

	/** The environment variable holding the address of the site, like the property. */
	public static final String SITE_URL_ENVIRONMENT_VARIABLE = "SITE_URL";

	private static final Logger LOG = LoggerFactory.getLogger(ConfirmationMail.class);

	private static final Pattern TAG = Pattern.compile("<[^>]+>");

	private static final Pattern BLANK_LINES = Pattern.compile("\\s*\\n\\s*(\\n\\s*)+");

	private ConfirmationMail()
	{
	}

	/**
	 * Sends the email, unless the {@link RegistrationLimits} keep it back; a failure is logged,
	 * signing in sends another one later, see {@link Accounts#isValidationResent(User)}.
	 *
	 * @param user
	 *            the account whose email is confirmed
	 * @param token
	 *            the token of the link
	 * @return whether the email was sent
	 */
	public static boolean send(User user, String token)
	{
		return send(user.getName(), () -> of(user, token));
	}

	/**
	 * Sends the email again with a new link, unless the {@link RegistrationLimits} keep it back;
	 * the link before stops working only when the new one is sent.
	 *
	 * @param user
	 *            the account whose email is confirmed
	 * @return whether the email was sent
	 */
	public static boolean resend(User user)
	{
		return send(user.getName(), () -> of(user, Accounts.get().renewValidation(user)));
	}

	/**
	 * Sends an email of the registration, unless the {@link RegistrationLimits} keep it back.
	 *
	 * @param recipient
	 *            whom it goes to
	 * @param mail
	 *            makes the email
	 * @return whether the email was sent
	 */
	static boolean send(String recipient, Supplier<Mail> mail)
	{
		if (!RegistrationLimits.get().mail(SignInAttempts.address(), recipient))
		{
			LOG.warn("Too many emails: one is not sent");
			return false;
		}
		try
		{
			((WicketApplication)Application.get()).getMailer().send(mail.get());
			return true;
		}
		catch (MailException e)
		{
			LOG.error("Cannot send an email to {}", recipient, e);
			return false;
		}
	}

	/**
	 * @return the email confirming the account's address with the token
	 */
	static Mail of(User user, String token)
	{
		Locale locale = (user.getLanguage() == null ? Language.ENGLISH : user.getLanguage())
			.getLocale();
		String link = link(token);
		long hours = Accounts.VALIDATION_TIME.toHours();
		MailTemplate.Rendered<ConfirmationMailPanel> mail = MailTemplate
			.render(id -> new ConfirmationMailPanel(id, locale, link, hours));
		String html = mail.html();
		String subject = mail.component().getString("subject");
		return new Mail(user.getName(), subject, toText(html), html);
	}

	/**
	 * @return the text of the HTML, without its tags and blank lines, and its entities decoded
	 */
	static String toText(String html)
	{
		String text = TAG.matcher(html).replaceAll("\n");
		text = Strings.unescapeMarkup(text).toString();
		return BLANK_LINES.matcher(text).replaceAll("\n\n").strip() + "\n";
	}

	/**
	 * @return the full address of the {@link EmailValidationPage} with the token
	 */
	static String link(String token)
	{
		return link(EmailValidationPage.class,
			new PageParameters().add(EmailValidationPage.TOKEN, token));
	}

	/**
	 * @return the full address of a page of the site, see {@link #SITE_URL_PROPERTY}
	 */
	static String link(Class<? extends Page> page, PageParameters parameters)
	{
		RequestCycle cycle = RequestCycle.get();
		Url url = cycle.mapUrlFor(page, parameters);
		String site = System.getProperty(SITE_URL_PROPERTY);
		if (Strings.isEmpty(site))
		{
			site = System.getenv(SITE_URL_ENVIRONMENT_VARIABLE);
		}
		if (Strings.isEmpty(site))
		{
			return cycle.getUrlRenderer().renderFullUrl(url);
		}
		return Strings.stripEnding(site, "/") + "/" + url.toString(Url.StringMode.LOCAL);
	}
}
