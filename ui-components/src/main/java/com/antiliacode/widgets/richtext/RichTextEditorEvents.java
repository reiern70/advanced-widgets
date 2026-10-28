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

import com.antiliacode.widgets.resizable.ResizableSize;

/**
 * The names of the events a {@link RichTextEditor} or a
 * {@link com.antiliacode.widgets.ckeditor.CKEditor} fires on its text area: those a text area fires
 * itself, {@link #INPUT} and {@link #CHANGE}, and those of the editor, whose {@code detail} holds
 * the editor and what the event tells. An editor listens to those it is
 * {@link AbstractRichTextEditor#addEvent(com.antiliacode.widgets.event.IClientEvent) given}, and
 * to those of any other name, which the scripts of its features fire for example.
 *
 * <pre>
 * editor.addEvent(new AjaxSubmitEvent(RichTextEditorEvents.SAVE) { ... });
 * editor.addEvent(AjaxEvent.of(RichTextEditorEvents.FOCUS, (eventId, values, target) -&gt; ...));
 * editor.addEvent(RichTextEditorResizeEvent.of((width, height, target) -&gt; ...));
 * </pre>
 *
 * @since 11.0.0
 */
public final class RichTextEditorEvents
{
	/** The user changed the text: each change, as a text area tells. */
	public static final String INPUT = "input";

	/**
	 * The text changed, once the editor lost the focus, as a text area tells.
	 * {@link RichTextEditorChangeEvent} hears of the text before and the text now.
	 */
	public static final String CHANGE = "change";

	/** The editor got the focus. */
	public static final String FOCUS = "antilia-rich-text-editor:focus";

	/**
	 * The editor lost the focus. {@link RichTextEditorChangeEvent} hears of the text before and
	 * the text now.
	 */
	public static final String BLUR = "antilia-rich-text-editor:blur";

	/**
	 * The user moved the cursor or selected text; {@code detail.selection} has {@code anchor} and
	 * {@code head}.
	 */
	public static final String SELECT = "antilia-rich-text-editor:select";

	/** The user pressed Ctrl+S, or Cmd+S, in the editor. */
	public static final String SAVE = "antilia-rich-text-editor:save";

	/**
	 * The user resized the editor, once they let go; {@code detail.size} has the {@code width}
	 * and the {@code height}, in pixels, of the dimensions the editor resizes in.
	 * {@link RichTextEditorResizeEvent} hears of both on the server, as numbers.
	 */
	public static final String RESIZE = "antilia-rich-text-editor:resize";

	/**
	 * The text came to have changes not saved, or no longer has; {@code detail.modified} tells
	 * which.
	 */
	public static final String MODIFIED = "antilia-rich-text-editor:modified";

	/**
	 * How long an event coming often, {@link #INPUT} or {@link #SELECT}, may wait for the user to
	 * pause, in milliseconds: see {@link com.antiliacode.widgets.event.AbstractClientEvent#setDelay(int)}.
	 */
	public static final int TYPING_DELAY = 500;

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

	private RichTextEditorEvents()
	{
	}
}
