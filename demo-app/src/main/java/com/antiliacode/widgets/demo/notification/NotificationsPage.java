/*
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
package com.antiliacode.widgets.demo.notification;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;

import com.antiliacode.widgets.button.ButtonBehavior;
import com.antiliacode.widgets.demo.DemoPage;
import com.antiliacode.widgets.demo.frameworks.FrameworkTabs;
import com.antiliacode.widgets.dropdown.Dropdown;
import com.antiliacode.widgets.notification.Notification;
import com.antiliacode.widgets.notification.NotificationPosition;
import com.antiliacode.widgets.notification.SessionNotifications;
import com.github.openjson.JSONObject;

/**
 * {@link com.antiliacode.widgets.notification.Notifications Notifications}, those every page of the
 * demo has: a form choosing the type, the title, the message, text or HTML, the position and the
 * timeout of a notification, which its button shows via Ajax, another on a new page, after a
 * redirect, through the {@link SessionNotifications session}, and closes them all; and a form
 * whose feedback messages become notifications, the error of its required field, which stays until
 * closed, or the page's success. Besides the four types of the component the demo has one of its
 * own, note, violet with a light bulb. The React and Svelte tabs show the same with their {@code Notifications}.
 */
public class NotificationsPage extends DemoPage
{
	private static final long serialVersionUID = 1L;

	/** The types the demo shows: the four of the component and the page's own, note. */
	static final List<String> TYPES = List.of("success", "error", "warning", "info", "note");

	/**
	 * The timeouts the demo offers, in milliseconds: -1 for the one of the notifications, 5 seconds
	 * and errors until closed; 0 keeps a notification until it is closed.
	 */
	static final List<Long> TIMEOUTS = List.of(-1L, 3000L, 5000L, 10000L, 0L);

	/** The icon of the page's own type, note: a light bulb (Material Icons, Apache License 2.0). */
	static final String NOTE_ICON = "M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 " +
		"5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-" +
		"3.36 3-5.74 0-3.86-3.14-7-7-7z";

	private String type = "success";

	private String title;

	private String message;

	private NotificationPosition position = NotificationPosition.TOP_RIGHT;

	private Long timeout = -1L;

	private boolean html;

	private String name;

	/**
	 * Constructor.
	 */
	public NotificationsPage()
	{
		WebMarkupContainer themed = newThemedContainer("themed");
		add(themed);

		Form<Void> showForm = new Form<>("showForm");
		themed.add(showForm);
		showForm.add(new Dropdown<>("type", new PropertyModel<>(this, "type"), TYPES,
			new KeyRenderer<>("type.")).setRequired(true));
		showForm.add(new TextField<>("title", new PropertyModel<>(this, "title")));
		showForm.add(new TextArea<>("message", new PropertyModel<>(this, "message")));
		showForm.add(new CheckBox("html", new PropertyModel<>(this, "html")));
		showForm.add(new Dropdown<>("position", new PropertyModel<>(this, "position"),
			Arrays.asList(NotificationPosition.values()), new KeyRenderer<>("position."))
			.setRequired(true));
		showForm.add(new Dropdown<>("timeout", new PropertyModel<>(this, "timeout"), TIMEOUTS,
			new KeyRenderer<>("timeout.")).setRequired(true));
		showForm.add(new AjaxButton("show")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				getNotifications().show(newNotification());
			}
		}.add(new ButtonBehavior(ButtonBehavior.Variant.PRIMARY, ButtonBehavior.Size.MEDIUM)));
		showForm.add(new Button("showAfterRedirect")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onSubmit()
			{
				// the session keeps the notification for the new page the redirect goes to
				SessionNotifications.show(newNotification());
				setResponsePage(NotificationsPage.class);
			}
		}.add(new ButtonBehavior()));
		showForm.add(new AjaxLink<Void>("closeAll")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target)
			{
				getNotifications().closeAll();
			}
		}.add(new ButtonBehavior()));

		Form<Void> saveForm = new Form<>("saveForm");
		themed.add(saveForm);
		saveForm.add(new TextField<>("name", new PropertyModel<>(this, "name")).setRequired(true)
			.setLabel(new ResourceModel("name")));
		saveForm.add(new AjaxButton("save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target)
			{
				// the feedback message becomes a notification
				success(getString("saved").replace("{0}", name.trim()));
			}
		}.add(new ButtonBehavior()));
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		title = getString("defaultTitle");
		message = getString("defaultMessage");
	}

	/**
	 * @return the notification the form says to show
	 */
	Notification newNotification()
	{
		Notification notification = Notification.of(type, message).setTitle(title)
			.setPosition(position).setTimeout(timeout < 0 ? null : Duration.ofMillis(timeout))
			.setEscapeBody(!html);
		return "note".equals(type) ? notification.setIcon(NOTE_ICON) : notification;
	}

	@Override
	protected String getFrameworkDemo()
	{
		return "notifications";
	}

	/**
	 * @return the texts and the form as the page has it
	 */
	@Override
	protected JSONObject getFrameworkConfig()
	{
		JSONObject texts = FrameworkTabs.texts(this, "showTitle", "feedbackTitle", "type",
			"message", "html", "position", "timeout", "show", "closeAll", "name", "save", "required",
			"saved");
		texts.put("title", getString("title.field"))
			.put("close", getString("Notifications.close"))
			.put("label", getString("Notifications.label"));
		JSONObject types = new JSONObject();
		TYPES.forEach(each -> types.put(each, getString("type." + each)));
		JSONObject positions = new JSONObject();
		for (NotificationPosition each : NotificationPosition.values())
		{
			positions.put(each.cssName(), getString("position." + each.cssName()));
		}
		JSONObject timeouts = new JSONObject();
		TIMEOUTS.forEach(each -> timeouts.put(String.valueOf(each), getString("timeout." + each)));
		texts.put("types", types).put("positions", positions).put("timeouts", timeouts);
		return new JSONObject()
			.put("texts", FrameworkTabs.withDropdownTexts(this, texts))
			.put("type", type)
			.put("title", title)
			.put("message", message)
			.put("position", position.cssName())
			.put("timeout", timeout)
			.put("html", html);
	}

	@Override
	public void renderHead(IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(
			new CssResourceReference(NotificationsPage.class, "NotificationsPage.css")));
	}

	/**
	 * Renders a choice by the text of the page's bundle under a prefix: {@code type.success},
	 * {@code position.top-right}, {@code timeout.5000}.
	 */
	private class KeyRenderer<T> implements IChoiceRenderer<T>
	{
		private static final long serialVersionUID = 1L;

		private final String prefix;

		KeyRenderer(String prefix)
		{
			this.prefix = prefix;
		}

		@Override
		public Object getDisplayValue(T choice)
		{
			return getString(prefix + getIdValue(choice, 0));
		}

		@Override
		public String getIdValue(T choice, int index)
		{
			return choice instanceof NotificationPosition each ? each.cssName() :
				String.valueOf(choice);
		}

		@Override
		public T getObject(String id, IModel<? extends List<? extends T>> choices)
		{
			return choices.getObject().stream().filter(choice -> getIdValue(choice, 0).equals(id))
				.findFirst().orElse(null);
		}
	}
}
