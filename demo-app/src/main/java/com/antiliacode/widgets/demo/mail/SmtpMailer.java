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

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;

import org.apache.wicket.util.string.Strings;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Sends the mails over SMTP, in HTML and in plain text, with STARTTLS, or with TLS from the start on port 465. The server is
 * configured by system properties or, when they are not set, environment variables:
 * <ul>
 * <li>{@value #HOST_PROPERTY} or {@value #HOST_ENVIRONMENT_VARIABLE}: the host; without it no
 * mail is sent</li>
 * <li>{@value #PORT_PROPERTY} or {@value #PORT_ENVIRONMENT_VARIABLE}: the port, 587 if not
 * set</li>
 * <li>{@value #USER_PROPERTY} or {@value #USER_ENVIRONMENT_VARIABLE} and
 * {@value #PASSWORD_PROPERTY} or {@value #PASSWORD_ENVIRONMENT_VARIABLE}: the account, if the
 * server asks for one</li>
 * <li>{@value #FROM_PROPERTY} or {@value #FROM_ENVIRONMENT_VARIABLE}: the sender, the user if not
 * set</li>
 * </ul>
 */
public class SmtpMailer implements IMailer
{
	public static final String HOST_PROPERTY = "antilia-widgets.smtp.host";

	public static final String HOST_ENVIRONMENT_VARIABLE = "SMTP_HOST";

	public static final String PORT_PROPERTY = "antilia-widgets.smtp.port";

	public static final String PORT_ENVIRONMENT_VARIABLE = "SMTP_PORT";

	public static final String USER_PROPERTY = "antilia-widgets.smtp.user";

	public static final String USER_ENVIRONMENT_VARIABLE = "SMTP_USER";

	public static final String PASSWORD_PROPERTY = "antilia-widgets.smtp.password";

	public static final String PASSWORD_ENVIRONMENT_VARIABLE = "SMTP_PASSWORD";

	public static final String FROM_PROPERTY = "antilia-widgets.mail.from";

	public static final String FROM_ENVIRONMENT_VARIABLE = "MAIL_FROM";

	/** The port of SMTP with TLS from the start rather than STARTTLS. */
	private static final int TLS_PORT = 465;

	/** How long connecting and talking to the server may take, in milliseconds. */
	private static final String TIMEOUT = "15000";

	private final Session session;

	private final String from;

	/**
	 * @param host
	 *            the host
	 * @param port
	 *            the port
	 * @param user
	 *            the account, {@code null} if the server asks for none
	 * @param password
	 *            the password of the account
	 * @param from
	 *            the sender
	 */
	public SmtpMailer(String host, int port, String user, String password, String from)
	{
		Properties properties = new Properties();
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", String.valueOf(port));
		properties.put("mail.smtp.connectiontimeout", TIMEOUT);
		properties.put("mail.smtp.timeout", TIMEOUT);
		properties.put("mail.smtp.writetimeout", TIMEOUT);
		if (port == TLS_PORT)
		{
			properties.put("mail.smtp.ssl.enable", "true");
		}
		else
		{
			properties.put("mail.smtp.starttls.enable", "true");
			properties.put("mail.smtp.starttls.required", "true");
		}
		Authenticator authenticator = null;
		if (!Strings.isEmpty(user))
		{
			properties.put("mail.smtp.auth", "true");
			authenticator = new Authenticator()
			{
				@Override
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(user, password);
				}
			};
		}
		this.session = Session.getInstance(properties, authenticator);
		this.from = from;
	}

	/**
	 * @return the mailer of the SMTP server the settings name, empty if they name none
	 */
	public static Optional<SmtpMailer> fromSettings()
	{
		String host = setting(HOST_PROPERTY, HOST_ENVIRONMENT_VARIABLE);
		if (Strings.isEmpty(host))
		{
			return Optional.empty();
		}
		String port = setting(PORT_PROPERTY, PORT_ENVIRONMENT_VARIABLE);
		String user = setting(USER_PROPERTY, USER_ENVIRONMENT_VARIABLE);
		String password = setting(PASSWORD_PROPERTY, PASSWORD_ENVIRONMENT_VARIABLE);
		String from = setting(FROM_PROPERTY, FROM_ENVIRONMENT_VARIABLE);
		return Optional.of(new SmtpMailer(host, Strings.isEmpty(port) ? 587 : Integer.parseInt(port),
			user, password, Strings.isEmpty(from) ? user : from));
	}

	@Override
	public void send(Mail mail)
	{
		try
		{
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(from));
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(mail.to()));
			message.setSubject(mail.subject(), StandardCharsets.UTF_8.name());
			// the plain text first: a program shows the last part it can
			MimeBodyPart text = new MimeBodyPart();
			text.setText(mail.text(), StandardCharsets.UTF_8.name());
			MimeBodyPart html = new MimeBodyPart();
			html.setText(mail.html(), StandardCharsets.UTF_8.name(), "html");
			message.setContent(new MimeMultipart("alternative", text, html));
			Transport.send(message);
		}
		catch (MessagingException e)
		{
			throw new MailException("Cannot send a mail to " + mail.to(), e);
		}
	}

	@Override
	public boolean isDelivering()
	{
		return true;
	}

	private static String setting(String property, String environmentVariable)
	{
		String value = System.getProperty(property);
		return Strings.isEmpty(value) ? System.getenv(environmentVariable) : value;
	}
}
