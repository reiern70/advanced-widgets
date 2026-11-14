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

import org.apache.wicket.util.io.IClusterable;

import com.github.openjson.JSONObject;

/**
 * What the browser tells of {@link CKEditor} running into an error ({@link CKEditorEvents#ERROR}),
 * which {@link CKEditorErrorEvent} hears of.
 * <p>
 * The browser sends it: the name and the message are what the error said there, from the user's
 * browser, never to be trusted nor shown as HTML. They come cut to {@value #MAX_LENGTH} characters
 * and without control characters, a line of a log fitting them.
 *
 * @param name
 *            the name of the error, {@code CKEditorError} for one of CKEditor
 * @param message
 *            what the error says
 * @param started
 *            whether CKEditor had started: {@code false} when it could not start, its text area
 *            then shown
 * @param restarting
 *            whether the editor starts CKEditor again, with the text it had
 * @param gaveUp
 *            whether the editor gave up starting CKEditor again, the errors coming too often
 * @param crashes
 *            how many times CKEditor failed after it started in this page, this time among them
 * @since 11.0.0
 */
public record CKEditorFailure(String name, String message, boolean started, boolean restarting,
	boolean gaveUp, int crashes) implements IClusterable
{
	/** The longest name or message kept, in characters. */
	public static final int MAX_LENGTH = 1000;

	/**
	 * Takes the name and the message as the server keeps them, cut and without control characters,
	 * the name {@code Error} for none.
	 */
	public CKEditorFailure
	{
		name = clean(name);
		if (name.isEmpty())
		{
			name = "Error";
		}
		message = clean(message);
		crashes = Math.max(crashes, 0);
	}

	/**
	 * @param values
	 *            what the browser sent with the event
	 * @return the failure; one of an {@code Error} saying nothing for values without it
	 */
	public static CKEditorFailure fromJson(JSONObject values)
	{
		JSONObject json = values == null ? new JSONObject() : values;
		return new CKEditorFailure(json.optString("name", "Error"), json.optString("message", ""),
			json.optBoolean("started", true), json.optBoolean("restarting", false),
			json.optBoolean("gaveUp", false), json.optInt("crashes", 0));
	}

	private static String clean(String text)
	{
		if (text == null)
		{
			return "";
		}
		String cut = text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
		return cut.replaceAll("\\p{Cntrl}+", " ").strip();
	}

	/**
	 * @return what happened, in a line of a log: the name and the message, and what the editor does
	 *         about it
	 */
	@Override
	public String toString()
	{
		String done = !started ? "could not start" : restarting ? "starts again" :
			gaveUp ? "gave up starting again" : "runs on";
		return name + (message.isEmpty() ? "" : ": " + message) + " (" + done + ", crashes: " +
			crashes + ")";
	}
}
