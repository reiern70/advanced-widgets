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

import java.io.Serializable;

import org.apache.wicket.util.lang.Args;

import com.github.openjson.JSONObject;

/**
 * An error a {@link CodeEditor} shows in its text: a feedback message of the editor, of its
 * validators for example, and where in the text it is when the validator told, as
 * {@link JsonValidator} does. The editor underlines it there and lists it in a bar along its
 * bottom.
 *
 * @param message
 *            what is wrong, as the user reads it
 * @param line
 *            the line where it is, counted from 1; {@code null} when the error does not say
 * @param column
 *            the column in that line, counted from 1; {@code null} for the start of the line
 * @since 11.0.0
 */
public record CodeEditorError(String message, Integer line, Integer column) implements Serializable
{
	/**
	 * @param message
	 *            what is wrong, as the user reads it
	 * @param line
	 *            the line where it is, counted from 1; {@code null} when the error does not say
	 * @param column
	 *            the column in that line, counted from 1; {@code null} for the start of the line
	 */
	public CodeEditorError
	{
		Args.notNull(message, "message");
		line = line == null ? null : Math.max(line, 1);
		column = line == null || column == null ? null : Math.max(column, 1);
	}

	/**
	 * @param message
	 *            what is wrong, as the user reads it
	 * @return an error that does not say where it is
	 */
	public static CodeEditorError of(String message)
	{
		return new CodeEditorError(message, null, null);
	}

	/**
	 * @return the error as the editor in the browser takes it: {@code message}, and {@code line}
	 *         and {@code column} when it says where it is
	 */
	public JSONObject toJson()
	{
		JSONObject json = new JSONObject().put("message", message);
		if (line != null)
		{
			json.put("line", line.intValue());
			if (column != null)
			{
				json.put("column", column.intValue());
			}
		}
		return json;
	}
}
