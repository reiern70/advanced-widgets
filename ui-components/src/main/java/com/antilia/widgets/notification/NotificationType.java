/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.notification;

import org.apache.wicket.feedback.FeedbackMessage;

/**
 * The types of {@link Notification} that have a color and an icon of their own. A notification
 * may have a type of the application's own too, see {@link Notification#of(String, String)}.
 *
 * @since 11.0.0
 */
public enum NotificationType
{
	/** Something went well, in green. */
	SUCCESS,

	/** Something went wrong, in red; an alert, which screen readers read out at once. */
	ERROR,

	/** Something to look at, in amber; an alert too. */
	WARNING,

	/** Something to know, in blue; the default. */
	INFO;

	/**
	 * @return the name the script takes, such as {@code success}
	 */
	public String cssName()
	{
		return name().toLowerCase();
	}

	/**
	 * @param message
	 *            a feedback message
	 * @return the type of the notification showing it: {@link #ERROR} for errors and fatal
	 *         errors, {@link #WARNING}, {@link #SUCCESS}, and {@link #INFO} for the others
	 */
	public static NotificationType of(FeedbackMessage message)
	{
		if (message.isError())
		{
			return ERROR;
		}
		else if (message.isWarning())
		{
			return WARNING;
		}
		else if (message.isSuccess())
		{
			return SUCCESS;
		}
		return INFO;
	}
}
