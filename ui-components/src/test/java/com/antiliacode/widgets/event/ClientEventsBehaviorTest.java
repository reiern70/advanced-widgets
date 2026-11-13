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
package com.antiliacode.widgets.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.github.openjson.JSONObject;

class ClientEventsBehaviorTest extends WicketTestCase
{
	@Test
	void sendsTheEventsToTheBrowserInTheirOrder()
	{
		List<String> heard = new ArrayList<>();
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(AjaxEvent.of("focusin", (values, target) -> heard.add("focusin")))
			.add(new ClientEvent("keydown", "function(event){console.log(event.key);}"))
			.add(AjaxEvent.of("antilia:custom", (values, target) -> heard.add("antilia:custom"))
				.setDelay(250)
				.setEventHandlerClientSideCode("function(event,component,options,send){send({a:1});}"));
		TestPage page = new TestPage(events);
		tester.startPage(page);

		tester.assertContains("antilia-client-events.js");
		String box = page.box.getMarkupId();
		// those going to the server with the attributes of their requests, Wicket's Ajax's:
		// posted, the value of a field going with them, a long text for example
		String ajax = "\"ajax\":\\{\"u\":\"[^\"]+\",\"m\":\"POST\",\"c\":\"" + box + "\"\\}";
		tester.assertContains("Wicket.ClientEvents.attach\\(\"" + box +
			"\",\\[\\{\"eventName\":\"focusin\",\"index\":0,\"delay\":-1," +
			"\"eventClientHandlerName\":\"Wicket.ClientEvents.send\"," + ajax + "\\}," +
			// code as code, the delay of the events having one, none for one in the browser
			"\\{\"eventName\":\"keydown\",\"index\":1," +
			"\"eventHandlerClientSideCode\":function\\(event\\)\\{console.log\\(event.key\\);" +
			"\\}\\}," +
			"\\{\"eventName\":\"antilia:custom\",\"index\":2,\"delay\":250," +
			"\"eventClientHandlerName\":\"Wicket.ClientEvents.send\"," +
			"\"eventHandlerClientSideCode\":function\\(event,component,options,send\\)\\{" +
			"send\\(\\{a:1\\}\\);\\}," + ajax + "\\}\\]," +
			"null,\\{\\}\\);");
		// the browser knows of no kinds, nor of events of Wicket's Ajax to fire
		tester.assertContainsNot("\"kind\"");
		tester.assertContainsNot("antilia-client-events:");
		assertEquals(3, events.getEvents().size());
	}

	@Test
	void quotesTheNameOfTheEvent()
	{
		tester.startPage(new TestPage(new ClientEventsBehavior().add(AjaxEvent.of(
			"x\"</script><script>alert(1)</script>", (values, target) -> {
			}))));

		// the name cannot close the script: < and > go in as unicode escapes
		tester.assertContains("\"eventName\":\"x\\\\\"\\\\u003c\\\\/script\\\\u003e\\\\u003cscript\\\\u003e" +
			"alert\\(1\\)\\\\u003c\\\\/script\\\\u003e\"");
		tester.assertContainsNot("x\"</script><script>alert");
	}

	@Test
	void rendersNothingWithoutEvents()
	{
		TestPage page = new TestPage(new ClientEventsBehavior());
		tester.startPage(page);

		tester.assertContainsNot("Wicket.ClientEvents");
		tester.assertContainsNot("antilia-client-events");
		assertTrue(page.box.getBehaviors(AbstractDefaultAjaxBehavior.class).isEmpty());
	}

	@Test
	void addsAnAjaxBehaviorForEachEventGoingToTheServer()
	{
		IAjaxEvent focus = AjaxEvent.of("focusin", (values, target) -> {
		});
		IAjaxEvent removed = AjaxEvent.of("focusout", (values, target) -> {
		});
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(new ClientEvent("keydown", "function(){}")).add(focus).add(removed);
		TestPage page = new TestPage(events);
		assertEquals(2, page.box.getBehaviors(AbstractDefaultAjaxBehavior.class).size());

		// taken away before its URL went to the browser: its behavior goes with it
		assertTrue(events.remove(removed));
		assertEquals(List.of(events.getAjaxBehavior(focus)),
			page.box.getBehaviors(AbstractDefaultAjaxBehavior.class));
		assertThrows(IllegalArgumentException.class, () -> events.getAjaxBehavior(removed));

		// for those staying in the browser none: the component stays stateless
		TestPage browser = new TestPage(new ClientEventsBehavior()
			.add(new ClientEvent("keydown", "function(){}")));
		tester.startPage(browser);
		assertTrue(browser.box.getBehaviors(AbstractDefaultAjaxBehavior.class).isEmpty());
		assertTrue(browser.box.isStateless());
	}

	@Test
	void givesTheEventsASubclassAddsTheirBehaviorsBeforeTheComponentRenders()
	{
		List<String> heard = new ArrayList<>();
		IAjaxEvent part = AjaxEvent.of("antilia:part", (values, target) -> heard.add("part"));
		ClientEventsBehavior events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public List<IClientEvent> getEvents()
			{
				List<IClientEvent> all = new ArrayList<>(getAddedEvents());
				all.add(part);
				return all;
			}
		};
		TestPage page = new TestPage(events);
		tester.startPage(page);

		assertEquals(List.of(events.getAjaxBehavior(part)),
			page.box.getBehaviors(AbstractDefaultAjaxBehavior.class));
		ClientEventsTester.fire(tester, page.box, "antilia:part");
		assertEquals(List.of("part"), heard);
	}

	@Test
	void hearsOfAnEventWithTheValuesOfTheBrowser()
	{
		List<String> heard = new ArrayList<>();
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(AjaxEvent.of("focusin", (values, target) -> heard.add("focusin " + values)))
			.add(new AjaxEvent("antilia:resize")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onEvent(JSONObject values, AjaxRequestTarget target)
				{
					heard.add(getEventName() + " " + values.getJSONObject("size").getInt("width"));
				}
			});
		TestPage page = new TestPage(events);
		tester.startPage(page);

		ClientEventsTester.fire(tester, page.box, "antilia:resize", new JSONObject().put("size",
			new JSONObject().put("width", 640)));
		ClientEventsTester.fire(tester, page.box, "focusin");
		assertEquals(List.of("antilia:resize 640", "focusin {}"), heard);
	}

	@Test
	void submitsTheFormWithAnEvent()
	{
		List<String> heard = new ArrayList<>();
		Model<String> name = Model.of("old");
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(new AjaxSubmitEvent("antilia:save")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onEvent(JSONObject values, Form<?> form, AjaxRequestTarget target)
				{
					heard.add(getEventName() + " " + name.getObject() + " " +
						values.optString("by") + " " + form.getId());
				}

				@Override
				public void onError(JSONObject values, Form<?> form, AjaxRequestTarget target)
				{
					heard.add("error " + getEventName());
				}
			});
		TestPage page = new TestPage(events, name);
		tester.startPage(page);
		// the attributes of its requests tell the form, and no event of the browser
		tester.assertContains("\"ajax\":\\{\"u\":\"[^\"]+\",\"m\":\"POST\",\"c\":\"" +
			page.box.getMarkupId() + "\",\"f\":\"" + page.form.getMarkupId() + "\"");
		tester.assertContainsNot("\"e\":");
		// nor a script of Wicket's behavior submitting the form, which would send it at once
		tester.assertContainsNot("Wicket\\.Ajax\\.ajax\\(");
		tester.assertContainsNot("antilia-client-events:");

		tester.getRequest().getPostParameters().setParameterValue(page.name.getInputName(),
			"new");
		ClientEventsTester.fire(tester, page.box, "antilia:save", new JSONObject().put("by",
			"keys"));
		assertEquals(List.of("antilia:save new keys form"), heard);
		assertEquals("new", name.getObject());

		// the name is required: the form does not take an empty one
		tester.getRequest().getPostParameters().setParameterValue(page.name.getInputName(), "");
		ClientEventsTester.fire(tester, page.box, "antilia:save");
		assertEquals(List.of("antilia:save new keys form", "error antilia:save"), heard);
		assertEquals("new", name.getObject());
	}

	@Test
	void takesSeveralEventsOfOneName()
	{
		List<String> heard = new ArrayList<>();
		IAjaxEvent first = AjaxEvent.of("antilia:resize", (values, target) -> heard.add(
			"first " + values));
		IAjaxEvent second = AjaxEvent.of("antilia:resize", (values, target) -> heard.add(
			"second " + values));
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(new ClientEvent("antilia:resize", "function(event){document.cookie='size=1';}"))
			.add(first).add(second);
		tester.startPage(new TestPage(events));

		// each its own listener in the browser
		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":0");
		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":1");
		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":2");

		// each going to the server by its own behavior, with its own URL
		assertNotSame(events.getAjaxBehavior(first), events.getAjaxBehavior(second));
		assertEquals(2, urlsOf(tester.getLastResponseAsString()).size());

		// each request goes to its own event, by its behavior
		for (IAjaxEvent event : List.of(second, first))
		{
			tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
				"{\"w\":" + (event == first ? 1 : 2) + "}");
			tester.executeBehavior(events.getAjaxBehavior(event));
		}
		assertEquals(List.of("second {\"w\":2}", "first {\"w\":1}"), heard);
	}

	@Test
	void ignoresARequestOfAnEventTheComponentNoLongerHas()
	{
		List<String> heard = new ArrayList<>();
		IAjaxEvent focus = AjaxEvent.of("focusin", (values, target) -> heard.add("focusin " +
			values));
		ClientEventsBehavior events = new ClientEventsBehavior().add(focus);
		TestPage page = new TestPage(events);
		tester.startPage(page);
		AbstractDefaultAjaxBehavior behavior = events.getAjaxBehavior(focus);

		// values not JSON: none
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER, "[1,");
		tester.executeBehavior(behavior);
		assertEquals(List.of("focusin {}"), heard);

		// taken away once its URL went to the browser, which may still send a request to it: the
		// behavior stays, leaving the requests alone
		assertTrue(events.remove(focus));
		tester.startPage(page);
		tester.assertContainsNot("focusin");
		assertSame(behavior, events.getAjaxBehavior(focus));
		assertTrue(page.box.getBehaviors().contains(behavior));
		tester.executeBehavior(behavior);
		assertEquals(List.of("focusin {}"), heard);
	}

	@Test
	void tellsASubclassGivingNewEventsEachTime()
	{
		ClientEventsBehavior events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public List<IClientEvent> getEvents()
			{
				return List.of(AjaxEvent.of("focusin", (values, target) -> {
				}));
			}
		};
		Throwable thrown = assertThrows(RuntimeException.class, () -> tester.startPage(
			new TestPage(events)));
		while (thrown.getCause() != null && !(thrown instanceof IllegalStateException))
		{
			thrown = thrown.getCause();
		}
		assertTrue(thrown instanceof IllegalStateException &&
			thrown.getMessage().contains("same events each time"), String.valueOf(thrown));
	}

	@Test
	void givesTwoEventsOfOneNameEachTheAttributesOfItsOwnBehavior()
	{
		// on blur the state goes under the page's veil, and another request the veil leaves alone
		SendStateEvent veiled = new SendStateEvent("blur")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				super.updateAjaxAttributes(attributes);
				attributes.getAjaxCallListeners().add(new AjaxCallListener().onBefore(
					"Veil.show();"));
			}
		};
		IAjaxEvent quiet = AjaxEvent.of("blur", (values, target) -> {
		}).setExtraParameter("_nb", "true");
		ClientEventsBehavior events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				attributes.setChannel(new AjaxChannel("editor"));
			}
		}.add(veiled).add(quiet);
		TestPage page = new TestPage(events);
		tester.startPage(page);
		String document = tester.getLastResponseAsString();

		String first = eventIn(document, 0);
		String second = eventIn(document, 1);
		assertTrue(first.contains("Veil.show();") && !first.contains("_nb"), first);
		assertTrue(second.contains("{\"name\":\"_nb\",\"value\":\"true\"}") &&
			!second.contains("Veil.show();"), second);
		// what the component changes in both
		assertTrue(first.contains("\"ch\":\"editor|s\"") &&
			second.contains("\"ch\":\"editor|s\""), document);
		assertEquals(2, urlsOf(document).size());
	}

	/**
	 * @return the URLs of the requests of the events in the document, each once
	 */
	private static Set<String> urlsOf(String document)
	{
		Set<String> urls = new HashSet<>();
		Matcher matcher = Pattern.compile("\"ajax\":\\{\"u\":\"([^\"]+)\"").matcher(document);
		while (matcher.find())
		{
			urls.add(matcher.group(1));
		}
		return urls;
	}

	/**
	 * @return what the browser gets of the event at the place among those of the component
	 */
	private static String eventIn(String document, int index)
	{
		int start = document.indexOf("\"index\":" + index + ",");
		assertTrue(start >= 0, document);
		int end = document.indexOf("{\"eventName\":", start);
		return document.substring(start, end < 0 ? document.indexOf("],null", start) : end);
	}

	@Test
	void takesEventsOneWayOrTheOther()
	{
		ClientEventsBehavior events = new ClientEventsBehavior();
		assertThrows(IllegalArgumentException.class, () -> events.add(new BothWays()));
		assertThrows(IllegalArgumentException.class, () -> events.add(new IClientEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getEventName()
			{
				return "keydown";
			}
		}));
		// the name of a function, not code
		assertThrows(IllegalArgumentException.class, () -> events.add(ClientEvent.handledBy(
			"keydown", "alert(1)")));
		assertThrows(IllegalArgumentException.class, () -> ClientEvent.handledBy("keydown", ""));
		assertThrows(IllegalArgumentException.class, () -> new ClientEvent("keydown", ""));
		assertThrows(IllegalArgumentException.class, () -> AjaxEvent.of("", (values, target) -> {
		}));
		// going to the server, with nothing the server does with it
		assertThrows(IllegalArgumentException.class, () -> events.add(new IAjaxRoundTripEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getEventName()
			{
				return "nowhere";
			}
		}));
		IClientEvent client = new ClientEvent("keydown", "function(){}");
		assertThrows(IllegalArgumentException.class, () -> events.add(client)
			.getAjaxBehavior(client));
		assertTrue(events.remove(client));
	}

	@Test
	void waitsOnlyAPositiveDelay()
	{
		SendStateEvent event = new SendStateEvent("keydown");
		assertEquals(AbstractDelayedClientEvent.NO_DELAY, event.getDelay());
		assertEquals(AbstractDelayedClientEvent.NO_DELAY, event.setDelay(0).getDelay());
		assertEquals(300, event.setDelay(300).getDelay());
		assertEquals(AbstractDelayedClientEvent.NO_DELAY, event.setDelay(-5).getDelay());
		assertEquals(250, event.setDelay(Duration.ofMillis(250)).getDelay());
		assertEquals(AbstractDelayedClientEvent.NO_DELAY, event.setDelay((Duration)null)
			.getDelay());
		assertSame(event, event.setEventHandlerClientSideCode("function(e){}"));
		assertEquals("SendStateEvent[keydown]", event.toString());
	}

	@Test
	void listensToOneComponent()
	{
		ClientEventsBehavior events = new ClientEventsBehavior();
		new WebMarkupContainer("one").add(events);
		assertThrows(IllegalStateException.class, () -> new WebMarkupContainer("two").add(
			events));
	}

	/** An event going to the server both ways, which no behavior takes. */
	private static final class BothWays implements IAjaxEvent, IAjaxSubmitEvent
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String getEventName()
		{
			return "both";
		}

		@Override
		public String getEventClientHandlerName()
		{
			return SEND_HANDLER;
		}

		@Override
		public void onEvent(JSONObject values, AjaxRequestTarget target)
		{
		}

		@Override
		public void onEvent(JSONObject values, Form<?> form, AjaxRequestTarget target)
		{
		}
	}

	@Test
	void keepsTheEventsByTheirNames()
	{
		IAjaxEvent first = AjaxEvent.of("input", (values, target) -> {
		});
		IClientEvent blur = ClientEvent.handledBy("blur", "Articles.left");
		IAjaxEvent second = AjaxEvent.of("input", (values, target) -> {
		});
		ClientEventsBehavior events = new ClientEventsBehavior().add(first).add(blur).add(second);

		// those of a name together, in the order the names came
		assertEquals(List.of(first, second, blur), events.getEvents());
		assertEquals(List.of("input", "blur"), List.copyOf(events.getEventsByName().keySet()));
		assertEquals(List.of(first, second), events.getEvents("input"));

		// one in the place of those of its name
		IClientEvent nothing = ClientEvent.nothing("input");
		events.set(nothing);
		assertEquals(List.of(nothing, blur), events.getEvents());
		assertEquals(IClientEvent.NO_HANDLER, nothing.getEventClientHandlerName());

		// none of a name
		assertEquals(List.of(nothing), events.removeEvents("input"));
		assertEquals(List.of(), events.removeEvents("input"));
		assertEquals(List.of(), events.getEvents("input"));
		assertTrue(events.remove(blur));
		assertFalse(events.remove(blur));
		assertTrue(events.getEventsByName().isEmpty());
	}

	@Test
	void takesAwayTheEventsOfSeveralNames()
	{
		IAjaxEvent input = AjaxEvent.of("input", (values, target) -> {
		});
		IClientEvent blur = ClientEvent.handledBy("blur", "Articles.left");
		IClientEvent scroll = ClientEvent.handledBy("scroll", "Articles.scrolled");
		ClientEventsBehavior events = new ClientEventsBehavior().add(input).add(blur).add(scroll);

		// in the order of the names, those it has not left alone
		assertEquals(List.of(scroll, input), events.removeEvents("scroll", "missing", "input"));
		assertEquals(List.of(blur), events.getEvents());
		IClientEventName named = () -> "blur";
		assertEquals(List.of(blur), events.removeEvents(named));
		assertTrue(events.getEvents().isEmpty());
	}

	@Test
	void letsTheComponentChangeTheAttributesOfTheRequests()
	{
		List<String> forms = new ArrayList<>();
		ClientEventsBehavior events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
			{
				forms.add(String.valueOf(attributes.getFormId()));
				attributes.getExtraParameters().put("_nb", "true");
				attributes.getAjaxCallListeners().add(new AjaxCallListener()
					.onBefore("attrs._quiet=true;"));
			}
		}.add(AjaxEvent.of("focusin", (values, target) -> {
		})).add(AjaxEvent.of("focusout", (values, target) -> {
		}).setExtraParameter("_nb", "false")).add(new AjaxSubmitEvent("antilia:save")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onEvent(JSONObject values, Form<?> form, AjaxRequestTarget target)
			{
			}
		});
		TestPage page = new TestPage(events);
		tester.startPage(page);

		// the requests of the behavior of each event, the event changing those of its own after
		String document = tester.getLastResponseAsString();
		assertEquals(2, document.split("\\{\"name\":\"_nb\",\"value\":\"true\"\\}", -1).length - 1,
			document);
		assertEquals(1, document.split("\\{\"name\":\"_nb\",\"value\":\"false\"\\}", -1).length - 1,
			document);
		assertEquals(3, document.split("attrs._quiet=true;", -1).length - 1);
		// the Ajax events without the form, those submitting it with
		assertTrue(forms.contains("null"), forms.toString());
		assertTrue(forms.contains(page.form.getMarkupId()), forms.toString());
	}

	@Test
	void givesTheRequestsOfAnEventItsOwnAttributes()
	{
		SendStateEvent blur = new SendStateEvent("blur").setExtraParameter("_nb", "true")
			.setExtraParameter("gone", "x").setExtraParameter("gone", null)
			.addDynamicExtraParameter("return {sentAt: 1};")
			.addAjaxCallListener(new AjaxCallListener().onFailure("console.warn('lost');"));
		AjaxEvent focus = AjaxEvent.of("focus", (values, target) -> {
		}).setExtraParameter("quiet", "1").setMethod(AjaxRequestAttributes.Method.GET);
		ClientEventsBehavior events = new ClientEventsBehavior().add(blur)
			.add(new CookieEvent()).add(new SendStateEvent("input")).add(focus);
		TestPage page = new TestPage(events);
		tester.startPage(page);
		String box = page.box.getMarkupId();

		assertEquals(Map.of("_nb", "true"), blur.getExtraParameters());
		assertEquals(List.of("return {sentAt: 1};"), blur.getDynamicExtraParameters());
		assertEquals(1, blur.getAjaxCallListeners().size());
		assertEquals(AjaxRequestAttributes.Method.GET, focus.getMethod());
		// its extra parameters in the ep of its requests, where a page veil sees them, its
		// dynamic ones and its listeners as Wicket's Ajax takes them
		tester.assertContains("\\{\"eventName\":\"blur\",\"index\":0,\"delay\":-1," +
			"\"eventClientHandlerName\":\"Wicket.ClientEvents.sendState\",\"ajax\":\\{\"u\":" +
			"\"[^\"]+\",\"m\":\"POST\",\"c\":\"" + box + "\",\"fh\":\\[function\\([^)]*\\)" +
			"\\{console.warn\\('lost'\\);\\}\\],\"ep\":\\[\\{\"name\":\"_nb\",\"value\":\"true\"\\}\\]," +
			"\"dep\":\\[function\\(attrs\\)\\{return \\{sentAt: 1\\};\\}\\]\\}\\}");
		// none for an event without
		tester.assertContains("\\{\"eventName\":\"input\",\"index\":2,\"delay\":-1," +
			"\"eventClientHandlerName\":\"Wicket.ClientEvents.sendState\",\"ajax\":\\{\"u\":" +
			"\"[^\"]+\",\"m\":\"POST\",\"c\":\"" + box + "\"\\}\\}");
		// in the URL: GET, Wicket's own default, which its Ajax does not write
		tester.assertContains("\\{\"eventName\":\"focus\",\"index\":3,\"delay\":-1," +
			"\"eventClientHandlerName\":\"Wicket.ClientEvents.send\",\"ajax\":\\{\"u\":" +
			"\"[^\"]+\",\"c\":\"" + box + "\",\"ep\":\\[\\{\"name\":\"quiet\",\"value\":\"1\"\\}" +
			"\\]\\}\\}");
		// in the attributes alone, not among the properties of the event
		tester.assertContainsNot("extraParameters");
		tester.assertContainsNot("ajaxCallListeners");
		tester.assertContainsNot("\"method\"");
		assertThrows(IllegalArgumentException.class, () -> blur.setExtraParameter("", "x"));
		assertThrows(IllegalArgumentException.class, () -> blur.addDynamicExtraParameter(""));
		assertThrows(IllegalArgumentException.class, () -> blur.addAjaxCallListener(null));
	}

	@Test
	void sendsTheRequestsOfAnEventByItsMethod()
	{
		List<String> heard = new ArrayList<>();
		AjaxEvent focus = AjaxEvent.of("focus", (values, target) -> heard.add("focus " + values))
			.setMethod(AjaxRequestAttributes.Method.GET);
		ClientEventsBehavior events = new ClientEventsBehavior().add(focus);
		TestPage page = new TestPage(events);
		tester.startPage(page);

		// the parameters in the URL, which the server reads as those in the body
		tester.getRequest().setMethod("GET");
		ClientEventsTester.fire(tester, page.box, "focus", new JSONObject().put("at", 1));
		assertEquals(List.of("focus {\"at\":1}"), heard);
	}

	@Test
	void takesWhatTheComponentDidNotSendFromThePagesOtherRequests()
	{
		List<String> heard = new ArrayList<>();
		ClientEventsBehavior events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected String getClientPendingValues()
			{
				return "function(component){return {kept:'1'};}";
			}

			@Override
			protected void onPendingValuesSent(AjaxRequestTarget target,
				IRequestParameters values)
			{
				heard.add("pending " + values.getParameterNames() + " " +
					values.getParameterValue("kept") + " " +
					values.getParameterValue("missing").isNull());
			}
		}.add(AjaxEvent.of("focusin", (values, target) -> heard.add("focusin")));
		TestPage page = new TestPage(events);
		AjaxEventBehavior change = new AjaxEventBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				heard.add("change");
				// a page telling its components of the target itself: taken once all the same
				getComponent().send(getComponent().getPage(), Broadcast.BREADTH, target);
			}
		};
		page.name.add(change);
		tester.startPage(page);
		tester.assertContains(",null,\\{pending:function\\(component\\)" +
			"\\{return \\{kept:'1'\\};\\}\\}\\);");

		// another component's request, handled first
		String parameter = ClientEventsBehavior.PENDING_PARAMETER_PREFIX +
			page.box.getMarkupId();
		tester.getRequest().setParameter(parameter, "{\"kept\":\"2\",\"number\":3}");
		tester.executeBehavior(change);
		assertEquals(List.of("change", "pending [kept, number] 2 true"), heard);

		// nothing for the component, nor what is not JSON
		heard.clear();
		tester.executeBehavior(change);
		tester.getRequest().setParameter(parameter, "not JSON");
		tester.executeBehavior(change);
		tester.getRequest().setParameter(parameter, "{}");
		tester.executeBehavior(change);
		assertEquals(List.of("change", "change", "change"), heard);
	}

	@Test
	void takesNothingPendingWithoutTheFunction()
	{
		List<String> heard = new ArrayList<>();
		ClientEventsBehavior events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onPendingValuesSent(AjaxRequestTarget target,
				IRequestParameters values)
			{
				heard.add("pending");
			}
		}.add(AjaxEvent.of("focusin", (values, target) -> heard.add("focusin")));
		TestPage page = new TestPage(events);
		AjaxEventBehavior change = new AjaxEventBehavior("change")
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onEvent(AjaxRequestTarget target)
			{
				heard.add("change");
			}
		};
		page.name.add(change);
		tester.startPage(page);
		tester.assertContainsNot("pending:");

		tester.getRequest().setParameter(ClientEventsBehavior.PENDING_PARAMETER_PREFIX +
			page.box.getMarkupId(), "{\"kept\":\"2\"}");
		tester.executeBehavior(change);
		assertEquals(List.of("change"), heard);
	}

	@Test
	void namesTheHandlerOrRendersItsCode()
	{
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(ClientEvent.handledBy("keyup", "Search.suggest"))
			.add(ClientEvent.handledBy("keydown", "Search.ignored")
				.setEventHandlerClientSideCode("function(){}"))
			.add(new SendStateEvent("blur"));
		TestPage page = new TestPage(events);
		tester.startPage(page);

		tester.assertContains("\\{\"eventName\":\"keyup\",\"index\":0," +
			"\"eventClientHandlerName\":\"Search.suggest\"\\}");
		// the code wins, in the browser
		tester.assertContains("\\{\"eventName\":\"keydown\",\"index\":1," +
			"\"eventClientHandlerName\":\"Search.ignored\"," +
			"\"eventHandlerClientSideCode\":function\\(\\)\\{\\}\\}");
		tester.assertContains("\\{\"eventName\":\"blur\",\"index\":2," +
			"\"delay\":-1,\"eventClientHandlerName\":\"Wicket.ClientEvents.sendState\"," +
			"\"ajax\":\\{");
	}

	@Test
	void givesTheComponentWhatItsEventsSendBesides()
	{
		List<String> heard = new ArrayList<>();
		ClientEventsBehavior events = new ClientEventsBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected String getClientValues()
			{
				return "function(component){return {kept:'1'};}";
			}

			@Override
			protected void onEventSent(AjaxRequestTarget target, IRequestParameters parameters)
			{
				heard.add("sent " + parameters.getParameterValue("kept"));
			}
		}.add(AjaxEvent.of("focusin", (values, target) -> heard.add("focusin")))
			.add(new SendStateEvent("leave"));
		TestPage page = new TestPage(events);
		tester.startPage(page);
		tester.assertContains(",null,\\{values:function\\(component\\)" +
			"\\{return \\{kept:'1'\\};\\}\\}\\);");

		ClientEventsTester.fire(tester, page.box, events.getEvents("focusin").get(0),
			new JSONObject(), Map.of("kept", "1"));
		// the component first, even for an event that no longer is there
		IClientEvent focus = events.getEvents("focusin").get(0);
		AbstractDefaultAjaxBehavior gone = events.getAjaxBehavior(focus);
		assertTrue(events.remove(focus));
		tester.getRequest().setParameter("kept", "2");
		tester.executeBehavior(gone);
		ClientEventsTester.fire(tester, page.box, "leave");
		assertEquals(List.of("sent 1", "focusin", "sent 2", "sent null"), heard);
	}

	@Test
	void updatesTheModelOfItsFormComponentWithAnEvent()
	{
		List<String> heard = new ArrayList<>();
		Model<String> name = Model.of("old");
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(AjaxUpdateEvent.of("antilia:save", (values, component, target) -> heard.add(
				"antilia:save " + component.getModelObject() + " " + values.optString("by")),
				(values, component, target, e) -> heard.add("error antilia:save " + e)));
		FieldPage page = new FieldPage(events, name);
		tester.startPage(page);
		tester.assertContains("\\{\"eventName\":\"antilia:save\",\"index\":0");

		tester.getRequest().getPostParameters().setParameterValue(page.name.getInputName(),
			"new");
		ClientEventsTester.fire(tester, page.name, "antilia:save", new JSONObject().put("by",
			"keys"));
		assertEquals(List.of("antilia:save new keys"), heard);
		assertEquals("new", name.getObject());

		// required: the model does not take an empty name
		tester.getRequest().getPostParameters().setParameterValue(page.name.getInputName(), "");
		ClientEventsTester.fire(tester, page.name, "antilia:save");
		assertEquals(List.of("antilia:save new keys", "error antilia:save null"), heard);
		assertEquals("new", name.getObject());
	}

	@Test
	void throwsAgainWhatUpdatingThrewByDefault()
	{
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(AjaxUpdateEvent.of("antilia:save", (values, component, target) -> {
				throw new IllegalStateException("broken");
			}));
		FieldPage page = new FieldPage(events, Model.of("old"));
		tester.startPage(page);

		tester.getRequest().getPostParameters().setParameterValue(page.name.getInputName(),
			"new");
		assertThrows(IllegalStateException.class, () -> ClientEventsTester.fire(tester,
			page.name, "antilia:save"));
	}

	@Test
	void updatesOnlyAFormComponent()
	{
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(AjaxUpdateEvent.of("antilia:save", (values, component, target) -> {
			}));
		RuntimeException e = assertThrows(RuntimeException.class, () -> tester.startPage(
			new TestPage(events)));
		while (e.getCause() instanceof RuntimeException cause)
		{
			e = cause;
		}
		assertTrue(e instanceof IllegalStateException, e.toString());
	}

	@Test
	void writesTheEventByItsGetters()
	{
		JSONObject json = new CookieEvent().toJson();

		assertEquals("antilia:resize", json.getString("eventName"));
		assertEquals("Sizes.keep", json.getString("eventClientHandlerName"));
		assertEquals("size", json.getString("cookieName"));
		assertTrue(json.getBoolean("remembered"));
		assertEquals("[1,2]", json.getJSONArray("sizes").toString());
		// those giving null left out, no delay for an event without
		assertFalse(json.has("nothing"));
		assertFalse(json.has("eventHandlerClientSideCode"));
		assertFalse(json.has("delay"));

		// the getters an anonymous class overrides or adds
		AjaxEvent typing = new AjaxEvent("input")
		{
			private static final long serialVersionUID = 1L;

			@Override
			public int getDelay()
			{
				return 700;
			}

			@SuppressWarnings("unused")
			public String getMode()
			{
				return "words";
			}

			@Override
			public void onEvent(JSONObject values, AjaxRequestTarget target)
			{
			}
		};
		json = typing.toJson();
		assertEquals(700, json.getInt("delay"));
		assertEquals("words", json.getString("mode"));
		assertEquals(IAjaxRoundTripEvent.SEND_HANDLER, json.getString("eventClientHandlerName"));

		// an enum by its name, an array as one, anything else as its text, never walked through
		WebMarkupContainer panel = new WebMarkupContainer("panel");
		json = new CookieEvent()
		{
			private static final long serialVersionUID = 1L;

			@SuppressWarnings("unused")
			public DayOfWeek getDay()
			{
				return DayOfWeek.MONDAY;
			}

			@SuppressWarnings("unused")
			public WebMarkupContainer getPanel()
			{
				return panel;
			}

			@SuppressWarnings("unused")
			public int[] getSteps()
			{
				return new int[] { 3, 4 };
			}
		}.toJson();
		assertEquals("MONDAY", json.getString("day"));
		assertEquals(panel.toString(), json.getString("panel"));
		assertEquals("[3,4]", json.getJSONArray("steps").toString());
	}

	@Test
	void givesTheHandlerThePropertiesOfTheEvent()
	{
		ClientEventsBehavior events = new ClientEventsBehavior().add(new CookieEvent())
			.add(new ClientEvent("keydown", "function(event){return event.a<1&&event.b>2;}"));
		TestPage page = new TestPage(events);
		tester.startPage(page);

		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":0," +
			"\"cookieName\":\"size\"," +
			"\"eventClientHandlerName\":\"Sizes.keep\",\"remembered\":true," +
			"\"sizes\":\\[1,2\\]\\}");
		// the code as it is, a text escaped
		tester.assertContains("function\\(event\\)\\{return event.a<1&&event.b>2;\\}");
		CookieEvent named = new CookieEvent();
		named.cookieName = "</script>";
		tester.startPage(new TestPage(new ClientEventsBehavior().add(named)));
		tester.assertContains("\"cookieName\":\"\\\\u003c\\\\/script\\\\u003e\"");
		tester.assertContainsNot("\"cookieName\":\"</script>");
	}

	@Test
	void keepsTheNamePlaceAndAttributesOfTheEvent()
	{
		ClientEventsBehavior events = new ClientEventsBehavior().add(new CookieEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public JSONObject toJson()
			{
				return new JSONObject().put("eventName", "other").put("index", 9)
					.put("ajax", new JSONObject().put("u", "elsewhere"))
					.put("eventClientHandlerName", "Sizes.keep")
					.put("own", 1);
			}
		});
		tester.startPage(new TestPage(events));

		// staying in the browser: no attributes of requests, whatever the event tells
		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":0," +
			"\"eventClientHandlerName\":\"Sizes.keep\",\"own\":1\\}");
		tester.assertContainsNot("elsewhere");
	}

	/** An event of a page's own, its getters telling its handler what to do. */
	private static class CookieEvent implements IClientEvent
	{
		private static final long serialVersionUID = 1L;

		String cookieName = "size";

		@Override
		public String getEventName()
		{
			return "antilia:resize";
		}

		@Override
		public String getEventClientHandlerName()
		{
			return "Sizes.keep";
		}

		public String getCookieName()
		{
			return cookieName;
		}

		public boolean isRemembered()
		{
			return true;
		}

		public List<Integer> getSizes()
		{
			return List.of(1, 2);
		}

		public String getNothing()
		{
			return null;
		}
	}

	/** A form with a required field listening to events. */
	public static class FieldPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final FormComponent<String> name;

		FieldPage(ClientEventsBehavior events, Model<String> model)
		{
			Form<Void> form = new Form<>("form");
			add(form);
			name = new TextField<>("name", model);
			name.setRequired(true);
			name.add(events);
			form.add(name);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<input type=\"text\" wicket:id=\"name\"/></form></body></html>");
		}
	}

	/** A form with a required field and a box listening to events. */
	@Test
	void rendersOnceAResponseBeforeTheScriptOfTheComponentWhenItAsks()
	{
		List<TestPage> pages = new ArrayList<>();
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(AjaxEvent.of("focusin", (values, target) -> target.add(pages.get(0).box)));
		TestPage page = new TestPage(events);
		pages.add(page);
		// a component whose script fires events as it starts renders the events before it
		page.box.remove(events);
		page.box.add(new Behavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void renderHead(Component component, IHeaderResponse response)
			{
				events.renderHead(component, response);
				response.render(OnDomReadyHeaderItem.forScript("Starting.box();"));
			}
		}, events);
		tester.startPage(page);
		assertListeningOnceBeforeStarting(tester.getLastResponseAsString());

		// and in each Ajax response rendering the component again
		ClientEventsTester.fire(tester, page.box, "focusin");
		assertListeningOnceBeforeStarting(tester.getLastResponseAsString());
	}

	private static void assertListeningOnceBeforeStarting(String response)
	{
		int listening = response.indexOf("Wicket.ClientEvents.attach(");
		assertTrue(listening >= 0 && listening < response.indexOf("Starting.box();"), response);
		assertEquals(listening, response.lastIndexOf("Wicket.ClientEvents.attach("), response);
	}

	public static class TestPage extends WebPage implements IMarkupResourceStreamProvider
	{
		private static final long serialVersionUID = 1L;

		final Form<Void> form;

		final TextField<String> name;

		final WebMarkupContainer box;

		TestPage(ClientEventsBehavior events)
		{
			this(events, Model.of("old"));
		}

		TestPage(ClientEventsBehavior events, Model<String> model)
		{
			form = new Form<>("form");
			add(form);
			name = new TextField<>("name", model);
			name.setRequired(true);
			form.add(name);
			box = new WebMarkupContainer("box");
			box.add(events);
			form.add(box);
		}

		@Override
		public IResourceStream getMarkupResourceStream(MarkupContainer container,
			Class<?> containerClass)
		{
			return new StringResourceStream("<html><head></head><body><form wicket:id=\"form\">" +
				"<input type=\"text\" wicket:id=\"name\"/><div wicket:id=\"box\"></div></form>" +
				"</body></html>");
		}
	}
}
