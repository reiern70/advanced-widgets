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
package com.antilia.widgets.notification;

import java.io.Serializable;
import java.time.Duration;

import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * A notification {@link Notifications} shows: its type, a title in its header and a text in its
 * body, how long it stays and where it goes.
 *
 * <pre>
 * notifications.show(Notification.success("The user was saved.").setTitle("Saved"));
 * notifications.show(Notification.error("The server is down.").setTimeout(Duration.ZERO));
 * </pre>
 *
 * A type of the application's own, given to {@link #of(String, String)}, takes its color from the
 * class {@code antilia-notification-<type>} of the application's style sheet, which sets
 * {@code --antilia-notification-color}, and has an icon only when given one with
 * {@link #setIcon(String)}. The body is text, unless {@link #setEscapeBody(boolean) told it is
 * markup}. An error stays until the user closes it, unless given a timeout of its own or the
 * {@link Notifications} {@link Notifications#setErrorTimeout(Duration) one for errors}.
 *
 * @since 11.0.0
 */
public class Notification implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String type;

	private final String body;

	private String title;

	private Duration timeout;

	private boolean closable = true;

	private NotificationPosition position;

	private String icon;

	private boolean noIcon;

	private String cssClass;

	private boolean escapeBody = true;

	/**
	 * @param type
	 *            the type: one of {@link NotificationType}'s names in lower case, or the
	 *            application's own
	 * @param body
	 *            the text of the body, none when {@code null}
	 */
	protected Notification(String type, String body)
	{
		this.type = Args.notEmpty(type, "type");
		this.body = body;
	}

	/**
	 * @param type
	 *            the type
	 * @param body
	 *            the text of the body, none when {@code null}
	 * @return a notification of the type
	 */
	public static Notification of(NotificationType type, String body)
	{
		return new Notification(Args.notNull(type, "type").cssName(), body);
	}

	/**
	 * @param type
	 *            the type, the application's own, such as {@code note}, or one of
	 *            {@link NotificationType}'s names in lower case
	 * @param body
	 *            the text of the body, none when {@code null}
	 * @return a notification of the type
	 */
	public static Notification of(String type, String body)
	{
		return new Notification(type, body);
	}

	/**
	 * @return a notification telling something went well
	 */
	public static Notification success(String body)
	{
		return of(NotificationType.SUCCESS, body);
	}

	/**
	 * @return a notification telling something went wrong
	 */
	public static Notification error(String body)
	{
		return of(NotificationType.ERROR, body);
	}

	/**
	 * @return a notification asking to look at something
	 */
	public static Notification warning(String body)
	{
		return of(NotificationType.WARNING, body);
	}

	/**
	 * @return a notification telling something
	 */
	public static Notification info(String body)
	{
		return of(NotificationType.INFO, body);
	}

	/**
	 * @return the type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @return the text of the body, if any
	 */
	public String getBody()
	{
		return body;
	}

	/**
	 * @return the title, if any
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * @param title
	 *            the title, in the header
	 * @return {@code this}
	 */
	public Notification setTitle(String title)
	{
		this.title = title;
		return this;
	}

	/**
	 * @return how long the notification stays; {@code null} for the timeout of the
	 *         {@link Notifications}, their error timeout for an error
	 */
	public Duration getTimeout()
	{
		return timeout;
	}

	/**
	 * @param timeout
	 *            how long the notification stays, {@link Duration#ZERO} to keep it until the user
	 *            closes it; {@code null} for the timeout of the {@link Notifications}, their
	 *            error timeout for an error
	 * @return {@code this}
	 */
	public Notification setTimeout(Duration timeout)
	{
		if (timeout != null)
		{
			Args.isFalse(timeout.isNegative(), "The timeout must not be negative");
		}
		this.timeout = timeout;
		return this;
	}

	/**
	 * @return whether the header has a button closing the notification
	 */
	public boolean isClosable()
	{
		return closable;
	}

	/**
	 * @param closable
	 *            whether the header has a button closing the notification, which Escape does too;
	 *            {@code true} by default
	 * @return {@code this}
	 */
	public Notification setClosable(boolean closable)
	{
		this.closable = closable;
		return this;
	}

	/**
	 * @return where the notification goes; {@code null} for the position of the
	 *         {@link Notifications}
	 */
	public NotificationPosition getPosition()
	{
		return position;
	}

	/**
	 * @param position
	 *            where the notification goes; {@code null} for the position of the
	 *            {@link Notifications}
	 * @return {@code this}
	 */
	public Notification setPosition(NotificationPosition position)
	{
		this.position = position;
		return this;
	}

	/**
	 * @return the path of the SVG of the icon, if one was given
	 */
	public String getIcon()
	{
		return icon;
	}

	/**
	 * @param icon
	 *            the icon, the path ({@code d}) of an SVG of 24 by 24; {@code null} for the icon
	 *            of the type, none for a type of the application's own
	 * @return {@code this}
	 */
	public Notification setIcon(String icon)
	{
		this.icon = icon;
		this.noIcon = false;
		return this;
	}

	/**
	 * Shows the notification without an icon, whatever its type.
	 *
	 * @return {@code this}
	 */
	public Notification withoutIcon()
	{
		this.icon = null;
		this.noIcon = true;
		return this;
	}

	/**
	 * @return the CSS classes of the notification, next to {@code antilia-notification}, if any
	 */
	public String getCssClass()
	{
		return cssClass;
	}

	/**
	 * @param cssClass
	 *            the CSS classes of the notification, next to {@code antilia-notification}
	 * @return {@code this}
	 */
	public Notification setCssClass(String cssClass)
	{
		this.cssClass = cssClass;
		return this;
	}

	/**
	 * @return whether the body is text, its markup escaped, rather than markup
	 */
	public boolean isEscapeBody()
	{
		return escapeBody;
	}

	/**
	 * @param escapeBody
	 *            whether the body is text, as it is by default, its markup escaped; {@code false}
	 *            for a body of markup, such as {@code The user <strong>Ada</strong> was saved.},
	 *            which goes into the notification as it is: only for markup the application trusts
	 * @return {@code this}
	 */
	public Notification setEscapeBody(boolean escapeBody)
	{
		this.escapeBody = escapeBody;
		return this;
	}

	/**
	 * @return the notification as the script takes it
	 */
	public JSONObject toJson()
	{
		JSONObject json = new JSONObject().put("type", type);
		if (title != null)
		{
			json.put("title", title);
		}
		if (body != null)
		{
			json.put("body", body);
			if (!escapeBody)
			{
				json.put("html", true);
			}
		}
		if (timeout != null)
		{
			json.put("timeout", timeout.toMillis());
		}
		if (!closable)
		{
			json.put("closable", false);
		}
		if (position != null)
		{
			json.put("position", position.cssName());
		}
		if (noIcon)
		{
			json.put("icon", false);
		}
		else if (icon != null)
		{
			json.put("icon", icon);
		}
		if (cssClass != null)
		{
			json.put("className", cssClass);
		}
		return json;
	}
}
