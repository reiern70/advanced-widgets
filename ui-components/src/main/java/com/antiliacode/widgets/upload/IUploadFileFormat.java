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
package com.antiliacode.widgets.upload;

import java.util.Collection;

import org.apache.wicket.util.io.IClusterable;

/**
 * A format of the files users upload, told by the first bytes of the file, never by its name or
 * the type the browser gives, which anyone can set: PNG, PDF or a format of an application's own.
 * The format a file was found to be is the one it is served as, by its media type, and it says
 * whether a browser may open a file of it in its window ({@link #isInline()}) or only save it.
 * <p>
 * The images of the rich text editors are of the formats of
 * {@link com.antiliacode.widgets.richtext.IRichTextImageType}, which extends this interface.
 *
 * @since 10.11.0
 */
public interface IUploadFileFormat extends IClusterable
{
	/** How many of the first bytes of a file {@link #matches(byte[])} is given, at most. */
	int HEAD = 32;

	/**
	 * @return the media type a file of the format is served as, {@code application/pdf} for
	 *         example, which the browser checks the files against too before sending them
	 */
	String getMimeType();

	/**
	 * @return the extension a file of the format takes, without the dot
	 */
	String getExtension();

	/**
	 * @return what the messages call the format: its name, PDF for example, the same in every
	 *         language. A format of an application's own may translate it, in the language of the
	 *         session.
	 */
	String getLabel();

	/**
	 * @param head
	 *            the first bytes of a file, up to {@link #HEAD} of them: fewer for a shorter file
	 * @return whether the file is of this format
	 */
	boolean matches(byte[] head);

	/**
	 * @return whether a browser may open a file of the format in its window, as an image or a PDF,
	 *         rather than only save it: the file is then served {@code inline}, else as an
	 *         {@code attachment}. Either way it is served as its media type, with
	 *         {@code X-Content-Type-Options: nosniff} and in a sandbox
	 *         ({@code Content-Security-Policy: sandbox}), so that it runs no script of the site's.
	 *         {@code false} by default.
	 */
	default boolean isInline()
	{
		return false;
	}

	/**
	 * @param head
	 *            the first bytes of a file, up to {@link #HEAD} of them
	 * @param formats
	 *            the formats the file may be of
	 * @return the first of the formats the file is of, {@code null} for none
	 */
	static <T extends IUploadFileFormat> T detect(byte[] head, Collection<T> formats)
	{
		if (head == null || formats == null)
		{
			return null;
		}
		for (T format : formats)
		{
			if (format != null && format.matches(head))
			{
				return format;
			}
		}
		return null;
	}
}
