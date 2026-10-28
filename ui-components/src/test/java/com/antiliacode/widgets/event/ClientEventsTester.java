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

import org.apache.wicket.Component;
import org.apache.wicket.util.lang.Args;
import org.apache.wicket.util.tester.BaseWicketTester;

import com.github.openjson.JSONObject;

/**
 * Sends a component the request the browser sends for one of its {@link IClientEvent events},
 * in a test with Wicket's tester, as if its function had called {@code send(values)}: the
 * functions of the events do not run in the tester.
 *
 * <pre>
 * tester.getRequest().getPostParameters().setParameterValue(editor.getInputName(), "&lt;p&gt;Saved&lt;/p&gt;");
 * ClientEventsTester.fire(tester, editor, RichTextEditorEvents.SAVE);
 * </pre>
 */
public final class ClientEventsTester
{
	private ClientEventsTester()
	{
	}

	/**
	 * Sends the first event of the name going to the server, without values.
	 *
	 * @param tester
	 *            the tester, whose request holds what the test set, the input of the fields for
	 *            an event submitting the form for example
	 * @param component
	 *            the component listening to the event, with a {@link ClientEventsBehavior}
	 * @param eventId
	 *            the name of the event
	 */
	public static void fire(BaseWicketTester tester, Component component, String eventId)
	{
		fire(tester, component, eventId, new JSONObject());
	}

	/**
	 * Sends the first event of the name going to the server.
	 *
	 * @param tester
	 *            the tester, whose request holds what the test set, the input of the fields for
	 *            an event submitting the form for example
	 * @param component
	 *            the component listening to the event, with a {@link ClientEventsBehavior}
	 * @param eventId
	 *            the name of the event
	 * @param values
	 *            what the function of the event sends
	 * @throws IllegalArgumentException
	 *             when the component has no event of the name going to the server
	 */
	public static void fire(BaseWicketTester tester, Component component, String eventId,
		JSONObject values)
	{
		Args.notNull(tester, "tester");
		Args.notNull(component, "component");
		Args.notEmpty(eventId, "eventId");
		ClientEventsBehavior behavior = component.getBehaviors(ClientEventsBehavior.class)
			.stream().findFirst().orElseThrow(() -> new IllegalArgumentException(
				component + " has no ClientEventsBehavior"));
		List<IClientEvent> events = behavior.getEvents();
		for (int index = 0; index < events.size(); index++)
		{
			IClientEvent event = events.get(index);
			if (event.getId().equals(eventId) &&
				(event instanceof IAjaxEvent || event instanceof IAjaxSubmitEvent))
			{
				tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER, eventId);
				tester.getRequest().setParameter(ClientEventsBehavior.INDEX_PARAMETER,
					String.valueOf(index));
				tester.getRequest().setParameter(ClientEventsBehavior.VALUES_PARAMETER,
					(values == null ? new JSONObject() : values).toString());
				tester.executeBehavior(behavior.getAjaxBehavior(event));
				return;
			}
		}
		throw new IllegalArgumentException(component + " has no event " + eventId +
			" going to the server");
	}
}
