/*
 * Copyright (c) 2026 Ernesto Reinaldo Barreiro (reiern70@gmail.com).
 * All rights reserved.
 */
package com.antilia.widgets.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Locale;
import java.util.function.Consumer;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.ErrorLevelFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.TagTester;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

class NotificationsTest extends WicketTestCase
{
	@Test
	void rendersAnElementTheScriptStacksTheNotificationsIn()
	{
		TestPage page = tester.startPage(new TestPage(page1 -> {
		}));

		TagTester element = tester.getTagByWicketId("notifications");
		assertEquals("antilia-notifications", element.getAttribute("class"));
		String id = page.notifications.getMarkupId();
		assertEquals(id, element.getAttribute("id"));
		assertEquals("", element.getValue());
		tester.assertContains("antilia-notifications.*\\.js");
		tester.assertContains("antilia-notifications.*\\.css");
		tester.assertContains("Wicket.Notifications.attach\\(\"" + id + "\", \\{\"position\":" +
			"\"top-right\",\"timeout\":5000,\"errorTimeout\":0,\"max\":5,\"pauseOnHover\":true," +
			"\"closeLabel\":\"Close\",\"label\":\"Notifications\"\\}\\);");
		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.show"));
		assertFalse(tester.getLastResponseAsString().contains("style="), "no inline style");
	}

	@Test
	void takesWhereTheNotificationsGoHowLongTheyStayAndTheTextsInTheLanguageOfTheSession()
	{
		tester.getSession().setLocale(Locale.GERMAN);
		tester.startPage(new TestPage(page -> page.notifications
			.setPosition(NotificationPosition.BOTTOM_CENTER).setTimeout(Duration.ZERO)
			.setErrorTimeout(Duration.ofSeconds(20)).setMax(3).setPauseOnHover(false)));

		tester.assertContains("\\{\"position\":\"bottom-center\",\"timeout\":0," +
			"\"errorTimeout\":20000,\"max\":3," +
			"\"pauseOnHover\":false,\"closeLabel\":\"Schließen\",\"label\":\"Mitteilungen\"\\}");
	}

	@Test
	void showsTheNotificationsGivenBeforeTheComponentRenders()
	{
		TestPage page = tester.startPage(new TestPage(test -> test.notifications.show(Notification
			.success("The user was saved.").setTitle("Saved"))));

		String id = page.notifications.getMarkupId();
		String response = tester.getLastResponseAsString();
		tester.assertContains("Wicket.Notifications.show\\(\"" + id + "\", \\{\"type\":\"success\"," +
			"\"title\":\"Saved\",\"body\":\"The user was saved.\"\\}\\);");
		assertTrue(response.indexOf("Wicket.Notifications.attach") <
			response.indexOf("Wicket.Notifications.show"));
		assertTrue(page.notifications.getPending().isEmpty());

		// shown once only
		tester.startPage(page);
		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.show"));
	}

	@Test
	void showsANotificationViaAjax()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
		}));
		page.onAjax = target -> page.notifications.show(Notification.of("note", "Remember")
			.setTitle("Note").setTimeout(Duration.ofSeconds(10)).setClosable(false)
			.setPosition(NotificationPosition.TOP_LEFT).setIcon("M0 0h24v24H0z")
			.setCssClass("mine"));

		tester.clickLink("ajax");

		tester.assertContains("Wicket.Notifications.show\\(\"" + page.notifications.getMarkupId() +
			"\", \\{\"type\":\"note\",\"title\":\"Note\",\"body\":\"Remember\",\"timeout\":10000," +
			"\"closable\":false,\"position\":\"top-left\",\"icon\":\"M0 0h24v24H0z\"," +
			"\"className\":\"mine\"\\}\\);");
		tester.assertComponentOnAjaxResponse(page.get("ajax"));
		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.attach"));
		assertTrue(page.notifications.getPending().isEmpty());
	}

	@Test
	void showsANotificationOfAPageRenderedAnewWhenItRenders()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
		}));
		page.onClick = () -> page.notifications.show(Notification.warning("Careful")
			.withoutIcon());

		tester.clickLink("link");

		tester.assertContains("Wicket.Notifications.show\\(\"" + page.notifications.getMarkupId() +
			"\", \\{\"type\":\"warning\",\"body\":\"Careful\",\"icon\":false\\}\\);");
	}

	@Test
	void closesAllNotificationsViaAjax()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
		}));
		page.onAjax = target -> page.notifications.closeAll();

		tester.clickLink("ajax");

		tester.assertContains("Wicket.Notifications.closeAll\\(\"" +
			page.notifications.getMarkupId() + "\"\\);");
	}

	@Test
	void forgetsTheNotificationsToShowWhenClosingAllBeforeRendering()
	{
		TestPage page = new TestPage(test -> {
		});
		page.notifications.show(Notification.info("Never shown")).closeAll();

		assertTrue(page.notifications.getPending().isEmpty());
	}

	@Test
	void makesTheFeedbackMessagesNotificationsTitledByTheirType()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
			test.error("Wrong");
			test.warn("Careful");
			test.success("Saved");
			test.info("Note this");
			test.fatal("Broken");
		}));

		String id = page.notifications.getMarkupId();
		tester.assertContains("Wicket.Notifications.show\\(\"" + id + "\", \\{\"type\":\"error\"," +
			"\"title\":\"Error\",\"body\":\"Wrong\"\\}\\);");
		tester.assertContains("\\{\"type\":\"warning\",\"title\":\"Warning\",\"body\":\"Careful\"\\}");
		tester.assertContains("\\{\"type\":\"success\",\"title\":\"Success\",\"body\":\"Saved\"\\}");
		tester.assertContains("\\{\"type\":\"info\",\"title\":\"Information\",\"body\":\"Note this\"\\}");
		tester.assertContains("\\{\"type\":\"error\",\"title\":\"Error\",\"body\":\"Broken\"\\}");
		assertTrue(page.getFeedbackMessages().toList().stream().allMatch(FeedbackMessage::isRendered));
	}

	@Test
	void makesTheFeedbackMessagesOfAnAjaxRequestNotifications()
	{
		tester.getSession().setLocale(Locale.of("es"));
		TestPage page = tester.startPage(new TestPage(test -> {
		}));
		page.onAjax = target -> page.get("ajax").error("Mal");

		tester.clickLink("ajax");

		tester.assertContains("Wicket.Notifications.show\\(\"" + page.notifications.getMarkupId() +
			"\", \\{\"type\":\"error\",\"title\":\"Error\",\"body\":\"Mal\"\\}\\);");
		page.onAjax = target -> page.success("Bien");
		tester.clickLink("ajax");
		tester.assertContains("\\{\"type\":\"success\",\"title\":\"Éxito\",\"body\":\"Bien\"\\}");
		assertFalse(tester.getLastResponseAsString().contains("Mal"), "shown once only");
	}

	@Test
	void makesTheFeedbackMessagesOfTheSessionNotificationsAfterARedirect()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
		}));
		page.onClick = () -> {
			page.getSession().success("Signed in");
			page.setResponsePage(new TestPage(test -> {
			}));
		};

		tester.clickLink("link");

		tester.assertContains(
			"\\{\"type\":\"success\",\"title\":\"Success\",\"body\":\"Signed in\"\\}");
		assertTrue(tester.getSession().getFeedbackMessages().toList().stream()
			.allMatch(FeedbackMessage::isRendered));

		// shown once only
		tester.startPage(new TestPage(test -> {
		}));
		assertFalse(tester.getLastResponseAsString().contains("Signed in"));
	}

	@Test
	void showsTheNotificationsOfTheSessionOnTheNextPageOnce()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
		}));
		page.onClick = () -> {
			SessionNotifications.show(Notification.success("You are signed in.")
				.setTitle("GitHub"));
			page.setResponsePage(new TestPage(test -> {
			}));
		};

		tester.clickLink("link");

		TestPage next = (TestPage)tester.getLastRenderedPage();
		tester.assertContains("Wicket.Notifications.show\\(\"" + next.notifications.getMarkupId() +
			"\", \\{\"type\":\"success\",\"title\":\"GitHub\"," +
			"\"body\":\"You are signed in.\"\\}\\);");
		assertTrue(SessionNotifications.getPending().isEmpty());
		assertFalse(tester.getSession().isTemporary(), "bound, to last until the next request");

		tester.startPage(next);
		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.show"));
	}

	@Test
	void showsTheNotificationsOfTheSessionViaAjax()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
		}));
		page.onAjax = target -> SessionNotifications.show(Notification.info("Elsewhere"));

		tester.clickLink("ajax");

		tester.assertContains("Wicket.Notifications.show\\(\"" + page.notifications.getMarkupId() +
			"\", \\{\"type\":\"info\",\"body\":\"Elsewhere\"\\}\\);");
		assertTrue(SessionNotifications.getPending().isEmpty());
	}

	@Test
	void showsOnlyTheFeedbackMessagesTheFilterAccepts()
	{
		tester.startPage(new TestPage(test -> {
			test.notifications.setFeedbackFilter(
				new ErrorLevelFeedbackMessageFilter(FeedbackMessage.WARNING));
			test.info("Hidden");
			test.warn("Shown");
		}));

		tester.assertContains("\"body\":\"Shown\"");
		assertFalse(tester.getLastResponseAsString().contains("Hidden"));
	}

	@Test
	void leavesTheFeedbackMessagesAloneWhenToldTo()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
			test.notifications.setShowFeedback(false);
			test.error("Elsewhere");
		}));

		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.show"));
		assertFalse(page.notifications.isShowFeedback());
	}

	@Test
	void letsASubclassChooseTheNotificationOfAFeedbackMessage()
	{
		tester.startPage(new TestPage(test -> test.error("Stays"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			Notifications newNotifications(String id)
			{
				return new Notifications(id)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected Notification newNotification(FeedbackMessage message)
					{
						return message.isError() ? super.newNotification(message)
							.setTimeout(Duration.ofSeconds(30)) : null;
					}
				};
			}
		});

		tester.assertContains("\\{\"type\":\"error\",\"title\":\"Error\",\"body\":\"Stays\"," +
			"\"timeout\":30000\\}");
	}

	@Test
	void takesABodyOfMarkupWhenToldTo()
	{
		TestPage page = tester.startPage(new TestPage(test -> test.notifications.show(Notification
			.success("<b>Ada</b> was saved.").setEscapeBody(false))));

		tester.assertContains("\\{\"type\":\"success\",\"body\":\"<b>Ada<\\\\/b> was saved.\"," +
			"\"html\":true\\}");
		assertFalse(Notification.info("x").toJson().has("html"), "text by default");
		assertFalse(Notification.info(null).setEscapeBody(false).toJson().has("html"), "no body");

		page.onClick = () -> {
			page.notifications.setEscapeModelStrings(false);
			page.info("<i>Note</i>");
		};
		tester.clickLink("link");
		tester.assertContains("\"body\":\"<i>Note<\\\\/i>\",\"html\":true");
	}

	@Test
	void leavesTheFeedbackMessagesAFeedbackPanelShowsToIt()
	{
		TestPage page = tester.startPage(new TestPage(test -> {
			test.feedback.setFilter(new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR))
				.setVisible(true);
			test.error("In the panel");
			test.info("A notification");
		}));

		assertFalse(tester.getLastResponseAsString().contains("\"body\":\"In the panel\""));
		tester.assertContains("\"body\":\"A notification\"");
		tester.assertContains("In the panel");

		page.feedback.setFilter(null).setOutputMarkupId(true);
		page.onAjax = target -> {
			page.warn("Also in the panel");
			target.add(page.feedback);
		};
		tester.clickLink("ajax");
		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.show"));
		tester.assertComponentOnAjaxResponse(page.feedback);

		// the panel does not render, so the message becomes a notification
		page.onAjax = target -> page.warn("Not in the panel");
		tester.clickLink("ajax");
		tester.assertContains("\"body\":\"Not in the panel\"");
	}

	@Test
	void showsNothingViaAjaxWhenHidden()
	{
		TestPage page = tester.startPage(new TestPage(test -> test.notifications.setVisible(false)));
		page.onAjax = target -> {
			page.notifications.show(Notification.info("Later"));
			SessionNotifications.show(Notification.info("Later too"));
			page.error("Wrong");
		};

		tester.clickLink("ajax");

		assertFalse(tester.getLastResponseAsString().contains("Wicket.Notifications.show"));
		assertEquals(1, page.notifications.getPending().size());
		assertEquals(1, SessionNotifications.getPending().size());
	}

	@Test
	void refusesNegativeTimeouts()
	{
		Notifications notifications = new Notifications("n");
		assertThrows(IllegalArgumentException.class,
			() -> notifications.setTimeout(Duration.ofSeconds(-1)));
		assertThrows(IllegalArgumentException.class,
			() -> Notification.info("x").setTimeout(Duration.ofSeconds(-1)));
		assertThrows(IllegalArgumentException.class,
			() -> notifications.setErrorTimeout(Duration.ofSeconds(-1)));
		assertThrows(IllegalArgumentException.class, () -> notifications.setMax(0));
		assertThrows(IllegalArgumentException.class, () -> Notification.of("", "x"));
	}

	@Test
	void namesTheTypesAndPositionsAsTheScriptDoes()
	{
		assertEquals("info", NotificationType.INFO.cssName());
		assertEquals("bottom-right", NotificationPosition.BOTTOM_RIGHT.cssName());
		assertEquals(NotificationType.INFO, NotificationType.of(new FeedbackMessage(null, "x",
			FeedbackMessage.DEBUG)));
		Notification notification = Notification.error("Body").setTitle("Title");
		assertEquals("error", notification.getType());
		assertEquals("Body", notification.getBody());
		assertEquals("Title", notification.getTitle());
		assertEquals(null, notification.getTimeout());
		assertTrue(notification.isClosable());
		assertEquals(null, notification.getPosition());
		assertEquals(null, notification.setIcon("M1").withoutIcon().getIcon());
		assertEquals(null, notification.getCssClass());
		assertTrue(notification.isEscapeBody());
		assertEquals(Duration.ZERO, new Notifications("n").getErrorTimeout());
	}

	/**
	 * A page with the notifications, a link, an Ajax link and a feedback panel, hidden.
	 */
	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final Notifications notifications;

		final FeedbackPanel feedback;

		transient Consumer<AjaxRequestTarget> onAjax;

		transient Runnable onClick;

		TestPage(Consumer<TestPage> setUp)
		{
			notifications = newNotifications("notifications");
			add(notifications);
			feedback = new FeedbackPanel("feedback");
			add(feedback.setVisible(false));
			add(new AjaxLink<Void>("ajax")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(AjaxRequestTarget target)
				{
					onAjax.accept(target);
					target.add(this);
				}
			}.setOutputMarkupId(true));
			add(new Link<Void>("link")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick()
				{
					onClick.run();
				}
			});
			setUp.accept(this);
		}

		Notifications newNotifications(String id)
		{
			return new Notifications(id);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body>" +
				"<a wicket:id=\"ajax\">Ajax</a><a wicket:id=\"link\">Link</a>" +
				"<div wicket:id=\"feedback\"></div><div wicket:id=\"notifications\"></div>" +
				"</body></html>");
		}
	}
}
