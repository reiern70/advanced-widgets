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

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;

/**
 * Whether the OWASP Java HTML Sanitizer is on the class path. The rich text editors, CKEditor and
 * their views clean the HTML with it ({@link RichTextPolicy},
 * {@link com.antiliacode.widgets.ckeditor.CKEditorPolicy CKEditorPolicy}), and nothing else in
 * the components needs it, so it is an optional dependency of ui-components: an application with
 * the editors adds {@value #ARTIFACT}. Without it, an editor or a view fails as it is added to a
 * page, rather than when the user saves the text.
 *
 * @since 11.0.0
 */
public final class OwaspHtmlSanitizer
{
	/** The Maven coordinates of the sanitizer, without the version. */
	public static final String ARTIFACT =
		"com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer";

	private static final boolean PRESENT = loads("org.owasp.html.PolicyFactory");

	private OwaspHtmlSanitizer()
	{
	}

	/**
	 * @return whether the sanitizer is on the class path
	 */
	public static boolean isPresent()
	{
		return PRESENT;
	}

	/**
	 * Fails unless the sanitizer is on the class path.
	 *
	 * @param component
	 *            the editor or the view needing it
	 * @throws WicketRuntimeException
	 *             telling the dependency to add, if the sanitizer is missing
	 */
	public static void require(Component component)
	{
		require(component, PRESENT);
	}

	static void require(Component component, boolean present)
	{
		if (!present)
		{
			throw new WicketRuntimeException("The rich text editors, CKEditor and their views " +
				"clean the HTML with the OWASP Java HTML Sanitizer, an optional dependency of " +
				"ui-components: add " + ARTIFACT + " to the application for " + component);
		}
	}

	static boolean loads(String className)
	{
		try
		{
			Class.forName(className, false, OwaspHtmlSanitizer.class.getClassLoader());
			return true;
		}
		catch (ClassNotFoundException | LinkageError e)
		{
			return false;
		}
	}
}
