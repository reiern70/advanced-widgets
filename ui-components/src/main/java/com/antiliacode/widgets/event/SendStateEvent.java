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

import java.time.Duration;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.github.openjson.JSONObject;

/**
 * An event sending the server the state of an editor, and its text when the user changed it, by
 * {@link #SEND_STATE_HANDLER}: only when they changed since the editor last sent them. The editors
 * listen to these by default: as the user changes the text, moves the cursor, scrolls and
 * resizes, a moment after the user paused ({@link #DEFAULT_DELAY}), and at once as the editor
 * loses the focus and, in a beacon, as the page goes. The editor takes what came before
 * {@link #onEvent(JSONObject, AjaxRequestTarget)}, which does nothing more by default.
 * <p>
 * A page sends the state at other times by giving the editor others, of other names, changes the
 * delay of one by giving it anew, and takes one away, the editor then sending nothing on it:
 *
 * <pre>
 * // the state goes as the editor loses the focus, not as the user types
 * editor.removeEvents(RichTextEditorEvents.INPUT);
 * // a second after the user resized the editor
 * editor.setEvent(new SendStateEvent(RichTextEditorEvents.RESIZE).setDelay(Duration.ofSeconds(1)));
 * // and as the user saves
 * editor.addEvent(new SendStateEvent(RichTextEditorEvents.SAVE));
 * // the page veil leaves the request on blur alone
 * editor.setEvent(new SendStateEvent(RichTextEditorEvents.BLUR).setExtraParameter("_nb", "true"));
 * </pre>
 *
 * Besides, the page's other Ajax requests carry the state the editor did not send yet: the user
 * scrolled it without focusing it, and the request of a check box redraws its form, for example.
 *
 * @since 11.0.0
 */
public class SendStateEvent extends AbstractAjaxRoundTripEvent<SendStateEvent>
	implements IAjaxEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * How long the default events waiting for the user to pause wait: those of the changes of the
	 * text, the selection, the scrolling and the size.
	 */
	public static final Duration DEFAULT_DELAY = Duration.ofMillis(400);

	/**
	 * The parameter of the requests of an editor's events holding its state, as JSON: what the
	 * editor gives each of its events going to the server, and the page's other Ajax requests
	 * when the editor did not send it yet, in
	 * {@link ClientEventsBehavior#PENDING_PARAMETER_PREFIX the parameter of the editor}. Scripts
	 * find the name in {@code Wicket.ClientEvents.parameters.state}.
	 * <p>
	 * Before 10.11.0-RC7 the editors sent the state as {@code state}, by a request of their own: a
	 * script telling those requests by it no longer finds them. Mark the requests of the events
	 * with {@link ClientEventsBehavior#updateAjaxAttributes} or an event's
	 * {@link #setExtraParameter(String, String) extra parameters} instead.
	 */
	public static final String STATE_PARAMETER = "antilia-state";

	/**
	 * The parameter of the requests of an editor's events telling whether the text changed since
	 * it was last sent: the editor then takes it from the text area, which Wicket's Ajax sends.
	 * Scripts find the name in {@code Wicket.ClientEvents.parameters.textChanged}.
	 */
	public static final String TEXT_CHANGED_PARAMETER = "antilia-text-changed";

	/**
	 * @param eventName
	 *            the name of the event in the browser
	 */
	public SendStateEvent(String eventName)
	{
		super(eventName, SEND_STATE_HANDLER, null);
	}

	/**
	 * @param eventName
	 *            the name of the event
	 */
	public SendStateEvent(IClientEventName eventName)
	{
		this(nameOf(eventName));
	}

	/**
	 * Does nothing: the editor took the state and the text already.
	 */
	@Override
	public void onEvent(JSONObject values, AjaxRequestTarget target)
	{
	}
}
