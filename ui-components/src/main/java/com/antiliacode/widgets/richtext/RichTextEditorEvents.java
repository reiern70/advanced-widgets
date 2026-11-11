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
package com.antiliacode.widgets.richtext;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.IClientEventName;
import com.antiliacode.widgets.resizable.ResizableSize;

/**
 * The events a {@link RichTextEditor} or a {@link com.antiliacode.widgets.ckeditor.CKEditor} fires
 * on its text area, by their names in the browser ({@link #getEventName()}): those a text area
 * fires itself, {@link #INPUT} and {@link #CHANGE}, those of the editor, whose {@code detail}
 * holds the editor and what the event tells, and those of the built-in types of events,
 * {@link #SELECTED_TEXT} and {@link #PASTE_CLEANED}. An editor listens to those it is
 * {@link AbstractRichTextEditor#addEvent(com.antiliacode.widgets.event.IClientEvent) given}, and
 * to those of any other name, which the scripts of its features fire for example. By default it
 * sends its state on six of them, {@link #INPUT}, {@link #SELECT}, {@link #SCROLL},
 * {@link #RESIZE}, {@link #BLUR} and {@link #LEAVE}: see
 * {@link com.antiliacode.widgets.event.SendStateEvent}.
 * <p>
 * A type of events of a page's own, or of a library's, fires on both editors as their own do: a
 * script registers it in the browser, by a name {@code <owner>:<event>}, with the events of the
 * editor it fires from, or none for one fired by hand, what it keeps for each editor and what each
 * event tells:
 *
 * <pre>
 * Wicket.RichTextEvents.register('antilia-text:headings', {
 *     from: [Wicket.RichTextEvents.events.text],
 *     init: function (editor) { return { headings: null }; },
 *     fire: function (editor, state, cause) {
 *         ... // what the event tells, or undefined for nothing
 *     }
 * });
 * Wicket.RichTextEvents.trigger(editorMarkupId, 'antilia-text:headings'); // by hand
 * </pre>
 *
 * An {@link com.antiliacode.widgets.event.IClientEvent} of that name renders the script
 * ({@link com.antiliacode.widgets.event.IClientEvent#renderHead(org.apache.wicket.Component,
 * org.apache.wicket.markup.head.IHeaderResponse)}) and reads what the event tells, as
 * {@link RichTextEditorSelectedTextEvent} does for {@link #SELECTED_TEXT}. A name registers once,
 * the owner {@code antilia-rich-text-editor} is the editors' own, and a type fires from the
 * events of the editors alone, not from other types.
 *
 * <pre>
 * editor.addEvent(new AjaxSubmitEvent(RichTextEditorEvents.SAVE) { ... });
 * editor.addEvent(AjaxEvent.of(RichTextEditorEvents.FOCUS, (values, target) -&gt; ...));
 * editor.addEvent(RichTextEditorResizeEvent.of((width, height, target) -&gt; ...));
 * </pre>
 *
 * @since 11.0.0
 */
public enum RichTextEditorEvents implements IClientEventName
{
	/** The user changed the text: each change, as a text area tells. */
	INPUT("input"),

	/**
	 * The text changed, once the editor lost the focus, as a text area tells.
	 * {@link RichTextEditorChangeEvent} hears of the text before and the text now.
	 */
	CHANGE("change"),

	/** The editor got the focus. */
	FOCUS("antilia-rich-text-editor:focus"),

	/**
	 * The editor lost the focus. {@link RichTextEditorChangeEvent} hears of the text before and
	 * the text now.
	 */
	BLUR("antilia-rich-text-editor:blur"),

	/**
	 * The user moved the cursor or selected text; {@code detail.selection} has {@code anchor} and
	 * {@code head}.
	 */
	SELECT("antilia-rich-text-editor:select"),

	/**
	 * The user scrolled the text; {@code detail.scroll} has {@code top} and {@code left}, in
	 * pixels.
	 */
	SCROLL("antilia-rich-text-editor:scroll"),

	/** The user pressed Ctrl+S, or Cmd+S, in the editor. */
	SAVE("antilia-rich-text-editor:save"),

	/**
	 * The user resized the editor, once they let go; {@code detail.size} has the {@code width}
	 * and the {@code height}, in pixels, of the dimensions the editor resizes in.
	 * {@link RichTextEditorResizeEvent} hears of both on the server, as numbers.
	 */
	RESIZE("antilia-rich-text-editor:resize"),

	/**
	 * The text came to have changes not saved, or no longer has; {@code detail.modified} tells
	 * which.
	 */
	MODIFIED("antilia-rich-text-editor:modified"),

	/**
	 * The page is going; {@code detail.leaving} is {@code true}. The events going to the server
	 * go in a beacon, which the browser sends even then; what the server answers is lost.
	 */
	LEAVE("antilia-rich-text-editor:leave"),

	/**
	 * A tab of {@link com.antiliacode.widgets.tabs.TabbedPanel} or a window of
	 * {@link com.antiliacode.widgets.desktop.Desktop} closed anyway put the text last saved back.
	 */
	DISCARD("antilia-rich-text-editor:discard"),

	/**
	 * The text the user selected changed, a built-in type of events; {@code detail.text} has it,
	 * as plain text, a line for each block, {@code ''} once nothing is selected. Fires once for
	 * each text, not as the cursor moves with nothing selected.
	 * {@link RichTextEditorSelectedTextEvent} hears of it on the server.
	 */
	SELECTED_TEXT("antilia-rich-text-editor:selected-text"),

	/**
	 * The cleaning in the browser left part of a paste out, a built-in type of events:
	 * {@code detail.elements} and {@code detail.attributes} have the names of what it left out,
	 * each with the number of times, never their content: a script, an event handler, a link or an
	 * image to an address the editor does not take, what the editor has no feature for. Not when a
	 * paste lost nothing. {@link RichTextEditorPasteCleanedEvent} hears of it on the server.
	 *
	 * @since 10.11.0
	 */
	PASTE_CLEANED("antilia-rich-text-editor:paste-cleaned");

	/**
	 * How long an event coming often, {@link #INPUT}, {@link #SELECT} or {@link #SELECTED_TEXT},
	 * may wait for the user to pause, in milliseconds: see
	 * {@link com.antiliacode.widgets.event.AbstractDelayedClientEvent#setDelay(int)}.
	 */
	public static final int TYPING_DELAY = 500;

	private final String eventName;

	RichTextEditorEvents(String eventName)
	{
		this.eventName = eventName;
	}

	@Override
	public String getEventName()
	{
		return eventName;
	}

	/**
	 * @param values
	 *            what the browser sent with {@link #RESIZE}, by default the data of its
	 *            {@code detail}
	 * @return the size the user resized the editor to, {@link ResizableSize#NONE} for none
	 */
	public static ResizableSize sizeOf(JSONObject values)
	{
		return ResizableSize.of(values == null ? null : values.optJSONObject("size"));
	}
}
