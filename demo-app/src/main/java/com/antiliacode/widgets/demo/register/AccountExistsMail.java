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

import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.antiliacode.widgets.demo.account.SignInPage;
import com.antiliacode.widgets.demo.data.Language;
import com.antiliacode.widgets.demo.data.User;
import com.antiliacode.widgets.demo.mail.Mail;
import com.antiliacode.widgets.demo.mail.MailTemplate;

/**
 * The email telling somebody who registers with the email of an account that the account exists,
 * with a link to the {@link SignInPage}: the {@link RegisterPage} answers the same whether the
 * email has an account or not, so that it does not tell, and only the owner of the email learns
 * it. In the language of the account, else the one chosen in the form; Wicket renders it from the
 * template of the {@link AccountExistsMailPanel}. It holds nothing the visitor typed, and is not
 * sent beyond the {@link RegistrationLimits}, as the confirmation email.
 */
public final class AccountExistsMail
{
	private AccountExistsMail()
	{
	}

	/**
	 * Sends the email, unless the {@link RegistrationLimits} keep it back.
	 *
	 * @param user
	 *            the account of the email
	 * @param language
	 *            the language chosen in the form, for an account without one
	 * @return whether the email was sent
	 */
	public static boolean send(User user, Language language)
	{
		return ConfirmationMail.send(user.getName(), () -> of(user, language));
	}

	/**
	 * @return the email telling the owner of the account that it exists
	 */
	static Mail of(User user, Language language)
	{
		Language chosen = user.getLanguage() != null ? user.getLanguage()
			: language != null ? language : Language.ENGLISH;
		Locale locale = chosen.getLocale();
		String link = ConfirmationMail.link(SignInPage.class, new PageParameters());
		MailTemplate.Rendered<AccountExistsMailPanel> mail = MailTemplate
			.render(id -> new AccountExistsMailPanel(id, locale, link));
		String html = mail.html();
		String subject = mail.component().getString("subject");
		return new Mail(user.getName(), subject, ConfirmationMail.toText(html), html);
	}
}
