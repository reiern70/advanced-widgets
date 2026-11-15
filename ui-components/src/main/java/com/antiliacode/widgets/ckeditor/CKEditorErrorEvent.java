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
package com.antiliacode.widgets.ckeditor;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.io.IClusterable;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.openjson.JSONObject;

import com.antiliacode.widgets.event.AjaxEvent;

/**
 * Hears on the server of {@link CKEditor} running into an error in the browser
 * ({@link CKEditorEvents#ERROR}): an error escaping CKEditor after it started, which CKEditor 49
 * tells by its {@code onEditorError}, or CKEditor not starting. It logs a warning with what the
 * browser told, then calls {@link #onError(CKEditorFailure, AjaxRequestTarget)}, which does
 * nothing by default.
 *
 * <pre>
 * // the errors in the log of the server
 * editor.addEvent(new CKEditorErrorEvent());
 * // and in the page
 * editor.addEvent(CKEditorErrorEvent.of((failure, target) -&gt;
 *     notifications.show(Notification.warning("The editor failed: " + failure.message()))));
 * </pre>
 *
 * What the editor does in the browser, starting CKEditor again or not, is its
 * {@link CKEditor#setRecovery(CKEditorRecovery) recovery}. The browser sends what the error said:
 * see {@link CKEditorFailure}.
 *
 * @since 11.0.0
 */
public class CKEditorErrorEvent extends AjaxEvent
{
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(CKEditorErrorEvent.class);

	/**
	 * What the server does with an error of CKEditor:
	 * {@link CKEditorErrorEvent#onError(CKEditorFailure, AjaxRequestTarget)}, as a lambda.
	 */
	@FunctionalInterface
	public interface IErrorListener extends IClusterable
	{
		/**
		 * @param failure
		 *            what the browser told of the error
		 * @param target
		 *            the Ajax request target
		 */
		void onError(CKEditorFailure failure, AjaxRequestTarget target);
	}

	/**
	 * An event logging the errors alone.
	 */
	public CKEditorErrorEvent()
	{
		super(CKEditorEvents.ERROR);
	}

	/**
	 * @param listener
	 *            what the server does with an error of CKEditor, after logging it
	 * @return the event
	 */
	public static CKEditorErrorEvent of(IErrorListener listener)
	{
		Args.notNull(listener, "listener");
		return new CKEditorErrorEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onError(CKEditorFailure failure, AjaxRequestTarget target)
			{
				listener.onError(failure, target);
			}
		};
	}

	/**
	 * Reads what the browser sent ({@link CKEditorFailure#fromJson(JSONObject)}), logs it as a
	 * warning, with the page, and calls {@link #onError(CKEditorFailure, AjaxRequestTarget)}.
	 */
	@Override
	public final void onEvent(JSONObject values, AjaxRequestTarget target)
	{
		CKEditorFailure failure = CKEditorFailure.fromJson(values);
		LOG.warn("CKEditor on {} ran into an error in the browser: {}",
			target.getPage().getClass().getName(), failure);
		onError(failure, target);
	}

	/**
	 * Called with what the browser told of an error of CKEditor, once logged. Does nothing by
	 * default.
	 *
	 * @param failure
	 *            what the browser told of the error
	 * @param target
	 *            the Ajax request target
	 */
	protected void onError(CKEditorFailure failure, AjaxRequestTarget target)
	{
	}
}
