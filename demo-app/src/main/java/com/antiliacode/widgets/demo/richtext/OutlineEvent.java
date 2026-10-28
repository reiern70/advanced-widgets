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
package com.antiliacode.widgets.demo.richtext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.AjaxEvent;

/**
 * A type of events of the demo's own, which the rich text editors have not: the outline of the
 * text, its headings, which the browser sends each time they changed. Its script,
 * {@code outline-event.js}, registers it in the browser for both editors, Tiptap's and
 * CKEditor's, firing from their event {@code text}; on the server, the headings come as a list of
 * texts.
 *
 * <pre>
 * editor.addEvent(OutlineEvent.of((headings, target) -&gt; ...));
 * </pre>
 *
 * The browser sends the headings: the server takes no more than {@link #MAX_HEADINGS}, of
 * {@link #MAX_LENGTH} characters at most, and escapes them where it shows them.
 */
public abstract class OutlineEvent extends AjaxEvent
{
	private static final long serialVersionUID = 1L;

	/** The name of the events, which the script registers. */
	public static final String NAME = "rich-text-demo:outline";

	/** The most headings the server takes. */
	public static final int MAX_HEADINGS = 50;

	/** The most characters of a heading the server takes. */
	public static final int MAX_LENGTH = 200;

	/** The script registering the type in the browser. */
	private static final ResourceReference SCRIPT = new JavaScriptResourceReference(
		OutlineEvent.class, "outline-event.js");

	/**
	 * What the server does with the headings: {@link OutlineEvent#onOutline(List,
	 * AjaxRequestTarget)}, as a lambda.
	 */
	@FunctionalInterface
	public interface IOutlineListener extends IClusterable
	{
		/**
		 * @param headings
		 *            the texts of the headings, in their order; none for a text without
		 * @param target
		 *            the Ajax request target
		 */
		void onOutline(List<String> headings, AjaxRequestTarget target);
	}

	/**
	 * Constructor.
	 */
	protected OutlineEvent()
	{
		super(NAME);
	}

	/**
	 * @param listener
	 *            what the server does with the headings
	 * @return the event
	 */
	public static OutlineEvent of(IOutlineListener listener)
	{
		Args.notNull(listener, "listener");
		return new OutlineEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onOutline(List<String> headings, AjaxRequestTarget target)
			{
				listener.onOutline(headings, target);
			}
		};
	}

	/**
	 * Renders the script registering the type, after the editor's, which defines
	 * {@code Wicket.RichTextEvents}.
	 */
	@Override
	public void renderHead(Component component, IHeaderResponse response)
	{
		response.render(JavaScriptHeaderItem.forReference(SCRIPT));
	}

	/**
	 * Reads the headings the browser sent, and calls {@link #onOutline(List, AjaxRequestTarget)}.
	 */
	@Override
	public final void onEvent(String eventId, JSONObject values, AjaxRequestTarget target)
	{
		onOutline(headingsOf(values), target);
	}

	/**
	 * @return the texts of the headings the browser sent, what is not a text left out, at most
	 *         {@link #MAX_HEADINGS} of {@link #MAX_LENGTH} characters at most
	 */
	static List<String> headingsOf(JSONObject values)
	{
		JSONArray sent = values.optJSONArray("headings");
		List<String> headings = new ArrayList<>();
		for (int index = 0; sent != null && index < sent.length() &&
			headings.size() < MAX_HEADINGS; index++)
		{
			if (sent.opt(index) instanceof String heading && !heading.isBlank())
			{
				String text = heading.strip();
				headings.add(text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text);
			}
		}
		return Collections.unmodifiableList(headings);
	}

	/**
	 * Called with the headings of the text, each time they changed.
	 *
	 * @param headings
	 *            the texts of the headings, in their order; none for a text without
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onOutline(List<String> headings, AjaxRequestTarget target);
}
