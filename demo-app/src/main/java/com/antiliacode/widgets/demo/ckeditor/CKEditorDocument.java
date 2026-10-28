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
package com.antiliacode.widgets.demo.ckeditor;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.antiliacode.widgets.ckeditor.CKEditorPolicy;
import com.antiliacode.widgets.richtext.RichTextEditorState;

/**
 * A text the CKEditor demo keeps in the session: the text, the text last saved and the state of
 * its editor.
 */
final class CKEditorDocument implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** Whether the text is HTML, not Markdown. */
	private final boolean html;

	private String text;

	private String saved;

	private RichTextEditorState state;

	/**
	 * @param text
	 *            the text, saved as it is
	 * @param html
	 *            whether it is HTML, not Markdown
	 */
	CKEditorDocument(String text, boolean html)
	{
		this.text = text;
		this.saved = text;
		this.html = html;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public String getSaved()
	{
		return saved;
	}

	public void setSaved(String saved)
	{
		this.saved = saved;
	}

	public RichTextEditorState getState()
	{
		return state;
	}

	public void setState(RichTextEditorState state)
	{
		this.state = state;
	}

	/**
	 * Takes the text as the text last saved.
	 */
	void save()
	{
		saved = text;
	}

	/**
	 * @return whether the text has changes not saved: other content, not only written another
	 *         way, as CKEditor writes HTML and Markdown in its own
	 */
	boolean isModified()
	{
		return !Objects.equals(normalize(text), normalize(saved));
	}

	/**
	 * @return a text in one shape whatever wrote it: HTML as the cleaning writes it, Markdown
	 *         without the white space at its end
	 */
	private String normalize(String value)
	{
		String given = value == null ? "" : value;
		return html ? CKEditorPolicy.ALL.sanitize(given) : given.stripTrailing();
	}

	/**
	 * @return the words of a text in HTML or Markdown: what is between its tags and marks
	 */
	static List<String> words(String text)
	{
		String plain = text == null ? "" : text.replaceAll("<[^>]*>", " ")
			.replaceAll("&[a-zA-Z#0-9]+;", " ").replaceAll("[#*>`_\\[\\]()~-]", " ");
		return List.of(plain.trim().split("\\s+")).stream().filter(word -> !word.isEmpty())
			.toList();
	}
}
