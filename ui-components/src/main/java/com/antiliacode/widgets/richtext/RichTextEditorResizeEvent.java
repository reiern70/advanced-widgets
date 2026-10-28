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
 * Hears on the server of the size the user resized a {@link RichTextEditor} or a
 * {@link com.antiliacode.widgets.ckeditor.CKEditor} to, once they let go
 * ({@link RichTextEditorEvents#RESIZE}), as its width and its height in pixels: the dimension the
 * user resizes as they left it, the other as the page lays the editor out, which the browser
 * measures.
 *
 * <pre>
 * editor.addEvent(RichTextEditorResizeEvent.of((width, height, target) -&gt; ...));
 * editor.addEvent(new RichTextEditorResizeEvent()
 * {
 *     protected void onResize(int width, int height, AjaxRequestTarget target)
 *     {
 *         ...
 *     }
 * });
 * </pre>
 *
 * The browser sends the size: the server checks it before it trusts it, a width of a million
 * pixels for example.
 *
 * @since 11.0.0
 */
public abstract class RichTextEditorResizeEvent extends AjaxEvent
{
	private static final long serialVersionUID = 1L;

	/** A dimension the browser did not send, or sent as something else than pixels. */
	public static final int UNKNOWN = -1;

	/**
	 * What the browser runs: sends the width and the height, those the user gave the editor, else
	 * those of the editor's box.
	 */
	static final String CLIENT_FUNCTION = "function(event,editor,send){" +
		"var s=(event.detail&&event.detail.size)||{}," +
		"b=editor&&editor.element?editor.element.getBoundingClientRect():null;" +
		"send({width:s.width!=null?s.width:(b?Math.round(b.width):null)," +
		"height:s.height!=null?s.height:(b?Math.round(b.height):null)});}";

	/**
	 * What the server does with the size: {@link RichTextEditorResizeEvent#onResize(int, int,
	 * AjaxRequestTarget)}, as a lambda.
	 */
	@FunctionalInterface
	public interface IResizeListener extends IClusterable
	{
		/**
		 * @param width
		 *            the width of the editor in pixels, {@link #UNKNOWN} when the browser did not
		 *            send it
		 * @param height
		 *            the height of the editor in pixels, {@link #UNKNOWN} when the browser did not
		 *            send it
		 * @param target
		 *            the Ajax request target
		 */
		void onResize(int width, int height, AjaxRequestTarget target);
	}

	/**
	 * Constructor.
	 */
	protected RichTextEditorResizeEvent()
	{
		super(RichTextEditorEvents.RESIZE);
		setClientFunction(CLIENT_FUNCTION);
	}

	/**
	 * @param listener
	 *            what the server does with the size
	 * @return the event
	 */
	public static RichTextEditorResizeEvent of(IResizeListener listener)
	{
		Args.notNull(listener, "listener");
		return new RichTextEditorResizeEvent()
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected void onResize(int width, int height, AjaxRequestTarget target)
			{
				listener.onResize(width, height, target);
			}
		};
	}

	/**
	 * Reads the size the browser sent, and calls {@link #onResize(int, int, AjaxRequestTarget)}.
	 */
	@Override
	public final void onEvent(String eventId, JSONObject values, AjaxRequestTarget target)
	{
		onResize(pixels(values.opt("width")), pixels(values.opt("height")), target);
	}

	/**
	 * @return the pixels of a number the browser sent, rounded; {@link #UNKNOWN} for none, for
	 *         one below zero and for what is not a number
	 */
	private static int pixels(Object value)
	{
		double pixels;
		if (value instanceof Number number)
		{
			pixels = number.doubleValue();
		}
		else if (value instanceof String text)
		{
			try
			{
				pixels = Double.parseDouble(text.trim());
			}
			catch (NumberFormatException e)
			{
				return UNKNOWN;
			}
		}
		else
		{
			return UNKNOWN;
		}
		return Double.isFinite(pixels) && pixels >= 0 && pixels <= Integer.MAX_VALUE ?
			(int)Math.round(pixels) : UNKNOWN;
	}

	/**
	 * Called with the size the user resized the editor to.
	 *
	 * @param width
	 *            the width of the editor in pixels, {@link #UNKNOWN} when the browser did not send
	 *            it
	 * @param height
	 *            the height of the editor in pixels, {@link #UNKNOWN} when the browser did not send
	 *            it
	 * @param target
	 *            the Ajax request target
	 */
	protected abstract void onResize(int width, int height, AjaxRequestTarget target);
}
