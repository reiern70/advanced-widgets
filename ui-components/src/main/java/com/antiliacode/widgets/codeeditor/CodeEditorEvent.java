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
package com.antiliacode.widgets.codeeditor;

import com.antiliacode.widgets.event.IClientEventName;

/**
 * What a {@link CodeEditor} tells the page of, by the events the browser fires on its text area,
 * by their names ({@link #getEventName()}): those a text area fires itself, {@code input} and
 * {@code change}, and those of the editor, whose {@code detail} holds the editor and what the event
 * tells. The editor listens to those it is
 * {@link CodeEditor#addEvent(com.antiliacode.widgets.event.IClientEvent) given}: an
 * {@link com.antiliacode.widgets.event.AjaxUpdateEvent} hears of one on the server once the model
 * took the text, valid. By default the editor sends its state on six of them, {@link #INPUT},
 * {@link #SELECT}, {@link #SCROLL}, {@link #RESIZE}, {@link #BLUR} and {@link #LEAVE}: see
 * {@link com.antiliacode.widgets.event.SendStateEvent}.
 *
 * @since 11.0.0
 */
public enum CodeEditorEvent implements IClientEventName
{
	/** The user changed the text: each change, as a text area tells. */
	INPUT("input"),

	/** The text changed, once the editor lost the focus, as a text area tells. */
	CHANGE("change"),

	/** The editor got the focus. */
	FOCUS("antilia-code-editor:focus"),

	/** The editor lost the focus. */
	BLUR("antilia-code-editor:blur"),

	/** The user moved the cursor or selected text. */
	SELECT("antilia-code-editor:select"),

	/** The user scrolled the text; {@code detail.scroll} has {@code top} and {@code left}. */
	SCROLL("antilia-code-editor:scroll"),

	/**
	 * The user pressed Ctrl+S, or Cmd+S, in the editor, or left it with changes not saved or errors
	 * shown, when it {@link CodeEditor#setSaveOnBlur(boolean) saves on blur}.
	 */
	SAVE("antilia-code-editor:save"),

	/** The user resized the editor. */
	RESIZE("antilia-code-editor:resize"),

	/** The text came to have changes not saved, or no longer has. */
	MODIFIED("antilia-code-editor:modified"),

	/**
	 * The page is going; {@code detail.leaving} is {@code true}. The events going to the server
	 * go in a beacon, which the browser sends even then; what the server answers is lost.
	 */
	LEAVE("antilia-code-editor:leave"),

	/**
	 * A tab of {@link com.antiliacode.widgets.tabs.TabbedPanel} or a window of
	 * {@link com.antiliacode.widgets.desktop.Desktop} closed anyway put the text last saved back.
	 */
	DISCARD("antilia-code-editor:discard");

	/**
	 * How long an event coming often, {@link #INPUT} or {@link #SELECT}, may wait for the user to
	 * pause, in milliseconds: see
	 * {@link com.antiliacode.widgets.event.AbstractDelayedClientEvent#setDelay(int)}.
	 */
	public static final int TYPING_DELAY = 500;

	private final String eventName;

	CodeEditorEvent(String eventName)
	{
		this.eventName = eventName;
	}

	/**
	 * @return the name of the event in the browser
	 */
	@Override
	public String getEventName()
	{
		return eventName;
	}
}
