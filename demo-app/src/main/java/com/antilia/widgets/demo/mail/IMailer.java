/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.mail;

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
