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
