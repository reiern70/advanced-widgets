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
package com.antiliacode.widgets.codeeditor;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.lang.Args;

/**
 * The language of the text of a {@link CodeEditor}, whose grammar highlights it: one the editor
 * knows, such as {@link #JAVA}, or one of the page's own, plugged in by a script.
 * <p>
 * The script of a language of one's own registers its grammar with
 * {@code Wicket.CodeEditor.registerLanguage(name, function (kit) {...})}, the function building it
 * from the CodeMirror classes of the kit, a {@code StreamLanguage} for example:
 *
 * <pre>
 * Wicket.CodeEditor.registerLanguage('log', function (kit) {
 *     return kit.StreamLanguage.define({
 *         token: function (stream) {
 *             if (stream.match(/^ERROR\b/)) {
 *                 return 'keyword';
 *             }
 *             stream.next();
 *             return null;
 *         }
 *     });
 * });
 * </pre>
 *
 * {@code CodeEditorLanguage.of("log", new JavaScriptResourceReference(MyPage.class, "log.js"))} is
 * the language then; the editor renders the script after its own.
 *
 * @since 11.0.0
 */
public final class CodeEditorLanguage implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** Java. */
	public static final CodeEditorLanguage JAVA = new CodeEditorLanguage("java", null);

	/** JavaScript, with JSX. */
	public static final CodeEditorLanguage JAVASCRIPT = new CodeEditorLanguage("javascript", null);

	/** TypeScript. */
	public static final CodeEditorLanguage TYPESCRIPT = new CodeEditorLanguage("typescript", null);

	/** JSON. */
	public static final CodeEditorLanguage JSON = new CodeEditorLanguage("json", null);

	/** Markdown, its fenced code blocks in the languages the editor knows. */
	public static final CodeEditorLanguage MARKDOWN = new CodeEditorLanguage("markdown", null);

	private static final List<CodeEditorLanguage> KNOWN = List.of(JAVA, JAVASCRIPT, TYPESCRIPT,
		JSON, MARKDOWN);

	private final String name;

	private final ResourceReference script;

	private CodeEditorLanguage(String name, ResourceReference script)
	{
		this.name = Args.notEmpty(name, "name").trim().toLowerCase(Locale.ROOT);
		this.script = script;
	}

	/**
	 * @param name
	 *            the name of a language the editor knows, or that a script of the page registered
	 * @return the language
	 */
	public static CodeEditorLanguage of(String name)
	{
		return of(name, null);
	}

	/**
	 * @param name
	 *            the name of the language, under which the script registers its grammar
	 * @param script
	 *            the script registering the grammar, {@code null} for none
	 * @return the language
	 */
	public static CodeEditorLanguage of(String name, ResourceReference script)
	{
		CodeEditorLanguage language = new CodeEditorLanguage(name, script);
		if (script == null)
		{
			for (CodeEditorLanguage known : KNOWN)
			{
				if (known.name.equals(language.name))
				{
					return known;
				}
			}
		}
		return language;
	}

	/**
	 * @return the languages the editor knows without a script of the page
	 */
	public static List<CodeEditorLanguage> known()
	{
		return KNOWN;
	}

	/**
	 * @return the name of the language in the browser, in lower case
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * @return the script registering the grammar, {@code null} for a language the editor knows
	 */
	public ResourceReference getScript()
	{
		return script;
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof CodeEditorLanguage language && language.name.equals(name);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public String toString()
	{
		return name;
	}
}
