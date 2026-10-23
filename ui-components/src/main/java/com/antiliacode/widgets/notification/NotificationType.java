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
package com.antiliacode.widgets.notification;

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
