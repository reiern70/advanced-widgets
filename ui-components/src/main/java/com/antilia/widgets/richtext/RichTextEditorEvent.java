/*
 * Copyright 2026 Ernesto Reinaldo Barreiro (reiern70, reiern70@gmail.com)
 *
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
package com.antilia.widgets.richtext;

/**
 * What a {@link RichTextEditor} tells the page of, by the events the browser fires on its text
 * area: those a text area fires itself, {@code input} and {@code change}, and those of the editor.
 * A {@link RichTextEditorEventBehavior} hears of one on the server, as any Ajax behavior listening
 * to {@link #getEventName() its name} does.
 *
 * @since 11.0.0
 */
public enum RichTextEditorEvent
{
	/** The user changed the text: each change, as a text area tells. */
	INPUT("input"),

	/** The text changed, once the editor lost the focus, as a text area tells. */
	CHANGE("change"),

	/** The editor got the focus: its text, its toolbar or a form of it. */
	FOCUS("antilia-rich-text-editor:focus"),

	/** The editor lost the focus. */
	BLUR("antilia-rich-text-editor:blur"),

	/** The user moved the cursor or selected text. */
	SELECT("antilia-rich-text-editor:select"),

	/** The user pressed Ctrl+S, or Cmd+S, in the editor. */
	SAVE("antilia-rich-text-editor:save"),

	/** The user resized the editor. */
	RESIZE("antilia-rich-text-editor:resize"),

	/** The text came to have changes not saved, or no longer has. */
	MODIFIED("antilia-rich-text-editor:modified");

	private final String eventName;

	RichTextEditorEvent(String eventName)
	{
		this.eventName = eventName;
	}

	/**
	 * @return the name of the event in the browser
	 */
	public String getEventName()
	{
		return eventName;
	}

	/**
	 * @return whether the event comes often, as the user types or moves the cursor: an Ajax
	 *         behavior then waits until the user paused
	 */
	public boolean isFrequent()
	{
		return this == INPUT || this == SELECT;
	}
}
