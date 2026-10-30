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
 * Hears on the server of what the cleaning in the browser left out of a paste into a
 * {@link RichTextEditor} or a {@link com.antiliacode.widgets.ckeditor.CKEditor}
 * ({@link RichTextEditorEvents#PASTE_CLEANED}, a built-in type of events of the editors): the
 * elements and the attributes, by name and number, a script, an event handler, a link or an image
 * to an address the editor does not take, what the editor has no feature for. Not when a paste
 * lost nothing.
 *
 * <pre>
 * editor.addEvent(RichTextEditorPasteCleanedEvent.of((report, target) -&gt;
 *     notifications.show(Notification.info("The paste lost: " + report))));
 * </pre>
 *
 * The browser sends the report: it says what the user pasted, not what the server keeps, which
 * cleans the text anew when the form comes ({@link RichTextCleaning}). Names that are no names of
 * HTML are left out, and so is the content of what was left out: the browser never sends it.
 *
 * @since 10.11.0
 */
public abstract class RichTextEditorPasteCleanedEvent extends AjaxEvent
{
	private static final long serialVersionUID = 1L;

	/**
	 * What the server does with what a paste lost:
	 * {@link RichTextEditorPasteCleanedEvent#onPasteCleaned(RichTextCleaningReport,
	 * AjaxRequestTarget)}, as a lambda.
	 */
	@FunctionalInterface
	public interface IPasteCleanedListener extends IClusterable
	{
		/**
		 * @param report
		 *            what the cleaning left out of the paste
		 * @param target
		 *            the Ajax request target
		 */
		void onPasteCleaned(RichTextCleaningReport report, AjaxRequestTarget target);
	}

	/**
	 * Constructor.
	 */
	protected RichTextEditorPasteCleanedEvent()
	{
		super(RichTextEditorEvents.PASTE_CLEANED);
	}

	/**
	 * @param listener
	 *            what the server does with what a paste lost
	 * @return the event
	 */
	public static RichTextEditorPasteCleanedEvent of(IPasteCleanedListener listener)
	{
		Args.notNull(listener, "listener");
		return new RichTextEditorPasteCleanedEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onPasteCleaned(RichTextCleaningReport report, AjaxRequestTarget target)
			{
				listener.onPasteCleaned(report, target);
			}
		};
	}

	/**
	 * Reads what the browser sent ({@link RichTextCleaningReport#fromJson(JSONObject)}) and calls
	 * {@link #onPasteCleaned(RichTextCleaningReport, AjaxRequestTarget)}, unless it tells of
	 * nothing.
	 */
	@Override
	public final void onEvent(String eventId, JSONObject values, AjaxRequestTarget target)
	{
		RichTextCleaningReport report = RichTextCleaningReport.fromJson(values);
		if (!report.isEmpty())
		{
			onPasteCleaned(report, target);
		}
	}

	/**
	 * Called with what the cleaning in the browser left out of a paste.
	 *
	 * @param report
	 *            the elements and the attributes left out, by name and number
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onPasteCleaned(RichTextCleaningReport report,
		AjaxRequestTarget target);
}
