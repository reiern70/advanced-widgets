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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.core.request.handler.ComponentNotFoundException;
import org.apache.wicket.request.Url;
import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;

import com.antiliacode.widgets.event.ClientEventsBehaviorTest.TestPage;

class RemovedComponentEventListenerTest extends WicketTestCase
{
	private final List<String> heard = new ArrayList<>();

	private TestPage page;

	private Url url;

	/**
	 * Renders a page whose box listens to focusin, then takes the box away on the server, leaving
	 * it in the browser: the URL of the event's requests.
	 */
	private void removeTheBoxOfARenderedPage()
	{
		ClientEventsBehavior events = new ClientEventsBehavior()
			.add(AjaxEvent.of("focusin", (values, target) -> heard.add("focusin")));
		page = new TestPage(events);
		tester.startPage(page);
		AbstractDefaultAjaxBehavior behavior = events.getAjaxBehavior(
			events.getEvents("focusin").get(0));
		url = Url.parse(behavior.getCallbackUrl().toString());
		page.form.remove(page.box);
	}

	@Test
	void isAddedToEachApplicationByTheLibrary()
	{
		assertTrue(tester.getApplication().getRequestCycleListeners().iterator().hasNext());
		boolean added = false;
		for (var listener : tester.getApplication().getRequestCycleListeners())
		{
			added |= listener instanceof RemovedComponentEventListener;
		}
		assertTrue(added);
	}

	@Test
	void anEventOfAComponentGoneFromThePageIsAnsweredWithNothing()
	{
		removeTheBoxOfARenderedPage();

		tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER, "focusin");
		tester.executeAjaxUrl(url);

		assertEquals(List.of(), heard);
		assertTrue(tester.getLastResponseAsString().endsWith("<ajax-response></ajax-response>"),
			tester.getLastResponseAsString());
		assertTrue(tester.getLastResponse().getContentType().startsWith("text/xml"),
			tester.getLastResponse().getContentType());
	}

	@Test
	void anotherRequestOfAComponentGoneFailsAsBefore()
	{
		removeTheBoxOfARenderedPage();

		// not of a client event
		assertThrows(ComponentNotFoundException.class, () -> tester.executeAjaxUrl(url));
	}

	@Test
	void anEventNotViaAjaxFailsAsBefore()
	{
		removeTheBoxOfARenderedPage();

		tester.getRequest().setParameter(ClientEventsBehavior.EVENT_PARAMETER, "focusin");
		assertThrows(ComponentNotFoundException.class, () -> tester.executeUrl(url.toString()));
	}
}
