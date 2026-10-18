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
package com.antilia.widgets.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.antilia.widgets.demo.mail.IMailer;
import com.antilia.widgets.demo.mail.Mail;

/**
 * Keeps the mails instead of sending them, for the tests to read; delivers as far as the
 * application can tell.
 */
public class RecordingMailer implements IMailer
{
	private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

	private final List<Mail> mails = new ArrayList<>();

	@Override
	public void send(Mail mail)
	{
		mails.add(mail);
	}

	@Override
	public boolean isDelivering()
	{
		return true;
	}

	/**
	 * @return the mails sent, the first first
	 */
	public List<Mail> getMails()
	{
		return mails;
	}

	/**
	 * @return the last mail sent
	 */
	public Mail last()
	{
		return mails.get(mails.size() - 1);
	}

	/**
	 * @return the token of the link of the last mail sent
	 */
	public String lastToken()
	{
		Matcher matcher = TOKEN.matcher(last().text());
		if (!matcher.find())
		{
			throw new AssertionError("No link in " + last().text());
		}
		return matcher.group(1);
	}

	/**
	 * Installs a recording mailer in the application.
	 *
	 * @param application
	 *            the application
	 * @return the mailer
	 */
	public static RecordingMailer install(WicketApplication application)
	{
		RecordingMailer mailer = new RecordingMailer();
		application.setMailer(mailer);
		return mailer;
	}
}
