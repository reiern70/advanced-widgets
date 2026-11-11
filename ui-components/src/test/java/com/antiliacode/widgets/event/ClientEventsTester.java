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

import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.tester.BaseWicketTester;

import com.github.openjson.JSONObject;

/**
 * Sends a component the request the browser sends for one of its {@link IClientEvent events},
 * in a test with Wicket's tester, as if its handler had called {@code send(values)}: the handlers
 * of the events do not run in the tester.
 *
 * <pre>
 * tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), "&lt;p&gt;Saved&lt;/p&gt;");
 * ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SAVE);
 * // the state of an editor, and its text, as the editor sends them with its events
 * ClientEventsTester.sendState(tester, editor, "{\"scrollTop\":40}", true);
 * </pre>
 */
public final class ClientEventsTester
{
	private ClientEventsTester()
	{
	}

	/**
	 * Sends the first event of the name going to the server, without values: the page's own
	 * before the {@link SendStateEvent}s an editor listens to by default.
	 *
	 * @param tester
	 *            the tester, whose request holds what the test set, the input of the fields for
	 *            an event submitting the form for example
	 * @param component
	 *            the component listening to the event, with a {@link ClientEventsBehavior}
	 * @param eventName
	 *            the name of the event
	 */
	public static void fire(BaseWicketTester tester, Component component, String eventName)
	{
		fire(tester, component, eventName, new JSONObject());
	}

	/**
	 * Sends the first event of the name going to the server, without values.
	 *
	 * @param tester
	 *            the tester
	 * @param component
	 *            the component listening to the event, with a {@link ClientEventsBehavior}
	 * @param eventName
	 *            the name of the event
	 */
	public static void fire(BaseWicketTester tester, Component component,
		IClientEventName eventName)
	{
		fire(tester, component, eventName.getEventName(), new JSONObject());
	}

	/**
	 * Sends the first event of the name going to the server.
	 *
	 * @param tester
	 *            the tester
	 * @param component
	 *            the component listening to the event, with a {@link ClientEventsBehavior}
	 * @param eventName
	 *            the name of the event
	 * @param values
	 *            what the handler of the event sends
	 */
	public static void fire(BaseWicketTester tester, Component component,
		IClientEventName eventName, JSONObject values)
	{
		fire(tester, component, eventName.getEventName(), values);
	}

	/**
	 * Sends the first event of the name going to the server: the page's own before the
	 * {@link SendStateEvent}s an editor listens to by default.
	 *
	 * @param tester
	 *            the tester, whose request holds what the test set, the input of the fields for
	 *            an event submitting the form for example
	 * @param component
	 *            the component listening to the event, with a {@link ClientEventsBehavior}
	 * @param eventName
	 *            the name of the event
	 * @param values
	 *            what the handler of the event sends
	 * @throws IllegalArgumentException
	 *             when the component has no event of the name going to the server
	 */
	public static void fire(BaseWicketTester tester, Component component, String eventName,
		JSONObject values)
	{
		Args.notNull(tester, "tester");
		Args.notEmpty(eventName, "eventName");
		ClientEventsBehavior behavior = behaviorOf(component);
		List<IClientEvent> events = behavior.getEvents();
		int chosen = -1;
		for (int index = 0; index < events.size(); index++)
		{
			IClientEvent event = events.get(index);
			if (event.getEventName().equals(eventName) && goesToServer(event) &&
				(chosen < 0 || events.get(chosen) instanceof SendStateEvent &&
					!(event instanceof SendStateEvent)))
			{
				chosen = index;
			}
		}
		if (chosen < 0)
		{
			throw new IllegalArgumentException(component + " has no event " + eventName +
				" going to the server");
		}
		send(tester, behavior, events.get(chosen), chosen, values, Map.of());
	}

	/**
	 * Sends an event of the component, with the parameters the component adds to its requests.
	 *
	 * @param tester
	 *            the tester, whose request holds what the test set
	 * @param component
	 *            the component listening to the event, with a {@link ClientEventsBehavior}
	 * @param event
	 *            the event, going to the server
	 * @param values
	 *            what the handler of the event sends
	 * @param parameters
	 *            what the component adds to the request, the state of an editor for example
	 * @throws IllegalArgumentException
	 *             when the component does not listen to the event
	 */
	public static void fire(BaseWicketTester tester, Component component, IClientEvent event,
		JSONObject values, Map<String, String> parameters)
	{
		Args.notNull(tester, "tester");
		ClientEventsBehavior behavior = behaviorOf(component);
		int index = behavior.getEvents().indexOf(event);
		if (index < 0 || !goesToServer(event))
		{
			throw new IllegalArgumentException(component + " has no event " + event +
				" going to the server");
		}
		send(tester, behavior, event, index, values, parameters);
	}

	private static void send(BaseWicketTester tester, ClientEventsBehavior behavior,
		IClientEvent event, int index, JSONObject values, Map<String, String> parameters)
	{
		parameters.forEach((name, value) -> tester.getRequest().setParameter(name, value));
		tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER,
			event.getEventName());
		tester.getRequest().setParameter(ClientEventsBehavior.INDEX_PARAMETER,
			String.valueOf(index));
		tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
			(values == null ? new JSONObject() : values).toString());
		tester.executeBehavior(behavior.getAjaxBehavior(event));
	}

	/**
	 * Sends an editor's state as its first {@link SendStateEvent} does, on blur by default: with
	 * the text, when the test put it in the request, as the browser posts it.
	 *
	 * @param tester
	 *            the tester, whose request holds the text of the editor, if the test put it there
	 * @param editor
	 *            the editor
	 * @param state
	 *            the state, as JSON
	 * @param textChanged
	 *            whether the text changed since it was last sent: the editor then takes it
	 * @throws IllegalArgumentException
	 *             when the editor has no {@link SendStateEvent}
	 */
	public static void sendState(BaseWicketTester tester, Component editor, String state,
		boolean textChanged)
	{
		ClientEventsBehavior behavior = behaviorOf(editor);
		IClientEvent event = behavior.getEvents().stream()
			.filter(SendStateEvent.class::isInstance).findFirst()
			.orElseThrow(() -> new IllegalArgumentException(editor + " sends no state"));
		fire(tester, editor, event, new JSONObject(), Map.of(SendStateEvent.STATE_PARAMETER,
			state, SendStateEvent.TEXT_CHANGED_PARAMETER, String.valueOf(textChanged)));
	}

	private static boolean goesToServer(IClientEvent event)
	{
		return event instanceof IAjaxEvent || event instanceof IAjaxUpdateEvent ||
			event instanceof IAjaxSubmitEvent;
	}

	private static ClientEventsBehavior behaviorOf(Component component)
	{
		Args.notNull(component, "component");
		return component.getBehaviors(ClientEventsBehavior.class).stream().findFirst()
			.orElseThrow(() -> new IllegalArgumentException(component +
				" has no ClientEventsBehavior"));
	}
}
