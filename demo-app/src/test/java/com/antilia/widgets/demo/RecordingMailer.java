/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
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
