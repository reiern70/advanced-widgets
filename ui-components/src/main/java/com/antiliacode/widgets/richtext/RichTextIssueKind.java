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

/**
 * What an issue a {@link IRichTextProofreader proofreader} found is: a word spelled wrong, a
 * mistake of grammar, or one of style. The editor underlines each in a color of its own.
 *
 * @since 11.0.0
 */
public enum RichTextIssueKind
{
	SPELLING("spelling"),
	GRAMMAR("grammar"),
	STYLE("style");

	private final String name;

	RichTextIssueKind(String name)
	{
		this.name = name;
	}

	/**
	 * @return its name in the browser: {@code spelling}, {@code grammar} or {@code style}
	 */
	public String getName()
	{
		return name;
	}
}
