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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.feedback.FeedbackCollector;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.visit.IVisit;

import com.github.openjson.JSONObject;

import com.antilia.widgets.theme.LookPart;
import com.antilia.widgets.theme.Looks;

/**
 * Notifications, as toasts: short messages stacked in a corner of the browser window, or in the
 * middle of its top or bottom edge, the same script of the core ({@code Wicket.Notifications}
 * from {@link #JS}) showing them as the {@code Notifications} of React and Svelte do. Each one has
 * a header, with the icon of its type, a title and a button closing it, and a body; it stays until
 * the user closes it, or goes after a timeout, which a bar along its bottom counts down and which
 * waits while the pointer is over it or the focus in it.
 *
 * <pre>
 * Notifications notifications = new Notifications("notifications");
 * add(notifications);
 * ...
 * notifications.show(Notification.success("The user was saved.").setTitle("Saved"));
 * </pre>
 *
 * {@link #show(Notification)} shows a notification via Ajax in an Ajax request, otherwise when the
 * page renders next; {@link SessionNotifications#show(Notification)} shows one wherever the session
 * renders notifications next, also after a redirect. The feedback messages of the page and of the
 * session, as a {@link FeedbackPanel} shows them, become notifications too, unless
 * {@link #setShowFeedback(boolean) told otherwise}: errors, warnings, successes and the others as
 * information, titled by their type; a {@link #setFeedbackFilter(IFeedbackMessageFilter) filter}
 * may choose them, and those a {@link FeedbackPanel} of the page shows, of a form in a dialog for
 * example, stay there. Errors stay until the user closes them, unless
 * {@link #setErrorTimeout(Duration) told otherwise}. Put the component in the page's theme, as one
 * of the body for example: the notifications are in the colors of the
 * {@link com.antilia.widgets.theme.Theme theme} it is in.
 * Its tag is a {@code div}, which the script stacks the notifications in.
 *
 * @since 11.0.0
 */
public class Notifications extends WebMarkupContainer implements IFeedback
{
	private static final long serialVersionUID = 1L;

	/** The script of the notifications, {@code Wicket.Notifications}. */
	public static final ResourceReference JS = new JavaScriptResourceReference(Notifications.class,
		"antilia-notifications.js");

	/** The style sheet of the notifications. */
	public static final ResourceReference CSS = new CssResourceReference(Notifications.class,
		"antilia-notifications.css");

	/** The CSS class of the element of the notifications. */
	public static final String CSS_CLASS = "antilia-notifications";

	/** How long notifications stay by default. */
	public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

	private NotificationPosition position = NotificationPosition.TOP_RIGHT;

	private Duration timeout = DEFAULT_TIMEOUT;

	private Duration errorTimeout = Duration.ZERO;

	private int max = 5;

	private boolean pauseOnHover = true;

	private boolean showFeedback = true;

	private IFeedbackMessageFilter feedbackFilter;

	/** The notifications shown when the component renders next. */
	private final List<Notification> pending = new ArrayList<>();

	/**
	 * @param id
	 *            the component id
	 */
	public Notifications(String id)
	{
		super(id);
		setOutputMarkupId(true);
	}

	/**
	 * Shows a notification: via Ajax in an Ajax request, otherwise when the component renders
	 * next.
	 *
	 * @param notification
	 *            the notification
	 * @return {@code this}
	 */
	public Notifications show(Notification notification)
	{
		Args.notNull(notification, "notification");
		AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class).orElse(null);
		if (target != null && isRendered())
		{
			target.appendJavaScript(getShowScript(notification));
		}
		else
		{
			pending.add(notification);
		}
		return this;
	}

	/**
	 * Closes every notification shown, via Ajax in an Ajax request; the notifications that were
	 * to be shown when the component renders next are not.
	 *
	 * @return {@code this}
	 */
	public Notifications closeAll()
	{
		pending.clear();
		RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(target -> target.appendJavaScript(
			"Wicket.Notifications.closeAll(" + JSONObject.quote(getMarkupId()) + ");"));
		return this;
	}

	/**
	 * @return whether the component was rendered in the page, so that the browser has its
	 *         notifications
	 */
	private boolean isRendered()
	{
		return findPage() != null && isVisibleInHierarchy();
	}

	/**
	 * @return the notifications to be shown when the component renders next
	 */
	public List<Notification> getPending()
	{
		return List.copyOf(pending);
	}

	/**
	 * @return where notifications go, unless one says otherwise
	 */
	public NotificationPosition getPosition()
	{
		return position;
	}

	/**
	 * @param position
	 *            where notifications go, unless one says otherwise; {@link NotificationPosition#TOP_RIGHT}
	 *            by default
	 * @return {@code this}
	 */
	public Notifications setPosition(NotificationPosition position)
	{
		this.position = Args.notNull(position, "position");
		return this;
	}

	/**
	 * @return how long notifications stay, unless one says otherwise; {@link Duration#ZERO} keeps
	 *         them until the user closes them
	 */
	public Duration getTimeout()
	{
		return timeout;
	}

	/**
	 * @param timeout
	 *            how long notifications stay, unless one says otherwise, {@link #DEFAULT_TIMEOUT}
	 *            by default; {@link Duration#ZERO} keeps them until the user closes them
	 * @return {@code this}
	 */
	public Notifications setTimeout(Duration timeout)
	{
		Args.notNull(timeout, "timeout");
		Args.isFalse(timeout.isNegative(), "The timeout must not be negative");
		this.timeout = timeout;
		return this;
	}

	/**
	 * @return how long errors stay, unless one says otherwise; {@link Duration#ZERO} keeps them
	 *         until the user closes them
	 */
	public Duration getErrorTimeout()
	{
		return errorTimeout;
	}

	/**
	 * @param errorTimeout
	 *            how long errors stay, unless one says otherwise; {@link Duration#ZERO}, the
	 *            default, keeps them until the user closes them
	 * @return {@code this}
	 */
	public Notifications setErrorTimeout(Duration errorTimeout)
	{
		Args.notNull(errorTimeout, "errorTimeout");
		Args.isFalse(errorTimeout.isNegative(), "The error timeout must not be negative");
		this.errorTimeout = errorTimeout;
		return this;
	}

	/**
	 * @return how many notifications a stack shows at most
	 */
	public int getMax()
	{
		return max;
	}

	/**
	 * @param max
	 *            how many notifications a stack shows at most, 5 by default; the oldest go first
	 * @return {@code this}
	 */
	public Notifications setMax(int max)
	{
		this.max = Args.withinRange(1, Integer.MAX_VALUE, max, "max");
		return this;
	}

	/**
	 * @return whether the countdown waits while the pointer is over a notification
	 */
	public boolean isPauseOnHover()
	{
		return pauseOnHover;
	}

	/**
	 * @param pauseOnHover
	 *            whether the countdown waits while the pointer is over a notification, as it does
	 *            by default; it always waits while the focus is in one
	 * @return {@code this}
	 */
	public Notifications setPauseOnHover(boolean pauseOnHover)
	{
		this.pauseOnHover = pauseOnHover;
		return this;
	}

	/**
	 * @return whether the feedback messages of the page and of the session become notifications
	 */
	public boolean isShowFeedback()
	{
		return showFeedback;
	}

	/**
	 * @param showFeedback
	 *            whether the feedback messages of the page and of the session become
	 *            notifications, as they do by default
	 * @return {@code this}
	 */
	public Notifications setShowFeedback(boolean showFeedback)
	{
		this.showFeedback = showFeedback;
		return this;
	}

	/**
	 * @return the filter choosing the feedback messages that become notifications, if any
	 */
	public IFeedbackMessageFilter getFeedbackFilter()
	{
		return feedbackFilter;
	}

	/**
	 * @param feedbackFilter
	 *            the filter choosing the feedback messages that become notifications;
	 *            {@code null}, the default, for all of them
	 * @return {@code this}
	 */
	public Notifications setFeedbackFilter(IFeedbackMessageFilter feedbackFilter)
	{
		this.feedbackFilter = feedbackFilter;
		return this;
	}

	/**
	 * Makes the notification showing a feedback message: of its {@link NotificationType type},
	 * titled by it, its text the message's, markup when the component does not
	 * {@link #setEscapeModelStrings(boolean) escape its model strings}. Override it to give some
	 * another timeout, for example.
	 *
	 * @param message
	 *            the feedback message
	 * @return the notification, {@code null} for none
	 */
	protected Notification newNotification(FeedbackMessage message)
	{
		NotificationType type = NotificationType.of(message);
		return Notification.of(type, String.valueOf(message.getMessage()))
			.setTitle(getString("Notifications." + type.cssName()))
			.setEscapeBody(getEscapeModelStrings());
	}

	/**
	 * @return the feedback messages that become notifications and were not shown yet, marked
	 *         shown
	 */
	private List<Notification> collectFeedback()
	{
		List<Notification> notifications = new ArrayList<>();
		if (!showFeedback)
		{
			return notifications;
		}
		List<FeedbackMessage> messages = new FeedbackCollector(getPage(), true).collect(message ->
			!message.isRendered() && (feedbackFilter == null || feedbackFilter.accept(message)) &&
			!isShownElsewhere(message));
		for (FeedbackMessage message : messages)
		{
			message.markRendered();
			Notification notification = newNotification(message);
			if (notification != null)
			{
				notifications.add(notification);
			}
		}
		return notifications;
	}

	/**
	 * Tells whether a feedback message is shown by a {@link FeedbackPanel} of the page, such as
	 * one of a form in a dialog, so that it does not become a notification too: a panel that is
	 * visible, accepts the message and renders, in an Ajax request because it or one of its
	 * parents was added to the request.
	 *
	 * @param message
	 *            the feedback message
	 * @return whether another component of the page shows the message
	 */
	protected boolean isShownElsewhere(FeedbackMessage message)
	{
		AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class).orElse(null);
		Boolean shown = getPage().visitChildren(FeedbackPanel.class,
			(FeedbackPanel panel, IVisit<Boolean> visit) -> {
				if (panel.isVisibleInHierarchy() &&
					(panel.getFilter() == null || panel.getFilter().accept(message)) &&
					(target == null || isUpdated(panel, target)))
				{
					visit.stop(true);
				}
			});
		return Boolean.TRUE.equals(shown);
	}

	/**
	 * @return whether a component renders in an Ajax request: it or one of its parents was added
	 */
	private static boolean isUpdated(Component component, AjaxRequestTarget target)
	{
		Collection<? extends Component> updated = target.getComponents();
		for (Component each = component; each != null; each = each.getParent())
		{
			if (updated.contains(each))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Shows the notifications of the session and the feedback messages of an Ajax request as
	 * notifications, which the page broadcasts once the request was handled.
	 */
	@Override
	public void onEvent(IEvent<?> event)
	{
		super.onEvent(event);
		if (event.getPayload() instanceof AjaxRequestTarget target && isRendered())
		{
			List<Notification> notifications = new ArrayList<>(SessionNotifications.take());
			notifications.addAll(collectFeedback());
			for (Notification notification : notifications)
			{
				target.appendJavaScript(getShowScript(notification));
			}
		}
	}

	@Override
	protected void onComponentTag(ComponentTag tag)
	{
		checkComponentTag(tag, "div");
		super.onComponentTag(tag);
		Looks.addClasses(tag, Looks.of(this), LookPart.NOTIFICATIONS);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		renderHeadItems(response);
		response.render(OnDomReadyHeaderItem.forScript("Wicket.Notifications.attach(" +
			JSONObject.quote(getMarkupId()) + ", " + getOptions() + ");"));
		List<Notification> notifications = new ArrayList<>(pending);
		pending.clear();
		notifications.addAll(SessionNotifications.take());
		notifications.addAll(collectFeedback());
		for (Notification notification : notifications)
		{
			response.render(OnDomReadyHeaderItem.forScript(getShowScript(notification)));
		}
	}

	/**
	 * @return the script showing a notification
	 */
	private String getShowScript(Notification notification)
	{
		return "Wicket.Notifications.show(" + JSONObject.quote(getMarkupId()) + ", " +
			notification.toJson() + ");";
	}

	/**
	 * @return the options of {@code Wicket.Notifications.attach}
	 */
	private JSONObject getOptions()
	{
		return new JSONObject()
			.put("position", position.cssName())
			.put("timeout", timeout.toMillis())
			.put("errorTimeout", errorTimeout.toMillis())
			.put("max", max)
			.put("pauseOnHover", pauseOnHover)
			.put("closeLabel", getString("Notifications.close"))
			.put("label", getString("Notifications.label"));
	}

	/**
	 * Renders what the notifications need in the browser: {@link #JS} and {@link #CSS}.
	 *
	 * @param response
	 *            the header response
	 */
	public static void renderHeadItems(IHeaderResponse response)
	{
		CoreLibrariesContributor.contribute(Application.get(), response);
		response.render(JavaScriptHeaderItem.forReference(JS));
		response.render(CssHeaderItem.forReference(CSS));
	}
}
