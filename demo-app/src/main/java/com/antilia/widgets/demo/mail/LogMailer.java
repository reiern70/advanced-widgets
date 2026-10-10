/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.demo.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes the mails to the log instead of sending them: on a development machine, where the link
 * of a confirmation email is then taken from the log.
 */
public class LogMailer implements IMailer
{
	private static final Logger LOG = LoggerFactory.getLogger(LogMailer.class);

	@Override
	public void send(Mail mail)
	{
		LOG.info("Mail to {} (not sent, no SMTP server is configured)\nSubject: {}\n\n{}",
			mail.to(), mail.subject(), mail.text());
	}

	@Override
	public boolean isDelivering()
	{
		return false;
	}
}
