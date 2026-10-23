/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antiliacode.widgets.demo.mail;

/**
 * Sends {@link Mail}s: over SMTP with a {@link SmtpMailer} when the server is configured for it,
 * else to the log with the {@link LogMailer}.
 */
public interface IMailer
{
	/**
	 * Sends a mail.
	 *
	 * @param mail
	 *            the mail
	 * @throws MailException
	 *             if it could not be sent
	 */
	void send(Mail mail);

	/**
	 * @return whether the mails reach the people they go to, rather than the log
	 */
	boolean isDelivering();

	/**
	 * @return the mailer the server is configured with: SMTP if {@link SmtpMailer#HOST_PROPERTY}
	 *         or {@link SmtpMailer#HOST_ENVIRONMENT_VARIABLE} names a host, else the log
	 */
	static IMailer fromSettings()
	{
		return SmtpMailer.fromSettings().map(IMailer.class::cast).orElseGet(LogMailer::new);
	}
}
