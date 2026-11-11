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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
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
				.setEventHandlerClientSideCode("function(event,component,send){send({a:1});}"));
		TestPage page = new TestPage(events);
		tester.startPage(page);

		tester.assertContains("antilia-client-events.js");
		tester.assertContains("Wicket.ClientEvents.attach\\(\"" + page.box.getMarkupId() +
			"\",\\[\\{\"eventName\":\"focusin\",\"index\":0,\"kind\":\"ajax\",\"delay\":-1," +
			"\"eventClientHandlerName\":\"Wicket.ClientEvents.send\"\\}," +
			// code as code, the delay of the events having one
			"\\{\"eventName\":\"keydown\",\"index\":1,\"kind\":\"client\"," +
			"\"eventHandlerClientSideCode\":function\\(event\\)\\{console.log\\(event.key\\);" +
			"\\}\\}," +
			"\\{\"eventName\":\"antilia:custom\",\"index\":2,\"kind\":\"ajax\",\"delay\":250," +
			"\"eventClientHandlerName\":\"Wicket.ClientEvents.send\"," +
			"\"eventHandlerClientSideCode\":function\\(event,component,send\\)\\{" +
			"send\\(\\{a:1\\}\\);\\}\\}\\]," +
			"null,\\{ajaxUrl:\"[^\"]+\"\\}\\);");
		// the Ajax events go by one behavior, which reads what the private event tells
		tester.assertContains("\"e\":\"antilia-client-events:ajax\"");
		tester.assertContains("antilia-event-values");
		// posted: the value of a field goes with it, a long text for example
		tester.assertContains("\"m\":\"POST\"");
		// no event submits the form: its behavior renders nothing
		tester.assertContainsNot("antilia-client-events:submit");
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
		tester.startPage(new TestPage(new ClientEventsBehavior()));

		tester.assertContainsNot("Wicket.ClientEvents");
		tester.assertContainsNot("antilia-client-events");
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
		tester.assertContains("\"e\":\"antilia-client-events:submit\"");
		tester.assertContains("\"f\":\"" + page.form.getMarkupId() + "\"");

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
		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":0,\"kind\":\"client\"");
		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":1,\"kind\":\"ajax\"");
		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":2,\"kind\":\"ajax\"");

		// each request goes to its own event, by its place
		for (int index : new int[] { 2, 1 })
		{
			tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER,
				"antilia:resize");
			tester.getRequest().setParameter(ClientEventsBehavior.INDEX_PARAMETER,
				String.valueOf(index));
			tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
				"{\"w\":" + index + "}");
			tester.executeBehavior(events.getAjaxBehavior(index == 1 ? first : second));
		}
		assertEquals(List.of("second {\"w\":2}", "first {\"w\":1}"), heard);
	}

	@Test
	void ignoresARequestOfAnEventTheComponentNoLongerHas()
	{
		List<String> heard = new ArrayList<>();
		IAjaxEvent focus = AjaxEvent.of("focusin", (values, target) -> heard.add("focusin"));
		ClientEventsBehavior events = new ClientEventsBehavior().add(focus);
		TestPage page = new TestPage(events);
		tester.startPage(page);

		// another name at the place of the event
		tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER, "focusout");
		tester.getRequest().setParameter(ClientEventsBehavior.INDEX_PARAMETER, "0");
		tester.executeBehavior(events.getAjaxBehavior(focus));
		// no event at the place
		tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER, "focusin");
		tester.getRequest().setParameter(ClientEventsBehavior.INDEX_PARAMETER, "3");
		tester.executeBehavior(events.getAjaxBehavior(focus));
		assertTrue(heard.isEmpty());

		// values not JSON: none
		tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER, "focusin");
		tester.getRequest().setParameter(ClientEventsBehavior.INDEX_PARAMETER, "0");
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER, "[1,");
		tester.executeBehavior(events.getAjaxBehavior(focus));
		assertEquals(List.of("focusin"), heard);
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
	void namesTheHandlerOrRendersItsCode()
	{
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(ClientEvent.handledBy("keyup", "Search.suggest"))
			.add(ClientEvent.handledBy("keydown", "Search.ignored")
				.setEventHandlerClientSideCode("function(){}"))
			.add(new SendStateEvent("blur"));
		TestPage page = new TestPage(events);
		tester.startPage(page);

		tester.assertContains("\\{\"eventName\":\"keyup\",\"index\":0,\"kind\":\"client\"," +
			"\"eventClientHandlerName\":\"Search.suggest\"\\}");
		// the code wins, in the browser
		tester.assertContains("\\{\"eventName\":\"keydown\",\"index\":1,\"kind\":\"client\"," +
			"\"eventClientHandlerName\":\"Search.ignored\"," +
			"\"eventHandlerClientSideCode\":function\\(\\)\\{\\}\\}");
		tester.assertContains("\\{\"eventName\":\"blur\",\"index\":2,\"kind\":\"ajax\"," +
			"\"delay\":-1,\"eventClientHandlerName\":\"Wicket.ClientEvents.sendState\"\\}");
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
		tester.assertContains(",null,\\{ajaxUrl:\"[^\"]+\",values:function\\(component\\)" +
			"\\{return \\{kept:'1'\\};\\}\\}\\);");
		// the dynamic parameters of the Ajax behavior read them from what the private event tells
		tester.assertContains("x=d.extra");

		ClientEventsTester.fire(tester, page.box, events.getEvents("focusin").get(0),
			new JSONObject(), Map.of("kept", "1"));
		// the component first, even for an event that no longer is there
		tester.getRequest().setParameter("kept", "2");
		tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER, "gone");
		tester.getRequest().setParameter(ClientEventsBehavior.INDEX_PARAMETER, "7");
		tester.executeBehavior(events.getAjaxBehavior(events.getEvents("focusin").get(0)));
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
		tester.assertContains("\\{\"eventName\":\"antilia:save\",\"index\":0,\"kind\":\"ajax\"");

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
		assertEquals(IClientEvent.SEND_HANDLER, json.getString("eventClientHandlerName"));

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
			"\"kind\":\"client\",\"cookieName\":\"size\"," +
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
	void keepsTheNamePlaceAndKindOfTheEvent()
	{
		ClientEventsBehavior events = new ClientEventsBehavior().add(new CookieEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public JSONObject toJson()
			{
				return new JSONObject().put("eventName", "other").put("index", 9)
					.put("kind", "submit").put("eventClientHandlerName", "Sizes.keep")
					.put("own", 1);
			}
		});
		tester.startPage(new TestPage(events));

		tester.assertContains("\\{\"eventName\":\"antilia:resize\",\"index\":0," +
			"\"kind\":\"client\",\"eventClientHandlerName\":\"Sizes.keep\",\"own\":1\\}");
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
