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
package com.antiliacode.widgets.richtext;

/**
 * How a {@link RichTextEditor} keeps its text, which its model holds.
 *
 * @since 11.0.0
 */
public enum RichTextFormat
{
	/** HTML, which the editor {@link RichTextPolicy cleans} on the server. */
	HTML("html"),

	/** Markdown: underlined text and alignment are left out, Markdown has no syntax for them. */
	MARKDOWN("markdown"),

	/** Tiptap's JSON, the document as ProseMirror has it. */
	JSON("json");

	private final String name;

	RichTextFormat(String name)
	{
		this.name = name;
	}

	/**
	 * @return the name of the format in the browser
	 */
	public String getName()
	{
		return name;
	}
}
