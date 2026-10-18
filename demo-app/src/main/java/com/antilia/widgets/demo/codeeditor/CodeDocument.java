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
package com.antilia.widgets.demo.codeeditor;

import java.io.Serializable;

import com.antilia.widgets.codeeditor.CodeEditorLanguage;
import com.antilia.widgets.codeeditor.CodeEditorState;

/**
 * A document of the code editor demo: its name, its language, its text, the text it was saved with
 * and the state the user left its editor in, as the server keeps them.
 */
public class CodeDocument implements Serializable
{
	private static final long serialVersionUID = 1L;

	private final String name;

	private final CodeEditorLanguage language;

	private String text;

	private String saved;

	private CodeEditorState state;

	/**
	 * @param name
	 *            the name of the document
	 * @param language
	 *            its language
	 * @param text
	 *            its text, saved
	 */
	public CodeDocument(String name, CodeEditorLanguage language, String text)
	{
		this.name = name;
		this.language = language;
		this.text = text;
		this.saved = text;
	}

	/**
	 * @return a document of the same name, language and text, for a page of its own
	 */
	CodeDocument copy()
	{
		return new CodeDocument(name, language, text);
	}

	public String getName()
	{
		return name;
	}

	public CodeEditorLanguage getLanguage()
	{
		return language;
	}

	public String getText()
	{
		return text;
	}

	/**
	 * @param text
	 *            the text as the user changed it, {@code null} for none
	 */
	public void setText(String text)
	{
		this.text = text == null ? "" : text.replace("\r\n", "\n");
	}

	/**
	 * @return the text it was last saved with
	 */
	public String getSaved()
	{
		return saved;
	}

	/**
	 * @param saved
	 *            the text it was last saved with
	 */
	public void setSaved(String saved)
	{
		this.saved = saved == null ? "" : saved.replace("\r\n", "\n");
	}

	/**
	 * @return the state the user left its editor in, {@code null} before
	 */
	public CodeEditorState getState()
	{
		return state;
	}

	public void setState(CodeEditorState state)
	{
		this.state = state;
	}

	/**
	 * @return whether the text has changes not saved
	 */
	public boolean isModified()
	{
		return !text.equals(saved);
	}

	/**
	 * Saves the text.
	 */
	public void save()
	{
		saved = text;
	}
}
