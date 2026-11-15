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

import com.antiliacode.widgets.event.IClientEventName;

/**
 * The events {@link CKEditor} fires in the browser besides those it shares with the Tiptap editor,
 * {@link com.antiliacode.widgets.richtext.RichTextEditorEvents}.
 *
 * @since 11.0.0
 */
public enum CKEditorEvents implements IClientEventName
{
	/**
	 * CKEditor ran into an error after it started, which CKEditor 49 tells by its
	 * {@code onEditorError}, or could not start: what the error says and what the editor does about
	 * it, starting CKEditor again or not ({@link CKEditor#setRecovery(CKEditorRecovery)}).
	 * {@link CKEditorErrorEvent} hears of it on the server.
	 */
	ERROR("antilia-ck-editor:error");

	private final String eventName;

	CKEditorEvents(String eventName)
	{
		this.eventName = eventName;
	}

	@Override
	public String getEventName()
	{
		return eventName;
	}
}
