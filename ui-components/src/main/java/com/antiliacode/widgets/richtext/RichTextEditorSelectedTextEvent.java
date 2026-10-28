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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.AjaxEvent;

/**
 * Hears on the server of the text the user selected in a {@link RichTextEditor} or a
 * {@link com.antiliacode.widgets.ckeditor.CKEditor}, as plain text, a line for each block, each
 * time it changed ({@link RichTextEditorEvents#SELECTED_TEXT}, a built-in type of events of the
 * editors): an empty text once nothing is selected. Not as the cursor moves with nothing
 * selected; a delay lets the user finish selecting first.
 *
 * <pre>
 * editor.addEvent(RichTextEditorSelectedTextEvent.of((selectedText, target) -&gt; ...)
 * 	.setDelay(RichTextEditorEvents.TYPING_DELAY));
 * </pre>
 *
 * The browser sends the text: it is the user's, which the server escapes where it shows it, as
 * Wicket's labels do, and does not take for a part of the editor's text, which comes with the form.
 *
 * @since 11.0.0
 */
public abstract class RichTextEditorSelectedTextEvent extends AjaxEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * What the server does with the text selected: {@link RichTextEditorSelectedTextEvent#onSelect(
	 * String, AjaxRequestTarget)}, as a lambda.
	 */
	@FunctionalInterface
	public interface ISelectedTextListener extends IClusterable
	{
		/**
		 * @param selectedText
		 *            the text selected, as plain text; empty for none
		 * @param target
		 *            the Ajax request target
		 */
		void onSelect(String selectedText, AjaxRequestTarget target);
	}

	/**
	 * Constructor.
	 */
	protected RichTextEditorSelectedTextEvent()
	{
		super(RichTextEditorEvents.SELECTED_TEXT);
	}

	/**
	 * @param listener
	 *            what the server does with the text selected
	 * @return the event
	 */
	public static RichTextEditorSelectedTextEvent of(ISelectedTextListener listener)
	{
		Args.notNull(listener, "listener");
		return new RichTextEditorSelectedTextEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSelect(String selectedText, AjaxRequestTarget target)
			{
				listener.onSelect(selectedText, target);
			}
		};
	}

	/**
	 * Reads the text the browser sent, and calls {@link #onSelect(String, AjaxRequestTarget)}: an
	 * empty text for none, and for what is not a text.
	 */
	@Override
	public final void onEvent(String eventId, JSONObject values, AjaxRequestTarget target)
	{
		Object text = values.opt("text");
		onSelect(text instanceof String selected ? selected : "", target);
	}

	/**
	 * Called with the text the user selected.
	 *
	 * @param selectedText
	 *            the text selected, as plain text, a line for each block; empty for none
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onSelect(String selectedText, AjaxRequestTarget target);
}
