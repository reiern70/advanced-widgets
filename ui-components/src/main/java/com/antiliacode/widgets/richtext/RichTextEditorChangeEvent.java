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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.AjaxEvent;

/**
 * Hears on the server of the text of a {@link RichTextEditor} or a
 * {@link com.antiliacode.widgets.ckeditor.CKEditor} with an event, the text changed
 * ({@link RichTextEditorEvents#CHANGE}, by default) or the focus lost
 * ({@link RichTextEditorEvents#BLUR}) for example, as the text before and the text now:
 * {@link #onChange(String, String, AjaxRequestTarget)}.
 * <p>
 * The text comes as the text area posts it, which Wicket sends with the event, not in values of
 * the event: the editor takes it as its input, cleans and validates it as when its form is sent,
 * without the rest of the form, and its model gets it. The text before is what the model had; the
 * text now, what it has, cleaned. When the text is not valid, the model keeps its text, and
 * {@link #onError(AjaxRequestTarget)} is called instead. A read-only editor hears of nothing.
 *
 * <pre>
 * editor.addEvent(RichTextEditorChangeEvent.of(editor, RichTextEditorEvents.BLUR,
 * 	(previousText, currentText, target) -&gt; {
 * 		if (!Objects.equals(previousText, currentText))
 * 		{
 * 			history.add(previousText);
 * 		}
 * 	}));
 * </pre>
 *
 * @since 11.0.0
 */
public abstract class RichTextEditorChangeEvent extends AjaxEvent
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(RichTextEditorChangeEvent.class);

	/**
	 * What the server does with the text: {@link RichTextEditorChangeEvent#onChange(String,
	 * String, AjaxRequestTarget)}, as a lambda.
	 */
	@FunctionalInterface
	public interface IChangeListener extends IClusterable
	{
		/**
		 * @param previousText
		 *            the text the model had
		 * @param currentText
		 *            the text the model has now, cleaned
		 * @param target
		 *            the Ajax request target
		 */
		void onChange(String previousText, String currentText, AjaxRequestTarget target);
	}

	private final AbstractRichTextEditor<?> editor;

	/**
	 * Hears of the text as it changed, once the editor lost the focus
	 * ({@link RichTextEditorEvents#CHANGE}).
	 *
	 * @param editor
	 *            the editor, which listens to the event
	 */
	protected RichTextEditorChangeEvent(AbstractRichTextEditor<?> editor)
	{
		this(editor, RichTextEditorEvents.CHANGE);
	}

	/**
	 * @param editor
	 *            the editor, which listens to the event
	 * @param eventId
	 *            the event the text comes with, {@link RichTextEditorEvents#BLUR} for example
	 */
	protected RichTextEditorChangeEvent(AbstractRichTextEditor<?> editor, String eventId)
	{
		super(eventId);
		this.editor = Args.notNull(editor, "editor");
	}

	/**
	 * @param editor
	 *            the editor, which listens to the event
	 * @param eventId
	 *            the event the text comes with, {@link RichTextEditorEvents#CHANGE} or
	 *            {@link RichTextEditorEvents#BLUR} for example
	 * @param listener
	 *            what the server does with the text before and the text now
	 * @return the event
	 */
	public static RichTextEditorChangeEvent of(AbstractRichTextEditor<?> editor, String eventId,
		IChangeListener listener)
	{
		Args.notNull(listener, "listener");
		return new RichTextEditorChangeEvent(editor, eventId)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onChange(String previousText, String currentText,
				AjaxRequestTarget target)
			{
				listener.onChange(previousText, currentText, target);
			}
		};
	}

	/**
	 * @return the editor, which listens to the event
	 */
	public AbstractRichTextEditor<?> getEditor()
	{
		return editor;
	}

	/**
	 * Takes the text area posted as the input of the editor, as an Ajax behavior
	 * updating a form component does, then calls {@link #onChange(String, String,
	 * AjaxRequestTarget)} or {@link #onError(AjaxRequestTarget)}.
	 */
	@Override
	public final void onEvent(String eventId, JSONObject values, AjaxRequestTarget target)
	{
		if (editor.isReadOnly() || !editor.isEnabledInHierarchy())
		{
			return;
		}
		if (!editor.getRequest().getRequestParameters().getParameterNames().contains(
			editor.getInputName()))
		{
			// without the text, the model would lose it
			LOG.debug("The event {} came without the text of {}", eventId, editor);
			return;
		}
		String previousText = editor.getModelObject();
		editor.inputChanged();
		editor.validate();
		if (editor.isValid())
		{
			editor.valid();
			editor.updateModel();
			onChange(previousText, editor.getModelObject(), target);
		}
		else
		{
			editor.invalid();
			onError(target);
		}
		editor.updateAutoLabels(target, false);
	}

	/**
	 * Called once the model got the text the browser sent.
	 *
	 * @param previousText
	 *            the text the model had
	 * @param currentText
	 *            the text the model has now, cleaned
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onChange(String previousText, String currentText,
		AjaxRequestTarget target);

	/**
	 * Called when the text the browser sent is not valid, required but empty for example; the
	 * editor has the error, and the model keeps its text. Does nothing by default.
	 *
	 * @param target
	 *            the Ajax request target
	 */
	protected void onError(AjaxRequestTarget target)
	{
	}
}
