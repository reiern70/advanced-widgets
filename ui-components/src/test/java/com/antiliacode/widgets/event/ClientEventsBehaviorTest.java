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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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
			.add(AjaxEvent.of("focusin", (eventId, values, target) -> heard.add(eventId)))
			.add(new ClientEvent("keydown", "function(event){console.log(event.key);}")
				.setDelay(250))
			.add(AjaxEvent.of("antilia:custom", (eventId, values, target) -> heard.add(eventId))
				.setClientFunction("function(event,component,send){send({a:1});}"));
		TestPage page = new TestPage(events);
		tester.startPage(page);

		tester.assertContains("antilia-client-events.js");
		tester.assertContains("Wicket.ClientEvents.attach\\(\"" + page.box.getMarkupId() +
			"\",\\[\\{id:\"focusin\",index:0,kind:'ajax',delay:-1\\}," +
			"\\{id:\"keydown\",index:1,kind:'client',delay:250," +
			"fn:function\\(event\\)\\{console.log\\(event.key\\);\\}\\}," +
			"\\{id:\"antilia:custom\",index:2,kind:'ajax',delay:-1," +
			"fn:function\\(event,component,send\\)\\{send\\(\\{a:1\\}\\);\\}\\}\\]\\);");
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
			"x\"</script><script>alert(1)</script>", (eventId, values, target) -> {
			}))));

		// the name cannot close the script: JSON writes <\/ for </
		tester.assertContains("id:\"x\\\\\"<\\\\/script><script>alert\\(1\\)<\\\\/script>\"");
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
			.add(AjaxEvent.of("focusin", (eventId, values, target) -> heard.add(eventId + " " +
				values)))
			.add(new AjaxEvent("antilia:resize")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onEvent(String eventId, JSONObject values, AjaxRequestTarget target)
				{
					heard.add(eventId + " " + values.getJSONObject("size").getInt("width"));
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
				public void onEvent(String eventId, JSONObject values, Form<?> form,
					AjaxRequestTarget target)
				{
					heard.add(eventId + " " + name.getObject() + " " + values.optString("by") +
						" " + form.getId());
				}

				@Override
				public void onError(String eventId, JSONObject values, Form<?> form,
					AjaxRequestTarget target)
				{
					heard.add("error " + eventId);
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
		IAjaxEvent first = AjaxEvent.of("antilia:resize", (eventId, values, target) -> heard.add(
			"first " + values));
		IAjaxEvent second = AjaxEvent.of("antilia:resize", (eventId, values, target) -> heard.add(
			"second " + values));
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(new ClientEvent("antilia:resize", "function(event){document.cookie='size=1';}"))
			.add(first).add(second);
		tester.startPage(new TestPage(events));

		// each its own listener in the browser
		tester.assertContains("\\{id:\"antilia:resize\",index:0,kind:'client'");
		tester.assertContains("\\{id:\"antilia:resize\",index:1,kind:'ajax'");
		tester.assertContains("\\{id:\"antilia:resize\",index:2,kind:'ajax'");

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
		IAjaxEvent focus = AjaxEvent.of("focusin", (eventId, values, target) -> heard.add(
			eventId));
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
			public String getId()
			{
				return "keydown";
			}

			@Override
			public String getClientFunction()
			{
				return null;
			}
		}));
		assertThrows(IllegalArgumentException.class, () -> new ClientEvent("keydown", ""));
		assertThrows(IllegalArgumentException.class, () -> AjaxEvent.of("", (eventId, values,
			target) -> {
		}));
		IClientEvent client = new ClientEvent("keydown", "function(){}");
		assertThrows(IllegalArgumentException.class, () -> events.add(client)
			.getAjaxBehavior(client));
		assertTrue(events.remove(client));
	}

	@Test
	void waitsOnlyAPositiveDelay()
	{
		ClientEvent event = new ClientEvent("keydown", "function(){}");
		assertEquals(IClientEvent.NO_DELAY, event.getDelay());
		assertEquals(IClientEvent.NO_DELAY, event.setDelay(0).getDelay());
		assertEquals(300, event.setDelay(300).getDelay());
		assertEquals(IClientEvent.NO_DELAY, event.setDelay(-5).getDelay());
		assertSame(event, event.setClientFunction("function(e){}"));
		assertEquals("ClientEvent[keydown]", event.toString());
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
		public String getId()
		{
			return "both";
		}

		@Override
		public String getClientFunction()
		{
			return null;
		}

		@Override
		public void onEvent(String eventId, JSONObject values, AjaxRequestTarget target)
		{
		}

		@Override
		public void onEvent(String eventId, JSONObject values, Form<?> form,
			AjaxRequestTarget target)
		{
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
